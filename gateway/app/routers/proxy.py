"""
Reverse Proxy core – forward HTTP requests & WebSocket connections tới upstream.
"""
import logging
from typing import Optional

import httpx
from fastapi import HTTPException, Request, status
from fastapi.responses import StreamingResponse

logger = logging.getLogger("gateway.proxy")

# Shared async HTTP client (connection pool)
_http_client: Optional[httpx.AsyncClient] = None


def get_http_client() -> httpx.AsyncClient:
    global _http_client
    if _http_client is None or _http_client.is_closed:
        _http_client = httpx.AsyncClient(
            timeout=httpx.Timeout(30.0, connect=5.0),
            follow_redirects=True,
            limits=httpx.Limits(max_connections=200, max_keepalive_connections=50),
        )
    return _http_client


async def close_http_client() -> None:
    global _http_client
    if _http_client and not _http_client.is_closed:
        await _http_client.aclose()


# Headers không được forward xuống upstream
_HOP_BY_HOP = {
    "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
    "te", "trailers", "transfer-encoding", "upgrade",
    "host",  # Sẽ được set lại bởi httpx
}


async def proxy_http_request(
    request: Request,
    upstream_url: str,
    strip_prefix: Optional[str] = None,
    extra_headers: Optional[dict] = None,
    strip_headers: Optional[set] = None,
    follow_redirects: bool = True,
) -> StreamingResponse:
    """
    Forward một HTTP request tới upstream_url.

    Args:
        request      : FastAPI Request gốc
        upstream_url : Base URL của upstream (vd: "http://backend:8080")
        strip_prefix : Prefix cần bỏ khỏi path trước khi gửi
        extra_headers: Headers bổ sung gửi tới upstream
        strip_headers: Tên header (lowercase) cần XÓA trước khi forward
                       (vd: {"authorization"} cho public endpoints)
    """
    path = request.url.path
    if strip_prefix and path.startswith(strip_prefix):
        path = path[len(strip_prefix):]
    if not path.startswith("/"):
        path = "/" + path

    query = request.url.query
    target_url = f"{upstream_url}{path}"
    if query:
        target_url = f"{target_url}?{query}"

    # Lọc headers hop-by-hop + headers cần strip, thêm X-Forwarded-For
    _strip = (_HOP_BY_HOP | {h.lower() for h in strip_headers}) if strip_headers else _HOP_BY_HOP
    forward_headers = {
        k: v
        for k, v in request.headers.items()
        if k.lower() not in _strip
    }
    client_ip = request.client.host if request.client else ""
    if client_ip:
        forward_headers["X-Forwarded-For"] = client_ip
    forward_headers["X-Forwarded-Proto"] = request.url.scheme
    forward_headers["X-Gateway"] = "pharmagent-gateway"

    if extra_headers:
        forward_headers.update(extra_headers)

    body = await request.body()
    client = get_http_client()

    try:
        upstream_request = client.build_request(
            method=request.method,
            url=target_url,
            headers=forward_headers,
            content=body,
        )
        upstream_response = await client.send(upstream_request, stream=True, follow_redirects=follow_redirects)
    except httpx.ConnectError as exc:
        logger.error("Upstream connection failed [%s]: %s", target_url, exc)
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Upstream service không khả dụng",
        )
    except httpx.TimeoutException:
        raise HTTPException(
            status_code=status.HTTP_504_GATEWAY_TIMEOUT,
            detail="Upstream service timeout",
        )

    # Lọc response headers
    response_headers = {
        k: v
        for k, v in upstream_response.headers.items()
        if k.lower() not in _HOP_BY_HOP
    }
    response_headers["X-Gateway"] = "pharmagent-gateway"

    async def iter_bytes():
        try:
            async for chunk in upstream_response.aiter_bytes():
                yield chunk
        finally:
            await upstream_response.aclose()

    return StreamingResponse(
        content=iter_bytes(),
        status_code=upstream_response.status_code,
        headers=response_headers,
        media_type=upstream_response.headers.get("content-type"),
    )
