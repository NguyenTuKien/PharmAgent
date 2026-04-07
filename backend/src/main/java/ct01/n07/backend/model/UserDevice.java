package ct01.n07.backend.model;

import ct01.n07.backend.model.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.mongodb.core.index.Indexed;
import org.bson.types.ObjectId; // Import class này của MongoDB

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDevice {
    @Builder.Default
    private String id = new ObjectId().toString();

    private String deviceName;

    @Indexed(unique = true)
    private String deviceToken;

    private DeviceType deviceType;
    private boolean isActive;

    @LastModifiedBy
    private Instant lastSeenAt;
}