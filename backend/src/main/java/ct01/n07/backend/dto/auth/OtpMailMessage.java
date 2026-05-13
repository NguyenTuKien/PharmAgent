package ct01.n07.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpMailMessage implements Serializable {
    private String email;
    private String otpCode;
    private String purpose;
    private String actionUrl;

    public OtpMailMessage(String email, String otpCode) {
        this(email, otpCode, "PASSWORD_RESET", null);
    }
}
