package ct01.n07.backend.service.mail;

import ct01.n07.backend.dto.auth.OtpMailMessage;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateServiceTest {

    private final EmailTemplateService templateService = new EmailTemplateService("https://app.pharmagent.example");

    @Test
    void buildsPharmAgentVerificationEmailWithCenteredBrandAssetsGreetingAndHelpLink() {
        OtpMailMessage message = new OtpMailMessage(
                "caregiver@example.com",
                "123456",
                "EMAIL_VERIFICATION",
                "https://app.pharmagent.example/verify-email?email=caregiver%40example.com&otp=123456",
                "An Nguyen");

        EmailContent email = templateService.build(message);

        assertThat(email.subject()).isEqualTo("123456 là mã kích hoạt tài khoản PharmAgent");
        assertThat(email.html())
                .contains("PharmAgent")
                .contains("Thuốc đúng, sống khỏe")
                .contains("cid:pharmagentLogo")
                .contains("cid:pharmagentTitle")
                .contains("text-align:center")
                .contains("Xin chào <strong>An Nguyen</strong>")
                .contains("Kích hoạt tài khoản")
                .contains("123456")
                .contains("mailto:pharmagent.team@gmail.com")
                .contains("<strong>PharmAgent</strong>")
                .contains("href='https://app.pharmagent.example'")
                .contains("https://app.pharmagent.example/verify-email?email=caregiver%40example.com&amp;otp=123456")
                .doesNotContain("WorkHub");
    }

    @Test
    void buildsPharmAgentPasswordResetEmailWithLinkOnlyAndTwelveHourExpiry() {
        OtpMailMessage message = new OtpMailMessage(
                "patient@example.com",
                "reset-token-abc",
                "PASSWORD_RESET",
                "https://app.pharmagent.example/reset-password?email=patient%40example.com&token=reset-token-abc",
                "Binh Tran");

        EmailContent email = templateService.build(message);

        assertThat(email.subject()).isEqualTo("Đặt lại mật khẩu PharmAgent");
        assertThat(email.html())
                .contains("Đặt lại mật khẩu")
                .contains("Xin chào <strong>Binh Tran</strong>")
                .contains("https://app.pharmagent.example/reset-password?email=patient%40example.com&amp;token=reset-token-abc")
                .contains("12 giờ")
                .doesNotContain("Mã OTP")
                .doesNotContain("one-time-code")
                .doesNotContain("WorkHub");
    }

    @Test
    void mailBrandAssetsAreAvailableOnTheBackendClasspath() {
        assertThat(new ClassPathResource("mail/assets/logo.svg").exists()).isTrue();
        assertThat(new ClassPathResource("mail/assets/title.svg").exists()).isTrue();
    }
}
