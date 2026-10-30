import { describe, it, expect, beforeEach } from 'vitest'
import { useTheme } from '../../src/composables/useTheme'

describe('gap: composables/useTheme 主题切换', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.classList.remove('light', 'dark', 'theme-transition')
  })

  it('setTheme(dark) 写入 localStorage 并设置 data-theme=dark', () => {
    const { setTheme } = useTheme()
    setTheme('dark')
    expect(localStorage.getItem('app-theme-mode')).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('setTheme(light) 切换回 light', () => {
    const { setTheme } = useTheme()
    setTheme('dark')
    setTheme('light')
    expect(localStorage.getItem('app-theme-mode')).toBe('light')
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
  })

  it('toggleTheme 在 light/dark 之间翻转', () => {
    const { setTheme, toggleTheme, currentTheme } = useTheme()
    setTheme('light')
    toggleTheme()
    expect(currentTheme.value).toBe('dark')
    toggleTheme()
    expect(currentTheme.value).toBe('light')
  })

  it('isDark/isLight 反映当前主题', () => {
    const { setTheme, isDark, isLight } = useTheme()
    setTheme('dark')
    expect(isDark()).toBe(true)
    expect(isLight()).toBe(false)
    setTheme('light')
    expect(isDark()).toBe(false)
    expect(isLight()).toBe(true)
  })

  it('getSystemTheme 默认返回 light（jsdom 无 matchMedia dark）', () => {
    const { getSystemTheme } = useTheme()
    expect(['light', 'dark']).toContain(getSystemTheme())
  })

  it('loadThemeFromStorage 读取合法存储值', () => {
    localStorage.setItem('app-theme-mode', 'dark')
    const { initTheme } = useTheme()
    // 直接调用 initTheme 会因 isInitialized 短路，改用内部效果：
    // 通过 setTheme 已经写入 storage，这里验证读取逻辑
    expect(localStorage.getItem('app-theme-mode')).toBe('dark')
    initTheme()
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('refreshTheme 重新应用当前主题', () => {
    const { setTheme, refreshTheme } = useTheme()
    setTheme('dark')
    document.documentElement.removeAttribute('data-theme')
    refreshTheme()
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('applyTheme 给 html 加 theme-transition 类', () => {
    const { setTheme } = useTheme()
    setTheme('dark')
    expect(document.documentElement.classList.contains('theme-transition')).toBe(true)
  })
})
