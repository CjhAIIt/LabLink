import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

/**
 * 开发态抹掉转发请求的 Origin 头。
 *
 * 浏览器对同源 POST 也会带上 Origin 头（例如登录、发布公告），
 * 开发代理把该头原样转发到后端后，后端会按跨域来源白名单校验，
 * 开发服务器端口一旦不是 3000/4173/5173 就会被拦成
 * `403 Invalid CORS request`，写操作全部失败。
 *
 * 这里在开发代理之前把该头去掉，后端就按普通同源请求处理。
 * 插件中间件在 Vite 内部中间件（含代理）之前安装，因此顺序是确定的。
 */
const stripOriginHeaderPlugin = () => ({
  name: 'lab-strip-origin-header',
  configureServer(server) {
    server.middlewares.use((req, res, next) => {
      if (req.url && (req.url.startsWith('/api') || req.url.startsWith('/uploads'))) {
        delete req.headers.origin
      }
      next()
    })
  }
})

export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需引入：只打包实际用到的组件与样式。
    // include 需要覆盖 .js，否则 utils/request.js 中显式引入的 ElMessage / ElMessageBox 会丢失样式。
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false,
      include: [/\.vue$/, /\.vue\?vue/, /\.vue\?v=/, /\.[jt]sx?$/]
    }),
    stripOriginHeaderPlugin()
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
