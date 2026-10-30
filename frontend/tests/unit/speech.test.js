import { vi } from 'vitest'

describe('speech 语音合成服务', () => {
  let speechService
  let mockSynth
  let Utterance

  beforeEach(async () => {
    mockSynth = {
      speak: vi.fn(),
      cancel: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      getVoices: vi.fn(() => [
        { name: 'zh-CN', lang: 'zh-CN' },
        { name: 'en-US', lang: 'en-US' }
      ]),
      onvoiceschanged: undefined
    }
    Utterance = class {
      constructor(text) { this.text = text; this.rate = 1; this.pitch = 1; this.volume = 1; this.lang = 'zh-CN' }
    }
    globalThis.speechSynthesis = mockSynth
    window.speechSynthesis = mockSynth
    globalThis.SpeechSynthesisUtterance = Utterance
    window.SpeechSynthesisUtterance = Utterance
    localStorage.clear()
    vi.resetModules()
    speechService = (await import('../../src/utils/speech')).default
  })

  it('应从 localStorage 读取启用状态（默认 true）', () => {
    expect(speechService.enabled).toBe(true)
  })

  it('localStorage 存 false 时应为禁用', async () => {
    localStorage.setItem('speech-enabled', 'false')
    vi.resetModules()
    const s = (await import('../../src/utils/speech')).default
    expect(s.enabled).toBe(false)
  })

  it('speak 应调用 synth.speak 并设置参数', () => {
    speechService.enabled = true
    speechService.speak('你好', { rate: 1.5 })
    expect(mockSynth.speak).toHaveBeenCalledTimes(1)
    const utter = mockSynth.speak.mock.calls[0][0]
    expect(utter.text).toBe('你好')
    expect(utter.rate).toBe(1.5)
  })

  it('禁用时 speak 不应发声', () => {
    speechService.enabled = false
    speechService.speak('不应发声')
    expect(mockSynth.speak).not.toHaveBeenCalled()
  })

  it('空文本 speak 不应发声', () => {
    speechService.enabled = true
    speechService.speak('')
    expect(mockSynth.speak).not.toHaveBeenCalled()
  })

  it('stop 应调用 synth.cancel', () => {
    speechService.stop()
    expect(mockSynth.cancel).toHaveBeenCalled()
  })

  it('pause / resume 应代理到 synth', () => {
    speechService.pause()
    expect(mockSynth.pause).toHaveBeenCalled()
    speechService.resume()
    expect(mockSynth.resume).toHaveBeenCalled()
  })

  it('setRate 应限制在 0.1~10', () => {
    speechService.setRate(0.05)
    expect(speechService.rate).toBe(0.1)
    speechService.setRate(99)
    expect(speechService.rate).toBe(10)
    speechService.setRate(2)
    expect(speechService.rate).toBe(2)
  })

  it('setPitch 应限制在 0~2', () => {
    speechService.setPitch(-1)
    expect(speechService.pitch).toBe(0)
    speechService.setPitch(5)
    expect(speechService.pitch).toBe(2)
  })

  it('setVolume 应限制在 0~1', () => {
    speechService.setVolume(-2)
    expect(speechService.volume).toBe(0)
    speechService.setVolume(3)
    expect(speechService.volume).toBe(1)
  })

  it('setEnabled(false) 应持久化并停止', () => {
    speechService.setEnabled(false)
    expect(speechService.enabled).toBe(false)
    expect(localStorage.getItem('speech-enabled')).toBe('false')
    expect(mockSynth.cancel).toHaveBeenCalled()
  })

  it('getVoices 应返回音色列表', () => {
    expect(speechService.getVoices().length).toBe(2)
  })

  it('setVoice 按名称切换音色', () => {
    speechService.setVoice('en-US')
    expect(speechService.voice.name).toBe('en-US')
  })

  it('不支持语音合成时 init 应降级', async () => {
    delete window.speechSynthesis
    delete globalThis.speechSynthesis
    vi.resetModules()
    const s = (await import('../../src/utils/speech')).default
    s.init()
    expect(s.enabled).toBe(false)
  })
})
