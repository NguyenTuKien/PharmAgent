package ct01.n07.backend.model;

import ct01.n07.backend.model.enums.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_devices")
public class UserDevice {
    @Id
    private String id;

    @Indexed
    @Field(targetType = FieldType.OBJECT_ID)
    private String userId; // Trỏ đến users._id

    private String deviceName;

    @Indexed(unique = true)
    private String deviceToken;

    private DeviceType deviceType;
    private boolean isActive;

    @LastModifiedDate
    private Instant lastSeenAt;
}