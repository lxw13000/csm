import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 客服端 H5：开发期代理 /api、/ws 到后端（application.yml 中 server.port=8081）
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    port: 5174,
    proxy: {
      '/api': { target: 'http://localhost:8081', changeOrigin: true },
      '/ws': { target: 'ws://localhost:8081', ws: true, changeOrigin: true }
    }
  }
})
