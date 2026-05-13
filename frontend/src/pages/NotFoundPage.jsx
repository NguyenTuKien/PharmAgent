import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="center-page">
      <h1>Khong tim thay trang</h1>
      <p>Duong dan nay khong ton tai trong frontend hien tai.</p>
      <Link className="btn btn--primary btn--md" to="/dashboard">
        Ve tong quan
      </Link>
    </div>
  )
}
