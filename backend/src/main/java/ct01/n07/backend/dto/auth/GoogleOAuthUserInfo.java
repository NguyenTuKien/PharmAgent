package ct01.n07.backend.dto.auth;

public record GoogleOAuthUserInfo(
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String picture,
        String nonce
) {
}
