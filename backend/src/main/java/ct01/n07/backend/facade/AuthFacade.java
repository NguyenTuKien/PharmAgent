package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.*;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.producer.MailProducerService;
import ct01.n07.backend.security.JwtService;
import ct01.n07.backend.service.RelationshipService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.service.UserService;
import ct01.n07.backend.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacade {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final RelationshipService relationshipService;
    private final JwtService jwtService;
    private final UserProfileMapper userProfileMapper;
    private final OtpUtil otpUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final MailProducerService mailProducerService;

    // ==========================================
    // API 1: XỬ LÝ ĐĂNG NHẬP
    // ==========================================
    public LoginResponse login(LoginRequest request, Pageable pageable) {
        log.info("Bắt đầu luồng đăng nhập cho email: {}", request.getEmail());

        // 1. UserService lo việc kiểm tra DB và Mật khẩu
        User user = userService.verifyUserCredentials(request.getEmail(), request.getPassword());

        // 2. JwtService cấp phát Token
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 3. UserProfileService lấy danh sách hồ sơ theo phân trang hiện có
        Page<UserProfileSummaryResponse> profiles = userProfileService
                .findAllByUserId(user.getId(), pageable)
                .map(userProfileMapper::toSummary);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .profiles(profiles)
                .build();
    }

    // ==========================================
    // API 2: CHỌN HỒ SƠ (CẤP QUYỀN)
    // ==========================================
    public String selectProfile(String authorizationHeader, String profileId) {
        String accessToken = normalizeToken(extractBearerToken(authorizationHeader));
        String currentUserId;
        try {
            currentUserId = jwtService.extractUserId(accessToken);
            if (!jwtService.isTokenValid(accessToken, currentUserId) || !jwtService.isAccessToken(accessToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
            }
        } catch (Exception ex) {
            log.warn("selectProfile rejected due to invalid/expired access token: {}", ex.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }

        // 1. Lấy thông tin Profile
        UserProfile profile = userProfileService.findById(profileId);

        // 2. Kiểm tra bảo mật: Profile này có thuộc về tài khoản đang đăng nhập không?
        if (!profile.getUserId().equals(currentUserId)) {
            log.warn("Tài khoản {} cố gắng truy cập trái phép hồ sơ {}", currentUserId, profileId);
            throw new AccessDeniedException("Bạn không có quyền truy cập hồ sơ này.");
        }

        // 3. Cấp Profile Token chứa Role
        String profileToken = jwtService.generateProfileToken(
                currentUserId,
                profileId,
                profile.getRole().name());

        return profileToken;
    }

    // ==========================================
    // API 3: REFRESH TOKEN (STATELESS ROTATION)
    // ==========================================
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        String token = normalizeToken(request.getRefreshToken());

        // 1. Kiểm tra tính hợp lệ và loại Token
        String userId;
        try {
            userId = jwtService.extractUserId(token);
            if (!jwtService.isTokenValid(token, userId) || !jwtService.isRefreshToken(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }
        } catch (Exception ex) {
            log.warn("refresh rejected due to invalid refresh token: {}", ex.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        // 2. Kiểm tra quyền truy cập Profile
        UserProfile profile = userProfileService.findById(request.getProfileId());
        if (!profile.getUserId().equals(userId)) {
            throw new AccessDeniedException("Dữ liệu không khớp. Vui lòng đăng nhập lại.");
        }

        // 3. Rotation: Cấp mới đủ 3 token để client tiếp tục gọi cả account-level và
        // profile-level APIs
        String newAccessToken = jwtService.generateAccessToken(userId);
        String newProfileToken = jwtService.generateProfileToken(userId, profile.getId(), profile.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(userId);

        // Mẹo bảo mật bổ sung: Ném token cũ vào blacklist để tránh bị dùng lại
        jwtService.blacklistTokens(List.of(token));

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .profileToken(newProfileToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    // ==========================================
    // API 4: ĐĂNG XUẤT
    // ==========================================
    public void logout(LogoutRequest request) {
        List<String> tokensToRevoke = java.util.stream.Stream.of(
                request.getAccessToken(),
                request.getRefreshToken(),
                request.getProfileToken()).map(this::normalizeToken)
                .filter(token -> token != null && !token.isBlank())
                .toList();

        // Tống toàn bộ vào danh sách đen
        if (!tokensToRevoke.isEmpty()) {
            jwtService.blacklistTokens(tokensToRevoke);
            // Lưu ý: Đảm bảo JwtService của bạn đang có hàm blacklistTokens(List<String>
            // tokens)
        }

        log.info("Đã xử lý đưa {} token vào blacklist.", tokensToRevoke.size());
    }

    // ==========================================
    // API 5: ĐĂNG KÝ
    // ==========================================
    @Transactional
    public LoginResponse signup(SignupRequest signupRequest, Pageable pageable) {
        if (!signupRequest.getPassword().equals(signupRequest.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu và xác nhận mật khẩu không khớp");
        }
        User user;
        LoginRequest loginRequest = LoginRequest.builder().email(signupRequest.getEmail())
                .password(signupRequest.getPassword()).build();
        if (signupRequest.getCaregiver() != null
                && userProfileService.findByPhone(signupRequest.getCaregiver().getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caregiver phone number already exists");
        }
        if (signupRequest.getElderly() != null && !signupRequest.getElderly().getPhone().isBlank()) {
            if (userProfileService.findByPhone(signupRequest.getElderly().getPhone())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elderly phone number already exists");
            }
        }
        try {
            user = userService.createUser(loginRequest);
        } catch (Exception e) {
            user = userService.verifyUserCredentials(loginRequest.getEmail(), loginRequest.getPassword());
        }

        UserProfile caregiverProfile = userProfileMapper.toCaregiverProfile(signupRequest, user.getId());
        userProfileService.saveUserProfile(caregiverProfile);

        if (signupRequest.getElderly() != null) {
            UserProfile elderlyProfile = userProfileMapper.toElderlyProfile(signupRequest, user.getId());
            userProfileService.saveUserProfile(elderlyProfile);
            relationshipService.createRelationship(caregiverProfile.getId(), elderlyProfile.getId(),
                    signupRequest.getElderly().getCaregiverTitle(), signupRequest.getElderly().getElderlyTitle(),
                    signupRequest.getElderly().getPermissionLevel());
        }
        return login(loginRequest, pageable);
    }

    // ==========================================
    // API 6: QUÊN MẬT KHẨU
    // ==========================================
    public void processForgotPassword(String email) {
        // Bước 1: Kiểm tra xem email có tồn tại trong MongoDB không
        try {
            userService.findByEmail(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Email không tồn tại");
        }

        // Bước 2: Sinh mã OTP
        String otpCode = otpUtil.generateOtp();

        // Bước 3: Lưu vào Redis với Key là "OTP:email", Value là mã OTP, thời gian sống
        // (TTL) là 5 phút
        String redisKey = "OTP:" + email;
        redisTemplate.opsForValue().set(redisKey, otpCode, Duration.ofMinutes(5));

        // Bước 4: Đẩy nhiệm vụ gửi mail vào RabbitMQ
        mailProducerService.sendOtpToQueue(email, otpCode);
    }

    // Hàm này dùng để gọi ở API bước sau (khi user nhập OTP vào app)
    public boolean verifyOtp(String email, String userProvidedOtp) {
        String redisKey = "OTP:" + email;

        // Lấy OTP từ Redis ra
        String savedOtp = redisTemplate.opsForValue().get(redisKey);

        if (savedOtp != null && savedOtp.equals(userProvidedOtp)) {
            // Xác thực thành công -> Xóa OTP khỏi Redis ngay lập tức để tránh dùng lại
            redisTemplate.delete(redisKey);
            return true;
        }
        return false;
    }

    // ==========================================
    // API 7: DAT LAI MAT KHAU
    // ==========================================
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

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    private String normalizeToken(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        String token = rawToken.trim();
        if (token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length()).trim();
        }
        return token;
    }

}
