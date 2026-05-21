import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../modules/auth/authFacade.js";
import { readGoogleOAuthCallback } from "../../modules/auth/oauth.js";
import { resendOTP } from "../../modules/auth/authApi.js";
import { getToastErrorMessage, notify } from "../../lib/toast.js";
import logo from "../../assets/logo.svg";
import title from "../../assets/title.svg";
import InteractiveBackground from "./InteractiveBackground";
import AuthFormBackground from "./AuthFormBackground";
import "../../styles/auth/auth.css";

export function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const { login, googleLogin, completeGoogleLogin } = useAuth();
  const navigate = useNavigate();
  const handledOAuthRef = useRef(false);

  useEffect(() => {
    if (handledOAuthRef.current) {
      return;
    }

    const callback = readGoogleOAuthCallback(window.location.search);
    if (!callback) {
      return;
    }

    handledOAuthRef.current = true;

    if (callback.error) {
      const message = "Không thể đăng nhập bằng Google. Vui lòng thử lại.";
      notify.error(message, {
        description: "Mở lại luồng đăng nhập Google hoặc đăng nhập bằng email.",
      });
      navigate("/login", { replace: true });
      return;
    }

    if (!callback.code) {
      const message = "Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn.";
      notify.error(message, {
        description: "Vui lòng bắt đầu lại từ nút đăng nhập Google.",
      });
      navigate("/login", { replace: true });
      return;
    }

    setIsLoading(true);
    completeGoogleLogin(callback.code)
      .then((result) => {
        navigate(result.redirectTo, { replace: true });
      })
      .catch((err) => {
        const message = getToastErrorMessage(
          err,
          "Không thể hoàn tất đăng nhập Google. Vui lòng thử lại."
        );
        notify.error(message, {
          description: "Phiên Google chưa hoàn tất. Vui lòng thử lại.",
        });
        navigate("/login", { replace: true });
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [completeGoogleLogin, navigate]);

  const handleGoogleLogin = async () => {
    setIsLoading(true);
    try {
      await googleLogin();
    } catch (err) {
      const message = err.message || "Đăng nhập Google chưa được cấu hình.";
      notify.error(message, {
        description: "Kiểm tra cấu hình OAuth hoặc thử lại sau.",
      });
      setIsLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!email || !password) {
      const message = "Vui lòng điền đầy đủ thông tin";
      notify.warning(message, {
        description: "Nhập cả email và mật khẩu để tiếp tục.",
      });
      return;
    }

    setIsLoading(true);
    try {
      const result = await login(email, password);
      notify.success("Đăng nhập thành công", {
        description: result.requiresProfileSelection
          ? "Vui lòng chọn hồ sơ bạn muốn sử dụng."
          : "Đang chuyển bạn vào PharmAgent.",
      });
      navigate(result.redirectTo, { replace: true });
    } catch (err) {
      const message = err.response?.data?.message || "";
      if (message.toLowerCase().includes("xác minh")) {
        try {
          await resendOTP(email);
        } catch {
          // The login redirect still works if resending the OTP fails.
        }
        notify.info("Tài khoản cần xác minh email", {
          description: "Mã xác minh đã được gửi lại nếu email còn hiệu lực.",
        });
        navigate(
          `/verify-email?email=${encodeURIComponent(email)}`
        );
        return;
      }
      const errorMessage = getToastErrorMessage(
        err,
        "Đăng nhập thất bại. Vui lòng thử lại."
      );
      notify.error(errorMessage, {
        description: "Kiểm tra email, mật khẩu hoặc đăng ký tài khoản mới.",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const togglePassword = () => {
    setShowPassword(!showPassword);
  };

  return (
    <div className="auth-page">
      {}
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
            Chăm sóc đúng giờ,
            <br />
            <span className="highlight">sống khỏe hơn.</span>
          </h1>

          <p>
            Theo dõi thuốc, lịch uống và kết nối người chăm sóc trong một trải
            nghiệm an toàn.
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
            <h2>Chào mừng trở lại!</h2>
            <p className="auth-subtitle">
              Đăng nhập để tiếp tục vào hệ thống
            </p>

            <form onSubmit={handleSubmit}>
              {}
              <div className="auth-input-box">
                <input
                  id="login-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  placeholder=" "
                  autoComplete="email"
                />
                <label htmlFor="login-email">Email</label>
                <span className="icon">
                  <ion-icon name="mail"></ion-icon>
                </span>
              </div>

              {}
              <div className="auth-input-box">
                <input
                  id="login-password"
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  placeholder=" "
                  autoComplete="current-password"
                />
                <label htmlFor="login-password">Mật khẩu</label>
                <span
                  className="icon clickable"
                  onClick={togglePassword}
                  title="Hiển thị/Ẩn mật khẩu"
                >
                  <ion-icon
                    name={showPassword ? "eye-off" : "eye"}
                  ></ion-icon>
                </span>
              </div>

              {}
              <div className="auth-remember">
                <label>
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                  />
                  Ghi nhớ đăng nhập
                </label>
                <Link to="/forgot-password">Quên mật khẩu?</Link>
              </div>

              {}
              <button
                id="login-submit"
                type="submit"
                disabled={isLoading}
                className="auth-btn-form"
              >
                {isLoading ? (
                  <>
                    <span className="spinner"></span>
                    Đang đăng nhập...
                  </>
                ) : (
                  "Đăng nhập"
                )}
              </button>
            </form>

            {}
            <div className="auth-oauth-divider">HOẶC</div>

            {}
            <button type="button" className="auth-google-btn" onClick={handleGoogleLogin} disabled={isLoading}>
              <svg
                className="google-icon"
                viewBox="0 0 24 24"
                width="20"
                height="20"
              >
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                />
              </svg>
              Đăng nhập với Google
            </button>

            {}
            <div className="auth-login-register">
              <p>
                Chưa có tài khoản?{" "}
                <Link to="/register">Đăng ký</Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
