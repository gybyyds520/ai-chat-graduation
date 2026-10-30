/**
 * useElfVoiceLoop.js — 桌面精灵语音联动闭环 composable
 *
 * 闭环：点击精灵 → 麦克风采集（Web Speech STT）→ 文本发送后端聊天 SSE →
 *       累积 AI 回复文本 → Edge TTS 合成音频播放。
 *
 * 能力：
 *  - barge-in：TTS 播放期间持续监听麦克风能量，检测到人声立即停止播放并重新开始识别
 *  - 语速/音量：TTS 播放端通过 HTMLAudioElement.playbackRate / volume 调节
 *  - 全程惰性能力探测（getUserMedia / SpeechRecognition / AudioContext），
 *    不支持时静默降级为文字对话，不抛错。
 */

import { ref } from 'vue'
import speechRecognitionService from '@/utils/speechRecognition'
import { buildChatStreamUrl, parseSSEBuffer, getOfflineReply } from '@/utils/elfChatSse'

export function useElfVoiceLoop(options = {}) {
  const isListening = ref(false)
  const isSpeaking = ref(false)
  const interimTranscript = ref('')
  const supported = ref(
    !!(window.SpeechRecognition || window.webkitSpeechRecognition) &&
    !!navigator.mediaDevices?.getUserMedia
  )

  // 语速/音量（前端配置面板滑块驱动）
  const speed = ref(options.speed ?? 1.0)
  const volume = ref(options.volume ?? 1.0)
  const voice = ref(options.voice || undefined)

  let audioEl = null
  let audioCtx = null
  let analyser = null
  let micStream = null
  let bargeInRaf = 0
  let bargeThreshold = 0.04 // RMS 阈值，超过视为有人声
  let speakingToken = 0

  const onFinalReply = options.onReply || (() => {})
  const onStateChange = options.onStateChange || (() => {})

  function notify(state) {
    try { onStateChange(state) } catch (e) { /* 忽略回调异常 */ }
  }

  /** 停止 TTS 播放（barge-in 或手动停止） */
  function stopSpeaking() {
    speakingToken++
    if (audioEl) {
      try { audioEl.pause() } catch (e) {}
      audioEl = null
    }
    if (bargeInRaf) {
      cancelAnimationFrame(bargeInRaf)
      bargeInRaf = 0
    }
    isSpeaking.value = false
    notify('idle')
  }

  /** 用麦克风能量做 barge-in 检测：检测到人声立即打断 */
  async function startBargeInWatch() {
    try {
      if (!audioCtx) {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)()
      }
      if (!micStream) {
        micStream = await navigator.mediaDevices.getUserMedia({ audio: true })
      }
      const src = audioCtx.createMediaStreamSource(micStream)
      analyser = audioCtx.createAnalyser()
      analyser.fftSize = 512
      src.connect(analyser)
      const buf = new Float32Array(analyser.fftSize)
      const token = speakingToken
      const loop = () => {
        if (token !== speakingToken) return // 已被打断/停止
        analyser.getFloatTimeDomainData(buf)
        let sum = 0
        for (let i = 0; i < buf.length; i++) sum += buf[i] * buf[i]
        const rms = Math.sqrt(sum / buf.length)
        if (rms > bargeThreshold) {
          // 人声打断：停 TTS → 重新识别
          stopSpeaking()
          startListening()
          return
        }
        bargeInRaf = requestAnimationFrame(loop)
      }
      loop()
    } catch (e) {
      // 无麦克风权限则不做 barge-in
    }
  }

  /** 播放 TTS 音频 blob */
  async function speakAudio(text) {
    if (!text) return
    stopSpeaking()
    const token = ++speakingToken
    isSpeaking.value = true
    notify('speaking')
    try {
      const body = { text, voice: voice.value }
      const resp = await fetch('/api/voice/tts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      })
      if (!resp.ok) throw new Error('tts failed')
      const blob = await resp.blob()
      if (token !== speakingToken) return // 期间被打断
      const url = URL.createObjectURL(blob)
      audioEl = new Audio(url)
      audioEl.playbackRate = speed.value
      audioEl.volume = volume.value
      audioEl.onended = () => {
        URL.revokeObjectURL(url)
        if (token === speakingToken) {
          isSpeaking.value = false
          // 说完自动继续听（连续对话）
          startListening()
        }
      }
      await audioEl.play()
      // 启动打断检测
      startBargeInWatch()
    } catch (e) {
      // TTS 不可用：静默降级，不影响文字闭环
      isSpeaking.value = false
      startListening()
    }
  }

  /** 把识别文本发送后端聊天 SSE，累积回复后朗读 */
  async function sendToChat(text) {
    if (!text || !text.trim()) return
    notify('thinking')
    let reply = ''
    try {
      const url = buildChatStreamUrl({
        sessionId: options.sessionId || 'elf-voice',
        userId: options.userId || '0',
        characterId: options.characterId || '0',
        message: text
      })
      const resp = await fetch(url)
      if (!resp.ok || !resp.body) throw new Error('chat stream unavailable')
      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const { events, remaining } = parseSSEBuffer(buffer)
        buffer = remaining
        for (const ev of events) {
          if (ev.type === 'message') reply += ev.data
        }
      }
    } catch (e) {
      reply = getOfflineReply(text)
    }
    const final = reply.trim() || getOfflineReply(text)
    onFinalReply(final)
    speakAudio(final)
  }

  /** 开始语音识别 */
  function startListening() {
    if (!supported.value) return
    stopSpeaking()
    speechRecognitionService.init()
    if (!speechRecognitionService.isSupported()) {
      supported.value = false
      return
    }
    speechRecognitionService.onResult = (transcript, isFinal) => {
      interimTranscript.value = transcript
      if (isFinal) {
        isListening.value = false
        interimTranscript.value = ''
        sendToChat(transcript)
      }
    }
    speechRecognitionService.onend = () => {
      isListening.value = false
    }
    if (speechRecognitionService.start()) {
      isListening.value = true
      notify('listening')
    }
  }

  /** 一键进入语音对话闭环 */
  function start() {
    stopSpeaking()
    startListening()
  }

  /** 完全停止并释放麦克风 */
  function stop() {
    stopSpeaking()
    speechRecognitionService.abort()
    isListening.value = false
    if (micStream) {
      micStream.getTracks().forEach((t) => t.stop())
      micStream = null
    }
    if (audioCtx) {
      audioCtx.close().catch(() => {})
      audioCtx = null
    }
  }

  function setSpeed(v) { speed.value = Math.max(0.5, Math.min(2, v)) }
  function setVolume(v) { volume.value = Math.max(0, Math.min(1, v)) }

  return {
    isListening,
    isSpeaking,
    interimTranscript,
    supported,
    speed,
    volume,
    start,
    stop,
    startListening,
    stopSpeaking,
    setSpeed,
    setVolume,
    speakAudio,
    sendToChat
  }
}
