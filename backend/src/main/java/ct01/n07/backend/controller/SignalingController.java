package ct01.n07.backend.controller;

import ct01.n07.backend.dto.chat.SignalPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SignalingController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/call.signal")
    public void processSignal(@Payload SignalPayload signalPayload) {
        log.info("Received WebRTC signal {} from {} to {}", 
                signalPayload.getType(), signalPayload.getSenderId(), signalPayload.getReceiverId());
        
        // Forward signal directly to receiver via RabbitMQ routing
        // This sends to queue: /queue/user.{receiverId}/call
        String destination = "/queue/user." + signalPayload.getReceiverId() + "/call";
        messagingTemplate.convertAndSend(destination, signalPayload);
        
        // Note: For production, we should authenticate whether senderId == STOMP token principal
    }
}
