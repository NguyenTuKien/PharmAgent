package ct01.n07.backend.dto.user;

import ct01.n07.backend.model.enums.DeviceType;
import lombok.Data;

import java.time.Instant;

@Data
public class UserDeviceResponse {
    private String id;
    private String userId;
    private String deviceName;
    private String deviceToken;
    private DeviceType deviceType;
    private boolean isActive;
    private Instant lastSeenAt;
}
