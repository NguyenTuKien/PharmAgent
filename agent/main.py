import logging
import base64
import cv2
import numpy as np
import json
import os
import httpx
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, APIRouter, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from typing import List, Optional

# --- CONFIGURATION & LOGGING ---
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# URL của Backend (để lấy presign và add image)
BACKEND_URL = os.getenv("BACKEND_URL", "http://backend:8080")

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
    description: Optional[str] = "Kết quả nhận diện từ camera real-time"
    imageUrl: Optional[str] = None  # URL ảnh đã upload lên Cloudinary


# --- CLOUDINARY UPLOAD HELPER ---
async def upload_frame_to_cloudinary(frame_bytes: bytes, pill_id: Optional[str]) -> Optional[str]:
    """
    Lấy presigned URL từ backend rồi upload frame ảnh lên Cloudinary.
    Trả về secure_url nếu thành công, None nếu thất bại.
    """
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            # 1. Lấy presigned info từ backend
            presign_resp = await client.get(
                f"{BACKEND_URL}/api/upload/presign",
                params={"folder": "pill"}
            )
            if presign_resp.status_code != 200:
                logger.warning(f"Presign request failed: {presign_resp.status_code}")
                return None

            presign = presign_resp.json()

            # 2. Upload ảnh lên Cloudinary
            upload_resp = await client.post(
                presign["uploadUrl"],
                data={
                    "api_key":   presign["apiKey"],
                    "timestamp": str(presign["timestamp"]),
                    "signature": presign["signature"],
                    "folder":    presign["folder"],
                },
                files={"file": ("scan.jpg", frame_bytes, "image/jpeg")}
            )

            if upload_resp.status_code != 200:
                logger.warning(f"Cloudinary upload failed: {upload_resp.status_code} {upload_resp.text}")
                return None

            secure_url = upload_resp.json().get("secure_url")
            logger.info(f"Uploaded scan image to Cloudinary: {secure_url}")

            # 3. Nếu có pillId thì gọi backend để add image vào pill
            if pill_id and secure_url:
                add_resp = await client.post(
                    f"{BACKEND_URL}/api/admin/pills/{pill_id}/images",
                    json={
                        "imageUrl":  secure_url,
                        "viewType":  "FRONT",
                        "isPrimary": False
                    }
                )
                if add_resp.status_code not in (200, 201):
                    logger.warning(f"Add pill image failed: {add_resp.status_code} {add_resp.text}")

            return secure_url

    except Exception as e:
        logger.error(f"Error during Cloudinary upload: {e}")
        return None


# --- ROUTER CONFIG ---
router = APIRouter(prefix="/ws")

@router.get("/health")
async def health():
    return {"status": "UP", "mode": "real-time"}

# --- REAL-TIME WEBSOCKET LOGIC ---
@router.websocket("/agent")
async def websocket_pill_scan(websocket: WebSocket):
    """
    Endpoint phục vụ việc 'lia cam'. Frontend gửi các frame ảnh liên tục (Base64),
    Backend xử lý và trả về kết quả ngay lập tức.
    Khi AI nhận diện thành công (confidence >= 0.8), ảnh được upload lên Cloudinary
    và URL ảnh được đính kèm trong response.
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
                # result = your_model.predict(frame)
                
                # Giả lập: AI nhận diện được Paracetamol khi nhận được dữ liệu
                detected_pill_id = "p001"
                confidence = 0.95

                scan_result = PillScanResponse(
                    pillId=detected_pill_id,
                    name="Paracetamol",
                    genericName="Acetaminophen",
                    brandName="Hapacol 500",
                    strength="500mg",
                    confidenceScore=confidence
                )

                # 4. Nếu confidence đủ cao → upload ảnh lên Cloudinary
                if confidence >= 0.8:
                    # Encode frame hiện tại thành JPEG bytes để upload
                    _, jpeg_buf = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, 85])
                    image_url = await upload_frame_to_cloudinary(
                        jpeg_buf.tobytes(),
                        pill_id=detected_pill_id
                    )
                    scan_result.imageUrl = image_url

                # 5. Trả về kết quả cho Frontend qua WebSocket
                await websocket.send_text(scan_result.model_dump_json())

            except Exception as e:
                logger.error(f"Error processing frame: {e}")
                await websocket.send_json({"error": "Frame processing failed"})

    except WebSocketDisconnect:
        logger.info("Client disconnected from Real-time Scanning")

app.include_router(router)