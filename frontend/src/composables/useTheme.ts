import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark'

const THEME_STORAGE_KEY = 'app-theme-mode'

const currentTheme = ref<ThemeMode>('light')
let broadcastChannel: BroadcastChannel | null = null
let isInitialized = false

export function useTheme() {
  const getSystemTheme = (): ThemeMode => {
    if (typeof window !== 'undefined' && window.matchMedia) {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }
    return 'light'
  }

  const loadThemeFromStorage = (): ThemeMode => {
    try {
      const stored = localStorage.getItem(THEME_STORAGE_KEY)
      if (stored === 'light' || stored === 'dark') {
        return stored
      }
    } catch (e) {
      console.warn('Failed to load theme from storage:', e)
    }
    return getSystemTheme()
  }

  const saveThemeToStorage = (theme: ThemeMode) => {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, theme)
    } catch (e) {
      console.warn('Failed to save theme to storage:', e)
    }
  }

  const applyTheme = (theme: ThemeMode) => {
    try {
      const html = document.documentElement
      
      html.classList.add('theme-transition')
      
      html.classList.remove('light', 'dark')
      html.classList.add(theme)
      
      html.setAttribute('data-theme', theme)
      
      const metaThemeColor = document.querySelector('meta[name="theme-color"]')
      if (metaThemeColor) {
        metaThemeColor.setAttribute('content', theme === 'dark' ? '#1e293b' : '#ffffff')
      }
      
      setTimeout(() => {
        html.classList.remove('theme-transition')
      }, 300)
    } catch (e) {
      console.error('Failed to apply theme:', e)
    }
  }

  const broadcastThemeChange = (theme: ThemeMode) => {
    if (broadcastChannel) {
      broadcastChannel.postMessage({
        type: 'theme-change',
        theme,
        timestamp: Date.now()
      })
    }
  }

  const setTheme = (theme: ThemeMode) => {
    currentTheme.value = theme
    applyTheme(theme)
    saveThemeToStorage(theme)
    broadcastThemeChange(theme)
  }

  const toggleTheme = () => {
    const newTheme = currentTheme.value === 'light' ? 'dark' : 'light'
    setTheme(newTheme)
  }

  const setupMultiTabSync = () => {
    if (typeof window === 'undefined') return

    window.addEventListener('storage', (e: StorageEvent) => {
      if (e.key === THEME_STORAGE_KEY && e.newValue) {
        const newTheme = e.newValue as ThemeMode
        if (newTheme !== currentTheme.value) {
          console.info(`🔄 检测到其他标签页主题变化(localStorage): ${newTheme}`)
          setTheme(newTheme)
        }
      }
    })

    if (window.BroadcastChannel) {
      broadcastChannel = new BroadcastChannel('app-theme-sync')
      
      broadcastChannel.onmessage = (e: MessageEvent) => {
        if (e.data && e.data.type === 'theme-change') {
          const newTheme = e.data.theme as ThemeMode
          if (newTheme !== currentTheme.value) {
            console.info(`📡 收到主题广播: ${newTheme}`)
            setTheme(newTheme)
          }
        }
      }

      const cleanup = () => {
        if (broadcastChannel) {
          broadcastChannel.close()
          broadcastChannel = null
        }
      }

      window.addEventListener('beforeunload', cleanup)
    }
  }

  const initTheme = () => {
    if (isInitialized) return
    
    try {
      if (typeof document !== 'undefined') {
        const theme = loadThemeFromStorage()
        currentTheme.value = theme
        applyTheme(theme)
        
        setupMultiTabSync()
        
        if (window.matchMedia) {
          const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
          mediaQuery.addEventListener('change', (e) => {
            const stored = localStorage.getItem(THEME_STORAGE_KEY)
            if (!stored) {
              setTheme(e.matches ? 'dark' : 'light')
            }
          })
        }
        
        isInitialized = true
      }
    } catch (e) {
      console.error('Failed to initialize theme:', e)
    }
  }

  const refreshTheme = () => {
    if (typeof document !== 'undefined') {
      applyTheme(currentTheme.value)
    }
  }

  const isDark = () => currentTheme.value === 'dark'
  const isLight = () => currentTheme.value === 'light'

  watch(currentTheme, (newTheme) => {
    applyTheme(newTheme)
  })

  return {
    currentTheme,
    toggleTheme,
    setTheme,
    initTheme,
    refreshTheme,
    isDark,
    isLight,
    getSystemTheme
  }
}

// 导出单例实例（可选）
export const themeManager = {
  currentTheme,
  toggleTheme: () => {
    const newTheme = currentTheme.value === 'light' ? 'dark' : 'light'
    currentTheme.value = newTheme
    document.documentElement.classList.remove('light', 'dark')
    document.documentElement.classList.add(newTheme)
    document.documentElement.setAttribute('data-theme', newTheme)
    localStorage.setItem(THEME_STORAGE_KEY, newTheme)
    
    // 发送广播
    if (broadcastChannel) {
      broadcastChannel.postMessage({
        type: 'theme-change',
        theme: newTheme,
        timestamp: Date.now()
      })
    }
  }
}
