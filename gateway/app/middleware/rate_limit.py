"""
Rate Limiting Middleware – Sliding Window algorithm via Redis.

Mỗi key = "rl:{tier}:{identifier}" (IP hoặc user-id).
Dùng Redis INCR + EXPIRE để đếm request trong cửa sổ thời gian.

Tiers:
  - public  : 60 req / 60s  (chưa đăng nhập)
  - auth    : 200 req / 60s (đã đăng nhập)
  - admin   : 500 req / 60s (ADMIN role)
"""
import logging
import time
from typing import Optional

from fastapi import status
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse, Response

from app.config import get_settings
from app.utils.jwt_utils import decode_token, extract_bearer_token, get_roles
from app.utils.redis_client import get_redis

logger = logging.getLogger("gateway.ratelimit")
settings = get_settings()

# Các path không cần rate limit chặt (auth endpoints)
_BYPASS_PATHS = {"/health", "/metrics", "/docs", "/openapi.json", "/redoc"}


def _get_tier_limits(roles: list[str]) -> tuple[int, int]:
    """Trả về (max_requests, window_seconds) theo role cao nhất."""
    if "ADMIN" in roles:
        return settings.RATE_LIMIT_ADMIN_REQUESTS, settings.RATE_LIMIT_ADMIN_WINDOW
    if roles:
        return settings.RATE_LIMIT_AUTH_REQUESTS, settings.RATE_LIMIT_AUTH_WINDOW
    return settings.RATE_LIMIT_PUBLIC_REQUESTS, settings.RATE_LIMIT_PUBLIC_WINDOW


def _get_identifier(request: Request) -> str:
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        return forwarded.split(",")[0].strip()
    return request.client.host if request.client else "unknown"


class RateLimitMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next) -> Response:
        # Bỏ qua WebSocket vì BaseHTTPMiddleware không hỗ trợ tốt
        if request.scope.get("type") == "websocket":
            return await call_next(request)

        if request.url.path in _BYPASS_PATHS:
            return await call_next(request)

        # Xác định tier từ JWT (nếu có) – không block nếu thiếu token ở đây
        roles: list[str] = []
        user_id: Optional[str] = None
        auth_header = request.headers.get("Authorization")
        token = extract_bearer_token(auth_header)
        if token:
            try:
                payload = decode_token(token)
                roles = get_roles(payload)
                user_id = payload.get("sub")
            except Exception:
                pass  # Sẽ bị chặn ở auth middleware sau

        max_req, window = _get_tier_limits(roles)
        tier = "admin" if "ADMIN" in roles else ("auth" if roles else "public")
        identifier = user_id or _get_identifier(request)
        redis_key = f"rl:{tier}:{identifier}"

        try:
            redis = await get_redis()
            pipe = redis.pipeline()
            now_window = int(time.time()) // window
            key_with_window = f"{redis_key}:{now_window}"

            pipe.incr(key_with_window)
            pipe.expire(key_with_window, window * 2)
            results = await pipe.execute()
            current_count = results[0]
        except Exception as exc:
            logger.warning("Redis rate-limit check failed, skipping: %s", exc)
            return await call_next(request)

        remaining = max(0, max_req - current_count)
        reset_at = (int(time.time()) // window + 1) * window

        if current_count > max_req:
            logger.warning("Rate limit exceeded: %s (tier=%s)", identifier, tier)
            return JSONResponse(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                content={
                    "error": "Too Many Requests",
                    "detail": f"Vượt quá giới hạn {max_req} request/{window}s. Thử lại sau.",
                    "retry_after": reset_at - int(time.time()),
                },
                headers={
                    "X-RateLimit-Limit": str(max_req),
                    "X-RateLimit-Remaining": "0",
                    "X-RateLimit-Reset": str(reset_at),
                    "Retry-After": str(reset_at - int(time.time())),
                },
            )

        response = await call_next(request)
        response.headers["X-RateLimit-Limit"] = str(max_req)
        response.headers["X-RateLimit-Remaining"] = str(remaining)
        response.headers["X-RateLimit-Reset"] = str(reset_at)
        return response
