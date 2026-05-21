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
        self.original_is_token_blacklisted = getattr(auth_middleware, "is_token_blacklisted", None)

    def tearDown(self):
        auth_middleware.decode_token = self.original_decode_token
        if self.original_is_token_blacklisted is None:
            try:
                delattr(auth_middleware, "is_token_blacklisted")
            except AttributeError:
                pass
        else:
            auth_middleware.is_token_blacklisted = self.original_is_token_blacklisted

    def test_current_user_rejects_refresh_token_on_protected_routes(self):
        auth_middleware.decode_token = lambda token: {
            "sub": "user-1",
            "type": "refresh",
        }

        async def fake_blacklist_check(token):
            return False

        auth_middleware.is_token_blacklisted = fake_blacklist_check

        with self.assertRaises(HTTPException) as error:
            asyncio.run(auth_middleware.get_current_user("Bearer refresh-token"))

        self.assertEqual(error.exception.status_code, 401)
        self.assertEqual(error.exception.detail, "Access token required")

    def test_current_user_rejects_blacklisted_access_token(self):
        auth_middleware.decode_token = lambda token: {
            "sub": "user-1",
            "type": "access",
            "role": "CAREGIVER",
            "profileId": "profile-1",
        }

        async def fake_blacklist_check(token):
            return True

        auth_middleware.is_token_blacklisted = fake_blacklist_check

        with self.assertRaises(HTTPException) as error:
            asyncio.run(auth_middleware.get_current_user("Bearer revoked-token"))

        self.assertEqual(error.exception.status_code, 401)
        self.assertEqual(error.exception.detail, "Token has been revoked")


if __name__ == "__main__":
    unittest.main()
