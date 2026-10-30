class SecurityProtector {
  constructor() {
    this.init()
  }

  init() {
    this.disableRightClick()
    this.disableKeyboardShortcuts()
    this.disableViewSource()
    this.preventFrame()
    this.addAntiTampering()
  }

  disableRightClick() {
    document.addEventListener('contextmenu', (e) => {
      e.preventDefault()
    }, { passive: false })
  }

  disableKeyboardShortcuts() {
    document.addEventListener('keydown', (e) => {
      const isCtrl = e.ctrlKey || e.metaKey
      const isShift = e.shiftKey

      if (e.key === 'F12') {
        e.preventDefault()
        return
      }

      if (isCtrl && isShift) {
        if (['i', 'j', 'c', 'k'].includes(e.key.toLowerCase())) {
          e.preventDefault()
          return
        }
      }

      if (isCtrl && ['u', 'i', 'j'].includes(e.key.toLowerCase())) {
        e.preventDefault()
        return
      }
    }, { passive: false })
  }

  disableViewSource() {
    const originalOpen = window.open.bind(window)
    window.open = function(url, ...args) {
      if (typeof url === 'string') {
        const lowerUrl = url.toLowerCase()
        if (lowerUrl.startsWith('view-source:') || lowerUrl.startsWith('javascript:')) {
          return null
        }
      }
      return originalOpen(url, ...args)
    }
  }

  preventFrame() {
    try {
      if (window.top !== window.self) {
        window.top.location.href = window.self.location.href
      }
    } catch (e) {
    }
  }

  addAntiTampering() {
    const style = document.createElement('style')
    style.textContent = `
      body {
        user-select: none;
        -webkit-user-select: none;
        -moz-user-select: none;
        -ms-user-select: none;
      }
    `
    document.head.appendChild(style)
  }
}

const securityProtector = new SecurityProtector()

export { SecurityProtector, securityProtector }

export default securityProtector