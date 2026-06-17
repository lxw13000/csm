import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 用户端 H5（内嵌业务 App WebView）：开发期代理 /api、/ws 到后端（application.yml 中 server.port=8081）
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    host: '0.0.0.0',  // 允许所有 IP 访问
    port: 5175,
    proxy: {
      '/api': { target: 'http://localhost:8081', changeOrigin: true },
      '/files': { target: 'http://localhost:8081', changeOrigin: true },
      '/ws': { target: 'ws://localhost:8081', ws: true, changeOrigin: true }
    }
  }
})
