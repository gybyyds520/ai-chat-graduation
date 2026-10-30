import {
  runFullValidation,
  switchThemeAndValidate,
  getValidationHistory
} from '../../src/utils/themeValidator'
import themeValidator from '../../src/utils/themeValidator'

const { validateThemeSwitch } = themeValidator

describe('themeValidator 主题校验', () => {
  beforeEach(() => {
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.classList.remove('dark', 'light')
    localStorage.removeItem('theme')
  })

  it('runFullValidation 应返回结构化结果', () => {
    const report = runFullValidation()
    expect(report).toBeDefined()
    expect(report.results).toBeDefined()
    expect(report.results.cssVariables).toBeDefined()
    expect(report.results.themeSwitch).toBeDefined()
    expect(typeof report.passed).toBe('number')
    expect(typeof report.total).toBe('number')
  })

  it('CSS 变量校验项数量应稳定', () => {
    const { results } = runFullValidation()
    // 20 个必需变量
    expect(results.cssVariables.length).toBe(20)
  })

  it('getValidationHistory 应返回数组', () => {
    expect(Array.isArray(getValidationHistory())).toBe(true)
  })

  it('校验历史应记录本次结果', () => {
    runFullValidation()
    expect(getValidationHistory().length).toBeGreaterThan(0)
  })

  describe('switchThemeAndValidate', () => {
    it('切到 dark 应设置 data-theme 与 dark 类', () => {
      switchThemeAndValidate('dark')
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
      expect(document.documentElement.classList.contains('dark')).toBe(true)
      expect(localStorage.getItem('theme')).toBe('dark')
    })

    it('切到 light 应移除 dark 类', () => {
      switchThemeAndValidate('dark')
      switchThemeAndValidate('light')
      expect(document.documentElement.getAttribute('data-theme')).toBe('light')
      expect(document.documentElement.classList.contains('dark')).toBe(false)
      expect(document.documentElement.classList.contains('light')).toBe(true)
    })
  })

  describe('validateThemeSwitch', () => {
    it('未设置主题时 data-theme 项应标记无效', () => {
      const results = validateThemeSwitch()
      const dataThemeCheck = results.find(r => r.check === 'data-theme 属性')
      expect(dataThemeCheck.isValid).toBe(false)
    })

    it('设置 dark 后 data-theme 项应有效', () => {
      document.documentElement.setAttribute('data-theme', 'dark')
      localStorage.setItem('theme', 'dark')
      const results = validateThemeSwitch()
      const dataThemeCheck = results.find(r => r.check === 'data-theme 属性')
      expect(dataThemeCheck.isValid).toBe(true)
    })
  })
})
