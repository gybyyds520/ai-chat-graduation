/**
 * 实时语音通话客户端（半双工轮次管道）。
 *
 * 状态机：idle -> recording -> processing -> playing -> recording -> ... -> idle
 * 一轮流程：startVoiceCall 开启会话 -> recordAudio 录音 -> sendAudio 上传
 *          -> 后端返回 userText/aiText/audioBase64 -> playAudio 播放 -> 自动回到 recording。
 *
 * 设计：网络请求通过构造注入的 http 函数完成，便于在单元测试中替换为 mock；
 *       状态转换与 base64 处理为纯逻辑，可独立测试。
 */

import request from './request'

// 通话状态
export const VOICE_STATE = {
  IDLE: 'idle',
  RECORDING: 'recording',
  PROCESSING: 'processing',
  PLAYING: 'playing'
}

// 单段音频大小上限 10MB（与后端一致）
const MAX_AUDIO_BYTES = 10 * 1024 * 1024

export class VoiceCallClient {
  /**
   * @param {object} options
   * @param {Function} options.http  注入的请求函数（返回 Promise<R>），默认用 axios request
   * @param {object} options.mediaRecorderFactory  注入 MediaRecorder 工厂，默认用浏览器原生
   */
  constructor (options = {}) {
    this.http = options.http || request
    this.mediaRecorderFactory = options.mediaRecorderFactory || null
    this.reset()
  }

  reset () {
    this.sessionId = null
    this.state = VOICE_STATE.IDLE
    this.adapterInfo = null
    this.mediaRecorder = null
    this.audioChunks = []
    this.currentAudio = null
    this.onStateChange = null
  }

  setState (next) {
    this.state = next
    if (typeof this.onStateChange === 'function') {
      this.onStateChange(next)
    }
  }

  /** 开启语音会话。 */
  async startVoiceCall (characterId) {
    const resp = await this.http.post('/api/voice-call/start', { characterId })
    const data = resp && resp.data ? resp.data : resp
    this.sessionId = data.sessionId
    this.adapterInfo = data.adapterInfo || null
    this.setState(VOICE_STATE.RECORDING)
    return this.sessionId
  }

  /** 结束语音会话。 */
  async endVoiceCall () {
    try {
      // 停止正在播放的回复音频
      if (this.currentAudio) {
        try { this.currentAudio.pause() } catch (e) { /* ignore */ }
        this.currentAudio = null
      }
      if (this.sessionId) {
        await this.http.post(`/api/voice-call/${this.sessionId}/end`, {})
      }
    } finally {
      this.stopRecording()
      this.reset()
    }
  }

  /**
   * 发送文本轮次：跳过后端 ASR，直接 LLM -> TTS。
   * 配合浏览器语音识别（Web Speech API）转写文本使用，真实可用、无需后端 ASR。
   */
  async sendText (text) {
    if (!this.sessionId) {
      throw new Error('语音会话未开启')
    }
    if (!text || !String(text).trim()) {
      throw new Error('语音文本为空')
    }
    this.setState(VOICE_STATE.PROCESSING)
    const resp = await this.http.post(`/api/voice-call/${this.sessionId}/text`, { text: String(text).trim() })
    const data = resp && resp.data ? resp.data : resp
    this.setState(VOICE_STATE.PLAYING)
    return data
  }

  /** 浏览器是否支持 Web Speech API 语音识别（Chrome/Edge 内置，无需密钥）。 */
  isBrowserAsrSupported () {
    return typeof window !== 'undefined' &&
      !!(window.SpeechRecognition || window.webkitSpeechRecognition)
  }

  /**
   * 启动浏览器语音识别（实时转写，中文）。
   * @param {Function} onText 每次识别结果回调（最终 + 临时文本）
   * @param {object} options { lang, onError }
   * @returns {Function} stop 函数：调用后返回 Promise，resolve 为最终转写文本
   *                     （等待 onend/最终结果合并，修复"停止后读不到最后一句"的时序 Bug）
   */
  startBrowserAsr (onText, options = {}) {
    if (typeof window === 'undefined') {
      throw new Error('当前环境不支持语音识别')
    }
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!SR) {
      throw new Error('当前浏览器不支持语音识别')
    }
    const onError = typeof options.onError === 'function' ? options.onError : null
    const rec = new SR()
    rec.lang = options.lang || 'zh-CN'
    rec.interimResults = true
    rec.continuous = true
    let finalText = ''
    let stopped = false
    let resolveStop = null
    // stop 时返回的 Promise：在 onend（最终结果已合并）时 resolve 完整文本
    const stopPromise = new Promise((resolve) => { resolveStop = resolve })
    const settle = () => { if (resolveStop) { resolveStop(finalText); resolveStop = null } }

    rec.onresult = (e) => {
      let interim = ''
      for (let i = e.resultIndex; i < e.results.length; i++) {
        const r = e.results[i]
        if (r.isFinal) finalText += r[0].transcript
        else interim += r[0].transcript
      }
      if (typeof onText === 'function') onText(finalText + interim)
    }
    rec.onerror = (e) => {
      if (onError) {
        if (e.error === 'not-allowed' || e.error === 'service-not-allowed') {
          onError('麦克风权限被拒绝，请在浏览器地址栏允许麦克风访问后重试')
        } else if (e.error === 'no-speech') {
          onError('没有听到声音，请靠近麦克风再试')
        } else if (e.error === 'audio-capture') {
          onError('未检测到麦克风设备，请检查麦克风连接')
        } else {
          onError('语音识别失败：' + e.error)
        }
      }
      // 识别终止时也结算最终文本（保证 stop 调用方不被挂起）
      if (stopped) settle()
    }
    rec.onend = () => {
      // stop() 触发 onend 时，最后一句 final 结果已合并完毕
      if (stopped) settle()
    }
    rec.start()
    return () => {
      stopped = true
      try { rec.stop() } catch (e) { /* ignore */ }
      // 返回最终文本 Promise；若 onend 已触发过则直接 resolve
      return stopPromise
    }
  }

  /** 上传一段录音并走全链路。返回后端结果。 */
  async sendAudio (blob) {
    if (!this.sessionId) {
      throw new Error('语音会话未开启')
    }
    if (!blob || blob.size === 0) {
      throw new Error('音频为空')
    }
    if (blob.size > MAX_AUDIO_BYTES) {
      throw new Error('音频超过 10MB 限制')
    }
    this.setState(VOICE_STATE.PROCESSING)
    const form = new FormData()
    form.append('file', blob, 'voice.webm')
    const resp = await this.http.post(`/api/voice-call/${this.sessionId}/audio`, form)
    const data = resp && resp.data ? resp.data : resp
    this.setState(VOICE_STATE.PLAYING)
    return data
  }

  /**
   * 开始录音。返回一个 stop 函数，调用后返回录制好的 Blob。
   * 需要浏览器 MediaRecorder 与麦克风权限。
   */
  async recordAudio (mimeType = 'audio/webm') {
    if (this.state === VOICE_STATE.RECORDING) {
      return // 已在录音
    }
    this.audioChunks = []
    let stream
    if (this.mediaRecorderFactory) {
      this.mediaRecorder = this.mediaRecorderFactory()
    } else {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      this.mediaRecorder = new MediaRecorder(stream)
    }
    this.mediaRecorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) {
        this.audioChunks.push(e.data)
      }
    }
    this.mediaRecorder.start()
    this.setState(VOICE_STATE.RECORDING)

    return () => new Promise((resolve) => {
      this.mediaRecorder.onstop = () => {
        const blob = new Blob(this.audioChunks, { type: mimeType })
        this.audioChunks = []
        resolve(blob)
      }
      this.mediaRecorder.stop()
    })
  }

  /** 停止录音（不返回 blob，用于取消）。 */
  stopRecording () {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      try { this.mediaRecorder.stop() } catch (e) { /* ignore */ }
    }
  }

  /**
   * 播放 base64 音频。返回 HTMLAudioElement。
   * @param {string} base64Data base64 编码的音频
   * @param {string} format  音频格式（wav/mp3/mpeg），影响 MIME 类型
   */
  playAudio (base64Data, format = 'wav') {
    if (!base64Data) return null
    const blob = base64ToBlob(base64Data, formatToMime(format))
    const url = URL.createObjectURL(blob)
    if (this.currentAudio) {
      try { this.currentAudio.pause() } catch (e) { /* ignore */ }
    }
    const audio = new Audio(url)
    this.currentAudio = audio
    audio.onended = () => {
      // 播放完毕回到录音状态，继续下一轮
      this.setState(VOICE_STATE.RECORDING)
      URL.revokeObjectURL(url)
    }
    audio.play()
    return audio
  }
}

/**
 * base64 字符串转 Blob（纯函数，便于测试）。
 */
export function base64ToBlob (base64Data, mimeType = 'audio/wav') {
  const binary = atob(base64Data)
  const len = binary.length
  const bytes = new Uint8Array(len)
  for (let i = 0; i < len; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return new Blob([bytes], { type: mimeType })
}

/** 音频格式标识 -> MIME 类型。 */
export function formatToMime (format) {
  switch (format) {
    case 'mp3':
    case 'mpeg':
      return 'audio/mpeg'
    case 'wav':
    case 'wave':
      return 'audio/wav'
    case 'ogg':
      return 'audio/ogg'
    case 'webm':
      return 'audio/webm'
    default:
      return 'application/octet-stream'
  }
}

// 导出默认单例
const voiceCallClient = new VoiceCallClient()
export default voiceCallClient
