/**
 * 多音色配置（与后端 VoiceProfile 枚举保持一致）。
 *
 * 预设 7 种音色：default/warm/cold/young/old/female/male。
 * 供语音通话面板下拉框与设置页音色偏好使用。
 */

export const DEFAULT_VOICE_ID = 'default'

export const VOICE_PROFILES = [
  { voiceId: 'default', name: '默认音色', description: '通用对话', scene: '通用聊天场景' },
  { voiceId: 'warm', name: '温暖柔和', description: '温暖治愈', scene: '情感陪伴、夜间闲聊' },
  { voiceId: 'cold', name: '冷静沉稳', description: '理性客观', scene: '知识问答、工作助手' },
  { voiceId: 'young', name: '年轻活力', description: '轻快活泼', scene: '轻松闲聊、娱乐场景' },
  { voiceId: 'old', name: '成熟稳重', description: '低沉厚重', scene: '正式对话、历史讲述' },
  { voiceId: 'female', name: '温柔女声', description: '亲和细腻', scene: '陪伴对话、朗读' },
  { voiceId: 'male', name: '沉稳男声', description: '稳重可靠', scene: '播报、正式场合' }
]

/**
 * 按 voiceId 查找音色；未找到或为空返回 DEFAULT。
 */
export function getVoiceProfile (voiceId) {
  if (!voiceId || typeof voiceId !== 'string') {
    return VOICE_PROFILES[0]
  }
  const normalized = voiceId.trim()
  return VOICE_PROFILES.find(v => v.voiceId === normalized) || VOICE_PROFILES[0]
}

/**
 * 判断 voiceId 是否合法。
 */
export function isValidVoiceId (voiceId) {
  if (!voiceId || typeof voiceId !== 'string') {
    return false
  }
  return VOICE_PROFILES.some(v => v.voiceId === voiceId.trim())
}

/**
 * 从 localStorage 读取用户音色偏好（未设置时返回 default）。
 */
export function loadVoicePreference () {
  const saved = localStorage.getItem('voice-preference')
  return isValidVoiceId(saved) ? saved : DEFAULT_VOICE_ID
}

/**
 * 持久化用户音色偏好。
 */
export function saveVoicePreference (voiceId) {
  if (isValidVoiceId(voiceId)) {
    localStorage.setItem('voice-preference', voiceId)
  }
}
