import logger from './logger.js'

// 动态解析 WebSocket 地址：优先走当前页面同源（vite proxy 转发 /ws → 8080），
// 兼容移动端局域网访问（IP 变化时自动跟随）；Electron file:// 或异常环境回退 localhost:8080。
export function resolveWsUrl(defaultPath, optionsUrl) {
  if (optionsUrl) return optionsUrl
  try {
    const hostname = window.location.hostname
    if (hostname) {
      const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
      const port = window.location.port || '5187'
      return `${proto}://${hostname}:${port}${defaultPath}`
    }
  } catch (e) {}
  return `ws://localhost:8080${defaultPath}`
}

// WebSocket聊天客户端
class ChatWebSocketClient {
  constructor(options = {}) {
    this.url = resolveWsUrl('/ws/chat', options.url)
    this.reconnectInterval = options.reconnectInterval || 3000
    this.maxReconnectAttempts = options.maxReconnectAttempts || 5
    this.userId = options.userId || null
    
    this.ws = null
    this.reconnectAttempts = 0
    this.isConnected = false
    this.messageQueue = []
    this.reconnectTimer = null
    
    this.onOpen = options.onOpen || (() => {})
    this.onClose = options.onClose || (() => {})
    this.onError = options.onError || (() => {})
    this.onMessage = options.onMessage || (() => {})
    this.onConnected = options.onConnected || (() => {})
    this.onStart = options.onStart || (() => {})
    this.onData = options.onData || (() => {})
    this.onDone = options.onDone || (() => {})
    this.onError = options.onError || (() => {})
  }

  connect() {
    try {
      let connectUrl = this.url
      const params = []
      if (this.userId) {
        params.push('userId=' + this.userId)
      }
      // 安全修复：WebSocket 握手必须携带登录 token，服务端以 token 校验身份，
      // 不再信任 URL 中的 userId 参数（防止冒充任意用户）。
      const token = localStorage.getItem('token')
      if (token) {
        params.push('token=' + encodeURIComponent(token))
      }
      if (params.length > 0) {
        connectUrl += (this.url.includes('?') ? '&' : '?') + params.join('&')
      }
      logger.info('[WebSocket] 正在连接:', connectUrl)
      this.ws = new WebSocket(connectUrl)
      
      this.ws.onopen = (event) => {
        logger.info('[WebSocket] 连接已建立')
        this.isConnected = true
        this.reconnectAttempts = 0
        this.onOpen(event)
        this.onConnected(event)
        
        // 发送队列中的消息
        while (this.messageQueue.length > 0) {
          const msg = this.messageQueue.shift()
          this.send(msg)
        }
      }
      
      this.ws.onclose = (event) => {
        logger.info('[WebSocket] 连接已关闭', event.code, event.reason)
        this.isConnected = false
        this.onClose(event)
        
        // 尝试重连
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++
          logger.info(`[WebSocket] ${this.reconnectInterval}ms后尝试第${this.reconnectAttempts}次重连...`)
          this.reconnectTimer = setTimeout(() => this.connect(), this.reconnectInterval)
        } else {
          logger.error('[WebSocket] 最大重连次数已用完')
        }
      }
      
      this.ws.onerror = (error) => {
        logger.error('[WebSocket] 连接错误:', error)
        this.onError(error)
      }
      
      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          logger.debug('[WebSocket] 收到消息:', data.type)
          
          switch (data.type) {
            case 'connected':
              this.onMessage(data)
              break
            case 'start':
              this.onStart(data)
              break
            case 'message':
              this.onData(data)
              break
            case 'done':
              this.onDone(data)
              break
            case 'error':
              logger.error('[WebSocket] 服务器错误:', data.message)
              this.onError(data)
              break
            case 'pong':
              logger.debug('[WebSocket] 心跳响应')
              break
            default:
              this.onMessage(data)
          }
        } catch (e) {
          logger.error('[WebSocket] 消息解析失败:', e, event.data)
        }
      }
    } catch (error) {
      logger.error('[WebSocket] 连接失败:', error)
      this.onError(error)
    }
  }

  send(data) {
    if (typeof data === 'object') {
      data = JSON.stringify(data)
    }
    
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(data)
    } else {
      logger.warn('[WebSocket] 未连接，消息已加入队列')
      this.messageQueue.push(data)
    }
  }

  sendChatMessage(message, userId, characterId, sessionId) {
    this.send({
      type: 'chat',
      message: message,
      userId: userId,
      characterId: characterId,
      sessionId: sessionId
    })
  }

  ping() {
    this.send({ type: 'ping' })
  }

  close() {
    if (this.ws) {
      this.reconnectAttempts = this.maxReconnectAttempts
      this.ws.close()
    }
  }

  destroy() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.reconnectAttempts = this.maxReconnectAttempts
    if (this.ws) {
      try {
        this.ws.close()
      } catch (e) {
        logger.warn('[WebSocket] 关闭连接时发生错误:', e)
      }
      this.ws = null
    }
    this.isConnected = false
    this.messageQueue = []
  }
}

export default ChatWebSocketClient
