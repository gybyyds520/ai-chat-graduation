import {
  PERSONALITY_TAGS,
  serializePersonalityTags,
  parsePersonalityTags,
  isValidPersonalityTags
} from '../../src/utils/characterCreate'

describe('角色创建纯逻辑', () => {
  describe('PERSONALITY_TAGS', () => {
    it('包含全部可选性格标签', () => {
      expect(PERSONALITY_TAGS).toEqual(
        expect.arrayContaining(['温柔', '傲娇', '活泼', '高冷', '治愈', '元气', '腹黑', '天然呆'])
      )
    })
  })

  describe('serialize / parse 互逆', () => {
    it('多选标签序列化为逗号分隔', () => {
      expect(serializePersonalityTags(['温柔', '傲娇'])).toBe('温柔,傲娇')
      expect(serializePersonalityTags([])).toBe('')
      expect(serializePersonalityTags(null)).toBe('')
    })
    it('反序列化去空去重', () => {
      expect(parsePersonalityTags('温柔,傲娇，温柔')).toEqual(['温柔', '傲娇'])
      expect(parsePersonalityTags('')).toEqual([])
    })
  })

  describe('isValidPersonalityTags', () => {
    it('合法标签通过', () => {
      expect(isValidPersonalityTags(['温柔', '治愈'])).toBe(true)
    })
    it('非法标签拒绝', () => {
      expect(isValidPersonalityTags(['温柔', '未知标签'])).toBe(false)
      expect(isValidPersonalityTags(null)).toBe(false)
    })
  })
})
