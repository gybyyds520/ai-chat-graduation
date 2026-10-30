/**
 * 深度研究纯逻辑工具（进度状态转换、报告格式化），便于单元测试。
 */

const STATUS_TEXT_MAP = {
  pending: '等待中',
  searching: '检索中',
  round2: '第二轮检索',
  integrating: '整合中',
  completed: '已完成',
  failed: '失败'
}

/**
 * 将研究状态码转换为可读文本。
 * @param {string} status 状态码
 * @returns {string} 中文描述
 */
export function researchStatusText(status) {
  return STATUS_TEXT_MAP[status] || (status || '未知')
}

/**
 * 判断状态是否为终态。
 * @param {string} status 状态码
 * @returns {boolean}
 */
export function isResearchFinished(status) {
  return status === 'completed' || status === 'failed'
}

/**
 * 由当前轮次与总轮次估算进度百分比（0-100）。
 * 终态直接返回 100。
 *
 * @param {string} status 状态码
 * @param {number} currentRound 当前轮次
 * @param {number} maxRounds 总轮次
 * @returns {number} 0-100
 */
export function estimateProgress(status, currentRound, maxRounds) {
  if (isResearchFinished(status)) {
    return 100
  }
  if (!maxRounds || maxRounds <= 0) {
    return 0
  }
  const rounds = Math.max(1, maxRounds)
  const roundRatio = (Math.max(1, currentRound || 1) - 1) / rounds
  // 轮次间占 0..80，最后整合阶段补到 90
  return Math.min(90, Math.round(roundRatio * 80 + 10))
}

/**
 * 格式化研究报告摘要：去掉多余空白并截取。
 * @param {string} summary 原始摘要
 * @param {number} maxLen 最大长度
 * @returns {string}
 */
export function formatSummary(summary, maxLen = 200) {
  if (!summary) return ''
  const s = String(summary).replace(/\s+/g, ' ').trim()
  return s.length > maxLen ? s.slice(0, maxLen) + '…' : s
}

export default {
  researchStatusText,
  isResearchFinished,
  estimateProgress,
  formatSummary
}
