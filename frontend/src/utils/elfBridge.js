/**
 * elfBridge.js — 桌面精灵全局配置桥接模块
 *
 * 负责：
 *  1. 从 localStorage 读取/写入桌面精灵全局配置（key = desktop-elf-config）
 *  2. 通过 storage 事件实现同窗口 / 跨窗口的配置同步
 *  3. 兼容迁移旧版 key（desktop-elf-widgets → config.widgets）
 *  4. 提供 Vue composable useElfConfig() 返回响应式配置
 *
 * 设计原则：
 *  - 纯函数模块，不依赖 DOM 渲染，可在 jsdom / node 下直接测试
 *  - 所有 localStorage 读写均做 try-catch 兜底，隐私模式 / SSR 下不崩溃
 */

import { ref, onMounted, onUnmounted } from 'vue'

export const ELF_CONFIG_KEY = 'desktop-elf-config'
export const ELF_LEGACY_WIDGETS_KEY = 'desktop-elf-widgets'

/**
 * 默认配置。注意 Object.freeze 防止外部误改常量。
 * @type {Readonly<Object>}
 */
const DEFAULT_CONFIG = Object.freeze({
  visible: true,
  characterId: null,
  skinId: null,
  outfitId: null,
  // ─── 默认形象（全面更新）：古风华服套装 + 古风角色青黛 ───
  outfitSetId: 'gufeng',
  outfitCustom: null,
  mode: '3d',          // '3d' | '2d'
  size: 1.0,
  autoRotate: true,
  bubbleEnabled: true,
  idleAnimation: true,
  widgets: [],
  // ─── 自由移动系统（二次升级新增，默认值保证旧配置兼容） ───
  movementEnabled: true,   // 自主巡逻开关
  followMouse: false,      // 跟随鼠标开关
  moveSpeed: 50,           // 移动速度（像素/秒）
  // ─── 自定义形象（三次升级新增）：用户上传的图片 dataURL/URL，渲染时优先显示 ───
  customImage: null,
  // ─── 自定义形象 3D 化（四次升级新增）：上传图片自动去背景后渲染为 3D 立体立牌 ───
  customImageRemoveBg: true   // 智能去背景开关（色度键 + 羽化）
})

/**
 * 安全读取 localStorage（隐私模式 / SSR 下返回 null）
 */
function readStorage(key) {
  try {
    if (typeof localStorage === 'undefined') return null
    return localStorage.getItem(key)
  } catch (e) {
    return null
  }
}

/**
 * 安全写入 localStorage
 */
function writeStorage(key, value) {
  try {
    if (typeof localStorage === 'undefined') return false
    localStorage.setItem(key, value)
    return true
  } catch (e) {
    return false
  }
}

/**
 * 从 localStorage 读取配置并合并默认值。
 *
 * 合并规则：
 *  - 若 desktop-elf-config 存在且为合法 JSON 对象，则与默认值浅合并
 *  - 若 desktop-elf-config 不存在，但旧版 desktop-elf-widgets 存在，则把它迁移进 widgets
 *  - 若 desktop-elf-config 存在但为非法 JSON，直接回退默认值
 *
 * @returns {Object} 合并后的完整配置（每次返回新对象）
 */
export function getElfConfig() {
  const config = { ...DEFAULT_CONFIG }
  const storedRaw = readStorage(ELF_CONFIG_KEY)

  if (storedRaw) {
    try {
      const parsed = JSON.parse(storedRaw)
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        Object.assign(config, parsed)
      }
    } catch (e) {
      // 无效 JSON → 回退默认值
      return { ...DEFAULT_CONFIG }
    }
  } else {
    // 兼容迁移：旧版 desktop-elf-widgets → config.widgets
    try {
      const legacyRaw = readStorage(ELF_LEGACY_WIDGETS_KEY)
      if (legacyRaw) {
        const legacy = JSON.parse(legacyRaw)
        if (Array.isArray(legacy)) {
          config.widgets = legacy
        }
      }
    } catch (e) {
      config.widgets = []
    }
  }

  // 兜底：widgets 必须是数组
  if (!Array.isArray(config.widgets)) {
    config.widgets = []
  }
  return config
}

/**
 * 同步读取当前配置（不创建响应式）。
 * 语义上与 getElfConfig 一致，单独导出便于调用方表达"非响应式读取"意图。
 * @returns {Object}
 */
export function getElfConfigSync() {
  return getElfConfig()
}

/**
 * 派发 storage 事件，让同窗口内的 watchElfConfig 监听器收到通知。
 * （浏览器原生 storage 事件只在其它窗口触发，同窗口需手动派发）
 */
function dispatchStorageEvent(config) {
  try {
    if (typeof window === 'undefined') return
    const detail = {
      key: ELF_CONFIG_KEY,
      newValue: JSON.stringify(config),
      oldValue: readStorage(ELF_CONFIG_KEY),
      url: (typeof window.location !== 'undefined' && window.location.href) || ''
    }
    let event = null
    if (typeof StorageEvent === 'function') {
      event = new StorageEvent('storage', detail)
    } else if (typeof document !== 'undefined' && typeof document.createEvent === 'function') {
      event = document.createEvent('Event')
      event.initEvent('storage', true, true)
      event.key = detail.key
      event.newValue = detail.newValue
    }
    if (event) {
      window.dispatchEvent(event)
    }
  } catch (e) {
    // 事件派发失败不影响持久化
  }
}

/**
 * 保存配置：与当前配置合并（只覆盖传入字段，不影响未传字段），
 * 写入 localStorage 并派发 storage 事件。
 *
 * @param {Object} [partialConfig={}] 需要更新的部分配置
 * @returns {Object} 合并后完整配置
 */
export function saveElfConfig(partialConfig = {}) {
  const current = getElfConfig()
  const merged = { ...current, ...partialConfig }
  if (!Array.isArray(merged.widgets)) {
    merged.widgets = Array.isArray(current.widgets) ? current.widgets : []
  }
  writeStorage(ELF_CONFIG_KEY, JSON.stringify(merged))
  dispatchStorageEvent(merged)
  return merged
}

/**
 * 监听配置变化。
 * @param {(config: Object) => void} callback 配置变化时回调
 * @returns {() => void} unwatch 函数，调用后停止监听
 */
export function watchElfConfig(callback) {
  if (typeof window === 'undefined' || typeof window.addEventListener !== 'function') {
    return () => {}
  }
  const handler = (event) => {
    if (!event) return
    // 仅响应本配置 key（key === null 表示 clear()，也刷新一次）
    if (event.key === ELF_CONFIG_KEY || event.key === null) {
      try {
        callback(getElfConfig())
      } catch (e) {
        // 忽略回调内部错误，避免监听器中断
      }
    }
  }
  window.addEventListener('storage', handler)
  return () => window.removeEventListener('storage', handler)
}

/**
 * Vue composable：返回响应式配置与更新函数。
 *
 * @returns {{ config: import('vue').Ref<Object>, updateConfig: (partial: Object) => void }}
 */
export function useElfConfig() {
  // 同步初始化（即使在测试环境没有组件实例，也能读到 localStorage）
  const config = ref(getElfConfig())
  let unwatch = null

  onMounted(() => {
    // 重新读取一次，确保 onMounted 后状态最新
    config.value = getElfConfig()
    unwatch = watchElfConfig((newConfig) => {
      config.value = newConfig
    })
  })

  onUnmounted(() => {
    if (unwatch) {
      unwatch()
      unwatch = null
    }
  })

  /**
   * 合并部分字段并持久化
   * @param {Object} partial
   */
  function updateConfig(partial = {}) {
    const merged = saveElfConfig(partial)
    config.value = merged
  }

  return { config, updateConfig }
}
