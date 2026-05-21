package ct01.n07.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthCodeExchangeRequest {
    @NotBlank(message = "OAuth code không được để trống")
    private String code;
}
