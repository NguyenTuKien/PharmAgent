package ct01.n07.backend.producer;

import ct01.n07.backend.config.RabbitMQConfig;
import ct01.n07.backend.dto.auth.OtpMailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class MailProducerService {

    private static final Logger log = LoggerFactory.getLogger(MailProducerService.class);
    private final RabbitTemplate rabbitTemplate;

    public MailProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendOtpToQueue(String email, String otpCode) {
        sendOtpToQueue(email, otpCode, OtpMailMessage.PASSWORD_RESET, null, null);
    }

    public void sendOtpToQueue(String email, String otpCode, String purpose, String actionUrl) {
        sendOtpToQueue(email, otpCode, purpose, actionUrl, null);
    }

    public void sendOtpToQueue(String email, String otpCode, String purpose, String actionUrl, String recipientName) {
        OtpMailMessage message = new OtpMailMessage(email, otpCode, purpose, actionUrl, recipientName);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                message
        );

        log.info("Producer: Đã đẩy yêu cầu gửi mail OTP [{}] cho [{}] vào RabbitMQ", purpose, email);
    }
}
