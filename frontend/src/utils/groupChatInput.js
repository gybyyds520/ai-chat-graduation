/**
 * 群聊 @提及 纯逻辑（与 UI 解耦，便于单测）。
 *
 * 约定：输入框中以 "@角色名 " 形式提及群内 AI 角色。
 * 本模块只负责从文本中解析出被 @ 的 AI 角色 id，以及判断本轮是否需要触发 AI 发言。
 */

/**
 * 从输入文本解析被 @ 的 AI 角色。
 * @param {string} text 用户输入文本
 * @param {{characterId:number, characterName?:string, id?:number}[]} aiMembers 群内 AI 成员
 * @returns {number[]} 被 @ 的 characterId 列表（去重，保持文本出现顺序）
 */
export function parseMentionedCharacters(text, aiMembers) {
  if (!text || !aiMembers || aiMembers.length === 0) return []
  // 按文本中 @ 的出现顺序匹配群内 AI 角色名
  const result = []
  const seen = new Set()
  const re = /@([^\s@]+)/g
  let m
  while ((m = re.exec(text)) !== null) {
    const name = m[1]
    const hit = aiMembers.find((a) => a.characterName === name)
    if (hit && !seen.has(hit.characterId)) {
      seen.add(hit.characterId)
      result.push(hit.characterId)
    }
  }
  return result
}

/**
 * 是否需要触发一轮 AI 发言：
 *  - 文本中显式 @ 了 AI 角色；或
 *  - 群内有 AI 角色且以触发词开头（如 "/ai"）。
 * @param {string} text
 * @param {number[]} mentionedIds parseMentionedCharacters 的结果
 */
export function shouldTriggerAi(text, mentionedIds) {
  if (!text) return false
  if (Array.isArray(mentionedIds) && mentionedIds.length > 0) return true
  return /^\s*\/ai\b/.test(text)
}

/** 去除文本中的 @提及前缀，作为发给 AI 的触发消息。 */
export function stripMentions(text) {
  if (!text) return ''
  return text.replace(/@[^\s@]+/g, '').trim()
}
