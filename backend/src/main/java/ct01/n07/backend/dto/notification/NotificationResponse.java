package ct01.n07.backend.dto.notification;

import ct01.n07.backend.model.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private String senderId;
    private String receiverId;
    private String content;
    private NotificationStatus status;
    private Instant sentAt;
}
