import { useState } from "react";
import { useSearchParams, Link, useNavigate } from "react-router-dom";
import { resetPassword } from "../api/authApi.js";
import { getToastErrorMessage, notify } from "../lib/toast.js";
import logo from "../assets/logo.svg";
import title from "../assets/title.svg";
import InteractiveBackground from "./InteractiveBackground";
import AuthFormBackground from "./AuthFormBackground";
import "../styles/auth/auth.css";

const ResetPasswordPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [email, setEmail] = useState(searchParams.get("email") || "");
  const [resetToken] = useState(searchParams.get("token") || "");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!email || !resetToken || !password || !confirmPassword) {
      const message = "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn";
      notify.warning(message, {
        description: "Mở lại liên kết đặt lại mật khẩu mới nhất trong email.",
      });
      return;
    }

    if (password.length < 6) {
      const message = "Mật khẩu phải có ít nhất 6 ký tự";
      notify.warning(message, {
        description: "Chọn mật khẩu dài hơn trước khi đặt lại.",
      });
      return;
    }

    if (password !== confirmPassword) {
      const message = "Mật khẩu xác nhận không khớp";
      notify.warning(message, {
        description: "Nhập lại phần xác nhận mật khẩu cho trùng khớp.",
      });
      return;
    }

    setIsLoading(true);
    try {
      await resetPassword(email, resetToken, password, confirmPassword);
      setIsSuccess(true);
      notify.success("Đặt lại mật khẩu thành công", {
        description: "Bạn có thể đăng nhập bằng mật khẩu mới.",
      });
      setTimeout(() => navigate("/login"), 3000);
    } catch (err) {
      const message = getToastErrorMessage(err, "Không thể đặt lại mật khẩu. Vui lòng thử lại.");
      notify.error(message, {
        description: "Liên kết có thể đã hết hạn hoặc đã được sử dụng.",
      });
    } finally {
      setIsLoading(false);
    }
  };

  

  return (
    <div className="auth-page">

      <div className="auth-hero hidden lg:flex lg:w-1/2">
        <img
          src="/login-hero.png"
          alt="Team collaboration"
          className="auth-hero-image"
        />
        <div className="auth-hero-content">
          <div className="auth-hero-logo">
            <img src={logo} alt="PharmAgent" />
            <img src={title} alt="PharmAgent" className="auth-hero-title" />
          </div>

          <h1>
            Thiết lập mật khẩu
            <br />
            <span className="highlight">mới an toàn.</span>
          </h1>

          <p>
            Chọn một mật khẩu mạnh để bảo vệ tài khoản của bạn luôn được an
            toàn.
          </p>
        </div>

        <InteractiveBackground />
      </div>


      <div className="auth-form-panel w-full lg:w-1/2">
        <AuthFormBackground />
        <div className="auth-wrapper">
          <div className="auth-form-box">
            {isSuccess ? (
              
              <div style={{ textAlign: "center" }}>
                <div className="auth-success-icon">
                  <ion-icon
                    name="checkmark-circle"
                    style={{ fontSize: 44, color: "#10b981" }}
                  ></ion-icon>
                </div>
                <h2 className="auth-success-title">Đặt lại mật khẩu thành công!</h2>
                <p className="auth-success-text">
                  Mật khẩu của bạn đã được cập nhật thành công.
                  <br />
                  Đang chuyển hướng đến trang đăng nhập...
                </p>
                <div style={{ marginBottom: 16 }}>
                  <span className="spinner" style={{
                    display: "inline-block",
                    width: 24,
                    height: 24,
                    border: "2px solid rgba(237,248,245,0.2)",
                    borderTopColor: "var(--auth-primary)",
                    borderRadius: "50%",
                    animation: "authSpin 0.8s linear infinite",
                  }}></span>
                </div>
                <Link to="/login" className="auth-resend-btn">
                  Đi đến đăng nhập ngay
                </Link>
              </div>
            ) : (
              
              <>
                <h2>Đặt mật khẩu mới</h2>
                <p className="auth-subtitle">
                  Liên kết đặt lại mật khẩu đã được xác thực. Mật khẩu mới phải
                  có ít nhất 6 ký tự.
                </p>


                <form onSubmit={handleSubmit}>
                  <div className="auth-input-box">
                    <input
                      id="reset-email"
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                      placeholder=" "
                      autoComplete="email"
                      autoFocus
                    />
                    <label htmlFor="reset-email">Email</label>
                    <span className="icon">
                      <ion-icon name="mail"></ion-icon>
                    </span>
                  </div>

                  <div className="auth-input-box">
                    <input
                      id="reset-password"
                      type={showPassword ? "text" : "password"}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      placeholder=" "
                      autoComplete="new-password"
                    />
                    <label htmlFor="reset-password">Mật khẩu mới</label>
                    <span
                      className="icon clickable"
                      onClick={() => setShowPassword(!showPassword)}
                      title="Hiển thị/Ẩn mật khẩu"
                    >
                      <ion-icon
                        name={showPassword ? "eye-off" : "eye"}
                      ></ion-icon>
                    </span>
                  </div>


                  <div className="auth-input-box">
                    <input
                      id="reset-confirm-password"
                      type={showConfirmPassword ? "text" : "password"}
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      required
                      placeholder=" "
                      autoComplete="new-password"
                    />
                    <label htmlFor="reset-confirm-password">
                      Xác nhận mật khẩu mới
                    </label>
                    <span
                      className="icon clickable"
                      onClick={() =>
                        setShowConfirmPassword(!showConfirmPassword)
                      }
                      title="Hiển thị/Ẩn mật khẩu"
                    >
                      <ion-icon
                        name={showConfirmPassword ? "eye-off" : "eye"}
                      ></ion-icon>
                    </span>
                  </div>


                  {confirmPassword && confirmPassword !== password && (
                    <p className="auth-match-text auth-match-text--mismatch">
                      ✗ Mật khẩu không khớp
                    </p>
                  )}
                  {confirmPassword && confirmPassword === password && (
                    <p className="auth-match-text auth-match-text--match">
                      ✓ Mật khẩu trùng khớp
                    </p>
                  )}


                  <button
                    id="reset-submit"
                    type="submit"
                    disabled={isLoading}
                    className="auth-btn-form"
                    style={{ marginTop: 36 }}
                  >
                    {isLoading ? (
                      <>
                        <span className="spinner"></span>
                        Đang đặt lại...
                      </>
                    ) : (
                      "Đặt lại mật khẩu"
                    )}
                  </button>
                </form>


                <div className="auth-login-register">
                  <p>
                    Nhớ mật khẩu rồi?{" "}
                    <Link to="/login">Đăng nhập</Link>
                  </p>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ResetPasswordPage;
