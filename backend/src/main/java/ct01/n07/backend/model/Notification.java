package ct01.n07.backend.model;

import ct01.n07.backend.model.enums.NotificationStatus;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;

@Data
@Builder
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;

    @Indexed
    private String senderId;

    @Indexed
    private String receiverId;

    @CreatedDate
    private Instant sentAt;

    private String content;

    private NotificationStatus status;
    private String errorMessage;
}