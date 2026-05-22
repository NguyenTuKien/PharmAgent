import { Link } from 'react-router-dom'

import { useAuthStore } from '../modules/auth/authStore.js'
import { getProfileLandingPath } from '../modules/auth/session.js'

export function UnauthorizedPage() {
  const activeProfile = useAuthStore((state) => state.activeProfile)

  return (
    <div className="center-page">
      <h1>Không có quyền truy cập</h1>
      <p>Hồ sơ hiện tại không có quyền để truy cập tới trang này.</p>
      <Link className="btn btn--primary btn--md" to={getProfileLandingPath(activeProfile)}>
        Về trang chủ
      </Link>
    </div>
  )
}
