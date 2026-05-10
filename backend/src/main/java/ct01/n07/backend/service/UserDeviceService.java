package ct01.n07.backend.service;

import ct01.n07.backend.dto.user.UserDeviceRequest;
import ct01.n07.backend.dto.user.UserDeviceResponse;

import java.util.List;

public interface UserDeviceService {
    UserDeviceResponse addDevice(UserDeviceRequest request);
    UserDeviceResponse updateDevice(String deviceId, UserDeviceRequest request);
    void deleteDevice(String deviceId);
    List<UserDeviceResponse> getMyDevices();
}
