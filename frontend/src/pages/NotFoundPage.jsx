import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="center-page">
      <h1>Không tìm thấy trang</h1>
      <p>Đường dẫn này không tồn tại trong frontend hiện tại.</p>
      <Link className="btn btn--primary btn--md" to="/dashboard">
        Về trang chủ
      </Link>
    </div>
  )
}
