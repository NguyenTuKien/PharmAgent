package ct01.n07.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPresenceService {

    private final StringRedisTemplate redisTemplate;
    
    private static final String ONLINE_KEY_PREFIX = "user:online:";

    public void setOnline(String userId, String sessionId) {
        // Có thể lưu sessionId làm giá trị để tiện theo dõi
        redisTemplate.opsForValue().set(ONLINE_KEY_PREFIX + userId, sessionId, Duration.ofHours(24));
        log.info("User {} is now ONLINE with session {}", userId, sessionId);
    }

    public void setOffline(String userId) {
        redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
        log.info("User {} is now OFFLINE", userId);
    }

    public boolean isUserOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY_PREFIX + userId));
    }
}
