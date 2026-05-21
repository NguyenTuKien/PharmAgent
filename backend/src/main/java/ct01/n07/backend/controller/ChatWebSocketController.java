package ct01.n07.backend.controller;

import ct01.n07.backend.dto.chat.ChatPayload;
import ct01.n07.backend.model.ChatMessage;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.repository.UserProfileRepository;
import ct01.n07.backend.service.ChatMessageService;
import ct01.n07.backend.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final UserProfileRepository userProfileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatPayload chatPayload, Principal principal) {
        validateChatMessage(chatPayload, principal);
        log.info("Received chat message from user {} to room {}", chatPayload.getSenderId(), chatPayload.getRoomId());
        
        // Save to DB
        ChatMessage savedMessage = chatMessageService.saveMessage(chatPayload);

        // Broadcast to the specific topic using RabbitMQ STOMP routing
        String destination = "/topic/room." + chatPayload.getRoomId();
        messagingTemplate.convertAndSend(destination, savedMessage);
    }

    private void validateChatMessage(ChatPayload payload, Principal principal) {
        if (principal == null) {
            throw new MessagingException("Unauthenticated chat sender");
        }
        if (payload.getRoomId() == null || payload.getRoomId().isBlank()
                || payload.getSenderId() == null || payload.getSenderId().isBlank()) {
            throw new MessagingException("Missing chat room or sender");
        }
        if (payload.getContent() == null || payload.getContent().isBlank()) {
            throw new MessagingException("Message content is required");
        }

        UserProfile senderProfile = userProfileRepository.findById(payload.getSenderId())
                .orElseThrow(() -> new MessagingException("Unknown sender profile"));
        if (senderProfile.getUserId() == null || !senderProfile.getUserId().equals(principal.getName())) {
            throw new MessagingException("Sender profile does not belong to current user");
        }
        if (!chatRoomService.isUserInRoom(payload.getRoomId(), payload.getSenderId())) {
            throw new MessagingException("Sender is not a participant of this room");
        }
    }
}
