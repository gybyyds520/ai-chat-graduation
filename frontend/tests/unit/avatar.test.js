import {
  generateAvatarUrl,
  getAvatarLetter,
  hashCode,
  getAvatarColors,
  colors,
  validateImageFile,
  AVATAR_MAX_SIZE_MB
} from '../../src/utils/avatar'

describe('avatar 头像工具', () => {
  describe('generateAvatarUrl', () => {
    it('应返回 LocalAvatar 组件描述对象', () => {
      const result = generateAvatarUrl('seed-abc', 80)
      expect(result.component).toBe('LocalAvatar')
      expect(result.props.seed).toBe('seed-abc')
      expect(result.props.size).toBe(80)
    })

    it('size 缺省时应为 40', () => {
      const result = generateAvatarUrl('seed-x')
      expect(result.props.size).toBe(40)
    })

    it('边界：seed 为空字符串', () => {
      const result = generateAvatarUrl('', 10)
      expect(result.props.seed).toBe('')
      expect(result.props.size).toBe(10)
    })
  })

  describe('getAvatarLetter', () => {
    it('应取清理后最后一个字母并大写', () => {
      expect(getAvatarLetter('alice')).toBe('E')
    })

    it('应支持中文单字', () => {
      expect(getAvatarLetter('张三')).toBe('三')
    })

    it('纯符号应返回 ?', () => {
      expect(getAvatarLetter('!!!@@@')).toBe('?')
    })

    it('空值应返回 ?', () => {
      expect(getAvatarLetter('')).toBe('?')
      expect(getAvatarLetter(null)).toBe('?')
      expect(getAvatarLetter(undefined)).toBe('?')
    })

    it('应过滤符号后取有效字符', () => {
      expect(getAvatarLetter('user@name')).toBe('E') // 去掉 @ 后为 username，最后 e
    })
  })

  describe('hashCode', () => {
    it('相同字符串应返回相同哈希', () => {
      expect(hashCode('abc')).toBe(hashCode('abc'))
    })

    it('不同字符串应返回不同哈希', () => {
      expect(hashCode('abc')).not.toBe(hashCode('abd'))
    })

    it('空字符串应返回 0', () => {
      expect(hashCode('')).toBe(0)
    })
  })

  describe('getAvatarColors', () => {
    it('应返回二元色组', () => {
      const pair = getAvatarColors('someone')
      expect(Array.isArray(pair)).toBe(true)
      expect(pair.length).toBe(2)
    })

    it('相同 seed 应返回相同色组', () => {
      expect(getAvatarColors('seed')).toEqual(getAvatarColors('seed'))
    })

    it('返回值应来自预设色板', () => {
      const pair = getAvatarColors('whatever')
      const flat = colors.flat()
      expect(flat).toContain(pair[0])
      expect(flat).toContain(pair[1])
    })

    it('边界：空 seed 不应抛错（随机兜底）', () => {
      expect(() => getAvatarColors('')).not.toThrow()
      expect(() => getAvatarColors(null)).not.toThrow()
    })
  })
})

  describe('validateImageFile 图片上传校验', () => {
    it('空文件提示选择文件', () => {
      expect(validateImageFile(null)).toMatchObject({ valid: false })
    })
    it('非图片类型被拒绝', () => {
      const r = validateImageFile({ type: 'application/pdf', size: 1024 })
      expect(r.valid).toBe(false)
      expect(r.message).toContain('图片')
    })
    it('超过体积上限被拒绝', () => {
      const big = { type: 'image/png', size: (AVATAR_MAX_SIZE_MB + 1) * 1024 * 1024 }
      expect(validateImageFile(big).valid).toBe(false)
    })
    it('合法小图片通过', () => {
      const ok = { type: 'image/jpeg', size: 1 * 1024 * 1024 }
      expect(validateImageFile(ok)).toMatchObject({ valid: true, message: '' })
    })
    it('自定义 label 出现在超限文案中', () => {
      const big = { type: 'image/gif', size: 9 * 1024 * 1024 }
      const r = validateImageFile(big, { label: '头像图片' })
      expect(r.message).toContain('头像图片')
    })
  })