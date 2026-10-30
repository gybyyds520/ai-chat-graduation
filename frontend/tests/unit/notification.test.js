import {
  groupByDate,
  formatNotificationTime,
  getNotificationIcon,
  getActionText,
  canJump,
  getTargetRoute
} from '../../src/utils/notification'

// 固定参考时间：2026-09-14 12:00:00（本地时区）
const NOW = new Date(2026, 8, 14, 12, 0, 0)

function makeNotification (createdAt, extra = {}) {
  return { id: Math.floor(Math.random() * 1e6), createdAt, ...extra }
}

describe('notification 通知中心纯逻辑工具', () => {
  describe('groupByDate 时间分组', () => {
    it('今天的通知归入「今天」组', () => {
      const list = [
        makeNotification('2026-09-14T08:30:00'),
        makeNotification('2026-09-14T11:00:00')
      ]
      const groups = groupByDate(list, NOW)
      expect(groups.length).toBe(1)
      expect(groups[0].label).toBe('今天')
      expect(groups[0].items.length).toBe(2)
    })

    it('昨天的通知归入「昨天」组', () => {
      const list = [
        makeNotification('2026-09-13T18:45:00'),
        makeNotification('2026-09-13T00:30:00')
      ]
      const groups = groupByDate(list, NOW)
      expect(groups.length).toBe(1)
      expect(groups[0].label).toBe('昨天')
      expect(groups[0].items.length).toBe(2)
    })

    it('更早的通知归入「更早」组，且按 今天→昨天→更早 顺序排列', () => {
      const list = [
        makeNotification('2026-08-20T10:00:00'),
        makeNotification('2026-09-13T09:00:00'),
        makeNotification('2026-09-14T09:00:00')
      ]
      const groups = groupByDate(list, NOW)
      expect(groups.map(g => g.label)).toEqual(['今天', '昨天', '更早'])
      expect(groups[2].items.length).toBe(1)
    })

    it('空数组或非数组返回空数组', () => {
      expect(groupByDate([], NOW)).toEqual([])
      expect(groupByDate(null, NOW)).toEqual([])
      expect(groupByDate(undefined, NOW)).toEqual([])
    })

    it('无 createdAt 的通知被跳过', () => {
      const list = [makeNotification(null), makeNotification('2026-09-14T10:00:00')]
      const groups = groupByDate(list, NOW)
      expect(groups.length).toBe(1)
      expect(groups[0].items.length).toBe(1)
    })
  })

  describe('formatNotificationTime 友好时间', () => {
    it('今天显示 HH:mm', () => {
      expect(formatNotificationTime('2026-09-14T08:05:00', NOW)).toBe('08:05')
    })

    it('昨天显示 昨天 HH:mm', () => {
      expect(formatNotificationTime('2026-09-13T18:45:00', NOW)).toBe('昨天 18:45')
    })

    it('今年更早显示 MM-DD', () => {
      expect(formatNotificationTime('2026-08-20T10:00:00', NOW)).toBe('08-20')
    })

    it('跨年显示 YYYY-MM-DD', () => {
      expect(formatNotificationTime('2025-12-01T10:00:00', NOW)).toBe('2025-12-01')
    })

    it('空值或非法日期返回空串', () => {
      expect(formatNotificationTime(null, NOW)).toBe('')
      expect(formatNotificationTime('not-a-date', NOW)).toBe('')
    })
  })

  describe('getNotificationIcon 图标', () => {
    it('like/comment/share/system 各返回正确图标', () => {
      expect(getNotificationIcon('like')).toBe('👍')
      expect(getNotificationIcon('comment')).toBe('💬')
      expect(getNotificationIcon('share')).toBe('🔄')
      expect(getNotificationIcon('system')).toBe('📢')
    })

    it('未知类型返回默认图标', () => {
      expect(getNotificationIcon('unknown')).toBe('🔔')
      expect(getNotificationIcon(undefined)).toBe('🔔')
    })
  })

  describe('getActionText 动作文字', () => {
    it('like/comment/share/system 返回对应文字', () => {
      expect(getActionText('like')).toBe('赞了你的')
      expect(getActionText('comment')).toBe('评论了你的')
      expect(getActionText('share')).toBe('分享了你的')
      expect(getActionText('system')).toBe('系统通知')
    })

    it('未知类型返回空串', () => {
      expect(getActionText('unknown')).toBe('')
    })
  })

  describe('canJump 是否支持跳转', () => {
    it('post/feed/friend/group 返回 true', () => {
      expect(canJump('post')).toBe(true)
      expect(canJump('feed')).toBe(true)
      expect(canJump('friend')).toBe(true)
      expect(canJump('group')).toBe(true)
    })

    it('其他类型返回 false', () => {
      expect(canJump('system')).toBe(false)
      expect(canJump('unknown')).toBe(false)
      expect(canJump(null)).toBe(false)
    })
  })

  describe('getTargetRoute 跳转路由', () => {
    it('post 返回 /post/{id}', () => {
      expect(getTargetRoute({ targetType: 'post', targetId: 123 })).toBe('/post/123')
    })

    it('friend 返回 /friend/{id}', () => {
      expect(getTargetRoute({ targetType: 'friend', targetId: 456 })).toBe('/friend/456')
    })

    it('feed/group 返回对应路由', () => {
      expect(getTargetRoute({ targetType: 'feed', targetId: 7 })).toBe('/feed/7')
      expect(getTargetRoute({ targetType: 'group', targetId: 9 })).toBe('/group/9')
    })

    it('未知类型或缺少 targetId 返回 null', () => {
      expect(getTargetRoute({ targetType: 'system', targetId: 1 })).toBeNull()
      expect(getTargetRoute({ targetType: 'post' })).toBeNull()
      expect(getTargetRoute({})).toBeNull()
      expect(getTargetRoute(null)).toBeNull()
    })
  })

  describe('M4 ⑭ 按通知类型路由跳转', () => {
    it('好友申请/申请/关注 → /friends', () => {
      expect(getTargetRoute({ type: 'friend' })).toBe('/friends')
      expect(getTargetRoute({ type: 'apply' })).toBe('/friends')
      expect(getTargetRoute({ type: 'follow' })).toBe('/friends')
    })

    it('点赞/评论/分享 → 社区帖子详情（带 postId）', () => {
      expect(getTargetRoute({ type: 'like', targetId: 123 })).toBe('/social-feed?postId=123')
      expect(getTargetRoute({ type: 'comment', targetId: 7 })).toBe('/social-feed?postId=7')
      expect(getTargetRoute({ type: 'share' })).toBe('/social-feed')
    })

    it('AI 提醒 → /chat', () => {
      expect(getTargetRoute({ type: 'elf' })).toBe('/chat')
      expect(getTargetRoute({ type: 'ai_remind' })).toBe('/chat')
      expect(getTargetRoute({ category: 'memory' })).toBe('/chat')
    })

    it('系统公告 → /announcements', () => {
      expect(getTargetRoute({ type: 'announcement' })).toBe('/announcements')
      expect(getTargetRoute({ type: 'system' })).toBe('/announcements')
    })

    it('canJump 传入通知对象时按路由是否存在判断', () => {
      expect(canJump({ type: 'friend' })).toBe(true)
      expect(canJump({ type: 'like', targetId: 1 })).toBe(true)
      expect(canJump({ type: 'elf' })).toBe(true)
      expect(canJump({ type: 'announcement' })).toBe(true)
    })

    it('历史 targetType 路由行为保持不变', () => {
      expect(getTargetRoute({ targetType: 'post', targetId: 123 })).toBe('/post/123')
      expect(getTargetRoute({ targetType: 'system', targetId: 1 })).toBeNull()
      expect(canJump('post')).toBe(true)
      expect(canJump('system')).toBe(false)
    })
  })
})
