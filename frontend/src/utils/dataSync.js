import { resolveWsUrl } from './websocket.js'

class DataSync {
  constructor() {
    this.socket = null
    this.url = resolveWsUrl('/ws/data-sync')
    this.isConnected = false
    this.reconnectInterval = 3000
    this.subscriptions = new Map()
    this.userId = null
  }

  connect(userId) {
    this.userId = userId
    
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      return
    }

    const fullUrl = `${this.url}?userId=${userId}`
    this.socket = new WebSocket(fullUrl)

    this.socket.onopen = () => {
      console.log('数据同步WebSocket连接已建立')
      this.isConnected = true
      this.startHeartbeat()
      this.subscribeAll()
    }

    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        this.handleMessage(message)
      } catch (error) {
        console.error('解析数据同步消息失败:', error)
      }
    }

    this.socket.onerror = (error) => {
      console.error('数据同步WebSocket错误:', error)
      this.isConnected = false
    }

    this.socket.onclose = () => {
      console.log('数据同步WebSocket连接已关闭')
      this.isConnected = false
      this.reconnect()
    }
  }

  reconnect() {
    if (this.isConnected) return
    
    setTimeout(() => {
      console.log('尝试重新连接数据同步WebSocket...')
      this.connect(this.userId)
    }, this.reconnectInterval)
  }

  startHeartbeat() {
    setInterval(() => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify({ type: 'ping' }))
      }
    }, 30000)
  }

  subscribe(channel, callback) {
    if (!this.subscriptions.has(channel)) {
      this.subscriptions.set(channel, [])
    }
    this.subscriptions.get(channel).push(callback)

    if (this.isConnected) {
      this.socket.send(JSON.stringify({ type: 'subscribe', channel }))
    }
  }

  unsubscribe(channel, callback) {
    const callbacks = this.subscriptions.get(channel)
    if (callbacks) {
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  subscribeAll() {
    this.subscriptions.forEach((_, channel) => {
      this.socket.send(JSON.stringify({ type: 'subscribe', channel }))
    })
  }

  handleMessage(message) {
    if (message.type === 'dataChange') {
      const callbacks = this.subscriptions.get(message.channel)
      if (callbacks) {
        callbacks.forEach(callback => {
          try {
            callback(message.data)
          } catch (error) {
            console.error('处理数据变更回调失败:', error)
          }
        })
      }
    } else if (message.type === 'connected') {
      console.log(message.message)
    } else if (message.type === 'pong') {
      console.log('心跳响应')
    }
  }

  disconnect() {
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
    this.isConnected = false
  }

  static getInstance() {
    if (!DataSync.instance) {
      DataSync.instance = new DataSync()
    }
    return DataSync.instance
  }
}

export default DataSync