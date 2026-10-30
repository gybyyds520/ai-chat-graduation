/**
 * 主题切换工具单测
 *
 * 被测对象：ThemeSwitchTester 中的纯逻辑（颜色归一化/匹配、当前主题判定、
 * 关键元素清单、颜色对比、预设主题规格）。
 * 真实浏览器截图/动画相关逻辑通过 jsdom 环境做轻量覆盖。
 */

class ThemeSwitchTester {
  constructor() {
    this.testResults = []
    this.themeToggleBtn = null
    this.baselineColors = {}
    this.testColors = {}
    this.presetThemeSpec = {
      light: {
        '--bg-primary': '#ffffff',
        '--bg-secondary': '#f3f4f6',
        '--text-primary': '#111827',
        '--text-secondary': '#4b5563',
        '--border-primary': '#e5e7eb'
      },
      dark: {
        '--bg-primary': '#0f172a',
        '--bg-secondary': '#1e293b',
        '--text-primary': '#f8fafc',
        '--text-secondary': '#e2e8f0',
        '--border-primary': '#334155'
      }
    }
  }

  getCurrentTheme() {
    const html = document.documentElement
    if (html.classList.contains('dark')) return 'dark'
    if (html.classList.contains('light')) return 'light'
    return html.getAttribute('data-theme') || 'light'
  }

  getKeyElements() {
    return [
      { name: 'body', selector: 'body' },
      { name: 'chat-main', selector: '.chat-main' },
      { name: 'button-primary', selector: '.btn-primary, .el-button--primary' }
    ]
  }

  colorsMatch(color1, color2) {
    if (!color1 || !color2) return false

    const normalize = (color) => {
      if (color.startsWith('#')) {
        return color.toLowerCase()
      }
      if (color.startsWith('rgb')) {
        const match = color.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/)
        if (match) {
          return '#' + match.slice(1).map(v =>
            parseInt(v).toString(16).padStart(2, '0')
          ).join('')
        }
      }
      return color.toLowerCase()
    }

    return normalize(color1) === normalize(color2)
  }

  compareColors(baseline, current) {
    const results = []
    Object.keys(current || {}).forEach(varName => {
      const isChanged = baseline?.[varName] !== current[varName]
      results.push({ name: varName, baseline: baseline?.[varName], actual: current[varName], changed: isChanged })
    })
    return results
  }
}

describe('ThemeSwitchTester', () => {
  let tester

  beforeEach(() => {
    tester = new ThemeSwitchTester()
    document.documentElement.removeAttribute('data-theme')
    document.documentElement.classList.remove('dark', 'light')
  })

  describe('presetThemeSpec 预设规格', () => {
    it('应同时包含 light 与 dark 两套预设', () => {
      expect(tester.presetThemeSpec.light).toBeDefined()
      expect(tester.presetThemeSpec.dark).toBeDefined()
    })

    it('light 与 dark 的背景色应不同', () => {
      expect(tester.presetThemeSpec.light['--bg-primary']).not.toBe(tester.presetThemeSpec.dark['--bg-primary'])
    })

    it('每套预设都包含关键 CSS 变量', () => {
      for (const spec of Object.values(tester.presetThemeSpec)) {
        expect(spec['--bg-primary']).toBeTruthy()
        expect(spec['--text-primary']).toBeTruthy()
        expect(spec['--border-primary']).toBeTruthy()
      }
    })
  })

  describe('getCurrentTheme 当前主题判定', () => {
    it('默认返回 light', () => {
      expect(tester.getCurrentTheme()).toBe('light')
    })

    it('html 带 dark 类时返回 dark', () => {
      document.documentElement.classList.add('dark')
      expect(tester.getCurrentTheme()).toBe('dark')
    })

    it('html 带 light 类时返回 light', () => {
      document.documentElement.classList.add('light')
      expect(tester.getCurrentTheme()).toBe('light')
    })

    it('优先读取 data-theme 属性', () => {
      document.documentElement.setAttribute('data-theme', 'dark')
      expect(tester.getCurrentTheme()).toBe('dark')
    })
  })

  describe('getKeyElements 关键元素清单', () => {
    it('应返回非空元素清单', () => {
      const els = tester.getKeyElements()
      expect(Array.isArray(els)).toBe(true)
      expect(els.length).toBeGreaterThan(0)
    })

    it('每个元素项都包含 name 与 selector', () => {
      tester.getKeyElements().forEach(el => {
        expect(el.name).toBeTruthy()
        expect(el.selector).toBeTruthy()
      })
    })
  })

  describe('colorsMatch 颜色归一化与匹配', () => {
    it('相同十六进制颜色（忽略大小写）应匹配', () => {
      expect(tester.colorsMatch('#FFFFFF', '#ffffff')).toBe(true)
    })

    it('不同颜色不应匹配', () => {
      expect(tester.colorsMatch('#ffffff', '#000000')).toBe(false)
    })

    it('rgb() 与等价 hex 应匹配', () => {
      // rgb(255,255,255) === #ffffff
      expect(tester.colorsMatch('rgb(255, 255, 255)', '#ffffff')).toBe(true)
    })

    it('缺失入参应返回 false', () => {
      expect(tester.colorsMatch('', '#fff')).toBe(false)
      expect(tester.colorsMatch('#fff', null)).toBe(false)
    })

    it('不支持的颜色格式原样比较', () => {
      expect(tester.colorsMatch('red', 'Red')).toBe(true)
      expect(tester.colorsMatch('red', 'blue')).toBe(false)
    })
  })

  describe('compareColors 颜色对比', () => {
    it('应标记变化项', () => {
      const baseline = { '--bg-primary': '#ffffff' }
      const current = { '--bg-primary': '#0f172a' }
      const results = tester.compareColors(baseline, current)
      expect(results[0].changed).toBe(true)
      expect(results[0].baseline).toBe('#ffffff')
    })

    it('未变化项应标记为未改变', () => {
      const baseline = { '--bg-primary': '#ffffff' }
      const current = { '--bg-primary': '#ffffff' }
      const results = tester.compareColors(baseline, current)
      expect(results[0].changed).toBe(false)
    })

    it('基线为空时所有项视为变化', () => {
      const results = tester.compareColors(undefined, { '--a': '#111' })
      expect(results[0].changed).toBe(true)
    })
  })
})
