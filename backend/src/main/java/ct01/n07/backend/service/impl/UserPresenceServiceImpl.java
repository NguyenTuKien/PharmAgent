package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.session.SessionInfo;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.repository.UserRepository;
import ct01.n07.backend.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPresenceServiceImpl implements UserPresenceService {

    private static final String SESSION_PREFIX = "session:";
    private static final String TOKEN_MAPPING_PREFIX = "token_map:";
    private static final String ONLINE_KEY_PREFIX = "user:online:";
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    public void trackSession(String token, String tokenId, String userId, String profileId, String role,
                           String ipAddress, String userAgent, LocalDateTime expiresAt) {
        try {
            LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);
            long ttlMs = Duration.between(now, expiresAt).toMillis();

            if (ttlMs > 0) {
                Duration ttl = Duration.ofMillis(ttlMs);
                HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

                // Store session data as hash
                Map<String, String> sessionData = Map.of(
                        "tokenId", tokenId,
                        "userId", userId,
                        "profileId", profileId != null ? profileId : "",
                        "role", role != null ? role : "",
                        "ipAddress", ipAddress != null ? ipAddress : "",
                        "userAgent", userAgent != null ? userAgent : "",
                        "loginAt", now.format(FORMATTER),
                        "expiresAt", expiresAt.format(FORMATTER)
                );

                hashOps.putAll(SESSION_PREFIX + tokenId, sessionData);
                redisTemplate.expire(SESSION_PREFIX + tokenId, ttl);

                // Store token mapping for revocation
                redisTemplate.opsForValue().set(TOKEN_MAPPING_PREFIX + tokenId, token, ttl);

                // Mark user as online (map userId -> tokenId)
                if (userId != null && !userId.isEmpty()) {
                    redisTemplate.opsForValue().set(ONLINE_KEY_PREFIX + userId, tokenId, ttl);
                }

                log.info("Tracked session {} for user {} with profile {} ({})", 
                        tokenId, userId, profileId, role);
            }
        } catch (Exception e) {
            log.error("Failed to track session", e);
        }
    }

    @Override
    public List<SessionInfo> getAllActiveSessions() {
        List<SessionInfo> sessions = new ArrayList<>();
        Set<String> keys = redisTemplate.keys(SESSION_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            return sessions;
        }

        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

        for (String key : keys) {
            try {
                Map<String, String> data = hashOps.entries(key);
                if (data != null && !data.isEmpty()) {
                    SessionInfo session = SessionInfo.builder()
                            .tokenId(data.get("tokenId"))
                            .userId(data.get("userId"))
                            .profileId(data.getOrDefault("profileId", null))
                            .role(data.getOrDefault("role", null))
                            .ipAddress(data.getOrDefault("ipAddress", null))
                            .userAgent(data.getOrDefault("userAgent", null))
                            .loginAt(LocalDateTime.parse(data.get("loginAt"), FORMATTER))
                            .expiresAt(LocalDateTime.parse(data.get("expiresAt"), FORMATTER))
                            .build();

                    enrichSessionWithUserInfo(session);
                    sessions.add(session);
                }
            } catch (Exception e) {
                log.error("Failed to deserialize session from key: {}", key, e);
            }
        }

        return sessions;
    }

    @Override
    public void removeSession(String tokenId) {
        try {
            // Read session to find userId
            HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
            Map<String, String> data = hashOps.entries(SESSION_PREFIX + tokenId);
            String userId = data != null ? data.get("userId") : null;

            redisTemplate.delete(SESSION_PREFIX + tokenId);
            redisTemplate.delete(TOKEN_MAPPING_PREFIX + tokenId);

            // If presence key points to this tokenId, remove it
            if (userId != null && !userId.isEmpty()) {
                Object current = redisTemplate.opsForValue().get(ONLINE_KEY_PREFIX + userId);
                if (current != null && tokenId.equals(current.toString())) {
                    redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
                }
            }
            log.info("Removed session: {} (user={})", tokenId, userId);
        } catch (Exception e) {
            log.error("Error removing session {}: {}", tokenId, e.getMessage());
        }
    }

    @Override
    public String getTokenByTokenId(String tokenId) {
        Object token = redisTemplate.opsForValue().get(TOKEN_MAPPING_PREFIX + tokenId);
        return token != null ? token.toString() : null;
    }

    // ----- UserPresenceService implementations -----
    @Override
    public void setOnline(String userId, String sessionId) {
        redisTemplate.opsForValue().set(ONLINE_KEY_PREFIX + userId, sessionId);
        log.info("User {} set online -> {}", userId, sessionId);
    }

    @Override
    public void setOffline(String userId) {
        redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
        log.info("User {} set offline", userId);
    }

    @Override
    public boolean isUserOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY_PREFIX + userId));
    }

    @Override
    public java.util.Set<String> getOnlineUsers() {
        Set<String> keys = redisTemplate.keys(ONLINE_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return java.util.Collections.emptySet();
        return keys.stream().map(k -> k.replaceFirst(ONLINE_KEY_PREFIX, "")).collect(Collectors.toSet());
    }

    @Override
    public String getSessionId(String userId) {
        Object v = redisTemplate.opsForValue().get(ONLINE_KEY_PREFIX + userId);
        return v != null ? v.toString() : null;
    }

    @Override
    public boolean forceLogout(String userId) {
        String sessionId = getSessionId(userId);
        if (sessionId != null) {
            // remove session by tokenId
            removeSession(sessionId);
            return true;
        }
        return false;
    }

    private void enrichSessionWithUserInfo(SessionInfo session) {
        // Enrich with user email
        userRepository.findById(session.getUserId()).ifPresent(user -> 
            session.setUserEmail(user.getEmail())
        );

        // Enrich with profile name
        if (session.getProfileId() != null && !session.getProfileId().isEmpty()) {
            userProfileRepository.findById(session.getProfileId()).ifPresent(profile -> {
                String fullName = (profile.getFirstName() != null ? profile.getFirstName() : "") + 
                                 " " + 
                                 (profile.getLastName() != null ? profile.getLastName() : "");
                session.setProfileName(fullName.trim());
            });
        }
    }
}

