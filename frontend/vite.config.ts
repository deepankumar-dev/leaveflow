import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// The API runs on :8080; proxying keeps the browser on one origin in dev. changeOrigin must stay false: the backend
// rejects cross-origin requests (no CORS_ORIGINS by default), so Host has to keep matching the browser's Origin.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 5173, proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: false } } },
})
