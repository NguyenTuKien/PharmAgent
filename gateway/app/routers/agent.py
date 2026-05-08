"""
Agent Router – chỉ xử lý WebSocket /ws/agent (pill scan real-time).
Mọi route khác (/ws/**, /api/**) đều thuộc backend router.

QUAN TRỌNG: Router này phải include() TRƯỚC backend router trong main.py
để /ws/agent (cụ thể) được match trước /ws/{path} (catch-all).
"""
import asyncio
import logging

import websockets as ws_lib
from fastapi import APIRouter, Request, WebSocket, WebSocketDisconnect, status

from app.config import get_settings
from app.utils.jwt_utils import decode_token, extract_bearer_token

logger = logging.getLogger("gateway.agent")
settings = get_settings()

router = APIRouter(tags=["Agent"])

_AGENT     = settings.AGENT_URL
_AGENT_WS  = _AGENT.replace("http://", "ws://").replace("https://", "wss://")


# ── Health check (public) ─────────────────────────────────────────────────────

@router.get("/api/agent")
async def agent_health():
    """Kiểm tra trạng thái AI Agent – gọi nội bộ tới agent:8000/ws/health."""
    from app.routers.proxy import get_http_client
    client = get_http_client()
    try:
        resp = await client.get(f"{_AGENT}/ws/health", timeout=5.0)
        return resp.json()
    except Exception as exc:
        logger.warning("Agent health check failed: %s", exc)
        return {"status": "DOWN", "error": str(exc)}


def _extract_ws_token(websocket: WebSocket, query_token: str | None) -> str | None:
    auth_token = extract_bearer_token(websocket.headers.get("authorization", ""))
    if auth_token:
        return auth_token

    protocol_header = websocket.headers.get("sec-websocket-protocol", "")
    if protocol_header:
        protocols = [p.strip() for p in protocol_header.split(",") if p.strip()]
        for protocol in protocols:
            if protocol.startswith("bearer."):
                return protocol.split(".", 1)[1]
        if len(protocols) >= 2 and protocols[0].lower() == "bearer":
            return protocols[1]

    return query_token


@router.websocket("/ws/agent")
async def ws_pill_scan(
    websocket: WebSocket,
    token: str | None = None,
):
    """
    WebSocket tunnel tới Agent /ws/agent (nhận diện thuốc real-time).
    Authenticated: token qua query string.
    """
    raw_token = _extract_ws_token(websocket, token)
    if not raw_token:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return
    try:
        decode_token(raw_token)
    except Exception:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return

    await websocket.accept()
    upstream_url = f"{_AGENT_WS}/ws/agent"
    logger.info("WS tunnel: client ↔ gateway ↔ agent (%s)", upstream_url)

    try:
        async with ws_lib.connect(upstream_url) as upstream:

            async def to_upstream():
                try:
                    async for msg in websocket.iter_text():
                        await upstream.send(msg)
                except (WebSocketDisconnect, Exception):
                    pass

            async def to_client():
                try:
                    async for msg in upstream:
                        await websocket.send_text(msg)
                except Exception:
                    pass

            await asyncio.gather(to_upstream(), to_client(), return_exceptions=True)

    except Exception as exc:
        logger.error("WS proxy error: %s", exc)
    finally:
        try:
            await websocket.close()
        except Exception:
            pass
