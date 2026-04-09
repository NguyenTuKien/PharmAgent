package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.user.UserDeviceRequest;
import ct01.n07.backend.dto.user.UserProfileResponse;
import ct01.n07.backend.mapper.UserProfileMapper;
import ct01.n07.backend.model.UserDevice;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.security.ProfileAccessContext;
import ct01.n07.backend.service.UserDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDeviceServiceImpl implements UserDeviceService {

    private final ProfileAccessContext profileAccessContext;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    @Override
    public UserProfileResponse addDevice(UserDeviceRequest request) {
        UserProfile profile = profileAccessContext.getCurrentUserProfile();
        if (profile.getUserDevices() == null) {
            profile.setUserDevices(new ArrayList<>());
        }

        java.util.Optional<UserDevice> existing = profile.getUserDevices().stream()
                .filter(d -> d.getDeviceToken().equals(request.getDeviceToken()))
                .findFirst();

        if (existing.isPresent()) {
            UserDevice d = existing.get();
            d.setDeviceName(request.getDeviceName());
            d.setDeviceType(request.getDeviceType());
            d.setActive(request.isActive());
            d.setLastSeenAt(Instant.now());
        } else {
            UserDevice device = UserDevice.builder()
                    .deviceName(request.getDeviceName())
                    .deviceToken(request.getDeviceToken())
                    .deviceType(request.getDeviceType())
                    .isActive(request.isActive())
                    .lastSeenAt(Instant.now())
                    .build();
            profile.getUserDevices().add(device);
        }
        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public UserProfileResponse updateDevice(String deviceId, UserDeviceRequest request) {
        UserProfile profile = profileAccessContext.getCurrentUserProfile();
        List<UserDevice> devices = profile.getUserDevices();
        if (devices == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Device list is empty");
        }
        UserDevice deviceToUpdate = devices.stream()
                .filter(d -> d.getId().equals(deviceId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        deviceToUpdate.setDeviceName(request.getDeviceName());
        deviceToUpdate.setDeviceToken(request.getDeviceToken());
        deviceToUpdate.setDeviceType(request.getDeviceType());
        deviceToUpdate.setActive(request.isActive());
        deviceToUpdate.setLastSeenAt(Instant.now());

        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public UserProfileResponse deleteDevice(String deviceId) {
        UserProfile profile = profileAccessContext.getCurrentUserProfile();
        List<UserDevice> devices = profile.getUserDevices();
        if (devices == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Device list is empty");
        }
        boolean removed = devices.removeIf(d -> d.getId().equals(deviceId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found");
        }
        return userProfileMapper.toResponse(userProfileRepository.save(profile));
    }

    @Override
    public List<UserDevice> getMyDevices() {
        UserProfile profile = profileAccessContext.getCurrentUserProfile();
        return profile.getUserDevices() != null ? profile.getUserDevices() : new ArrayList<>();
    }
}
