import logger from './logger.js'

// 语音识别服务
class SpeechRecognitionService {
  constructor() {
    this.recognition = null
    this.isListening = false
    this.enabled = false
    this.lang = 'zh-CN'
    this.interimResults = true
    this.maxAlternatives = 1
  }

  // 初始化语音识别
  init() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    
    if (SpeechRecognition) {
      this.recognition = new SpeechRecognition()
      this.recognition.continuous = false
      this.recognition.interimResults = this.interimResults
      this.recognition.lang = this.lang
      this.recognition.maxAlternatives = this.maxAlternatives
      
      this.recognition.onstart = () => {
        this.isListening = true
        logger.info('[ASR] 语音识别开始')
      }
      
      this.recognition.onresult = (event) => {
        const result = event.results[0]
        const transcript = result[0].transcript
        const isFinal = result.isFinal
        
        logger.debug(`[ASR] ${isFinal ? '最终' : '临时'}识别结果:`, transcript)
        
        if (this.onResult) {
          this.onResult(transcript, isFinal)
        }
      }
      
      this.recognition.onerror = (event) => {
        logger.error('[ASR] 识别错误:', event.error)
        this.isListening = false
        
        if (this.onError) {
          this.onError(event.error)
        }
      }
      
      this.recognition.onend = () => {
        this.isListening = false
        logger.info('[ASR] 语音识别结束')
        
        if (this.onEnd) {
          this.onEnd()
        }
      }
      
      this.enabled = true
      logger.info('[ASR] 语音识别引擎已就绪')
    } else {
      logger.warn('[ASR] 当前浏览器不支持语音识别')
      this.enabled = false
    }
  }

  // 开始监听
  start() {
    if (!this.enabled || !this.recognition) {
      logger.warn('[ASR] 语音识别未初始化')
      return false
    }
    
    if (this.isListening) {
      logger.warn('[ASR] 正在监听中')
      return false
    }
    
    try {
      this.recognition.start()
      return true
    } catch (error) {
      logger.error('[ASR] 启动失败:', error)
      return false
    }
  }

  // 停止监听
  stop() {
    if (this.recognition && this.isListening) {
      this.recognition.stop()
    }
  }

  // 中止监听
  abort() {
    if (this.recognition) {
      this.recognition.abort()
      this.isListening = false
    }
  }

  // 设置语言
  setLang(lang) {
    this.lang = lang
    if (this.recognition) {
      this.recognition.lang = lang
    }
  }

  // 设置是否返回临时结果
  setInterimResults(interim) {
    this.interimResults = interim
    if (this.recognition) {
      this.recognition.interimResults = interim
    }
  }

  // 获取支持的语言
  getSupportedLanguages() {
    if ('speechSynthesis' in window) {
      return speechSynthesis.getVoices().map(v => ({
        code: v.lang,
        name: v.name
      }))
    }
    return []
  }

  // 检查是否支持语音识别
  isSupported() {
    return !!(window.SpeechRecognition || window.webkitSpeechRecognition)
  }
}

// 创建全局单例
const speechRecognitionService = new SpeechRecognitionService()

export default speechRecognitionService
