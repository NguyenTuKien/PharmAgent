package ct01.n07.backend.consumer;

import ct01.n07.backend.config.RabbitMQConfig;
import ct01.n07.backend.dto.auth.OtpMailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailConsumerService {

    private static final Logger log = LoggerFactory.getLogger(MailConsumerService.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@pharmagent.local}")
    private String fromEmail;

    public MailConsumerService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
        helper.setSubject(resolveSubject(message.getPurpose()));

        helper.setText(buildHtmlTemplate(message), true);
        mailSender.send(mimeMessage);
    }

    private String resolveSubject(String purpose) {
        if ("EMAIL_VERIFICATION".equals(purpose)) {
            return "Kích hoạt tài khoản PharmAgent";
        }
        return "Đặt lại mật khẩu PharmAgent";
    }

    private String buildHtmlTemplate(OtpMailMessage message) {
        boolean isVerification = "EMAIL_VERIFICATION".equals(message.getPurpose());
        String title = isVerification ? "Kích hoạt tài khoản" : "Đặt lại mật khẩu";
        String intro = isVerification
                ? "Cảm ơn bạn đã đăng ký PharmAgent. Bấm nút bên dưới hoặc nhập mã OTP để xác minh email chính chủ."
                : "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản PharmAgent của bạn. Bấm nút bên dưới rồi nhập mã OTP này.";
        String buttonLabel = isVerification ? "Kích hoạt tài khoản" : "Mở trang đặt lại mật khẩu";
        String actionButton = message.getActionUrl() == null || message.getActionUrl().isBlank()
                ? ""
                : "<a href='" + escapeHtml(message.getActionUrl()) + "' style='display:inline-block;background:#1f8a70;color:#ffffff;text-decoration:none;padding:14px 22px;border-radius:10px;font-weight:800;margin:8px 0 24px;'>"
                + buttonLabel
                + "</a>";

        return "<!doctype html>"
                + "<html><body style='margin:0;background:#f4f7f5;padding:32px 16px;font-family:Arial,sans-serif;color:#15221f;'>"
                + "<div style='max-width:620px;margin:0 auto;background:#ffffff;border:1px solid #dde8e3;border-radius:18px;overflow:hidden;'>"
                + "<div style='background:#10211d;padding:28px 32px;color:#ffffff;'>"
                + "<div style='font-size:14px;letter-spacing:.08em;text-transform:uppercase;color:#9de6cd;font-weight:800;'>PharmAgent</div>"
                + "<h1 style='margin:10px 0 0;font-size:28px;line-height:1.2;'>" + title + "</h1>"
                + "</div>"
                + "<div style='padding:30px 32px;'>"
                + "<p style='font-size:16px;line-height:1.7;margin:0 0 20px;'>Xin chào,</p>"
                + "<p style='font-size:16px;line-height:1.7;margin:0 0 24px;'>" + intro + "</p>"
                + "<div style='background:#edf8f4;border:1px solid #c8eadf;border-radius:14px;text-align:center;padding:22px;margin-bottom:24px;'>"
                + "<div style='font-size:13px;color:#52635e;font-weight:700;text-transform:uppercase;letter-spacing:.08em;'>Mã OTP</div>"
                + "<a href='" + escapeHtml(message.getActionUrl() == null ? "#" : message.getActionUrl()) + "' style='display:block;margin-top:8px;color:#10211d;text-decoration:none;font-size:34px;letter-spacing:8px;font-weight:900;'>"
                + escapeHtml(message.getOtpCode())
                + "</a>"
                + "</div>"
                + actionButton
                + "<p style='font-size:14px;line-height:1.6;color:#5e6d69;margin:0;'>Mã OTP có thời hạn ngắn. Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.</p>"
                + "</div>"
                + "<div style='border-top:1px solid #edf1ef;padding:18px 32px;color:#72817d;font-size:12px;'>"
                + "Email này được gửi tự động để bảo vệ tài khoản và dữ liệu chăm sóc thuốc của bạn."
                + "</div></div></body></html>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
