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

        // 1. Kiểm tra Header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            // 2. Lấy userId từ JWT thay vì username
            userId = jwtService.extractUserId(jwt);
        } catch (Exception ex) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Nếu có userId và chưa được xác thực trong Context
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 4. Validate Token (Check hạn và Blacklist)
            if (jwtService.isTokenValid(jwt, userId) && jwtService.isAccessToken(jwt)) {
                List<GrantedAuthority> authorities = new ArrayList<>();
                String role = jwtService.extractRole(jwt);
                String profileId = jwtService.extractProfileId(jwt);

                if (role == null || role.isBlank() || profileId == null || profileId.isBlank()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Thêm tiền tố "ROLE_" theo chuẩn của Spring Security
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

                // Gắn profileId vào Request để Controller dùng, không cần bóc JWT lại.
                request.setAttribute("profileId", profileId);

                // 6. Tạo Authentication object (Principal bây giờ là userId)
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId, // principal
                        null,   // credentials (không cần mật khẩu)
                        authorities // roles
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
