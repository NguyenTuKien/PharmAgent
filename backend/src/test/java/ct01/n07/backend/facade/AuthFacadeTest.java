package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.TokenRefreshRequest;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.security.JwtService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthFacadeTest {

    @Mock
    private UserService userService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserProfileMapper userProfileMapper;

    @InjectMocks
    private AuthFacade authFacade;

    @Test
    void refreshRotatesServerManagedRefreshToken() {
        TokenRefreshRequest request = TokenRefreshRequest.builder()
                .refreshToken("old-refresh-token")
                .profileId("profile-1")
                .build();
        UserProfile profile = UserProfile.builder()
                .id("profile-1")
                .userId("user-1")
                .role(Role.CAREGIVER)
                .build();

        when(jwtService.resolveRefreshTokenUserId("old-refresh-token")).thenReturn(Optional.of("user-1"));
        when(userProfileService.findById("profile-1")).thenReturn(profile);
        when(jwtService.generateAuthToken("user-1")).thenReturn("auth-token");
        when(jwtService.generateAccessToken("user-1", "profile-1", "CAREGIVER")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("user-1")).thenReturn("new-refresh-token");

        var response = authFacade.refresh(request);

        assertThat(response.getAuthToken()).isEqualTo("auth-token");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(jwtService).blacklistTokens(List.of("old-refresh-token"));
    }

    @Test
    void refreshMigratesLegacyJwtRefreshTokenToServerManagedToken() {
        TokenRefreshRequest request = TokenRefreshRequest.builder()
                .refreshToken("legacy-jwt-refresh-token")
                .profileId("profile-1")
                .build();
        UserProfile profile = UserProfile.builder()
                .id("profile-1")
                .userId("user-1")
                .role(Role.CAREGIVER)
                .build();

        when(jwtService.resolveRefreshTokenUserId("legacy-jwt-refresh-token")).thenReturn(Optional.of("user-1"));
        when(userProfileService.findById("profile-1")).thenReturn(profile);
        when(jwtService.generateAuthToken("user-1")).thenReturn("auth-token");
        when(jwtService.generateAccessToken("user-1", "profile-1", "CAREGIVER")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("user-1")).thenReturn("persistent-refresh-token");

        var response = authFacade.refresh(request);

        assertThat(response.getRefreshToken()).isEqualTo("persistent-refresh-token");
        verify(jwtService).blacklistTokens(List.of("legacy-jwt-refresh-token"));
    }

    @Test
    void refreshWithoutProfileReturnsOnlyAuthAndRefreshTokens() {
        TokenRefreshRequest request = TokenRefreshRequest.builder()
                .refreshToken("old-refresh-token")
                .build();

        when(jwtService.resolveRefreshTokenUserId("old-refresh-token")).thenReturn(Optional.of("user-1"));
        when(jwtService.generateAuthToken("user-1")).thenReturn("auth-token");
        when(jwtService.generateRefreshToken("user-1")).thenReturn("new-refresh-token");

        var response = authFacade.refresh(request);

        assertThat(response.getAuthToken()).isEqualTo("auth-token");
        assertThat(response.getAccessToken()).isNull();
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(jwtService).blacklistTokens(List.of("old-refresh-token"));
    }
}
