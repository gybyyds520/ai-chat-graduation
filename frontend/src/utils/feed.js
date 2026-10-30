/**
 * AI 投喂（Feed）纯逻辑工具函数。
 *
 * 这些函数不依赖 Vue / DOM / axios，便于在 vitest 中直接单测。
 * 与 FeedView.vue 共用：路径解析、文件大小格式化、学习效果分级、知识库关联判断等。
 */

/**
 * 把后端存储的逗号分隔字符串解析为数组。
 * 后端 filePaths / fileNames / imagePaths / imageNames 以逗号拼接存储，
 * 前端展示时需要 split(',') 还原为数组。
 *
 * 规则：
 *  - null / undefined / 空字符串 -> []
 *  - 单元素字符串（无逗号）-> [该元素]
 *  - 多个元素 -> 去除每个元素首尾空白后过滤空串
 *
 * @param {string} pathsStr 逗号分隔字符串
 * @returns {string[]}
 */
export function parseMultiPaths(pathsStr) {
  if (pathsStr === null || pathsStr === undefined || pathsStr === '') {
    return []
  }
  if (Array.isArray(pathsStr)) {
    return pathsStr.map((s) => String(s).trim()).filter((s) => s.length > 0)
  }
  return String(pathsStr)
    .split(',')
    .map((s) => s.trim())
    .filter((s) => s.length > 0)
}

/**
 * 字节数人类可读格式化：B / KB / MB / GB。
 * 与 FeedView.vue 中原 formatFileSize 行为保持一致，抽到 utils 以便单测。
 *
 * @param {number} bytes 字节数
 * @returns {string}
 */
export function formatFileSize(bytes) {
  if (bytes === null || bytes === undefined || isNaN(bytes)) return '-'
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1)
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 学习效果（effectiveness，0-100）对应的 Element Plus 类型与展示文案。
 *  - >= 80 -> success（绿）
 *  - 60 ~ 79 -> warning（黄）
 *  - < 60 -> danger（红）
 *
 * @param {number} effectiveness 0-100
 * @returns {{ type: 'success'|'warning'|'danger', text: string }}
 */
export function getEffectivenessColor(effectiveness) {
  const value = Number(effectiveness)
  if (isNaN(value)) {
    return { type: 'info', text: '未知' }
  }
  if (value >= 80) {
    return { type: 'success', text: '优秀' }
  }
  if (value >= 60) {
    return { type: 'warning', text: '良好' }
  }
  return { type: 'danger', text: '待提升' }
}

/**
 * 学习效果中文标签。
 *  - >= 80 -> 优秀
 *  - 60 ~ 79 -> 良好
 *  - 40 ~ 59 -> 一般
 *  - < 40 -> 较差
 *
 * @param {number} effectiveness 0-100
 * @returns {string}
 */
export function getEffectivenessLabel(effectiveness) {
  const value = Number(effectiveness)
  if (isNaN(value)) return '未知'
  if (value >= 80) return '优秀'
  if (value >= 60) return '良好'
  if (value >= 40) return '一般'
  return '较差'
}

/**
 * 把后端返回的 feedback 文本解析为结构化摘要。
 * 当前后端 feedback 是一段可解释的学习效果说明文本，
 * 这里做轻量结构化：按换行/句号切分要点，便于结果弹窗展示。
 *
 * @param {string} feedback 后端 feedback 文本
 * @returns {{ summary: string, points: string[] }}
 */
export function parseFeedFeedback(feedback) {
  if (!feedback || typeof feedback !== 'string') {
    return { summary: '', points: [] }
  }
  const trimmed = feedback.trim()
  if (!trimmed) {
    return { summary: '', points: [] }
  }
  // 优先按换行切分；没有换行再按中文句号 / 英文句号切分
  let points
  if (trimmed.includes('\n')) {
    points = trimmed.split(/\n+/).map((s) => s.trim()).filter(Boolean)
  } else {
    points = trimmed
      .split(/[。；;]/)
      .map((s) => s.trim())
      .filter((s) => s.length > 0)
  }
  return {
    summary: trimmed,
    points
  }
}

/**
 * 判断任务是否已进入知识库（documentId 非空即视为已入库）。
 *
 * @param {object} task 投喂任务对象
 * @returns {boolean}
 */
export function hasKnowledgeBaseLink(task) {
  if (!task || typeof task !== 'object') return false
  const id = task.documentId
  return id !== null && id !== undefined && String(id).trim() !== ''
}

/**
 * 安全读取任务的知识点数量。
 * 后端 knowledgePoints 可能是数组或逗号分隔字符串。
 *
 * @param {object} task 投喂任务对象
 * @returns {number}
 */
export function getKnowledgePointCount(task) {
  if (!task || typeof task !== 'object') return 0
  const kp = task.knowledgePoints
  if (kp === null || kp === undefined) return 0
  if (Array.isArray(kp)) return kp.length
  if (typeof kp === 'string') return parseMultiPaths(kp).length
  return 0
}

export default {
  parseMultiPaths,
  formatFileSize,
  getEffectivenessColor,
  getEffectivenessLabel,
  parseFeedFeedback,
  hasKnowledgeBaseLink,
  getKnowledgePointCount
}
