package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.*;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.security.JwtService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.service.UserPresenceService;
import ct01.n07.backend.service.UserService;
import ct01.n07.backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacade {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final JwtService jwtService;
    private final JwtUtil jwtUtil;
    private final UserProfileMapper userProfileMapper;
    private final UserPresenceService userPresenceService;

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
    public String selectProfile(String authorizationHeader, String profileId, HttpServletRequest request) {
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

        // 4. Track session
        try {
            String tokenId = jwtUtil.extractTokenId(accessToken);
            LocalDateTime expiresAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(jwtUtil.extractExpiration(accessToken).getTime()),
                    ZoneId.of("Asia/Ho_Chi_Minh")
            );
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            
            userPresenceService.trackSession(
                    accessToken,
                    tokenId, 
                    currentUserId, 
                    profileId, 
                    profile.getRole().name(),
                    ipAddress, 
                    userAgent, 
                    expiresAt
            );
        } catch (Exception e) {
            log.warn("Failed to track session: {}", e.getMessage());
        }

        return accessToken;
    }

    // ==========================================
    // API 3: REFRESH TOKEN (SERVER-MANAGED SESSION)
    // ==========================================
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        String token = normalizeToken(request.getRefreshToken());

        // 1. Kiểm tra refresh session token. Token mới là opaque token lưu hash ở server.
        // Legacy JWT refresh token còn hợp lệ sẽ được migrate bằng cách rotate sang opaque token mới.
        Optional<String> resolvedUserId = jwtService.resolveRefreshTokenUserId(token);
        if (resolvedUserId.isEmpty()) {
            log.warn("refresh rejected due to invalid refresh token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        String userId = resolvedUserId.get();

        // 2. Nếu đã có profile, xác thực quyền trước khi rotate token để request sai
        // không vô tình làm mất phiên hợp lệ hiện tại.
        String profileId = normalizeToken(request.getProfileId());
        UserProfile profile = null;
        if (profileId != null && !profileId.isBlank()) {
            profile = userProfileService.findById(profileId);
            if (!profile.getUserId().equals(userId)) {
                throw new AccessDeniedException("Dữ liệu không khớp. Vui lòng đăng nhập lại.");
            }
        }

        // 3. Rotate refresh token trên mỗi lần dùng để giảm rủi ro replay nếu token cũ bị lộ.
        String newAuthToken = jwtService.generateAuthToken(userId);
        String newRefreshToken = jwtService.generateRefreshToken(userId);
        jwtService.blacklistTokens(List.of(token));

        // 4. Nếu chưa chọn profile, chỉ cấp lại auth token cho màn chọn profile.
        if (profile == null) {
            return TokenRefreshResponse.builder()
                    .authToken(newAuthToken)
                    .refreshToken(newRefreshToken)
                    .build();
        }

        // 5. Nếu đã có profile, cấp lại access token profile-scoped.
        String newAccessToken = jwtService.generateAccessToken(userId, profile.getId(), profile.getRole().name());

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
            
            // Remove sessions
            for (String token : tokensToRevoke) {
                try {
                    String tokenId = jwtUtil.extractTokenId(token);
                    userPresenceService.removeSession(tokenId);
                } catch (Exception e) {
                    log.warn("Failed to remove session for token: {}", e.getMessage());
                }
            }
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

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}
