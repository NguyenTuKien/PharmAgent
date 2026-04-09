package ct01.n07.backend.dto.auth;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@Builder
public class LoginResponse {
    private String authToken;
    private String refreshToken;
    private Page<UserProfileSummaryResponse> profiles;
}
