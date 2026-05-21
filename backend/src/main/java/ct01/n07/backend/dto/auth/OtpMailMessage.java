package ct01.n07.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpMailMessage implements Serializable {
    public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";

    private String email;
    private String otpCode;
    private String purpose;
    private String actionUrl;
    private String recipientName;

    public OtpMailMessage(String email, String otpCode) {
        this(email, otpCode, PASSWORD_RESET, null, null);
    }

    public OtpMailMessage(String email, String otpCode, String purpose, String actionUrl) {
        this(email, otpCode, purpose, actionUrl, null);
    }
}
