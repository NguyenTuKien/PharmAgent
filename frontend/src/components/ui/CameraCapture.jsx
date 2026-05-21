import { useCallback, useEffect, useRef, useState } from 'react'
import { Camera, X, RefreshCcw } from 'lucide-react'
import { Button } from './Button.jsx'

export function CameraCapture({ open, onClose, onCapture }) {
  const videoRef = useRef(null)
  const streamRef = useRef(null)
  const [error, setError] = useState('')
  const [facingMode, setFacingMode] = useState('environment')

  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop())
      streamRef.current = null
    }
  }, [])

  const startCamera = useCallback(async () => {
    stopCamera()
    setError('')
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode, width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false,
      })
      streamRef.current = stream
      if (videoRef.current) {
        videoRef.current.srcObject = stream
        videoRef.current.play()
      }
    } catch (err) {
      console.error('Camera error:', err)
      setError('Không thể truy cập camera. Vui lòng kiểm tra quyền truy cập.')
    }
  }, [facingMode, stopCamera])

  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => {
    if (open) {
      startCamera()
    } else {
      stopCamera()
    }
    return () => stopCamera()
  }, [open, startCamera, stopCamera])

  const handleCapture = () => {
    if (!videoRef.current) return
    const canvas = document.createElement('canvas')
    canvas.width = videoRef.current.videoWidth
    canvas.height = videoRef.current.videoHeight
    const ctx = canvas.getContext('2d')
    ctx.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height)
    
    canvas.toBlob((blob) => {
      if (!blob) return
      const file = new File([blob], `capture-${Date.now()}.jpg`, { type: 'image/jpeg' })
      onCapture(file)
      onClose()
    }, 'image/jpeg', 0.9)
  }

  const toggleCamera = () => {
    setFacingMode((prev) => (prev === 'user' ? 'environment' : 'user'))
  }

  if (!open) return null

  return (
    <>
      <div className="admin-drawer-overlay" style={{ zIndex: 9998 }} onClick={onClose} />
      <div 
        className="camera-modal" 
        style={{
          position: 'fixed',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          backgroundColor: '#fff',
          padding: '16px',
          borderRadius: '12px',
          zIndex: 9999,
          width: '90%',
          maxWidth: '500px',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px'
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 600 }}>Chụp ảnh</h3>
          <button className="icon-button" onClick={onClose}><X size={20} /></button>
        </div>
        
        <div style={{ backgroundColor: '#000', borderRadius: '8px', overflow: 'hidden', aspectRatio: '4/3', position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          {error ? (
            <p style={{ color: '#fff', padding: '20px', textAlign: 'center' }}>{error}</p>
          ) : (
            <video 
              ref={videoRef} 
              autoPlay 
              playsInline 
              muted 
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />
          )}
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', gap: '8px' }}>
          <Button variant="ghost" onClick={toggleCamera}>
            <RefreshCcw size={16} style={{ marginRight: 8 }} /> Đổi camera
          </Button>
          <Button variant="primary" onClick={handleCapture} disabled={Boolean(error)}>
            <Camera size={16} style={{ marginRight: 8 }} /> Chụp
          </Button>
        </div>
      </div>
    </>
  )
}
