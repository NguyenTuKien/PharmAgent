package ct01.n07.backend.service.impl;

import ct01.n07.backend.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPresenceServiceImpl implements UserPresenceService {

    private final StringRedisTemplate redisTemplate;
    private static final String ONLINE_KEY_PREFIX = "user:online:";

    @Override
    public void setOnline(String userId, String sessionId) {
        redisTemplate.opsForValue().set(ONLINE_KEY_PREFIX + userId, sessionId, Duration.ofHours(24));
        log.info("User {} is now ONLINE with session {}", userId, sessionId);
    }

    @Override
    public void setOffline(String userId) {
        redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
        log.info("User {} is now OFFLINE", userId);
    }

    @Override
    public boolean isUserOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY_PREFIX + userId));
    }
}

