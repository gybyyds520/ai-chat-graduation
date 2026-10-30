/**
 * Vitest 全局 setup：补齐 jsdom 未实现的浏览器 API。
 * 仅在测试环境生效，不影响生产构建。
 */

// jsdom 未实现 ObjectURL，musicPlayer 依赖它创建音频 blob URL
if (typeof URL.createObjectURL !== 'function') {
  let counter = 0
  URL.createObjectURL = (blob) => `blob:mock-${counter++}`
}
if (typeof URL.revokeObjectURL !== 'function') {
  URL.revokeObjectURL = () => {}
}

// jsdom 未实现 <audio>.play()，且其返回 undefined（生产代码会 .then().catch()）。
// 这里提供一个返回 Promise 且内部状态自洽的最小实现。
if (typeof HTMLMediaElement !== 'undefined') {
  // jsdom 的 paused 是只读 getter，重写为可读写，用内部标志位跟踪
  Object.defineProperty(HTMLMediaElement.prototype, 'paused', {
    configurable: true,
    get () { return this._paused !== false },
    set (v) { this._paused = !!v }
  })

  HTMLMediaElement.prototype.play = function play () {
    this._paused = false
    if (typeof this.load === 'function') this.load()
    return Promise.resolve()
  }
  HTMLMediaElement.prototype.pause = function pause () {
    this._paused = true
  }
  if (!('load' in HTMLMediaElement.prototype)) {
    HTMLMediaElement.prototype.load = function load () {}
  }
}

// 全局静音 jsdom 的 "Not implemented" 导航/资源告警，保持测试输出干净
// （不抛错，仅抑制控制台噪音）
const originalError = console.error
console.error = (...args) => {
  const msg = args[0]
  if (typeof msg === 'string' && /Not implemented|Could not navigate|Not implemented: navigation/.test(msg)) {
    return
  }
  originalError(...args)
}
