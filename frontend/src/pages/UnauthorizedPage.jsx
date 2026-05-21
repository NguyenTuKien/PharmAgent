import { Link } from 'react-router-dom'

export function UnauthorizedPage() {
  return (
    <div className="center-page">
      <h1>Không có quyền truy cập</h1>
      <p>Hồ sơ hiện tại không có quyền để truy cập tới trang này.</p>
      <Link className="btn btn--primary btn--md" to="/dashboard">
        Về trang chủ
      </Link>
    </div>
  )
}
