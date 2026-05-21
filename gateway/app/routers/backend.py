"""
Backend Router – xử lý TẤT CẢ traffic trừ /ws/agent (của agent router).

Routing:
  /ws/**              → backend (STOMP WebSocket, public – auth qua STOMP interceptor)
  /api/auth/**        → backend (public)
  /api/admin/**       → backend (ADMIN only)
  /api/caregiver/**   → backend (CAREGIVER, theo backend security config hiện tại)
  /api/elderly/**     → backend (ELDERLY, theo backend security config hiện tại)
  /api/**             → backend (mọi user đã đăng nhập)
  /actuator/**        → backend (public, health check)
"""
import asyncio
import logging

import websockets as ws_lib
from fastapi import APIRouter, Depends, Request, WebSocket, WebSocketDisconnect

from app.config import get_settings
from app.middleware.auth import TokenUser, get_current_user, require_admin, require_caregiver, require_elderly
from app.routers.proxy import proxy_http_request

logger = logging.getLogger("gateway.backend")
settings = get_settings()

router = APIRouter(tags=["Backend"])
_BACKEND = settings.BACKEND_URL
_BACKEND_WS = _BACKEND.replace("http://", "ws://").replace("https://", "wss://")


# ── WebSocket STOMP (public catch-all) ────────────────────────────────────────

def _ws_target(path: str = "") -> str:
    target = f"{_BACKEND_WS}/ws"
    if path:
        target = f"{target}/{path.lstrip('/')}"
    return target


@router.api_route("/ws", methods=["GET", "POST", "OPTIONS"])
async def proxy_stomp_root(request: Request):
    """STOMP HTTP fallback (/ws) → backend."""
    return await proxy_http_request(request, _BACKEND)


@router.api_route("/ws/{path:path}", methods=["GET", "POST", "OPTIONS"])
async def proxy_stomp_http(request: Request, path: str):
    """STOMP WebSocket + SockJS fallback → backend. Auth do STOMP interceptor xử lý."""
    return await proxy_http_request(request, _BACKEND)


@router.websocket("/ws")
async def proxy_stomp_ws_root(websocket: WebSocket):
    await _proxy_stomp_ws(websocket)


@router.websocket("/ws/{path:path}")
async def proxy_stomp_ws(websocket: WebSocket, path: str):
    await _proxy_stomp_ws(websocket, path)


async def _proxy_stomp_ws(websocket: WebSocket, path: str = ""):
    await websocket.accept()
    upstream_url = _ws_target(path)
    if websocket.url.query:
        upstream_url = f"{upstream_url}?{websocket.url.query}"

    try:
        async with ws_lib.connect(upstream_url) as upstream:
            async def to_upstream():
                try:
                    async for msg in websocket.iter_text():
                        await upstream.send(msg)
                except (WebSocketDisconnect, ws_lib.ConnectionClosed):
                    logger.debug("Client websocket closed while proxying to upstream: %s", upstream_url)

            async def to_client():
                try:
                    async for msg in upstream:
                        if isinstance(msg, bytes):
                            await websocket.send_bytes(msg)
                        else:
                            await websocket.send_text(msg)
                except (WebSocketDisconnect, ws_lib.ConnectionClosed):
                    logger.debug("Upstream websocket closed while proxying to client: %s", upstream_url)

            await asyncio.gather(to_upstream(), to_client(), return_exceptions=True)
    except Exception as exc:
        logger.error("Backend WS proxy error for %s: %s", upstream_url, exc)
    finally:
        try:
            await websocket.close()
        except Exception:
            pass


# ── Auth (public) ─────────────────────────────────────────────────────────────

@router.api_route("/api/auth/oauth/google/{path:path}", methods=["GET", "POST", "OPTIONS"])
async def proxy_google_oauth(request: Request, path: str):
    return await proxy_http_request(request, _BACKEND, follow_redirects=False)


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


# ── Actuator (public) ─────────────────────────────────────────────────────────

@router.api_route("/api/actuator/{path:path}", methods=["GET"])
async def proxy_api_actuator(request: Request, path: str):
    """Actuator accessible via /api/actuator/* (strip /api prefix)"""
    return await proxy_http_request(request, _BACKEND, strip_prefix="/api")


# ── Generic API (authenticated) ───────────────────────────────────────────────

@router.api_route("/api/{path:path}", methods=["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"])
async def proxy_api(request: Request, path: str, user: TokenUser = Depends(get_current_user)):
    return await proxy_http_request(
        request, _BACKEND,
        extra_headers={"X-User-Id": user.user_id, "X-User-Roles": ",".join(user.roles)},
    )


@router.api_route("/actuator/{path:path}", methods=["GET"])
async def proxy_actuator(request: Request, path: str):
    return await proxy_http_request(request, _BACKEND)
