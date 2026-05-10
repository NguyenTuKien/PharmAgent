package ct01.n07.backend.service.impl;

import ct01.n07.backend.dto.user.UserDeviceRequest;
import ct01.n07.backend.dto.user.UserDeviceResponse;
import ct01.n07.backend.model.UserDevice;
import ct01.n07.backend.repository.UserDeviceRepository;
import ct01.n07.backend.security.ProfileAccessContext;
import ct01.n07.backend.service.UserDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDeviceServiceImpl implements UserDeviceService {

    private final ProfileAccessContext profileAccessContext;
    private final UserDeviceRepository userDeviceRepository;

    @Override
    public UserDeviceResponse addDevice(UserDeviceRequest request) {
        String userId = profileAccessContext.getCurrentUserId();
        String deviceToken = request.getDeviceToken();

        Optional<UserDevice> existingByToken = userDeviceRepository.findByDeviceToken(deviceToken);
        UserDevice device;

        if (existingByToken.isPresent()) {
            device = existingByToken.get();
            if (!userId.equals(device.getUserId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Device token is already registered to another user");
            }

            device.setDeviceName(request.getDeviceName());
            device.setDeviceType(request.getDeviceType());
            device.setActive(request.isActive());
            device.setLastSeenAt(Instant.now());
        } else {
            device = UserDevice.builder()
                    .userId(userId)
                    .deviceName(request.getDeviceName())
                    .deviceToken(deviceToken)
                    .deviceType(request.getDeviceType())
                    .isActive(request.isActive())
                    .lastSeenAt(Instant.now())
                    .build();
        }

        return toResponse(userDeviceRepository.save(device));
    }

    @Override
    public UserDeviceResponse updateDevice(String deviceId, UserDeviceRequest request) {
        String userId = profileAccessContext.getCurrentUserId();
        String deviceToken = request.getDeviceToken();

        UserDevice device = userDeviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        userDeviceRepository.findByDeviceToken(deviceToken)
                .filter(existing -> !existing.getId().equals(deviceId))
                .filter(existing -> !existing.getUserId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Device token is already registered to another user");
                });

        device.setDeviceName(request.getDeviceName());
        device.setDeviceToken(deviceToken);
        device.setDeviceType(request.getDeviceType());
        device.setActive(request.isActive());
        device.setLastSeenAt(Instant.now());

        return toResponse(userDeviceRepository.save(device));
    }

    @Override
    public void deleteDevice(String deviceId) {
        String userId = profileAccessContext.getCurrentUserId();

        UserDevice device = userDeviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        userDeviceRepository.delete(device);
    }

    @Override
    public List<UserDeviceResponse> getMyDevices() {
        String userId = profileAccessContext.getCurrentUserId();
        return userDeviceRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private UserDeviceResponse toResponse(UserDevice device) {
        UserDeviceResponse response = new UserDeviceResponse();
        response.setId(device.getId());
        response.setUserId(device.getUserId());
        response.setDeviceName(device.getDeviceName());
        response.setDeviceToken(device.getDeviceToken());
        response.setDeviceType(device.getDeviceType());
        response.setActive(device.isActive());
        response.setLastSeenAt(device.getLastSeenAt());
        return response;
    }
}
