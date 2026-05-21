package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.ChangePasswordRequest;
import ct01.n07.backend.dto.auth.OtpMailMessage;
import ct01.n07.backend.dto.auth.ResetPasswordRequest;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.producer.MailProducerService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.service.UserService;
import ct01.n07.backend.util.ResetTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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
    private static final String LEGACY_APP_NAME_PLACEHOLDER = "PharmAgent";
    private static final Duration PASSWORD_RESET_TTL = Duration.ofHours(12);

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final ResetTokenUtil resetTokenUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final MailProducerService mailProducerService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url:${APP_FRONTEND_URL:${FRONTEND_URL:http://localhost:5173}}}")
    private String frontendUrl;

    public void processForgotPassword(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userService.findByEmail(normalizedEmail);

        String resetToken = resetTokenUtil.generateUrlToken();

        String redisKey = PASSWORD_RESET_PREFIX + normalizedEmail;
        redisTemplate.opsForValue().set(redisKey, resetToken, PASSWORD_RESET_TTL);

        mailProducerService.sendOtpToQueue(
                normalizedEmail,
                resetToken,
                OtpMailMessage.PASSWORD_RESET,
                buildResetUrl(normalizedEmail, resetToken),
                resolveRecipientName(user));
    }

    public boolean verifyResetToken(String email, String userProvidedToken) {
        String redisKey = PASSWORD_RESET_PREFIX + normalizeEmail(email);

        String savedToken = redisTemplate.opsForValue().get(redisKey);

        if (savedToken != null && savedToken.equals(userProvidedToken)) {
            redisTemplate.delete(redisKey);
            return true;
        }
        return false;
    }

    public void resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        String resetToken = normalizeResetToken(request);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        if (!verifyResetToken(email, resetToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
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

    private String buildResetUrl(String email, String resetToken) {
        String normalizedBase = frontendUrl.replaceAll("/+$", "");
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedToken = URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
        return normalizedBase + "/reset-password?email=" + encodedEmail + "&token=" + encodedToken;
    }

    private String normalizeResetToken(ResetPasswordRequest request) {
        String token = request.getToken();
        if (!hasText(token)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        }
        return token.trim();
    }

    private String resolveRecipientName(User user) {
        if (user == null || !hasText(user.getId())) {
            return user == null ? null : user.getEmail();
        }

        try {
            return userProfileService.findAllByUserId(user.getId(), PageRequest.of(0, 1))
                    .getContent()
                    .stream()
                    .findFirst()
                    .map(profile -> displayName(profile, user.getEmail()))
                    .orElse(user.getEmail());
        } catch (RuntimeException ex) {
            log.warn("Không lấy được tên người nhận email reset cho user [{}]", user.getId());
            return user.getEmail();
        }
    }

    private String displayName(UserProfile profile, String fallback) {
        if (profile == null) {
            return fallback;
        }
        String fullName = (cleanProfileNamePart(profile.getFirstName())
                + " "
                + cleanProfileNamePart(profile.getLastName())).trim();
        return hasText(fullName) ? fullName : fallback;
    }

    private String cleanProfileNamePart(String value) {
        String normalized = value == null ? "" : value.trim();
        return LEGACY_APP_NAME_PLACEHOLDER.equalsIgnoreCase(normalized) ? "" : normalized;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
