package ct01.n07.backend.facade;

import ct01.n07.backend.dto.auth.LoginRequest;
import ct01.n07.backend.dto.auth.LoginResponse;
import ct01.n07.backend.dto.auth.SignupRequest;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.User;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.service.RelationshipService;
import ct01.n07.backend.service.UserProfileService;
import ct01.n07.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationFacade {

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final RelationshipService relationshipService;
    private final UserProfileMapper userProfileMapper;
    private final AuthFacade authFacade;

    @Transactional
    public LoginResponse signup(SignupRequest signupRequest, Pageable pageable) {
        if (!signupRequest.getPassword().equals(signupRequest.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu và xác nhận mật khẩu không khớp");
        }

        User user;
        LoginRequest loginRequest = LoginRequest.builder()
                .email(signupRequest.getEmail())
                .password(signupRequest.getPassword())
                .build();

        if (signupRequest.getCaregiver() != null && userProfileService.findByPhone(signupRequest.getCaregiver().getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caregiver phone number already exists");
        }

        if (signupRequest.getElderly() != null && !signupRequest.getElderly().getPhone().isBlank()) {
            if (userProfileService.findByPhone(signupRequest.getElderly().getPhone())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elderly phone number already exists");
            }
        }

        try {
            user = userService.createUser(loginRequest);
        } catch (Exception e) {
            user = userService.verifyUserCredentials(loginRequest.getEmail(), loginRequest.getPassword());
        }

        UserProfile caregiverProfile = userProfileMapper.toCaregiverProfile(signupRequest, user.getId());
        userProfileService.saveUserProfile(caregiverProfile);

        if (signupRequest.getElderly() != null) {
            UserProfile elderlyProfile = userProfileMapper.toElderlyProfile(signupRequest, user.getId());
            userProfileService.saveUserProfile(elderlyProfile);
            relationshipService.createRelationship(
                    caregiverProfile.getId(),
                    elderlyProfile.getId(),
                    signupRequest.getElderly().getCaregiverTitle(),
                    signupRequest.getElderly().getElderlyTitle(),
                    signupRequest.getElderly().getPermissionLevel()
            );
        }

        return authFacade.login(loginRequest, pageable);
    }
}
