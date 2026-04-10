from fastapi import FastAPI, UploadFile, File, APIRouter, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from typing import List, Optional
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="PharmAgent AI Service")

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """
    Handle validation errors and log details to the console for debugging.
    Specifically useful for diagnosing 422 Unprocessable Entity errors.
    """
    errors = exc.errors()
    logger.error(f"Validation error on {request.method} {request.url.path}: {errors}")
    return JSONResponse(
        status_code=422,
        content={"detail": errors},
    )

class PillScanResponse(BaseModel):
    pillId: Optional[str] = None
    name: Optional[str] = None
    genericName: Optional[str] = None
    brandName: Optional[str] = None
    strength: Optional[str] = None
    dosageForm: Optional[str] = None
    color: Optional[str] = None
    shape: Optional[str] = None
    description: Optional[str] = None
    usageInstructions: Optional[str] = None
    warning: Optional[str] = None
    sideEffects: Optional[str] = None
    manufacturer: Optional[str] = None
    confidenceScore: float = 0.0
    imageUrls: List[str] = Field(default_factory=list)

# Router with /api prefix
router = APIRouter(prefix="/api")

@router.get("/")
async def root():
    return {"message": "PharmAgent AI Service is running"}

@router.get("/health")
async def health():
    return {"status": "UP"}

@router.post("/pills/scan", response_model=PillScanResponse)
async def scan_pill(file: UploadFile = File(...)):
    """
    Scan a pill image and return identification results.
    Currently returns mock data to match existing Java implementation.
    """
    logger.info(f"Received scan request for file: {file.filename}")
    
    return PillScanResponse(
        pillId="mock-pill-id-123",
        name="Paracetamol",
        genericName="Acetaminophen",
        brandName="Hapacol 500",
        strength="500mg",
        dosageForm="Tablet",
        color="White",
        shape="Round",
        description="Thuốc giảm đau, hạ sốt phổ biến được sử dụng rộng rãi.",
        usageInstructions="Uống 1-2 viên mỗi 4-6 giờ khi cần thiết. Không vượt quá 4g mỗi ngày.",
        warning="Thận trọng với người có bệnh lý về gan hoặc uống nhiều rượu bia.",
        sideEffects="Dị ứng, phát ban, buồn nôn trong trường hợp hiếm gặp.",
        manufacturer="Dược Hậu Giang (DHG)",
        confidenceScore=0.98,
        imageUrls=["https://example.com/images/pills/paracetamol.jpg"]
    )

app.include_router(router)
