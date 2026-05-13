import { Camera, PlugZap } from 'lucide-react'
import { useRef, useState } from 'react'
import { toast } from 'sonner'

import { Button } from '../components/ui/Button.jsx'
import { createCameraScanClient } from '../lib/cameraClient.js'
import { useAuthStore } from '../modules/auth/authStore.js'

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

export function ScanPage() {
  const clientRef = useRef(null)
  const [connected, setConnected] = useState(false)
  const [result, setResult] = useState(null)
  const token = useAuthStore((state) => state.accessToken || state.authToken)

  const connect = () => {
    clientRef.current?.close()
    clientRef.current = createCameraScanClient({
      token,
      onOpen: () => {
        setConnected(true)
        toast.success('Da ket noi camera WebSocket')
      },
      onClose: () => setConnected(false),
      onError: () => toast.error('Khong ket noi duoc WebSocket camera'),
      onResult: setResult,
    })
  }

  const sendFrame = async (event) => {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }

    const frame = await readFileAsDataUrl(file)
    const sent = clientRef.current?.sendFrame(frame)
    if (!sent) {
      toast.error('Hay ket noi WebSocket truoc khi gui frame')
    }
  }

  return (
    <div className="scan-layout">
      <section className="work-panel">
        <p className="eyebrow">/ws/agent</p>
        <h2>WebSocket camera client</h2>
        <p>
          Client nay gui frame anh Base64 den gateway, gateway xac thuc token va tunnel sang
          AI Agent nhan dien thuoc real-time.
        </p>
        <div className="inline-actions">
          <Button variant={connected ? 'secondary' : 'primary'} onClick={connect}>
            <PlugZap size={18} />
            {connected ? 'Ket noi lai' : 'Ket noi'}
          </Button>
          <label className="file-button">
            <Camera size={18} />
            Gui frame
            <input accept="image/*" type="file" onChange={sendFrame} />
          </label>
        </div>
      </section>

      <section className="work-panel">
        <p className="eyebrow">Ket qua gan nhat</p>
        {result ? (
          <pre className="result-box">{JSON.stringify(result, null, 2)}</pre>
        ) : (
          <div className="empty-state compact">
            <h2>Chua co ket qua</h2>
            <p>Ket qua nhan dien se hien thi tai day sau khi agent tra ve message.</p>
          </div>
        )}
      </section>
    </div>
  )
}
