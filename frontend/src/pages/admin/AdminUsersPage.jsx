import { useCallback, useEffect, useState } from 'react'
import {
  LockKeyhole,
  LockKeyholeOpen,
  Pencil,
  Plus,
  Trash2,
  UserCheck,
  UserX,
  Search,
  SlidersHorizontal,
} from 'lucide-react'

import { Button } from '../../components/ui/Button.jsx'
import { ConfirmDialog } from '../../components/ui/Modal.jsx'
import {
  createUser,
  deleteUser,
  getAllUsers,
  lockUser,
  unlockUser,
  updateUser,
} from '../../modules/admin/adminApi.js'
import { notify } from '../../lib/toast.js'

// ─── Status Badge ────────────────────────────────────────────────────────────
function StatusBadge({ status }) {
  if (status === 'LOCKED') {
    return (
      <span className="admin-badge admin-badge--danger">
        <UserX size={12} /> Đã khóa
      </span>
    )
  }
  if (status === 'INACTIVE') {
    return (
      <span className="admin-badge admin-badge--warning">
        <UserX size={12} /> Chưa kích hoạt
      </span>
    )
  }
  return (
    <span className="admin-badge admin-badge--success">
      <UserCheck size={12} /> Hoạt động
    </span>
  )
}

// ─── Form Thêm / Sửa tài khoản (Drawer) ───────────────────────────────────────
function UserFormDrawer({ editTarget, open, onClose, onSaved }) {
  const isEdit = Boolean(editTarget)
  const [form, setForm] = useState({
    email: '',
    password: '',
    userStatus: 'ACTIVE',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (editTarget) {
      setForm({
        email: editTarget.email ?? '',
        password: '',
        userStatus: editTarget.userStatus ?? 'ACTIVE',
      })
    } else {
      setForm({ email: '', password: '', userStatus: 'ACTIVE' })
    }
    setErrors({})
  }, [editTarget, open])

  const set = (field) => (e) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }))
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: null }))
    }
  }

  function validate() {
    const nextErrors = {}
    if (!form.email) {
      nextErrors.email = 'Email không được để trống'
    } else if (!/\S+@\S+\.\S+/.test(form.email)) {
      nextErrors.email = 'Email không đúng định dạng'
    }

    if (!isEdit) {
      if (!form.password) {
        nextErrors.password = 'Mật khẩu không được để trống'
      } else if (form.password.length < 6) {
        nextErrors.password = 'Mật khẩu phải có ít nhất 6 ký tự'
      }
    } else if (form.password && form.password.length < 6) {
      nextErrors.password = 'Mật khẩu phải có ít nhất 6 ký tự'
    }

    setErrors(nextErrors)
    return Object.keys(nextErrors).length === 0
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!validate()) return

    setLoading(true)
    try {
      if (isEdit) {
        const payload = {
          email: form.email,
          userStatus: form.userStatus,
        }
        await updateUser(editTarget.id, payload)
        notify.success('Cập nhật tài khoản thành công')
      } else {
        await createUser({
          email: form.email,
          password: form.password,
        })
        notify.success('Tạo tài khoản thành công')
      }
      onSaved()
      onClose()
    } catch (err) {
      notify.apiError(err, 'Không thể lưu tài khoản')
    } finally {
      setLoading(false)
    }
  }

  if (!open) return null

  return (
    <>
      <div className="admin-drawer-overlay" onClick={onClose} />
      <aside className="admin-drawer" aria-modal="true" role="dialog">
        <div className="admin-drawer-header">
          <h2>{isEdit ? 'Chỉnh sửa tài khoản' : 'Thêm tài khoản mới'}</h2>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Đóng">
            ✕
          </button>
        </div>
        <form className="admin-form" onSubmit={handleSubmit} noValidate>
          <div className="admin-field">
            <label>Email</label>
            <input
              type="email"
              required
              className={errors.email ? 'has-error' : ''}
              value={form.email}
              onChange={set('email')}
              placeholder="user@example.com"
              disabled={isEdit}
            />
            {errors.email && <span className="admin-field-error">{errors.email}</span>}
          </div>

          {!isEdit && (
            <div className="admin-field">
              <label>Mật khẩu</label>
              <input
                type="password"
                required
                className={errors.password ? 'has-error' : ''}
                value={form.password}
                onChange={set('password')}
                placeholder="Ít nhất 6 ký tự"
              />
              {errors.password && <span className="admin-field-error">{errors.password}</span>}
            </div>
          )}

          {isEdit && (
            <div className="admin-field">
              <label>Trạng thái tài khoản</label>
              <select value={form.userStatus} onChange={set('userStatus')}>
                <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                <option value="INACTIVE">Chưa kích hoạt (INACTIVE)</option>
                <option value="LOCKED">Đã khóa (LOCKED)</option>
              </select>
            </div>
          )}

          <div className="admin-drawer-actions">
            <Button variant="ghost" type="button" onClick={onClose}>Hủy</Button>
            <Button variant="primary" type="submit" disabled={loading}>
              {loading ? 'Đang lưu…' : isEdit ? 'Lưu thay đổi' : 'Tạo tài khoản'}
            </Button>
          </div>
        </form>
      </aside>
    </>
  )
}

// ─── Phân trang ───────────────────────────────────────────────────────────────
function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null
  return (
    <div className="admin-pagination">
      <Button size="sm" variant="ghost" disabled={page === 0} onClick={() => onChange(page - 1)}>
        ‹ Trước
      </Button>
      <span className="admin-page-info">Trang {page + 1} / {totalPages}</span>
      <Button size="sm" variant="ghost" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Tiếp ›
      </Button>
    </div>
  )
}

// ─── Trang chính: Quản lý Tài khoản ─────────────────────────────────────────
export function AdminUsersPage() {
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 })
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [loading, setLoading] = useState(false)

  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [lockTarget, setLockTarget] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)

  const fetchUsers = useCallback(async (p = page) => {
    setLoading(true)
    try {
      const result = await getAllUsers({ page: p, size: 10 })
      setData(result)
    } catch (err) {
      notify.apiError(err, 'Không thể tải danh sách người dùng')
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    fetchUsers(page)
  }, [page, fetchUsers])

  // Lọc cục bộ trên page hiện tại
  const filteredUsers = (data.content || []).filter((user) => {
    const matchesSearch = user.email?.toLowerCase().includes(search.toLowerCase())
    const matchesFilter = statusFilter === 'ALL' || user.userStatus === statusFilter
    return matchesSearch && matchesFilter
  })

  async function handleLockConfirm() {
    if (!lockTarget) return
    try {
      if (lockTarget.userStatus === 'LOCKED') {
        await unlockUser(lockTarget.id)
        notify.success(`Đã mở khóa tài khoản ${lockTarget.email}`)
      } else {
        await lockUser(lockTarget.id)
        notify.warning(`Đã khóa tài khoản ${lockTarget.email}`)
      }
      fetchUsers(page)
    } catch (err) {
      notify.apiError(err, 'Thao tác thất bại')
    } finally {
      setLockTarget(null)
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return
    try {
      await deleteUser(deleteTarget.id)
      notify.success('Đã xóa tài khoản thành công')
      fetchUsers(page)
    } catch (err) {
      notify.apiError(err, 'Không thể xóa tài khoản')
    } finally {
      setDeleteTarget(null)
    }
  }

  function openCreate() {
    setEditTarget(null)
    setDrawerOpen(true)
  }

  function openEdit(user) {
    setEditTarget(user)
    setDrawerOpen(true)
  }

  function formatDate(isoString) {
    if (!isoString) return '—'
    return new Date(isoString).toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return (
    <div className="admin-page">
      {/* Header */}
      <div className="admin-page-header">
        <div>
          <p className="eyebrow">Quản trị viên</p>
          <h2 className="admin-page-title">Quản lý tài khoản</h2>
          <p className="admin-page-subtitle">
            {data.totalElements} tài khoản trong hệ thống
          </p>
        </div>
        <Button variant="primary" onClick={openCreate}>
          <Plus size={16} /> Thêm tài khoản
        </Button>
      </div>

      {/* Toolbar lọc / tìm kiếm */}
      <div className="admin-filter-bar">
        <div className="admin-search-wrap" style={{ flex: 1, minWidth: '240px' }}>
          <Search size={16} className="admin-search-icon" />
          <input
            className="admin-search-input"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Tìm theo email tài khoản…"
          />
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <SlidersHorizontal size={16} className="admin-muted" />
          <select
            className="admin-status-filter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">Hoạt động</option>
            <option value="INACTIVE">Chưa kích hoạt</option>
            <option value="LOCKED">Đã khóa</option>
          </select>
        </div>
      </div>

      {/* Bảng dữ liệu (Desktop) */}
      <div className="admin-table-wrap">
        {loading ? (
          <div className="admin-loading">
            <span className="admin-spinner" />
            Đang tải…
          </div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>Email tài khoản</th>
                <th>Trạng thái</th>
                <th>Ngày tạo</th>
                <th>Cập nhật cuối</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={5} className="admin-empty-row">
                    Không tìm thấy tài khoản nào khớp điều kiện
                  </td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr key={user.id} className="admin-table-row">
                    <td className="admin-pill-name">{user.email}</td>
                    <td>
                      <StatusBadge status={user.userStatus} />
                    </td>
                    <td className="admin-muted">{formatDate(user.createdAt)}</td>
                    <td className="admin-muted">{formatDate(user.updatedAt)}</td>
                    <td>
                      <div className="inline-actions">
                        <button
                          className="icon-button"
                          title={user.userStatus === 'LOCKED' ? 'Mở khóa' : 'Khóa tài khoản'}
                          onClick={() => setLockTarget(user)}
                        >
                          {user.userStatus === 'LOCKED' ? (
                            <LockKeyholeOpen size={16} />
                          ) : (
                            <LockKeyhole size={16} />
                          )}
                        </button>
                        <button
                          className="icon-button"
                          title="Chỉnh sửa"
                          onClick={() => openEdit(user)}
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          className="icon-button admin-icon-danger"
                          title="Xóa"
                          onClick={() => setDeleteTarget(user)}
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* Mobile responsive view (Cards) */}
      <div className="admin-card-grid">
        {loading ? (
          <div className="admin-loading">
            <span className="admin-spinner" />
            Đang tải…
          </div>
        ) : filteredUsers.length === 0 ? (
          <div className="admin-empty-row">Không tìm thấy tài khoản nào khớp điều kiện</div>
        ) : (
          filteredUsers.map((user) => (
            <div key={user.id} className="admin-card">
              <div className="admin-card-header" style={{ justifyContent: 'space-between' }}>
                <strong style={{ wordBreak: 'break-all' }}>{user.email}</strong>
                <StatusBadge status={user.userStatus} />
              </div>
              <div className="admin-card-body" style={{ marginTop: 8 }}>
                <dt>Ngày tạo:</dt>
                <dd>{formatDate(user.createdAt)}</dd>
                <dt>Cập nhật:</dt>
                <dd>{formatDate(user.updatedAt)}</dd>
              </div>
              <div className="admin-card-actions">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setLockTarget(user)}
                >
                  {user.userStatus === 'LOCKED' ? 'Mở khóa' : 'Khóa'}
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => openEdit(user)}
                >
                  Sửa
                </Button>
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => setDeleteTarget(user)}
                >
                  Xóa
                </Button>
              </div>
            </div>
          ))
        )}
      </div>

      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />

      {/* Drawer */}
      <UserFormDrawer
        editTarget={editTarget}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onSaved={() => fetchUsers(page)}
      />

      {/* Confirm lock dialog */}
      <ConfirmDialog
        open={Boolean(lockTarget)}
        title={lockTarget?.userStatus === 'LOCKED' ? 'Mở khóa tài khoản' : 'Khóa tài khoản'}
        description={`Bạn có chắc chắn muốn ${
          lockTarget?.userStatus === 'LOCKED' ? 'mở khóa' : 'khóa'
        } tài khoản "${lockTarget?.email}"?`}
        confirmLabel="Xác nhận"
        onConfirm={handleLockConfirm}
        onOpenChange={(v) => {
          if (!v) setLockTarget(null)
        }}
      />

      {/* Confirm delete dialog */}
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Xóa tài khoản"
        description={`Bạn có chắc chắn muốn xóa tài khoản "${deleteTarget?.email}"? Thao tác này không thể hoàn tác.`}
        confirmLabel="Xóa"
        onConfirm={handleDelete}
        onOpenChange={(v) => {
          if (!v) setDeleteTarget(null)
        }}
      />
    </div>
  )
}
