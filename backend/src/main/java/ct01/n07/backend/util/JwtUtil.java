package ct01.n07.backend.util;

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
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.auth-expiration-ms:300000}") // Token bước 1: 5 phút
    private long authExpirationMs;

    @Value("${jwt.access-expiration-ms:900000}") // Token bước 2 (gọi API): 15 phút
    private long accessExpirationMs;

    // ==========================================
    // 1. TRÍCH XUẤT THÔNG TIN CHUNG
    // ==========================================
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
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

    // Bước 1: Tạo Auth Token (Chỉ chứa userId)
    public String generateAuthToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "auth");
        return buildToken(claims, userId, authExpirationMs);
    }

    // Bước 2: Tạo Access Token (Chứa userId, profileId, role)
    public String generateAccessToken(String userId, String profileId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("profileId", profileId);
        claims.put("role", role);
        return buildToken(claims, userId, accessExpirationMs);
    }

    // ==========================================
    // 4. CÁC HÀM KIỂM TRA (VALIDATE)
    // ==========================================

    public boolean isTokenValid(String token, String userId) {
        final String extractedUserId = extractUserId(token);
        return (extractedUserId.equals(userId)) && !isTokenExpired(token);
    }

    public boolean isAuthToken(String token) {
        return "auth".equals(extractTokenType(token));
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
        String jti = UUID.randomUUID().toString(); // Generate unique token ID
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject) // Subject chính là userId
                .setId(jti) // Set JTI (JWT ID)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
