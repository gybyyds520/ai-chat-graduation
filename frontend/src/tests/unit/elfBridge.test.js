import {
  ELF_CONFIG_KEY,
  ELF_LEGACY_WIDGETS_KEY,
  getElfConfig,
  getElfConfigSync,
  saveElfConfig,
  watchElfConfig,
  useElfConfig
} from '../../utils/elfBridge'

describe('elfBridge 桌面精灵全局配置桥接', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  describe('getElfConfig 默认值与读取', () => {
    it('1. 无存储时应返回默认值（visible=true, mode=3d, size=1.0）', () => {
      const cfg = getElfConfig()
      expect(cfg.visible).toBe(true)
      expect(cfg.mode).toBe('3d')
      expect(cfg.size).toBe(1.0)
      expect(cfg.autoRotate).toBe(true)
      expect(cfg.bubbleEnabled).toBe(true)
      expect(cfg.idleAnimation).toBe(true)
      expect(cfg.widgets).toEqual([])
      expect(cfg.characterId).toBeNull()
      expect(cfg.skinId).toBeNull()
      expect(cfg.outfitId).toBeNull()
      // 默认形象：古风华服套装（参考图青绿汉服）
      expect(cfg.outfitSetId).toBe('gufeng')
      expect(cfg.outfitCustom).toBeNull()
      expect(cfg.customImage).toBeNull()
    })

    it('6. 无效 JSON 应回退默认值', () => {
      localStorage.setItem(ELF_CONFIG_KEY, '{{不是合法JSON')
      const cfg = getElfConfig()
      expect(cfg.visible).toBe(true)
      expect(cfg.mode).toBe('3d')
      expect(cfg.size).toBe(1.0)
    })

    it('3. 旧版 desktop-elf-widgets 应迁移到 config.widgets', () => {
      const legacyWidgets = [
        { id: 'w1', type: 'clock', x: 10, y: 10 },
        { id: 'w2', type: 'notes', x: 50, y: 60 }
      ]
      localStorage.setItem(ELF_LEGACY_WIDGETS_KEY, JSON.stringify(legacyWidgets))
      const cfg = getElfConfig()
      expect(cfg.widgets).toEqual(legacyWidgets)
      // 其余字段仍为默认
      expect(cfg.mode).toBe('3d')
      expect(cfg.visible).toBe(true)
    })

    it('getElfConfigSync 应返回配置对象', () => {
      const cfg = getElfConfigSync()
      expect(cfg).toBeDefined()
      expect(typeof cfg.mode).toBe('string')
      expect(Array.isArray(cfg.widgets)).toBe(true)
    })
  })

  describe('saveElfConfig 持久化与合并', () => {
    it('2. 写入后 getElfConfig 应返回新值', () => {
      saveElfConfig({ size: 1.75, mode: '2d' })
      const cfg = getElfConfig()
      expect(cfg.size).toBe(1.75)
      expect(cfg.mode).toBe('2d')
    })

    it('7. 只传部分字段不应覆盖未传字段', () => {
      // 先写入一批完整字段
      saveElfConfig({ characterId: 'ball', skinId: 'neon', mode: '3d' })
      // 再只更新 size
      saveElfConfig({ size: 2.0 })
      const cfg = getElfConfig()
      expect(cfg.size).toBe(2.0)
      expect(cfg.characterId).toBe('ball')
      expect(cfg.skinId).toBe('neon')
      expect(cfg.mode).toBe('3d')
      // 未传字段仍保持默认
      expect(cfg.visible).toBe(true)
    })

    it('widgets 非数组时应回退为空数组', () => {
      saveElfConfig({ widgets: '不是数组' })
      const cfg = getElfConfig()
      expect(Array.isArray(cfg.widgets)).toBe(true)
    })
  })

  describe('watchElfConfig 事件监听', () => {
    it('8. storage 事件应触发回调', () => {
      const cb = vi.fn()
      const unwatch = watchElfConfig(cb)
      const event = new StorageEvent('storage', {
        key: ELF_CONFIG_KEY,
        newValue: JSON.stringify({ visible: false })
      })
      window.dispatchEvent(event)
      expect(cb).toHaveBeenCalledTimes(1)
      unwatch()
    })

    it('unwatch 后不应再触发回调', () => {
      const cb = vi.fn()
      const unwatch = watchElfConfig(cb)
      unwatch()
      const event = new StorageEvent('storage', {
        key: ELF_CONFIG_KEY,
        newValue: JSON.stringify({ size: 1.2 })
      })
      window.dispatchEvent(event)
      expect(cb).not.toHaveBeenCalled()
    })

    it('saveElfConfig 应触发同窗口 watchElfConfig 回调', () => {
      const cb = vi.fn()
      const unwatch = watchElfConfig(cb)
      saveElfConfig({ size: 1.5 })
      expect(cb).toHaveBeenCalled()
      unwatch()
    })

    it('非本配置 key 的 storage 事件不应触发回调', () => {
      const cb = vi.fn()
      const unwatch = watchElfConfig(cb)
      const event = new StorageEvent('storage', {
        key: 'some-other-key',
        newValue: 'x'
      })
      window.dispatchEvent(event)
      expect(cb).not.toHaveBeenCalled()
      unwatch()
    })
  })

  describe('useElfConfig composable', () => {
    it('5. 初始值应从 localStorage 读取', () => {
      localStorage.setItem(
        ELF_CONFIG_KEY,
        JSON.stringify({ visible: false, mode: '2d', size: 1.5, characterId: 'star' })
      )
      const { config } = useElfConfig()
      expect(config.value.visible).toBe(false)
      expect(config.value.mode).toBe('2d')
      expect(config.value.size).toBe(1.5)
      expect(config.value.characterId).toBe('star')
    })

    it('4. updateConfig 应合并部分字段并持久化', () => {
      const { config, updateConfig } = useElfConfig()
      updateConfig({ characterId: 'ball', mode: '2d' })
      expect(config.value.characterId).toBe('ball')
      expect(config.value.mode).toBe('2d')
      // 未传字段保持默认
      expect(config.value.visible).toBe(true)
      // 持久化到 localStorage
      const stored = JSON.parse(localStorage.getItem(ELF_CONFIG_KEY))
      expect(stored.characterId).toBe('ball')
      expect(stored.mode).toBe('2d')
    })

    it('updateConfig 连续调用应累积合并', () => {
      const { config, updateConfig } = useElfConfig()
      updateConfig({ characterId: 'ball' })
      updateConfig({ skinId: 'neon' })
      expect(config.value.characterId).toBe('ball')
      expect(config.value.skinId).toBe('neon')
    })
  })
})
