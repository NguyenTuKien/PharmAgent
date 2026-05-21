from __future__ import annotations

from pathlib import Path
from typing import Any, Sequence

from fastapi import FastAPI, File, Form, HTTPException, UploadFile

from .analyze import AnalyzeResponse, AnalyzeTextRequest, analyze_ocr_result, analyze_text
from .catalog import CatalogProduct
from .database import DEFAULT_DB_PATH, load_catalog_products_from_db
from .ocr import extract_ocr_result


def create_app(
    *,
    catalog: Sequence[CatalogProduct] | None = None,
    db_path: str | Path = DEFAULT_DB_PATH,
) -> Any:
    app = FastAPI(title="MedicalOCR", version="0.1.0")
    loaded_catalog = (
        list(catalog)
        if catalog is not None
        else load_catalog_products_from_db(db_path=db_path)
    )
    app.state.catalog = loaded_catalog
    app.state.db_path = str(db_path)

    @app.get("/health")
    async def health() -> dict[str, object]:
        return {"status": "ok", "catalog_size": len(loaded_catalog)}

    @app.post("/match", response_model=AnalyzeResponse)
    async def match_text(payload: AnalyzeTextRequest) -> AnalyzeResponse:
        return analyze_text(
            payload.ocr_text,
            loaded_catalog,
            top_k=payload.top_k,
            request_id=payload.request_id,
        )

    @app.post("/analyze", response_model=AnalyzeResponse)
    async def analyze(
        image: UploadFile | None = File(default=None),
        ocr_text: str | None = Form(default=None),
        top_k: int = Form(default=4),
    ) -> AnalyzeResponse:
        if not image and not ocr_text:
            raise HTTPException(status_code=400, detail="Send an image or ocr_text.")
        if ocr_text:
            return analyze_text(ocr_text, loaded_catalog, top_k=top_k)
        assert image is not None
        try:
            image_bytes = await image.read()
            ocr_result = extract_ocr_result(image_bytes)
        except Exception as exc:  # pragma: no cover - runtime safety net
            raise HTTPException(status_code=503, detail=f"OCR failed: {exc}") from exc
        return analyze_ocr_result(ocr_result, loaded_catalog, top_k=top_k)

    return app
