package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.AuthMessageResponse;
import ct01.n07.backend.dto.auth.LoginRequest;
import ct01.n07.backend.dto.auth.RegisterRequest;
import ct01.n07.backend.dto.auth.VerifyEmailRequest;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.FamilyRelation;
import ct01.n07.backend.model.enums.Gender;
import ct01.n07.backend.model.enums.PermissionLevel;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.model.enums.UserStatus;
import ct01.n07.backend.producer.MailProducerService;
import ct01.n07.backend.security.JwtService;
import ct01.n07.backend.service.RelationshipService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.service.UserService;
import ct01.n07.backend.util.OtpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationFacadeTest {

    @Mock
    private UserService userService;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private OtpUtil otpUtil;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MailProducerService mailProducerService;

    @InjectMocks
    private RegistrationFacade registrationFacade;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(registrationFacade, "frontendUrl", "http://localhost:5173");
    }

    @Test
    void registerCreatesInactiveUserAndQueuesVerificationOtp() {
        RegisterRequest request = registerRequest();
        User user = User.builder()
                .id("user-1")
                .email("caregiver@example.com")
                .userStatus(UserStatus.INACTIVE)
                .build();
        UserProfile caregiverProfile = UserProfile.builder()
                .id("profile-1")
                .userId("user-1")
                .firstName("An")
                .lastName("Nguyen")
                .role(Role.CAREGIVER)
                .build();

        when(userService.createUser(any(LoginRequest.class), eq(UserStatus.INACTIVE))).thenReturn(user);
        when(userProfileMapper.toCaregiverProfile(request, "user-1")).thenReturn(caregiverProfile);
        when(jwtService.generateAuthToken("user-1")).thenReturn("onboarding-token");
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthMessageResponse response = registrationFacade.register(request);

        assertThat(response.getEmail()).isEqualTo("caregiver@example.com");
        assertThat(response.getOnboardingToken()).isEqualTo("onboarding-token");
        assertThat(response.getMessage()).contains("xác minh");
        verify(userProfileService).saveUserProfile(caregiverProfile);
        verify(valueOperations).set(
                "VERIFY_EMAIL:caregiver@example.com",
                "123456",
                Duration.ofMinutes(15));
        verify(mailProducerService).sendOtpToQueue(
                eq("caregiver@example.com"),
                eq("123456"),
                eq("EMAIL_VERIFICATION"),
                eq("http://localhost:5173/verify-email?email=caregiver%40example.com&otp=123456"),
                eq("An Nguyen"));
    }

    @Test
    void verifyEmailActivatesInactiveUserWithValidOtp() {
        User user = User.builder()
                .id("user-1")
                .email("caregiver@example.com")
                .userStatus(UserStatus.INACTIVE)
                .build();
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail("caregiver@example.com");
        request.setOtp("123456");

        when(userService.findByEmail("caregiver@example.com")).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("VERIFY_EMAIL:caregiver@example.com")).thenReturn("123456");

        AuthMessageResponse response = registrationFacade.verifyEmail(request);

        assertThat(response.getEmail()).isEqualTo("caregiver@example.com");
        assertThat(response.getMessage()).contains("xác minh");
        verify(redisTemplate).delete("VERIFY_EMAIL:caregiver@example.com");
        verify(userService).updateStatus("user-1", UserStatus.ACTIVE);
    }

    @Test
    void registerNormalizesEmailBeforePersistingUserAndOtp() {
        RegisterRequest request = registerRequest();
        request.setEmail("  CareGiver@Example.COM  ");
        User user = User.builder()
                .id("user-1")
                .email("caregiver@example.com")
                .userStatus(UserStatus.INACTIVE)
                .build();
        UserProfile caregiverProfile = UserProfile.builder()
                .id("profile-1")
                .userId("user-1")
                .role(Role.CAREGIVER)
                .build();
        ArgumentCaptor<LoginRequest> loginCaptor = ArgumentCaptor.forClass(LoginRequest.class);

        when(userService.createUser(loginCaptor.capture(), eq(UserStatus.INACTIVE))).thenReturn(user);
        when(userProfileMapper.toCaregiverProfile(request, "user-1")).thenReturn(caregiverProfile);
        when(jwtService.generateAuthToken("user-1")).thenReturn("onboarding-token");
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        registrationFacade.register(request);

        assertThat(loginCaptor.getValue().getEmail()).isEqualTo("caregiver@example.com");
        verify(valueOperations).set(
                eq("VERIFY_EMAIL:caregiver@example.com"),
                eq("123456"),
                eq(Duration.ofMinutes(15)));
    }

    @Test
    void registerWithElderlyCreatesRelationshipWithFamilyRelationAndFullManagement() {
        RegisterRequest request = registerRequest();
        RegisterRequest.ElderlyRegisterRequest elderly = RegisterRequest.ElderlyRegisterRequest.builder()
                .firstName("Binh")
                .lastName("Nguyen")
                .phone("0987654321")
                .dateOfBirth(LocalDate.of(1948, 6, 2))
                .gender(Gender.MALE)
                .relation(FamilyRelation.FATHER)
                .build();
        request.setElderly(elderly);
        User user = User.builder()
                .id("user-1")
                .email("caregiver@example.com")
                .userStatus(UserStatus.INACTIVE)
                .build();
        UserProfile caregiverProfile = UserProfile.builder()
                .id("caregiver-1")
                .userId("user-1")
                .firstName("An")
                .lastName("Nguyen")
                .role(Role.CAREGIVER)
                .build();
        UserProfile elderlyProfile = UserProfile.builder()
                .id("elderly-1")
                .userId("user-1")
                .role(Role.ELDERLY)
                .build();

        when(userService.createUser(any(LoginRequest.class), eq(UserStatus.INACTIVE))).thenReturn(user);
        when(userProfileMapper.toCaregiverProfile(request, "user-1")).thenReturn(caregiverProfile);
        when(userProfileMapper.toElderlyProfile(request, "user-1")).thenReturn(elderlyProfile);
        when(userProfileService.saveUserProfile(caregiverProfile)).thenReturn(caregiverProfile);
        when(userProfileService.saveUserProfile(elderlyProfile)).thenReturn(elderlyProfile);
        when(jwtService.generateAuthToken("user-1")).thenReturn("onboarding-token");
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        registrationFacade.register(request);

        verify(relationshipService).createRelationship(
                "caregiver-1",
                "elderly-1",
                FamilyRelation.FATHER,
                null,
                PermissionLevel.MANAGE_ALL);
    }

    private RegisterRequest registerRequest() {
        RegisterRequest.CaregiverRegisterRequest caregiver = RegisterRequest.CaregiverRegisterRequest.builder()
                .firstName("An")
                .lastName("Nguyen")
                .phone("0912345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .address("Ha Noi")
                .build();

        return RegisterRequest.builder()
                .email("caregiver@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .caregiver(caregiver)
                .build();
    }
}
