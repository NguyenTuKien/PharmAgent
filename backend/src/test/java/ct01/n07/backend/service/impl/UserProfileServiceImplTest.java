package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.user.UpdateAvatarRequest;
import ct01.n07.backend.dto.user.UserProfileResponse;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.model.enums.Role;
import ct01.n07.backend.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void updateAvatarTrimsUrlBeforeSaving() {
        UserProfile profile = UserProfile.builder()
                .id("profile-1")
                .userId("user-1")
                .role(Role.CAREGIVER)
                .build();
        UpdateAvatarRequest request = new UpdateAvatarRequest();
        request.setAvatarUrl("  https://cdn.example.com/avatar.png  ");

        arrangeCurrentProfile(profile);
        when(userProfileRepository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userProfileMapper.toResponse(any(UserProfile.class))).thenReturn(new UserProfileResponse());

        userProfileService.updateAvatar(request);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        org.mockito.Mockito.verify(userProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getAvatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
    }

    @Test
    void updateAvatarStoresNullWhenAvatarIsCleared() {
        UserProfile profile = UserProfile.builder()
                .id("profile-1")
                .userId("user-1")
                .role(Role.CAREGIVER)
                .avatarUrl("https://cdn.example.com/old.png")
                .build();
        UpdateAvatarRequest request = new UpdateAvatarRequest();
        request.setAvatarUrl(null);

        arrangeCurrentProfile(profile);
        when(userProfileRepository.findById("profile-1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userProfileMapper.toResponse(any(UserProfile.class))).thenReturn(new UserProfileResponse());

        userProfileService.updateAvatar(request);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        org.mockito.Mockito.verify(userProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getAvatarUrl()).isNull();
    }

    private void arrangeCurrentProfile(UserProfile profile) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(profile.getUserId(), "password", List.of()));
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute("profileId", profile.getId());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
    }
}
