package ct01.n07.backend.service;

import ct01.n07.backend.dto.userProfile.UserDeviceRequest;
import ct01.n07.backend.dto.userProfile.UserProfileResponse;
import ct01.n07.backend.model.UserDevice;

import java.util.List;

public interface UserDeviceService {
    UserProfileResponse addDevice(UserDeviceRequest request);
    UserProfileResponse updateDevice(String deviceId, UserDeviceRequest request);
    UserProfileResponse deleteDevice(String deviceId);
    List<UserDevice> getMyDevices();
}
