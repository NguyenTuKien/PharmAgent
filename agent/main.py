import logging
import base64
import cv2
import numpy as np
import json
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, APIRouter, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from typing import List, Optional

# --- CONFIGURATION & LOGGING ---
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="PharmAgent AI Service - Real-time")

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = exc.errors()
    logger.error(f"Validation error on {request.method} {request.url.path}: {errors}")
    return JSONResponse(status_code=422, content={"detail": errors})

# --- DATA MODELS ---
class PillScanResponse(BaseModel):
    pillId: Optional[str] = None
    name: Optional[str] = None
    genericName: Optional[str] = None
    brandName: Optional[str] = None
    strength: Optional[str] = None
    confidenceScore: float = 0.0
    # Các trường mở rộng khác giữ nguyên như cũ
    description: Optional[str] = "Kết quả nhận diện từ camera real-time"

# --- ROUTER CONFIG ---
router = APIRouter(prefix="/api")

@router.get("/health")
async def health():
    return {"status": "UP", "mode": "real-time"}

# --- REAL-TIME WEBSOCKET LOGIC ---
@router.websocket("/pills/scan/ws")
async def websocket_pill_scan(websocket: WebSocket):
    """
    Endpoint phục vụ việc 'lia cam'. Frontend gửi các frame ảnh liên tục (Base64),
    Backend xử lý và trả về kết quả ngay lập tức.
    """
    await websocket.accept()
    logger.info("Client connected for Real-time Scanning")
    
    try:
        while True:
            # 1. Nhận dữ liệu từ Frontend (dạng văn bản chứa chuỗi Base64 của frame ảnh)
            data = await websocket.receive_text()
            
            # 2. Xử lý chuỗi Base64 thành ảnh OpenCV
            try:
                # Tách bỏ tiền tố 'data:image/jpeg;base64,' nếu có
                if "," in data:
                    data = data.split(",")[1]
                
                img_bytes = base64.b64decode(data)
                nparr = np.frombuffer(img_bytes, np.uint8)
                frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

                if frame is None:
                    continue

                # 3. MOCK AI LOGIC (Đây là nơi bạn gọi Model YOLO/TensorFlow của mình)
                # Ví dụ: chỉ trả về kết quả nếu nhận diện được vật thể có diện tích > 10% frame
                # result = your_model.predict(frame)
                
                # Giả lập: AI nhận diện được Paracetamol khi nhận được dữ liệu
                scan_result = PillScanResponse(
                    pillId="p001",
                    name="Paracetamol",
                    genericName="Acetaminophen",
                    brandName="Hapacol 500",
                    strength="500mg",
                    confidenceScore=0.95
                )

                # 4. Trả về kết quả cho Frontend qua WebSocket
                await websocket.send_text(scan_result.model_dump_json())

            except Exception as e:
                logger.error(f"Error processing frame: {e}")
                await websocket.send_json({"error": "Frame processing failed"})

    except WebSocketDisconnect:
        logger.info("Client disconnected from Real-time Scanning")

app.include_router(router)