/**
 * 搜索引用标注纯逻辑工具函数。
 * 无副作用，便于单元测试。
 */

/**
 * 匹配回答文本中的引用上标 [n]（n 为正整数）。
 */
const CITATION_REF_REGEX = /\[(\d+)\]/g

/**
 * 从文本中解析所有引用序号，按出现顺序返回（去重保序）。
 *
 * @param {string} text 回答文本，可能含 [1] [2] 等标注
 * @returns {number[]} 有序且去重后的引用序号列表
 */
export function parseCitationRefs(text) {
  if (!text || typeof text !== 'string') {
    return []
  }
  const result = []
  const seen = new Set()
  let m
  const re = new RegExp(CITATION_REF_REGEX.source, 'g')
  while ((m = re.exec(text)) !== null) {
    const n = parseInt(m[1], 10)
    if (!seen.has(n)) {
      seen.add(n)
      result.push(n)
    }
  }
  return result
}

/**
 * 校验 URL 是否安全可跳转：仅允许 http/https 协议，阻止 javascript:/data:/file: 等。
 *
 * @param {string} url 待校验 URL
 * @returns {boolean} 是否安全
 */
export function isSafeUrl(url) {
  if (!url || typeof url !== 'string') {
    return false
  }
  const trimmed = url.trim()
  if (!trimmed) {
    return false
  }
  // 仅允许显式的 http/https 链接，阻止 javascript:/data:/file: 以及相对路径
  if (!/^https?:\/\//i.test(trimmed)) {
    return false
  }
  try {
    const parsed = new URL(trimmed)
    return parsed.protocol === 'http:' || parsed.protocol === 'https:'
  } catch (e) {
    return false
  }
}

/**
 * 将一个引用对象转换为纯文本引用格式，便于“复制引用”。
 *
 * @param {object} citation 引用对象 {index, title, url, source}
 * @returns {string} 形如 "[1] 标题（来源）- https://..."
 */
export function buildCitationText(citation) {
  if (!citation) {
    return ''
  }
  const idx = citation.index != null ? `[${citation.index}]` : ''
  const title = citation.title || '无标题'
  const source = citation.source ? `（${citation.source}）` : ''
  const url = citation.url || ''
  return `${idx} ${title}${source} - ${url}`.trim()
}

/**
 * 把回答文本切分为“普通片段 + 引用上标”的渲染片段，供 v-for 渲染为可点击上标。
 *
 * @param {string} text 回答文本
 * @returns {Array<{type: 'text'|'ref', value: string, index?: number}>}
 */
export function tokenizeAnswer(text) {
  if (!text || typeof text !== 'string') {
    return []
  }
  const tokens = []
  let lastIndex = 0
  const re = new RegExp(CITATION_REF_REGEX.source, 'g')
  let m
  while ((m = re.exec(text)) !== null) {
    if (m.index > lastIndex) {
      tokens.push({ type: 'text', value: text.slice(lastIndex, m.index) })
    }
    tokens.push({ type: 'ref', value: m[0], index: parseInt(m[1], 10) })
    lastIndex = m.index + m[0].length
  }
  if (lastIndex < text.length) {
    tokens.push({ type: 'text', value: text.slice(lastIndex) })
  }
  return tokens
}

export default {
  parseCitationRefs,
  isSafeUrl,
  buildCitationText,
  tokenizeAnswer
}
