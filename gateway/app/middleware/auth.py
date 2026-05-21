"""
Authentication & Authorization dependencies – dùng trong FastAPI Depends().

Cung cấp:
  - get_current_user   : yêu cầu JWT hợp lệ
  - require_roles(...)  : yêu cầu có ít nhất 1 trong các role được chỉ định
  - optional_user      : lấy user nếu có token, không bắt buộc
"""
import logging
from typing import Annotated, Optional

from fastapi import Depends, Header, HTTPException, status
from pydantic import BaseModel

from app.utils.jwt_utils import decode_token, extract_bearer_token, get_roles, get_subject, is_token_blacklisted

logger = logging.getLogger("gateway.auth")


# ── Data Model ────────────────────────────────────────────────────────────────

class TokenUser(BaseModel):
    user_id: str
    roles: list[str]
    raw_payload: dict


# ── Core dependency ───────────────────────────────────────────────────────────

async def require_access_payload(authorization: Optional[str]) -> dict:
    token = extract_bearer_token(authorization)
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Thiếu token xác thực",
            headers={"WWW-Authenticate": "Bearer"},
        )

    payload = decode_token(token)
    if payload.get("type") != "access":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Access token required",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if await is_token_blacklisted(token):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has been revoked",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not get_subject(payload) or not get_roles(payload):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid access token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return payload

async def get_current_user(
    authorization: Annotated[Optional[str], Header()] = None,
) -> TokenUser:
    """
    FastAPI dependency: bắt buộc có JWT Bearer token hợp lệ.
    Sử dụng: user: TokenUser = Depends(get_current_user)
    """
    payload = await require_access_payload(authorization)
    return TokenUser(
        user_id=get_subject(payload),
        roles=get_roles(payload),
        raw_payload=payload,
    )


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
        payload = await require_access_payload(authorization)
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
