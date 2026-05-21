package ct01.n07.backend.service.mail;

import ct01.n07.backend.dto.auth.OtpMailMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
public class EmailTemplateService {

    private static final String APP_NAME = "PharmAgent";
    private static final String SLOGAN = "Thuốc đúng, sống khỏe";
    private static final String SUPPORT_EMAIL = "pharmagent.team@gmail.com";
    private static final int VERIFY_TOKEN_TTL_MINUTES = 15;
    private static final int RESET_TOKEN_TTL_HOURS = 12;
    private final String frontendUrl;

    public EmailTemplateService(
            @Value("${app.frontend-url:${APP_FRONTEND_URL:${FRONTEND_URL:http://localhost:5173}}}") String frontendUrl) {
        this.frontendUrl = normalizeBaseUrl(frontendUrl);
    }

    public EmailContent build(OtpMailMessage message) {
        if (OtpMailMessage.EMAIL_VERIFICATION.equals(message.getPurpose())) {
            return buildVerificationEmail(message);
        }
        return buildPasswordResetEmail(message);
    }

    private EmailContent buildVerificationEmail(OtpMailMessage message) {
        String name = hasText(message.getRecipientName()) ? message.getRecipientName().trim() : message.getEmail();
        String escapedName = escapeHtml(name);
        String escapedOtp = escapeHtml(message.getOtpCode());
        String escapedActionUrl = escapeHtml(message.getActionUrl());

        String content = "<h1 style=\"margin:0 0 14px;font-size:26px;line-height:1.25;color:#082f2f;font-weight:900;letter-spacing:0;\">Kích hoạt tài khoản</h1>"
                + "<p style=\"margin:0 0 24px;font-size:16px;line-height:1.7;color:#35504e;\">"
                + "Xin chào <strong>" + escapedName + "</strong>, cảm ơn bạn đã đăng ký " + APP_NAME + ". Nhấn vào mã OTP hoặc nút bên dưới để xác minh email chính chủ."
                + "</p>"
                + otpBlock(escapedOtp, escapedActionUrl, "Nhấn để kích hoạt tài khoản")
                + button(escapedActionUrl, "Kích hoạt tài khoản")
                + "<div style=\"background:#e9f9f5;border-left:4px solid #19b8a8;border-radius:12px;padding:14px 16px;\">"
                + "<p style=\"margin:0;font-size:14px;line-height:1.6;color:#35504e;\">Mã OTP có hiệu lực trong <strong>" + VERIFY_TOKEN_TTL_MINUTES + " phút</strong>. Nếu bạn không tạo tài khoản " + APP_NAME + ", hãy bỏ qua email này.</p>"
                + "</div>";

        return new EmailContent(
                message.getOtpCode() + " là mã kích hoạt tài khoản " + APP_NAME,
                baseTemplate(content));
    }

    private EmailContent buildPasswordResetEmail(OtpMailMessage message) {
        String name = hasText(message.getRecipientName()) ? message.getRecipientName().trim() : message.getEmail();
        String escapedName = escapeHtml(name);
        String escapedActionUrl = escapeHtml(message.getActionUrl());

        String content = "<h1 style=\"margin:0 0 14px;font-size:26px;line-height:1.25;color:#082f2f;font-weight:900;letter-spacing:0;\">Đặt lại mật khẩu</h1>"
                + "<p style=\"margin:0 0 24px;font-size:16px;line-height:1.7;color:#35504e;\">"
                + "Xin chào <strong>" + escapedName + "</strong>, chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản " + APP_NAME + ". Nhấn nút bên dưới để mở trang đặt lại mật khẩu an toàn."
                + "</p>"
                + button(escapedActionUrl, "Mở trang đặt lại mật khẩu")
                + "<div style=\"background:#f5faf9;border:1px solid #dcecea;border-radius:12px;padding:14px 16px;margin:0 0 22px;\">"
                + "<p style=\"margin:0 0 8px;font-size:12px;font-weight:900;text-transform:uppercase;letter-spacing:1px;color:#5d7370;\">Hoặc sao chép liên kết</p>"
                + "<a href=\"" + escapedActionUrl + "\" target=\"_blank\" style=\"font-size:12px;line-height:1.6;color:#146bd5;text-decoration:none;word-break:break-all;\">" + escapedActionUrl + "</a>"
                + "</div>"
                + "<div style=\"background:#fff8e8;border-left:4px solid #f59e0b;border-radius:12px;padding:14px 16px;\">"
                + "<p style=\"margin:0;font-size:14px;line-height:1.6;color:#62440b;\">Liên kết hết hạn sau <strong>" + RESET_TOKEN_TTL_HOURS + " giờ</strong>. Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này hoặc liên hệ " + escapeHtml(SUPPORT_EMAIL) + ".</p>"
                + "</div>";

        return new EmailContent("Đặt lại mật khẩu " + APP_NAME, baseTemplate(content));
    }

    private String baseTemplate(String content) {
        return "<!DOCTYPE html>"
                + "<html lang=\"vi\">"
                + "<head>"
                + "  <meta charset=\"UTF-8\" />"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />"
                + "  <title>" + APP_NAME + "</title>"
                + "</head>"
                + "<body style=\"margin:0;padding:0;background:#ecf6f5;font-family:Segoe UI,Roboto,Arial,sans-serif;color:#12302f;\">"
                + "  <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ecf6f5;padding:36px 14px;\">"
                + "    <tr>"
                + "      <td align=\"center\">"
                + "        <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:620px;background:#ffffff;border:1px solid #cfe4e1;border-radius:22px;overflow:hidden;box-shadow:0 20px 54px rgba(17,68,65,0.16);\">"
                + "          <tr>"
                + "            <td style=\"padding:30px 36px;background:#073b3a;text-align:center;\">"
                + "              <a href='" + frontendUrl + "' target=\"_blank\" style=\"text-decoration:none;display:inline-block;\">"
                + "                <img src=\"cid:pharmagentLogo\" alt=\"" + APP_NAME + " logo\" width=\"54\" height=\"54\" style=\"border-radius:16px;vertical-align:middle;\" />"
                + "                <img src=\"cid:pharmagentTitle\" alt=\"" + APP_NAME + "\" height=\"32\" style=\"vertical-align:middle;margin-left:12px;\" />"
                + "              </a>"
                + "              <div style=\"font-size:13px;font-weight:700;color:#c7fff6;margin-top:8px;\">" + SLOGAN + "</div>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style=\"padding:30px 36px 34px;\">"
                + "              " + content
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style=\"padding:20px 36px 28px;background:#f7fbfa;border-top:1px solid #e2eeec;text-align:center;\">"
                + "              <p style=\"margin:0 0 6px;font-size:13px;line-height:1.6;color:#526765;\">"
                + "                Email này được gửi tự động bởi <strong>" + APP_NAME + "</strong> để bảo vệ tài khoản và dữ liệu chăm sóc thuốc của bạn."
                + "              </p>"
                + "              <p style=\"margin:0 0 6px;font-size:12px;color:#7b918f;\">Hỗ trợ: <a href=\"mailto:" + SUPPORT_EMAIL + "\" style=\"color:#129a8e;\">" + SUPPORT_EMAIL + "</a></p>"
                + "              <p style=\"margin:0;font-size:12px;color:#7b918f;\">© " + Year.now().getValue() + " " + APP_NAME + ". All rights reserved.</p>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    private String otpBlock(String otp, String actionUrl, String label) {
        String href = actionUrl == null || actionUrl.isBlank() ? "#" : actionUrl;
        return "  <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 24px;\">"
                + "    <tr>"
                + "      <td align=\"center\">"
                + "        <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#edfdf9;border:2px dashed #59cfc3;border-radius:18px;padding:22px 34px;\">"
                + "          <tr>"
                + "            <td align=\"center\">"
                + "              <p style=\"margin:0 0 10px;font-size:12px;font-weight:900;color:#148579;text-transform:uppercase;letter-spacing:2px;\">" + label + "</p>"
                + "              <a href=\"" + href + "\" target=\"_blank\" style=\"display:block;color:#083b3a;text-decoration:none;font-size:38px;font-weight:900;letter-spacing:10px;font-family:Consolas,Courier New,monospace;\">"
                + "                " + otp
                + "              </a>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>";
    }

    private String button(String actionUrl, String label) {
        if (actionUrl == null || actionUrl.isBlank()) {
            return "";
        }
        return "  <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 24px;\">"
                + "    <tr>"
                + "      <td align=\"center\">"
                + "        <a href=\"" + actionUrl + "\" target=\"_blank\" style=\"display:inline-block;background:linear-gradient(135deg,#129a8e,#146bd5);color:#ffffff;text-decoration:none;font-size:16px;font-weight:900;padding:14px 28px;border-radius:12px;box-shadow:0 10px 24px rgba(20,107,213,0.24);\">"
                + "          " + label
                + "        </a>"
                + "      </td>"
                + "    </tr>"
                + "  </table>";
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:5173";
        }
        return value.replaceAll("/+$", "");
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
