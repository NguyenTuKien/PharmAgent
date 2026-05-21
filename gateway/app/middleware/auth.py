"""
Authentication & Authorization dependencies – dùng trong FastAPI Depends().

Cung cấp:
  - get_current_user   : yêu cầu access JWT hợp lệ
  - require_roles(...)  : yêu cầu có ít nhất 1 trong các role được chỉ định
  - optional_user      : lấy user nếu có token, không bắt buộc
"""
import logging
from typing import Annotated, Optional

from fastapi import Depends, Header, HTTPException, status
from pydantic import BaseModel

from app.utils.jwt_utils import decode_token, extract_bearer_token, get_roles, get_subject
from app.utils.redis_client import get_redis

logger = logging.getLogger("gateway.auth")


# ── Data Model ────────────────────────────────────────────────────────────────

class TokenUser(BaseModel):
    user_id: str
    roles: list[str]
    raw_payload: dict


def _require_access_token_payload(payload: dict) -> None:
    if payload.get("type") != "access":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Access token required",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not get_subject(payload):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token subject is missing",
            headers={"WWW-Authenticate": "Bearer"},
        )


# ── Blacklist Verification ────────────────────────────────────────────────────

async def check_token_blacklist(token: str, payload: dict) -> None:
    """ Kiểm tra xem token hoặc user có nằm trong danh sách blacklist ở Redis không. """
    try:
        redis = await get_redis()
        # 1. Kiểm tra blacklist cho từng token cụ thể
        if await redis.exists(f"blacklist:{token}"):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Token đã bị thu hồi (blacklist)",
                headers={"WWW-Authenticate": "Bearer"},
            )
        
        # 2. Kiểm tra xem toàn bộ các session của user có bị thu hồi không (revoke all)
        user_id = get_subject(payload)
        if user_id:
            blacklisted_at_str = await redis.get(f"blacklist_user:{user_id}")
            if blacklisted_at_str:
                blacklisted_at = int(blacklisted_at_str)
                iat = payload.get("iat", 0)
                # iat trong JWT là giây, blacklisted_at trong Redis là mili giây
                if iat * 1000 <= blacklisted_at:
                    raise HTTPException(
                        status_code=status.HTTP_401_UNAUTHORIZED,
                        detail="Phiên làm việc đã bị thu hồi",
                        headers={"WWW-Authenticate": "Bearer"},
                    )
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Lỗi khi kiểm tra token blacklist từ Redis: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Không thể xác minh trạng thái phiên",
        ) from exc


# ── Core dependency ───────────────────────────────────────────────────────────

async def require_access_payload(authorization: Optional[str]) -> TokenUser:
    token = extract_bearer_token(authorization)
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Thiếu token xác thực",
            headers={"WWW-Authenticate": "Bearer"},
        )

    payload = decode_token(token)
    _require_access_token_payload(payload)
    await check_token_blacklist(token, payload)
    return TokenUser(
        user_id=get_subject(payload),
        roles=get_roles(payload),
        raw_payload=payload,
    )


async def get_current_user(
    authorization: Annotated[Optional[str], Header()] = None,
) -> TokenUser:
    return await require_access_payload(authorization)


async def optional_user(
    authorization: Annotated[Optional[str], Header()] = None,
) -> Optional[TokenUser]:
    """
    FastAPI dependency: trả về TokenUser nếu có token hợp lệ, None nếu không.
    """
    token = extract_bearer_token(authorization)
    if not token:
        return None
    try:
        payload = decode_token(token)
        _require_access_token_payload(payload)
        await check_token_blacklist(token, payload)
        return TokenUser(
            user_id=get_subject(payload),
            roles=get_roles(payload),
            raw_payload=payload,
        )
    except HTTPException:
        return None


# ── RBAC factory ──────────────────────────────────────────────────────────────

def require_roles(*allowed_roles: str):
    """
    Factory tạo dependency kiểm tra role.

    Ví dụ:
        @router.get("/admin/users")
        async def list_users(user = Depends(require_roles("ADMIN"))):
            ...
    """
    async def _check(user: TokenUser = Depends(get_current_user)) -> TokenUser:
        for role in allowed_roles:
            if role in user.roles:
                return user
        logger.warning(
            "Access denied: user=%s roles=%s required=%s",
            user.user_id, user.roles, allowed_roles,
        )
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"Không có quyền truy cập. Yêu cầu role: {list(allowed_roles)}",
        )
    return _check


# ── Convenience aliases ───────────────────────────────────────────────────────
require_admin = require_roles("ADMIN")
require_caregiver = require_roles("CAREGIVER", "ADMIN")
require_elderly = require_roles("ELDERLY", "ADMIN")
