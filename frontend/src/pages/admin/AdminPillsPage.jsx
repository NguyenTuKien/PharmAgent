import { useCallback, useEffect, useRef, useState } from 'react'
import { Camera, ImagePlus, Pencil, Plus, Search, Trash2, Upload, X } from 'lucide-react'
import { uploadImageToCloudinary } from '../../lib/uploadImage.js'
import { compressImage } from '../../lib/imageCompressor.js'

import { Button } from '../../components/ui/Button.jsx'
import { ConfirmDialog } from '../../components/ui/Modal.jsx'
import { CameraCapture } from '../../components/ui/CameraCapture.jsx'
import {
  addPillImage,
  createPill,
  deletePill,
  deletePillImage,
  getPillCatalog,
  updatePill,
} from '../../modules/admin/adminApi.js'
import { notify } from '../../lib/toast.js'

// ─── Form Thêm / Sửa thuốc ───────────────────────────────────────────────────
function PillFormDrawer({ editTarget, open, onClose, onSaved }) {
  const isEdit = Boolean(editTarget)
  const [form, setForm] = useState({ name: '', activeIngredient: '', dosage: '', manufacturer: '' })
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (editTarget) {
      setForm({
        name: editTarget.name ?? '',
        activeIngredient: editTarget.activeIngredient ?? '',
        dosage: editTarget.dosage ?? '',
        manufacturer: editTarget.manufacturer ?? '',
      })
    } else {
      setForm({ name: '', activeIngredient: '', dosage: '', manufacturer: '' })
    }
  }, [editTarget, open])

  const set = (field) => (e) => setForm((prev) => ({ ...prev, [field]: e.target.value }))

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    try {
      if (isEdit) {
        await updatePill(editTarget.id, form)
        notify.success('Cập nhật thuốc thành công')
      } else {
        await createPill(form)
        notify.success('Thêm thuốc mới thành công')
      }
      onSaved()
      onClose()
    } catch (err) {
      notify.apiError(err, 'Không thể lưu thông tin thuốc')
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
          <h2>{isEdit ? 'Chỉnh sửa thuốc' : 'Thêm thuốc mới'}</h2>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Đóng">✕</button>
        </div>
        <form className="admin-form" onSubmit={handleSubmit}>
          <div className="admin-field">
            <label>Tên thuốc</label>
            <input required value={form.name} onChange={set('name')} placeholder="Paracetamol 500mg" />
          </div>
          <div className="admin-field">
            <label>Hoạt chất</label>
            <input value={form.activeIngredient} onChange={set('activeIngredient')} placeholder="Paracetamol" />
          </div>
          <div className="admin-field">
            <label>Hàm lượng</label>
            <input value={form.dosage} onChange={set('dosage')} placeholder="500mg" />
          </div>
          <div className="admin-field">
            <label>Nhà sản xuất</label>
            <input value={form.manufacturer} onChange={set('manufacturer')} placeholder="Công ty ABC Pharma" />
          </div>
          <div className="admin-drawer-actions">
            <Button variant="ghost" type="button" onClick={onClose}>Hủy</Button>
            <Button variant="primary" type="submit" disabled={loading}>
              {loading ? 'Đang lưu…' : isEdit ? 'Lưu thay đổi' : 'Thêm thuốc'}
            </Button>
          </div>
        </form>
      </aside>
    </>
  )
}

// ─── Panel quản lý hình ảnh AI của 1 thuốc ───────────────────────────────────
function PillImagePanel({ pill, open, onClose, onUpdated }) {
  const [imageUrl, setImageUrl] = useState('')
  const [loadingAdd, setLoadingAdd] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [deleteImageTarget, setDeleteImageTarget] = useState(null)
  const [showCamera, setShowCamera] = useState(false)
  const fileInputRef = useRef(null)

  async function handleFileChange(e) {
    const file = e.target.files?.[0]
    if (!file) return
    e.target.value = ''
    await handleFileUpload(file)
  }

  async function handleFileUpload(file) {
    setUploading(true)
    try {
      const compressed = await compressImage(file, { maxWidth: 1024, quality: 0.8 })
      const url = await uploadImageToCloudinary(compressed, 'pill')
      await addPillImage(pill.id, { imageUrl: url, viewType: 'OTHER', isPrimary: false })
      notify.success('Upload và thêm ảnh thành công')
      onUpdated()
    } catch (err) {
      notify.apiError(err, 'Upload ảnh thất bại')
    } finally {
      setUploading(false)
    }
  }

  async function handleAddImage(e) {
    e.preventDefault()
    if (!imageUrl.trim()) return
    setLoadingAdd(true)
    try {
      await addPillImage(pill.id, { imageUrl: imageUrl.trim(), viewType: 'OTHER', isPrimary: false })
      notify.success('Đã thêm ảnh nhận diện')
      setImageUrl('')
      onUpdated()
    } catch (err) {
      notify.apiError(err, 'Không thể thêm ảnh')
    } finally {
      setLoadingAdd(false)
    }
  }

  async function handleDeleteImage() {
    try {
      await deletePillImage(pill.id, deleteImageTarget.id)
      notify.success('Đã xóa ảnh')
      onUpdated()
    } catch (err) {
      notify.apiError(err, 'Không thể xóa ảnh')
    } finally {
      setDeleteImageTarget(null)
    }
  }

  if (!open || !pill) return null

  const images = pill.images ?? []

  return (
    <>
      <div className="admin-drawer-overlay" onClick={onClose} />
      <aside className="admin-drawer admin-drawer--wide" aria-modal="true" role="dialog">
        <div className="admin-drawer-header">
          <div>
            <h2>Ảnh nhận diện AI</h2>
            <p className="admin-muted">{pill.name}</p>
          </div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Đóng">✕</button>
        </div>

        {/* Form thêm ảnh */}
        <form className="admin-image-add-form" onSubmit={handleAddImage}>
          <input
            type="url"
            value={imageUrl}
            onChange={(e) => setImageUrl(e.target.value)}
            placeholder="Dán URL ảnh vào đây hoặc nhấn Upload"
          />
          <Button variant="secondary" type="submit" size="sm" disabled={loadingAdd || uploading || !imageUrl.trim()}>
            <ImagePlus size={15} />
            {loadingAdd ? 'Đang thêm…' : 'Thêm URL'}
          </Button>
          <Button
            variant="primary"
            type="button"
            size="sm"
            disabled={uploading || loadingAdd}
            onClick={() => fileInputRef.current?.click()}
          >
            <Upload size={15} />
            Upload ảnh
          </Button>
          <Button
            variant="primary"
            type="button"
            size="sm"
            disabled={uploading || loadingAdd}
            onClick={() => setShowCamera(true)}
          >
            <Camera size={15} />
            Chụp ảnh
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            style={{ display: 'none' }}
            onChange={handleFileChange}
          />
        </form>

        {/* Lưới ảnh */}
        {images.length === 0 ? (
          <div className="admin-image-empty">
            <ImagePlus size={40} />
            <p>Chưa có ảnh mẫu nhận diện nào</p>
          </div>
        ) : (
          <div className="admin-image-grid">
            {images.map((img) => (
              <div key={img.id} className="admin-image-item">
                <img src={img.imageUrl ?? img.url} alt="pill sample" loading="lazy" />
                <button
                  className="admin-image-delete-btn"
                  type="button"
                  title="Xóa ảnh"
                  onClick={() => setDeleteImageTarget(img)}
                >
                  <X size={14} />
                </button>
              </div>
            ))}
          </div>
        )}
      </aside>

      <ConfirmDialog
        open={Boolean(deleteImageTarget)}
        title="Xóa ảnh nhận diện"
        description="Bạn có chắc muốn xóa ảnh mẫu này? Thao tác này không thể hoàn tác."
        confirmLabel="Xóa ảnh"
        onConfirm={handleDeleteImage}
        onOpenChange={(v) => { if (!v) setDeleteImageTarget(null) }}
      />
      
      <CameraCapture 
        open={showCamera}
        onClose={() => setShowCamera(false)}
        onCapture={handleFileUpload}
      />
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

// ─── Trang chính: Quản lý Thư viện Thuốc ─────────────────────────────────────
export function AdminPillsPage() {
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 })
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)

  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [imageTarget, setImageTarget] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)

  const searchTimeout = useRef(null)

  const fetchPills = useCallback(async (p = page, q = search) => {
    setLoading(true)
    try {
      const result = await getPillCatalog({ search: q, page: p, size: 10 })
      setData(result)
    } catch (err) {
      notify.apiError(err, 'Không thể tải danh sách thuốc')
    } finally {
      setLoading(false)
    }
  }, [page, search])

  useEffect(() => { fetchPills(page, search) }, [page])

  function handleSearchChange(e) {
    const q = e.target.value
    setSearch(q)
    clearTimeout(searchTimeout.current)
    searchTimeout.current = setTimeout(() => {
      setPage(0)
      fetchPills(0, q)
    }, 380)
  }

  async function handleDelete() {
    try {
      await deletePill(deleteTarget.id)
      notify.success('Đã xóa thuốc thành công')
      fetchPills(page, search)
    } catch (err) {
      notify.apiError(err, 'Không thể xóa thuốc')
    }
  }

  function openCreate() { setEditTarget(null); setDrawerOpen(true) }
  function openEdit(pill) { setEditTarget(pill); setDrawerOpen(true) }

  return (
    <div className="admin-page">
      {/* Header */}
      <div className="admin-page-header">
        <div>
          <p className="eyebrow">Quản trị viên</p>
          <h2 className="admin-page-title">Thư viện thuốc</h2>
          <p className="admin-page-subtitle">
            {data.totalElements} loại thuốc trong hệ thống nhận diện AI
          </p>
        </div>
        <Button variant="primary" onClick={openCreate}>
          <Plus size={16} /> Thêm thuốc
        </Button>
      </div>

      {/* Tìm kiếm */}
      <div className="admin-search-wrap">
        <Search size={16} className="admin-search-icon" />
        <input
          className="admin-search-input"
          value={search}
          onChange={handleSearchChange}
          placeholder="Tìm theo tên thuốc, hoạt chất…"
        />
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
                <th>Tên thuốc</th>
                <th>Hoạt chất</th>
                <th>Hàm lượng</th>
                <th>Nhà sản xuất</th>
                <th>Ảnh AI</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {data.content.length === 0 ? (
                <tr>
                  <td colSpan={6} className="admin-empty-row">
                    {search ? `Không tìm thấy kết quả cho "${search}"` : 'Chưa có thuốc nào'}
                  </td>
                </tr>
              ) : (
                data.content.map((pill) => (
                  <tr key={pill.id} className="admin-table-row">
                    <td>
                      <span className="admin-pill-name">{pill.name}</span>
                    </td>
                    <td className="admin-muted">{pill.activeIngredient ?? '—'}</td>
                    <td className="admin-muted">{pill.dosage ?? '—'}</td>
                    <td className="admin-muted">{pill.manufacturer ?? '—'}</td>
                    <td>
                      <span className="admin-image-count">
                        {(pill.images ?? []).length} ảnh
                      </span>
                    </td>
                    <td>
                      <div className="inline-actions">
                        <button
                          className="icon-button"
                          title="Quản lý ảnh AI"
                          onClick={() => setImageTarget(pill)}
                        >
                          <ImagePlus size={16} />
                        </button>
                        <button
                          className="icon-button"
                          title="Chỉnh sửa"
                          onClick={() => openEdit(pill)}
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          className="icon-button admin-icon-danger"
                          title="Xóa"
                          onClick={() => setDeleteTarget(pill)}
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

      <PillFormDrawer
        editTarget={editTarget}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onSaved={() => fetchPills(page, search)}
      />

      <PillImagePanel
        pill={imageTarget}
        open={Boolean(imageTarget)}
        onClose={() => setImageTarget(null)}
        onUpdated={() => fetchPills(page, search)}
      />

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Xóa thuốc"
        description={`Bạn có chắc muốn xóa thuốc "${deleteTarget?.name}"? Tất cả ảnh mẫu nhận diện của thuốc này cũng sẽ bị xóa.`}
        confirmLabel="Xóa"
        onConfirm={handleDelete}
        onOpenChange={(v) => { if (!v) setDeleteTarget(null) }}
      />
    </div>
  )
}
