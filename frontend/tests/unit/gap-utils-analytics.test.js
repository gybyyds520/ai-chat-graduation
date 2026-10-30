import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import analytics, { EVENT_TYPES, EVENT_CATEGORIES } from '../../src/utils/analytics'

describe('gap: utils/analytics 埋点统计（回归 getStats）', () => {
  beforeEach(() => {
    // 清空历史事件，避免单例 trackAppStart 污染统计
    analytics.stopFlushTimer()
    analytics.events = []
    localStorage.clear()
  })
  afterEach(() => {
    analytics.startFlushTimer()
  })

  it('EVENT_CATEGORIES 是对象，所有 category key 都有值', () => {
    expect(typeof EVENT_CATEGORIES).toBe('object')
    expect(EVENT_CATEGORIES.WIDGET).toBe('widget')
    expect(EVENT_CATEGORIES.ERROR).toBe('error')
  })

  it('track() 写入事件并按 category 分类', () => {
    analytics.track(EVENT_TYPES.WIDGET_ADD, { type: 'clock' })
    analytics.track(EVENT_TYPES.MUSIC_PLAY, { name: 'song' })
    analytics.track(EVENT_TYPES.ELF_CLICK)
    expect(analytics.events.length).toBe(3)
    expect(analytics.events[0].category).toBe('widget')
    expect(analytics.events[1].category).toBe('music')
    expect(analytics.events[2].category).toBe('interaction')
  })

  it('回归：getStats() 不抛 TypeError 且 byCategory 统计正确', () => {
    // 修复前：EVENT_CATEGORIES.forEach 会抛 "forEach is not a function"
    analytics.track(EVENT_TYPES.WIDGET_ADD)
    analytics.track(EVENT_TYPES.WIDGET_REMOVE)
    analytics.track(EVENT_TYPES.MUSIC_PLAY)
    analytics.track(EVENT_TYPES.ERROR_OCCURRED, { message: 'x' })

    let stats
    expect(() => { stats = analytics.getStats() }).not.toThrow()
    expect(stats.totalEvents).toBe(4)
    expect(stats.byCategory.widget).toBe(2)
    expect(stats.byCategory.music).toBe(1)
    expect(stats.byCategory.error).toBe(1)
    // 未出现的 category 计数应为 0
    expect(stats.byCategory.alarm).toBe(0)
    expect(stats.byCategory.performance).toBe(0)
  })

  it('getStats().recentEvents 返回最后 10 条', () => {
    for (let i = 0; i < 15; i++) analytics.track(EVENT_TYPES.ELF_CLICK)
    const stats = analytics.getStats()
    expect(stats.recentEvents.length).toBe(10)
  })

  it('setEnabled(false) 后 track 不再记录', () => {
    analytics.setEnabled(false)
    analytics.track(EVENT_TYPES.ELF_CLICK)
    expect(analytics.events.length).toBe(0)
    analytics.setEnabled(true)
  })

  it('getCategory 对未知前缀归入 interaction', () => {
    analytics.track('some_unknown_event', {})
    expect(analytics.events[0].category).toBe('interaction')
  })
})
