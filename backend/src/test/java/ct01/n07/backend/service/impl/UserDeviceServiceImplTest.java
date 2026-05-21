package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.user.UserDeviceRequest;
import ct01.n07.backend.model.UserDevice;
import ct01.n07.backend.model.enums.DeviceType;
import ct01.n07.backend.repository.UserDeviceRepository;
import ct01.n07.backend.security.ProfileAccessContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDeviceServiceImplTest {

    @Mock
    private ProfileAccessContext profileAccessContext;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private UserDeviceServiceImpl userDeviceService;

    @Test
    void updateDeviceRejectsTokenUsedByAnotherDeviceOwnedBySameUser() {
        UserDevice firstDevice = UserDevice.builder()
                .id("device-1")
                .userId("user-1")
                .deviceName("Phone")
                .deviceToken("token-one")
                .deviceType(DeviceType.IOS)
                .isActive(true)
                .build();
        UserDevice secondDevice = UserDevice.builder()
                .id("device-2")
                .userId("user-1")
                .deviceName("Browser")
                .deviceToken("token-two")
                .deviceType(DeviceType.WEB)
                .isActive(true)
                .build();
        UserDeviceRequest request = new UserDeviceRequest();
        request.setDeviceName("Browser");
        request.setDeviceToken("token-one");
        request.setDeviceType(DeviceType.WEB);
        request.setActive(true);

        when(profileAccessContext.getCurrentUserId()).thenReturn("user-1");
        when(userDeviceRepository.findByIdAndUserId("device-2", "user-1")).thenReturn(Optional.of(secondDevice));
        when(userDeviceRepository.findByDeviceToken("token-one")).thenReturn(Optional.of(firstDevice));

        assertThatThrownBy(() -> userDeviceService.updateDevice("device-2", request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Device token is already registered");
                });
        verify(userDeviceRepository, never()).save(any(UserDevice.class));
    }
}
