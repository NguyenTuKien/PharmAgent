package ct01.web.backend.dto.auth;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Page<UserProfileSummaryResponse> profiles;
}
