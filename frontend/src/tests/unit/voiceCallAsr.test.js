import { VoiceCallClient } from '../../utils/voiceCall'

/**
 * 语音识别"停止并发送"时序回归测试：
 * 用户反馈"无法停止并发送 / 无法识别内容"——
 * 根因：Web Speech API 的 rec.stop() 是异步的，最终转写结果在 onend 前才合并，
 *       旧实现 stop() 后立即读文本导致读到空 → 永远"未识别到内容"。
 * 修复：stop 函数返回 Promise，在 onend（最终结果合并后）resolve 完整文本。
 */

function installMockSpeechRecognition () {
  let instance = null
  class MockSR {
    constructor () {
      this.lang = ''
      this.interimResults = false
      this.continuous = false
      this.onresult = null
      this.onerror = null
      this.onend = null
      instance = this
    }
    start () { this._started = true }
    stop () {
      // 模拟浏览器行为：stop() 后先补发新增段 final 结果（resultIndex 指向新增起点），再触发 onend
      if (this.onresult) {
        const evt = {
          resultIndex: 1,
          results: [
            { isFinal: true, 0: { transcript: '你好' } },
            { isFinal: true, 0: { transcript: '世界' } }
          ]
        }
        this.onresult(evt)
      }
      if (this.onend) this.onend()
    }
    abort () { if (this.onend) this.onend() }
  }
  window.SpeechRecognition = MockSR
  return () => instance
}

describe('VoiceCallClient.startBrowserAsr 停止时序', () => {
  let getInstance

  beforeEach(() => {
    getInstance = installMockSpeechRecognition()
  })

  afterEach(() => {
    delete window.SpeechRecognition
  })

  it('1. 支持检测浏览器 Web Speech API', () => {
    const client = new VoiceCallClient()
    expect(client.isBrowserAsrSupported()).toBe(true)
  })

  it('2. stop() 返回 Promise 并在最终结果合并后 resolve 完整文本', async () => {
    const client = new VoiceCallClient()
    let latest = ''
    const stop = client.startBrowserAsr((t) => { latest = t })
    const inst = getInstance()
    // 录音中：先收到一句 final 结果
    inst.onresult({
      resultIndex: 0,
      results: [{ isFinal: true, 0: { transcript: '你好' } }]
    })
    expect(latest).toBe('你好')
    // 点击"停止并发送"：await stop() 拿到合并后的完整最终文本（旧实现这里读到空/缺句）
    const text = await stop()
    expect(text).toBe('你好世界')
    expect(latest).toBe('你好世界')
  })

  it('3. 静音停止时 resolve 空文本（UI 提示重说，不卡死）', async () => {
    const client = new VoiceCallClient()
    const stop = client.startBrowserAsr(() => {})
    const inst = getInstance()
    // 无任何 onresult 时 stop → onend resolve 空字符串
    inst.onresult = null
    inst.onend()
    const text = await stop()
    expect(text).toBe('')
  })

  it('4. 识别错误通过 onError 回调暴露（权限/设备/无语音）', () => {
    const client = new VoiceCallClient()
    const errs = []
    client.startBrowserAsr(() => {}, { onError: (m) => errs.push(m) })
    const inst = getInstance()
    inst.onerror({ error: 'not-allowed' })
    expect(errs[0]).toContain('麦克风权限')
  })

  it('5. 识别错误后 stop 仍结算（不悬挂 Promise）', async () => {
    const client = new VoiceCallClient()
    let settle = false
    const stop = client.startBrowserAsr(() => {})
    const inst = getInstance()
    inst.onerror({ error: 'no-speech' })
    const text = await stop()
    expect(typeof text).toBe('string')
  })
})
