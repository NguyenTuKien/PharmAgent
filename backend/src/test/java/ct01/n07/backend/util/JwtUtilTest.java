package ct01.n07.backend.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String TEST_SECRET = Base64.getEncoder()
            .encodeToString("12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8));

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(JwtUtil.class)
            .withPropertyValues("jwt.secret=" + TEST_SECRET);

    @Test
    void defaultAuthAndAccessTokenLifetimeIsFifteenMinutes() {
        contextRunner.run(context -> {
            JwtUtil jwtUtil = context.getBean(JwtUtil.class);

            String authToken = jwtUtil.generateAuthToken("user-1");
            String accessToken = jwtUtil.generateAccessToken("user-1", "profile-1", "CAREGIVER");

            assertThat(tokenLifetimeMs(jwtUtil, authToken)).isEqualTo(900_000L);
            assertThat(tokenLifetimeMs(jwtUtil, accessToken)).isEqualTo(900_000L);
        });
    }

    private long tokenLifetimeMs(JwtUtil jwtUtil, String token) {
        return jwtUtil.extractExpiration(token).getTime() - jwtUtil.extractIssuedAt(token).getTime();
    }
}
