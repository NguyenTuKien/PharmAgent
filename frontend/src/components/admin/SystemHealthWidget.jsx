import { useCallback, useEffect, useState } from 'react'
import {
  Activity,
  Cloud,
  Database,
  Globe,
  HardDrive,
  MessageSquare,
  Monitor,
  RefreshCw,
  Server,
  Wifi,
  WifiOff,
  AlertTriangle,
} from 'lucide-react'

import { Button } from '../ui/Button.jsx'
import { getActuatorHealth, getAgentHealth, getSystemHealth } from '../../modules/admin/adminApi.js'

function StatusDot({ status }) {
  const cls =
    status === 'UP' ? 'health-dot--up' : status === 'DOWN' ? 'health-dot--down' : 'health-dot--unknown'
  return <span className={`health-dot ${cls}`} />
}

function StatusText({ status, label }) {
  const cls =
    status === 'UP'
      ? 'health-status-text--up'
      : status === 'DOWN'
        ? 'health-status-text--down'
        : 'health-status-text--unknown'
  const fallbackLabel =
    status === 'UP' ? 'Đang chạy' : status === 'DOWN' ? 'Lỗi' : 'Không xác định'
  return (
    <span className={`health-card-status ${cls}`}>
      <StatusDot status={status} />
      {label ?? fallbackLabel}
    </span>
  )
}

function HealthCard({ icon: Icon, name, status, detail }) {
  return (
    <div className="health-card">
      <div className="health-card-icon">
        <Icon size={22} />
      </div>
      <div className="health-card-info">
        <strong>{name}</strong>
        <StatusText status={status} />
        {detail && <span style={{ fontSize: '0.76rem', color: 'var(--muted)' }}>{detail}</span>}
      </div>
    </div>
  )
}

export function SystemHealthWidget() {
  const [loading, setLoading] = useState(true)
  const [checking, setChecking] = useState(false)
  const [services, setServices] = useState({
    frontend: { status: 'UP', detail: 'Đang chạy' },
    gateway: { status: null, detail: null },
    backend: { status: null, detail: null },
    agent: { status: null, detail: null },
    mongodb: { status: null, detail: null },
    redis: { status: null, detail: null },
    rabbitmq: { status: null, detail: null },
    cloudinary: { status: null, detail: null },
  })
  const [agentDetail, setAgentDetail] = useState(null)
  const [lastCheck, setLastCheck] = useState(null)

  const checkHealth = useCallback(async () => {
    setChecking(true)
    const updates = {
      frontend: { status: 'UP', detail: 'Đang chạy' },
      gateway: { status: null, detail: null },
      backend: { status: null, detail: null },
      agent: { status: null, detail: null },
      mongodb: { status: null, detail: null },
      redis: { status: null, detail: null },
      rabbitmq: { status: null, detail: null },
      cloudinary: { status: null, detail: null },
    }

    const [systemResult, actuatorResult, agentResult] = await Promise.allSettled([
      getSystemHealth(),
      getActuatorHealth(),
      getAgentHealth(),
    ])

    if (systemResult.status === 'fulfilled') {
      const data = systemResult.value ?? {}
      const serviceMap = new Map((data?.services ?? []).map((svc) => [svc.name, svc]))
      const pickService = (name) => {
        const svc = serviceMap.get(name)
        if (!svc) {
          return { status: 'UNKNOWN', detail: 'Không xác định' }
        }
        return {
          status: svc.status ?? 'UNKNOWN',
          detail: svc.message ?? 'Không xác định',
        }
      }

      updates.mongodb = pickService('MongoDB')
      updates.redis = pickService('Redis')
      updates.rabbitmq = pickService('RabbitMQ')
      updates.cloudinary = pickService('Cloudinary')
    } else {
      const unknown = { status: 'UNKNOWN', detail: 'Không xác định' }
      updates.mongodb = { ...unknown }
      updates.redis = { ...unknown }
      updates.rabbitmq = { ...unknown }
      updates.cloudinary = { ...unknown }
    }

    if (actuatorResult.status === 'fulfilled') {
      const data = actuatorResult.value ?? {}
      const status = data?.status ?? 'UNKNOWN'
      const detail =
        status === 'UP'
          ? 'Backend is running'
          : data?.details?.error ?? data?.details?.message ?? 'Backend is not healthy'
      updates.backend = { status, detail }
    } else {
      updates.backend = { status: 'DOWN', detail: 'Không thể kết nối backend' }
    }

    if (agentResult.status === 'fulfilled') {
      const data = agentResult.value ?? {}
      const status = data?.status ?? 'UNKNOWN'
      const detail = data?.mode ?? data?.error ?? null
      updates.agent = { status, detail }
    } else {
      updates.agent = { status: 'DOWN', detail: 'Không thể kết nối AI Agent' }
    }

    const gatewayOk =
      systemResult.status === 'fulfilled' ||
      actuatorResult.status === 'fulfilled' ||
      agentResult.status === 'fulfilled'
    updates.gateway = gatewayOk
      ? { status: 'UP', detail: 'Phản hồi bình thường' }
      : { status: 'UNKNOWN', detail: 'Không xác định' }

    if (agentResult.status === 'fulfilled') {
      setAgentDetail(agentResult.value)
    } else {
      setAgentDetail(null)
    }

    const updatedAt =
      systemResult.status === 'fulfilled' && systemResult.value?.updatedAt
        ? new Date(systemResult.value.updatedAt)
        : new Date()
    setLastCheck(updatedAt)

    setServices(updates)
    setLoading(false)
    setChecking(false)
  }, [])

  useEffect(() => {
    checkHealth()
  }, [checkHealth])

  const serviceList = [
    { key: 'frontend', icon: Monitor, name: 'Frontend' },
    { key: 'gateway', icon: Globe, name: 'API Gateway' },
    { key: 'backend', icon: Server, name: 'Backend' },
    { key: 'agent', icon: Activity, name: 'AI Agent' },
    { key: 'mongodb', icon: Database, name: 'MongoDB' },
    { key: 'redis', icon: HardDrive, name: 'Redis' },
    { key: 'rabbitmq', icon: MessageSquare, name: 'RabbitMQ' },
    { key: 'cloudinary', icon: Cloud, name: 'Cloudinary' },
  ]

  return (
    <div className="admin-section" style={{ marginTop: '20px' }}>
      <div className="admin-section-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3>Tình trạng hệ thống</h3>
        <Button variant="secondary" size="sm" onClick={checkHealth} disabled={checking}>
          <RefreshCw size={14} className={checking ? 'spin-icon' : ''} />
          {checking ? 'Đang kiểm tra…' : 'Làm mới'}
        </Button>
      </div>

      {loading ? (
        <div className="admin-loading" style={{ padding: '20px' }}>
          <span className="admin-spinner" />
          Đang kiểm tra trạng thái hệ thống…
        </div>
      ) : (
        <>
          <div className="health-grid" style={{ marginBottom: '16px' }}>
            {serviceList.map((svc) => (
              <HealthCard
                key={svc.key}
                icon={svc.icon}
                name={svc.name}
                status={services[svc.key]?.status}
                detail={services[svc.key]?.detail}
              />
            ))}
          </div>

          <div className="admin-dashboard-grid">
            <div className="health-detail-section" style={{ flex: 1 }}>
              <h3>Chi tiết kết nối</h3>
              <div className="health-detail-row">
                <span style={{ fontWeight: 700 }}>
                  <Wifi size={14} style={{ marginRight: 6, verticalAlign: -2 }} />
                  Backend WebSocket (/ws)
                </span>
                <StatusText
                  status={services.backend.status}
                  label={
                    services.backend.status === 'UP'
                      ? 'Sẵn sàng'
                      : services.backend.status === 'DOWN'
                        ? 'Không khả dụng'
                        : 'Không xác định'
                  }
                />
              </div>
              <div className="health-detail-row">
                <span style={{ fontWeight: 700 }}>
                  <Activity size={14} style={{ marginRight: 6, verticalAlign: -2 }} />
                  Agent Health (/api/agent)
                </span>
                <StatusText
                  status={services.agent.status}
                  label={agentDetail?.status ?? 'Không xác định'}
                />
              </div>
              {agentDetail?.mode && (
                <div className="health-detail-row">
                  <span style={{ fontWeight: 700 }}>Agent Mode</span>
                  <span className="admin-role-badge">{agentDetail.mode}</span>
                </div>
              )}
            </div>

            <div className="health-error-log" style={{ flex: 1 }}>
              <h3>Lỗi gần đây</h3>
              <div className="health-error-placeholder">
                <AlertTriangle size={32} />
                <p>Chưa có endpoint log errors được cấu hình.</p>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
