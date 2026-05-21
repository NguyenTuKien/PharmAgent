import { useCallback, useEffect, useRef, useState } from 'react'
import { Camera, RefreshCcw, X } from 'lucide-react'
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
    setTimeout(() => {
      setError('')
    }, 0)
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
      setTimeout(() => {
        setError('Không thể truy cập camera. Vui lòng kiểm tra quyền truy cập.')
      }, 0)
    }
  }, [facingMode, stopCamera])

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
      <div className="fixed inset-0 z-[9980] bg-slate-950/45 backdrop-blur-sm" onClick={onClose} />
      <div
        aria-modal="true"
        className="fixed left-1/2 top-1/2 z-[9990] grid w-[min(500px,calc(100vw-32px))] -translate-x-1/2 -translate-y-1/2 gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-2xl shadow-slate-950/25"
        role="dialog"
      >
        <div className="flex items-center justify-between gap-4">
          <h3 className="m-0 text-xl font-black text-slate-950">Chụp ảnh</h3>
          <button
            aria-label="Đóng camera"
            className="grid h-9 w-9 place-items-center rounded-lg border border-slate-200 bg-white text-slate-600 transition hover:border-emerald-200 hover:bg-emerald-50 hover:text-emerald-700 focus:outline-none focus:ring-4 focus:ring-emerald-100"
            type="button"
            onClick={onClose}
          >
            <X size={20} />
          </button>
        </div>
        
        <div className="grid aspect-[4/3] place-items-center overflow-hidden rounded-lg bg-slate-950">
          {error ? (
            <p className="px-5 text-center text-sm font-bold text-white">{error}</p>
          ) : (
            <video 
              ref={videoRef} 
              autoPlay 
              playsInline 
              muted 
              className="h-full w-full object-cover"
            />
          )}
        </div>

        <div className="flex flex-wrap justify-between gap-2">
          <Button variant="ghost" onClick={toggleCamera}>
            <RefreshCcw size={16} />
            Đổi camera
          </Button>
          <Button variant="primary" onClick={handleCapture} disabled={Boolean(error)}>
            <Camera size={16} />
            Chụp
          </Button>
        </div>
      </div>
    </>
  )
}
