import { SecurityProtector, securityProtector } from '../../src/utils/security'

describe('security 安全防护（SecurityProtector）', () => {
  it('应导出类与单例', () => {
    expect(typeof SecurityProtector).toBe('function')
    expect(securityProtector).toBeDefined()
  })

  describe('disableViewSource 拦截危险 URL', () => {
    it('应拦截 view-source: 开头的 URL', () => {
      expect(window.open('view-source:http://example.com')).toBeNull()
    })

    it('应拦截 javascript: 伪协议', () => {
      expect(window.open('javascript:alert(1)')).toBeNull()
      expect(window.open('JavaScript:alert(1)')).toBeNull()
    })

    it('非字符串入参不应抛错', () => {
      expect(() => window.open(null)).not.toThrow()
      expect(() => window.open(undefined)).not.toThrow()
    })
  })

  describe('addAntiTampering 注入样式', () => {
    it('应在 head 注入 user-select 样式', () => {
      new SecurityProtector()
      const styles = document.head.querySelectorAll('style')
      let found = false
      styles.forEach(s => {
        if (s.textContent.includes('user-select')) found = true
      })
      expect(found).toBe(true)
    })
  })

  describe('preventFrame 防嵌套', () => {
    it('顶层窗口调用不应抛错', () => {
      expect(() => securityProtector.preventFrame()).not.toThrow()
    })
  })

  describe('构造函数初始化', () => {
    it('实例应绑定文档事件监听', () => {
      const sp = new SecurityProtector()
      expect(sp).toBeDefined()
      expect(typeof sp.disableRightClick).toBe('function')
      expect(typeof sp.disableKeyboardShortcuts).toBe('function')
    })
  })
})
