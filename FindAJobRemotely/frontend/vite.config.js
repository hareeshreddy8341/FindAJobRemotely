import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the API runs on :8080. Proxying keeps everything same-origin, which is what the
// SameSite=Strict refresh cookie needs. In production put nginx in front (see ../deploy/nginx.conf).
const target = process.env.VITE_API_TARGET || 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: { '/api': { target, changeOrigin: false } } },
  preview: { port: 4173, proxy: { '/api': { target, changeOrigin: false } } },
  build: { chunkSizeWarningLimit: 900 },
});
