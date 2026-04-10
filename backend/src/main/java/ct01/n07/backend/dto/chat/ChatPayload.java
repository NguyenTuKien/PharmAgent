package ct01.n07.backend.dto.chat;

import ct01.n07.backend.model.enums.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatPayload {
    private String roomId;
    private String senderId;
    private String content;
    private ChatMessageType type;
}
