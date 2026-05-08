"""
PharmAgent API Gateway – FastAPI entry point.

Luồng xử lý mỗi request:
  AccessLogMiddleware → RateLimitMiddleware → Router → Auth Dep → Proxy → Upstream
"""
import logging
import logging.config

from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import get_settings
from app.middleware.logging import AccessLogMiddleware
from app.middleware.rate_limit import RateLimitMiddleware
from app.routers import agent, backend
from app.routers.proxy import close_http_client
from app.utils.redis_client import close_redis

settings = get_settings()

# ── Logging setup ─────────────────────────────────────────────────────────────
logging.config.dictConfig({
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "default": {
            "format": "%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
            "datefmt": "%Y-%m-%d %H:%M:%S",
        }
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "default",
        }
    },
    "root": {"level": "INFO", "handlers": ["console"]},
    "loggers": {
        "gateway": {"level": "INFO", "propagate": True},
        "uvicorn": {"level": "INFO", "propagate": False, "handlers": ["console"]},
        "uvicorn.access": {"level": "WARNING", "propagate": False},  # Tránh log trùng
    },
})

logger = logging.getLogger("gateway")


# ── Lifespan (startup / shutdown) ─────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("🚀 %s v%s starting...", settings.APP_NAME, settings.APP_VERSION)
    yield
    logger.info("🛑 Shutting down gateway...")
    await close_http_client()
    await close_redis()


# ── App factory ───────────────────────────────────────────────────────────────
app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description="""
## PharmAgent API Gateway

Central entry point cho toàn bộ hệ thống PharmAgent.

### Tính năng
- **Xác thực** – JWT Bearer token (HMAC-SHA256, tương thích Spring Boot)
- **Phân quyền** – Role-based: `ADMIN`, `CAREGIVER`, `ELDERLY`
- **Rate Limiting** – Sliding window qua Redis (60/200/500 req/min)
- **Proxy HTTP** – httpx async, connection pooling
- **Proxy WebSocket** – Tunnel hai chiều tới Agent

### Upstream services
| Service  | URL nội bộ         | Prefix gateway |
|----------|--------------------|----------------|
| Backend  | `backend:8080`     | `/api/**`      |
| Agent    | `agent:8000`       | `/agent/**`    |
    """,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
    lifespan=lifespan,
)


# ── Middleware stack (thứ tự: ngoài → trong) ──────────────────────────────────

# 1. CORS
origins = [o.strip() for o in settings.CORS_ORIGINS.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins if origins != ["*"] else ["*"],
    allow_origin_regex=r".*" if origins == ["*"] else None,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset", "X-Response-Time"],
)

# 2. Rate Limit
app.add_middleware(RateLimitMiddleware)

# 3. Access Log
app.add_middleware(AccessLogMiddleware)


# ── Routers ───────────────────────────────────────────────────────────────────
# Thứ tự include quan trọng:
# 1. agent  → /ws/agent (cụ thể, phải match trước)
# 2. backend → /ws/{path} (catch-all STOMP, phải đến sau)
app.include_router(agent.router)
app.include_router(backend.router)


# ── Gateway-level endpoints ───────────────────────────────────────────────────

@app.get("/health", tags=["Gateway"])
async def gateway_health():
    """Health check của chính gateway."""
    return {
        "status": "UP",
        "service": settings.APP_NAME,
        "version": settings.APP_VERSION,
    }


@app.get("/routes", tags=["Gateway"])
async def list_routes():
    """Liệt kê toàn bộ route đã đăng ký (dùng khi debug)."""
    return [
        {"path": route.path, "methods": list(getattr(route, "methods", []))}
        for route in app.routes
    ]


# ── Global exception handler ──────────────────────────────────────────────────

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.exception("Unhandled error on %s %s", request.method, request.url.path)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"error": "Internal Gateway Error", "detail": str(exc)},
    )
