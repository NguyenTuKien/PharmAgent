package ct01.n07.backend.service;

import ct01.n07.backend.dto.auth.GoogleOAuthUserInfo;

public interface GoogleOAuthClient {
    String exchangeCodeForIdToken(String code, String redirectUri);

    GoogleOAuthUserInfo verifyIdToken(String idToken);
}
