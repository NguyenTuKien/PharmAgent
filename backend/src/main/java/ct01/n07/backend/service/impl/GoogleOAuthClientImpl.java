package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.auth.GoogleOAuthUserInfo;
import ct01.n07.backend.service.GoogleOAuthClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class GoogleOAuthClientImpl implements GoogleOAuthClient {

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    @Value("${GOOGLE_CLIENT_ID:${google.client-id:}}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET:${google.client-secret:}}")
    private String clientSecret;

    @Override
    public String exchangeCodeForIdToken(String code, String redirectUri) {
        requireConfigured();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        try {
            Map<?, ?> response = restClient.post()
                    .uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Object rawIdToken = response == null ? null : response.get("id_token");
            String idToken = rawIdToken == null ? "" : String.valueOf(rawIdToken);
            if (!hasText(idToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google OAuth token response is invalid");
            }

            return idToken;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không thể xác thực mã Google OAuth");
        }
    }

    @Override
    public GoogleOAuthUserInfo verifyIdToken(String idToken) {
        requireConfigured();

        try {
            JsonNode tokenInfo = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("oauth2.googleapis.com")
                            .path("/tokeninfo")
                            .queryParam("id_token", idToken)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode claims = readJwtPayload(idToken);
            String audience = firstText(tokenInfo, claims, "aud");
            String issuer = firstText(tokenInfo, claims, "iss");
            String subject = firstText(tokenInfo, claims, "sub");
            String email = firstText(tokenInfo, claims, "email");
            String name = firstText(tokenInfo, claims, "name");
            String picture = firstText(tokenInfo, claims, "picture");
            String nonce = text(claims, "nonce");
            boolean emailVerified = booleanValue(tokenInfo, claims, "email_verified");
            long exp = longValue(tokenInfo, claims, "exp");

            if (!clientId.equals(audience)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google OAuth audience không hợp lệ");
            }

            if (!"accounts.google.com".equals(issuer) && !"https://accounts.google.com".equals(issuer)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google OAuth issuer không hợp lệ");
            }

            if (exp <= Instant.now().getEpochSecond()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google OAuth token đã hết hạn");
            }

            if (!hasText(subject) || !hasText(email) || !emailVerified) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google OAuth email chưa được xác minh");
            }

            return new GoogleOAuthUserInfo(
                    subject,
                    email.trim().toLowerCase(),
                    true,
                    name,
                    picture,
                    nonce);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google OAuth token không hợp lệ");
        }
    }

    private void requireConfigured() {
        if (!hasText(clientId) || !hasText(clientSecret)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google OAuth chưa được cấu hình");
        }
    }

    private JsonNode readJwtPayload(String idToken) throws Exception {
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT");
        }
        byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
        return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
    }

    private String firstText(JsonNode primary, JsonNode fallback, String field) {
        String value = text(primary, field);
        return hasText(value) ? value : text(fallback, field);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText("");
    }

    private boolean booleanValue(JsonNode primary, JsonNode fallback, String field) {
        JsonNode value = primary != null && primary.has(field) ? primary.get(field) : fallback.get(field);
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        return "true".equalsIgnoreCase(value.asText());
    }

    private long longValue(JsonNode primary, JsonNode fallback, String field) {
        JsonNode value = primary != null && primary.has(field) ? primary.get(field) : fallback.get(field);
        if (value == null || value.isNull()) {
            return 0L;
        }
        return value.asLong(0L);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
