import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import legacy from '@vitejs/plugin-legacy'
import { resolve } from 'path'
import terser from '@rollup/plugin-terser'

export default defineConfig({
  plugins: [
    vue(),
    legacy({
      targets: [
        'last 2 versions',
        '> 1%',
        'iOS >= 10',
        'Android >= 5',
        'Chrome >= 49',
        'Firefox >= 45',
        'Safari >= 10',
        'Edge >= 12',
        'IE >= 11'
      ],
      additionalLegacyPolyfills: ['regenerator-runtime/runtime'],
      modernPolyfills: true
    })
  ],
  base: '/',
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5187,
    host: '0.0.0.0',
    // ===== HTTPS 开发服务器（可选开启，默认关闭）=====
    // 默认仍走 HTTP(5187)。如需在本地用 https://localhost:5187 调试，取消下面注释并确保
    // key/cert 两个文件已生成（见 docs/HTTPS部署指南.md「前端 HTTPS 开发配置」一节，
    // 可用 mkcert 生成本地受信任证书，或 node 自带自签证书脚本）。
    // 注意：取消注释前需在文件顶部加 `import fs from 'fs'`。
    // https: {
    //   key: fs.readFileSync(resolve(__dirname, 'cert/server.key')),
    //   cert: fs.readFileSync(resolve(__dirname, 'cert/server.crt'))
    // },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            proxyReq.setHeader('Connection', 'keep-alive')
          })
        }
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8080',
        ws: true
      }
    }
  },
  build: {
    target: 'es2015',
    modulePreload: {
      polyfill: true
    },
    cssTarget: 'chrome61',
    chunkSizeWarningLimit: 2000,
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
        pure_funcs: ['console.log', 'console.info', 'console.warn', 'console.error', 'debugger'],
        dead_code: true,
        unused: true,
        side_effects: true
      },
      mangle: {
        toplevel: true,
        properties: {
          regex: /^_/,
          keep_quoted: true
        }
      },
      output: {
        comments: false,
        beautify: false
      }
    },
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('echarts')) {
              return 'vendor-echarts'
            }
            if (id.includes('three')) {
              return 'vendor-three'
            }
            if (id.includes('@mediapipe')) {
              return 'vendor-mediapipe'
            }
            if (id.includes('vue') || id.includes('element-plus')) {
              return 'vendor-vue'
            }
            if (id.includes('axios')) {
              return 'vendor-axios'
            }
            if (id.includes('@vueuse')) {
              return 'vendor-vueuse'
            }
            return 'vendor-common'
          }
        },
        compact: true,
        indent: false
      },
      plugins: [
        terser({
          compress: {
            passes: 2,
            toplevel: true
          },
          mangle: {
            toplevel: true
          },
          output: {
            comments: /^\**!/
          }
        })
      ]
    }
  }
})