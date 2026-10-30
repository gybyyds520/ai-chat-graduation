/**
 * 主题切换日志记录工具
 * 用于追踪和验证主题切换过程中 CSS 变量的变化
 */

// 日志级别
export const LogLevel = {
  DEBUG: 'DEBUG',
  INFO: 'INFO',
  WARN: 'WARN',
  ERROR: 'ERROR'
}

// 日志配置
const logConfig = {
  enabled: true,
  level: LogLevel.DEBUG,
  prefix: '[ThemeLogger]',
  maxHistory: 100
}

// 日志历史记录
const logHistory = []

/**
 * 获取当前时间戳
 */
function getTimestamp() {
  const now = new Date()
  return now.toISOString() + `.${String(now.getMilliseconds()).padStart(3, '0')}`
}

/**
 * 格式化日志消息
 */
function formatMessage(level, component, action, data = {}) {
  return {
    timestamp: getTimestamp(),
    level,
    component,
    action,
    ...data
  }
}

/**
 * 记录日志
 */
function log(level, component, action, data) {
  if (!logConfig.enabled) return

  const message = formatMessage(level, component, action, data)
  logHistory.push(message)

  // 限制历史记录数量
  if (logHistory.length > logConfig.maxHistory) {
    logHistory.shift()
  }

  // 控制台输出
  const consoleMethod = level === LogLevel.ERROR ? 'error' :
                       level === LogLevel.WARN ? 'warn' :
                       level === LogLevel.DEBUG ? 'debug' : 'log'

  console[consoleMethod](
    `${logConfig.prefix} [${level}] [${component}] ${action}`,
    data
  )
}

/**
 * 获取所有主题相关的 CSS 变量值
 */
export function getThemeVariables() {
  const computedStyle = getComputedStyle(document.documentElement)

  const variables = {
    // 背景色
    'bg-primary': computedStyle.getPropertyValue('--bg-primary').trim(),
    'bg-secondary': computedStyle.getPropertyValue('--bg-secondary').trim(),
    'bg-tertiary': computedStyle.getPropertyValue('--bg-tertiary').trim(),
    'bg-hover': computedStyle.getPropertyValue('--bg-hover').trim(),

    // 文字颜色
    'text-primary': computedStyle.getPropertyValue('--text-primary').trim(),
    'text-secondary': computedStyle.getPropertyValue('--text-secondary').trim(),
    'text-muted': computedStyle.getPropertyValue('--text-muted').trim(),
    'text-inverse': computedStyle.getPropertyValue('--text-inverse').trim(),

    // 边框颜色
    'border-primary': computedStyle.getPropertyValue('--border-primary').trim(),
    'border-secondary': computedStyle.getPropertyValue('--border-secondary').trim(),

    // 品牌色
    'color-primary': computedStyle.getPropertyValue('--color-primary').trim(),
    'color-primary-light': computedStyle.getPropertyValue('--color-primary-light').trim(),

    // 功能色
    'color-success': computedStyle.getPropertyValue('--color-success').trim(),
    'color-warning': computedStyle.getPropertyValue('--color-warning').trim(),
    'color-error': computedStyle.getPropertyValue('--color-error').trim(),

    // 卡片
    'card-bg': computedStyle.getPropertyValue('--card-bg').trim(),

    // 阴影
    'shadow-sm': computedStyle.getPropertyValue('--shadow-sm').trim(),
    'shadow-md': computedStyle.getPropertyValue('--shadow-md').trim(),
    'shadow-lg': computedStyle.getPropertyValue('--shadow-lg').trim(),

    // Feed 模块专属变量
    'feed-bg-gradient': computedStyle.getPropertyValue('--feed-bg-gradient').trim(),
    'feed-text-color': computedStyle.getPropertyValue('--feed-text-color').trim(),
    'feed-btn-bg': computedStyle.getPropertyValue('--feed-btn-bg').trim(),
  }

  return variables
}

/**
 * 比较两组 CSS 变量值的差异
 */
export function compareThemeVariables(before, after) {
  const changes = {}
  const unchanged = {}

  for (const key in after) {
    if (before[key] !== after[key]) {
      changes[key] = {
        before: before[key],
        after: after[key]
      }
    } else {
      unchanged[key] = after[key]
    }
  }

  return { changes, unchanged }
}

/**
 * 记录主题切换前的状态
 */
export function logThemeBefore(componentName) {
  const variables = getThemeVariables()
  const currentTheme = document.documentElement.getAttribute('data-theme') || 'light'

  log(LogLevel.DEBUG, componentName, 'THEME_SWITCH_BEFORE', {
    theme: currentTheme,
    variables,
    timestamp: Date.now()
  })

  return variables
}

/**
 * 记录主题切换后的状态
 */
export function logThemeAfter(componentName, beforeVariables) {
  const variables = getThemeVariables()
  const currentTheme = document.documentElement.getAttribute('data-theme') || 'light'
  const comparison = compareThemeVariables(beforeVariables, variables)

  log(LogLevel.INFO, componentName, 'THEME_SWITCH_AFTER', {
    theme: currentTheme,
    variables,
    changes: comparison.changes,
    unchangedCount: Object.keys(comparison.unchanged).length,
    changedCount: Object.keys(comparison.changes).length,
    timestamp: Date.now()
  })

  return { variables, comparison }
}

/**
 * 记录组件挂载时的主题状态
 */
export function logThemeOnMounted(componentName) {
  const variables = getThemeVariables()
  const currentTheme = document.documentElement.getAttribute('data-theme') || 'light'

  log(LogLevel.INFO, componentName, 'COMPONENT_MOUNTED', {
    theme: currentTheme,
    variables,
    timestamp: Date.now()
  })

  return variables
}

/**
 * 记录主题切换触发事件
 */
export function logThemeSwitchTrigger(componentName, triggerSource) {
  const currentTheme = document.documentElement.getAttribute('data-theme') || 'light'

  log(LogLevel.DEBUG, componentName, 'THEME_SWITCH_TRIGGER', {
    currentTheme,
    triggerSource,
    timestamp: Date.now()
  })
}

/**
 * 记录主题切换完成事件
 */
export function logThemeSwitchComplete(componentName, duration) {
  const variables = getThemeVariables()
  const currentTheme = document.documentElement.getAttribute('data-theme') || 'light'

  log(LogLevel.INFO, componentName, 'THEME_SWITCH_COMPLETE', {
    theme: currentTheme,
    variables,
    duration: `${duration}ms`,
    timestamp: Date.now()
  })
}

/**
 * 获取日志历史记录
 */
export function getLogHistory() {
  return [...logHistory]
}

/**
 * 清除日志历史记录
 */
export function clearLogHistory() {
  logHistory.length = 0
  log(LogLevel.INFO, 'ThemeLogger', 'LOG_HISTORY_CLEARED', {
    timestamp: Date.now()
  })
}

/**
 * 导出日志为 JSON
 */
export function exportLogsAsJSON() {
  const data = {
    exportTime: new Date().toISOString(),
    totalLogs: logHistory.length,
    logs: logHistory
  }

  const json = JSON.stringify(data, null, 2)
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)

  const a = document.createElement('a')
  a.href = url
  a.download = `theme-logs-${Date.now()}.json`
  a.click()

  URL.revokeObjectURL(url)

  log(LogLevel.INFO, 'ThemeLogger', 'LOGS_EXPORTED', {
    logCount: logHistory.length,
    filename: `theme-logs-${Date.now()}.json`
  })
}

/**
 * 启用或禁用日志记录
 */
export function setLoggingEnabled(enabled) {
  logConfig.enabled = enabled
  log(LogLevel.INFO, 'ThemeLogger', 'LOGGING_CONFIG_CHANGED', {
    enabled,
    timestamp: Date.now()
  })
}

/**
 * 设置日志级别
 */
export function setLogLevel(level) {
  logConfig.level = level
  log(LogLevel.INFO, 'ThemeLogger', 'LOG_LEVEL_CHANGED', {
    level,
    timestamp: Date.now()
  })
}

// 默认导出
export default {
  logThemeBefore,
  logThemeAfter,
  logThemeOnMounted,
  logThemeSwitchTrigger,
  logThemeSwitchComplete,
  getThemeVariables,
  compareThemeVariables,
  getLogHistory,
  clearLogHistory,
  exportLogsAsJSON,
  setLoggingEnabled,
  setLogLevel
}