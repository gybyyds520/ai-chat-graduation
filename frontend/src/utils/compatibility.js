/**
 * 兼容性工具函数
 * 提供跨浏览器和设备的兼容性支持
 */

/**
 * 获取浏览器信息
 */
export const getBrowserInfo = () => {
  const userAgent = navigator.userAgent
  let browser = 'unknown'
  let version = '0'
  let os = 'unknown'

  // 检测浏览器
  if (userAgent.includes('Chrome') && !userAgent.includes('Edg')) {
    browser = 'chrome'
    const match = userAgent.match(/Chrome\/(\d+)/)
    version = match ? match[1] : '0'
  } else if (userAgent.includes('Firefox')) {
    browser = 'firefox'
    const match = userAgent.match(/Firefox\/(\d+)/)
    version = match ? match[1] : '0'
  } else if (userAgent.includes('Safari') && !userAgent.includes('Chrome')) {
    browser = 'safari'
    const match = userAgent.match(/Version\/(\d+)/)
    version = match ? match[1] : '0'
  } else if (userAgent.includes('Edg')) {
    browser = 'edge'
    const match = userAgent.match(/Edg\/(\d+)/)
    version = match ? match[1] : '0'
  } else if (userAgent.includes('Trident') || userAgent.includes('MSIE')) {
    browser = 'ie'
    const match = userAgent.match(/(MSIE |rv:)(\d+)/)
    version = match ? match[2] : '0'
  }

  // 检测操作系统
  // 注意：iOS 的 UserAgent 形如 "... iPhone OS ... like Mac OS X"，其中包含 "Mac OS"，
  // 因此必须先判定 iPhone/iPad/iPod，否则会被误判为 macos。
  if (userAgent.includes('Windows')) {
    os = 'windows'
  } else if (userAgent.includes('Android')) {
    os = 'android'
  } else if (userAgent.includes('iPhone') || userAgent.includes('iPad') || userAgent.includes('iPod')) {
    os = 'ios'
  } else if (userAgent.includes('Mac OS')) {
    os = 'macos'
  } else if (userAgent.includes('Linux')) {
    os = 'linux'
  }

  return { browser, version, os, userAgent }
}

/**
 * 检测设备类型
 */
export const detectDevice = () => {
  const userAgent = navigator.userAgent
  const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(userAgent)
  const isTablet = /iPad|Tablet|Nexus 7|Nexus 10/i.test(userAgent)
  const isDesktop = !isMobile && !isTablet
  
  return { isMobile, isTablet, isDesktop }
}

/**
 * 检测触摸支持
 */
export const hasTouchSupport = () => {
  return 'ontouchstart' in window || navigator.maxTouchPoints > 0
}

/**
 * 检测CSS属性支持
 */
export const supportsCSSProperty = (property) => {
  const prefixes = ['', '-webkit-', '-moz-', '-ms-', '-o-']
  const el = document.createElement('div')
  
  for (const prefix of prefixes) {
    if (prefix + property in el.style) {
      return true
    }
  }
  return false
}

/**
 * 修复vh单位在移动端的问题
 */
export const fixVHUnit = () => {
  const setVH = () => {
    const vh = window.innerHeight * 0.01
    document.documentElement.style.setProperty('--vh', `${vh}px`)
  }
  
  setVH()
  window.addEventListener('resize', setVH)
  window.addEventListener('orientationchange', setVH)
  
  return () => {
    window.removeEventListener('resize', setVH)
    window.removeEventListener('orientationchange', setVH)
  }
}

/**
 * 修复滚动行为
 */
export const fixScrollBehavior = () => {
  // 检测平滑滚动支持
  const supportsSmoothScroll = 'scrollBehavior' in document.documentElement.style
  
  if (!supportsSmoothScroll) {
    // 添加polyfill
    Element.prototype.scrollIntoView = function (options) {
      const element = this
      const block = options && options.block === 'start' ? 'start' : 'end'
      
      const rect = element.getBoundingClientRect()
      const windowHeight = window.innerHeight
      const scrollY = window.scrollY
      
      let scrollTo = scrollY
      
      if (block === 'start') {
        scrollTo += rect.top
      } else {
        scrollTo += rect.bottom - windowHeight
      }
      
      // 平滑滚动模拟
      const startY = window.scrollY
      const distance = scrollTo - startY
      const duration = 300
      const startTime = performance.now()
      
      const animate = (currentTime) => {
        const elapsed = currentTime - startTime
        const progress = Math.min(elapsed / duration, 1)
        
        // ease-out-cubic
        const easeProgress = 1 - Math.pow(1 - progress, 3)
        
        window.scrollTo(0, startY + distance * easeProgress)
        
        if (progress < 1) {
          requestAnimationFrame(animate)
        }
      }
      
      requestAnimationFrame(animate)
    }
  }
}

/**
 * 修复Promise支持（IE 11）
 */
export const fixPromise = () => {
  if (typeof Promise === 'undefined') {
    // Promise polyfill
    window.Promise = class Promise {
      constructor(executor) {
        this._state = 'pending'
        this._value = undefined
        this._handlers = []
        
        const resolve = (value) => {
          if (this._state !== 'pending') return
          this._state = 'fulfilled'
          this._value = value
          this._notifyHandlers()
        }
        
        const reject = (reason) => {
          if (this._state !== 'pending') return
          this._state = 'rejected'
          this._value = reason
          this._notifyHandlers()
        }
        
        try {
          executor(resolve, reject)
        } catch (e) {
          reject(e)
        }
      }
      
      then(onFulfilled, onRejected) {
        return new Promise((resolve, reject) => {
          this._handlers.push({
            onFulfilled: onFulfilled || ((v) => v),
            onRejected: onRejected || ((e) => { throw e }),
            resolve,
            reject
          })
          this._notifyHandlers()
        })
      }
      
      catch(onRejected) {
        return this.then(null, onRejected)
      }
      
      _notifyHandlers() {
        if (this._state === 'pending') return
        
        setTimeout(() => {
          this._handlers.forEach((handler) => {
            try {
              const result = this._state === 'fulfilled'
                ? handler.onFulfilled(this._value)
                : handler.onRejected(this._value)
              handler.resolve(result)
            } catch (e) {
              handler.reject(e)
            }
          })
          this._handlers = []
        }, 0)
      }
      
      static resolve(value) {
        return new Promise((resolve) => resolve(value))
      }
      
      static reject(reason) {
        return new Promise((_, reject) => reject(reason))
      }
      
      static all(promises) {
        return new Promise((resolve, reject) => {
          const results = []
          let completed = 0
          
          promises.forEach((promise, index) => {
            promise.then((value) => {
              results[index] = value
              completed++
              if (completed === promises.length) {
                resolve(results)
              }
            }).catch(reject)
          })
        })
      }
      
      static race(promises) {
        return new Promise((resolve, reject) => {
          promises.forEach((promise) => {
            promise.then(resolve).catch(reject)
          })
        })
      }
    }
  }
}

/**
 * 修复fetch支持
 */
export const fixFetch = () => {
  if (typeof fetch === 'undefined') {
    window.fetch = function (url, options) {
      return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest()
        const method = options && options.method || 'GET'
        const headers = options && options.headers || {}
        const body = options && options.body
        
        xhr.open(method, url, true)
        
        Object.keys(headers).forEach((key) => {
          xhr.setRequestHeader(key, headers[key])
        })
        
        xhr.onload = () => {
          const response = {
            ok: xhr.status >= 200 && xhr.status < 300,
            status: xhr.status,
            statusText: xhr.statusText,
            headers: {
              get: (name) => xhr.getResponseHeader(name)
            },
            text: () => Promise.resolve(xhr.responseText),
            json: () => Promise.resolve(JSON.parse(xhr.responseText))
          }
          resolve(response)
        }
        
        xhr.onerror = () => {
          reject(new TypeError('Network request failed'))
        }
        
        xhr.send(body)
      })
    }
  }
}

/**
 * 修复Array.prototype.includes
 */
export const fixArrayIncludes = () => {
  if (!Array.prototype.includes) {
    Array.prototype.includes = function (searchElement, fromIndex) {
      const O = Object(this)
      const len = parseInt(O.length, 10) || 0
      if (len === 0) return false
      
      const n = parseInt(fromIndex, 10) || 0
      let k
      
      if (n >= 0) {
        k = n
      } else {
        k = len + n
        if (k < 0) k = 0
      }
      
      while (k < len) {
        const currentElement = O[k]
        if (searchElement === currentElement || 
            (searchElement !== searchElement && currentElement !== currentElement)) {
          return true
        }
        k++
      }
      
      return false
    }
  }
}

/**
 * 修复Object.assign
 */
export const fixObjectAssign = () => {
  if (typeof Object.assign !== 'function') {
    Object.assign = function (target, ...sources) {
      if (target === null || target === undefined) {
        throw new TypeError('Cannot convert undefined or null to object')
      }
      
      const to = Object(target)
      
      for (let i = 0; i < sources.length; i++) {
        const source = sources[i]
        if (source !== null && source !== undefined) {
          for (const key in source) {
            if (Object.prototype.hasOwnProperty.call(source, key)) {
              to[key] = source[key]
            }
          }
        }
      }
      
      return to
    }
  }
}

/**
 * 初始化所有兼容性修复
 */
export const initCompatibility = () => {
  const browserInfo = getBrowserInfo()
  const deviceInfo = detectDevice()
  
  console.info('[Compatibility] Browser:', browserInfo)
  console.info('[Compatibility] Device:', deviceInfo)
  
  // 应用兼容性修复
  fixVHUnit()
  fixScrollBehavior()
  fixPromise()
  fixFetch()
  fixArrayIncludes()
  fixObjectAssign()
  
  // 添加类名到html元素
  document.documentElement.classList.add(`browser-${browserInfo.browser}`)
  document.documentElement.classList.add(`os-${browserInfo.os}`)
  
  if (deviceInfo.isMobile) {
    document.documentElement.classList.add('device-mobile')
  } else if (deviceInfo.isTablet) {
    document.documentElement.classList.add('device-tablet')
  } else {
    document.documentElement.classList.add('device-desktop')
  }
  
  if (hasTouchSupport()) {
    document.documentElement.classList.add('touch-support')
  }
  
  return { browserInfo, deviceInfo }
}

export default {
  getBrowserInfo,
  detectDevice,
  hasTouchSupport,
  supportsCSSProperty,
  fixVHUnit,
  fixScrollBehavior,
  fixPromise,
  fixFetch,
  fixArrayIncludes,
  fixObjectAssign,
  initCompatibility
}