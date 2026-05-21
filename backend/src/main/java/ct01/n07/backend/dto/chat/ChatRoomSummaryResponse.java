package ct01.n07.backend.dto.chat;

import ct01.n07.backend.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomSummaryResponse {
    private String id;
    private String type;
    private List<String> participantIds;
    private List<ChatParticipantResponse> participants;
    private ChatParticipantResponse peerProfile;
    private ChatMessage lastMessage;
    private long unreadCount;
    private Instant createdAt;
    private Instant updatedAt;
}
