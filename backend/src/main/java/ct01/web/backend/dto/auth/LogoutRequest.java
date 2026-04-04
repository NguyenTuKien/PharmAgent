package ct01.web.backend.dto.auth;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    private String accessToken;
    private String profileToken;
    private String refreshToken;
}
