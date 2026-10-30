import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/theme.css'
import logger from './utils/logger.js'

const app = createApp(App)

// ============ 全局错误捕获（前端日志收口） ============
// 1. Vue 组件渲染错误
app.config.errorHandler = (err, vm, info) => {
  const componentName = vm?.$options?.name || vm?.$options?.__name || 'AnonymousComponent'
  logger.error(`[VueError] ${componentName} @ ${info}:`, err)
}

// 2. 未捕获的同步异常
window.onerror = (message, source, lineno, colno, error) => {
  logger.error(`[WindowError] ${message} @ ${source}:${lineno}:${colno}`, error || message)
  return false // 让浏览器默认行为继续（控制台仍打印）
}

// 3. 未捕获的 Promise rejection
window.onunhandledrejection = (event) => {
  const reason = event?.reason
  logger.error('[UnhandledRejection]', reason)
}

app.use(router)
app.use(ElementPlus)
app.mount('#app')
