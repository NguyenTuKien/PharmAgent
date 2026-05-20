package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.ResetPasswordRequest;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.producer.MailProducerService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.service.UserService;
import ct01.n07.backend.util.ResetTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordFacadeTest {

    @Mock
    private UserService userService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private ResetTokenUtil resetTokenUtil;

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
    void forgotPasswordRejectsMissingEmailBeforeQueueingResetMail() {
        when(userService.findByEmail("missing@example.com"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản chưa tồn tại"));

        assertThatThrownBy(() -> passwordFacade.processForgotPassword("missing@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tài khoản chưa tồn tại");

        verify(resetTokenUtil, never()).generateUrlToken();
        verifyNoInteractions(mailProducerService);
    }

    @Test
    void forgotPasswordQueuesNamespacedResetTokenForExistingUserForTwelveHours() {
        when(userService.findByEmail("user@example.com"))
                .thenReturn(User.builder().id("user-1").email("user@example.com").build());
        when(resetTokenUtil.generateUrlToken()).thenReturn("reset-token-abc");
        when(userProfileService.findAllByUserId("user-1", org.springframework.data.domain.PageRequest.of(0, 1)))
                .thenReturn(new PageImpl<>(java.util.List.of(UserProfile.builder()
                        .firstName("An")
                        .lastName("Nguyen")
                        .build())));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        passwordFacade.processForgotPassword("user@example.com");

        verify(valueOperations).set(
                "PASSWORD_RESET:user@example.com",
                "reset-token-abc",
                Duration.ofHours(12));
        verify(mailProducerService).sendOtpToQueue(
                eq("user@example.com"),
                eq("reset-token-abc"),
                eq("PASSWORD_RESET"),
                eq("http://localhost:5173/reset-password?email=user%40example.com&token=reset-token-abc"),
                eq("An Nguyen"));
    }

    @Test
    void resetPasswordUsesLinkTokenAndDeletesItAfterSuccess() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("USER@example.com");
        request.setToken("reset-token-abc");
        request.setNewPassword("new-password");
        request.setConfirmPassword("new-password");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("PASSWORD_RESET:user@example.com")).thenReturn("reset-token-abc");

        passwordFacade.resetPassword(request);

        verify(redisTemplate).delete("PASSWORD_RESET:user@example.com");
        verify(userService).updatePassword("user@example.com", "new-password");
    }

    @Test
    void resetPasswordRejectsMissingLinkToken() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("user@example.com");
        request.setNewPassword("new-password");
        request.setConfirmPassword("new-password");

        assertThatThrownBy(() -> passwordFacade.resetPassword(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");

        verify(userService, never()).updatePassword(eq("user@example.com"), eq("new-password"));
    }
}
