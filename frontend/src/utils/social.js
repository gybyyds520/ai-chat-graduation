/**
 * 好友/社交纯逻辑（与 UI 解耦，便于单测）。
 * 覆盖：在线状态判定、备注名展示、共同角色推荐理由。
 */

/** 在线阈值：最近 5 分钟活跃视为在线 */
export const ONLINE_THRESHOLD_MS = 5 * 60 * 1000

/**
 * 根据最近活跃时间判定在线状态。
 * @param {string|number|Date} lastActiveTime 最近活跃时间
 * @param {number} nowMs 当前时间戳（ms），便于测试注入
 * @param {number} thresholdMs 在线阈值，默认 5 分钟
 * @returns {boolean}
 */
export function isOnline(lastActiveTime, nowMs = Date.now(), thresholdMs = ONLINE_THRESHOLD_MS) {
  if (!lastActiveTime) return false
  const t = new Date(lastActiveTime).getTime()
  if (Number.isNaN(t)) return false
  return nowMs - t <= thresholdMs
}

/**
 * 好友展示名：有备注优先备注，否则用用户名/昵称。
 * @param {object} friend 好友对象，含 remark/nickname/username
 * @returns {string}
 */
export function displayFriendName(friend) {
  if (!friend) return '好友'
  return friend.remark || friend.nickname || friend.username || '好友'
}

/**
 * 生成好友推荐理由：基于共同使用的角色。
 * @param {string[]} commonCharacters 双方共同使用/喜欢的角色名数组
 * @returns {string} 如 "你们都在玩「温柔助手」「学霸」等 2 个角色"
 */
export function recommendReason(commonCharacters) {
  const list = Array.isArray(commonCharacters) ? commonCharacters.filter(Boolean) : []
  if (!list.length) return '你可能认识的人'
  if (list.length === 1) return `你们都在玩「${list[0]}」这个角色`
  if (list.length === 2) return `你们都在玩「${list[0]}」和「${list[1]}」`
  return `你们都在玩「${list[0]}」「${list[1]}」等 ${list.length} 个角色`
}
