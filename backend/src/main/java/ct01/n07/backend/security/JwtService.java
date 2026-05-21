package ct01.n07.backend.security;

import java.util.Date;
import java.util.List;

import ct01.n07.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j // Dùng thư viện log chuẩn thay vì System.err.println
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    // ==========================================
    // 1. CÁC HÀM TẠO TOKEN
    // ==========================================

    // Bước 1: Tạo Auth Token cho tài khoản gốc
    public String generateAuthToken(String userId) {
        return jwtUtil.generateAuthToken(userId);
    }

    // Bước 2: Tạo Access Token mang theo quyền (Role)
    public String generateAccessToken(String userId, String profileId, String role) {
        return jwtUtil.generateAccessToken(userId, profileId, role);
    }

    // Bước 3: Tạo Refresh Token dạng JWT (Đã bổ sung truyền userId)
    public String generateRefreshToken(String userId) {
        return jwtUtil.generateRefreshToken(userId);
    }

    // ==========================================
    // 2. CÁC HÀM TRÍCH XUẤT (EXTRACT)
    // ==========================================

    public String extractUserId(String token) {
        return jwtUtil.extractUserId(token);
    }

    public String extractProfileId(String token) {
        return jwtUtil.extractProfileId(token);
    }

    public String extractRole(String token) {
        return jwtUtil.extractRole(token);
    }

    // ==========================================
    // 3. CÁC HÀM KIỂM TRA (VALIDATE)
    // ==========================================

    // Hàm này được JwtAuthenticationFilter gọi để check Token ở mỗi Request
    public boolean isTokenValid(String token, String userId) {
        // 1. Kiểm tra xem token có bị Admin/Hệ thống thu hồi chưa (Logout)
        if (tokenBlacklistService.isBlacklisted(token)) {
            return false;
        }
        
        // 2. Kiểm tra xem toàn bộ user session có bị thu hồi không
        try {
            Date issuedAt = jwtUtil.extractIssuedAt(token);
            if (tokenBlacklistService.isUserBlacklisted(userId, issuedAt)) {
                return false;
            }
        } catch (Exception e) {
            return false; // Token hỏng
        }

        // 3. Kiểm tra chữ ký và hạn sử dụng bằng JJWT
        return jwtUtil.isTokenValid(token, userId);
    }

    public boolean isAuthToken(String token) {
        return jwtUtil.isAuthToken(token);
    }

    public boolean isAccessToken(String token) {
        return jwtUtil.isAccessToken(token);
    }

    public boolean isRefreshToken(String token) {
        return jwtUtil.isRefreshToken(token);
    }

    // ==========================================
    // 4. CHỨC NĂNG ĐĂNG XUẤT / HỦY NHIỀU TOKEN
    // ==========================================

    // Dùng cho API Logout: Nhận một mảng các token từ Frontend gửi lên
    public void blacklistTokens(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        for (String token : tokens) {
            if (token != null && !token.isBlank()) {
                blacklistSingleToken(token);
            }
        }
    }

    public void blacklistUser(String userId) {
        tokenBlacklistService.blacklistUser(userId);
    }

    // Xử lý đưa từng token vào Blacklist
    private void blacklistSingleToken(String token) {
        try {
            // Hàm extractExpiration của JJWT sẽ tự động ném lỗi nếu token bị sai chữ ký hoặc đã hết hạn.
            // Nên nếu code chạy qua được dòng này, token đó là hợp lệ và chưa hết hạn.
            Date expiry = jwtUtil.extractExpiration(token);
            tokenBlacklistService.blacklist(token, expiry);

        } catch (Exception e) {
            // Log lại để debug, không làm crash hệ thống vì token hỏng thì coi như đã vô dụng rồi
            log.warn("Bỏ qua token không hợp lệ hoặc đã hết hạn khi đưa vào blacklist: {}", e.getMessage());
        }
    }
}