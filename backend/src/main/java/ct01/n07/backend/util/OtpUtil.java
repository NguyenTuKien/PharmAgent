package ct01.n07.backend.util;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class OtpUtil {

    private static final int OTP_LENGTH = 6;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp() {
        // Sinh ra mã 6 chữ số ngẫu nhiên từ 100000 đến 999999
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }
}
