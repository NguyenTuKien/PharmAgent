import asyncio
import unittest

try:
    from fastapi import HTTPException
except ModuleNotFoundError:  # pragma: no cover - local env may not have gateway deps installed.
    HTTPException = None

if HTTPException is not None:
    from app.middleware import auth as auth_middleware


@unittest.skipIf(HTTPException is None, "gateway FastAPI dependencies are not installed")
class GatewayAuthDependencyTest(unittest.TestCase):
    def setUp(self):
        self.original_decode_token = auth_middleware.decode_token
        self.original_check_token_blacklist = auth_middleware.check_token_blacklist

    def tearDown(self):
        auth_middleware.decode_token = self.original_decode_token
        auth_middleware.check_token_blacklist = self.original_check_token_blacklist

    def test_current_user_rejects_refresh_token_on_protected_routes(self):
        auth_middleware.decode_token = lambda token: {
            "sub": "user-1",
            "type": "refresh",
        }

        async def fake_blacklist_check(token, payload):
            return None

        auth_middleware.check_token_blacklist = fake_blacklist_check

        with self.assertRaises(HTTPException) as error:
            asyncio.run(auth_middleware.get_current_user("Bearer refresh-token"))

        self.assertEqual(error.exception.status_code, 401)
        self.assertEqual(error.exception.detail, "Access token required")

    def test_current_user_rejects_auth_token_on_protected_routes(self):
        auth_middleware.decode_token = lambda token: {
            "sub": "user-1",
            "type": "auth",
        }

        async def fake_blacklist_check(token, payload):
            return None

        auth_middleware.check_token_blacklist = fake_blacklist_check

        with self.assertRaises(HTTPException) as error:
            asyncio.run(auth_middleware.get_current_user("Bearer auth-token"))

        self.assertEqual(error.exception.status_code, 401)
        self.assertEqual(error.exception.detail, "Access token required")

    def test_current_user_rejects_blacklisted_access_token(self):
        auth_middleware.decode_token = lambda token: {
            "sub": "user-1",
            "type": "access",
            "role": "CAREGIVER",
            "profileId": "profile-1",
        }

        async def fake_blacklist_check(token, payload):
            raise HTTPException(status_code=401, detail="Token has been revoked")

        auth_middleware.check_token_blacklist = fake_blacklist_check

        with self.assertRaises(HTTPException) as error:
            asyncio.run(auth_middleware.get_current_user("Bearer revoked-token"))

        self.assertEqual(error.exception.status_code, 401)
        self.assertEqual(error.exception.detail, "Token has been revoked")


if __name__ == "__main__":
    unittest.main()
