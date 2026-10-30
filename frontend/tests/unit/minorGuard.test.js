import {
  ADULT_AGE,
  classifyDeclaredAge,
  isNightRestrictedHour,
  isNowNightRestricted,
  describeMinorModeStatus
} from '../../src/utils/minorGuard'

describe('minorGuard 未成年保护纯逻辑', () => {
  describe('classifyDeclaredAge 年龄声明分类', () => {
    it('未填写年龄视为不声明，按成年处理', () => {
      expect(classifyDeclaredAge(undefined)).toMatchObject({ age: null, valid: true, isMinor: false })
      expect(classifyDeclaredAge(null)).toMatchObject({ age: null, valid: true, isMinor: false })
      expect(classifyDeclaredAge('')).toMatchObject({ age: null, valid: true, isMinor: false })
    })

    it('0 视为未声明', () => {
      expect(classifyDeclaredAge(0)).toMatchObject({ age: null, valid: true, isMinor: false })
    })

    it('1-17 判定为未成年人并给出提示', () => {
      const r = classifyDeclaredAge(12)
      expect(r.valid).toBe(true)
      expect(r.isMinor).toBe(true)
      expect(r.message).toContain('未成年保护')
    })

    it('恰好 ADULT_AGE(18) 为成年人', () => {
      expect(classifyDeclaredAge(ADULT_AGE)).toMatchObject({ age: 18, isMinor: false })
    })

    it('成年用户无提示文案', () => {
      expect(classifyDeclaredAge(30).message).toBe('')
    })

    it('负数 / 超界年龄校验失败', () => {
      expect(classifyDeclaredAge(-1).valid).toBe(false)
      expect(classifyDeclaredAge(121).valid).toBe(false)
    })

    it('非整数 / 非数字校验失败', () => {
      expect(classifyDeclaredAge('abc').valid).toBe(false)
      expect(classifyDeclaredAge(12.5).valid).toBe(false)
    })

    it('字符串数字可被解析', () => {
      expect(classifyDeclaredAge('10')).toMatchObject({ age: 10, isMinor: true })
    })
  })

  describe('isNightRestrictedHour 夜间窗口', () => {
    it('22:00-23:59 属于夜间窗口', () => {
      expect(isNightRestrictedHour(22)).toBe(true)
      expect(isNightRestrictedHour(23)).toBe(true)
    })

    it('00:00-05:59 属于夜间窗口', () => {
      expect(isNightRestrictedHour(0)).toBe(true)
      expect(isNightRestrictedHour(5)).toBe(true)
    })

    it('06:00-21:59 不属于夜间窗口', () => {
      expect(isNightRestrictedHour(6)).toBe(false)
      expect(isNightRestrictedHour(12)).toBe(false)
      expect(isNightRestrictedHour(21)).toBe(false)
    })

    it('非法输入返回 false', () => {
      expect(isNightRestrictedHour(-1)).toBe(false)
      expect(isNightRestrictedHour(24)).toBe(false)
      expect(isNightRestrictedHour('x')).toBe(false)
    })
  })

  describe('isNowNightRestricted', () => {
    it('根据给定 Date 的小时判定', () => {
      expect(isNowNightRestricted(new Date(2026, 8, 24, 23, 0))).toBe(true)
      expect(isNowNightRestricted(new Date(2026, 8, 24, 14, 0))).toBe(false)
    })
  })

  describe('describeMinorModeStatus', () => {
    it('未开启返回未开启文案', () => {
      expect(describeMinorModeStatus({ minorModeActive: false }).active).toBe(false)
      expect(describeMinorModeStatus({}).active).toBe(false)
    })

    it('夜间受限给出夜间提示', () => {
      const r = describeMinorModeStatus({ minorModeActive: true, nightRestricted: true })
      expect(r.active).toBe(true)
      expect(r.text).toContain('夜间')
    })

    it('白天开启给出过滤提示', () => {
      const r = describeMinorModeStatus({ minorModeActive: true, nightRestricted: false })
      expect(r.active).toBe(true)
      expect(r.text).toContain('敏感')
    })
  })
})
