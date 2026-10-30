/**
 * AI 搜索历史与搜索建议纯逻辑工具（便于单元测试）。
 *
 * - 最近 N 条搜索历史，按时间倒序，新搜索置顶，重复搜索去重并置顶；
 * - 搜索建议：从历史中按前缀/包含关系做简单联想；
 * - 持久化到 localStorage，key 可注入（测试时用内存 storage）。
 */

const DEFAULT_KEY = 'ai_search_history'
const DEFAULT_MAX = 10

/**
 * 读取历史。容错：存储为空 / 非法 JSON / 非数组时返回空数组。
 * @param {Storage} storage 存储介质，默认 localStorage
 * @param {string} key 存储键
 * @returns {string[]}
 */
export function loadHistory(storage = (typeof localStorage !== 'undefined' ? localStorage : null), key = DEFAULT_KEY) {
  if (!storage) return []
  try {
    const raw = storage.getItem(key)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed.filter(item => typeof item === 'string' && item.trim() !== '')
  } catch (e) {
    return []
  }
}

/**
 * 保存历史（整体覆盖写入）。
 */
export function saveHistory(history, storage = (typeof localStorage !== 'undefined' ? localStorage : null), key = DEFAULT_KEY) {
  if (!storage) return
  try {
    storage.setItem(key, JSON.stringify(Array.isArray(history) ? history : []))
  } catch (e) {
    // 隐私模式 / 配额已满等：静默失败，不影响主流程
  }
}

/**
 * 记录一次搜索：去重 + 置顶 + 截断到 max 条。
 * @param {string} query 搜索词（空串/空白忽略）
 * @param {string[]} history 当前历史（可空）
 * @param {number} max 最多保留条数，默认 10
 * @returns {string[]} 新历史（不变更入参）
 */
export function pushHistory(query, history = [], max = DEFAULT_MAX) {
  const q = typeof query === 'string' ? query.trim() : ''
  if (!q) return Array.isArray(history) ? [...history] : []
  const base = Array.isArray(history) ? history.filter(item => typeof item === 'string') : []
  const filtered = base.filter(item => item !== q)
  const next = [q, ...filtered]
  const limit = max > 0 ? max : DEFAULT_MAX
  return next.slice(0, limit)
}

/**
 * 记录一次搜索并持久化。
 * @returns {string[]} 写入后的新历史
 */
export function recordSearch(query, storage = (typeof localStorage !== 'undefined' ? localStorage : null), key = DEFAULT_KEY, max = DEFAULT_MAX) {
  const next = pushHistory(query, loadHistory(storage, key), max)
  saveHistory(next, storage, key)
  return next
}

/**
 * 删除单条历史。
 */
export function removeHistoryItem(query, storage = (typeof localStorage !== 'undefined' ? localStorage : null), key = DEFAULT_KEY) {
  const next = loadHistory(storage, key).filter(item => item !== query)
  saveHistory(next, storage, key)
  return next
}

/**
 * 清空历史。
 */
export function clearHistory(storage = (typeof localStorage !== 'undefined' ? localStorage : null), key = DEFAULT_KEY) {
  if (storage) {
    try { storage.removeItem(key) } catch (e) { /* ignore */ }
  }
  return []
}

/**
 * 根据输入从历史中给出搜索建议。
 * - 输入为空：返回最近 historyLength 条历史（用于点击搜索框下拉）；
 * - 输入非空：历史中"以前缀开头"优先，其次"包含"，去重，最多返回 limit 条；
 * - 命中建议时排除与输入完全相同的项。
 *
 * @param {string} input 当前输入
 * @param {string[]} history 历史（可空，缺省从 storage 读取）
 * @param {object} opts { limit=6, historyLength=6 }
 * @returns {string[]}
 */
export function suggest(input, history = null, opts = {}) {
  const limit = opts.limit > 0 ? opts.limit : 6
  const historyLength = opts.historyLength > 0 ? opts.historyLength : 6
  const list = Array.isArray(history) ? history : loadHistory()
  const q = (input == null ? '' : String(input)).trim()

  if (!q) {
    return list.slice(0, historyLength)
  }
  const lower = q.toLowerCase()
  const prefix = []
  const contains = []
  for (const item of list) {
    if (typeof item !== 'string' || item === q) continue
    const l = item.toLowerCase()
    if (l.startsWith(lower)) {
      prefix.push(item)
    } else if (l.includes(lower)) {
      contains.push(item)
    }
  }
  return [...prefix, ...contains].slice(0, limit)
}

export default {
  DEFAULT_KEY,
  DEFAULT_MAX,
  loadHistory,
  saveHistory,
  pushHistory,
  recordSearch,
  removeHistoryItem,
  clearHistory,
  suggest
}
