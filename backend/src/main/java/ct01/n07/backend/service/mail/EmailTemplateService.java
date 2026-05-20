package ct01.n07.backend.service.mail;

import ct01.n07.backend.dto.auth.OtpMailMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
public class EmailTemplateService {

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
        String escapedActionUrl = escapeHtml(message.getActionUrl());
        String escapedOtp = escapeHtml(message.getOtpCode());
        String content = ""
                + heading("Kích hoạt tài khoản")
                + greeting(message)
                + paragraph("Cảm ơn bạn đã đăng ký sử dụng PharmAgent. Nhấn vào mã OTP bên dưới để xác minh rằng email này thuộc về bạn.")
                + otpBlock(escapedOtp, escapedActionUrl, "Mã xác minh của bạn")
                + notice("Mã OTP có hiệu lực trong " + VERIFY_TOKEN_TTL_MINUTES + " phút. Nếu bạn không tạo tài khoản trên PharmAgent, hãy bỏ qua email này.")
                + supportLine();

        return new EmailContent(
                message.getOtpCode() + " là mã kích hoạt tài khoản PharmAgent",
                baseTemplate(content));
    }

    private EmailContent buildPasswordResetEmail(OtpMailMessage message) {
        String escapedActionUrl = escapeHtml(message.getActionUrl());
        String content = ""
                + heading("Đặt lại mật khẩu")
                + greeting(message)
                + paragraph("Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản PharmAgent của bạn. Nhấn nút bên dưới để đặt lại mật khẩu mới.")
                + button(escapedActionUrl, "Đặt lại mật khẩu")
                + linkFallback(escapedActionUrl)
                + warning("Liên kết đặt lại mật khẩu hết hạn sau " + RESET_TOKEN_TTL_HOURS + " giờ. Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này và không cần thực hiện thêm thao tác nào.")
                + supportLine();

        return new EmailContent("Đặt lại mật khẩu PharmAgent", baseTemplate(content));
    }

    private String baseTemplate(String content) {
        return "<!doctype html>"
                + "<html lang='vi'>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>PharmAgent</title>"
                + "</head>"
                + "<body style='margin:0;padding:0;background:#ecf6f5;font-family:Segoe UI,Roboto,Arial,sans-serif;color:#12302f;'>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background:#ecf6f5;padding:36px 14px;'>"
                + "<tr><td align='center'>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='max-width:620px;background:#ffffff;border:1px solid #cfe4e1;border-radius:22px;overflow:hidden;box-shadow:0 20px 54px rgba(17,68,65,0.16);'>"
                + header()
                + "<tr><td align='center' style='padding:30px 36px 34px;text-align:center;'>"
                + content
                + "</td></tr>"
                + footer()
                + "</table>"
                + "</td></tr>"
                + "</table>"
                + "</body></html>";
    }

    private String header() {
        return "<tr><td align='center' style='padding:30px 36px;background:#073b3a;text-align:center;'>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0'>"
                + "<tr><td align='center' style='text-align:center;'>"
                + "<img src='cid:pharmagentLogo' alt='PharmAgent logo' width='82' style='display:block;width:82px;max-width:82px;height:auto;margin:0 auto 14px;border:0;'>"
                + "<img src='cid:pharmagentTitle' alt='PharmAgent - Thuốc đúng, sống khỏe' width='250' style='display:block;width:250px;max-width:100%;height:auto;margin:0 auto;border:0;'>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr>";
    }

    private String footer() {
        String escapedFrontendUrl = escapeHtml(frontendUrl);
        return "<tr><td align='center' style='padding:20px 36px 28px;background:#f7fbfa;border-top:1px solid #e2eeec;text-align:center;'>"
                + "<p style='margin:0 0 6px;font-size:13px;line-height:1.6;color:#526765;'>Email này được gửi tự động bởi <a href='" + escapedFrontendUrl + "' target='_blank' style='color:#129a8e;text-decoration:none;'><strong>PharmAgent</strong></a>.</p>"
                + "<p style='margin:0 0 6px;font-size:13px;line-height:1.6;color:#526765;'>Nền tảng hỗ trợ quản lý thuốc và chăm sóc sức khỏe cho gia đình.</p>"
                + "<p style='margin:0;font-size:12px;color:#7b918f;'>© " + Year.now().getValue() + " <a href='" + escapedFrontendUrl + "' target='_blank' style='color:#7b918f;text-decoration:none;'><strong>PharmAgent</strong></a>. All rights reserved.</p>"
                + "</td></tr>";
    }

    private String heading(String value) {
        return "<h1 style='margin:0 0 14px;font-size:26px;line-height:1.25;color:#082f2f;font-weight:900;letter-spacing:0;text-align:center;'>"
                + escapeHtml(value)
                + "</h1>";
    }

    private String paragraph(String value) {
        return "<p style='margin:0 auto 24px;font-size:16px;line-height:1.7;color:#35504e;text-align:center;max-width:500px;'>"
                + escapeHtml(value)
                + "</p>";
    }

    private String greeting(OtpMailMessage message) {
        String name = hasText(message.getRecipientName()) ? message.getRecipientName().trim() : message.getEmail();
        return "<p style='margin:0 auto 14px;font-size:17px;line-height:1.7;color:#12302f;text-align:center;max-width:500px;'><strong>Xin chào "
                + escapeHtml(name)
                + "</strong>,</p>";
    }

    private String otpBlock(String escapedOtp, String escapedActionUrl, String label) {
        String href = escapedActionUrl == null || escapedActionUrl.isBlank() ? "#" : escapedActionUrl;
        return "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='margin:0 0 24px;'>"
                + "<tr><td align='center'>"
                + "<table role='presentation' cellpadding='0' cellspacing='0' style='background:#edfdf9;border:2px dashed #59cfc3;border-radius:18px;padding:22px 34px;'>"
                + "<tr><td align='center'>"
                + "<p style='margin:0 0 10px;font-size:12px;font-weight:900;color:#148579;text-transform:uppercase;letter-spacing:2px;'>"
                + escapeHtml(label)
                + "</p>"
                + "<a href='" + href + "' target='_blank' style='display:block;color:#083b3a;text-decoration:none;font-size:38px;font-weight:900;letter-spacing:10px;font-family:Consolas,Courier New,monospace;'>"
                + escapedOtp
                + "</a>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr>"
                + "</table>";
    }

    private String button(String escapedActionUrl, String label) {
        if (escapedActionUrl == null || escapedActionUrl.isBlank()) {
            return "";
        }
        return "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='margin:0 0 24px;'>"
                + "<tr><td align='center'>"
                + "<a href='" + escapedActionUrl + "' target='_blank' style='display:inline-block;background:linear-gradient(135deg,#129a8e,#146bd5);color:#ffffff;text-decoration:none;font-size:16px;font-weight:900;padding:14px 28px;border-radius:12px;box-shadow:0 10px 24px rgba(20,107,213,0.24);'>"
                + escapeHtml(label)
                + "</a>"
                + "</td></tr>"
                + "</table>";
    }

    private String linkFallback(String escapedActionUrl) {
        if (escapedActionUrl == null || escapedActionUrl.isBlank()) {
            return "";
        }
        return "<div style='background:#f5faf9;border:1px solid #dcecea;border-radius:12px;padding:14px 16px;margin:0 auto 22px;text-align:center;max-width:500px;'>"
                + "<p style='margin:0 0 8px;font-size:12px;font-weight:900;text-transform:uppercase;letter-spacing:1px;color:#5d7370;'>Hoặc sao chép liên kết</p>"
                + "<a href='" + escapedActionUrl + "' target='_blank' style='font-size:12px;line-height:1.6;color:#146bd5;text-decoration:none;word-break:break-all;'>"
                + escapedActionUrl
                + "</a>"
                + "</div>";
    }

    private String notice(String value) {
        return "<div style='background:#e9f9f5;border-left:4px solid #19b8a8;border-radius:12px;padding:14px 16px;text-align:center;max-width:500px;margin:0 auto 18px;'>"
                + "<p style='margin:0;font-size:14px;line-height:1.6;color:#35504e;text-align:center;'>"
                + escapeHtml(value)
                + "</p>"
                + "</div>";
    }

    private String warning(String value) {
        return "<div style='background:#fff8e8;border-left:4px solid #f59e0b;border-radius:12px;padding:14px 16px;text-align:center;max-width:500px;margin:0 auto 18px;'>"
                + "<p style='margin:0;font-size:14px;line-height:1.6;color:#62440b;text-align:center;'>"
                + escapeHtml(value)
                + "</p>"
                + "</div>";
    }

    private String supportLine() {
        return "<p style='margin:18px auto 0;font-size:14px;line-height:1.6;color:#526765;text-align:center;max-width:500px;'>Bạn cần trợ giúp? Liên hệ với chúng tôi qua địa chỉ "
                + "<a href='mailto:" + SUPPORT_EMAIL + "' style='color:#146bd5;text-decoration:none;font-weight:800;'>"
                + SUPPORT_EMAIL
                + "</a></p>";
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
