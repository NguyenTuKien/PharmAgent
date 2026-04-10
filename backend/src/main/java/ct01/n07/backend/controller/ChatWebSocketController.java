package ct01.n07.backend.controller;

import ct01.n07.backend.dto.chat.ChatPayload;
import ct01.n07.backend.model.ChatMessage;
import ct01.n07.backend.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatPayload chatPayload) {
        log.info("Received chat message from user {} to room {}", chatPayload.getSenderId(), chatPayload.getRoomId());
        
        // Save to DB
        ChatMessage savedMessage = chatMessageService.saveMessage(chatPayload);

        // Broadcast to the specific topic using RabbitMQ STOMP routing
        String destination = "/topic/room." + chatPayload.getRoomId();
        messagingTemplate.convertAndSend(destination, savedMessage);
    }
}
