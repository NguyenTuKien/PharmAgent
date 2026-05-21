import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Camera,
  ImagePlus,
  Pencil,
  Plus,
  Search,
  Trash2,
  Upload,
  X,
  Star,
  Eye,
} from 'lucide-react'
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
  getPillById,
} from '../../modules/admin/adminApi.js'
import { notify } from '../../lib/toast.js'

// ─── Form Thêm / Sửa thuốc (Drawer) ───────────────────────────────────────────
function PillFormDrawer({ editTarget, open, onClose, onSaved }) {
  const isEdit = Boolean(editTarget)
  const [form, setForm] = useState({
    name: '',
    activeIngredient: '',
    dosage: '',
    manufacturer: '',
    description: '',
  })
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState({})

  useEffect(() => {
    if (editTarget) {
      setForm({
        name: editTarget.name ?? '',
        activeIngredient: editTarget.activeIngredient ?? '',
        dosage: editTarget.dosage ?? '',
        manufacturer: editTarget.manufacturer ?? '',
        description: editTarget.description ?? '',
      })
    } else {
      setForm({
        name: '',
        activeIngredient: '',
        dosage: '',
        manufacturer: '',
        description: '',
      })
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
    if (!form.name.trim()) {
      nextErrors.name = 'Tên thuốc không được để trống'
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
      <aside className="admin-drawer admin-drawer--wide" aria-modal="true" role="dialog">
        <div className="admin-drawer-header">
          <h2>{isEdit ? 'Chỉnh sửa thuốc' : 'Thêm thuốc mới'}</h2>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Đóng">✕</button>
        </div>
        <form className="admin-form" onSubmit={handleSubmit} noValidate>
          <div className="admin-form-2col">
            {/* Cột trái: Trường nhập liệu */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div className="admin-field">
                <label>Tên thuốc *</label>
                <input
                  required
                  className={errors.name ? 'has-error' : ''}
                  value={form.name}
                  onChange={set('name')}
                  placeholder="Paracetamol 500mg"
                />
                {errors.name && <span className="admin-field-error">{errors.name}</span>}
              </div>

              <div className="admin-field">
                <label>Hoạt chất</label>
                <input
                  value={form.activeIngredient}
                  onChange={set('activeIngredient')}
                  placeholder="Paracetamol"
                />
              </div>

              <div className="admin-field">
                <label>Hàm lượng / Liều lượng</label>
                <input
                  value={form.dosage}
                  onChange={set('dosage')}
                  placeholder="500mg"
                />
              </div>

              <div className="admin-field">
                <label>Nhà sản xuất</label>
                <input
                  value={form.manufacturer}
                  onChange={set('manufacturer')}
                  placeholder="Công ty ABC Pharma"
                />
              </div>

              <div className="admin-field">
                <label>Mô tả bổ sung</label>
                <textarea
                  value={form.description}
                  onChange={set('description')}
                  placeholder="Hướng dẫn bảo quản, lưu ý đặc biệt..."
                />
              </div>
            </div>

            {/* Cột phải: Live Preview */}
            <div className="admin-pill-preview">
              <h4>Xem trước thuốc hiển thị</h4>
              <div className="admin-pill-preview-card">
                <div className="admin-pill-preview-thumb">
                  <Star size={24} style={{ color: 'var(--muted)' }} />
                </div>
                <div className="admin-pill-preview-info">
                  <strong>{form.name || 'Tên thuốc chưa nhập'}</strong>
                  {form.activeIngredient && (
                    <span>Hoạt chất: {form.activeIngredient}</span>
                  )}
                  {form.dosage && <span>Hàm lượng: {form.dosage}</span>}
                  {form.manufacturer && <span>Hãng: {form.manufacturer}</span>}
                </div>
              </div>
            </div>
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
  const [localPill, setLocalPill] = useState(pill)
  const [imageUrl, setImageUrl] = useState('')
  const [viewType, setViewType] = useState('OTHER')
  const [isPrimary, setIsPrimary] = useState(false)

  const [loadingAdd, setLoadingAdd] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [deleteImageTarget, setDeleteImageTarget] = useState(null)
  const [showCamera, setShowCamera] = useState(false)
  const fileInputRef = useRef(null)

  useEffect(() => {
    setLocalPill(pill)
    setImageUrl('')
    setViewType('OTHER')
    setIsPrimary(false)
  }, [pill])

  const reloadPill = async () => {
    if (!pill?.id) return
    try {
      const updated = await getPillById(pill.id)
      setLocalPill(updated)
      onUpdated()
    } catch (err) {
      notify.error('Không thể cập nhật thông tin ảnh')
    }
  }

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
      await addPillImage(localPill.id, { imageUrl: url, viewType, isPrimary })
      notify.success('Upload và thêm ảnh thành công')
      reloadPill()
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
      await addPillImage(localPill.id, {
        imageUrl: imageUrl.trim(),
        viewType,
        isPrimary,
      })
      notify.success('Đã thêm ảnh nhận diện')
      setImageUrl('')
      reloadPill()
    } catch (err) {
      notify.apiError(err, 'Không thể thêm ảnh')
    } finally {
      setLoadingAdd(false)
    }
  }

  async function handleDeleteImage() {
    try {
      await deletePillImage(localPill.id, deleteImageTarget.id)
      notify.success('Đã xóa ảnh')
      reloadPill()
    } catch (err) {
      notify.apiError(err, 'Không thể xóa ảnh')
    } finally {
      setDeleteImageTarget(null)
    }
  }

  if (!open || !localPill) return null

  const images = localPill.images ?? []

  // Nhóm hình ảnh theo viewType
  const groupedImages = {
    FRONT: images.filter((img) => img.viewType === 'FRONT'),
    BACK: images.filter((img) => img.viewType === 'BACK'),
    PACKAGE: images.filter((img) => img.viewType === 'PACKAGE'),
    OTHER: images.filter((img) => img.viewType === 'OTHER' || !img.viewType),
  }

  const viewTypeLabels = {
    FRONT: 'Mặt trước',
    BACK: 'Mặt sau',
    PACKAGE: 'Bao bì',
    OTHER: 'Khác',
  }

  return (
    <>
      <div className="admin-drawer-overlay" onClick={onClose} />
      <aside className="admin-drawer admin-drawer--wide" aria-modal="true" role="dialog">
        <div className="admin-drawer-header">
          <div>
            <h2>Ảnh nhận diện AI</h2>
            <p className="admin-muted">{localPill.name}</p>
          </div>
          <button className="icon-button" type="button" onClick={onClose} aria-label="Đóng">✕</button>
        </div>

        {/* Toolbar & Form thêm ảnh */}
        <form className="admin-form" onSubmit={handleAddImage} style={{ padding: '0 22px 20px', borderBottom: '1px solid var(--border)' }}>
          <div className="admin-field">
            <label>Dán URL ảnh hoặc chọn file upload</label>
            <input
              type="url"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              placeholder="Dán URL ảnh mẫu nhận diện tại đây…"
            />
          </div>

          <div className="admin-image-upload-row">
            <select value={viewType} onChange={(e) => setViewType(e.target.value)}>
              <option value="FRONT">Mặt trước (FRONT)</option>
              <option value="BACK">Mặt sau (BACK)</option>
              <option value="PACKAGE">Bao bì (PACKAGE)</option>
              <option value="OTHER">Khác (OTHER)</option>
            </select>

            <label>
              <input
                type="checkbox"
                checked={isPrimary}
                onChange={(e) => setIsPrimary(e.target.checked)}
              />
              Đặt làm ảnh chính
            </label>

            <Button
              variant="secondary"
              type="submit"
              size="sm"
              disabled={loadingAdd || uploading || !imageUrl.trim()}
            >
              <ImagePlus size={15} /> Thêm URL
            </Button>
            <Button
              variant="primary"
              type="button"
              size="sm"
              disabled={uploading || loadingAdd}
              onClick={() => fileInputRef.current?.click()}
            >
              <Upload size={15} /> Upload file
            </Button>
            <Button
              variant="primary"
              type="button"
              size="sm"
              disabled={uploading || loadingAdd}
              onClick={() => setShowCamera(true)}
            >
              <Camera size={15} /> Chụp ảnh camera
            </Button>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              style={{ display: 'none' }}
              onChange={handleFileChange}
            />
          </div>
        </form>

        {/* Thư viện hình ảnh theo Nhóm */}
        <div style={{ flex: 1, overflowY: 'auto' }}>
          {images.length === 0 ? (
            <div className="admin-image-empty" style={{ margin: '40px 0' }}>
              <ImagePlus size={40} />
              <p>Chưa có ảnh mẫu nhận diện nào</p>
            </div>
          ) : (
            Object.entries(groupedImages).map(([group, groupImgs]) => {
              if (groupImgs.length === 0) return null
              return (
                <div key={group} className="admin-gallery-section">
                  <h4>{viewTypeLabels[group]} ({groupImgs.length})</h4>
                  <div className="admin-image-grid">
                    {groupImgs.map((img) => (
                      <div key={img.id} className="admin-image-item">
                        <img src={img.imageUrl} alt="pill sample" loading="lazy" />
                        {img.isPrimary && (
                          <div className="admin-primary-marker" title="Ảnh chính">
                            ★
                          </div>
                        )}
                        <span className="admin-view-type-badge">
                          {viewTypeLabels[img.viewType ?? 'OTHER']}
                        </span>
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
                </div>
              )
            })
          )}
        </div>
      </aside>

      <ConfirmDialog
        open={Boolean(deleteImageTarget)}
        title="Xóa ảnh nhận diện"
        description="Bạn có chắc muốn xóa ảnh mẫu này? Thao tác này không thể hoàn tác."
        confirmLabel="Xóa ảnh"
        onConfirm={handleDeleteImage}
        onOpenChange={(v) => {
          if (!v) setDeleteImageTarget(null)
        }}
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
  const [detailTarget, setDetailTarget] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)

  const searchTimeout = useRef(null)

  const fetchPills = useCallback(
    async (p = page, q = search) => {
      setLoading(true)
      try {
        const result = await getPillCatalog({ search: q, page: p, size: 10 })
        setData(result)
      } catch (err) {
        notify.apiError(err, 'Không thể tải danh sách thuốc')
      } finally {
        setLoading(false)
      }
    },
    [page, search]
  )

  useEffect(() => {
    fetchPills(page, search)
  }, [page, fetchPills])

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
    } finally {
      setDeleteTarget(null)
    }
  }

  function openCreate() {
    setEditTarget(null)
    setDrawerOpen(true)
  }

  function openEdit(pill) {
    setEditTarget(pill)
    setDrawerOpen(true)
  }

  // Lấy ảnh chính đại diện
  function getPrimaryImage(pill) {
    const images = pill.images ?? []
    return images.find((img) => img.isPrimary) ?? images[0]
  }

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
          placeholder="Tìm theo tên thuốc, hoạt chất, hãng sản xuất…"
        />
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
                <th>Thông tin thuốc</th>
                <th>Hoạt chất</th>
                <th>Hàm lượng</th>
                <th>Nhà sản xuất</th>
                <th>Ảnh nhận diện</th>
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
                data.content.map((pill) => {
                  const primaryImg = getPrimaryImage(pill)
                  return (
                    <tr key={pill.id} className="admin-table-row">
                      <td>
                        <div className="admin-pill-cell">
                          <div className="admin-pill-thumb">
                            {primaryImg ? (
                              <img src={primaryImg.imageUrl} alt={pill.name} />
                            ) : (
                              <Star size={16} />
                            )}
                          </div>
                          <span className="admin-pill-name">{pill.name}</span>
                        </div>
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
                            title="Xem chi tiết"
                            onClick={() => setDetailTarget(pill)}
                          >
                            <Eye size={16} />
                          </button>
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
                  )
                })
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
        ) : data.content.length === 0 ? (
          <div className="admin-empty-row">Chưa có loại thuốc nào</div>
        ) : (
          data.content.map((pill) => {
            const primaryImg = getPrimaryImage(pill)
            return (
              <div key={pill.id} className="admin-card">
                <div className="admin-card-header">
                  <div className="admin-pill-thumb">
                    {primaryImg ? (
                      <img src={primaryImg.imageUrl} alt={pill.name} />
                    ) : (
                      <Star size={16} />
                    )}
                  </div>
                  <div>
                    <strong>{pill.name}</strong>
                    <div style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>
                      {(pill.images ?? []).length} ảnh nhận diện
                    </div>
                  </div>
                </div>
                <div className="admin-card-body">
                  <dt>Hoạt chất:</dt>
                  <dd>{pill.activeIngredient || '—'}</dd>
                  <dt>Hàm lượng:</dt>
                  <dd>{pill.dosage || '—'}</dd>
                  <dt>Hãng SX:</dt>
                  <dd>{pill.manufacturer || '—'}</dd>
                </div>
                <div className="admin-card-actions">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setDetailTarget(pill)}
                  >
                    Chi tiết
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setImageTarget(pill)}
                  >
                    Ảnh AI
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => openEdit(pill)}
                  >
                    Sửa
                  </Button>
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={() => setDeleteTarget(pill)}
                  >
                    Xóa
                  </Button>
                </div>
              </div>
            )
          })
        )}
      </div>

      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />

      {/* Pill Detail Drawer (Read-only view) */}
      <ConfirmDialog
        open={Boolean(detailTarget)}
        title="Chi tiết thông tin thuốc"
        description={
          detailTarget && (
            <div style={{ textAlign: 'left', marginTop: 12 }}>
              <div
                style={{
                  display: 'flex',
                  gap: 16,
                  alignItems: 'center',
                  marginBottom: 16,
                }}
              >
                <div
                  style={{
                    width: 80,
                    height: 80,
                    borderRadius: 8,
                    overflow: 'hidden',
                    border: '1px solid var(--border)',
                    background: 'var(--surface-strong)',
                    display: 'grid',
                    placeItems: 'center',
                  }}
                >
                  {getPrimaryImage(detailTarget) ? (
                    <img
                      src={getPrimaryImage(detailTarget).imageUrl}
                      alt={detailTarget.name}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                    />
                  ) : (
                    <Star size={32} style={{ color: 'var(--muted)' }} />
                  )}
                </div>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 800 }}>
                    {detailTarget.name}
                  </h3>
                  <span style={{ fontSize: '0.86rem', color: 'var(--muted)' }}>
                    ID: {detailTarget.id}
                  </span>
                </div>
              </div>
              <table style={{ width: '100%', fontSize: '0.88rem', borderCollapse: 'collapse' }}>
                <tbody>
                  <tr style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '8px 0', fontWeight: 700, width: '120px' }}>
                      Hoạt chất:
                    </td>
                    <td style={{ padding: '8px 0' }}>{detailTarget.activeIngredient ?? '—'}</td>
                  </tr>
                  <tr style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '8px 0', fontWeight: 700 }}>Hàm lượng:</td>
                    <td style={{ padding: '8px 0' }}>{detailTarget.dosage ?? '—'}</td>
                  </tr>
                  <tr style={{ borderBottom: '1px solid var(--border)' }}>
                    <td style={{ padding: '8px 0', fontWeight: 700 }}>Nhà sản xuất:</td>
                    <td style={{ padding: '8px 0' }}>{detailTarget.manufacturer ?? '—'}</td>
                  </tr>
                  <tr>
                    <td style={{ padding: '8px 0', fontWeight: 700, verticalAlign: 'top' }}>
                      Mô tả chi tiết:
                    </td>
                    <td style={{ padding: '8px 0', whiteSpace: 'pre-wrap' }}>
                      {detailTarget.description ?? 'Không có mô tả bổ sung.'}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          )
        }
        confirmLabel="Đóng"
        onConfirm={() => setDetailTarget(null)}
        onOpenChange={(v) => {
          if (!v) setDetailTarget(null)
        }}
      />

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
        onOpenChange={(v) => {
          if (!v) setDeleteTarget(null)
        }}
      />
    </div>
  )
}
