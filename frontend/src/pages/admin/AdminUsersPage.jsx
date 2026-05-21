import { useCallback, useEffect, useState } from 'react'
import {
  LockKeyhole,
  LockKeyholeOpen,
  Pencil,
  Plus,
  Trash2,
  UserCheck,
  UserX,
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

// ─── Badge trạng thái tài khoản ──────────────────────────────────────────────
function StatusBadge({ locked }) {
  return locked ? (
    <span className="admin-badge admin-badge--danger">
      <UserX size={12} /> Đã khóa
    </span>
  ) : (
    <span className="admin-badge admin-badge--success">
      <UserCheck size={12} /> Hoạt động
    </span>
  )
}

// ─── Form Thêm / Sửa tài khoản ───────────────────────────────────────────────
function UserFormDrawer({ editTarget, open, onClose, onSaved }) {
  const isEdit = Boolean(editTarget)
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'ELDERLY',
  })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (editTarget) {
      setForm({
        firstName: editTarget.firstName ?? '',
        lastName: editTarget.lastName ?? '',
        email: editTarget.email ?? '',
        password: '',
        role: editTarget.role ?? 'ELDERLY',
      })
    } else {
      setForm({ firstName: '', lastName: '', email: '', password: '', role: 'ELDERLY' })
    }
  }, [editTarget, open])

  const set = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }))

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    try {
      const payload = { ...form }
      if (isEdit && !payload.password) delete payload.password
      if (isEdit) {
        await updateUser(editTarget.id, payload)
        notify.success('Cập nhật tài khoản thành công')
      } else {
        await createUser(payload)
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
        <form className="admin-form" onSubmit={handleSubmit}>
          <div className="admin-form-row">
            <div className="admin-field">
              <label>Họ</label>
              <input required value={form.lastName} onChange={set('lastName')} placeholder="Nguyễn" />
            </div>
            <div className="admin-field">
              <label>Tên</label>
              <input required value={form.firstName} onChange={set('firstName')} placeholder="Văn A" />
            </div>
          </div>
          <div className="admin-field">
            <label>Email</label>
            <input type="email" required value={form.email} onChange={set('email')} placeholder="user@example.com" disabled={isEdit} />
          </div>
          <div className="admin-field">
            <label>{isEdit ? 'Mật khẩu mới (để trống nếu không đổi)' : 'Mật khẩu'}</label>
            <input
              type="password"
              required={!isEdit}
              value={form.password}
              onChange={set('password')}
              placeholder={isEdit ? '••••••••' : 'Ít nhất 8 ký tự'}
            />
          </div>
          <div className="admin-field">
            <label>Vai trò</label>
            <select value={form.role} onChange={set('role')}>
              <option value="ELDERLY">Người dùng (ELDERLY)</option>
              <option value="CAREGIVER">Người chăm sóc (CAREGIVER)</option>
              <option value="ADMIN">Quản trị viên (ADMIN)</option>
            </select>
          </div>
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
  const [loading, setLoading] = useState(false)

  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)

  const fetchUsers = useCallback(async (p = page) => {
    setLoading(true)
    try {
      const result = await getAllUsers({ page: p, size: 10 })
      if (result && result.content) {
        result.content = result.content.map((u) => ({
          ...u,
          locked: u.userStatus === 'LOCKED',
        }))
      }
      setData(result)
    } catch (err) {
      notify.apiError(err, 'Không thể tải danh sách người dùng')
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => { fetchUsers(page) }, [page])

  async function handleLockToggle(user) {
    try {
      if (user.locked) {
        await unlockUser(user.id)
        notify.success(`Đã mở khóa tài khoản ${user.email}`)
      } else {
        await lockUser(user.id)
        notify.warning(`Đã khóa tài khoản ${user.email}`)
      }
      fetchUsers(page)
    } catch (err) {
      notify.apiError(err, 'Thao tác thất bại')
    }
  }

  async function handleDelete() {
    try {
      await deleteUser(deleteTarget.id)
      notify.success('Đã xóa tài khoản thành công')
      fetchUsers(page)
    } catch (err) {
      notify.apiError(err, 'Không thể xóa tài khoản')
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

      {/* Bảng dữ liệu */}
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
                <th>Người dùng</th>
                <th>Email</th>
                <th>Vai trò</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {data.content.length === 0 ? (
                <tr>
                  <td colSpan={5} className="admin-empty-row">Không có tài khoản nào</td>
                </tr>
              ) : (
                data.content.map((user) => (
                  <tr key={user.id} className="admin-table-row">
                    <td>
                      <div className="admin-user-cell">
                        <div className="avatar-circle admin-avatar">
                          <span>{(user.firstName?.[0] ?? user.email?.[0] ?? '?').toUpperCase()}</span>
                        </div>
                        <span className="admin-user-name">
                          {user.lastName} {user.firstName}
                        </span>
                      </div>
                    </td>
                    <td className="admin-muted">{user.email}</td>
                    <td>
                      <span className="admin-role-badge">{user.role}</span>
                    </td>
                    <td>
                      <StatusBadge locked={user.locked} />
                    </td>
                    <td>
                      <div className="inline-actions">
                        <button
                          className="icon-button"
                          title={user.locked ? 'Mở khóa' : 'Khóa tài khoản'}
                          onClick={() => handleLockToggle(user)}
                        >
                          {user.locked ? <LockKeyholeOpen size={16} /> : <LockKeyhole size={16} />}
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

      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />

      <UserFormDrawer
        editTarget={editTarget}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onSaved={() => fetchUsers(page)}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Xóa tài khoản"
        description={`Bạn có chắc chắn muốn xóa tài khoản "${deleteTarget?.email}"? Thao tác này không thể hoàn tác.`}
        confirmLabel="Xóa"
        onConfirm={handleDelete}
        onOpenChange={(v) => { if (!v) setDeleteTarget(null) }}
      />
    </div>
  )
}
