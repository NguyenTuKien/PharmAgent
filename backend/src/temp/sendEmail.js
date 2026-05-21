import nodemailer from "nodemailer";

const APP_NAME = "PharmAgent";
const SLOGAN = "Thuốc đúng, sống khỏe";
const SUPPORT_EMAIL = process.env.EMAIL_SUPPORT || process.env.SPRING_MAIL_USERNAME || process.env.EMAIL_USER;
const FROM_EMAIL = process.env.SPRING_MAIL_USERNAME || process.env.EMAIL_USER;

const transporter = nodemailer.createTransport({
  host: process.env.SPRING_MAIL_HOST || process.env.EMAIL_HOST,
  port: Number(process.env.SPRING_MAIL_PORT || process.env.EMAIL_PORT || 587),
  secure: String(process.env.SPRING_MAIL_SECURE || "false") === "true",
  auth: {
    user: FROM_EMAIL,
    pass: process.env.SPRING_MAIL_PASSWORD || process.env.EMAIL_PASS,
  },
});

const escapeHtml = (value = "") =>
  String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

const baseTemplate = (content) => `
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${APP_NAME}</title>
</head>
<body style="margin:0;padding:0;background:#ecf6f5;font-family:Segoe UI,Roboto,Arial,sans-serif;color:#12302f;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#ecf6f5;padding:36px 14px;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:620px;background:#ffffff;border:1px solid #cfe4e1;border-radius:22px;overflow:hidden;box-shadow:0 20px 54px rgba(17,68,65,0.16);">
          <tr>
            <td style="padding:30px 36px;background:#073b3a;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
                <tr>
                  <td width="58" valign="middle">
                    <div style="width:54px;height:54px;border-radius:16px;background:linear-gradient(135deg,#45e6d6,#146bd5);color:#ffffff;text-align:center;line-height:54px;font-size:25px;font-weight:900;box-shadow:0 12px 26px rgba(20,107,213,0.28);">+</div>
                  </td>
                  <td valign="middle" style="padding-left:14px;">
                    <div style="font-size:27px;font-weight:900;letter-spacing:0;color:#ffffff;">Pharm<span style="color:#45e6d6;">Agent</span></div>
                    <div style="font-size:13px;font-weight:700;letter-spacing:0;color:#c7fff6;margin-top:4px;">${SLOGAN}</div>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="padding:30px 36px 34px;">
              ${content}
            </td>
          </tr>
          <tr>
            <td style="padding:20px 36px 28px;background:#f7fbfa;border-top:1px solid #e2eeec;">
              <p style="margin:0 0 6px;font-size:13px;line-height:1.6;color:#526765;">
                Email này được gửi tự động bởi ${APP_NAME} để bảo vệ tài khoản và dữ liệu chăm sóc thuốc của bạn.
              </p>
              <p style="margin:0;font-size:12px;color:#7b918f;">© ${new Date().getFullYear()} ${APP_NAME}. All rights reserved.</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
`;

const otpBlock = (otp, actionUrl, label) => `
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:0 0 24px;">
    <tr>
      <td align="center">
        <table role="presentation" cellpadding="0" cellspacing="0" style="background:#edfdf9;border:2px dashed #59cfc3;border-radius:18px;padding:22px 34px;">
          <tr>
            <td align="center">
              <p style="margin:0 0 10px;font-size:12px;font-weight:900;color:#148579;text-transform:uppercase;letter-spacing:2px;">${escapeHtml(label)}</p>
              <a href="${escapeHtml(actionUrl)}" target="_blank" style="display:block;color:#083b3a;text-decoration:none;font-size:38px;font-weight:900;letter-spacing:10px;font-family:Consolas,Courier New,monospace;">
                ${escapeHtml(otp)}
              </a>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
`;

const button = (actionUrl, label) => `
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="margin:0 0 24px;">
    <tr>
      <td align="center">
        <a href="${escapeHtml(actionUrl)}" target="_blank" style="display:inline-block;background:linear-gradient(135deg,#129a8e,#146bd5);color:#ffffff;text-decoration:none;font-size:16px;font-weight:900;padding:14px 28px;border-radius:12px;box-shadow:0 10px 24px rgba(20,107,213,0.24);">
          ${escapeHtml(label)}
        </a>
      </td>
    </tr>
  </table>
`;

export const sendVerificationEmail = async (email, fullName, otp, verificationLink) => {
  const content = `
    <h1 style="margin:0 0 14px;font-size:26px;line-height:1.25;color:#082f2f;font-weight:900;letter-spacing:0;">Kích hoạt tài khoản</h1>
    <p style="margin:0 0 24px;font-size:16px;line-height:1.7;color:#35504e;">
      Xin chào <strong>${escapeHtml(fullName || email)}</strong>, cảm ơn bạn đã đăng ký ${APP_NAME}. Nhấn vào mã OTP hoặc nút bên dưới để xác minh email chính chủ.
    </p>
    ${otpBlock(otp, verificationLink, "Nhấn để kích hoạt tài khoản")}
    ${button(verificationLink, "Kích hoạt tài khoản")}
    <div style="background:#e9f9f5;border-left:4px solid #19b8a8;border-radius:12px;padding:14px 16px;">
      <p style="margin:0;font-size:14px;line-height:1.6;color:#35504e;">Mã OTP có hiệu lực trong <strong>15 phút</strong>. Nếu bạn không tạo tài khoản ${APP_NAME}, hãy bỏ qua email này.</p>
    </div>
  `;

  await transporter.sendMail({
    from: `"${APP_NAME}" <${FROM_EMAIL}>`,
    to: email,
    subject: `${otp} là mã kích hoạt tài khoản ${APP_NAME}`,
    html: baseTemplate(content),
  });
};

export const sendResetPasswordEmail = async (email, fullName, resetLink) => {
  const content = `
    <h1 style="margin:0 0 14px;font-size:26px;line-height:1.25;color:#082f2f;font-weight:900;letter-spacing:0;">Đặt lại mật khẩu</h1>
    <p style="margin:0 0 24px;font-size:16px;line-height:1.7;color:#35504e;">
      Xin chào <strong>${escapeHtml(fullName || email)}</strong>, chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản ${APP_NAME}. Nhấn nút bên dưới để mở trang đặt lại mật khẩu an toàn.
    </p>
    ${button(resetLink, "Mở trang đặt lại mật khẩu")}
    <div style="background:#f5faf9;border:1px solid #dcecea;border-radius:12px;padding:14px 16px;margin:0 0 22px;">
      <p style="margin:0 0 8px;font-size:12px;font-weight:900;text-transform:uppercase;letter-spacing:1px;color:#5d7370;">Hoặc sao chép liên kết</p>
      <a href="${escapeHtml(resetLink)}" target="_blank" style="font-size:12px;line-height:1.6;color:#146bd5;text-decoration:none;word-break:break-all;">${escapeHtml(resetLink)}</a>
    </div>
    <div style="background:#fff8e8;border-left:4px solid #f59e0b;border-radius:12px;padding:14px 16px;">
      <p style="margin:0;font-size:14px;line-height:1.6;color:#62440b;">Liên kết hết hạn sau <strong>12 giờ</strong>. Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này hoặc liên hệ ${escapeHtml(SUPPORT_EMAIL || "bộ phận hỗ trợ")}.</p>
    </div>
  `;

  await transporter.sendMail({
    from: `"${APP_NAME}" <${FROM_EMAIL}>`,
    to: email,
    subject: `Đặt lại mật khẩu ${APP_NAME}`,
    html: baseTemplate(content),
  });
};
