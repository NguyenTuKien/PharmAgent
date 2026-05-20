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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class GoogleOAuthFacade {

    private static final String GOOGLE_AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String STATE_PREFIX = "GOOGLE_OAUTH_STATE:";
    private static final String HANDOFF_PREFIX = "GOOGLE_OAUTH_HANDOFF:";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final Duration HANDOFF_TTL = Duration.ofSeconds(90);
    private static final int LOGIN_PROFILE_PAGE_SIZE = 10;

    private final GoogleOAuthClient googleOAuthClient;
    private final UserRepository userRepository;
    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;
    private final JwtService jwtService;
    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${GOOGLE_CLIENT_ID:${google.client-id:}}")
    private String clientId;

    @Value("${GOOGLE_CLIENT_SECRET:${google.client-secret:}}")
    private String clientSecret;

    @Value("${app.frontend-url:${APP_FRONTEND_URL:http://localhost:5173}}")
    private String frontendUrl;

    @Value("${oauth.google.redirect-uri:}")
    private String configuredRedirectUri;

    public URI buildAuthorizationRedirect() {
        requireConfigured();

        String state = randomUrlToken();
        String nonce = randomUrlToken();
        String redirectUri = resolveRedirectUri();

        storeState(state, new StatePayload(nonce, redirectUri));

        return UriComponentsBuilder.fromUriString(GOOGLE_AUTHORIZATION_ENDPOINT)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("prompt", "select_account")
                .build()
                .encode()
                .toUri();
    }

    public URI completeCallback(String code, String state) {
        requireText(code, "Missing Google OAuth code");
        requireText(state, "Invalid Google OAuth state");

        StatePayload statePayload = consumeState(state);
        String idToken = googleOAuthClient.exchangeCodeForIdToken(code, statePayload.redirectUri());
        GoogleOAuthUserInfo googleUser = googleOAuthClient.verifyIdToken(idToken);

        if (!statePayload.nonce().equals(googleUser.nonce())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google OAuth nonce");
        }

        User user = findOrCreateUser(googleUser);
        LoginResponse loginResponse = buildLoginResponse(user);
        String handoffCode = randomUrlToken();
        storeHandoff(handoffCode, loginResponse);

        return UriComponentsBuilder.fromUriString(normalizedFrontendUrl())
                .path("/login")
                .queryParam("oauth", "google")
                .queryParam("code", handoffCode)
                .build()
                .encode()
                .toUri();
    }

    public LoginResponse exchangeHandoffCode(String code) {
        requireText(code, "OAuth code không hợp lệ");

        String key = HANDOFF_PREFIX + code;
        String rawPayload = redisTemplate.opsForValue().get(key);
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OAuth code không hợp lệ hoặc đã hết hạn");
        }
        redisTemplate.delete(key);

        try {
            HandoffPayload payload = objectMapper.readValue(rawPayload, HandoffPayload.class);
            List<UserProfileSummaryResponse> profiles = payload.profiles().stream()
                    .map(ProfileSnapshot::toResponse)
                    .toList();
            return LoginResponse.builder()
                    .authToken(payload.authToken())
                    .refreshToken(payload.refreshToken())
                    .profiles(new PageImpl<>(profiles))
                    .build();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OAuth code không hợp lệ hoặc đã hết hạn");
        }
    }

    public URI buildFailureRedirect(String reason) {
        return UriComponentsBuilder.fromUriString(normalizedFrontendUrl())
                .path("/login")
                .queryParam("oauth", "google")
                .queryParam("error", normalizeReason(reason))
                .build()
                .encode()
                .toUri();
    }

    private User findOrCreateUser(GoogleOAuthUserInfo googleUser) {
        Optional<User> byGoogleSubject = userRepository.findByGoogleSubject(googleUser.subject());
        Optional<User> byEmail = userRepository.findByEmail(googleUser.email());

        if (byGoogleSubject.isPresent() && byEmail.isPresent()
                && !byGoogleSubject.get().getId().equals(byEmail.get().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Google account conflicts with an existing email");
        }

        User user = byGoogleSubject.or(() -> byEmail).orElseGet(() -> createGoogleUser(googleUser));

        if (user.getUserStatus() == UserStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa");
        }

        boolean changed = false;
        if (!hasText(user.getGoogleSubject())) {
            user.setGoogleSubject(googleUser.subject());
            changed = true;
        } else if (!user.getGoogleSubject().equals(googleUser.subject())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Google account conflicts with an existing email");
        }

        if (user.getUserStatus() == UserStatus.INACTIVE) {
            user.setUserStatus(UserStatus.ACTIVE);
            changed = true;
        }

        if (changed) {
            user = userRepository.save(user);
        }

        ensureDefaultProfile(user.getId(), googleUser);
        return user;
    }

    private User createGoogleUser(GoogleOAuthUserInfo googleUser) {
        User user = User.builder()
                .email(googleUser.email())
                .googleSubject(googleUser.subject())
                .password(passwordEncoder.encode(randomUrlToken()))
                .userStatus(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    private void ensureDefaultProfile(String userId, GoogleOAuthUserInfo googleUser) {
        Page<UserProfile> profiles = userProfileService.findAllByUserId(
                userId,
                PageRequest.of(0, LOGIN_PROFILE_PAGE_SIZE));
        if (profiles.hasContent()) {
            return;
        }

        NameParts nameParts = splitName(googleUser.name(), googleUser.email());
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .firstName(nameParts.firstName())
                .lastName(nameParts.lastName())
                .avatarUrl(blankToNull(googleUser.picture()))
                .role(Role.CAREGIVER)
                .userContacts(new ArrayList<>())
                .build();
        userProfileService.saveUserProfile(profile);
    }

    private LoginResponse buildLoginResponse(User user) {
        String authToken = jwtService.generateAuthToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        Page<UserProfileSummaryResponse> profiles = userProfileService
                .findAllByUserId(user.getId(), PageRequest.of(0, LOGIN_PROFILE_PAGE_SIZE))
                .map(userProfileMapper::toSummary);

        return LoginResponse.builder()
                .authToken(authToken)
                .refreshToken(refreshToken)
                .profiles(profiles)
                .build();
    }

    private void storeState(String state, StatePayload payload) {
        try {
            redisTemplate.opsForValue().set(STATE_PREFIX + state, objectMapper.writeValueAsString(payload), STATE_TTL);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể khởi tạo Google OAuth");
        }
    }

    private StatePayload consumeState(String state) {
        String key = STATE_PREFIX + state;
        String rawPayload = redisTemplate.opsForValue().get(key);
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google OAuth state");
        }
        redisTemplate.delete(key);

        try {
            StatePayload payload = objectMapper.readValue(rawPayload, StatePayload.class);
            if (!hasText(payload.nonce()) || !hasText(payload.redirectUri())) {
                throw new IllegalArgumentException("State payload incomplete");
            }
            return payload;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google OAuth state");
        }
    }

    private void storeHandoff(String handoffCode, LoginResponse response) {
        try {
            List<ProfileSnapshot> profileSnapshots = response.getProfiles().getContent().stream()
                    .map(ProfileSnapshot::from)
                    .toList();
            HandoffPayload payload = new HandoffPayload(
                    response.getAuthToken(),
                    response.getRefreshToken(),
                    profileSnapshots);
            redisTemplate.opsForValue().set(
                    HANDOFF_PREFIX + handoffCode,
                    objectMapper.writeValueAsString(payload),
                    HANDOFF_TTL);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể hoàn tất Google OAuth");
        }
    }

    private String resolveRedirectUri() {
        if (hasText(configuredRedirectUri)) {
            return configuredRedirectUri.trim();
        }
        return normalizedFrontendUrl() + "/api/auth/oauth/google/callback";
    }

    private String normalizedFrontendUrl() {
        return frontendUrl.replaceAll("/+$", "");
    }

    private String normalizeReason(String reason) {
        if (!hasText(reason)) {
            return "oauth_failed";
        }
        return reason.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private void requireConfigured() {
        if (!hasText(clientId) || !hasText(clientSecret)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google OAuth chưa được cấu hình");
        }
    }

    private void requireText(String value, String message) {
        if (!hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String randomUrlToken() {
        byte[] bytes = new byte[32];
        SecureRandomHolder.INSTANCE.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private NameParts splitName(String name, String email) {
        String source = hasText(name) ? name.trim() : email.substring(0, email.indexOf('@'));
        String[] parts = source.split("\\s+", 2);
        String firstName = blankToNull(parts[0]);
        String lastName = parts.length > 1 ? blankToNull(parts[1]) : null;
        return new NameParts(firstName == null ? "Google" : firstName, lastName);
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record StatePayload(String nonce, String redirectUri) {
    }

    private record NameParts(String firstName, String lastName) {
    }

    private record HandoffPayload(String authToken, String refreshToken, List<ProfileSnapshot> profiles) {
    }

    private record ProfileSnapshot(
            String id,
            String userId,
            String phone,
            String firstName,
            String lastName,
            String avatarUrl,
            Role role
    ) {
        static ProfileSnapshot from(UserProfileSummaryResponse response) {
            return new ProfileSnapshot(
                    response.getId(),
                    response.getUserId(),
                    response.getPhone(),
                    response.getFirstName(),
                    response.getLastName(),
                    response.getAvatarUrl(),
                    response.getRole());
        }

        UserProfileSummaryResponse toResponse() {
            return UserProfileSummaryResponse.builder()
                    .id(id)
                    .userId(userId)
                    .phone(phone)
                    .firstName(firstName)
                    .lastName(lastName)
                    .avatarUrl(avatarUrl)
                    .role(role)
                    .build();
        }
    }

    private static final class SecureRandomHolder {
        private static final java.security.SecureRandom INSTANCE = new java.security.SecureRandom();
    }
}
