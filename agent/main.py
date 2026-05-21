import asyncio
import base64
import logging

from fastapi import APIRouter, FastAPI, File, Form, HTTPException, Request, UploadFile, WebSocket, WebSocketDisconnect
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from medicalocr.analyze import AnalyzeResponse, AnalyzeTextRequest, analyze_ocr_result, analyze_text
from medicalocr.database import DEFAULT_DB_PATH, load_catalog_products_from_db
from medicalocr.ocr import extract_ocr_result

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="PharmAgent AI Service - MedicalOCR")
app.state.catalog = load_catalog_products_from_db(DEFAULT_DB_PATH)


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = exc.errors()
    logger.error("Validation error on %s %s: %s", request.method, request.url.path, errors)
    return JSONResponse(status_code=422, content={"detail": errors})


api_router = APIRouter()
ws_router = APIRouter(prefix="/ws")


@ws_router.get("/health")
async def health():
    return {
        "status": "UP",
        "mode": "medicalocr",
        "catalog_size": len(app.state.catalog),
    }


def _decode_image_payload(data: str) -> bytes:
    if "," in data:
        data = data.split(",", 1)[1]
    return base64.b64decode(data)


def _analyze_image(image_bytes: bytes, top_k: int = 4):
    ocr_result = extract_ocr_result(image_bytes)
    return analyze_ocr_result(ocr_result, app.state.catalog, top_k=top_k)


@api_router.post("/analyze", response_model=AnalyzeResponse)
async def analyze_image(image: UploadFile = File(...), top_k: int = Form(default=4)):
    image_bytes = await image.read()
    if not image_bytes:
        raise HTTPException(status_code=400, detail="Image is required.")

    try:
        return await asyncio.to_thread(_analyze_image, image_bytes, top_k)
    except Exception as exc:
        logger.exception("MedicalOCR image analysis failed")
        raise HTTPException(status_code=503, detail=f"OCR failed: {exc}") from exc


@api_router.post("/search", response_model=AnalyzeResponse)
async def search_text(payload: AnalyzeTextRequest):
    if not payload.ocr_text.strip():
        raise HTTPException(status_code=400, detail="ocr_text is required.")

    return await asyncio.to_thread(
        analyze_text,
        payload.ocr_text,
        app.state.catalog,
        top_k=payload.top_k,
        request_id=payload.request_id,
    )


@ws_router.websocket("/agent")
async def websocket_pill_scan(websocket: WebSocket):
    await websocket.accept()
    logger.info("Client connected for MedicalOCR scanning")

    try:
        while True:
            data = await websocket.receive_text()

            try:
                image_bytes = _decode_image_payload(data)
                result = await asyncio.to_thread(_analyze_image, image_bytes)
                await websocket.send_text(result.model_dump_json())
            except Exception as exc:
                logger.exception("Error processing MedicalOCR frame")
                await websocket.send_json(
                    {
                        "error": "Frame processing failed",
                        "detail": str(exc),
                    }
                )

    except WebSocketDisconnect:
        logger.info("Client disconnected from MedicalOCR scanning")


app.include_router(api_router)
app.include_router(ws_router)
