package ct01.n07.backend.service.impl;

import ct01.n07.backend.mapper.UserMapper;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.enums.UserStatus;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void verifyUserCredentialsRejectsMissingAccountWithClearMessage() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.verifyUserCredentials("missing@example.com", "Password123!"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Tài khoản chưa tồn tại");
                });
    }

    @Test
    void verifyUserCredentialsRejectsWrongPasswordWithClearMessage() {
        User activeUser = User.builder()
                .email("user@example.com")
                .password("hash")
                .userStatus(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.verifyUserCredentials("user@example.com", "wrong-password"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getReason()).isEqualTo("Mật khẩu không đúng");
                });
    }

    @Test
    void verifyUserCredentialsRejectsInactiveAccountBeforeIssuingTokens() {
        User inactiveUser = User.builder()
                .email("user@example.com")
                .password("hash")
                .userStatus(UserStatus.INACTIVE)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.verifyUserCredentials("user@example.com", "Password123!"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("xác minh email");
    }
}
