"""
Logging middleware – ghi log mọi request vào gateway (method, path, status, latency).
"""
import logging
import time

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

logger = logging.getLogger("gateway.access")


class AccessLogMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next) -> Response:
        # Bỏ qua WebSocket
        if request.scope.get("type") == "websocket":
            return await call_next(request)

        start = time.perf_counter()
        response = await call_next(request)
        latency_ms = (time.perf_counter() - start) * 1000

        client_ip = request.headers.get("X-Forwarded-For", request.client.host if request.client else "unknown")
        logger.info(
            "%s %s %s | %dms | %s",
            request.method,
            request.url.path,
            response.status_code,
            latency_ms,
            client_ip,
        )
        # Thêm header X-Response-Time vào response
        response.headers["X-Response-Time"] = f"{latency_ms:.2f}ms"
        return response
