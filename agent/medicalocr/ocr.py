from __future__ import annotations

import os
import re
from io import BytesIO
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageEnhance, ImageOps
from rapidocr.ch_ppocr_det import TextDetector
from rapidocr.ch_ppocr_rec import TextRecognizer, TextRecInput
from rapidocr.utils.parse_parameters import ParseParams
from rapidocr.utils.process_img import get_rotate_crop_image
from rapidocr.utils.typings import EngineType, LangDet, LangRec, ModelType, OCRVersion, TaskType

from .analyze import OCRBlock, OCRResult
from .normalize import collapse_whitespace


@dataclass(frozen=True, slots=True)
class OCRPipeline:
    det_model_path: Path
    rec_model_path: Path
    rec_keys_path: Path
    detector: TextDetector
    recognizer: TextRecognizer


def _package_models_dir() -> Path:
    import rapidocr

    return Path(rapidocr.__file__).resolve().parent / "models"


def _repo_models_dir() -> Path:
    raw = os.getenv("MEDICALOCR_OCR_MODEL_DIR", "models/ocr")
    path = Path(raw).expanduser()
    if not path.is_absolute():
        path = Path.cwd() / path
    return path


def _build_pipeline() -> OCRPipeline:
    model_dir = _repo_models_dir()
    det_model = model_dir / "ch_PP-OCRv5_mobile_det.onnx"
    if not det_model.exists():
        det_model = model_dir / "ch_PP-OCRv5_det_mobile.onnx"
    rec_model = model_dir / "latin_PP-OCRv5_rec_mobile_infer.onnx"
    if not rec_model.exists():
        rec_model = model_dir / "latin_PP-OCRv5_rec_mobile.onnx"
    rec_keys = model_dir / "ppocrv5_latin_dict.txt"
    if not rec_keys.exists():
        rec_keys = model_dir / "ppocrv5_dict.txt"

    missing = [path.name for path in [det_model, rec_model, rec_keys] if not path.exists()]
    if missing:
        raise FileNotFoundError(
            "Missing required PP-OCRv5 model files in "
            f"{model_dir}. Missing: {', '.join(missing)}"
        )

    cfg = ParseParams.load(_package_models_dir().parent / "config.yaml")
    cfg.Global.use_cls = False
    cfg.Global.model_root_dir = model_dir
    cfg.Global.font_path = None

    cfg.Det.engine_type = EngineType.ONNXRUNTIME
    cfg.Det.ocr_version = OCRVersion.PPOCRV5
    cfg.Det.lang_type = LangDet.CH
    cfg.Det.model_type = ModelType.MOBILE
    cfg.Det.engine_cfg = cfg.EngineConfig[cfg.Det.engine_type.value]
    cfg.Det.model_root_dir = model_dir
    cfg.Det.model_path = det_model

    cfg.Rec.engine_type = EngineType.ONNXRUNTIME
    cfg.Rec.ocr_version = OCRVersion.PPOCRV5
    cfg.Rec.lang_type = LangRec.LATIN
    cfg.Rec.model_type = ModelType.MOBILE
    cfg.Rec.engine_cfg = cfg.EngineConfig[cfg.Rec.engine_type.value]
    cfg.Rec.font_path = cfg.Global.font_path
    cfg.Rec.model_root_dir = model_dir
    cfg.Rec.model_path = rec_model
    cfg.Rec.rec_keys_path = rec_keys
    cfg.Rec.task_type = TaskType.REC

    detector = TextDetector(cfg.Det)
    recognizer = TextRecognizer(cfg.Rec)
    return OCRPipeline(
        det_model_path=det_model,
        rec_model_path=rec_model,
        rec_keys_path=rec_keys,
        detector=detector,
        recognizer=recognizer,
    )


@lru_cache(maxsize=1)
def get_ocr_pipeline() -> OCRPipeline:
    return _build_pipeline()


def _bbox_from_points(points: Any) -> list[int] | None:
    if points is None:
        return None

    arr = np.asarray(points, dtype=float)
    if arr.size < 8:
        return None

    arr = arr.reshape(-1, 2)
    left = int(np.floor(np.min(arr[:, 0])))
    top = int(np.floor(np.min(arr[:, 1])))
    right = int(np.ceil(np.max(arr[:, 0])))
    bottom = int(np.ceil(np.max(arr[:, 1])))
    return [left, top, right, bottom]


def _as_sequence(value: Any) -> list[Any]:
    if value is None:
        return []
    if isinstance(value, np.ndarray):
        return list(value)
    if isinstance(value, (list, tuple)):
        return list(value)
    return [value]


_NON_WORD_RE = re.compile(r"^[\W_]+$", re.UNICODE)


def _is_meaningful_ocr_text(text: str, confidence: float) -> bool:
    if confidence < 0.25:
        return False
    if not text or _NON_WORD_RE.fullmatch(text):
        return False
    if len(text) <= 2:
        return False
    if " " not in text and len(text) <= 3 and not any(ch.isdigit() for ch in text):
        return False
    if " " not in text and len(text) <= 4 and confidence < 0.8:
        return False
    upper_count = sum(1 for ch in text if ch.isupper())
    lower_count = sum(1 for ch in text if ch.islower())
    if " " not in text and upper_count > 1 and lower_count > 1 and confidence < 0.85:
        return False
    return True


def _build_display_text(blocks: list[OCRBlock]) -> str:
    if not blocks:
        return ""

    line_items: list[dict[str, object]] = []
    for index, block in enumerate(blocks):
        text = collapse_whitespace(block.text)
        if not text or not _is_meaningful_ocr_text(text, block.confidence):
            continue

        bbox = block.bbox
        if bbox:
            left, top, right, bottom = bbox
            center_y = (top + bottom) / 2.0
            height = max(1.0, float(bottom - top))
        else:
            left = top = right = bottom = 0.0
            center_y = float("inf")
            height = 1.0

        line_items.append(
            {
                "index": index,
                "text": text,
                "bbox": bbox,
                "confidence": float(block.confidence),
                "left": float(left),
                "top": float(top),
                "right": float(right),
                "bottom": float(bottom),
                "center_y": float(center_y),
                "height": float(height),
            }
        )

    if not line_items:
        return ""

    line_items.sort(key=lambda item: (item["top"], item["left"], item["index"]))

    merged_lines: list[dict[str, object]] = []
    for item in line_items:
        if not merged_lines:
            merged_lines.append(
                {
                    "index": item["index"],
                    "texts": [item["text"]],
                    "bbox": item["bbox"],
                    "confidence_values": [item["confidence"]],
                    "left": item["left"],
                    "top": item["top"],
                    "right": item["right"],
                    "bottom": item["bottom"],
                    "center_y": item["center_y"],
                    "height": item["height"],
                }
            )
            continue

        last = merged_lines[-1]
        same_line = False
        if last["bbox"] and item["bbox"]:
            last_height = max(1.0, float(last["height"]))
            item_height = max(1.0, float(item["height"]))
            tolerance = max(last_height, item_height) * 0.68
            same_line = abs(float(item["center_y"]) - float(last["center_y"])) <= tolerance

        if same_line:
            last["texts"].append(item["text"])
            last["confidence_values"].append(item["confidence"])
            last["left"] = min(float(last["left"]), float(item["left"]))
            last["top"] = min(float(last["top"]), float(item["top"]))
            last["right"] = max(float(last["right"]), float(item["right"]))
            last["bottom"] = max(float(last["bottom"]), float(item["bottom"]))
            last["center_y"] = (float(last["top"]) + float(last["bottom"])) / 2.0
            last["height"] = max(1.0, float(last["bottom"]) - float(last["top"]))
            continue

        merged_lines.append(
            {
                "index": item["index"],
                "texts": [item["text"]],
                "bbox": item["bbox"],
                "confidence_values": [item["confidence"]],
                "left": item["left"],
                "top": item["top"],
                "right": item["right"],
                "bottom": item["bottom"],
                "center_y": item["center_y"],
                "height": item["height"],
            }
        )

    scored: list[tuple[float, int, str, list[int] | None]] = []
    tops = [float(line["top"]) for line in merged_lines if line["bbox"]]
    areas: list[float] = []
    for line in merged_lines:
        if not line["bbox"]:
            continue
        areas.append(max(0.0, (float(line["right"]) - float(line["left"])) * (float(line["bottom"]) - float(line["top"]))))

    min_top = min(tops) if tops else 0.0
    max_top = max(tops) if tops else 0.0
    max_area = max(areas) if areas else 0.0

    for line in merged_lines:
        text = collapse_whitespace(" ".join(line["texts"]))
        tokens = [token for token in text.split() if token]
        if not tokens:
            continue
        if len(tokens) > 18 and len(text) > 90:
            continue

        area_score = 0.0
        top_score = 0.0
        if line["bbox"]:
            area = max(0.0, (float(line["right"]) - float(line["left"])) * (float(line["bottom"]) - float(line["top"])))
            area_score = (area / max_area) if max_area else 0.0
            top_score = 1.0
            if max_top > min_top:
                top_score = 1.0 - ((float(line["top"]) - min_top) / (max_top - min_top))
                top_score = max(0.0, min(1.0, top_score))

        word_count = len(tokens)
        if word_count <= 4:
            brevity_score = 1.0
        elif word_count <= 8:
            brevity_score = 0.92
        elif word_count <= 14:
            brevity_score = 0.8
        elif word_count <= 22:
            brevity_score = 0.58
        else:
            brevity_score = 0.35

        confidence = float(np.mean(line["confidence_values"])) if line["confidence_values"] else 0.0
        score = (
            0.40 * area_score
            + 0.25 * max(0.0, min(1.0, confidence))
            + 0.20 * top_score
            + 0.15 * brevity_score
        )
        scored.append((score, int(line["index"]), text, line["bbox"]))

    if not scored:
        return ""

    scored.sort(key=lambda item: (-item[0], item[1]))
    chosen: list[tuple[int, str, list[int] | None]] = []
    seen: set[str] = set()
    for _, index, text, bbox in scored:
        normalized = collapse_whitespace(text).lower()
        if not normalized or normalized in seen:
            continue
        seen.add(normalized)
        chosen.append((index, text, bbox))
        if len(chosen) >= 4:
            break

    if not chosen:
        return ""

    def sort_key(item: tuple[int, str, list[int] | None]) -> tuple[float, float, int]:
        index, _, bbox = item
        if bbox:
            return (float(bbox[1]), float(bbox[0]), index)
        return (float("inf"), float("inf"), index)

    chosen.sort(key=sort_key)
    return collapse_whitespace("\n".join(text for _, text, _ in chosen))


def _load_image_as_pil(image: Any) -> Image.Image:
    if isinstance(image, Image.Image):
        return ImageOps.exif_transpose(image).convert("RGB")

    if isinstance(image, np.ndarray):
        array = np.asarray(image)
        if array.ndim == 2:
            return Image.fromarray(array.astype(np.uint8), mode="L").convert("RGB")
        if array.ndim == 3 and array.shape[2] >= 3:
            rgb = array[:, :, :3][:, :, ::-1].copy()
            return Image.fromarray(rgb.astype(np.uint8), mode="RGB")
        raise TypeError("Unsupported numpy image shape.")

    raw: bytes
    if hasattr(image, "read"):
        raw = image.read()
    elif isinstance(image, (bytes, bytearray, memoryview)):
        raw = bytes(image)
    elif isinstance(image, (str, Path)):
        with Path(image).open("rb") as handle:
            raw = handle.read()
    else:
        raise TypeError(f"Unsupported image type: {type(image)!r}")

    with Image.open(BytesIO(raw)) as pil_image:
        return ImageOps.exif_transpose(pil_image).convert("RGB")


def _preprocess_pil_image(image: Image.Image) -> Image.Image:
    processed = image
    longest_side = max(processed.size)
    if longest_side < 960:
        scale = max(1.15, 960 / max(longest_side, 1))
        new_size = (
            max(1, int(round(processed.width * scale))),
            max(1, int(round(processed.height * scale))),
        )
        processed = processed.resize(new_size, Image.Resampling.LANCZOS)
    elif longest_side > 2400:
        processed.thumbnail((2400, 2400), Image.Resampling.LANCZOS)

    processed = ImageOps.autocontrast(processed)
    processed = ImageEnhance.Contrast(processed).enhance(1.18)
    processed = ImageEnhance.Sharpness(processed).enhance(1.12)
    return processed


def _pil_to_model_array(image: Image.Image) -> np.ndarray:
    array = np.asarray(image)
    if array.ndim == 2:
        return array
    return array[:, :, ::-1].copy()


def extract_ocr_result(image: Any) -> OCRResult:
    pipeline = get_ocr_pipeline()
    try:
        pil_image = _load_image_as_pil(image)
    except Exception as exc:
        raise ValueError(f"Failed to decode image: {exc}") from exc

    prepared_image = _pil_to_model_array(_preprocess_pil_image(pil_image))
    det_res = pipeline.detector(prepared_image)
    if det_res.boxes is None or len(det_res.boxes) == 0:
        return OCRResult(engine="rapidocr", raw_text="", confidence=0.0, blocks=[])

    crop_img_list = [get_rotate_crop_image(prepared_image, box) for box in det_res.boxes]
    rec_res = pipeline.recognizer(TextRecInput(img=crop_img_list, return_word_box=False))

    texts = _as_sequence(getattr(rec_res, "txts", None))
    scores = _as_sequence(getattr(rec_res, "scores", None))
    boxes = _as_sequence(getattr(det_res, "boxes", None))

    blocks: list[OCRBlock] = []
    for index, text in enumerate(texts):
        cleaned_text = collapse_whitespace(str(text))
        if not cleaned_text:
            continue

        confidence = 0.0
        if index < len(scores):
            try:
                confidence = float(scores[index])
            except (TypeError, ValueError):
                confidence = 0.0

        bbox = None
        if index < len(boxes):
            bbox = _bbox_from_points(boxes[index])

        if not _is_meaningful_ocr_text(cleaned_text, confidence):
            continue

        blocks.append(OCRBlock(text=cleaned_text, bbox=bbox, confidence=confidence))

    raw_text = collapse_whitespace(" ".join(block.text for block in blocks))
    display_text = _build_display_text(blocks)
    confidence = float(np.mean([block.confidence for block in blocks])) if blocks else 0.0
    return OCRResult(
        engine="rapidocr",
        raw_text=raw_text,
        display_text=display_text,
        confidence=confidence,
        blocks=blocks,
    )
