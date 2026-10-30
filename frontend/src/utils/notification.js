/**
 * 通知中心纯逻辑工具函数（便于单元测试）。
 * 所有时间判断均支持传入 `now` 参数以便测试确定性。
 */

// 工具：取某天 00:00:00
function startOfDay (date) {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  return d
}

/**
 * 将通知按时间分组：今天 / 昨天 / 更早。
 * 仅返回非空分组，顺序固定为 今天 → 昨天 → 更早。
 * @param {Array} notifications 通知数组，每项需有 createdAt
 * @param {Date} now 参考时间，默认 new Date()
 * @returns {Array<{label: string, items: Array}>}
 */
export function groupByDate (notifications, now = new Date()) {
  if (!Array.isArray(notifications)) return []
  const groups = [
    { label: '今天', items: [] },
    { label: '昨天', items: [] },
    { label: '更早', items: [] }
  ]
  const todayStart = startOfDay(now)
  const yesterdayStart = new Date(todayStart)
  yesterdayStart.setDate(yesterdayStart.getDate() - 1)

  for (const n of notifications) {
    if (!n || !n.createdAt) continue
    const d = new Date(n.createdAt)
    if (isNaN(d.getTime())) continue
    if (d.getTime() >= todayStart.getTime()) {
      groups[0].items.push(n)
    } else if (d.getTime() >= yesterdayStart.getTime()) {
      groups[1].items.push(n)
    } else {
      groups[2].items.push(n)
    }
  }
  return groups.filter(g => g.items.length > 0)
}

/**
 * 友好时间显示：
 * 今天 → "HH:mm"
 * 昨天 → "昨天 HH:mm"
 * 今年更早 → "MM-DD"
 * 跨年 → "YYYY-MM-DD"
 * @param {string} isoString 时间字符串
 * @param {Date} now 参考时间
 * @returns {string}
 */
export function formatNotificationTime (isoString, now = new Date()) {
  if (!isoString) return ''
  const d = new Date(isoString)
  if (isNaN(d.getTime())) return ''
  const todayStart = startOfDay(now)
  const yesterdayStart = new Date(todayStart)
  yesterdayStart.setDate(yesterdayStart.getDate() - 1)

  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')

  if (d.getTime() >= todayStart.getTime()) {
    return `${hh}:${mm}`
  }
  if (d.getTime() >= yesterdayStart.getTime()) {
    return `昨天 ${hh}:${mm}`
  }
  const MM = String(d.getMonth() + 1).padStart(2, '0')
  const DD = String(d.getDate()).padStart(2, '0')
  if (d.getFullYear() === now.getFullYear()) {
    return `${MM}-${DD}`
  }
  return `${d.getFullYear()}-${MM}-${DD}`
}

/**
 * 按通知类型返回对应图标 emoji。
 * @param {string} type like/comment/share/system
 * @returns {string}
 */
export function getNotificationIcon (type) {
  switch (type) {
    case 'like':
      return '👍'
    case 'comment':
      return '💬'
    case 'share':
      return '🔄'
    case 'system':
      return '📢'
    default:
      return '🔔'
  }
}

/**
 * 按通知类型返回动作文字。
 * @param {string} type like/comment/share/system
 * @returns {string}
 */
export function getActionText (type) {
  switch (type) {
    case 'like':
      return '赞了你的'
    case 'comment':
      return '评论了你的'
    case 'share':
      return '分享了你的'
    case 'system':
      return '系统通知'
    default:
        return ''
  }
}

/**
 * 判断该通知是否支持跳转。
 * 兼容两种调用方式：
 *  - 传入字符串（历史调用，仅按 targetType 判断）：post/feed/friend/group → true；
 *  - 传入通知对象（M4 ⑭ 升级）：根据 type/category/targetType 综合计算路由，能算出路由即 true。
 * @param {string|object} input targetType 字符串或整条通知对象
 * @returns {boolean}
 */
export function canJump (input) {
  if (input && typeof input === 'object') {
    return getTargetRoute(input) !== null
  }
  return ['post', 'feed', 'friend', 'group'].includes(input)
}

/**
 * 根据通知的 type/category/targetType/targetId 计算跳转路由路径。
 *
 * M4 ⑭ 路由规则：
 *  - 好友申请（friend/apply/follow）            → /friends
 *  - 点赞/评论/分享（like/comment/share）        → /social-feed（社区帖子详情，带 ?postId=）
 *  - AI 提醒（elf/memory/spirit/ai_remind）     → /chat
 *  - 系统公告（system/announcement/notice/admin）→ /announcements
 *  - 其余回退到历史 targetType 路由（post/feed/friend/group + targetId）。
 *
 * @param {object} notification 通知对象
 * @returns {string|null}
 */
export function getTargetRoute (notification) {
  if (!notification) return null

  // 1) 优先按 type / category 归类（M4 ⑭ 新增，不依赖 targetId）
  const t = (notification.type || '').toString().toLowerCase()
  const c = (notification.category || '').toString().toLowerCase()
  const kind = t || c

  if (kind) {
    if (['friend', 'apply', 'follow'].includes(kind)) {
      return '/friends'
    }
    if (['like', 'comment', 'share'].includes(kind)) {
      const id = notification.targetId
      return id ? `/social-feed?postId=${id}` : '/social-feed'
    }
    if (['elf', 'memory', 'spirit', 'ai_remind'].includes(kind)) {
      return '/chat'
    }
    if (['system', 'announcement', 'notice', 'admin'].includes(kind)) {
      return '/announcements'
    }
  }

  // 2) 历史 targetType 路由（保持向后兼容）
  const { targetType, targetId } = notification
  if (!targetType || targetId === undefined || targetId === null || targetId === '') {
    return null
  }
  switch (targetType) {
    case 'post':
      return `/post/${targetId}`
    case 'feed':
      return `/feed/${targetId}`
    case 'friend':
      return `/friend/${targetId}`
    case 'group':
      return `/group/${targetId}`
    default:
      return null
  }
}

export default {
  groupByDate,
  formatNotificationTime,
  getNotificationIcon,
  getActionText,
  canJump,
  getTargetRoute
}
