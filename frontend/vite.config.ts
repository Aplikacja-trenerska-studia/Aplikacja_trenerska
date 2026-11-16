import path from 'node:path'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, loadEnv } from 'vite'
import { normalizeApiOrigin } from './src/lib/api-url.ts'

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, import.meta.dirname, 'VITE_')
  const isVercelBuild = command === 'build' && process.env.VERCEL === '1'
  const apiOrigin = normalizeApiOrigin(env.VITE_API_URL, isVercelBuild)
  if (isVercelBuild && !apiOrigin) {
    throw new Error('Ustaw VITE_API_URL w projekcie frontendu Vercel na origin wdrożonego backendu HTTPS.')
  }

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': path.resolve(import.meta.dirname, './src'),
      },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    preview: {
      port: 3000,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
