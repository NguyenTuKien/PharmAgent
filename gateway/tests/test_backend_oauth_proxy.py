import unittest

try:
    from fastapi import FastAPI, Response
    from fastapi.testclient import TestClient
except ModuleNotFoundError:  # pragma: no cover - local env may not have gateway deps installed.
    FastAPI = None
    Response = None
    TestClient = None

if FastAPI is not None:
    from app.routers import backend as backend_router


@unittest.skipIf(FastAPI is None, "gateway FastAPI dependencies are not installed")
class BackendOAuthProxyTest(unittest.TestCase):
    def setUp(self):
        self.calls = []
        self.original_proxy = backend_router.proxy_http_request

        async def fake_proxy_http_request(request, upstream_url, **kwargs):
            self.calls.append(
                {
                    "path": request.url.path,
                    "upstream_url": upstream_url,
                    "follow_redirects": kwargs.get("follow_redirects", True),
                }
            )
            return Response(status_code=302, headers={"Location": "https://accounts.google.com/o/oauth2/v2/auth"})

        backend_router.proxy_http_request = fake_proxy_http_request
        app = FastAPI()
        app.include_router(backend_router.router)
        self.client = TestClient(app)

    def tearDown(self):
        backend_router.proxy_http_request = self.original_proxy

    def test_google_oauth_routes_do_not_follow_redirects_inside_gateway(self):
        response = self.client.get("/api/auth/oauth/google/start", follow_redirects=False)

        self.assertEqual(response.status_code, 302)
        self.assertEqual(response.headers["location"], "https://accounts.google.com/o/oauth2/v2/auth")
        self.assertEqual(self.calls[0]["path"], "/api/auth/oauth/google/start")
        self.assertFalse(self.calls[0]["follow_redirects"])


if __name__ == "__main__":
    unittest.main()
