"""
Backend Router – xử lý TẤT CẢ traffic trừ /ws/agent (của agent router).

Routing:
  /ws/**              → backend (STOMP WebSocket, public – auth qua STOMP interceptor)
  /api/auth/**        → backend (public)
  /api/admin/**       → backend (ADMIN only)
  /api/caregiver/**   → backend (CAREGIVER hoặc ADMIN)
  /api/elderly/**     → backend (ELDERLY hoặc ADMIN)
  /api/**             → backend (mọi user đã đăng nhập)
  /actuator/**        → backend (public, health check)
"""
import logging

from fastapi import APIRouter, Depends, Request

from app.config import get_settings
from app.middleware.auth import TokenUser, get_current_user, require_admin, require_caregiver, require_elderly
from app.routers.proxy import proxy_http_request

logger = logging.getLogger("gateway.backend")
settings = get_settings()

router = APIRouter(tags=["Backend"])
_BACKEND = settings.BACKEND_URL


# ── WebSocket STOMP (public catch-all) ────────────────────────────────────────

@router.api_route("/ws/{path:path}", methods=["GET", "POST", "OPTIONS"])
async def proxy_stomp(request: Request, path: str):
    """STOMP WebSocket + SockJS fallback → backend. Auth do STOMP interceptor xử lý."""
    return await proxy_http_request(request, _BACKEND)


# ── Auth (public) ─────────────────────────────────────────────────────────────

@router.api_route("/api/auth/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"])
async def proxy_auth(request: Request, path: str):
    return await proxy_http_request(request, _BACKEND)


# ── Admin ─────────────────────────────────────────────────────────────────────

@router.api_route("/api/admin/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"])
async def proxy_admin(request: Request, path: str, user: TokenUser = Depends(require_admin)):
    return await proxy_http_request(
        request, _BACKEND,
        extra_headers={"X-User-Id": user.user_id, "X-User-Roles": ",".join(user.roles)},
    )


# ── Caregiver ─────────────────────────────────────────────────────────────────

@router.api_route("/api/caregiver/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"])
async def proxy_caregiver(request: Request, path: str, user: TokenUser = Depends(require_caregiver)):
    return await proxy_http_request(
        request, _BACKEND,
        extra_headers={"X-User-Id": user.user_id, "X-User-Roles": ",".join(user.roles)},
    )


# ── Elderly ───────────────────────────────────────────────────────────────────

@router.api_route("/api/elderly/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"])
async def proxy_elderly(request: Request, path: str, user: TokenUser = Depends(require_elderly)):
    return await proxy_http_request(
        request, _BACKEND,
        extra_headers={"X-User-Id": user.user_id, "X-User-Roles": ",".join(user.roles)},
    )


# ── Generic API (authenticated) ───────────────────────────────────────────────

@router.api_route("/api/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"])
async def proxy_api(request: Request, path: str, user: TokenUser = Depends(get_current_user)):
    return await proxy_http_request(
        request, _BACKEND,
        extra_headers={"X-User-Id": user.user_id, "X-User-Roles": ",".join(user.roles)},
    )


# ── Actuator (public) ─────────────────────────────────────────────────────────

@router.api_route("/actuator/{path:path}", methods=["GET"])
async def proxy_actuator(request: Request, path: str):
    return await proxy_http_request(request, _BACKEND)
