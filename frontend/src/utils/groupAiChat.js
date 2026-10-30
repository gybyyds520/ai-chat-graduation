/**
 * 多 AI 角色同场群聊纯逻辑（轮流顺序计算、回复格式化）。
 */

/**
 * 计算一轮发言顺序（从游标 startIndex 开始，轮转覆盖所有成员）。
 * @param {Array} members 成员数组
 * @param {number} startIndex 起始游标
 * @returns {Array} 排序后的成员副本
 */
export function roundRobinOrder(members, startIndex) {
  if (!members || members.length === 0) return []
  const n = members.length
  const start = ((startIndex % n) + n) % n
  const ordered = []
  for (let i = 0; i < n; i++) {
    ordered.push(members[(start + i) % n])
  }
  return ordered
}

/**
 * 群成员列表区分人类成员与 AI 角色（AI 成员加 🤖 标记前缀）。
 * @param {{isAi?:boolean, name:string}[]} members
 * @returns {string[]} 带标记的显示名
 */
export function formatMemberList(members) {
  if (!members) return []
  return members.map((m) => (m && m.isAi ? `🤖 ${m.name}` : m.name))
}

/**
 * 格式化一轮 AI 回复，用于在 UI 中按角色分组展示。
 * @param {{characterName:string, content:string}[]} replies
 * @returns {string[]} 形如 "【Alice】内容"
 */
export function formatAiReplies(replies) {
  if (!replies) return []
  return replies.map((r) => `【${r.characterName}】${r.content}`)
}
