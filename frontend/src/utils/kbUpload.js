/**
 * 知识库上传与文档列表展示纯逻辑（便于单元测试）。
 *
 * - XHR onprogress 进度百分比计算（loaded/total）；
 * - 文件大小友好格式化（B/KB/MB/GB）；
 * - 文档状态中文标签与状态映射。
 */

/**
 * 计算上传进度百分比 0-100。
 * total<=0 时返回已上报的 loaded 对应占位（50，提示"接收中"），避免除零。
 * @param {number} loaded 已上传字节
 * @param {number} total 总字节
 * @returns {number} 0-100 整数
 */
export function progressPercent(loaded, total) {
  const l = Number(loaded) || 0
  const t = Number(total) || 0
  if (t <= 0) return l > 0 ? 50 : 0
  const pct = Math.floor((l / t) * 100)
  if (pct < 0) return 0
  if (pct > 100) return 100
  return pct
}

/**
 * 文件大小友好格式化。
 * @param {number} bytes 字节数
 * @param {number} digits 小数位，默认 1
 * @returns {string} 如 "1.2 MB"
 */
export function formatFileSize(bytes, digits = 1) {
  const b = Number(bytes)
  if (isNaN(b) || b <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = b
  let unit = 0
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024
    unit += 1
  }
  const fixed = unit === 0 ? Math.round(size) : Number(size.toFixed(digits))
  return `${fixed} ${units[unit]}`
}

/**
 * 文档状态 -> 中文标签。
 * ready 就绪 / processing 解析中 / failed 失败。
 */
export function docStatusLabel(status) {
  switch ((status || '').toString().toLowerCase()) {
    case 'ready':
    case 'success':
    case 'done':
      return '就绪'
    case 'processing':
    case 'parsing':
    case 'pending':
      return '解析中'
    case 'failed':
    case 'error':
      return '失败'
    default:
      return '未知'
  }
}

/**
 * 文档状态对应的 Element 标签类型（success/warning/danger/info）。
 */
export function docStatusType(status) {
  switch ((status || '').toString().toLowerCase()) {
    case 'ready':
    case 'success':
    case 'done':
      return 'success'
    case 'processing':
    case 'parsing':
    case 'pending':
      return 'warning'
    case 'failed':
    case 'error':
      return 'danger'
    default:
      return 'info'
  }
}

/**
 * 是否可"重新解析"：仅失败或就绪态允许（处理中不允许重复触发）。
 */
export function canReparse(status) {
  const s = (status || '').toString().toLowerCase()
  return s === 'ready' || s === 'failed' || s === 'error' || s === ''
}

/**
 * 构造多文档问答请求体：
 * - documentIds 为空 / null / ['__all__'] -> 全部文档（documentIds=null）；
 * - 否则返回清洗后的 ID 列表。
 * @param {string} query 问题
 * @param {number[]|null} documentIds 选定文档 ID
 * @param {number} topK
 * @returns {{query:string, documentIds:(number[]|null), topK:number}}
 */
export function buildAskBody(query, documentIds = null, topK = 3) {
  const q = (query || '').trim()
  let ids = null
  if (Array.isArray(documentIds) && documentIds.length > 0) {
    const cleaned = documentIds
      .map(id => Number(id))
      .filter(id => !isNaN(id) && id > 0)
    if (cleaned.length > 0) ids = cleaned
  }
  return { query: q, documentIds: ids, topK: topK > 0 ? topK : 3 }
}

export default {
  progressPercent,
  formatFileSize,
  docStatusLabel,
  docStatusType,
  canReparse,
  buildAskBody
}
