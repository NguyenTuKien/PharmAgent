package ct01.n07.backend.config;

import ct01.n07.backend.security.JwtService;
import ct01.n07.backend.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserPresenceService userPresenceService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            String sessionId = accessor.getSessionId();

            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        String userId = jwtService.extractUserId(token);
                        if (jwtService.isTokenValid(token, userId)) {
                            accessor.setUser(new java.security.Principal() {
                                @Override
                                public String getName() {
                                    return userId;
                                }
                            });
                            userPresenceService.setOnline(userId, sessionId);
                        } else {
                            throw new IllegalArgumentException("Invalid JWT token in STOMP connection");
                        }
                    } catch (Exception e) {
                        log.error("STOMP connect authentication failed: {}", e.getMessage());
                    }
                }
            } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                if (accessor.getUser() != null) {
                    userPresenceService.setOffline(accessor.getUser().getName());
                }
            }
        }
        return message;
    }
}
