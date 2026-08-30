import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'node:path'

export default defineConfig({
  base: '/dashboard/',
  plugins: [vue()],
  build: {
    outDir: resolve(__dirname, '../Application/src/main/resources/static/dashboard'),
    emptyOutDir: true,
    sourcemap: false,
    chunkSizeWarningLimit: 800,
    rollupOptions: { output: { manualChunks: { leaflet: ['leaflet'], charts: ['echarts'] } } }
  },
  server: { port: 5173, proxy: { '/api': 'http://localhost:8080' } }
})
