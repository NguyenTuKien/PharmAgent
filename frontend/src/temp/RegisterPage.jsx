import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../modules/auth/authFacade.js";
import { getToastErrorMessage, notify } from "../lib/toast.js";
import logo from "../assets/logo.svg";
import title from "../assets/title.svg";
import InteractiveBackground from "./InteractiveBackground";
import AuthFormBackground from "./AuthFormBackground";
import "../styles/auth/auth.css";

const RegisterPage = () => {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!username || !email || !password || !confirmPassword) {
      const message = "Vui lòng điền đầy đủ thông tin";
      notify.warning(message, {
        description: "Hoàn tất tên người dùng, email và mật khẩu để đăng ký.",
      });
      return;
    }

    if (password !== confirmPassword) {
      const message = "Mật khẩu xác nhận không khớp";
      notify.warning(message, {
        description: "Nhập lại mật khẩu xác nhận trùng với mật khẩu mới.",
      });
      return;
    }

    if (password.length < 8) {
      const message = "Mật khẩu phải có ít nhất 8 ký tự";
      notify.warning(message, {
        description: "Dùng mật khẩu dài hơn để bảo vệ tài khoản tốt hơn.",
      });
      return;
    }

    setIsLoading(true);
    try {
      await register(username, email, password);
      notify.success("Đăng ký tài khoản thành công", {
        description: "Tiếp tục thêm hồ sơ người thân để hoàn tất thiết lập.",
      });
      navigate(`/register/elderly?email=${encodeURIComponent(email)}`);
    } catch (err) {
      const message = getToastErrorMessage(err, "Đăng ký thất bại. Vui lòng thử lại.");
      notify.error(message, {
        description: "Kiểm tra lại email hoặc thử lại sau ít phút.",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-page">
      {}
      <div className="auth-hero hidden lg:flex lg:w-1/2">
        <img
          src="/register-hero.png"
          alt="Team collaboration"
          className="auth-hero-image"
        />
        <div className="auth-hero-content">
          <div className="auth-hero-logo">
            <img src={logo} alt="PharmAgent" />
            <img src={title} alt="PharmAgent" className="auth-hero-title" />
          </div>

          <h1>
            Bắt đầu chăm sóc
            <br />
            <span className="highlight">thân chủ của bạn.</span>
          </h1>

          <p>
            Tạo tài khoản người chăm sóc, sau đó thêm hồ sơ người thân cần theo
            dõi thuốc.
          </p>
        </div>

        {}
        <InteractiveBackground />
      </div>

      {}
      <div className="auth-form-panel w-full lg:w-1/2">
        <AuthFormBackground />
        <div className="auth-wrapper">
          <div className="auth-form-box">
            <h2>Tạo tài khoản mới</h2>
            <p className="auth-subtitle">
              Điền thông tin để đăng ký tài khoản
            </p>

            <form onSubmit={handleSubmit}>
              {}
              <div className="auth-input-box">
                <input
                  id="register-username"
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  placeholder=" "
                  autoComplete="username"
                />
                <label htmlFor="register-username">Tên người dùng</label>
                <span className="icon">
                  <ion-icon name="person"></ion-icon>
                </span>
              </div>

              {}
              <div className="auth-input-box">
                <input
                  id="register-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  placeholder=" "
                  autoComplete="email"
                />
                <label htmlFor="register-email">Email</label>
                <span className="icon">
                  <ion-icon name="mail"></ion-icon>
                </span>
              </div>

              {}
              <div className="auth-input-box">
                <input
                  id="register-password"
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  placeholder=" "
                  autoComplete="new-password"
                />
                <label htmlFor="register-password">Mật khẩu</label>
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

              {}
              <div className="auth-input-box">
                <input
                  id="register-confirm-password"
                  type={showConfirmPassword ? "text" : "password"}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  placeholder=" "
                  autoComplete="new-password"
                />
                <label htmlFor="register-confirm-password">Xác nhận mật khẩu</label>
                <span
                  className="icon clickable"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  title="Hiển thị/Ẩn mật khẩu"
                >
                  <ion-icon
                    name={showConfirmPassword ? "eye-off" : "eye"}
                  ></ion-icon>
                </span>
              </div>

              {}
              <button
                id="register-submit"
                type="submit"
                disabled={isLoading}
                className="auth-btn-form"
                style={{ marginTop: "36px" }}
              >
                {isLoading ? (
                  <>
                    <span className="spinner"></span>
                    Đang đăng ký...
                  </>
                ) : (
                  "Đăng ký"
                )}
              </button>
            </form>

            {}
            <div className="auth-login-register">
              <p>
                Đã có tài khoản?{" "}
                <Link to="/login">Đăng nhập</Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
