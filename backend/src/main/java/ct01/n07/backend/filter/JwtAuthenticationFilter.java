package ct01.n07.backend.filter;

import ct01.n07.backend.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userId;

        // 1. Kiểm tra Header - không có token thì bỏ qua, để Spring Security xử lý
        // anonymous
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            // 2. Lấy userId từ JWT
            userId = jwtService.extractUserId(jwt);
        } catch (Exception ex) {
            // Token không parse được → 401 ngay
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // 3. Nếu có userId và chưa được xác thực trong Context
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 4. Validate Token: hạn sử dụng + chữ ký + blacklist token + user-level
            // revocation
            if (!jwtService.isTokenValid(jwt, userId)) {
                // Token hợp lệ về cú pháp nhưng đã bị thu hồi hoặc hết hạn
                // Trả về 401 để frontend interceptor kích hoạt refresh hoặc logout
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token revoked or expired");
                return;
            }

            List<GrantedAuthority> authorities = new ArrayList<>();

            // 5. Phân loại Token và Cấp quyền
            if (jwtService.isAccessToken(jwt)) {
                String role = jwtService.extractRole(jwt);
                String profileId = jwtService.extractProfileId(jwt);
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                request.setAttribute("profileId", profileId);
            }
            // Auth Token → authorities rỗng (chỉ dùng được ở /select-profile)

            // 6. Tạo và ghi Authentication vào SecurityContext
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
