package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.*;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.security.JwtService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacade {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final JwtService jwtService;
    private final UserProfileMapper userProfileMapper;

    // ==========================================
    // API 1: XỬ LÝ ĐĂNG NHẬP
    // ==========================================
    public LoginResponse login(LoginRequest request, Pageable pageable) {
        log.info("Bắt đầu luồng đăng nhập cho email: {}", request.getEmail());

        // 1. UserService lo việc kiểm tra DB và Mật khẩu
        User user = userService.verifyUserCredentials(request.getEmail(), request.getPassword());

        // 2. JwtService cấp phát Token
        String authToken = jwtService.generateAuthToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 3. UserProfileService lấy danh sách hồ sơ theo phân trang hiện có
        Page<UserProfileSummaryResponse> profiles = userProfileService
                .findAllByUserId(user.getId(), pageable)
                .map(userProfileMapper::toSummary);

        return LoginResponse.builder()
                .authToken(authToken)
                .refreshToken(refreshToken)
                .profiles(profiles)
                .build();
    }

    // ==========================================
    // API 2: CHỌN HỒ SƠ (CẤP QUYỀN)
    // ==========================================
    public String selectProfile(String authorizationHeader, String profileId) {
        String authToken = normalizeToken(extractBearerToken(authorizationHeader));
        String currentUserId;
        try {
            currentUserId = jwtService.extractUserId(authToken);
            if (!jwtService.isTokenValid(authToken, currentUserId) || !jwtService.isAuthToken(authToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
            }
        } catch (Exception ex) {
            log.warn("selectProfile rejected due to invalid/expired auth token: {}", ex.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        }

        // 1. Lấy thông tin Profile
        UserProfile profile = userProfileService.findById(profileId);

        // 2. Kiểm tra bảo mật: Profile này có thuộc về tài khoản đang đăng nhập không?
        if (!profile.getUserId().equals(currentUserId)) {
            log.warn("Tài khoản {} cố gắng truy cập trái phép hồ sơ {}", currentUserId, profileId);
            throw new AccessDeniedException("Bạn không có quyền truy cập hồ sơ này.");
        }

        // 3. Cấp Access Token chứa Role
        String accessToken = jwtService.generateAccessToken(
                currentUserId,
                profileId,
                profile.getRole().name());

        return accessToken;
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

        // 3. Rotation
        String newAuthToken = jwtService.generateAuthToken(userId);
        String newAccessToken = jwtService.generateAccessToken(userId, profile.getId(), profile.getRole().name());
        String newRefreshToken = jwtService.generateRefreshToken(userId);

        jwtService.blacklistTokens(List.of(token));

        return TokenRefreshResponse.builder()
                .authToken(newAuthToken)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    // ==========================================
    // API 4: ĐĂNG XUẤT
    // ==========================================
    public void logout(LogoutRequest request) {
        List<String> tokensToRevoke = java.util.stream.Stream.of(
                request.getAuthToken(),
                request.getAccessToken(),
                request.getRefreshToken()).map(this::normalizeToken)
                .filter(token -> token != null && !token.isBlank())
                .toList();

        if (!tokensToRevoke.isEmpty()) {
            jwtService.blacklistTokens(tokensToRevoke);
        }

        log.info("Đã xử lý đưa {} token vào blacklist.", tokensToRevoke.size());
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
