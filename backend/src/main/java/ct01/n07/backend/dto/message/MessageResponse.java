package ct01.n07.backend.dto.message;

import ct01.n07.backend.model.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String id;
    private String senderId;
    private String receiverId;
    private String content;
    private MessageStatus status;
    private Instant sentAt;
}
