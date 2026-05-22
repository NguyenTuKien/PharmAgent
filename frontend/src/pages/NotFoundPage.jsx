import { Link } from 'react-router-dom'

import { useAuthStore } from '../modules/auth/authStore.js'
import { getProfileLandingPath } from '../modules/auth/session.js'

export function NotFoundPage() {
  const activeProfile = useAuthStore((state) => state.activeProfile)

  return (
    <div className="center-page">
      <h1>Không tìm thấy trang</h1>
      <p>Đường dẫn này không tồn tại trong frontend hiện tại.</p>
      <Link className="btn btn--primary btn--md" to={getProfileLandingPath(activeProfile)}>
        Về trang chủ
      </Link>
    </div>
  )
}
