package ct01.n07.backend.filter;

import ct01.n07.backend.security.JwtService;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doesNotAuthenticateAuthTokenForProtectedRequests() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/medications");
        request.addHeader("Authorization", "Bearer account-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUserId("account-token")).thenReturn("user-1");
        when(jwtService.isTokenValid("account-token", "user-1")).thenReturn(true);
        when(jwtService.isAccessToken("account-token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void authenticatesAccessTokenWithProfileRoleAndRequestProfile() throws ServletException, IOException {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/medications");
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUserId("access-token")).thenReturn("user-1");
        when(jwtService.isTokenValid("access-token", "user-1")).thenReturn(true);
        when(jwtService.isAccessToken("access-token")).thenReturn(true);
        when(jwtService.extractRole("access-token")).thenReturn("CAREGIVER");
        when(jwtService.extractProfileId("access-token")).thenReturn("profile-1");

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("user-1", authentication.getPrincipal());
        assertEquals("ROLE_CAREGIVER", authentication.getAuthorities().iterator().next().getAuthority());
        assertEquals("profile-1", request.getAttribute("profileId"));
    }
}
