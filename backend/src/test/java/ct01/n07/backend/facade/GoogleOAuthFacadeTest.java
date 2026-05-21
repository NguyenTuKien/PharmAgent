package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.GoogleOAuthUserInfo;
import ct01.n07.backend.dto.auth.LoginResponse;
import ct01.n07.backend.dto.auth.UserProfileSummaryResponse;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.model.enums.UserStatus;
import ct01.n07.backend.repository.UserRepository;
import ct01.n07.backend.security.JwtService;
import ct01.n07.backend.service.GoogleOAuthClient;
import ct01.n07.backend.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthFacadeTest {

    @Mock
    private GoogleOAuthClient googleOAuthClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private PasswordEncoder passwordEncoder;

    private GoogleOAuthFacade googleOAuthFacade;

    @BeforeEach
    void setUp() {
        googleOAuthFacade = new GoogleOAuthFacade(
                googleOAuthClient,
                userRepository,
                userProfileService,
                userProfileMapper,
                jwtService,
                redisTemplate,
                passwordEncoder);
        ReflectionTestUtils.setField(googleOAuthFacade, "clientId", "google-client-id");
        ReflectionTestUtils.setField(googleOAuthFacade, "clientSecret", "google-client-secret");
        ReflectionTestUtils.setField(googleOAuthFacade, "frontendUrl", "http://localhost:5173");
    }

    @Test
    void startLoginStoresStateAndRedirectsToGoogleWithoutClientSecret() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        URI redirect = googleOAuthFacade.buildAuthorizationRedirect();

        assertThat(redirect.toString()).startsWith("https://accounts.google.com/o/oauth2/v2/auth?");
        assertThat(redirect.toString()).contains("client_id=google-client-id");
        assertThat(redirect.toString()).contains("redirect_uri=http://localhost:5173/api/auth/oauth/google/callback");
        assertThat(redirect.toString()).contains("scope=openid%20email%20profile");
        assertThat(redirect.toString()).doesNotContain("google-client-secret");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofMinutes(10)));

        assertThat(keyCaptor.getValue()).startsWith("GOOGLE_OAUTH_STATE:");
        assertThat(valueCaptor.getValue()).contains("\"nonce\"");
        assertThat(valueCaptor.getValue()).contains("\"redirectUri\"");
    }

    @Test
    void callbackRejectsUnknownStateBeforeCallingGoogle() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("GOOGLE_OAUTH_STATE:missing-state")).thenReturn(null);

        assertThatThrownBy(() -> googleOAuthFacade.completeCallback("auth-code", "missing-state"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid Google OAuth state");

        verify(googleOAuthClient, never()).exchangeCodeForIdToken(any(), any());
    }

    @Test
    void callbackCreatesActiveGoogleUserDefaultCaregiverProfileAndStoresOneTimeHandoff() {
        String statePayload = "{\"nonce\":\"nonce-123\",\"redirectUri\":\"http://localhost:5173/api/auth/oauth/google/callback\"}";
        User savedUser = User.builder()
                .id("user-1")
                .email("caregiver@example.com")
                .googleSubject("google-subject")
                .userStatus(UserStatus.ACTIVE)
                .build();
        UserProfile profile = UserProfile.builder()
                .id("profile-1")
                .userId("user-1")
                .firstName("An")
                .lastName("Nguyen")
                .avatarUrl("https://lh3.googleusercontent.com/avatar")
                .role(Role.CAREGIVER)
                .build();
        UserProfileSummaryResponse profileSummary = UserProfileSummaryResponse.builder()
                .id("profile-1")
                .userId("user-1")
                .firstName("An")
                .lastName("Nguyen")
                .avatarUrl("https://lh3.googleusercontent.com/avatar")
                .role(Role.CAREGIVER)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("GOOGLE_OAUTH_STATE:state-123")).thenReturn(statePayload);
        when(googleOAuthClient.exchangeCodeForIdToken("auth-code", "http://localhost:5173/api/auth/oauth/google/callback"))
                .thenReturn("id-token");
        when(googleOAuthClient.verifyIdToken("id-token")).thenReturn(new GoogleOAuthUserInfo(
                "google-subject",
                "caregiver@example.com",
                true,
                "An Nguyen",
                "https://lh3.googleusercontent.com/avatar",
                "nonce-123"));
        when(userRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("caregiver@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-oauth-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userProfileService.saveUserProfile(any(UserProfile.class))).thenReturn(profile);
        when(userProfileService.findAllByUserId("user-1", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of()), new PageImpl<>(List.of(profile)));
        when(userProfileMapper.toSummary(profile)).thenReturn(profileSummary);
        when(jwtService.generateAuthToken("user-1")).thenReturn("auth-token");
        when(jwtService.generateRefreshToken("user-1")).thenReturn("refresh-token");

        URI frontendRedirect = googleOAuthFacade.completeCallback("auth-code", "state-123");

        assertThat(frontendRedirect.toString()).startsWith("http://localhost:5173/login?oauth=google&code=");
        assertThat(frontendRedirect.toString()).doesNotContain("auth-token");
        assertThat(frontendRedirect.toString()).doesNotContain("refresh-token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("caregiver@example.com");
        assertThat(userCaptor.getValue().getGoogleSubject()).isEqualTo("google-subject");
        assertThat(userCaptor.getValue().getUserStatus()).isEqualTo(UserStatus.ACTIVE);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileService).saveUserProfile(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getRole()).isEqualTo(Role.CAREGIVER);
        assertThat(profileCaptor.getValue().getFirstName()).isEqualTo("An");
        assertThat(profileCaptor.getValue().getLastName()).isEqualTo("Nguyen");
        assertThat(profileCaptor.getValue().getAvatarUrl()).isEqualTo("https://lh3.googleusercontent.com/avatar");

        ArgumentCaptor<String> handoffKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> handoffPayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(handoffKeyCaptor.capture(), handoffPayloadCaptor.capture(), eq(Duration.ofSeconds(90)));
        assertThat(handoffKeyCaptor.getValue()).startsWith("GOOGLE_OAUTH_HANDOFF:");
        assertThat(handoffPayloadCaptor.getValue()).contains("\"authToken\":\"auth-token\"");
        assertThat(handoffPayloadCaptor.getValue()).contains("\"refreshToken\":\"refresh-token\"");
        assertThat(handoffPayloadCaptor.getValue()).contains("\"profiles\"");
        verify(redisTemplate).delete("GOOGLE_OAUTH_STATE:state-123");
    }

    @Test
    void exchangeHandoffCodeReturnsLoginResponseOnce() {
        String payload = """
                {
                  "authToken": "auth-token",
                  "refreshToken": "refresh-token",
                  "profiles": [
                    {
                      "id": "profile-1",
                      "userId": "user-1",
                      "firstName": "An",
                      "lastName": "Nguyen",
                      "avatarUrl": "https://lh3.googleusercontent.com/avatar",
                      "role": "CAREGIVER"
                    }
                  ]
                }
                """;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("GOOGLE_OAUTH_HANDOFF:handoff-code")).thenReturn(payload);

        LoginResponse response = googleOAuthFacade.exchangeHandoffCode("handoff-code");

        assertThat(response.getAuthToken()).isEqualTo("auth-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getProfiles().getContent()).hasSize(1);
        assertThat(response.getProfiles().getContent().getFirst().getRole()).isEqualTo(Role.CAREGIVER);
        verify(redisTemplate).delete("GOOGLE_OAUTH_HANDOFF:handoff-code");
    }
}
