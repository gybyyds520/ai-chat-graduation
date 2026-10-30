/**
 * 角色创建纯逻辑（与 UI 解耦，便于单测）。
 * 覆盖：性格标签多选、标签序列化。
 */

/** 可选性格标签 */
export const PERSONALITY_TAGS = ['温柔', '傲娇', '活泼', '高冷', '治愈', '元气', '腹黑', '天然呆']

/**
 * 多选标签序列化为逗号分隔字符串（存入 personalityTags 字段）。
 * @param {string[]} tags
 * @returns {string}
 */
export function serializePersonalityTags(tags) {
  if (!Array.isArray(tags)) return ''
  return tags.filter(Boolean).join(',')
}

/**
 * 反序列化 personalityTags 字符串为数组。
 * @param {string} str 逗号分隔
 * @returns {string[]}
 */
export function parsePersonalityTags(str) {
  if (!str) return []
  return String(str)
    .split(/[,，]/)
    .map((t) => t.trim())
    .filter(Boolean)
    .filter((t, i, arr) => arr.indexOf(t) === i)
}

/**
 * 校验性格标签是否都在可选范围内。
 * @param {string[]} tags
 * @returns {boolean}
 */
export function isValidPersonalityTags(tags) {
  if (!Array.isArray(tags)) return false
  return tags.every((t) => PERSONALITY_TAGS.includes(t))
}
