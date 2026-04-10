package ct01.n07.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import lombok.RequiredArgsConstructor;
import java.net.URI;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompChannelInterceptor stompChannelInterceptor;
    private final RabbitMqProps rabbitMqProps;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");

        if (rabbitMqProps.getAddresses() == null || rabbitMqProps.getAddresses().isEmpty()) {
            log.warn("RabbitMQ addresses is empty. STOMP broker relay will not be configured.");
            return;
        }

        try {
            // "Phẫu thuật" chuỗi addresses (ví dụ: amqp://user:pass@host:port/vhost)
            // Chúng ta thay thế amqp thành http để lớp URI của Java parse được userInfo
            String uriString = rabbitMqProps.getAddresses().replace("amqp://", "http://").replace("amqps://", "https://");
            URI uri = new URI(uriString);

            String host = uri.getHost();
            String userInfo = uri.getUserInfo();
            String user = userInfo.split(":")[0];
            String pass = userInfo.split(":")[1];
            String vhost = uri.getPath().substring(1); // Bỏ dấu / ở đầu

            config.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(host)
                    .setRelayPort(61613) // Port STOMP mặc định
                    .setClientLogin(user)
                    .setClientPasscode(pass)
                    .setSystemLogin(user)
                    .setSystemPasscode(pass)
                    .setVirtualHost(vhost)
                    .setSystemHeartbeatSendInterval(10000)
                    .setSystemHeartbeatReceiveInterval(10000);

        } catch (Exception e) {
            throw new RuntimeException("Không thể parse RabbitMQ addresses: " + e.getMessage());
        }
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompChannelInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
}