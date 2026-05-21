package ct01.n07.backend.consumer;

import ct01.n07.backend.config.RabbitMQConfig;
import ct01.n07.backend.dto.auth.OtpMailMessage;
import ct01.n07.backend.service.mail.EmailContent;
import ct01.n07.backend.service.mail.EmailTemplateService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailConsumerService {

    private static final Logger log = LoggerFactory.getLogger(MailConsumerService.class);
    private static final String LOGO_CONTENT_ID = "pharmagentLogo";
    private static final String TITLE_CONTENT_ID = "pharmagentTitle";
    private static final String SVG_CONTENT_TYPE = "image/svg+xml";
    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    @Value("${spring.mail.username:no-reply@pharmagent.local}")
    private String fromEmail;

    public MailConsumerService(JavaMailSender mailSender, EmailTemplateService emailTemplateService) {
        this.mailSender = mailSender;
        this.emailTemplateService = emailTemplateService;
    }

    // Annotation này đánh dấu hàm này luôn lắng nghe từ Queue đã chỉ định
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeMessageFromQueue(OtpMailMessage message) {
        log.info("Consumer: Nhận được yêu cầu gửi mail từ Queue cho [{}]", message.getEmail());

        try {
            sendHtmlEmail(message);
            log.info("Consumer: Gửi mail thành công cho [{}]", message.getEmail());
        } catch (Exception e) {
            log.error("Consumer: Gửi mail thất bại cho [{}] - Lỗi: {}", message.getEmail(), e.getMessage());
        }
    }

    private void sendHtmlEmail(OtpMailMessage message) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(message.getEmail());
        EmailContent emailContent = emailTemplateService.build(message);
        helper.setSubject(emailContent.subject());

        helper.setText(emailContent.html(), true);
        helper.addInline(LOGO_CONTENT_ID, new ClassPathResource("mail/assets/logo.svg"), SVG_CONTENT_TYPE);
        helper.addInline(TITLE_CONTENT_ID, new ClassPathResource("mail/assets/title.svg"), SVG_CONTENT_TYPE);
        mailSender.send(mimeMessage);
    }
}
