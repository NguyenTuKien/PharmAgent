import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const gatewayTarget =
    process.env.VITE_GATEWAY_PROXY_TARGET ||
    env.VITE_GATEWAY_PROXY_TARGET ||
    'http://localhost:9000'
  const usePolling = (process.env.CHOKIDAR_USEPOLLING || env.CHOKIDAR_USEPOLLING) === 'true'
  const pollInterval = Number(process.env.CHOKIDAR_INTERVAL || env.CHOKIDAR_INTERVAL || 100)

  return {
    plugins: [react(), tailwindcss()],
    server: {
      host: '0.0.0.0',
      hmr: {
        host: 'localhost',
        clientPort: 5173,
      },
      watch: {
        usePolling,
        interval: pollInterval,
      },
      proxy: {
        '/actuator': {
          target: gatewayTarget,
          changeOrigin: true,
        },
        '/api': {
          target: gatewayTarget,
          changeOrigin: true,
        },
        '/ws': {
          target: gatewayTarget,
          changeOrigin: true,
          ws: true,
        },
      },
    },
  }
})
