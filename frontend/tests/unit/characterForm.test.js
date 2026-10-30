import {
  parseTagInput,
  validateCharacterForm,
  fallbackGreeting
} from '../../src/utils/characterForm'

describe('角色表单纯逻辑', () => {
  describe('parseTagInput', () => {
    it('应支持中英文逗号/顿号/空格分隔并去重', () => {
      expect(parseTagInput('治愈, 陪伴，日常、治愈')).toEqual(['治愈', '陪伴', '日常'])
    })

    it('应限制最多标签数', () => {
      const input = 'a,b,c,d,e,f,g,h,i,j'
      expect(parseTagInput(input, 8)).toHaveLength(8)
    })

    it('空输入应返回空数组', () => {
      expect(parseTagInput('')).toEqual([])
      expect(parseTagInput(null)).toEqual([])
    })
  })

  describe('validateCharacterForm', () => {
    it('名称过短应报错', () => {
      expect(validateCharacterForm({ name: 'a', prompt: '这是一段足够长的角色描述内容' }))
        .toContain('名称')
    })

    it('描述过短应报错', () => {
      expect(validateCharacterForm({ name: '导师', prompt: '太短' })).toContain('描述')
    })

    it('合法表单应返回空串', () => {
      expect(validateCharacterForm({ name: '导师', prompt: '这是一段足够长的角色描述内容' })).toBe('')
    })
  })

  describe('fallbackGreeting', () => {
    it('有开场白应原样返回', () => {
      expect(fallbackGreeting('  你好呀  ', 'X')).toBe('你好呀')
    })
    it('空开场白应回退默认语', () => {
      expect(fallbackGreeting('', '小明')).toContain('小明')
      expect(fallbackGreeting(null, '小明')).toContain('小明')
    })
  })
})
