package ct01.n07.backend.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void createTokenStoresOnlyHashedTokenServerSide() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RefreshTokenService service = new RefreshTokenService(redisTemplate);

        String token = service.createToken("user-1");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), eq("user-1"));
        assertThat(token).hasSizeGreaterThanOrEqualTo(43);
        assertThat(keyCaptor.getValue()).startsWith("refresh-session:");
        assertThat(keyCaptor.getValue()).doesNotContain(token);
    }

    @Test
    void resolveUserIdLooksUpTheHashedToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("user-1");
        RefreshTokenService service = new RefreshTokenService(redisTemplate);

        assertThat(service.resolveUserId("refresh-token")).contains("user-1");
    }

    @Test
    void revokeDeletesTheHashedTokenKey() {
        RefreshTokenService service = new RefreshTokenService(redisTemplate);

        service.revoke("refresh-token");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).delete(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).startsWith("refresh-session:");
        assertThat(keyCaptor.getValue()).doesNotContain("refresh-token");
    }
}
