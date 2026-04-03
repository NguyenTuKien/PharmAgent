package ct01.web.backend.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenRefreshResponse {
    private String profileToken;
    private String refreshToken;
}

