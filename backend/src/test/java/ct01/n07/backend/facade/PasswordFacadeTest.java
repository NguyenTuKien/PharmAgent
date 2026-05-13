package ct01.n07.backend.facade;

import ct01.n07.backend.model.User;
import ct01.n07.backend.producer.MailProducerService;
import ct01.n07.backend.service.UserService;
import ct01.n07.backend.util.OtpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordFacadeTest {

    @Mock
    private UserService userService;

    @Mock
    private OtpUtil otpUtil;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MailProducerService mailProducerService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordFacade passwordFacade;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordFacade, "frontendUrl", "http://localhost:5173");
    }

    @Test
    void forgotPasswordDoesNotRevealMissingEmail() {
        when(userService.findByEmail("missing@example.com"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        passwordFacade.processForgotPassword("missing@example.com");

        verify(otpUtil, never()).generateOtp();
        verify(mailProducerService, never()).sendOtpToQueue(
                eq("missing@example.com"),
                eq("123456"),
                eq("PASSWORD_RESET"),
                eq("http://localhost:5173/reset-password?email=missing%40example.com"));
    }

    @Test
    void forgotPasswordQueuesNamespacedResetOtpForExistingUser() {
        when(userService.findByEmail("user@example.com"))
                .thenReturn(User.builder().id("user-1").email("user@example.com").build());
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        passwordFacade.processForgotPassword("user@example.com");

        verify(valueOperations).set(
                "PASSWORD_RESET:user@example.com",
                "123456",
                Duration.ofMinutes(5));
        verify(mailProducerService).sendOtpToQueue(
                eq("user@example.com"),
                eq("123456"),
                eq("PASSWORD_RESET"),
                eq("http://localhost:5173/reset-password?email=user%40example.com"));
    }
}
