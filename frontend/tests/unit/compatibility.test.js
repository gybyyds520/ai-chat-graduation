import {
  getBrowserInfo,
  detectDevice,
  hasTouchSupport,
  supportsCSSProperty,
  fixArrayIncludes,
  fixObjectAssign,
  initCompatibility
} from '../../src/utils/compatibility'

const originalUA = navigator.userAgent

function setUA(ua) {
  Object.defineProperty(navigator, 'userAgent', { value: ua, configurable: true, writable: true })
}

afterAll(() => {
  setUA(originalUA)
})

describe('compatibility 浏览器兼容检测', () => {
  describe('getBrowserInfo 浏览器识别', () => {
    it('应识别 Chrome', () => {
      setUA('Mozilla/5.0 (Windows NT 10.0) Chrome/120.0.0.0 Safari/537.36')
      const info = getBrowserInfo()
      expect(info.browser).toBe('chrome')
      expect(info.version).toBe('120')
      expect(info.os).toBe('windows')
    })

    it('应识别 Firefox', () => {
      setUA('Mozilla/5.0 (X11; Linux x86_64) Firefox/121.0')
      const info = getBrowserInfo()
      expect(info.browser).toBe('firefox')
      expect(info.version).toBe('121')
      expect(info.os).toBe('linux')
    })

    it('应识别 Safari（排除 Chrome）', () => {
      setUA('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) Version/17.0 Safari/605.1.15')
      const info = getBrowserInfo()
      expect(info.browser).toBe('safari')
      expect(info.version).toBe('17')
      expect(info.os).toBe('macos')
    })

    it('应识别 Edge', () => {
      setUA('Mozilla/5.0 (Windows NT 10.0) Chrome/120.0.0.0 Edg/120.0.0.0')
      const info = getBrowserInfo()
      expect(info.browser).toBe('edge')
    })

    it('应识别 iOS', () => {
      setUA('Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)')
      const info = getBrowserInfo()
      expect(info.os).toBe('ios')
    })

    it('未知 UA 应兜底为 unknown', () => {
      setUA('SomeStrangeBrowser/1.0')
      const info = getBrowserInfo()
      expect(info.browser).toBe('unknown')
      expect(info.userAgent).toBe('SomeStrangeBrowser/1.0')
    })
  })

  describe('detectDevice 设备类型', () => {
    it('桌面 UA 应判定为 desktop', () => {
      setUA('Mozilla/5.0 (Windows NT 10.0) Chrome/120.0')
      const d = detectDevice()
      expect(d.isDesktop).toBe(true)
      expect(d.isMobile).toBe(false)
    })

    it('Android UA 应判定为 mobile', () => {
      setUA('Mozilla/5.0 (Linux; Android 14) Mobile')
      const d = detectDevice()
      expect(d.isMobile).toBe(true)
      expect(d.isDesktop).toBe(false)
    })

    it('iPad UA 应判定为 tablet', () => {
      setUA('Mozilla/5.0 (iPad; CPU OS 17_0) Tablet')
      const d = detectDevice()
      expect(d.isTablet).toBe(true)
    })
  })

  describe('hasTouchSupport', () => {
    it('应返回布尔值', () => {
      expect(typeof hasTouchSupport()).toBe('boolean')
    })
  })

  describe('supportsCSSProperty', () => {
    it('标准属性应返回 true', () => {
      expect(supportsCSSProperty('display')).toBe(true)
      expect(supportsCSSProperty('color')).toBe(true)
    })

    it('不存在的属性应返回 false', () => {
      expect(supportsCSSProperty('nonexistentPropXYZ')).toBe(false)
    })
  })

  describe('polyfill 修复', () => {
    it('fixArrayIncludes 在已存在时不应抛错', () => {
      expect(() => fixArrayIncludes()).not.toThrow()
      expect([1, 2, 3].includes(2)).toBe(true)
    })

    it('fixObjectAssign 在已存在时不应抛错', () => {
      expect(() => fixObjectAssign()).not.toThrow()
      expect(Object.assign({ a: 1 }, { b: 2 })).toEqual({ a: 1, b: 2 })
    })
  })

  describe('initCompatibility 初始化', () => {
    it('应返回 browserInfo 与 deviceInfo', () => {
      setUA('Mozilla/5.0 (Windows NT 10.0) Chrome/120.0')
      const result = initCompatibility()
      expect(result.browserInfo).toBeDefined()
      expect(result.deviceInfo).toBeDefined()
    })

    it('应在 html 上追加 browser/os 类名', () => {
      setUA('Mozilla/5.0 (Windows NT 10.0) Chrome/120.0')
      initCompatibility()
      expect(document.documentElement.classList.contains('browser-chrome')).toBe(true)
      expect(document.documentElement.classList.contains('os-windows')).toBe(true)
    })
  })
})
