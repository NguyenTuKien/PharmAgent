package ct01.n07.backend.consumer;

import ct01.n07.backend.config.RabbitMQConfig;
import ct01.n07.backend.dto.auth.OtpMailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailConsumerService {

    private static final Logger log = LoggerFactory.getLogger(MailConsumerService.class);
    private final JavaMailSender mailSender;

    public MailConsumerService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Annotation này đánh dấu hàm này luôn lắng nghe từ Queue đã chỉ định
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeMessageFromQueue(OtpMailMessage message) {
        log.info("Consumer: Nhận được yêu cầu gửi mail từ Queue cho [{}]", message.getEmail());

        try {
            sendHtmlEmail(message.getEmail(), message.getOtpCode());
            log.info("Consumer: Gửi mail thành công cho [{}]", message.getEmail());
        } catch (Exception e) {
            log.error("Consumer: Gửi mail thất bại cho [{}] - Lỗi: {}", message.getEmail(), e.getMessage());
            // Trong hệ thống thực tế, nếu gửi lỗi, bạn có thể cấu hình đẩy lại vào Dead Letter Queue (DLQ)
        }
    }

    private void sendHtmlEmail(String toEmail, String otp) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom("no-reply@yourdomain.com"); // Thay bằng tên hệ thống của bạn
        helper.setTo(toEmail);
        helper.setSubject("Mã xác nhận bảo mật - Đổi mật khẩu");

        String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>"
                + "<h2 style='color: #333;'>Yêu cầu đổi mật khẩu</h2>"
                + "<p>Xin chào,</p>"
                + "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với email này. Đây là mã OTP của bạn:</p>"
                + "<div style='text-align: center; margin: 20px 0;'>"
                + "  <span style='font-size: 24px; font-weight: bold; background-color: #f4f4f4; padding: 10px 20px; border-radius: 5px; color: #2d89ef;'>" + otp + "</span>"
                + "</div>"
                + "<p>Mã này có hiệu lực trong <b>5 phút</b>. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>"
                + "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'/>"
                + "<p style='font-size: 12px; color: #999;'>Nếu bạn không yêu cầu thay đổi mật khẩu, vui lòng bỏ qua email này.</p>"
                + "</div>";

        helper.setText(htmlContent, true);
        mailSender.send(mimeMessage);
    }
}