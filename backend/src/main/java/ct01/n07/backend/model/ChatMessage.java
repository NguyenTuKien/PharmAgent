package ct01.n07.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {
    @Id
    private String id;

    @Indexed
    private String roomId;

    @Indexed
    private String senderId;

    private String content;

    // e.g., TEXT, IMAGE, SYSTEM, CALL_LOG
    private String type;

    private List<String> readBy;

    @CreatedDate
    private Instant sentAt;
}
