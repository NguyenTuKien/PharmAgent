package ct01.web.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-ms:300000}") // Token bước 1: 5 phút
    private long accessExpirationMs;

    @Value("${jwt.profile-expiration-ms:86400000}") // Token bước 2 (Gọi API): 1 ngày
    private long profileExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}") // Token làm mới: 7 ngày
    private long refreshExpirationMs;

    // ==========================================
    // 1. TRÍCH XUẤT THÔNG TIN CHUNG
    // ==========================================
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    // ==========================================
    // 2. TRÍCH XUẤT THÔNG TIN ĐẶC THÙ (PROFILE)
    // ==========================================
    public String extractProfileId(String token) {
        return extractClaim(token, claims -> claims.get("profileId", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ==========================================
    // 3. CÁC HÀM TẠO TOKEN
    // ==========================================

    // Bước 1: Tạo Access Token (Chỉ chứa userId)
    public String generateAccessToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        return buildToken(claims, userId, accessExpirationMs);
    }

    // Bước 2: Tạo Profile Token (Chứa userId, profileId, role)
    public String generateProfileToken(String userId, String profileId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "profile");
        claims.put("profileId", profileId);
        claims.put("role", role);
        return buildToken(claims, userId, profileExpirationMs);
    }

    // Bước 3: Tạo Refresh Token (Bây giờ là JWT, có hạn sử dụng dài)
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, userId, refreshExpirationMs);
    }

    // ==========================================
    // 4. CÁC HÀM KIỂM TRA (VALIDATE)
    // ==========================================

    public boolean isTokenValid(String token, String userId) {
        final String extractedUserId = extractUserId(token);
        return (extractedUserId.equals(userId)) && !isTokenExpired(token);
    }

    public boolean isProfileToken(String token) {
        return "profile".equals(extractTokenType(token));
    }

    public boolean isAccessToken(String token) {
        return "access".equals(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long ttlMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttlMs);
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject) // Subject chính là userId
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}