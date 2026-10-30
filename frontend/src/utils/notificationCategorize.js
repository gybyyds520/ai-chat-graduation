/**
 * 通知中心分类页签纯逻辑（便于单元测试）。
 *
 * 三大页签：
 *  - interaction 互动：好友申请 / 点赞 / 评论 / 分享
 *  - system 系统：公告 / 系统通知
 *  - ai AI：精灵提醒 / 记忆提醒
 *
 * 同时提供未读统计、一键全部已读后的列表计算。
 */

export const TABS = [
  { key: 'interaction', label: '互动' },
  { key: 'system', label: '系统' },
  { key: 'ai', label: 'AI' }
]

/**
 * 把单条通知归类到某个页签 key。
 * 依据字段优先级：category > type > targetType。
 * @param {object} notification 通知对象
 * @returns {'interaction'|'system'|'ai'}
 */
export function categorize(notification) {
  if (!notification) return 'system'
  // 显式 category 优先
  const c = (notification.category || '').toString().toLowerCase()
  if (c === 'interaction' || c === 'social' || c === 'like' || c === 'comment' || c === 'friend' || c === 'follow') {
    return 'interaction'
  }
  if (c === 'ai' || c === 'elf' || c === 'memory' || c === 'spirit') {
    return 'ai'
  }
  if (c === 'system' || c === 'announcement' || c === 'notice') {
    return 'system'
  }
  // 其次看 type
  const t = (notification.type || '').toString().toLowerCase()
  if (['like', 'comment', 'share', 'friend', 'follow', 'apply', 'request'].includes(t)) {
    return 'interaction'
  }
  if (['elf', 'memory', 'spirit', 'ai_remind'].includes(t)) {
    return 'ai'
  }
  if (['system', 'announcement', 'notice', 'admin'].includes(t)) {
    return 'system'
  }
  // targetType 兜底
  const tt = (notification.targetType || '').toString().toLowerCase()
  if (['post', 'feed', 'comment', 'like'].includes(tt)) {
    return 'interaction'
  }
  return 'system'
}

/**
 * 按页签分组通知，返回 { interaction:[], system:[], ai:[] }。
 * @param {Array} notifications
 * @returns {object}
 */
export function groupByTab(notifications) {
  const result = { interaction: [], system: [], ai: [] }
  if (!Array.isArray(notifications)) return result
  for (const n of notifications) {
    const key = categorize(n)
    result[key].push(n)
  }
  return result
}

/**
 * 统计未读数（total + 各页签）。
 * 判定未读：read===false / readStatus===0 / isRead===false。
 * @param {Array} notifications
 * @returns {{total:number, interaction:number, system:number, ai:number}}
 */
export function unreadCounts(notifications) {
  const counts = { total: 0, interaction: 0, system: 0, ai: 0 }
  if (!Array.isArray(notifications)) return counts
  for (const n of notifications) {
    if (!n) continue
    if (!isUnread(n)) continue
    counts.total += 1
    counts[categorize(n)] += 1
  }
  return counts
}

/**
 * 判断单条通知是否未读。
 */
export function isUnread(notification) {
  if (!notification) return false
  if (notification.read === false) return true
  if (notification.readStatus === 0) return true
  if (notification.isRead === false) return true
  return false
}

/**
 * 一键全部已读：返回标记为已读后的新数组（不变更入参）。
 * @param {Array} notifications
 * @returns {Array}
 */
export function markAllRead(notifications) {
  if (!Array.isArray(notifications)) return []
  return notifications.map(n => ({ ...n, read: true, readStatus: 1, isRead: true }))
}

/**
 * 过滤出某个页签的通知列表。
 */
export function filterByTab(notifications, tabKey) {
  if (!Array.isArray(notifications)) return []
  return notifications.filter(n => categorize(n) === tabKey)
}

export default {
  TABS,
  categorize,
  groupByTab,
  unreadCounts,
  isUnread,
  markAllRead,
  filterByTab
}
