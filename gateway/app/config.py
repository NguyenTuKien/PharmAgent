"""
Gateway configuration – loaded from environment variables.
"""
from functools import lru_cache
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # ── Application ────────────────────────────────────────────────────────────
    APP_NAME: str = "PharmAgent API Gateway"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False

    # ── JWT (phải giống backend Spring Boot) ──────────────────────────────────
    JWT_SECRET: str = "VGhpc0lzQURldlNlY3JldEtleUZvckpXVFNpZ25pbmdPbmx5ISE="
    JWT_ALGORITHM: str = "HS256"

    # ── Upstream services ──────────────────────────────────────────────────────
    BACKEND_URL: str = "http://backend:8080"
    AGENT_URL: str = "http://agent:8000"

    # ── Redis (rate-limiting) ──────────────────────────────────────────────────
    REDIS_URL: str = "redis://:hM3tdC4ETyAOXeoAtHjwKbHyTYJd6Wfq@redis:6379"

    # ── Rate limit defaults ────────────────────────────────────────────────────
    # Số request tối đa / cửa sổ thời gian (giây)
    RATE_LIMIT_PUBLIC_REQUESTS: int = 60
    RATE_LIMIT_PUBLIC_WINDOW: int = 60

    RATE_LIMIT_AUTH_REQUESTS: int = 200
    RATE_LIMIT_AUTH_WINDOW: int = 60

    RATE_LIMIT_ADMIN_REQUESTS: int = 500
    RATE_LIMIT_ADMIN_WINDOW: int = 60

    # ── CORS ───────────────────────────────────────────────────────────────────
    CORS_ORIGINS: str = "*"

    class Config:
        env_file = ".env"
        extra = "ignore"


@lru_cache()
def get_settings() -> Settings:
    return Settings()
