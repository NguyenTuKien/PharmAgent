import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const gatewayTarget = env.VITE_GATEWAY_PROXY_TARGET || 'http://localhost:9000'

  return {
    plugins: [react(), tailwindcss()],
    server: {
      host: '0.0.0.0',
      hmr: {
        host: 'localhost',
        clientPort: 5173,
      },
      watch: {
        usePolling: env.CHOKIDAR_USEPOLLING === 'true',
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
