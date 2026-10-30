import {
  buildGreetingMessage,
  isSystemBubble,
  isAiBubble,
  generateQuickReplies,
  deletedMessagePlaceholder,
  typewriterVisibleCount,
  greetingSnippet
} from '../../src/utils/chatExperience'

describe('角色聊天体验纯逻辑', () => {
  describe('buildGreetingMessage', () => {
    it('有 greeting 时返回 AI 开场白消息', () => {
      const m = buildGreetingMessage({ name: '小助手', greeting: '你好呀，我是小助手～' })
      expect(m.senderType).toBe('ai')
      expect(m.content).toBe('你好呀，我是小助手～')
      expect(m.isGreeting).toBe(true)
    })
    it('无 greeting 或无角色时返回 null', () => {
      expect(buildGreetingMessage({ name: 'x' })).toBeNull()
      expect(buildGreetingMessage(null)).toBeNull()
      expect(buildGreetingMessage({ greeting: '   ' })).toBeNull()
    })
  })

  describe('气泡类型判定', () => {
    it('isSystemBubble 识别系统消息', () => {
      expect(isSystemBubble({ senderType: 'system' })).toBe(true)
      expect(isSystemBubble({ isSystem: true })).toBe(true)
      expect(isSystemBubble({ senderType: 'ai' })).toBe(false)
    })
    it('isAiBubble 识别 AI 消息', () => {
      expect(isAiBubble({ senderType: 'ai' })).toBe(true)
      expect(isAiBubble({ senderType: 'user' })).toBe(false)
    })
  })

  describe('generateQuickReplies', () => {
    it('始终返回 3 条建议', () => {
      expect(generateQuickReplies('温柔体贴')).toHaveLength(3)
      expect(generateQuickReplies('')).toHaveLength(3)
      expect(generateQuickReplies(null)).toHaveLength(3)
    })
    it('命中不同人设返回不同口吻', () => {
      expect(generateQuickReplies('温柔治愈')).toEqual(
        expect.arrayContaining(['你今天过得怎么样？'])
      )
      expect(generateQuickReplies('傲娇')).toEqual(
        expect.arrayContaining(['哼，才不是特意找你'])
      )
      expect(generateQuickReplies('高冷')).toEqual(expect.arrayContaining(['说重点']))
      expect(generateQuickReplies('活泼元气')).toEqual(
        expect.arrayContaining(['快带我去看看！'])
      )
    })
  })

  describe('deletedMessagePlaceholder', () => {
    it('返回固定占位文本', () => {
      expect(deletedMessagePlaceholder()).toBe('消息已删除')
    })
  })

  describe('typewriterVisibleCount', () => {
    it('按速率计算可见字符数，不超过全文', () => {
      expect(typewriterVisibleCount('abcdefgh', 1000, 8)).toBe(8)
      expect(typewriterVisibleCount('abcdefgh', 500, 8)).toBe(4)
      expect(typewriterVisibleCount('', 1000)).toBe(0)
      expect(typewriterVisibleCount('abc', 999999)).toBe(3)
    })
  })

  describe('greetingSnippet', () => {
    it('截取前 N 字并加省略号', () => {
      expect(greetingSnippet('你好呀我是角色，很高兴认识你，今天想聊点什么呢？', 10)).toBe(
        '你好呀我是角色，很高…'
      )
      expect(greetingSnippet('短', 10)).toBe('短')
      expect(greetingSnippet('', 10)).toBe('')
    })
  })
})
