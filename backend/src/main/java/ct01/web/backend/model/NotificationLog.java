package ct01.web.backend.model;

import ct01.web.backend.model.enums.NotificationStatus;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "notification_logs")
public class NotificationLog {
    @Id
    private String id;

    @Indexed
    @Field(targetType = FieldType.OBJECT_ID)
    private String doseEventId;

    private String deviceToken; // Lưu token trực tiếp thay vì ID vì thiết bị ở DB NoSQL bị nhúng vào User

    @CreatedDate
    private Instant sentAt;

    private NotificationStatus status;
    private String errorMessage;
}