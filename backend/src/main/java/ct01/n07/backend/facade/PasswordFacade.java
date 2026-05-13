package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.ChangePasswordRequest;
import ct01.n07.backend.dto.auth.ResetPasswordRequest;
import ct01.n07.backend.model.User;
import ct01.n07.backend.producer.MailProducerService;
import ct01.n07.backend.service.UserService;
import ct01.n07.backend.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordFacade {

    private static final String PASSWORD_RESET_PREFIX = "PASSWORD_RESET:";
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(5);

    private final UserService userService;
    private final OtpUtil otpUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final MailProducerService mailProducerService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url:${FRONTEND_URL:http://localhost:5173}}")
    private String frontendUrl;

    public void processForgotPassword(String email) {
        String normalizedEmail = normalizeEmail(email);

        try {
            userService.findByEmail(normalizedEmail);
        } catch (ResponseStatusException e) {
            log.info("Bỏ qua yêu cầu quên mật khẩu cho email không tồn tại");
            return;
        }

        String otpCode = otpUtil.generateOtp();

        String redisKey = PASSWORD_RESET_PREFIX + normalizedEmail;
        redisTemplate.opsForValue().set(redisKey, otpCode, PASSWORD_RESET_TTL);

        mailProducerService.sendOtpToQueue(
                normalizedEmail,
                otpCode,
                "PASSWORD_RESET",
                buildResetUrl(normalizedEmail));
    }

    public boolean verifyOtp(String email, String userProvidedOtp) {
        String redisKey = PASSWORD_RESET_PREFIX + normalizeEmail(email);

        String savedOtp = redisTemplate.opsForValue().get(redisKey);

        if (savedOtp != null && savedOtp.equals(userProvidedOtp)) {
            redisTemplate.delete(redisKey);
            return true;
        }
        return false;
    }

    public void resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        boolean isOtpValid = verifyOtp(email, request.getOtp());
        if (!isOtpValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP không hợp lệ hoặc đã hết hạn");
        }

        userService.updatePassword(email, request.getNewPassword());
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        User user = userService.findById(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }

        userService.updatePassword(user.getEmail(), request.getNewPassword());
    }

    private String buildResetUrl(String email) {
        String normalizedBase = frontendUrl.replaceAll("/+$", "");
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        return normalizedBase + "/reset-password?email=" + encodedEmail;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
