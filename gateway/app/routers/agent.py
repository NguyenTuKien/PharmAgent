"""
Agent Router – chỉ xử lý WebSocket /ws/agent (pill scan real-time).
Mọi route khác (/ws/**, /api/**) đều thuộc backend router.

QUAN TRỌNG: Router này phải include() TRƯỚC backend router trong main.py
để /ws/agent (cụ thể) được match trước /ws/{path} (catch-all).
"""
import logging

from fastapi import APIRouter, HTTPException, Request, status, Depends

from app.config import get_settings
from app.utils.jwt_utils import decode_token, extract_bearer_token
from app.middleware.auth import TokenUser, require_admin, get_current_user

logger = logging.getLogger("gateway.agent")
settings = get_settings()

router = APIRouter(tags=["Agent"])

_AGENT     = settings.AGENT_URL


# ── Health check (public) ─────────────────────────────────────────────────────

@router.get("/api/agent")
async def agent_health():
    """Kiểm tra trạng thái AI Agent – gọi nội bộ tới agent:8000/ws/health."""
    from app.routers.proxy import get_http_client
    client = get_http_client()
    try:
        resp = await client.get(f"{_AGENT}/health", timeout=5.0)
        return resp.json()
    except Exception as exc:
        logger.warning("Agent health check failed: %s", exc)
        return {"status": "DOWN", "error": str(exc)}


def _require_http_token(request: Request) -> None:
    raw_token = extract_bearer_token(request.headers.get("authorization", ""))
    if not raw_token:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing token")
    try:
        decode_token(raw_token)
    except Exception as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token") from exc


async def _proxy_agent_http(request: Request):
    from app.routers.proxy import proxy_http_request

    _require_http_token(request)
    return await proxy_http_request(request, _AGENT, strip_prefix="/api/agent")


@router.post("/api/agent/analyze")
async def agent_analyze(request: Request):
    """Upload ảnh thuốc lên Agent /analyze để OCR + match database."""
    return await _proxy_agent_http(request)


@router.post("/api/agent/search")
async def agent_search(request: Request):
    """Nhận text người dùng nhập và tìm thuốc trong database, độc lập với scan ảnh."""
    return await _proxy_agent_http(request)


# ── Proxy Pills CRUD to Agent ─────────────────────────────────────────────────

@router.api_route("/api/pills", methods=["GET", "OPTIONS"])
@router.api_route("/api/pills/{path:path}", methods=["GET", "OPTIONS"])
async def proxy_pills(request: Request, user: TokenUser = Depends(get_current_user), path: str = ""):
    """Proxy quản lý danh mục thuốc (chỉ đọc) tới Agent."""
    from app.routers.proxy import proxy_http_request
    return await proxy_http_request(request, _AGENT, strip_prefix="/api")


@router.api_route("/api/admin/pills", methods=["POST", "PUT", "DELETE", "PATCH", "OPTIONS"])
@router.api_route("/api/admin/pills/{path:path}", methods=["POST", "PUT", "DELETE", "PATCH", "OPTIONS"])
async def proxy_admin_pills(request: Request, user: TokenUser = Depends(require_admin), path: str = ""):
    """Proxy quản lý danh mục thuốc (CRUD) tới Agent."""
    from app.routers.proxy import proxy_http_request
    return await proxy_http_request(request, _AGENT, strip_prefix="/api")
