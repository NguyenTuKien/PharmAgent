package ct01.n07.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    @Size(max = 500, message = "Access Token không được vượt quá 500 ký tự")
    private String accessToken;

    @Size(max = 500, message = "Profile Token không được vượt quá 500 ký tự")
    private String profileToken;

    @NotBlank(message = "Refresh Token không được để trống")
    @Size(max = 500, message = "Refresh Token không được vượt quá 500 ký tự")
    private String refreshToken;
}
