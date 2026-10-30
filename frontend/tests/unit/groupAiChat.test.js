import {
  roundRobinOrder,
  formatMemberList,
  formatAiReplies
} from '../../src/utils/groupAiChat'

describe('多 AI 群聊纯逻辑', () => {
  describe('roundRobinOrder 轮流顺序', () => {
    const members = [{ characterId: 1 }, { characterId: 2 }, { characterId: 3 }]

    it('游标 0 应保持原序', () => {
      expect(roundRobinOrder(members, 0).map((m) => m.characterId)).toEqual([1, 2, 3])
    })

    it('游标 1 应从下一个开始轮转', () => {
      expect(roundRobinOrder(members, 1).map((m) => m.characterId)).toEqual([2, 3, 1])
    })

    it('游标越界应取模回绕', () => {
      expect(roundRobinOrder(members, 4).map((m) => m.characterId)).toEqual([2, 3, 1])
    })

    it('空成员应返回空数组', () => {
      expect(roundRobinOrder([], 0)).toEqual([])
    })
  })

  describe('formatMemberList 区分人类与 AI', () => {
    it('AI 成员应加 🤖 标记', () => {
      const list = formatMemberList([
        { name: 'Alice', isAi: false },
        { name: '小助手', isAi: true }
      ])
      expect(list).toEqual(['Alice', '🤖 小助手'])
    })
  })

  describe('formatAiReplies 回复格式化', () => {
    it('应按角色名分组前缀', () => {
      const r = formatAiReplies([
        { characterName: 'Alice', content: '我认为…' },
        { characterName: 'Bob', content: '我同意…' }
      ])
      expect(r).toEqual(['【Alice】我认为…', '【Bob】我同意…'])
    })
  })
})
