import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['tests/**/*.test.js', 'src/**/*.test.js'],
    setupFiles: ['./tests/setup.js'],
    restoreMocks: false,
    testTimeout: 10000,
    // jsdom 环境较重，限制并发 fork 数避免单 worker 堆内存溢出
    pool: 'forks',
    poolOptions: {
      forks: {
        maxForks: 2,
        minForks: 1
      }
    }
  }
})
