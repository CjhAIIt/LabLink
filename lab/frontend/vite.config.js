import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

function resolveNodeModuleChunk(id) {
  if (!id.includes('node_modules')) {
    return undefined
  }

  if (id.includes('/vue-router/')) {
    return 'vendor-router'
  }

  if (id.includes('/pinia/')) {
    return 'vendor-store'
  }

  if (id.includes('/axios/')) {
    return 'vendor-axios'
  }

  if (id.includes('/dayjs/')) {
    return 'vendor-dayjs'
  }

  if (id.includes('/recorder-core/')) {
    return 'vendor-recorder'
  }

  return 'vendor-misc'
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  optimizeDeps: {
    include: ['element-plus/es/components/*/style/index'],
  },
  build: {
    chunkSizeWarningLimit: 700,
    rollupOptions: {
      output: {
        manualChunks: resolveNodeModuleChunk
      }
    }
  }
})
