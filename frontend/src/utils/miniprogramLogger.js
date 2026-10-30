/**
 * 小程序日志工具
 * 用于记录小程序核心交互的详细日志
 */

class MiniProgramLogger {
  constructor() {
    this.moduleName = 'MiniProgram'
    this.enabled = true
    this.logLevel = 'debug' // debug, info, warn, error
    this.logs = []
    this.maxLogs = 1000
  }

  /**
   * 获取当前时间戳
   */
  getTimestamp() {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const hours = String(now.getHours()).padStart(2, '0')
    const minutes = String(now.getMinutes()).padStart(2, '0')
    const seconds = String(now.getSeconds()).padStart(2, '0')
    const ms = String(now.getMilliseconds()).padStart(3, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}.${ms}`
  }

  /**
   * 格式化日志消息
   */
  formatMessage(level, operation, description, data = null) {
    let msg = `[${this.getTimestamp()}] [${this.moduleName}] [${level.toUpperCase()}] [${operation}] ${description}`
    if (data !== null && data !== undefined) {
      msg += ` | Data: ${typeof data === 'string' ? data : JSON.stringify(data, null, 2)}`
    }
    return msg
  }

  /**
   * 保存日志到内存
   */
  saveLog(level, operation, description, data) {
    const entry = {
      timestamp: this.getTimestamp(),
      level,
      module: this.moduleName,
      operation,
      description,
      data
    }
    
    this.logs.push(entry)
    if (this.logs.length > this.maxLogs) {
      this.logs.shift()
    }
  }

  /**
   * Debug 级别日志
   */
  debug(operation, description, data = null) {
    if (!this.enabled || this.logLevel !== 'debug') return
    const msg = this.formatMessage('debug', operation, description, data)
    console.log('%c' + msg, 'color: #999')
    this.saveLog('debug', operation, description, data)
  }

  /**
   * Info 级别日志
   */
  info(operation, description, data = null) {
    if (!this.enabled) return
    const msg = this.formatMessage('info', operation, description, data)
    console.log('%c' + msg, 'color: #2196F3; font-weight: bold')
    this.saveLog('info', operation, description, data)
  }

  /**
   * Warn 级别日志
   */
  warn(operation, description, data = null) {
    if (!this.enabled) return
    const msg = this.formatMessage('warn', operation, description, data)
    console.warn('%c' + msg, 'color: #FF9800; font-weight: bold')
    this.saveLog('warn', operation, description, data)
  }

  /**
   * Error 级别日志
   */
  error(operation, description, data = null) {
    if (!this.enabled) return
    const msg = this.formatMessage('error', operation, description, data)
    console.error('%c' + msg, 'color: #F44336; font-weight: bold')
    this.saveLog('error', operation, description, data)
  }

  /**
   * 记录触摸事件
   */
  logTouchEvent(event, phase, data = {}) {
    const touchData = {
      phase,
      type: event.type,
      timestamp: this.getTimestamp(),
      ...data
    }
    this.info('Touch', `Touch ${phase}`, touchData)
  }

  /**
   * 记录下拉刷新事件
   */
  logRefreshEvent(action, data = {}) {
    this.info('Refresh', `Refresh ${action}`, {
      timestamp: this.getTimestamp(),
      ...data
    })
  }

  /**
   * 记录 Tab 切换事件
   */
  logTabChange(fromIndex, toIndex, tabName) {
    this.info('TabChange', `Tab switched from ${fromIndex} to ${toIndex}`, {
      fromIndex,
      toIndex,
      tabName,
      timestamp: this.getTimestamp()
    })
  }

  /**
   * 记录页面加载事件
   */
  logPageLoad(pageName, data = {}) {
    this.info('PageLoad', `Page ${pageName} loaded`, {
      pageName,
      timestamp: this.getTimestamp(),
      ...data
    })
  }

  /**
   * 记录错误事件
   */
  logError(operation, error) {
    this.error(operation, `Error in ${operation}`, {
      errorMessage: error.message || error,
      errorStack: error.stack || 'No stack trace',
      timestamp: this.getTimestamp()
    })
  }

  /**
   * 获取所有日志
   */
  getLogs() {
    return this.logs
  }

  /**
   * 清空日志
   */
  clearLogs() {
    this.logs = []
    this.info('Logger', 'Logs cleared')
  }

  /**
   * 导出日志为 JSON
   */
  exportLogs() {
    return JSON.stringify(this.logs, null, 2)
  }

  /**
   * 设置日志级别
   */
  setLogLevel(level) {
    this.logLevel = level
    this.info('Logger', `Log level set to ${level}`)
  }

  /**
   * 启用/禁用日志
   */
  setEnabled(enabled) {
    this.enabled = enabled
    console.log(`[${this.moduleName}] Logger ${enabled ? 'enabled' : 'disabled'}`)
  }
}

// 创建单例实例
const mpLogger = new MiniProgramLogger()

// 导出到全局 (方便在控制台调试)
if (typeof window !== 'undefined') {
  window.mpLogger = mpLogger
}

export default mpLogger