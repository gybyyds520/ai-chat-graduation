/**
 * 角色广场纯逻辑工具（与 UI 解耦，便于单测）。
 */

/** 全部分类标签的固定值 */
export const ALL_CATEGORIES = '全部'

/**
 * 按分类过滤角色。
 * @param {Array} characters 角色列表
 * @param {string} category 分类，"全部" 或空表示不过滤
 * @returns {Array}
 */
export function filterByCategory(characters, category) {
  if (!characters) return []
  if (!category || category === ALL_CATEGORIES) return [...characters]
  return characters.filter((c) => c && c.category === category)
}

/**
 * 解析标签字符串为数组（逗号分隔，去空去重）。
 * @param {string} tagsStr 如 "治愈,陪伴,日常"
 * @returns {string[]}
 */
export function parseTags(tagsStr) {
  if (!tagsStr) return []
  return tagsStr
    .split(/[,，]/)
    .map((t) => t.trim())
    .filter(Boolean)
    .filter((t, i, arr) => arr.indexOf(t) === i)
}

/**
 * 广场排序：hot=使用次数 / created=最新(按id) / rating=评分。
 * 返回新数组，不修改原数组。
 * @param {Array} characters
 * @param {string} sortBy
 * @returns {Array}
 */
export function sortCharacters(characters, sortBy) {
  if (!characters) return []
  const arr = [...characters]
  switch (sortBy) {
    case 'rating':
      return arr.sort((a, b) => (b.rating || 0) - (a.rating || 0) || (b.useCount || 0) - (a.useCount || 0))
    case 'created':
      return arr.sort((a, b) => (b.id || 0) - (a.id || 0))
    case 'hot':
    default:
      return arr.sort((a, b) => (b.useCount || 0) - (a.useCount || 0) || (b.id || 0) - (a.id || 0))
  }
}

/**
 * 使用次数友好显示。
 * @param {number} n
 * @returns {string}
 */
export function formatUseCount(n) {
  if (!n || n <= 0) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return String(n)
}

/**
 * 判断是否还有下一页（与后端 hasMore 口径一致）。
 * @param {number} total 总条数
 * @param {number} page  当前页码（从 1 开始）
 * @param {number} size  每页大小
 * @returns {boolean}
 */
export function canLoadMore(total, page, size) {
  const t = Number(total) || 0
  const p = Number(page) || 1
  const s = Number(size) || 12
  return p * s < t
}
