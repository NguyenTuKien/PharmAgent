package ct01.n07.backend.service;

import ct01.n07.backend.dto.chat.ChatPayload;
import ct01.n07.backend.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatMessageService {
    ChatMessage saveMessage(ChatPayload payload);
    Page<ChatMessage> getRoomMessages(String roomId, Pageable pageable);
    void markRoomAsRead(String roomId, String profileId);
}
