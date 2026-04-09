package ct01.n07.backend.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenRefreshResponse {
    private String authToken;
    private String accessToken;
    private String refreshToken;
}

