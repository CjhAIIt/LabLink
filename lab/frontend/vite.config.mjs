import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需引入：只打包实际用到的组件与样式。
    // include 需要覆盖 .js，否则 utils/request.js 中显式引入的 ElMessage / ElMessageBox 会丢失样式。
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false,
      include: [/\.vue$/, /\.vue\?vue/, /\.vue\?v=/, /\.[jt]sx?$/]
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        // 只固定 Vue 核心到 vendor 分块。Element Plus 交给按需引入自然分包，
        // 若在这里固定整包会把未使用的组件也打进首屏。
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia']
        }
      }
    }
  }
})
