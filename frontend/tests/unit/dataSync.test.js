import DataSync from '../../src/utils/dataSync'

class MockWS {
  static OPEN = 1
  static instances = []
  constructor(url) {
    this.url = url
    this.readyState = MockWS.OPEN
    this.sent = []
    MockWS.instances.push(this)
  }
  send(d) { this.sent.push(d) }
  close() { this.readyState = 3; if (this.onclose) this.onclose() }
}

describe('dataSync 数据同步', () => {
  let sync

  beforeEach(() => {
    MockWS.instances = []
    global.WebSocket = MockWS
    sync = new DataSync()
  })

  afterEach(() => {
    sync.disconnect()
  })

  it('connect 应携带 userId', () => {
    sync.connect('u1')
    expect(MockWS.instances.at(-1).url).toContain('userId=u1')
  })

  it('重复 connect 且已打开时应复用连接', () => {
    sync.connect('u1')
    const first = MockWS.instances.at(-1)
    first.onopen()
    sync.connect('u1')
    expect(MockWS.instances.length).toBe(1)
  })

  it('onopen 应标记已连接并启动心跳', () => {
    sync.connect('u1')
    MockWS.instances.at(-1).onopen()
    expect(sync.isConnected).toBe(true)
  })

  it('subscribe 在连接后应发送订阅消息', () => {
    sync.connect('u1')
    const sock = MockWS.instances.at(-1)
    sock.onopen()
    sync.subscribe('channel-a', vi.fn())
    expect(sock.sent.some(s => s.includes('channel-a'))).toBe(true)
  })

  it('未连接时 subscribe 仅登记回调', () => {
    const cb = vi.fn()
    sync.subscribe('ch', cb)
    expect(sync.subscriptions.get('ch')).toContain(cb)
  })

  it('收到 dataChange 应触发对应 channel 回调', () => {
    const cb = vi.fn()
    sync.subscribe('ch', cb)
    sync.handleMessage({ type: 'dataChange', channel: 'ch', data: { v: 1 } })
    expect(cb).toHaveBeenCalledWith({ v: 1 })
  })

  it('无订阅的 channel 不应报错', () => {
    expect(() => sync.handleMessage({ type: 'dataChange', channel: 'empty', data: {} })).not.toThrow()
  })

  it('回调抛错不应中断其他回调', () => {
    const bad = vi.fn(() => { throw new Error('x') })
    const good = vi.fn()
    sync.subscriptions.set('ch', [bad, good])
    sync.handleMessage({ type: 'dataChange', channel: 'ch', data: {} })
    expect(good).toHaveBeenCalled()
  })

  it('unsubscribe 应移除回调', () => {
    const cb = vi.fn()
    sync.subscribe('ch', cb)
    sync.unsubscribe('ch', cb)
    expect(sync.subscriptions.get('ch')).not.toContain(cb)
  })

  it('onmessage 应解析 JSON 并分发', () => {
    const cb = vi.fn()
    sync.subscribe('ch', cb)
    sync.connect('u1')
    MockWS.instances.at(-1).onmessage({ data: JSON.stringify({ type: 'dataChange', channel: 'ch', data: 1 }) })
    expect(cb).toHaveBeenCalledWith(1)
  })

  it('非法 JSON 不应抛错', () => {
    sync.connect('u1')
    expect(() => MockWS.instances.at(-1).onmessage({ data: 'oops' })).not.toThrow()
  })

  it('disconnect 应关闭连接并置空', () => {
    sync.connect('u1')
    const sock = MockWS.instances.at(-1)
    sync.disconnect()
    expect(sync.socket).toBeNull()
    expect(sync.isConnected).toBe(false)
  })

  it('onclose 后应触发重连', () => {
    vi.useFakeTimers()
    sync.connect('u1')
    const sock = MockWS.instances.at(-1)
    sock.onopen()
    sock.readyState = 3 // CLOSED
    sock.onclose()
    vi.advanceTimersByTime(3000)
    expect(MockWS.instances.length).toBe(2)
    vi.useRealTimers()
  })
})
