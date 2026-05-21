package ct01.n07.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {
    @NotBlank(message = "Refresh Token không được để trống")
    @Size(max = 500, message = "Refresh Token không được vượt quá 500 ký tự")
    private String refreshToken;

    @Size(max = 50, message = "Profile ID không được vượt quá 50 ký tự")
    private String profileId;
}
