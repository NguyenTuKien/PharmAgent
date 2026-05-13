# PharmAgent Frontend

React + Vite frontend scaffold for PharmAgent.

## Stack

- Routing: `react-router-dom`
- Styling: Tailwind CSS v4, project font `Momo Trust Sans`
- Icons: Ionicons filled icons through the `ionicons` package
- API client: `axios`
- Auth store: `zustand`
- Form validation: `react-hook-form`, `zod`, `@hookform/resolvers`
- Toasts, search tabs, and modal: `goey-toast`, `gooey-search-tabs`, `framer-motion`, `@radix-ui/react-dialog`
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
VITE_FRONTEND_URL=http://localhost:5173
```

Vite dev server proxies `/api` and `/ws` to the gateway on port `9000`.

## Reusable UI

Use the installed gooey search tabs UI through:

```jsx
import { GooeySearchTabs } from './src/components/ui/GooeySearchTabs.jsx'
```

## Scripts

```bash
npm run dev
npm run lint
npm run build
npm test
```
