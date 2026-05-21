# PharmAgent Frontend

React + Vite SPA cho hệ thống quản lý dùng thuốc thông minh PharmAgent.

---

## Tech Stack

| Concern | Library |
|---|---|
| Routing | `react-router-dom` |
| Styling | Tailwind CSS v4, font `Momo Trust Sans` |
| Icons | `ionicons` (filled) |
| HTTP Client | `axios` (wrapped in `apiClient`) |
| Auth State | `zustand` |
| Form | `react-hook-form` + `zod` + `@hookform/resolvers` |
| UI Components | `goey-toast`, `gooey-search-tabs`, `framer-motion`, `@radix-ui/react-dialog` |
| Date / Time | `react-day-picker`, `date-fns` |
| Charts | `recharts` |
| Realtime chat | `@stomp/stompjs`, `sockjs-client`, `reconnecting-websocket` |

---

## Cách chạy local (FE dev)

### Yêu cầu
- Node.js ≥ 20
- Toàn bộ backend đang chạy (xem phần Docker dưới)

### 1. Chạy backend bằng Docker Compose

```bash
# Từ thư mục gốc PharmAgent/
docker compose up -d
```

Các service sẽ khởi động:

| Service | Port | Mô tả |
|---|---|---|
| `gateway` | 9000 | API Gateway (FastAPI) — entry point cho mọi request |
| `backend` | 8080 | Spring Boot (auth, users, medications, chat) |
| `agent` | 8000 | AI Agent (FastAPI) — OCR nhận dạng thuốc, quản lý catalog |
| `database` | 27017 | MongoDB |
| `redis` | 6379 | Cache + rate limiting |
| `rabbitmq` | 5672 / 15672 | Message broker (email async) |
| `frontend` | 5173 | Nginx (chạy trong Docker) |

### 2. Chạy frontend ở chế độ dev (hot-reload)

```bash
cd frontend
npm install
npm run dev
```

Vite dev server chạy tại `http://localhost:5173` và **tự động proxy** mọi request:
- `/api/*` → `http://localhost:9000`  (REST)
- `/ws/*` → `http://localhost:9000`   (WebSocket / STOMP)

> Cấu hình proxy nằm trong `vite.config.js`, đọc từ biến `VITE_GATEWAY_PROXY_TARGET`.

### 3. Biến môi trường

Copy file mẫu nếu cần override:

```bash
cp .env.example .env.local
```

| Biến | Mặc định | Mô tả |
|---|---|---|
| `VITE_API_BASE_URL` | `/api` | Prefix cho mọi REST call |
| `VITE_WS_BASE_URL` | `/ws` | Prefix cho STOMP WebSocket |
| `VITE_GATEWAY_PROXY_TARGET` | `http://localhost:9000` | Gateway URL (chỉ dùng khi dev) |
| `VITE_FRONTEND_URL` | `http://localhost:5173` | URL của chính frontend |

> **Lưu ý**: Các biến `VITE_*` được **bake vào bundle** lúc build. Không có cách inject runtime — nếu cần thay đổi URL cho production, phải build lại image.

---

## Luồng Request

```
Browser
  │
  ├─ /api/*  ──► nginx (prod) / Vite proxy (dev)
  │               └─► Gateway :9000
  │                     ├─► Backend :8080  (auth, users, medications, chat)
  │                     └─► Agent   :8000  (OCR scan, pill catalog CRUD)
  │
  └─ /ws/*   ──► nginx / Vite proxy
                  └─► Gateway :9000
                        └─► Backend :8080  (STOMP chat over WebSocket)
```

---

## Cấu trúc thư mục

```
frontend/src/
├── api/              # authApi.js — raw axios calls cho /auth
├── components/
│   └── ui/           # Modal, GooeySearchTabs, Toast wrappers, v.v.
├── config/
│   └── env.js        # Tập trung đọc VITE_* env vars
├── layout/
│   ├── AppShell.jsx  # Layout chính (sidebar, topbar)
│   └── navigation.js # Định nghĩa menu items theo role
├── lib/
│   ├── apiClient.js  # axios instance (base URL, interceptors, JWT attach)
│   ├── uploadImage.js# Upload ảnh qua presign flow (avatar)
│   └── imageCompressor.js # Client-side image compression trước khi upload
├── modules/
│   ├── auth/         # authStore (zustand), authFacade, authApi
│   └── admin/        # adminApi.js (users + pills CRUD)
├── pages/
│   ├── auth/         # Login, Register, ForgotPassword, ResetPassword, VerifyEmail
│   ├── admin/        # AdminLayout, AdminUsersPage, AdminPillsPage
│   ├── DashboardPage.jsx
│   ├── ScanPage.jsx  # Chụp ảnh thuốc → OCR → kết quả nhận dạng
│   ├── ProfileSelectPage.jsx
│   └── WorkspacePage.jsx  # Placeholder cho các module chưa hoàn thiện
└── routes/
    └── guards.jsx    # GuestRoute, ProtectedRoute, RoleRoute, ProfileSelectionRoute
```

---

## Route Map

| Path | Guard | Component | Ghi chú |
|---|---|---|---|
| `/login` | Guest only | `LoginPage` | |
| `/register` | Guest only | `RegisterPage` | |
| `/register/elderly` | Guest only | `ElderlySetupPage` | |
| `/forgot-password` | Guest only | `ForgotPasswordPage` | |
| `/reset-password` | Guest only | `ResetPasswordPage` | |
| `/verify-email` | Guest only | `VerifyEmailPage` | |
| `/profiles` | Logged in, no profile | `ProfileSelectPage` | |
| `/dashboard` | Protected | `DashboardPage` | |
| `/scan` | Protected | `ScanPage` | OCR nhận dạng thuốc |
| `/medications` | Protected | `WorkspacePage` | Placeholder |
| `/relationships` | Protected | `WorkspacePage` | Placeholder |
| `/reports` | CAREGIVER / ADMIN | `WorkspacePage` | Placeholder |
| `/admin/users` | ADMIN only | `AdminUsersPage` | |
| `/admin/pills` | ADMIN only | `AdminPillsPage` | Quản lý danh mục thuốc |

---

## API Client Pattern

Mọi HTTP call đều đi qua `src/lib/apiClient.js`:

```js
import { apiClient } from '../lib/apiClient.js'

// GET với query params
const { data } = await apiClient.get('/pills', { params: { search: 'paracetamol', page: 0, size: 10 } })

// POST JSON
const { data } = await apiClient.post('/admin/pills', { name: 'Paracetamol', ... })

// Upload file (multipart)
const form = new FormData()
form.append('file', file)
await apiClient.post(`/admin/pills/${id}/images/upload`, form)
```

`apiClient` tự động:
- Đính kèm `Authorization: Bearer <token>` từ zustand store
- Thực hiện token refresh khi nhận 401
- Redirect về `/login` nếu refresh thất bại

---

## Auth Flow

```
1. POST /api/auth/login  → { accessToken, refreshToken }
2. Lưu vào zustand (authStore)  +  refresh token vào cookie/localStorage
3. Mọi request sau đó: Authorization header tự động được đính kèm
4. Khi access token hết hạn: interceptor tự gọi POST /api/auth/refresh
5. Sau khi login: chuyển hướng → /profiles (chọn profile ELDERLY / CAREGIVER)
6. Sau khi chọn profile: chuyển hướng → /dashboard
```

---

## Reusable UI Components

```jsx
// Search tabs với animation
import { GooeySearchTabs } from './src/components/ui/GooeySearchTabs.jsx'

// Modal wrapper (Radix Dialog)
import { Modal } from './src/components/ui/Modal.jsx'

// Toast notifications
import { toast } from 'goey-toast'
toast.success('Thành công!')
toast.error('Có lỗi xảy ra')
```

---

## Scripts

```bash
npm run dev       # Dev server với hot-reload tại :5173
npm run build     # Build production bundle vào dist/
npm run lint      # ESLint
npm run test      # Chạy unit tests (nếu đã cấu hình)
```

---

## Build & Deploy (Docker)

```bash
# Build image thủ công
docker build -t pharmagent-frontend ./frontend

# Chạy standalone (cần gateway đang chạy tại host:9000)
docker run -p 5173:5173 pharmagent-frontend
```

Production image dùng **multi-stage build**:
1. `node:20-alpine` → `npm run build` → tạo `dist/`
2. `nginx:alpine` → copy `dist/` vào `/usr/share/nginx/html`

Nginx phục vụ SPA tại port **5173** và proxy `/api/*`, `/ws/*` đến `gateway:9000`.

---

## Helm (Kubernetes)

Chart nằm tại `frontend/helm/`.

```bash
# Lint
helm lint ./frontend/helm

# Install vào cluster
helm install frontend ./frontend/helm \
  --set image.tag=latest \
  --namespace pharmagent --create-namespace

# Port-forward để test
kubectl port-forward svc/frontend 5173:5173 -n pharmagent
```

---

## Lưu ý cho FE dev

- **Không commit `.env.local`** — file này đã được `.gitignore`
- **Pill catalog** giờ được quản lý bởi **Agent** (không phải Backend). Các API `/api/pills` và `/api/admin/pills` được Gateway proxy sang Agent.
- **Upload ảnh avatar** vẫn đi qua Backend presign flow (`/api/upload/presign`).
- Trang `/medications`, `/relationships`, `/reports` hiện là `WorkspacePage` placeholder — sẵn sàng để tích hợp feature mới.
