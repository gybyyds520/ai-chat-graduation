import {
  TABS,
  categorize,
  groupByTab,
  unreadCounts,
  isUnread,
  markAllRead,
  filterByTab
} from '../../src/utils/notificationCategorize'

describe('notificationCategorize 通知分类页签', () => {
  it('TABS 含互动/系统/AI 三类', () => {
    expect(TABS.map(t => t.key)).toEqual(['interaction', 'system', 'ai'])
  })

  it('category=interaction 归互动', () => {
    expect(categorize({ category: 'interaction' })).toBe('interaction')
  })

  it('type=like/comment 归互动', () => {
    expect(categorize({ type: 'like' })).toBe('interaction')
    expect(categorize({ type: 'comment' })).toBe('interaction')
    expect(categorize({ type: 'friend' })).toBe('interaction')
  })

  it('type=elf/memory 归 AI', () => {
    expect(categorize({ type: 'elf' })).toBe('ai')
    expect(categorize({ category: 'memory' })).toBe('ai')
  })

  it('type=announcement 归系统', () => {
    expect(categorize({ type: 'announcement' })).toBe('system')
    expect(categorize({ category: 'system' })).toBe('system')
  })

  it('未知类型兜底系统', () => {
    expect(categorize({ type: 'weird' })).toBe('system')
    expect(categorize(null)).toBe('system')
  })

  it('groupByTab 分组', () => {
    const list = [
      { type: 'like' },
      { type: 'comment' },
      { type: 'announcement' },
      { type: 'elf' }
    ]
    const g = groupByTab(list)
    expect(g.interaction.length).toBe(2)
    expect(g.system.length).toBe(1)
    expect(g.ai.length).toBe(1)
  })

  it('isUnread 多种字段判定', () => {
    expect(isUnread({ read: false })).toBe(true)
    expect(isUnread({ readStatus: 0 })).toBe(true)
    expect(isUnread({ isRead: false })).toBe(true)
    expect(isUnread({ read: true })).toBe(false)
    expect(isUnread({})).toBe(false)
  })

  it('unreadCounts 统计总数与分类', () => {
    const list = [
      { type: 'like', read: false },
      { type: 'like', read: true },
      { type: 'elf', read: false },
      { type: 'announcement', read: false }
    ]
    const c = unreadCounts(list)
    expect(c.total).toBe(3)
    expect(c.interaction).toBe(1)
    expect(c.ai).toBe(1)
    expect(c.system).toBe(1)
  })

  it('markAllRead 全部置已读且不变更入参', () => {
    const list = [{ type: 'like', read: false }, { type: 'elf', readStatus: 0 }]
    const next = markAllRead(list)
    expect(next[0].read).toBe(true)
    expect(next[1].readStatus).toBe(1)
    expect(list[0].read).toBe(false)
  })

  it('filterByTab 过滤指定页签', () => {
    const list = [{ type: 'like' }, { type: 'announcement' }, { type: 'elf' }]
    expect(filterByTab(list, 'ai').length).toBe(1)
    expect(filterByTab(list, 'system').length).toBe(1)
  })
})
