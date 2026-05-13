package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.AuthMessageResponse;
import ct01.n07.backend.dto.auth.LoginRequest;
import ct01.n07.backend.dto.auth.SignupRequest;
import ct01.n07.backend.dto.auth.VerifyEmailRequest;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.UserStatus;
import ct01.n07.backend.producer.MailProducerService;
import ct01.n07.backend.service.RelationshipService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.service.UserService;
import ct01.n07.backend.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationFacade {

    private static final String VERIFY_EMAIL_PREFIX = "VERIFY_EMAIL:";
    private static final Duration VERIFY_EMAIL_TTL = Duration.ofMinutes(15);

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final RelationshipService relationshipService;
    private final UserProfileMapper userProfileMapper;
    private final OtpUtil otpUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final MailProducerService mailProducerService;

    @Value("${app.frontend-url:${FRONTEND_URL:http://localhost:5173}}")
    private String frontendUrl;

    @Transactional
    public AuthMessageResponse signup(SignupRequest signupRequest) {
        signupRequest.setEmail(normalizeEmail(signupRequest.getEmail()));

        if (!signupRequest.getPassword().equals(signupRequest.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu và xác nhận mật khẩu không khớp");
        }

        LoginRequest loginRequest = LoginRequest.builder()
                .email(signupRequest.getEmail())
                .password(signupRequest.getPassword())
                .build();

        if (signupRequest.getCaregiver() != null && userProfileService.findByPhone(signupRequest.getCaregiver().getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caregiver phone number already exists");
        }

        if (signupRequest.getElderly() != null && !signupRequest.getElderly().getPhone().isBlank()) {
            if (userProfileService.findByPhone(signupRequest.getElderly().getPhone())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elderly phone number already exists");
            }
        }

        User user = userService.createUser(loginRequest, UserStatus.INACTIVE);

        UserProfile caregiverProfile = userProfileMapper.toCaregiverProfile(signupRequest, user.getId());
        userProfileService.saveUserProfile(caregiverProfile);

        if (signupRequest.getElderly() != null) {
            UserProfile elderlyProfile = userProfileMapper.toElderlyProfile(signupRequest, user.getId());
            userProfileService.saveUserProfile(elderlyProfile);
            relationshipService.createRelationship(
                    caregiverProfile.getId(),
                    elderlyProfile.getId(),
                    signupRequest.getElderly().getCaregiverTitle(),
                    signupRequest.getElderly().getElderlyTitle(),
                    signupRequest.getElderly().getPermissionLevel()
            );
        }

        sendVerificationEmail(user.getEmail());

        return AuthMessageResponse.builder()
                .email(user.getEmail())
                .message("Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản.")
                .build();
    }

    public AuthMessageResponse verifyEmail(VerifyEmailRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userService.findByEmail(email);

        if (user.getUserStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        if (user.getUserStatus() == UserStatus.ACTIVE) {
            return AuthMessageResponse.builder()
                    .email(email)
                    .message("Email đã được xác minh.")
                    .build();
        }

        String redisKey = VERIFY_EMAIL_PREFIX + email;
        String savedOtp = redisTemplate.opsForValue().get(redisKey);
        if (savedOtp == null || !savedOtp.equals(request.getOtp())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP xác minh không hợp lệ hoặc đã hết hạn");
        }

        redisTemplate.delete(redisKey);
        userService.updateStatus(user.getId(), UserStatus.ACTIVE);

        return AuthMessageResponse.builder()
                .email(email)
                .message("Email đã được xác minh. Bạn có thể đăng nhập.")
                .build();
    }

    public AuthMessageResponse resendVerificationEmail(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        User user = userService.findByEmail(email);

        if (user.getUserStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        if (user.getUserStatus() == UserStatus.ACTIVE) {
            return AuthMessageResponse.builder()
                    .email(email)
                    .message("Email đã được xác minh.")
                    .build();
        }

        sendVerificationEmail(email);
        return AuthMessageResponse.builder()
                .email(email)
                .message("Mã xác minh mới đã được gửi tới email của bạn.")
                .build();
    }

    private void sendVerificationEmail(String email) {
        String otpCode = otpUtil.generateOtp();
        String redisKey = VERIFY_EMAIL_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, otpCode, VERIFY_EMAIL_TTL);
        mailProducerService.sendOtpToQueue(
                email,
                otpCode,
                "EMAIL_VERIFICATION",
                buildFrontendUrl("/verify-email", email, otpCode));
    }

    private String buildFrontendUrl(String path, String email, String otp) {
        String normalizedBase = frontendUrl.replaceAll("/+$", "");
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedOtp = URLEncoder.encode(otp, StandardCharsets.UTF_8);
        return normalizedBase + path + "?email=" + encodedEmail + "&otp=" + encodedOtp;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
