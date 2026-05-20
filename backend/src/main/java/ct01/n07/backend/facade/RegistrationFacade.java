package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.AuthMessageResponse;
import ct01.n07.backend.dto.auth.LoginRequest;
import ct01.n07.backend.dto.auth.OtpMailMessage;
import ct01.n07.backend.dto.auth.RegisterElderlyRequest;
import ct01.n07.backend.dto.auth.RegisterRequest;
import ct01.n07.backend.dto.auth.VerifyEmailRequest;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.model.enums.UserStatus;
import ct01.n07.backend.producer.MailProducerService;
import ct01.n07.backend.security.JwtService;
import ct01.n07.backend.service.RelationshipService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.service.UserService;
import ct01.n07.backend.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationFacade {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String VERIFY_EMAIL_PREFIX = "VERIFY_EMAIL:";
    private static final Duration VERIFY_EMAIL_TTL = Duration.ofMinutes(15);

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final RelationshipService relationshipService;
    private final UserProfileMapper userProfileMapper;
    private final JwtService jwtService;
    private final OtpUtil otpUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final MailProducerService mailProducerService;

    @Value("${app.frontend-url:${APP_FRONTEND_URL:${FRONTEND_URL:http://localhost:5173}}}")
    private String frontendUrl;

    @Transactional
    public AuthMessageResponse register(RegisterRequest registerRequest) {
        registerRequest.setEmail(normalizeEmail(registerRequest.getEmail()));

        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu và xác nhận mật khẩu không khớp");
        }

        LoginRequest loginRequest = LoginRequest.builder()
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .build();

        if (registerRequest.getCaregiver() != null
                && hasText(registerRequest.getCaregiver().getPhone())
                && userProfileService.findByPhone(registerRequest.getCaregiver().getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caregiver phone number already exists");
        }

        if (registerRequest.getElderly() != null && hasText(registerRequest.getElderly().getPhone())) {
            if (userProfileService.findByPhone(registerRequest.getElderly().getPhone())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elderly phone number already exists");
            }
        }

        User user = userService.createUser(loginRequest, UserStatus.INACTIVE);

        UserProfile caregiverProfile = userProfileMapper.toCaregiverProfile(registerRequest, user.getId());
        UserProfile savedCaregiverProfile = userProfileService.saveUserProfile(caregiverProfile);
        caregiverProfile = savedCaregiverProfile == null ? caregiverProfile : savedCaregiverProfile;

        if (registerRequest.getElderly() != null) {
            UserProfile elderlyProfile = userProfileMapper.toElderlyProfile(registerRequest, user.getId());
            elderlyProfile = userProfileService.saveUserProfile(elderlyProfile);
            relationshipService.createRelationship(
                    caregiverProfile.getId(),
                    elderlyProfile.getId(),
                    registerRequest.getElderly().getCaregiverTitle(),
                    registerRequest.getElderly().getElderlyTitle(),
                    registerRequest.getElderly().getPermissionLevel()
            );
        }

        sendVerificationEmail(user.getEmail(), displayName(caregiverProfile, user.getEmail()));

        return AuthMessageResponse.builder()
                .email(user.getEmail())
                .onboardingToken(jwtService.generateAuthToken(user.getId()))
                .message("Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản.")
                .build();
    }

    @Transactional
    public AuthMessageResponse registerElderlyProfile(String authorizationHeader, RegisterElderlyRequest request) {
        String onboardingToken = extractAndValidateOnboardingToken(authorizationHeader);
        String userId = jwtService.extractUserId(onboardingToken);
        User user = userService.findById(userId);

        if (user.getUserStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        if (userProfileService.findByPhone(request.getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elderly phone number already exists");
        }

        UserProfile caregiverProfile = userProfileService.findAllByUserId(userId, PageRequest.of(0, 50))
                .getContent()
                .stream()
                .filter(profile -> profile.getRole() == Role.CAREGIVER)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caregiver profile is required"));

        UserProfile elderlyProfile = UserProfile.builder()
                .userId(userId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .avatarUrl(request.getAvatarUrl())
                .role(Role.ELDERLY)
                .userContacts(new ArrayList<>())
                .build();

        elderlyProfile = userProfileService.saveUserProfile(elderlyProfile);
        relationshipService.createRelationship(
                caregiverProfile.getId(),
                elderlyProfile.getId(),
                request.getCaregiverTitle(),
                request.getElderlyTitle(),
                request.getPermissionLevel());

        return AuthMessageResponse.builder()
                .email(user.getEmail())
                .onboardingToken(onboardingToken)
                .message("Đã tạo hồ sơ người thân cần chăm sóc.")
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

        sendVerificationEmail(email, resolveRecipientName(user));
        return AuthMessageResponse.builder()
                .email(email)
                .message("Mã xác minh mới đã được gửi tới email của bạn.")
                .build();
    }

    private void sendVerificationEmail(String email, String recipientName) {
        String otpCode = otpUtil.generateOtp();
        String redisKey = VERIFY_EMAIL_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, otpCode, VERIFY_EMAIL_TTL);
        mailProducerService.sendOtpToQueue(
                email,
                otpCode,
                OtpMailMessage.EMAIL_VERIFICATION,
                buildFrontendUrl("/verify-email", email, otpCode),
                recipientName);
    }

    private String buildFrontendUrl(String path, String email, String otp) {
        String normalizedBase = frontendUrl.replaceAll("/+$", "");
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedOtp = URLEncoder.encode(otp, StandardCharsets.UTF_8);
        return normalizedBase + path + "?email=" + encodedEmail + "&otp=" + encodedOtp;
    }

    private String extractAndValidateOnboardingToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            String userId = jwtService.extractUserId(token);
            if (!jwtService.isTokenValid(token, userId) || !jwtService.isAuthToken(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid onboarding token");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid onboarding token");
        }

        return token;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
            log.warn("Không lấy được tên người nhận email xác minh cho user [{}]", user.getId());
            return user.getEmail();
        }
    }

    private String displayName(UserProfile profile, String fallback) {
        if (profile == null) {
            return fallback;
        }
        String fullName = ((profile.getFirstName() == null ? "" : profile.getFirstName().trim())
                + " "
                + (profile.getLastName() == null ? "" : profile.getLastName().trim())).trim();
        return hasText(fullName) ? fullName : fallback;
    }
}
