import { Link } from 'react-router-dom'

export function UnauthorizedPage() {
  return (
    <div className="center-page">
      <h1>Khong co quyen truy cap</h1>
      <p>Ho so hien tai khong co role phu hop de mo khu vuc nay.</p>
      <Link className="btn btn--primary btn--md" to="/dashboard">
        Ve tong quan
      </Link>
    </div>
  )
}
