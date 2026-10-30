/**
 * 用户数据管理（UserDataRecord）纯逻辑工具函数。
 *
 * 本模块不依赖 Vue / Element / DOM，可在单元测试中直接调用。
 * 与后端 /api/admin/data-pool 能力对齐：
 *  - integrityScore 后端为 Double，默认 0.5（0~1 制），这里统一归一化为 0~100 展示
 *  - status / backupStatus 后端为大写枚举字符串（VALIDATED / PENDING_BACKUP 等）
 */

// 数据类型 → 中文显示名
export const DATA_TYPE_LABELS = {
  social_post: '社区帖子',
  social_comment: '社区评论',
  social_feed: '动态信息流',
  private_chat: '私聊记录',
  group_chat: '群聊记录',
  ai_chat: 'AI对话',
  ai_character: 'AI角色',
  ai_elf_interaction: '桌宠交互',
  ai_memory: 'AI记忆',
  knowledge_query: '知识查询',
  tool_call: '工具调用',
  web_search: '网页搜索'
}

// 记录状态 → 中文显示名
export const STATUS_LABELS = {
  VALIDATED: '已验证',
  ACTIVE: '活跃',
  PENDING: '待处理',
  ARCHIVED: '已归档',
  DELETED: '已删除'
}

// 备份状态 → 中文显示名
export const BACKUP_STATUS_LABELS = {
  BACKED_UP: '已备份',
  PENDING_BACKUP: '待备份',
  FAILED: '备份失败'
}

/**
 * 数据类型显示名。未知类型原样返回。
 */
export function getDataTypeLabel (type) {
  if (!type) return '-'
  return DATA_TYPE_LABELS[type] || type
}

/**
 * 记录状态显示名。未知状态原样返回。
 */
export function getStatusLabel (status) {
  if (!status) return '-'
  return STATUS_LABELS[status] || status
}

/**
 * 备份状态显示名。未知状态原样返回。
 */
export function getBackupStatusLabel (status) {
  if (!status) return '-'
  return BACKUP_STATUS_LABELS[status] || status
}

/**
 * 将后端 integrityScore 归一化为 0~100。
 * 后端默认 0.5（0~1 制），若值在 [0,1] 区间则按比例换算为百分制。
 * @param {number|null|undefined} score
 * @returns {number} 0~100 的整数百分比
 */
export function getIntegrityPercent (score) {
  if (score === null || score === undefined) return 0
  const n = Number(score)
  if (Number.isNaN(n) || n < 0) return 0
  // 0~1 制 → 百分制
  if (n <= 1) return Math.round(n * 100)
  return Math.round(n)
}

/**
 * 完整性评分等级。
 * 百分制：≥80 优秀 / 60~79 良好 / 40~59 一般 / <40 较差
 * @returns {'excellent'|'good'|'fair'|'poor'}
 */
export function getIntegrityGrade (score) {
  const p = getIntegrityPercent(score)
  if (p >= 80) return 'excellent'
  if (p >= 60) return 'good'
  if (p >= 40) return 'fair'
  return 'poor'
}

/**
 * 完整性评分对应 Element Plus 标签类型颜色。
 * excellent→success(绿) / good→warning(黄) / fair→warning(橙) / poor→danger(红)
 */
export function getIntegrityTagType (score) {
  const grade = getIntegrityGrade(score)
  const map = {
    excellent: 'success',
    good: 'warning',
    fair: 'warning',
    poor: 'danger'
  }
  return map[grade] || 'info'
}

/**
 * 状态标签颜色。
 */
export function getStatusTagType (status) {
  const map = {
    VALIDATED: 'success',
    ACTIVE: 'success',
    PENDING: 'warning',
    ARCHIVED: 'info',
    DELETED: 'danger'
  }
  return map[status] || 'info'
}

/**
 * 备份状态标签颜色。
 */
export function getBackupStatusTagType (status) {
  const map = {
    BACKED_UP: 'success',
    PENDING_BACKUP: 'warning',
    FAILED: 'danger'
  }
  return map[status] || 'info'
}

/**
 * 内容摘要截断：超过 maxLen 个字符时截断并追加省略号。
 * @param {string} text
 * @param {number} [maxLen=40]
 * @returns {string}
 */
export function truncateContent (text, maxLen = 40) {
  if (text === null || text === undefined) return ''
  const str = String(text)
  if (str.length <= maxLen) return str
  return str.slice(0, maxLen) + '...'
}

/**
 * 手机号掩码：13812345678 → 138****5678
 * 仅匹配 11 位中国大陆手机号段。
 */
export function maskPhone (text) {
  if (!text) return ''
  return String(text).replace(/(1[3-9]\d)\d{4}(\d{4})/g, '$1****$2')
}

/**
 * 邮箱掩码：zhangsan@example.com → z***@example.com
 */
export function maskEmail (text) {
  if (!text) return ''
  return String(text).replace(
    /([A-Za-z0-9])[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\.[A-Za-z]{2,})/g,
    '$1***$2'
  )
}

/**
 * 敏感信息掩码：对文本中的手机号与邮箱统一打码。
 */
export function maskSensitive (text) {
  if (!text) return ''
  return maskEmail(maskPhone(String(text)))
}

/**
 * 从筛选条件构建 records 查询参数。
 * 仅保留后端 /records 端点支持的参数（page/size/userId/dataType/keyword），
 * 空值 / 空串不传入。
 * @param {object} filters
 * @param {number} filters.page
 * @param {number} filters.size
 * @param {string|number} [filters.userId]
 * @param {string} [filters.dataType]
 * @param {string} [filters.keyword]
 * @returns {object} 可直接作为 axios params 的对象
 */
export function buildRecordQueryParams (filters = {}) {
  const params = {
    page: filters.page || 1,
    size: filters.size || 20
  }
  if (filters.userId !== null && filters.userId !== undefined && filters.userId !== '') {
    params.userId = filters.userId
  }
  if (filters.dataType) {
    params.dataType = filters.dataType
  }
  if (filters.keyword) {
    params.keyword = filters.keyword
  }
  return params
}

export default {
  DATA_TYPE_LABELS,
  STATUS_LABELS,
  BACKUP_STATUS_LABELS,
  getDataTypeLabel,
  getStatusLabel,
  getBackupStatusLabel,
  getIntegrityPercent,
  getIntegrityGrade,
  getIntegrityTagType,
  getStatusTagType,
  getBackupStatusTagType,
  truncateContent,
  maskPhone,
  maskEmail,
  maskSensitive,
  buildRecordQueryParams
}
