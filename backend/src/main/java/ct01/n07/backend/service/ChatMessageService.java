package ct01.n07.backend.service;

import ct01.n07.backend.dto.chat.ChatPayload;
import ct01.n07.backend.model.ChatMessage;
import ct01.n07.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;

    public ChatMessage saveMessage(ChatPayload payload) {
        ChatMessage message = ChatMessage.builder()
                .roomId(payload.getRoomId())
                .senderId(payload.getSenderId())
                .content(payload.getContent())
                .type(payload.getType() != null ? payload.getType().name() : "TEXT")
                .sentAt(Instant.now())
                .readBy(Collections.singletonList(payload.getSenderId()))
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        
        // Update room's last message ID
        chatRoomService.updateLastMessage(payload.getRoomId(), saved.getId());
        
        return saved;
    }

    public Page<ChatMessage> getRoomMessages(String roomId, Pageable pageable) {
        return chatMessageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable);
    }
}
