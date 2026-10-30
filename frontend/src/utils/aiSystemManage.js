/**
 * AI 系统管理面板的纯逻辑工具函数。
 * 不依赖 Vue / Element Plus / axios，便于在 vitest 中直接单测。
 */

/**
 * 将字节数格式化为友好的大小字符串。
 * @param {number|string|null|undefined} bytes 字节数
 * @returns {string} 例如 "0 B" / "512 KB" / "1.23 GB"
 */
export function formatModelSize (bytes) {
  const n = Number(bytes)
  if (!Number.isFinite(n) || n <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let value = n
  while (value >= 1024 && i < units.length - 1) {
    value /= 1024
    i++
  }
  const rounded = Math.round(value * 100) / 100
  return `${rounded} ${units[i]}`
}

/**
 * 根据 Ollama available 标志返回用于 el-tag 的状态描述。
 * @param {boolean|string|number|undefined} available
 * @returns {{ type: 'success'|'info', text: string }}
 */
export function ollamaStatusTag (available) {
  const isOnline = available === true || available === 1 || available === 'true'
  return isOnline
    ? { type: 'success', text: '在线' }
    : { type: 'info', text: '离线' }
}

/**
 * 把后端返回的配置对象（数值/布尔可能是字符串）归一化为表单可直接绑定的类型。
 * 缺省值采用保守安全默认。
 * @param {object|null|undefined} raw
 * @returns {{defaultModel: string, temperature: number, maxTokens: number,
 *            requestTimeoutSec: number, aiChatEnabled: boolean, maintenanceNotice: string}}
 */
export function normalizeAiConfig (raw) {
  const src = raw || {}
  const toNumber = (v, fallback) => {
    const n = Number(v)
    return Number.isFinite(n) ? n : fallback
  }
  const toBool = (v) => v === true || v === 1 || v === 'true' || v === '1'
  return {
    defaultModel: typeof src.defaultModel === 'string' ? src.defaultModel : (src.defaultModel ? String(src.defaultModel) : ''),
    temperature: toNumber(src.temperature, 0.7),
    maxTokens: Math.round(toNumber(src.maxTokens, 2048)),
    requestTimeoutSec: Math.round(toNumber(src.requestTimeoutSec, 60)),
    aiChatEnabled: toBool(src.aiChatEnabled),
    maintenanceNotice: typeof src.maintenanceNotice === 'string' ? src.maintenanceNotice : (src.maintenanceNotice ? String(src.maintenanceNotice) : '')
  }
}

/**
 * API Key 脱敏：保留前 8 位与后 4 位，中间用 **** 替代。
 * 长度不足时退化为整体脱敏。
 * @param {string|null|undefined} key
 * @returns {string}
 */
export function maskApiKey (key) {
  if (!key) return '-'
  const s = String(key)
  if (s.length <= 12) return '****'
  return `${s.slice(0, 8)}****${s.slice(-4)}`
}

/**
 * 给数字加千分位分隔符，用于统计卡片展示。
 * @param {number|string|null|undefined} n
 * @returns {string}
 */
export function formatThousands (n) {
  const num = Number(n)
  if (!Number.isFinite(num)) return '0'
  return num.toLocaleString('en-US')
}

/**
 * 校验 Ollama 模型名称。
 * 规则：非空、不含空白字符、不含连续斜杠外的奇怪字符（保留 namespace/owner/repo:tag 形式）。
 * @param {string|null|undefined} name
 * @returns {string} 空串表示通过，否则为错误提示
 */
export function validateOllamaModelName (name) {
  if (name === null || name === undefined) return '模型名称不能为空'
  const s = String(name).trim()
  if (!s) return '模型名称不能为空'
  if (/\s/.test(s)) return '模型名称不能包含空格'
  if (s.length > 128) return '模型名称过长（≤128 字符）'
  // 允许字母数字 . _ - / :
  if (!/^[A-Za-z0-9._\-/:/]+$/.test(s)) return '模型名称包含非法字符'
  return ''
}

export default {
  formatModelSize,
  ollamaStatusTag,
  normalizeAiConfig,
  maskApiKey,
  formatThousands,
  validateOllamaModelName
}
