"""
JWT utilities – decode & validate tokens sinh ra bởi Spring Boot backend.
"""
import base64
import logging
from typing import Optional

import jwt
from fastapi import HTTPException, status

from app.config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()


def _get_signing_key() -> bytes:
    """Decode base64 JWT secret (Spring Boot / jjwt convention)."""
    return base64.b64decode(settings.JWT_SECRET)


def decode_token(token: str) -> dict:
    """Decode & verify JWT. Raises 401 nếu token không hợp lệ / hết hạn."""
    try:
        payload = jwt.decode(
            token,
            _get_signing_key(),
            algorithms=[settings.JWT_ALGORITHM],
        )
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token đã hết hạn",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except jwt.InvalidTokenError as exc:
        logger.warning("Invalid JWT: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token không hợp lệ",
            headers={"WWW-Authenticate": "Bearer"},
        )


def extract_bearer_token(authorization: Optional[str]) -> Optional[str]:
    """Tách token từ header 'Authorization: Bearer <token>'."""
    if not authorization:
        return None
    parts = authorization.split()
    if len(parts) == 2 and parts[0].lower() == "bearer":
        return parts[1]
    return None


def get_roles(payload: dict) -> list[str]:
    """
    Lấy danh sách role từ payload.
    Spring Security thường lưu dạng: roles: ["ROLE_ADMIN"] hoặc authorities.
    """
    raw = payload.get("roles", payload.get("authorities", payload.get("role", [])))
    if isinstance(raw, str):
        raw = [raw]
    return [r.replace("ROLE_", "") for r in raw]


def get_subject(payload: dict) -> str:
    return payload.get("sub", "")
