import { describe, it, expect, vi } from 'vitest'
import { VoiceCallClient, VOICE_STATE, base64ToBlob, formatToMime } from '../../src/utils/voiceCall'

describe('voiceCall 语音通话客户端', () => {
  function makeHttp () {
    return {
      post: vi.fn()
    }
  }

  it('初始状态应为 idle', () => {
    const client = new VoiceCallClient({ http: makeHttp() })
    expect(client.state).toBe(VOICE_STATE.IDLE)
    expect(client.sessionId).toBeNull()
  })

  it('startVoiceCall 应请求并切换到 recording', async () => {
    const http = makeHttp()
    http.post.mockResolvedValue({ data: { sessionId: 'sess-123', adapterInfo: { mockMode: true } } })
    const client = new VoiceCallClient({ http })

    const sid = await client.startVoiceCall(100)

    expect(sid).toBe('sess-123')
    expect(client.sessionId).toBe('sess-123')
    expect(client.state).toBe(VOICE_STATE.RECORDING)
    expect(http.post).toHaveBeenCalledWith('/api/voice-call/start', { characterId: 100 })
  })

  it('sendAudio 应切换 processing->playing 并返回后端数据', async () => {
    const http = makeHttp()
    http.post
      .mockResolvedValueOnce({ data: { sessionId: 's1' } })
      .mockResolvedValueOnce({ data: { userText: '你好', aiText: '回复', audioBase64: 'QUJD' } })
    const client = new VoiceCallClient({ http })
    await client.startVoiceCall(null)

    const states = []
    client.onStateChange = (s) => states.push(s)

    const result = await client.sendAudio(new Blob([new Uint8Array([1, 2, 3])]))

    expect(result.userText).toBe('你好')
    expect(client.state).toBe(VOICE_STATE.PLAYING)
    expect(states).toContain(VOICE_STATE.PROCESSING)
    expect(states).toContain(VOICE_STATE.PLAYING)
  })

  it('sendAudio 空音频应抛错', async () => {
    const client = new VoiceCallClient({ http: makeHttp() })
    client.sessionId = 's1'
    await expect(client.sendAudio(new Blob()))
      .rejects.toThrow(/音频为空/)
  })

  it('sendAudio 超过 10MB 应抛错', async () => {
    const client = new VoiceCallClient({ http: makeHttp() })
    client.sessionId = 's1'
    const big = new Blob([new Uint8Array(10 * 1024 * 1024 + 1)])
    await expect(client.sendAudio(big)).rejects.toThrow(/10MB/)
  })

  it('sendAudio 未开启会话应抛错', async () => {
    const client = new VoiceCallClient({ http: makeHttp() })
    await expect(client.sendAudio(new Blob([1])))
      .rejects.toThrow(/未开启/)
  })

  it('base64ToBlob 应正确解码字节', () => {
    // "QUJD" 解码为 "ABC"
    const blob = base64ToBlob('QUJD', 'audio/wav')
    expect(blob.type).toBe('audio/wav')
    expect(blob.size).toBe(3)
  })

  it('formatToMime 应映射常见格式', () => {
    expect(formatToMime('mp3')).toBe('audio/mpeg')
    expect(formatToMime('mpeg')).toBe('audio/mpeg')
    expect(formatToMime('wav')).toBe('audio/wav')
    expect(formatToMime('unknown')).toBe('application/octet-stream')
  })
})
