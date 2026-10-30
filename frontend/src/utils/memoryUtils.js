/**
 * AI 记忆纯逻辑工具（类型映射、重要性星级渲染、日期格式化）。
 * 不依赖 Vue/请求，便于单元测试。
 */

// 记忆类型映射：后端枚举 -> 中文标签
export const MEMORY_TYPE_MAP = {
  fact: '事实',
  preference: '偏好',
  event: '事件',
  personality: '性格',
  other: '其他'
}

// 来源映射
export const MEMORY_SOURCE_MAP = {
  chat: '聊天沉淀',
  manual: '手动添加',
  profile: '资料',
  system: '系统'
}

// 类型筛选下拉项（含"全部"）
export const MEMORY_TYPE_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'fact', label: '事实' },
  { value: 'preference', label: '偏好' },
  { value: 'event', label: '事件' },
  { value: 'personality', label: '性格' },
  { value: 'other', label: '其他' }
]

// 来源筛选下拉项（含"全部"）
export const MEMORY_SOURCE_OPTIONS = [
  { value: '', label: '全部来源' },
  { value: 'chat', label: '聊天沉淀' },
  { value: 'manual', label: '手动添加' },
  { value: 'profile', label: '资料' },
  { value: 'system', label: '系统' }
]

// 记忆自动分类（市场级升级）：FACT/PREFERENCE/EVENT/RELATIONSHIP
export const MEMORY_CATEGORY_MAP = {
  FACT: '事实',
  PREFERENCE: '偏好',
  EVENT: '事件',
  RELATIONSHIP: '关系'
}

// 分类筛选 Tab（含"全部"）
export const MEMORY_CATEGORY_OPTIONS = [
  { value: '', label: '全部分类' },
  { value: 'FACT', label: '事实' },
  { value: 'PREFERENCE', label: '偏好' },
  { value: 'EVENT', label: '事件' },
  { value: 'RELATIONSHIP', label: '关系' }
]

/** 分类枚举 -> 中文标签；未知回退原值。 */
export function categoryLabel(category) {
  return MEMORY_CATEGORY_MAP[category] || category || ''
}

/** 类型值 -> 中文标签；未知类型回退"其他"。 */
export function typeLabel(type) {
  return MEMORY_TYPE_MAP[type] || '其他'
}

/** 来源值 -> 中文标签；未知回退"其他"。 */
export function sourceLabel(source) {
  return MEMORY_SOURCE_MAP[source] || '其他'
}

/**
 * 重要性 1-5 -> 星级渲染字符串（实心★ + 空心☆）。
 * @param {number} importance 1-5
 * @returns {string} 例如 importance=3 -> '★★★☆☆'
 */
export function importanceStars(importance) {
  const n = Number(importance)
  const clamped = Number.isFinite(n) ? Math.max(1, Math.min(5, Math.round(n))) : 3
  return '★'.repeat(clamped) + '☆'.repeat(5 - clamped)
}

/** 重要性数值 -> 中文描述。 */
export function importanceLabel(importance) {
  const map = { 1: '很低', 2: '较低', 3: '一般', 4: '较高', 5: '很高' }
  return map[importance] || '一般'
}

/**
 * 将 ISO 时间字符串格式化为 'YYYY-MM-DD HH:mm'。
 * @param {string|Date} iso
 * @returns {string}
 */
export function formatDateTime(iso) {
  if (!iso) return '—'
  const d = iso instanceof Date ? iso : new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  const pad = (v) => String(v).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 记忆类型对应的 el-tag 类型（用于 element-plus 颜色）。 */
export function typeTagType(type) {
  const map = {
    fact: '',
    preference: 'success',
    event: 'warning',
    personality: 'danger',
    other: 'info'
  }
  return map[type] || 'info'
}

/**
 * 构建记忆列表查询参数（GET /api/memory/list）。
 *
 * 设计：仅当过滤值 trim 后非空时才加入对应键，避免把空串发给后端造成误过滤；
 * userId 由后端从登录令牌解析（AuthContext.requireUserId），前端不传递，
 * 因此返回对象恒定只含 page/size，按需附带 keyword/source/memoryType。
 *
 * @param {string} keyword 内容关键词（可空）
 * @param {string} source   来源过滤（chat/manual/profile/system，可空）
 * @param {string} type     记忆类型过滤（fact/preference/event/personality/other，可空）
 * @param {number} page     页码（可空，默认 1）
 * @param {number} size     每页条数（可空，默认 50）
 * @returns {{page:number,size:number,keyword?:string,source?:string,memoryType?:string}}
 */
export function buildMemorySearchParams(keyword, source, type, page, size) {
  const params = {
    page: Number.isFinite(Number(page)) && Number(page) > 0 ? Number(page) : 1,
    size: Number.isFinite(Number(size)) && Number(size) > 0 ? Number(size) : 50
  }
  const kw = (keyword || '').trim()
  if (kw) params.keyword = kw
  const src = (source || '').trim()
  if (src) params.source = src
  const t = (type || '').trim()
  if (t) params.memoryType = t
  return params
}
