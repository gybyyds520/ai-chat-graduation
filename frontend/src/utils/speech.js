import logger from './logger.js'

// 语音合成服务
class SpeechService {
  constructor() {
    this.synth = window.speechSynthesis
    this.enabled = this.loadEnabledState()
    this.voice = null
    this.rate = 1.0
    this.pitch = 1.0
    this.volume = 1.0
  }

  // 从localStorage加载启用状态
  loadEnabledState() {
    const saved = localStorage.getItem('speech-enabled')
    if (saved !== null) {
      return saved === 'true'
    }
    return true
  }

  // 保存启用状态到localStorage
  saveEnabledState(enabled) {
    localStorage.setItem('speech-enabled', String(enabled))
  }

  // 初始化语音
  init() {
    if ('speechSynthesis' in window) {
      const loadVoices = () => {
        const voices = this.synth.getVoices()
        this.voice = voices.find(v => v.lang.includes('zh')) || 
                    voices.find(v => v.lang.includes('en')) || 
                    voices[0]
        logger.info('[Speech] 语音引擎已就绪:', this.voice?.name)
      }

      if (speechSynthesis.onvoiceschanged !== undefined) {
        speechSynthesis.onvoiceschanged = loadVoices
      }
      loadVoices()
    } else {
      logger.warn('[Speech] 当前浏览器不支持语音合成')
      this.enabled = false
      this.saveEnabledState(false)
    }
  }

  // 朗读文本
  speak(text, options = {}) {
    if (!this.enabled || !text) return

    // 停止之前的朗读
    this.stop()

    const utterance = new SpeechSynthesisUtterance(text)
    
    if (this.voice) {
      utterance.voice = this.voice
    }
    utterance.rate = options.rate || this.rate
    utterance.pitch = options.pitch || this.pitch
    utterance.volume = options.volume || this.volume

    // 设置语言
    utterance.lang = options.lang || (this.voice?.lang || 'zh-CN')

    utterance.onstart = () => logger.debug('[Speech] 开始朗读')
    utterance.onend = () => logger.debug('[Speech] 朗读完成')
    utterance.onerror = (e) => logger.error('[Speech] 朗读错误:', e)

    this.synth.speak(utterance)
  }

  // 停止朗读
  stop() {
    if (this.synth) {
      this.synth.cancel()
    }
  }

  // 暂停朗读
  pause() {
    if (this.synth) {
      this.synth.pause()
    }
  }

  // 继续朗读
  resume() {
    if (this.synth) {
      this.synth.resume()
    }
  }

  // 获取可用语音列表
  getVoices() {
    if (this.synth) {
      return this.synth.getVoices()
    }
    return []
  }

  // 设置语音
  setVoice(voiceName) {
    const voices = this.getVoices()
    const voice = voices.find(v => v.name === voiceName)
    if (voice) {
      this.voice = voice
      logger.info('[Speech] 已切换到语音:', voice.name)
    }
  }

  // 设置语速
  setRate(rate) {
    this.rate = Math.max(0.1, Math.min(10, rate))
  }

  // 设置音调
  setPitch(pitch) {
    this.pitch = Math.max(0, Math.min(2, pitch))
  }

  // 设置音量
  setVolume(volume) {
    this.volume = Math.max(0, Math.min(1, volume))
  }

  // 启用/禁用
  setEnabled(enabled) {
    this.enabled = enabled
    this.saveEnabledState(enabled)
    if (!enabled) {
      this.stop()
    }
    logger.info('[Speech] 语音输出已' + (enabled ? '启用' : '禁用'))
  }
}

// 创建全局单例
const speechService = new SpeechService()

export default speechService
