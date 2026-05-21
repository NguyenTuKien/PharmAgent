import { CameraCapture } from '../components/ui/CameraCapture.jsx'
import { Camera, ExternalLink, ImagePlus, Loader2, Search, UploadCloud } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'

import { Button } from '../components/ui/Button.jsx'
import { apiClient, getApiErrorMessage } from '../lib/apiClient.js'
import { notify } from '../lib/toast.js'

function percent(score) {
  if (typeof score !== 'number') {
    return null
  }
  return `${Math.round(score * 100)}%`
}

function unique(values) {
  return [...new Set(values.filter(Boolean))]
}

function candidateId(candidate) {
  return String(candidate?.product_id ?? candidate?.id ?? candidate?.title ?? '')
}

function candidateTitle(candidate) {
  return candidate?.display_name ?? candidate?.title ?? 'Không rõ tên thuốc'
}

function candidateImage(candidate) {
  return candidate?.primary_image_url ?? candidate?.image_url
}

function decisionLabel(decision) {
  if (decision === 'accept') {
    return 'Tin cậy'
  }
  if (decision === 'not_found') {
    return 'Không tìm thấy'
  }
  return 'Cần xem lại'
}

export function ScanPage() {
  const fileInputRef = useRef(null)
  const [cameraOpen, setCameraOpen] = useState(false)
  const [loadingMode, setLoadingMode] = useState('')
  const [result, setResult] = useState(null)
  const [selectedFile, setSelectedFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [selectedCandidateId, setSelectedCandidateId] = useState('')
  const [searchText, setSearchText] = useState('')
  const loading = Boolean(loadingMode)

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl)
      }
    }
  }, [previewUrl])

  const handleCameraCapture = (file) => {
    setSelectedFile(file)
    setResult(null)
    setSelectedCandidateId('')
    setPreviewUrl((currentUrl) => {
      if (currentUrl) {
        URL.revokeObjectURL(currentUrl)
      }
      return URL.createObjectURL(file)
    })
  }

  const applyResult = (payload) => {
    setSelectedCandidateId('')
    setResult(payload)
  }

  const chooseImage = (event) => {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }

    setSelectedFile(file)
    setResult(null)
    setSelectedCandidateId('')
    setPreviewUrl((currentUrl) => {
      if (currentUrl) {
        URL.revokeObjectURL(currentUrl)
      }
      return URL.createObjectURL(file)
    })
  }

  const analyzeImage = async () => {
    if (!selectedFile) {
      notify.warning('Chưa chọn ảnh thuốc')
      return
    }

    const formData = new FormData()
    formData.append('image', selectedFile)
    formData.append('top_k', '4')

    try {
      setLoadingMode('image')
      const response = await apiClient.post('/agent/analyze', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      applyResult(response.data)
    } catch (error) {
      notify.error('Phân tích ảnh thất bại', {
        description: getApiErrorMessage(error),
      })
    } finally {
      setLoadingMode('')
    }
  }

  const searchByText = async () => {
    const text = searchText.trim()
    if (!text) {
      notify.warning('Chưa nhập text để tìm thuốc')
      return
    }

    try {
      setLoadingMode('text')
      const response = await apiClient.post('/agent/search', {
        ocr_text: text,
        top_k: 4,
      })
      applyResult(response.data)
    } catch (error) {
      notify.error('Tìm thuốc thất bại', {
        description: getApiErrorMessage(error),
      })
    } finally {
      setLoadingMode('')
    }
  }

  const candidates = result?.match?.top_candidates ?? result?.ui?.top_candidates ?? []
  const selectedCandidate = selectedCandidateId
    ? candidates.find((candidate) => candidateId(candidate) === selectedCandidateId)
    : null
  const primaryCandidate = selectedCandidate ?? result?.match?.best_match ?? candidates[0]
  const ocrText = result?.ocr?.display_text || result?.ocr?.raw_text || ''
  const decision = result?.match?.decision
  const chips = unique([
    primaryCandidate?.source_category ?? result?.ui?.category,
    primaryCandidate?.brand_name,
    primaryCandidate?.manufacturer_name,
  ])

  return (
    <div className="medicalocr-scan">
      <section className="work-panel medicalocr-upload-panel">
        <div className="medicalocr-panel-heading">
          <div>
            <p className="eyebrow">MedicalOCR</p>
            <h2>Quét thuốc từ ảnh</h2>
            <p>Chọn ảnh bao bì/vỉ thuốc, hệ thống sẽ OCR và so khớp với database thuốc.</p>
          </div>
        </div>

        <button
          className={previewUrl ? 'image-dropzone has-image' : 'image-dropzone'}
          type="button"
          onClick={() => fileInputRef.current?.click()}
        >
          {previewUrl ? (
            <img alt="Ảnh thuốc đã chọn" src={previewUrl} />
          ) : (
            <span>
              <ImagePlus size={42} />
              <strong>Chọn ảnh thuốc</strong>
              <small>Hỗ trợ ảnh hộp, vỉ hoặc nhãn thuốc.</small>
            </span>
          )}
        </button>

        <input
          ref={fileInputRef}
          accept="image/*"
          className="visually-hidden"
          type="file"
          onChange={chooseImage}
        />

        <div className="inline-actions scan-actions">
          <Button variant="ghost" onClick={() => setCameraOpen(true)}>
            <Camera size={18} />
            Chụp ảnh
          </Button>
          <Button variant="ghost" onClick={() => fileInputRef.current?.click()}>
            <ImagePlus size={18} />
            Chọn ảnh
          </Button>
          <Button disabled={loading || !selectedFile} variant="primary" onClick={analyzeImage}>
            {loadingMode === 'image' ? <Loader2 className="spin-icon" size={18} /> : <UploadCloud size={18} />}
            {loadingMode === 'image' ? 'Đang phân tích' : 'Phân tích ảnh'}
          </Button>
        </div>

        <div className="text-search-box">
          <div>
            <p className="eyebrow">Tìm thuốc bằng tên</p>
          </div>
          <textarea
            className="text-search-input"
            placeholder="Ví dụ: Panadol Extra 500mg"
            rows={3}
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
          />
          <Button disabled={loading || !searchText.trim()} variant="secondary" onClick={searchByText}>
            {loadingMode === 'text' ? <Loader2 className="spin-icon" size={18} /> : <Search size={18} />}
            {loadingMode === 'text' ? 'Đang tìm' : 'Tìm thuốc'}
          </Button>
        </div>
      </section>

      <section className="work-panel medicalocr-result-panel">
        <div className="medicalocr-panel-heading">
          <div>
            <p className="eyebrow">Kết quả</p>
            <h2>Nhận diện thuốc</h2>
          </div>
          {decision ? <span className={`decision-pill ${decision}`}>{decisionLabel(decision)}</span> : null}
        </div>

        {loading ? (
          <div className="medicalocr-loading">
            <Loader2 className="spin-icon" size={32} />
            <p>{loadingMode === 'text' ? 'Đang tìm trong database...' : 'Đang OCR và so khớp database...'}</p>
          </div>
        ) : primaryCandidate ? (
          <div className="medicalocr-results">
            <article className="primary-drug-card">
              <div className="drug-image-frame">
                {candidateImage(primaryCandidate) ? (
                  <img alt={candidateTitle(primaryCandidate)} src={candidateImage(primaryCandidate)} />
                ) : (
                  <Search size={38} />
                )}
              </div>
              <div>
                <h3>{candidateTitle(primaryCandidate)}</h3>
                {primaryCandidate?.source_url ? (
                  <a className="source-link" href={primaryCandidate.source_url} rel="noreferrer" target="_blank">
                    <ExternalLink size={16} />
                    Xem trên Pharmacity
                  </a>
                ) : null}
                {chips.length ? (
                  <div className="chip-row">
                    {chips.map((chip) => (
                      <span className="info-chip" key={chip}>
                        {chip}
                      </span>
                    ))}
                  </div>
                ) : null}
                {primaryCandidate?.indication ?? result?.ui?.indication ? (
                  <p className="drug-indication">
                    {primaryCandidate?.indication ?? result.ui.indication}
                  </p>
                ) : null}
              </div>
            </article>

            <div className="medicalocr-grid">
              <div className="candidate-panel">
                <div className="medicalocr-section-heading">
                  <h3>Top 4</h3>
                </div>
                <div className="candidate-list">
                  {candidates.slice(0, 4).map((candidate) => {
                    const id = candidateId(candidate)
                    const isActive = id === candidateId(primaryCandidate)
                    return (
                      <button
                        className={isActive ? 'candidate-card active' : 'candidate-card'}
                        key={id}
                        type="button"
                        onClick={() => setSelectedCandidateId(id)}
                      >
                        <span className="candidate-thumb">
                          {candidateImage(candidate) ? (
                            <img alt={candidateTitle(candidate)} src={candidateImage(candidate)} />
                          ) : (
                            <Search size={22} />
                          )}
                        </span>
                        <strong>{candidateTitle(candidate)}</strong>
                        <em>{percent(candidate.score)}</em>
                      </button>
                    )
                  })}
                </div>
              </div>

              <div className="ocr-panel">
                <div className="medicalocr-section-heading">
                  <h3>Text đầu vào</h3>
                  {percent(result?.ocr?.confidence) ? <span>{percent(result.ocr.confidence)}</span> : null}
                </div>
                <p>{ocrText || 'Chưa có text đầu vào.'}</p>
                {result?.ocr?.blocks?.length ? (
                  <div className="chip-row">
                    {result.ocr.blocks.slice(0, 10).map((block, index) => (
                      <span className="info-chip" key={`${block.text}-${index}`}>
                        {block.text}
                      </span>
                    ))}
                  </div>
                ) : null}
              </div>
            </div>
          </div>
        ) : (
          <div className="empty-state compact">
            <h2>Chưa có kết quả</h2>
            <p>Kết quả sẽ hiển thị sau khi phân tích ảnh hoặc tìm bằng text.</p>
          </div>
        )}
      </section>

      <CameraCapture
        open={cameraOpen}
        onClose={() => setCameraOpen(false)}
        onCapture={handleCameraCapture}
      />
    </div>
  )
}
