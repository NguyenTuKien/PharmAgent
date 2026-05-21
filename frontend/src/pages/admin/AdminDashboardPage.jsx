import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Activity,
  ArrowRight,
  HeartPulse,
  LockKeyhole,
  Pill,
  Plus,
  Shield,
  UserCheck,
  Users,
  UserX,
} from 'lucide-react'
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts'

import { Button } from '../../components/ui/Button.jsx'
import {
  getAgentHealth,
  getAllUsers,
  getPillCatalog,
} from '../../modules/admin/adminApi.js'
import { SystemHealthWidget } from '../../components/admin/SystemHealthWidget.jsx'

const ROLE_COLORS = {
  ELDERLY: '#1f8a70',
  CAREGIVER: '#2f6fed',
  ADMIN: '#b7791f',
}

function KpiCard({ icon: Icon, iconClass = '', label, value, children }) {
  return (
    <div className="admin-kpi-card">
      <div className={`admin-kpi-icon ${iconClass}`}>
        <Icon size={20} />
      </div>
      {children ?? <span className="admin-kpi-value">{value ?? '—'}</span>}
      <span className="admin-kpi-label">{label}</span>
    </div>
  )
}

function HealthKpi({ label, status, icon: Icon = HeartPulse, iconClass }) {
  const isUp = status === 'UP'
  return (
    <div className="admin-kpi-card">
      <div className={`admin-kpi-icon ${iconClass ?? (isUp ? '' : 'admin-kpi-icon--danger')}`}>
        <Icon size={20} />
      </div>
      <span className={`admin-kpi-status ${isUp ? 'admin-kpi-status--up' : 'admin-kpi-status--down'}`}>
        <span className={`health-dot ${isUp ? 'health-dot--up' : 'health-dot--down'}`} />
        {status ?? 'UNKNOWN'}
      </span>
      <span className="admin-kpi-label">{label}</span>
    </div>
  )
}

export function AdminDashboardPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState({
    totalUsers: 0,
    activeUsers: 0,
    lockedUsers: 0,
    totalPills: 0,
    agentStatus: null,
    recentUsers: [],
    recentPills: [],
    roleDistribution: [],
  })

  const fetchDashboard = useCallback(async () => {
    setLoading(true)
    const [usersResult, pillsResult, agentResult] = await Promise.allSettled([
      getAllUsers({ page: 0, size: 100 }),
      getPillCatalog({ page: 0, size: 5 }),
      getAgentHealth(),
    ])

    const users = usersResult.status === 'fulfilled' ? usersResult.value : null
    const pills = pillsResult.status === 'fulfilled' ? pillsResult.value : null
    const agent = agentResult.status === 'fulfilled' ? agentResult.value : null

    const userList = users?.content ?? []
    const activeUsers = userList.filter((u) => u.userStatus !== 'LOCKED').length
    const lockedUsers = userList.filter((u) => u.userStatus === 'LOCKED').length

    // Role distribution (approximate from profiles — backend returns email+status only,
    // so we count by status for now since role isn't in AdminUserResponse)
    const statusCounts = {}
    userList.forEach((u) => {
      const key = u.userStatus || 'ACTIVE'
      statusCounts[key] = (statusCounts[key] || 0) + 1
    })
    const roleDistribution = Object.entries(statusCounts).map(([name, value]) => ({
      name,
      value,
    }))

    setStats({
      totalUsers: users?.totalElements ?? 0,
      activeUsers,
      lockedUsers,
      totalPills: pills?.totalElements ?? 0,
      agentStatus: agent?.status ?? 'DOWN',
      recentUsers: userList.slice(0, 5),
      recentPills: (pills?.content ?? []).slice(0, 5),
      roleDistribution,
    })
    setLoading(false)
  }, [])

  useEffect(() => {
    fetchDashboard()
  }, [fetchDashboard])

  const STATUS_COLORS = ['#1f8a70', '#b42318', '#b7791f', '#2f6fed']

  if (loading) {
    return (
      <div className="admin-page">
        <div className="admin-loading">
          <span className="admin-spinner" />
          Đang tải dữ liệu tổng quan…
        </div>
      </div>
    )
  }

  return (
    <div className="admin-page">
      {/* Header */}
      <div className="admin-page-header">
        <div>
          <p className="eyebrow">Tổng quan hệ thống</p>
          <h2 className="admin-page-title">Admin Dashboard</h2>
          <p className="admin-page-subtitle">
            Giám sát vận hành và dữ liệu nền PharmAgent
          </p>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="admin-kpi-grid">
        <KpiCard icon={Users} label="Tổng tài khoản" value={stats.totalUsers} />
        <KpiCard icon={UserCheck} label="Đang hoạt động" value={stats.activeUsers} />
        <KpiCard
          icon={LockKeyhole}
          iconClass="admin-kpi-icon--danger"
          label="Đã khóa"
          value={stats.lockedUsers}
        />
        <KpiCard icon={Pill} iconClass="admin-kpi-icon--blue" label="Tổng thuốc" value={stats.totalPills} />
        <HealthKpi label="Agent AI" status={stats.agentStatus} icon={Activity} />
        <HealthKpi label="Gateway" status="UP" icon={HeartPulse} iconClass="admin-kpi-icon" />
      </div>

      {/* Dashboard grid: Activity + Chart */}
      <div className="admin-dashboard-grid">
        {/* Recent activity */}
        <div className="admin-section">
          <div className="admin-section-header">
            <h3>Hoạt động gần đây</h3>
          </div>
          <div className="admin-activity-list">
            {stats.recentUsers.length === 0 && stats.recentPills.length === 0 ? (
              <p className="admin-muted" style={{ textAlign: 'center', padding: '20px' }}>
                Chưa có hoạt động nào
              </p>
            ) : (
              <>
                {stats.recentUsers.map((user) => (
                  <div key={user.id} className="admin-activity-item">
                    <div className="avatar-circle">
                      <span>{(user.email?.[0] ?? '?').toUpperCase()}</span>
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 750 }}>{user.email}</div>
                      <div className="admin-activity-meta">
                        {user.userStatus === 'LOCKED' ? (
                          <span style={{ color: 'var(--danger)' }}>
                            <UserX size={11} /> Đã khóa
                          </span>
                        ) : (
                          <span style={{ color: 'var(--accent-strong)' }}>
                            <UserCheck size={11} /> Hoạt động
                          </span>
                        )}
                        {user.createdAt && (
                          <span> · {new Date(user.createdAt).toLocaleDateString('vi-VN')}</span>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
                {stats.recentPills.map((pill) => (
                  <div key={pill.id} className="admin-activity-item">
                    <div className="avatar-circle" style={{ background: '#e8f0fd', color: 'var(--blue)' }}>
                      <Pill size={14} />
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 750 }}>{pill.name}</div>
                      <div className="admin-activity-meta">
                        {pill.activeIngredient || pill.manufacturer || 'Thuốc mới'}
                        {' · '}
                        {(pill.images ?? []).length} ảnh
                      </div>
                    </div>
                  </div>
                ))}
              </>
            )}
          </div>
        </div>

        {/* Right column: Quick actions + Chart */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
          {/* Quick Actions */}
          <div className="admin-section">
            <div className="admin-section-header">
              <h3>Thao tác nhanh</h3>
            </div>
            <div className="admin-quick-actions">
              <Button variant="primary" size="sm" onClick={() => navigate('/admin/users?action=create')}>
                <Plus size={15} /> Tạo tài khoản
              </Button>
              <Button variant="secondary" size="sm" onClick={() => navigate('/admin/pills?action=create')}>
                <Plus size={15} /> Thêm thuốc
              </Button>
              <Button variant="ghost" size="sm" onClick={() => navigate('/admin/sessions')}>
                <Shield size={15} /> Sessions
                <ArrowRight size={14} />
              </Button>
            </div>
          </div>

          {/* Status Distribution Chart */}
          <div className="admin-section" style={{ flex: 1 }}>
            <div className="admin-section-header">
              <h3>Phân bố trạng thái</h3>
            </div>
            <div className="admin-chart-wrap">
              {stats.roleDistribution.length === 0 ? (
                <p className="admin-muted">Chưa có dữ liệu</p>
              ) : (
                <ResponsiveContainer width="100%" height={200}>
                  <PieChart>
                    <Pie
                      data={stats.roleDistribution}
                      cx="50%"
                      cy="50%"
                      innerRadius={50}
                      outerRadius={80}
                      paddingAngle={4}
                      dataKey="value"
                      stroke="none"
                    >
                      {stats.roleDistribution.map((entry, index) => (
                        <Cell
                          key={entry.name}
                          fill={STATUS_COLORS[index % STATUS_COLORS.length]}
                        />
                      ))}
                    </Pie>
                    <Tooltip
                      contentStyle={{
                        borderRadius: '8px',
                        border: '1px solid var(--border)',
                        fontSize: '0.84rem',
                      }}
                    />
                    <Legend
                      iconType="circle"
                      iconSize={8}
                      wrapperStyle={{ fontSize: '0.82rem', fontWeight: 700 }}
                    />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>
        </div>
      </div>

      <SystemHealthWidget />
    </div>
  )
}
