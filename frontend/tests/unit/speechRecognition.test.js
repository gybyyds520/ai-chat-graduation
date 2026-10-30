import { vi } from 'vitest'
import speechRecognitionService from '../../src/utils/speechRecognition'

class FakeRecognition {
  constructor() { this.start = vi.fn(); this.stop = vi.fn(); this.abort = vi.fn() }
}

describe('speechRecognition 语音识别服务', () => {
  beforeEach(() => {
    window.SpeechRecognition = FakeRecognition
    window.webkitSpeechRecognition = undefined
    speechRecognitionService.onResult = null
    speechRecognitionService.onError = null
    speechRecognitionService.onEnd = null
    speechRecognitionService.init()
  })

  it('isSupported 在有实现时应返回 true', () => {
    expect(speechRecognitionService.isSupported()).toBe(true)
  })

  it('init 成功后 enabled 应为 true', () => {
    expect(speechRecognitionService.enabled).toBe(true)
    expect(speechRecognitionService.recognition).toBeDefined()
  })

  it('init 应设置 lang 与 interimResults', () => {
    expect(speechRecognitionService.recognition.lang).toBe('zh-CN')
    expect(speechRecognitionService.recognition.interimResults).toBe(true)
  })

  it('start 成功应返回 true 并标记 listening', () => {
    const result = speechRecognitionService.start()
    expect(result).toBe(true)
    expect(speechRecognitionService.recognition.start).toHaveBeenCalled()
  })

  it('未 init 时 start 应返回 false', () => {
    speechRecognitionService.enabled = false
    expect(speechRecognitionService.start()).toBe(false)
  })

  it('重复 start 应返回 false', () => {
    speechRecognitionService.start()
    speechRecognitionService.isListening = true
    expect(speechRecognitionService.start()).toBe(false)
  })

  it('start 抛错应捕获并返回 false', () => {
    speechRecognitionService.recognition.start = vi.fn(() => { throw new Error('boom') })
    expect(speechRecognitionService.start()).toBe(false)
  })

  it('onresult 应触发 onResult 回调', () => {
    const onResult = vi.fn()
    speechRecognitionService.onResult = onResult
    speechRecognitionService.start()
    speechRecognitionService.recognition.onresult({
      results: [{ 0: { transcript: '你好' }, isFinal: true }]
    })
    expect(onResult).toHaveBeenCalledWith('你好', true)
  })

  it('onerror 应触发 onError 并复位 listening', () => {
    const onError = vi.fn()
    speechRecognitionService.onError = onError
    speechRecognitionService.start()
    speechRecognitionService.isListening = true
    speechRecognitionService.recognition.onerror({ error: 'not-allowed' })
    expect(onError).toHaveBeenCalledWith('not-allowed')
    expect(speechRecognitionService.isListening).toBe(false)
  })

  it('onend 应触发 onEnd 并复位 listening', () => {
    const onEnd = vi.fn()
    speechRecognitionService.onEnd = onEnd
    speechRecognitionService.start()
    speechRecognitionService.isListening = true
    speechRecognitionService.recognition.onend()
    expect(onEnd).toHaveBeenCalled()
    expect(speechRecognitionService.isListening).toBe(false)
  })

  it('stop 应调用 recognition.stop', () => {
    speechRecognitionService.start()
    speechRecognitionService.isListening = true
    speechRecognitionService.stop()
    expect(speechRecognitionService.recognition.stop).toHaveBeenCalled()
  })

  it('abort 应调用 recognition.abort', () => {
    speechRecognitionService.abort()
    expect(speechRecognitionService.recognition.abort).toHaveBeenCalled()
    expect(speechRecognitionService.isListening).toBe(false)
  })

  it('setLang 应同步到 recognition', () => {
    speechRecognitionService.setLang('en-US')
    expect(speechRecognitionService.lang).toBe('en-US')
    expect(speechRecognitionService.recognition.lang).toBe('en-US')
  })

  it('setInterimResults 应同步', () => {
    speechRecognitionService.setInterimResults(false)
    expect(speechRecognitionService.interimResults).toBe(false)
    expect(speechRecognitionService.recognition.interimResults).toBe(false)
  })

  it('不支持时 enabled 应为 false', () => {
    delete window.SpeechRecognition
    delete window.webkitSpeechRecognition
    speechRecognitionService.init()
    expect(speechRecognitionService.enabled).toBe(false)
    expect(speechRecognitionService.isSupported()).toBe(false)
    // 还原
    window.SpeechRecognition = FakeRecognition
    speechRecognitionService.init()
  })
})
