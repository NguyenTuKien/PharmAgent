import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  ArrowLeft,
  Camera,
  CameraOff,
  CheckCheck,
  ImagePlus,
  LoaderCircle,
  MessageCircle,
  Mic,
  MicOff,
  Phone,
  PhoneCall,
  PhoneIncoming,
  PhoneOff,
  RefreshCw,
  Search,
  Send,
  UserRound,
  Video,
  Volume2,
  VolumeX,
  Wifi,
  WifiOff,
  X,
} from 'lucide-react'

import { createStompClient } from '../lib/stompClient.js'
import { notify } from '../lib/toast.js'
import { uploadImageToCloudinary } from '../lib/uploadImage.js'
import { useAuthStore } from '../modules/auth/authStore.js'
import {
  createDirectRoom,
  getChatRooms,
  getRoomMessages,
  markRoomRead,
} from '../modules/chat/chatApi.js'

const MESSAGE_PAGE_SIZE = 60

const connectionLabels = {
  connecting: 'Đang kết nối',
  connected: 'Realtime online',
  reconnecting: 'Đang nối lại',
  error: 'Mất kết nối',
  offline: 'Offline',
}

const callStatusLabels = {
  calling: 'Đang gọi',
  ringing: 'Đang đổ chuông',
  connected: 'Đang trong cuộc gọi',
  reconnecting: 'Đang nối lại cuộc gọi',
  ended: 'Cuộc gọi đã kết thúc',
}

function cn(...classes) {
  return classes.filter(Boolean).join(' ')
}

function createIdleCallState() {
  return {
    status: 'idle',
    direction: null,
    roomId: null,
    peerId: null,
    peerProfile: null,
    callId: null,
    startedAt: null,
    endedAt: null,
    muted: false,
    cameraOff: false,
    speakerOn: true,
    endReason: null,
  }
}

function profileName(profile, fallback = 'Người dùng') {
  const fullName = [profile?.firstName, profile?.lastName].filter(Boolean).join(' ').trim()
  return fullName || profile?.phone || fallback
}

function getInitials(profile) {
  const name = profileName(profile, 'U')
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((word) => word[0])
    .join('')
    .toUpperCase()
}

function profileRoleLabel(role) {
  if (role === 'ELDERLY') return 'Người cao tuổi'
  if (role === 'CAREGIVER') return 'Người chăm sóc'
  return 'Thành viên'
}

function getPeerProfile(room, activeProfileId) {
  if (!room) return null
  if (room.peerProfile) return room.peerProfile
  return room.participants?.find((participant) => participant.profileId !== activeProfileId) ?? null
}

function getParticipant(room, profileId) {
  return room?.participants?.find((participant) => participant.profileId === profileId) ?? null
}

function getRoomActivity(room) {
  const raw = room?.lastMessage?.sentAt ?? room?.updatedAt ?? room?.createdAt
  return raw ? new Date(raw).getTime() : 0
}

function sortRooms(rooms) {
  return [...rooms].sort((a, b) => getRoomActivity(b) - getRoomActivity(a))
}

function upsertRoom(rooms, nextRoom) {
  if (!nextRoom?.id) return rooms
  const exists = rooms.some((room) => room.id === nextRoom.id)
  const nextRooms = exists
    ? rooms.map((room) => (room.id === nextRoom.id ? { ...room, ...nextRoom } : room))
    : [nextRoom, ...rooms]
  return sortRooms(nextRooms)
}

function updateRoomWithMessage(rooms, message, activeRoomId, activeProfileId) {
  if (!message?.roomId) return rooms
  return sortRooms(
    rooms.map((room) => {
      if (room.id !== message.roomId) return room
      const shouldIncreaseUnread = message.senderId !== activeProfileId && message.roomId !== activeRoomId
      return {
        ...room,
        lastMessage: message,
        unreadCount:
          message.roomId === activeRoomId
            ? 0
            : Number(room.unreadCount ?? 0) + (shouldIncreaseUnread ? 1 : 0),
        updatedAt: message.sentAt ?? room.updatedAt,
      }
    }),
  )
}

function extractMessages(page) {
  const content = Array.isArray(page) ? page : page?.content
  if (!Array.isArray(content)) return []
  return [...content].sort((a, b) => new Date(a.sentAt ?? 0) - new Date(b.sentAt ?? 0))
}

function mergeMessages(messages, nextMessage) {
  if (!nextMessage) return messages
  if (nextMessage.id && messages.some((message) => message.id === nextMessage.id)) {
    return messages.map((message) => (message.id === nextMessage.id ? nextMessage : message))
  }
  return [...messages, nextMessage].sort((a, b) => new Date(a.sentAt ?? 0) - new Date(b.sentAt ?? 0))
}

function formatMessageTime(value) {
  if (!value) return ''
  const date = new Date(value)
  const now = new Date()
  const sameDay = date.toDateString() === now.toDateString()
  return new Intl.DateTimeFormat('vi-VN', {
    day: sameDay ? undefined : '2-digit',
    month: sameDay ? undefined : '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function formatDuration(seconds = 0) {
  const safeSeconds = Math.max(0, Number(seconds) || 0)
  const minutes = Math.floor(safeSeconds / 60)
  const remainingSeconds = safeSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(remainingSeconds).padStart(2, '0')}`
}

function messagePreview(message) {
  if (!message) return 'Chưa có tin nhắn'
  if (message.type === 'IMAGE') return 'Đã gửi một hình ảnh'
  if (message.type === 'CALL_LOG') return 'Nhật ký cuộc gọi'
  return message.content
}

function parseJson(value) {
  if (!value) return {}
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return {}
  }
}

function signalData(signal) {
  return parseJson(signal?.data)
}

function Avatar({ profile, size = 'md', className }) {
  const sizeClass = {
    sm: 'h-9 w-9 text-xs',
    md: 'h-11 w-11 text-sm',
    lg: 'h-16 w-16 text-lg',
    xl: 'h-24 w-24 text-3xl',
  }[size]

  if (profile?.avatarUrl) {
    return (
      <img
        alt={profileName(profile)}
        className={cn(sizeClass, 'shrink-0 rounded-full object-cover ring-2 ring-white', className)}
        src={profile.avatarUrl}
      />
    )
  }

  return (
    <div
      className={cn(
        sizeClass,
        'grid shrink-0 place-items-center rounded-full bg-emerald-100 font-semibold text-emerald-800 ring-2 ring-white',
        className,
      )}
    >
      {getInitials(profile)}
    </div>
  )
}

function ConnectionBadge({ status }) {
  const connected = status === 'connected'
  const Icon = connected ? Wifi : WifiOff
  return (
    <span
      className={cn(
        'inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs font-semibold',
        connected
          ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
          : 'border-amber-200 bg-amber-50 text-amber-700',
      )}
    >
      <Icon className="h-3.5 w-3.5" />
      {connectionLabels[status] ?? connectionLabels.offline}
    </span>
  )
}

function RoomList({
  rooms,
  activeProfileId,
  activeRoomId,
  loading,
  openingDirect,
  searchTerm,
  onSearch,
  onSelectRoom,
}) {
  const filteredRooms = useMemo(() => {
    const keyword = searchTerm.trim().toLowerCase()
    if (!keyword) return rooms
    return rooms.filter((room) => {
      const peer = getPeerProfile(room, activeProfileId)
      return [profileName(peer), peer?.phone, messagePreview(room.lastMessage)]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(keyword))
    })
  }, [activeProfileId, rooms, searchTerm])

  return (
    <aside
      className={cn(
        'flex min-h-[calc(100vh-13rem)] flex-col border-slate-200 bg-white lg:border-r',
        activeRoomId ? 'hidden lg:flex' : 'flex',
      )}
    >
      <div className="border-b border-slate-100 p-4">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-700">
              Phòng chat
            </p>
            <h2 className="text-xl font-semibold text-slate-950">Tin nhắn</h2>
          </div>
          {(loading || openingDirect) && (
            <LoaderCircle className="h-5 w-5 animate-spin text-emerald-600" />
          )}
        </div>

        <label className="mt-4 flex items-center gap-2 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-500">
          <Search className="h-4 w-4" />
          <input
            className="min-w-0 flex-1 bg-transparent text-slate-900 outline-none placeholder:text-slate-400"
            onChange={(event) => onSearch(event.target.value)}
            placeholder="Tìm người hoặc tin nhắn"
            value={searchTerm}
          />
        </label>
      </div>

      <div className="flex-1 overflow-y-auto p-2">
        {loading && rooms.length === 0 ? (
          <div className="grid h-48 place-items-center text-sm text-slate-500">
            <LoaderCircle className="mb-2 h-6 w-6 animate-spin text-emerald-600" />
            Đang tải phòng chat
          </div>
        ) : null}

        {!loading && filteredRooms.length === 0 ? (
          <div className="grid h-56 place-items-center px-6 text-center">
            <div>
              <div className="mx-auto grid h-14 w-14 place-items-center rounded-full bg-emerald-50 text-emerald-700">
                <MessageCircle className="h-7 w-7" />
              </div>
              <p className="mt-3 text-sm font-semibold text-slate-900">Chưa có cuộc trò chuyện</p>
              <p className="mt-1 text-xs leading-5 text-slate-500">
                Chọn Chat từ hồ sơ người thân để bắt đầu cuộc trò chuyện trực tiếp.
              </p>
            </div>
          </div>
        ) : null}

        <div className="space-y-1">
          {filteredRooms.map((room) => {
            const peer = getPeerProfile(room, activeProfileId)
            const active = activeRoomId === room.id
            return (
              <button
                className={cn(
                  'flex w-full items-center gap-3 rounded-2xl px-3 py-3 text-left transition',
                  active
                    ? 'bg-emerald-50 text-emerald-950 ring-1 ring-emerald-100'
                    : 'hover:bg-slate-50',
                )}
                key={room.id}
                onClick={() => onSelectRoom(room)}
                type="button"
              >
                <Avatar profile={peer} />
                <span className="min-w-0 flex-1">
                  <span className="flex items-center justify-between gap-3">
                    <span className="truncate text-sm font-semibold text-slate-950">
                      {profileName(peer, 'Cuộc trò chuyện')}
                    </span>
                    <span className="shrink-0 text-[11px] text-slate-400">
                      {formatMessageTime(room.lastMessage?.sentAt ?? room.updatedAt)}
                    </span>
                  </span>
                  <span className="mt-1 flex items-center justify-between gap-2">
                    <span className="truncate text-xs text-slate-500">
                      {messagePreview(room.lastMessage)}
                    </span>
                    {Number(room.unreadCount) > 0 ? (
                      <span className="grid h-5 min-w-5 place-items-center rounded-full bg-rose-500 px-1.5 text-[11px] font-bold text-white">
                        {room.unreadCount}
                      </span>
                    ) : null}
                  </span>
                </span>
              </button>
            )
          })}
        </div>
      </div>
    </aside>
  )
}

function EmptyChatState() {
  return (
    <div className="hidden min-h-[calc(100vh-13rem)] flex-1 items-center justify-center bg-[radial-gradient(circle_at_top_left,#ecfdf5,transparent_34%),linear-gradient(135deg,#ffffff,#f8fafc)] p-8 text-center lg:flex">
      <div className="max-w-sm">
        <div className="mx-auto grid h-20 w-20 place-items-center rounded-full bg-white text-emerald-700 shadow-sm ring-1 ring-emerald-100">
          <MessageCircle className="h-10 w-10" />
        </div>
        <h2 className="mt-5 text-2xl font-semibold text-slate-950">Chọn một phòng chat</h2>
        <p className="mt-2 text-sm leading-6 text-slate-500">
          Trao đổi nhanh với người thân về lịch uống thuốc, nhắc nhở và tình trạng hiện tại.
        </p>
      </div>
    </div>
  )
}

function CallLogBubble({ message }) {
  const log = parseJson(message.content)
  const label =
    {
      COMPLETED: 'Cuộc gọi đã kết thúc',
      REJECTED: 'Cuộc gọi bị từ chối',
      MISSED: 'Cuộc gọi nhỡ',
      CANCELLED: 'Cuộc gọi đã hủy',
      ENDED_BY_REMOTE: 'Cuộc gọi đã kết thúc',
    }[log.status] ?? 'Nhật ký cuộc gọi'

  return (
    <div className="my-3 flex justify-center">
      <div className="inline-flex items-center gap-2 rounded-full border border-emerald-100 bg-white px-4 py-2 text-xs font-medium text-slate-600 shadow-sm">
        <Phone className="h-4 w-4 text-emerald-700" />
        <span>{label}</span>
        {Number.isFinite(Number(log.durationSeconds)) ? (
          <span className="text-slate-400">{formatDuration(log.durationSeconds)}</span>
        ) : null}
      </div>
    </div>
  )
}

function MessageBubble({ message, activeProfileId }) {
  if (message.type === 'CALL_LOG') {
    return <CallLogBubble message={message} />
  }

  const mine = message.senderId === activeProfileId
  const readByCount = Array.isArray(message.readBy) ? message.readBy.length : 0

  return (
    <div className={cn('flex', mine ? 'justify-end' : 'justify-start')}>
      <div
        className={cn(
          'max-w-[78%] rounded-3xl px-4 py-3 shadow-sm md:max-w-[64%]',
          mine
            ? 'rounded-br-lg bg-emerald-600 text-white'
            : 'rounded-bl-lg border border-slate-100 bg-white text-slate-900',
        )}
      >
        {message.type === 'IMAGE' ? (
          <a href={message.content} rel="noreferrer" target="_blank">
            <img
              alt="Ảnh trong tin nhắn"
              className="max-h-72 w-full rounded-2xl object-cover"
              src={message.content}
            />
          </a>
        ) : (
          <p className="whitespace-pre-wrap break-words text-sm leading-6">{message.content}</p>
        )}
        <div
          className={cn(
            'mt-1 flex items-center justify-end gap-1 text-[11px]',
            mine ? 'text-emerald-50/80' : 'text-slate-400',
          )}
        >
          <span>{formatMessageTime(message.sentAt)}</span>
          {mine ? (
            <span className="inline-flex items-center gap-1">
              <CheckCheck className="h-3.5 w-3.5" />
              {readByCount > 1 ? 'Đã xem' : 'Đã gửi'}
            </span>
          ) : null}
        </div>
      </div>
    </div>
  )
}

function ChatPanel({
  activeProfileId,
  connectionStatus,
  draft,
  imageUploading,
  messages,
  messagesEndRef,
  messagesLoading,
  onAttach,
  onCall,
  onDraftChange,
  onRefresh,
  onSubmit,
  room,
}) {
  if (!room) {
    return <EmptyChatState />
  }

  const peer = getPeerProfile(room, activeProfileId)
  const connected = connectionStatus === 'connected'

  return (
    <main className="flex min-h-[calc(100vh-13rem)] flex-1 flex-col bg-slate-50">
      <header className="flex items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 py-3">
        <div className="flex min-w-0 items-center gap-3">
          <Link
            className="grid h-10 w-10 shrink-0 place-items-center rounded-full border border-slate-200 text-slate-600 transition hover:bg-slate-50 lg:hidden"
            to="/chat"
          >
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <Avatar profile={peer} />
          <div className="min-w-0">
            <h2 className="truncate text-base font-semibold text-slate-950">
              {profileName(peer, 'Cuộc trò chuyện')}
            </h2>
            <p className="truncate text-xs text-slate-500">
              {profileRoleLabel(peer?.role)} · {connected ? 'Có thể nhắn realtime' : 'Đang chờ kết nối'}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            className="grid h-10 w-10 place-items-center rounded-full border border-slate-200 text-slate-600 transition hover:bg-slate-50"
            onClick={onRefresh}
            title="Tải lại tin nhắn"
            type="button"
          >
            <RefreshCw className="h-4 w-4" />
          </button>
          <button
            className="grid h-10 w-10 place-items-center rounded-full bg-emerald-600 text-white shadow-sm transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-slate-300"
            disabled={!connected}
            onClick={onCall}
            title="Gọi video"
            type="button"
          >
            <PhoneCall className="h-4 w-4" />
          </button>
        </div>
      </header>

      <div className="flex-1 overflow-y-auto px-4 py-5">
        {messagesLoading ? (
          <div className="grid h-52 place-items-center text-sm text-slate-500">
            <LoaderCircle className="mb-2 h-6 w-6 animate-spin text-emerald-600" />
            Đang tải tin nhắn
          </div>
        ) : null}

        {!messagesLoading && messages.length === 0 ? (
          <div className="grid h-52 place-items-center text-center">
            <div>
              <div className="mx-auto grid h-14 w-14 place-items-center rounded-full bg-white text-emerald-700 shadow-sm">
                <MessageCircle className="h-7 w-7" />
              </div>
              <p className="mt-3 text-sm font-semibold text-slate-900">Bắt đầu cuộc trò chuyện</p>
              <p className="mt-1 text-xs text-slate-500">
                Gửi tin nhắn, hình ảnh hoặc bắt đầu cuộc gọi khi cần hỗ trợ nhanh.
              </p>
            </div>
          </div>
        ) : null}

        <div className="space-y-3">
          {messages.map((message) => (
            <MessageBubble activeProfileId={activeProfileId} key={message.id ?? message.sentAt} message={message} />
          ))}
          <div ref={messagesEndRef} />
        </div>
      </div>

      <form
        className="border-t border-slate-200 bg-white p-3"
        onSubmit={(event) => {
          event.preventDefault()
          onSubmit()
        }}
      >
        <div className="flex items-end gap-2 rounded-3xl border border-slate-200 bg-slate-50 p-2">
          <button
            className="grid h-11 w-11 shrink-0 place-items-center rounded-full text-slate-600 transition hover:bg-white disabled:cursor-not-allowed disabled:text-slate-300"
            disabled={!connected || imageUploading}
            onClick={onAttach}
            title="Đính kèm ảnh"
            type="button"
          >
            {imageUploading ? (
              <LoaderCircle className="h-5 w-5 animate-spin" />
            ) : (
              <ImagePlus className="h-5 w-5" />
            )}
          </button>
          <textarea
            className="max-h-32 min-h-11 flex-1 resize-none bg-transparent px-1 py-2.5 text-sm text-slate-900 outline-none placeholder:text-slate-400"
            onChange={(event) => onDraftChange(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault()
                onSubmit()
              }
            }}
            placeholder="Nhập tin nhắn..."
            rows={1}
            value={draft}
          />
          <button
            className="grid h-11 w-11 shrink-0 place-items-center rounded-full text-slate-600 transition hover:bg-white disabled:cursor-not-allowed disabled:text-slate-300"
            disabled={!connected}
            onClick={onCall}
            title="Gọi"
            type="button"
          >
            <Phone className="h-5 w-5" />
          </button>
          <button
            className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-emerald-600 text-white shadow-sm transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-slate-300"
            disabled={!connected || !draft.trim()}
            title="Gửi"
            type="submit"
          >
            <Send className="h-5 w-5" />
          </button>
        </div>
      </form>
    </main>
  )
}

function IncomingCallModal({ call, onAccept, onReject }) {
  if (!call) return null

  return (
    <div className="fixed inset-0 z-[70] grid place-items-center bg-slate-950/60 p-4 backdrop-blur-sm">
      <div className="w-full max-w-sm rounded-[2rem] border border-white/20 bg-white p-6 text-center shadow-2xl">
        <div className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-emerald-50 text-emerald-700">
          <PhoneIncoming className="h-6 w-6" />
        </div>
        <Avatar className="mx-auto mt-4" profile={call.caller} size="xl" />
        <h2 className="mt-4 text-xl font-semibold text-slate-950">
          {profileName(call.caller)} đang gọi
        </h2>
        <p className="mt-1 text-sm text-slate-500">Cuộc gọi video realtime</p>
        <div className="mt-6 flex items-center justify-center gap-4">
          <button
            className="grid h-14 w-14 place-items-center rounded-full bg-rose-500 text-white shadow-lg shadow-rose-500/25 transition hover:bg-rose-600"
            onClick={onReject}
            title="Từ chối"
            type="button"
          >
            <X className="h-6 w-6" />
          </button>
          <button
            className="grid h-14 w-14 place-items-center rounded-full bg-emerald-600 text-white shadow-lg shadow-emerald-600/25 transition hover:bg-emerald-700"
            onClick={onAccept}
            title="Nghe"
            type="button"
          >
            <Phone className="h-6 w-6" />
          </button>
        </div>
      </div>
    </div>
  )
}

function CallScreen({
  callState,
  localVideoRef,
  onHangUp,
  onToggleCamera,
  onToggleMute,
  onToggleSpeaker,
  remoteVideoRef,
  hasLocalStream,
  hasRemoteStream,
}) {
  const [duration, setDuration] = useState(() => {
    if (!callState.startedAt) return 0
    return Math.max(0, Math.floor((Date.now() - new Date(callState.startedAt).getTime()) / 1000))
  })

  useEffect(() => {
    if (callState.status !== 'connected' || !callState.startedAt) {
      return undefined
    }

    const interval = setInterval(() => {
      const elapsed = Math.floor((Date.now() - new Date(callState.startedAt).getTime()) / 1000)
      setDuration(Math.max(0, elapsed))
    }, 1000)

    return () => clearInterval(interval)
  }, [callState.status, callState.startedAt])

  if (callState.status === 'idle') return null

  const peer = callState.peerProfile

  return (
    <div className="fixed inset-0 z-[80] overflow-hidden bg-slate-950 text-white">
      <video
        autoPlay
        className={cn('absolute inset-0 h-full w-full object-cover', hasRemoteStream ? 'block' : 'hidden')}
        playsInline
        ref={remoteVideoRef}
      />

      {!hasRemoteStream ? (
        <div className="absolute inset-0 grid place-items-center bg-[radial-gradient(circle_at_top,#064e3b,transparent_36%),linear-gradient(135deg,#0f172a,#111827)] p-6 text-center">
          <div>
            <Avatar className="mx-auto ring-slate-800" profile={peer} size="xl" />
            <h2 className="mt-5 text-2xl font-semibold">{profileName(peer, 'Người nhận')}</h2>
            <p className="mt-2 text-sm text-slate-300">
              {callStatusLabels[callState.status] ?? 'Đang kết nối'}
            </p>
          </div>
        </div>
      ) : null}

      <div className="absolute left-4 right-4 top-4 flex items-center justify-between gap-3">
        <div className="rounded-full bg-slate-950/50 px-4 py-2 text-sm font-medium backdrop-blur">
          {callStatusLabels[callState.status] ?? 'Cuộc gọi'}
        </div>
        <div className="rounded-full bg-slate-950/50 px-4 py-2 text-sm font-medium backdrop-blur">
          {formatDuration(duration)}
        </div>
      </div>

      <div className="absolute right-4 top-20 h-36 w-24 overflow-hidden rounded-3xl border border-white/20 bg-slate-900 shadow-2xl sm:h-44 sm:w-32">
        <video
          autoPlay
          className={cn('h-full w-full object-cover', hasLocalStream ? 'block' : 'hidden')}
          muted
          playsInline
          ref={localVideoRef}
        />
        {!hasLocalStream || callState.cameraOff ? (
          <div className="grid h-full w-full place-items-center bg-slate-800 text-slate-300">
            <UserRound className="h-8 w-8" />
          </div>
        ) : null}
      </div>

      <div className="absolute inset-x-0 bottom-0 px-4 pb-6 pt-20">
        <div className="mx-auto flex max-w-md items-center justify-center gap-3 rounded-full bg-slate-950/70 p-3 backdrop-blur">
          <button
            className={cn(
              'grid h-12 w-12 place-items-center rounded-full transition',
              callState.muted ? 'bg-white text-slate-950' : 'bg-white/10 text-white hover:bg-white/20',
            )}
            onClick={onToggleMute}
            title={callState.muted ? 'Bật micro' : 'Tắt micro'}
            type="button"
          >
            {callState.muted ? <MicOff className="h-5 w-5" /> : <Mic className="h-5 w-5" />}
          </button>
          <button
            className={cn(
              'grid h-12 w-12 place-items-center rounded-full transition',
              callState.cameraOff ? 'bg-white text-slate-950' : 'bg-white/10 text-white hover:bg-white/20',
            )}
            onClick={onToggleCamera}
            title={callState.cameraOff ? 'Bật camera' : 'Tắt camera'}
            type="button"
          >
            {callState.cameraOff ? <CameraOff className="h-5 w-5" /> : <Camera className="h-5 w-5" />}
          </button>
          <button
            className="grid h-14 w-14 place-items-center rounded-full bg-rose-500 text-white shadow-lg shadow-rose-500/30 transition hover:bg-rose-600"
            onClick={onHangUp}
            title="Kết thúc"
            type="button"
          >
            <PhoneOff className="h-6 w-6" />
          </button>
          <button
            className={cn(
              'grid h-12 w-12 place-items-center rounded-full transition',
              callState.speakerOn ? 'bg-white/10 text-white hover:bg-white/20' : 'bg-white text-slate-950',
            )}
            onClick={onToggleSpeaker}
            title={callState.speakerOn ? 'Tắt loa' : 'Bật loa'}
            type="button"
          >
            {callState.speakerOn ? <Volume2 className="h-5 w-5" /> : <VolumeX className="h-5 w-5" />}
          </button>
          <div className="grid h-12 w-12 place-items-center rounded-full bg-white/10 text-white">
            <Video className="h-5 w-5" />
          </div>
        </div>
      </div>
    </div>
  )
}

export function ChatPage() {
  const navigate = useNavigate()
  const { roomId: activeRoomId } = useParams()
  const [searchParams] = useSearchParams()
  const activeProfile = useAuthStore((state) => state.activeProfile)
  const accessToken = useAuthStore((state) => state.accessToken)
  const activeProfileId = activeProfile?.id

  const [rooms, setRooms] = useState([])
  const [roomsLoading, setRoomsLoading] = useState(false)
  const [openingDirect, setOpeningDirect] = useState(false)
  const [searchTerm, setSearchTerm] = useState('')
  const [messages, setMessages] = useState([])
  const [messagesLoading, setMessagesLoading] = useState(false)
  const [draft, setDraft] = useState('')
  const [imageUploading, setImageUploading] = useState(false)
  const [connectionStatus, setConnectionStatus] = useState('offline')
  const [incomingCall, setIncomingCall] = useState(null)
  const [callState, setCallState] = useState(createIdleCallState)
  const [mediaVersion, setMediaVersion] = useState(0)

  const clientRef = useRef(null)
  const roomSubscriptionsRef = useRef([])
  const callSubscriptionRef = useRef(null)
  const openingTargetRef = useRef(null)
  const fileInputRef = useRef(null)
  const messagesEndRef = useRef(null)
  const peerConnectionRef = useRef(null)
  const localStreamRef = useRef(null)
  const remoteStreamRef = useRef(null)
  const localVideoRef = useRef(null)
  const remoteVideoRef = useRef(null)
  const callStateRef = useRef(callState)
  const callResetTimerRef = useRef(null)

  const activeRoom = useMemo(
    () => rooms.find((room) => room.id === activeRoomId) ?? null,
    [activeRoomId, rooms],
  )
  const roomSubscriptionKey = useMemo(
    () =>
      rooms
        .map((room) => room.id)
        .filter(Boolean)
        .sort()
        .join('|'),
    [rooms],
  )

  useEffect(() => {
    callStateRef.current = callState
  }, [callState])

  const loadRooms = useCallback(async () => {
    if (!activeProfileId) return
    setRoomsLoading(true)
    try {
      const data = await getChatRooms()
      setRooms(sortRooms(Array.isArray(data) ? data : []))
    } catch (error) {
      notify.apiError(error, 'Không thể tải danh sách phòng chat.')
    } finally {
      setRoomsLoading(false)
    }
  }, [activeProfileId])

  const loadMessages = useCallback(async (roomId) => {
    if (!roomId) return
    setMessagesLoading(true)
    try {
      const page = await getRoomMessages(roomId, { page: 0, size: MESSAGE_PAGE_SIZE })
      setMessages(extractMessages(page))
      await markRoomRead(roomId).catch(() => undefined)
      setRooms((current) =>
        current.map((room) => (room.id === roomId ? { ...room, unreadCount: 0 } : room)),
      )
    } catch (error) {
      notify.apiError(error, 'Không thể tải tin nhắn.')
      setMessages([])
    } finally {
      setMessagesLoading(false)
    }
  }, [])

  const sendChatPayload = useCallback(
    (payload) => {
      const client = clientRef.current
      if (!client?.connected) {
        notify.warning('Realtime chưa sẵn sàng, vui lòng thử lại sau.')
        return false
      }
      if (!activeProfileId || !payload?.roomId) {
        return false
      }

      client.publish({
        destination: '/app/chat.send',
        body: JSON.stringify({
          roomId: payload.roomId,
          senderId: activeProfileId,
          content: payload.content,
          type: payload.type ?? 'TEXT',
        }),
      })
      return true
    },
    [activeProfileId],
  )

  const publishCallLog = useCallback(
    ({ roomId, peerId, status, startedAt, endedAt, direction }) => {
      const started = startedAt ? new Date(startedAt) : new Date()
      const ended = endedAt ? new Date(endedAt) : new Date()
      const durationSeconds = Math.max(0, Math.round((ended.getTime() - started.getTime()) / 1000))
      sendChatPayload({
        roomId,
        type: 'CALL_LOG',
        content: JSON.stringify({
          peerId,
          status,
          direction,
          startedAt: started.toISOString(),
          endedAt: ended.toISOString(),
          durationSeconds,
        }),
      })
    },
    [sendChatPayload],
  )

  const sendCallSignal = useCallback(
    (receiverId, type, data = {}) => {
      const client = clientRef.current
      if (!client?.connected || !activeProfileId || !receiverId) return false

      client.publish({
        destination: '/app/call.signal',
        body: JSON.stringify({
          senderId: activeProfileId,
          receiverId,
          type,
          data,
        }),
      })
      return true
    },
    [activeProfileId],
  )

  const cleanupCallMedia = useCallback(() => {
    peerConnectionRef.current?.close()
    peerConnectionRef.current = null
    localStreamRef.current?.getTracks().forEach((track) => track.stop())
    remoteStreamRef.current?.getTracks().forEach((track) => track.stop())
    localStreamRef.current = null
    remoteStreamRef.current = null
    setMediaVersion((value) => value + 1)
  }, [])

  const finishCall = useCallback(
    ({ status = 'COMPLETED', notifyRemote = false, createLog = false } = {}) => {
      const current = callStateRef.current
      const endedAt = new Date().toISOString()

      if (notifyRemote && current.peerId) {
        sendCallSignal(current.peerId, 'HANG_UP', {
          roomId: current.roomId,
          callId: current.callId,
          status,
        })
      }

      if (createLog && current.roomId) {
        publishCallLog({
          roomId: current.roomId,
          peerId: current.peerId,
          status,
          startedAt: current.startedAt ?? endedAt,
          endedAt,
          direction: current.direction,
        })
      }

      cleanupCallMedia()
      setIncomingCall(null)
      setCallState((previous) => ({
        ...previous,
        status: 'ended',
        endedAt,
        endReason: status,
      }))

      if (callResetTimerRef.current) {
        window.clearTimeout(callResetTimerRef.current)
      }
      callResetTimerRef.current = window.setTimeout(() => {
        setCallState(createIdleCallState())
      }, 1200)
    },
    [cleanupCallMedia, publishCallLog, sendCallSignal],
  )

  const createPeerConnection = useCallback(
    ({ peerId, roomId, callId }) => {
      if (!window.RTCPeerConnection) {
        throw new Error('Trình duyệt không hỗ trợ WebRTC.')
      }

      const connection = new RTCPeerConnection({
        iceServers: [{ urls: 'stun:stun.l.google.com:19302' }],
      })

      connection.onicecandidate = (event) => {
        if (event.candidate) {
          sendCallSignal(peerId, 'ICE_CANDIDATE', {
            roomId,
            callId,
            candidate: event.candidate,
          })
        }
      }

      connection.ontrack = (event) => {
        if (event.streams?.[0]) {
          remoteStreamRef.current = event.streams[0]
        } else {
          remoteStreamRef.current = remoteStreamRef.current ?? new MediaStream()
          remoteStreamRef.current.addTrack(event.track)
        }
        setMediaVersion((value) => value + 1)
        setCallState((previous) =>
          previous.status === 'ended' ? previous : { ...previous, status: 'connected' },
        )
      }

      connection.onconnectionstatechange = () => {
        const state = connection.connectionState
        if (state === 'connected') {
          setCallState((previous) =>
            previous.status === 'ended' ? previous : { ...previous, status: 'connected' },
          )
        }
        if (state === 'disconnected' || state === 'failed') {
          setCallState((previous) =>
            previous.status === 'ended' ? previous : { ...previous, status: 'reconnecting' },
          )
        }
      }

      peerConnectionRef.current = connection
      return connection
    },
    [sendCallSignal],
  )

  const prepareLocalMedia = useCallback(async () => {
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new Error('Trình duyệt không hỗ trợ camera hoặc micro.')
    }

    const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    localStreamRef.current = stream
    setMediaVersion((value) => value + 1)
    return stream
  }, [])

  const startCall = useCallback(async () => {
    if (!activeRoom) return
    const peer = getPeerProfile(activeRoom, activeProfileId)
    if (!peer?.profileId) {
      notify.warning('Không xác định được người nhận cuộc gọi.')
      return
    }
    if (!clientRef.current?.connected) {
      notify.warning('Realtime chưa sẵn sàng cho cuộc gọi.')
      return
    }

    const callId = `${activeRoom.id}-${Date.now()}`
    const startedAt = new Date().toISOString()
    setCallState({
      ...createIdleCallState(),
      status: 'calling',
      direction: 'outgoing',
      roomId: activeRoom.id,
      peerId: peer.profileId,
      peerProfile: peer,
      callId,
      startedAt,
    })

    try {
      const stream = await prepareLocalMedia()
      const connection = createPeerConnection({ peerId: peer.profileId, roomId: activeRoom.id, callId })
      stream.getTracks().forEach((track) => connection.addTrack(track, stream))
      const offer = await connection.createOffer()
      await connection.setLocalDescription(offer)
      sendCallSignal(peer.profileId, 'OFFER', {
        roomId: activeRoom.id,
        callId,
        startedAt,
        sdp: offer,
      })
    } catch (error) {
      cleanupCallMedia()
      setCallState(createIdleCallState())
      notify.error(error?.message ?? 'Không thể bắt đầu cuộc gọi.')
    }
  }, [
    activeProfileId,
    activeRoom,
    cleanupCallMedia,
    createPeerConnection,
    prepareLocalMedia,
    sendCallSignal,
  ])

  const acceptIncomingCall = useCallback(async () => {
    if (!incomingCall) return

    const data = signalData(incomingCall.signal)
    const peerId = incomingCall.signal.senderId
    const roomId = data.roomId ?? incomingCall.roomId
    const callId = data.callId ?? `${roomId}-${Date.now()}`
    const startedAt = data.startedAt ?? new Date().toISOString()

    setIncomingCall(null)
    setCallState({
      ...createIdleCallState(),
      status: 'ringing',
      direction: 'incoming',
      roomId,
      peerId,
      peerProfile: incomingCall.caller,
      callId,
      startedAt,
    })

    try {
      const stream = await prepareLocalMedia()
      const connection = createPeerConnection({ peerId, roomId, callId })
      stream.getTracks().forEach((track) => connection.addTrack(track, stream))
      await connection.setRemoteDescription(new RTCSessionDescription(data.sdp))
      const answer = await connection.createAnswer()
      await connection.setLocalDescription(answer)
      sendCallSignal(peerId, 'ANSWER', {
        roomId,
        callId,
        sdp: answer,
      })
    } catch (error) {
      sendCallSignal(peerId, 'REJECT', { roomId, callId, reason: 'media_failed' })
      cleanupCallMedia()
      setCallState(createIdleCallState())
      notify.error(error?.message ?? 'Không thể nhận cuộc gọi.')
    }
  }, [cleanupCallMedia, createPeerConnection, incomingCall, prepareLocalMedia, sendCallSignal])

  const rejectIncomingCall = useCallback(() => {
    if (!incomingCall) return
    const data = signalData(incomingCall.signal)
    const roomId = data.roomId ?? incomingCall.roomId
    sendCallSignal(incomingCall.signal.senderId, 'REJECT', {
      roomId,
      callId: data.callId,
      reason: 'rejected',
    })
    publishCallLog({
      roomId,
      peerId: incomingCall.signal.senderId,
      status: 'REJECTED',
      startedAt: data.startedAt ?? new Date().toISOString(),
      endedAt: new Date().toISOString(),
      direction: 'incoming',
    })
    setIncomingCall(null)
  }, [incomingCall, publishCallLog, sendCallSignal])

  const handleCallSignal = useCallback(
    async (signal) => {
      if (!signal?.type || signal.senderId === activeProfileId) return
      const data = signalData(signal)

      if (signal.type === 'OFFER') {
        const room = rooms.find((item) => item.id === data.roomId)
        const caller = getParticipant(room, signal.senderId) ?? {
          profileId: signal.senderId,
          firstName: 'Người gọi',
          lastName: '',
        }
        if (callStateRef.current.status !== 'idle') {
          sendCallSignal(signal.senderId, 'REJECT', {
            roomId: data.roomId,
            callId: data.callId,
            reason: 'busy',
          })
          return
        }
        setIncomingCall({
          signal,
          caller,
          room,
          roomId: data.roomId,
        })
        return
      }

      if (signal.type === 'ANSWER') {
        if (peerConnectionRef.current && data.sdp) {
          await peerConnectionRef.current.setRemoteDescription(new RTCSessionDescription(data.sdp))
        }
        setCallState((previous) => ({ ...previous, status: 'connected' }))
        return
      }

      if (signal.type === 'ICE_CANDIDATE') {
        if (peerConnectionRef.current && data.candidate) {
          await peerConnectionRef.current.addIceCandidate(new RTCIceCandidate(data.candidate))
        }
        return
      }

      if (signal.type === 'HANG_UP') {
        finishCall({ status: data.status ?? 'ENDED_BY_REMOTE' })
        return
      }

      if (signal.type === 'REJECT') {
        notify.info('Cuộc gọi đã bị từ chối.')
        finishCall({ status: 'REJECTED' })
      }
    },
    [activeProfileId, finishCall, rooms, sendCallSignal],
  )

  const handleRealtimeMessage = useCallback(
    (message) => {
      if (!message?.roomId) return
      setRooms((current) => updateRoomWithMessage(current, message, activeRoomId, activeProfileId))

      if (message.roomId === activeRoomId) {
        setMessages((current) => mergeMessages(current, message))
        if (message.senderId !== activeProfileId) {
          markRoomRead(message.roomId).catch(() => undefined)
          setRooms((current) =>
            current.map((room) => (room.id === message.roomId ? { ...room, unreadCount: 0 } : room)),
          )
        }
      }
    },
    [activeProfileId, activeRoomId],
  )

  const submitMessage = useCallback(() => {
    const content = draft.trim()
    if (!content || !activeRoomId) return
    const sent = sendChatPayload({
      roomId: activeRoomId,
      content,
      type: 'TEXT',
    })
    if (sent) {
      setDraft('')
    }
  }, [activeRoomId, draft, sendChatPayload])

  const handleImageSelected = useCallback(
    async (event) => {
      const file = event.target.files?.[0]
      event.target.value = ''
      if (!file || !activeRoomId) return

      setImageUploading(true)
      try {
        const imageUrl = await uploadImageToCloudinary(file, 'chat')
        sendChatPayload({
          roomId: activeRoomId,
          content: imageUrl,
          type: 'IMAGE',
        })
      } catch (error) {
        notify.apiError(error, 'Không thể tải ảnh lên.')
      } finally {
        setImageUploading(false)
      }
    },
    [activeRoomId, sendChatPayload],
  )

  const toggleMute = useCallback(() => {
    const nextMuted = !callStateRef.current.muted
    localStreamRef.current?.getAudioTracks().forEach((track) => {
      track.enabled = !nextMuted
    })
    setCallState((previous) => ({ ...previous, muted: nextMuted }))
  }, [])

  const toggleCamera = useCallback(() => {
    const nextCameraOff = !callStateRef.current.cameraOff
    localStreamRef.current?.getVideoTracks().forEach((track) => {
      track.enabled = !nextCameraOff
    })
    setCallState((previous) => ({ ...previous, cameraOff: nextCameraOff }))
  }, [])

  const toggleSpeaker = useCallback(() => {
    const nextSpeakerOn = !callStateRef.current.speakerOn
    if (remoteVideoRef.current) {
      remoteVideoRef.current.muted = !nextSpeakerOn
    }
    setCallState((previous) => ({ ...previous, speakerOn: nextSpeakerOn }))
  }, [])

  const hangUpCall = useCallback(() => {
    const current = callStateRef.current
    finishCall({
      status: current.status === 'calling' ? 'CANCELLED' : 'COMPLETED',
      notifyRemote: true,
      createLog: true,
    })
  }, [finishCall])

  useEffect(() => {
    loadRooms()
  }, [loadRooms])

  useEffect(() => {
    const targetProfileId = searchParams.get('profileId')
    if (!targetProfileId || !activeProfileId || openingTargetRef.current === targetProfileId) return

    openingTargetRef.current = targetProfileId
    setOpeningDirect(true)
    createDirectRoom(targetProfileId)
      .then((room) => {
        setRooms((current) => upsertRoom(current, room))
        navigate(`/chat/${room.id}`, { replace: true })
      })
      .catch((error) => {
        notify.apiError(error, 'Không thể mở phòng chat trực tiếp.')
      })
      .finally(() => {
        openingTargetRef.current = null
        setOpeningDirect(false)
      })
  }, [activeProfileId, navigate, searchParams])

  useEffect(() => {
    if (activeRoomId) {
      loadMessages(activeRoomId)
    } else {
      setMessages([])
    }
  }, [activeRoomId, loadMessages])

  useEffect(() => {
    if (!accessToken || !activeProfileId) {
      setConnectionStatus('offline')
      return undefined
    }

    setConnectionStatus('connecting')
    const client = createStompClient({
      accessToken,
      onConnect: () => setConnectionStatus('connected'),
      onDisconnect: () => setConnectionStatus('offline'),
      onError: () => setConnectionStatus((current) => (current === 'connected' ? 'reconnecting' : 'error')),
      onReady: (readyClient) => {
        clientRef.current = readyClient
      },
    })

    clientRef.current = client
    client.activate()

    return () => {
      roomSubscriptionsRef.current.forEach((subscription) => subscription?.unsubscribe?.())
      callSubscriptionRef.current?.unsubscribe?.()
      roomSubscriptionsRef.current = []
      callSubscriptionRef.current = null
      client.deactivate()
      clientRef.current = null
      setConnectionStatus('offline')
    }
  }, [accessToken, activeProfileId])

  useEffect(() => {
    const client = clientRef.current
    if (connectionStatus !== 'connected' || !client?.connected || !activeProfileId) return undefined

    callSubscriptionRef.current?.unsubscribe?.()
    callSubscriptionRef.current = client.subscribe(`/queue/user.${activeProfileId}.call`, (message) => {
      const body = message.body ? JSON.parse(message.body) : null
      handleCallSignal(body).catch(() => undefined)
    })

    return () => {
      callSubscriptionRef.current?.unsubscribe?.()
      callSubscriptionRef.current = null
    }
  }, [activeProfileId, connectionStatus, handleCallSignal])

  useEffect(() => {
    const client = clientRef.current
    const roomIds = roomSubscriptionKey ? roomSubscriptionKey.split('|') : []
    if (connectionStatus !== 'connected' || !client?.connected || roomIds.length === 0) return undefined

    const subscriptions = roomIds.map((roomId) =>
      client.subscribe(`/topic/room.${roomId}`, (message) => {
        const body = message.body ? JSON.parse(message.body) : null
        handleRealtimeMessage(body)
      }),
    )
    roomSubscriptionsRef.current = subscriptions

    return () => {
      subscriptions.forEach((subscription) => subscription?.unsubscribe?.())
      roomSubscriptionsRef.current = []
    }
  }, [connectionStatus, handleRealtimeMessage, roomSubscriptionKey])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ block: 'end' })
  }, [messages])

  useEffect(() => {
    if (localVideoRef.current) {
      localVideoRef.current.srcObject = localStreamRef.current
    }
    if (remoteVideoRef.current) {
      remoteVideoRef.current.srcObject = remoteStreamRef.current
      remoteVideoRef.current.muted = !callState.speakerOn
    }
  }, [callState.speakerOn, mediaVersion])

  useEffect(
    () => () => {
      if (callResetTimerRef.current) {
        window.clearTimeout(callResetTimerRef.current)
      }
      cleanupCallMedia()
    },
    [cleanupCallMedia],
  )

  return (
    <section className="space-y-4">
      <input
        accept="image/*"
        className="hidden"
        onChange={handleImageSelected}
        ref={fileInputRef}
        type="file"
      />

      <div className="rounded-[2rem] border border-emerald-100 bg-white p-4 shadow-sm sm:p-5">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-emerald-700">
              Trò chuyện
            </p>
            <h1 className="mt-1 text-2xl font-semibold text-slate-950 sm:text-3xl">
              Trò chuyện chăm sóc
            </h1>
            <p className="mt-1 max-w-2xl text-sm leading-6 text-slate-500">
              Nhắn tin, gửi ảnh và gọi video realtime giữa người chăm sóc và người cao tuổi.
            </p>
          </div>
          <ConnectionBadge status={connectionStatus} />
        </div>
      </div>

      <div className="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-sm">
        <div className="grid grid-cols-1 lg:grid-cols-[360px_minmax(0,1fr)]">
          <RoomList
            activeProfileId={activeProfileId}
            activeRoomId={activeRoomId}
            loading={roomsLoading}
            onSearch={setSearchTerm}
            onSelectRoom={(room) => navigate(`/chat/${room.id}`)}
            openingDirect={openingDirect}
            rooms={rooms}
            searchTerm={searchTerm}
          />

          <div className={cn(activeRoomId ? 'flex' : 'hidden lg:flex')}>
            <ChatPanel
              activeProfileId={activeProfileId}
              connectionStatus={connectionStatus}
              draft={draft}
              imageUploading={imageUploading}
              messages={messages}
              messagesEndRef={messagesEndRef}
              messagesLoading={messagesLoading}
              onAttach={() => fileInputRef.current?.click()}
              onCall={startCall}
              onDraftChange={setDraft}
              onRefresh={() => activeRoomId && loadMessages(activeRoomId)}
              onSubmit={submitMessage}
              room={activeRoom}
            />
          </div>
        </div>
      </div>

      <IncomingCallModal call={incomingCall} onAccept={acceptIncomingCall} onReject={rejectIncomingCall} />
      <CallScreen
        callState={callState}
        hasLocalStream={Boolean(localStreamRef.current)}
        hasRemoteStream={Boolean(remoteStreamRef.current)}
        localVideoRef={localVideoRef}
        onHangUp={hangUpCall}
        onToggleCamera={toggleCamera}
        onToggleMute={toggleMute}
        onToggleSpeaker={toggleSpeaker}
        remoteVideoRef={remoteVideoRef}
      />
    </section>
  )
}
