import ChatWebSocketClient from '../../src/utils/websocket'

class MockWebSocket {
  static CONNECTING = 0
  static OPEN = 1
  static CLOSING = 2
  static CLOSED = 3
  static instances = []

  constructor(url) {
    this.url = url
    this.readyState = MockWebSocket.CONNECTING
    this.sent = []
    MockWebSocket.instances.push(this)
  }

  // 用 getter/setter 包装：触发 onopen/onclose 时自动翻转 readyState，
  // 避免生产代码在 onopen 中 flush 队列时误判为未连接而死循环入队。
  set onopen(fn) {
    this._onopen = (event) => {
      this.readyState = MockWebSocket.OPEN
      fn(event)
    }
  }
  get onopen() { return this._onopen }

  set onclose(fn) {
    this._onclose = (event) => {
      this.readyState = MockWebSocket.CLOSED
      fn(event)
    }
  }
  get onclose() { return this._onclose }

  send(data) { this.sent.push(data) }
  close() {
    this.readyState = MockWebSocket.CLOSED
    if (this.onclose) this.onclose({ code: 1000, reason: '' })
  }
}

describe('websocket 聊天连接管理', () => {
  let client

  beforeEach(() => {
    MockWebSocket.instances = []
    global.WebSocket = MockWebSocket
    localStorage.clear()
    client = new ChatWebSocketClient({
      url: 'ws://test/ws/chat',
      reconnectInterval: 1000,
      maxReconnectAttempts: 3
    })
  })

  afterEach(() => {
    client.destroy()
  })

  it('connect 应携带 userId 与 token 拼接查询参数', () => {
    localStorage.setItem('token', 'tok')
    client.userId = 'u1'
    client.connect()
    const sock = MockWebSocket.instances.at(-1)
    expect(sock.url).toContain('userId=u1')
    expect(sock.url).toContain('token=tok')
  })

  it('连接成功后 isConnected 应为 true', () => {
    client.connect()
    const sock = MockWebSocket.instances.at(-1)
    sock.onopen({})
    expect(client.isConnected).toBe(true)
    expect(client.reconnectAttempts).toBe(0)
  })

  it('未连接时 send 应入队，连接后自动 flush', () => {
    client.send({ type: 'chat', message: 'hi' })
    expect(client.messageQueue.length).toBe(1)

    client.connect()
    const sock = MockWebSocket.instances.at(-1)
    sock.onopen({})
    expect(client.messageQueue.length).toBe(0)
    expect(sock.sent.some(s => s.includes('"chat"'))).toBe(true)
  })

  it('连接后 send 应直接发送', () => {
    client.connect()
    const sock = MockWebSocket.instances.at(-1)
    sock.onopen({})
    client.send('raw-message')
    expect(sock.sent.at(-1)).toBe('raw-message')
  })

  it('sendChatMessage 应封装为 chat 类型', () => {
    client.connect()
    const sock = MockWebSocket.instances.at(-1)
    sock.onopen({})
    client.sendChatMessage('你好', 'u1', 'c1', 's1')
    const last = JSON.parse(sock.sent.at(-1))
    expect(last.type).toBe('chat')
    expect(last.message).toBe('你好')
  })

  it('ping 应发送心跳', () => {
    client.connect()
    const sock = MockWebSocket.instances.at(-1)
    sock.onopen({})
    client.ping()
    expect(JSON.parse(sock.sent.at(-1)).type).toBe('ping')
  })

  describe('消息路由', () => {
    beforeEach(() => {
      client.connect()
      client._sock = MockWebSocket.instances.at(-1)
      client._sock.onopen({})
    })

    it('type=message 应触发 onData', () => {
      const onData = vi.fn()
      client.onData = onData
      client._sock.onmessage({ data: JSON.stringify({ type: 'message', content: 'x' }) })
      expect(onData).toHaveBeenCalled()
    })

    it('type=start 应触发 onStart', () => {
      const onStart = vi.fn()
      client.onStart = onStart
      client._sock.onmessage({ data: JSON.stringify({ type: 'start' }) })
      expect(onStart).toHaveBeenCalled()
    })

    it('type=done 应触发 onDone', () => {
      const onDone = vi.fn()
      client.onDone = onDone
      client._sock.onmessage({ data: JSON.stringify({ type: 'done' }) })
      expect(onDone).toHaveBeenCalled()
    })

    it('type=connected 应触发 onMessage', () => {
      const onMessage = vi.fn()
      client.onMessage = onMessage
      client._sock.onmessage({ data: JSON.stringify({ type: 'connected' }) })
      expect(onMessage).toHaveBeenCalled()
    })

    it('type=pong 心跳不应报错', () => {
      expect(() => client._sock.onmessage({ data: JSON.stringify({ type: 'pong' }) })).not.toThrow()
    })

    it('非法 JSON 不应抛错', () => {
      expect(() => client._sock.onmessage({ data: '{bad json' })).not.toThrow()
    })

    it('未知 type 应回退到 onMessage', () => {
      const onMessage = vi.fn()
      client.onMessage = onMessage
      client._sock.onmessage({ data: JSON.stringify({ type: 'other', v: 1 }) })
      expect(onMessage).toHaveBeenCalled()
    })
  })

  describe('重连', () => {
    it('onclose 后应按间隔重连（未达上限）', () => {
      vi.useFakeTimers()
      client.connect()
      const first = MockWebSocket.instances.at(-1)
      first.onopen({})
      first.onclose({ code: 1006, reason: '' })
      expect(client.isConnected).toBe(false)
      expect(client.reconnectAttempts).toBe(1)
      vi.advanceTimersByTime(1000)
      expect(MockWebSocket.instances.length).toBe(2)
      vi.useRealTimers()
    })

    it('close() 后不应再重连', () => {
      vi.useFakeTimers()
      client.connect()
      const first = MockWebSocket.instances.at(-1)
      client.close()
      first.onclose({ code: 1000, reason: '' })
      vi.advanceTimersByTime(5000)
      expect(MockWebSocket.instances.length).toBe(1)
      vi.useRealTimers()
    })

    it('超过最大重连次数后停止', () => {
      vi.useFakeTimers()
      client.connect()
      client.reconnectAttempts = 3
      const sock = MockWebSocket.instances.at(-1)
      sock.onclose({ code: 1006, reason: '' })
      vi.advanceTimersByTime(5000)
      expect(MockWebSocket.instances.length).toBe(1)
      vi.useRealTimers()
    })
  })

  describe('destroy', () => {
    it('destroy 应清空队列并置空 ws', () => {
      client.connect()
      client.send('queued')
      client.destroy()
      expect(client.ws).toBeNull()
      expect(client.messageQueue.length).toBe(0)
      expect(client.isConnected).toBe(false)
    })

    it('destroy 不应抛错（无 ws 时）', () => {
      expect(() => client.destroy()).not.toThrow()
    })
  })
})
