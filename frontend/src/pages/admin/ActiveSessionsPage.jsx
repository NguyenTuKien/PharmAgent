import { useCallback, useEffect, useState } from 'react'
import { RefreshCw, Shield, Users, Clock, Monitor, X, AlertTriangle } from 'lucide-react'
import { Button } from '../../components/ui/Button.jsx'
import { getActiveSessions, revokeSession, revokeAllUserSessions } from '../../modules/admin/adminApi.js'

function formatDateTime(dateString) {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function SessionCard({ session, onRevoke }) {
  const [revoking, setRevoking] = useState(false)

  const handleRevoke = async () => {
    if (!confirm(`Bạn có chắc muốn thu hồi session của ${session.userEmail || session.userId}?`)) {
      return
    }
    
    setRevoking(true)
    try {
      await onRevoke(session.tokenId)
    } catch (error) {
      alert('Lỗi khi thu hồi session: ' + (error.response?.data?.message || error.message))
    } finally {
      setRevoking(false)
    }
  }

  const getRoleBadgeClass = (role) => {
    switch (role?.toUpperCase()) {
      case 'ADMIN':
        return 'admin-role-badge--admin'
      case 'CAREGIVER':
        return 'admin-role-badge--caregiver'
      case 'ELDERLY':
        return 'admin-role-badge--elderly'
      default:
        return ''
    }
  }

  return (
    <div className="session-card">
      <div className="session-card-header">
        <div>
          <div className="session-user">
            <Users size={16} />
            <strong>{session.userEmail || session.userId}</strong>
          </div>
          {session.profileName && (
            <div className="session-profile">
              <span className={`admin-role-badge ${getRoleBadgeClass(session.role)}`}>
                {session.role}
              </span>
              <span>{session.profileName}</span>
            </div>
          )}
        </div>
        <Button
          variant="danger"
          size="sm"
          onClick={handleRevoke}
          disabled={revoking}
        >
          <X size={14} />
          {revoking ? 'Đang thu hồi...' : 'Thu hồi'}
        </Button>
      </div>

      <div className="session-card-body">
        <div className="session-info-row">
          <Monitor size={14} />
          <span className="session-info-label">IP:</span>
          <span>{session.ipAddress || '-'}</span>
        </div>
        <div className="session-info-row">
          <Clock size={14} />
          <span className="session-info-label">Đăng nhập:</span>
          <span>{formatDateTime(session.loginAt)}</span>
        </div>
        <div className="session-info-row">
          <Clock size={14} />
          <span className="session-info-label">Hết hạn:</span>
          <span>{formatDateTime(session.expiresAt)}</span>
        </div>
        {session.userAgent && (
          <div className="session-info-row">
            <span className="session-info-label">User Agent:</span>
            <span className="session-user-agent">{session.userAgent}</span>
          </div>
        )}
      </div>
    </div>
  )
}

export function ActiveSessionsPage() {
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [sessions, setSessions] = useState([])
  const [totalSessions, setTotalSessions] = useState(0)
  const [lastUpdate, setLastUpdate] = useState(null)

  const fetchSessions = useCallback(async () => {
    try {
      const data = await getActiveSessions()
      setSessions(data.sessions || [])
      setTotalSessions(data.totalSessions || 0)
      setLastUpdate(new Date())
    } catch (error) {
      console.error('Failed to fetch sessions:', error)
      alert('Lỗi khi tải danh sách sessions: ' + (error.response?.data?.message || error.message))
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [])

  useEffect(() => {
    fetchSessions()
  }, [fetchSessions])

  const handleRefresh = async () => {
    setRefreshing(true)
    await fetchSessions()
  }

  const handleRevoke = async (tokenId) => {
    await revokeSession(tokenId)
    await fetchSessions()
  }

  const groupedSessions = sessions.reduce((acc, session) => {
    const email = session.userEmail || session.userId
    if (!acc[email]) {
      acc[email] = []
    }
    acc[email].push(session)
    return acc
  }, {})

  return (
    <div className="admin-page">
      {/* Header */}
      <div className="admin-page-header">
        <div>
          <p className="eyebrow">Quản lý bảo mật</p>
          <h2 className="admin-page-title">Active Sessions</h2>
          <p className="admin-page-subtitle">
            Quản lý phiên đăng nhập đang hoạt động • {totalSessions} session(s) đang hoạt động
            {lastUpdate && (
              <> · Cập nhật lúc {lastUpdate.toLocaleTimeString('vi-VN')}</>
            )}
          </p>
        </div>
        <Button variant="secondary" onClick={handleRefresh} disabled={refreshing}>
          <RefreshCw size={16} className={refreshing ? 'spin-icon' : ''} />
          {refreshing ? 'Đang tải...' : 'Làm mới'}
        </Button>
      </div>

      {loading ? (
        <div className="admin-loading">
          <span className="admin-spinner" />
          Đang tải sessions...
        </div>
      ) : totalSessions === 0 ? (
        <div className="admin-empty-state">
          <Shield size={48} />
          <h3>Không có session nào đang hoạt động</h3>
          <p>Chưa có người dùng nào đăng nhập vào hệ thống</p>
        </div>
      ) : (
        <div className="sessions-container">
          {Object.entries(groupedSessions).map(([email, userSessions]) => (
            <div key={email} className="user-sessions-group">
              <div className="user-sessions-header">
                <div>
                  <h3>{email}</h3>
                  <p>{userSessions.length} session(s)</p>
                </div>
                {userSessions.length > 1 && (
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={async () => {
                      if (!confirm(`Thu hồi TẤT CẢ ${userSessions.length} sessions của ${email}?`)) {
                        return
                      }
                      try {
                        await revokeAllUserSessions(userSessions[0].userId)
                        await fetchSessions()
                      } catch (error) {
                        alert('Lỗi: ' + (error.response?.data?.message || error.message))
                      }
                    }}
                  >
                    <AlertTriangle size={14} />
                    Thu hồi tất cả
                  </Button>
                )}
              </div>
              <div className="sessions-grid">
                {userSessions.map((session) => (
                  <SessionCard
                    key={session.tokenId}
                    session={session}
                    onRevoke={handleRevoke}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
