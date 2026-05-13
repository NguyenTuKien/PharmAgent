# PharmAgent Frontend

React + Vite frontend scaffold for PharmAgent.

## Stack

- Routing: `react-router-dom`
- API client: `axios`
- Auth store: `zustand`
- Form validation: `react-hook-form`, `zod`, `@hookform/resolvers`
- Toasts and modal: `sonner`, `@radix-ui/react-dialog`
- Date/time picker: `react-day-picker`, `date-fns`
- Charts: `recharts`
- Realtime: `@stomp/stompjs`, `sockjs-client`, `reconnecting-websocket`

## Environment

Copy `.env.example` to `.env.local` when local overrides are needed.

```env
VITE_API_BASE_URL=/api
VITE_WS_BASE_URL=/ws
VITE_CAMERA_WS_PATH=/ws/agent
VITE_GATEWAY_PROXY_TARGET=http://localhost:9000
```

Vite dev server proxies `/api` and `/ws` to the gateway on port `9000`.

## Scripts

```bash
npm run dev
npm run lint
npm run build
npm test
```
