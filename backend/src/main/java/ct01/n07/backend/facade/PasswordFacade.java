package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.ResetPasswordRequest;
import ct01.n07.backend.producer.MailProducerService;
import ct01.n07.backend.service.UserService;
import ct01.n07.backend.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordFacade {

    private final UserService userService;
    private final OtpUtil otpUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final MailProducerService mailProducerService;

    public void processForgotPassword(String email) {
        // Bước 1: Kiểm tra xem email có tồn tại trong MongoDB không
        try {
            userService.findByEmail(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại");
        }

        // Bước 2: Sinh mã OTP
        String otpCode = otpUtil.generateOtp();

        // Bước 3: Lưu vào Redis với Key là "OTP:email", Value là mã OTP, TTL 5 phút
        String redisKey = "OTP:" + email;
        redisTemplate.opsForValue().set(redisKey, otpCode, Duration.ofMinutes(5));

        // Bước 4: Đẩy nhiệm vụ gửi mail vào RabbitMQ
        mailProducerService.sendOtpToQueue(email, otpCode);
    }

    public boolean verifyOtp(String email, String userProvidedOtp) {
        String redisKey = "OTP:" + email;

        // Lấy OTP từ Redis ra
        String savedOtp = redisTemplate.opsForValue().get(redisKey);

        if (savedOtp != null && savedOtp.equals(userProvidedOtp)) {
            // Xác thực thành công -> Xóa OTP khỏi Redis
            redisTemplate.delete(redisKey);
            return true;
        }
        return false;
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        boolean isOtpValid = verifyOtp(request.getEmail(), request.getOtp());
        if (!isOtpValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP không hợp lệ hoặc đã hết hạn");
        }

        userService.updatePassword(request.getEmail(), request.getNewPassword());
    }
}
