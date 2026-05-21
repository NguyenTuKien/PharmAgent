import { useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";

import { readOnboardingState, useAuth } from "../../modules/auth/authFacade.js";
import { getToastErrorMessage, notify } from "../../lib/toast.js";
import logo from "../../assets/logo.svg";
import title from "../../assets/title.svg";
import AuthFormBackground from "./AuthFormBackground.jsx";
import InteractiveBackground from "./InteractiveBackground.jsx";
import "../../styles/auth/auth.css";

const FAMILY_RELATION_OPTIONS = [
  { value: "FATHER", label: "Bố" },
  { value: "MOTHER", label: "Mẹ" },
  { value: "PATERNAL_GRANDFATHER", label: "Ông Nội" },
  { value: "PATERNAL_GRANDMOTHER", label: "Bà Nội" },
  { value: "MATERNAL_GRANDFATHER", label: "Ông Ngoại" },
  { value: "MATERNAL_GRANDMOTHER", label: "Bà Ngoại" },
  { value: "OTHER", label: "Khác" },
];

export function ElderlySetupPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { registerElderly } = useAuth();
  const onboarding = useMemo(() => readOnboardingState(), []);
  const email = searchParams.get("email") || onboarding.email || "";

  const [values, setValues] = useState({
    firstName: "",
    lastName: "",
    phone: "",
    dateOfBirth: "",
    gender: "FEMALE",
    address: "",
    caregiverTitle: "Người chăm sóc",
    relation: "FATHER",
    customRelation: "",
  });
  const [isLoading, setIsLoading] = useState(false);

  const updateField = (field) => (event) => {
    setValues((current) => ({
      ...current,
      [field]: event.target.value,
    }));
  };

  const goToVerifyEmail = () => {
    navigate(`/verify-email?email=${encodeURIComponent(email)}`);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!onboarding.onboardingToken) {
      const message = "Không tìm thấy phiên đăng ký. Vui lòng đăng ký lại.";
      notify.warning(message, {
        description: "Quay lại bước đăng ký để tạo phiên thiết lập mới.",
      });
      return;
    }

    if (!values.firstName || !values.lastName || !values.phone || !values.dateOfBirth || !values.gender) {
      const message = "Vui lòng điền đầy đủ thông tin bắt buộc";
      notify.warning(message, {
        description: "Điền tên, họ, số điện thoại, ngày sinh và giới tính.",
      });
      return;
    }

    setIsLoading(true);
    try {
      await registerElderly(values);
      notify.success("Đã tạo hồ sơ người thân", {
        description: "Tiếp tục xác minh email để kích hoạt tài khoản.",
      });
      goToVerifyEmail();
    } catch (err) {
      const message = getToastErrorMessage(
        err,
        err.message || "Không thể tạo hồ sơ người thân. Vui lòng thử lại.",
      );
      notify.error(message, {
        description: "Kiểm tra lại thông tin hồ sơ hoặc thử lại sau.",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-hero hidden lg:flex lg:w-1/2">
        <img
          src="/register-hero.png"
          alt="Caregiver and elderly support"
          className="auth-hero-image"
        />
        <div className="auth-hero-content">
          <div className="auth-hero-logo">
            <img src={logo} alt="PharmAgent" />
            <img src={title} alt="PharmAgent" className="auth-hero-title" />
          </div>

          <h1>
            Thêm hồ sơ
            <br />
            <span className="highlight">người cần chăm sóc.</span>
          </h1>

          <p>
            Hồ sơ elderly giúp PharmAgent cá nhân hóa lịch thuốc, nhắc uống và
            kết nối chăm sóc trong gia đình.
          </p>
        </div>

        <InteractiveBackground />
      </div>

      <div className="auth-form-panel w-full lg:w-1/2">
        <AuthFormBackground />
        <div className="auth-wrapper">
          <div className="auth-form-box">
            <Link to="/register" className="auth-back-link">
              <ion-icon name="arrow-back"></ion-icon>
              Quay lại đăng ký
            </Link>

            <h2>Tạo hồ sơ người thân</h2>
            <p className="auth-subtitle">
              Bạn có thể thêm ngay bây giờ hoặc bỏ qua để xác minh email trước.
            </p>

            <form onSubmit={handleSubmit}>
              <div className="auth-name-row">
                <div className="auth-input-box">
                  <input
                    id="elderly-first-name"
                    type="text"
                    value={values.firstName}
                    onChange={updateField("firstName")}
                    required
                    placeholder=" "
                    autoComplete="given-name"
                  />
                  <label htmlFor="elderly-first-name">Tên</label>
                  <span className="icon">
                    <ion-icon name="person"></ion-icon>
                  </span>
                </div>

                <div className="auth-input-box">
                  <input
                    id="elderly-last-name"
                    type="text"
                    value={values.lastName}
                    onChange={updateField("lastName")}
                    required
                    placeholder=" "
                    autoComplete="family-name"
                  />
                  <label htmlFor="elderly-last-name">Họ</label>
                  <span className="icon">
                    <ion-icon name="people"></ion-icon>
                  </span>
                </div>
              </div>

              <div className="auth-input-box">
                <input
                  id="elderly-phone"
                  type="tel"
                  value={values.phone}
                  onChange={updateField("phone")}
                  required
                  placeholder=" "
                  autoComplete="tel"
                />
                <label htmlFor="elderly-phone">Số điện thoại</label>
                <span className="icon">
                  <ion-icon name="call"></ion-icon>
                </span>
              </div>

              <div className="auth-name-row">
                <div className="auth-input-box">
                  <input
                    id="elderly-date-of-birth"
                    type="date"
                    value={values.dateOfBirth}
                    onChange={updateField("dateOfBirth")}
                    required
                    placeholder=" "
                  />
                  <label htmlFor="elderly-date-of-birth">Ngày sinh</label>
                  <span className="icon">
                    <ion-icon name="calendar"></ion-icon>
                  </span>
                </div>

                <div className="auth-input-box">
                  <select
                    id="elderly-gender"
                    value={values.gender}
                    onChange={updateField("gender")}
                    required
                  >
                    <option value="FEMALE">Nữ</option>
                    <option value="MALE">Nam</option>
                    <option value="OTHER">Khác</option>
                  </select>
                  <label htmlFor="elderly-gender">Giới tính</label>
                  <span className="icon">
                    <ion-icon name="accessibility"></ion-icon>
                  </span>
                </div>
              </div>

              <div className="auth-input-box">
                <input
                  id="elderly-address"
                  type="text"
                  value={values.address}
                  onChange={updateField("address")}
                  placeholder=" "
                  autoComplete="street-address"
                />
                <label htmlFor="elderly-address">Địa chỉ</label>
                <span className="icon">
                  <ion-icon name="home"></ion-icon>
                </span>
              </div>

              <div className="auth-name-row">
                <div className="auth-input-box">
                  <input
                    id="caregiver-title"
                    type="text"
                    value={values.caregiverTitle}
                    onChange={updateField("caregiverTitle")}
                    placeholder=" "
                  />
                  <label htmlFor="caregiver-title">Vai trò của bạn</label>
                  <span className="icon">
                    <ion-icon name="heart"></ion-icon>
                  </span>
                </div>

                <div className="auth-input-box">
                  <select
                    id="elderly-relation"
                    value={values.relation}
                    onChange={updateField("relation")}
                    required
                  >
                    {FAMILY_RELATION_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  <label htmlFor="elderly-relation">Quan hệ</label>
                  <span className="icon">
                    <ion-icon name="medkit"></ion-icon>
                  </span>
                </div>
              </div>

              {values.relation === "OTHER" ? (
                <div className="auth-input-box">
                  <input
                    id="elderly-custom-relation"
                    type="text"
                    value={values.customRelation}
                    onChange={updateField("customRelation")}
                    placeholder=" "
                  />
                  <label htmlFor="elderly-custom-relation">Quan hệ khác</label>
                  <span className="icon">
                    <ion-icon name="people"></ion-icon>
                  </span>
                </div>
              ) : null}

              <button
                id="elderly-submit"
                type="submit"
                disabled={isLoading}
                className="auth-btn-form"
                style={{ marginTop: 32 }}
              >
                {isLoading ? (
                  <>
                    <span className="spinner"></span>
                    Đang tạo hồ sơ...
                  </>
                ) : (
                  "Tạo hồ sơ và xác minh email"
                )}
              </button>
            </form>

            <div className="auth-login-register">
              <p>
                Thêm sau?{" "}
                <button type="button" className="auth-resend-btn" onClick={goToVerifyEmail}>
                  Xác minh email trước
                </button>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ElderlySetupPage;
