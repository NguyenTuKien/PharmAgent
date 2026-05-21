package ct01.n07.backend.controller;

import ct01.n07.backend.dto.chat.SignalPayload;
import ct01.n07.backend.model.UserProfile;
import ct01.n07.backend.repository.UserProfileRepository;
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
public class SignalingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserProfileRepository userProfileRepository;

    @MessageMapping("/call.signal")
    public void processSignal(@Payload SignalPayload signalPayload, Principal principal) {
        validateSignal(signalPayload, principal);
        log.info("Received WebRTC signal {} from {} to {}", 
                signalPayload.getType(), signalPayload.getSenderId(), signalPayload.getReceiverId());
        
        // Forward signal directly to receiver via RabbitMQ routing
        // This sends to queue: /queue/user.{receiverId}.call
        String destination = "/queue/user." + signalPayload.getReceiverId() + ".call";
        messagingTemplate.convertAndSend(destination, signalPayload);
    }

    private void validateSignal(SignalPayload payload, Principal principal) {
        if (principal == null) {
            throw new MessagingException("Unauthenticated call signal");
        }
        if (payload.getSenderId() == null || payload.getSenderId().isBlank()
                || payload.getReceiverId() == null || payload.getReceiverId().isBlank()
                || payload.getType() == null) {
            throw new MessagingException("Invalid call signal");
        }

        UserProfile senderProfile = userProfileRepository.findById(payload.getSenderId())
                .orElseThrow(() -> new MessagingException("Unknown sender profile"));
        if (senderProfile.getUserId() == null || !senderProfile.getUserId().equals(principal.getName())) {
            throw new MessagingException("Sender profile does not belong to current user");
        }
    }
}
