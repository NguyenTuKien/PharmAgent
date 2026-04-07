package ct01.n07.backend.dto.userProfile;

import ct01.n07.backend.model.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserDeviceRequest {
    @NotBlank(message = "Device name is required")
    private String deviceName;

    @NotBlank(message = "Device token is required")
    private String deviceToken;

    @NotNull(message = "Device type is required")
    private DeviceType deviceType;

    private boolean active = true;
}
