package ct01.n07.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "spring.rabbitmq")
public class RabbitMqProps {
    private String addresses;
    private String host;
    private int port;
    private String username;
    private String password;
    private String virtualHost;
}