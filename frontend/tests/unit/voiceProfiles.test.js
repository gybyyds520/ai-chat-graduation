import { describe, it, expect, beforeEach } from 'vitest'
import {
  VOICE_PROFILES,
  DEFAULT_VOICE_ID,
  getVoiceProfile,
  isValidVoiceId,
  loadVoicePreference,
  saveVoicePreference
} from '../../src/utils/voiceProfiles'

describe('voiceProfiles 多音色配置', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('应至少提供 5 种预设音色', () => {
    expect(VOICE_PROFILES.length).toBeGreaterThanOrEqual(5)
  })

  it('getVoiceProfile 空入参应返回默认音色', () => {
    expect(getVoiceProfile(null).voiceId).toBe(DEFAULT_VOICE_ID)
    expect(getVoiceProfile('').voiceId).toBe(DEFAULT_VOICE_ID)
  })

  it('getVoiceProfile 未知音色应降级默认', () => {
    expect(getVoiceProfile('robot-x').voiceId).toBe(DEFAULT_VOICE_ID)
  })

  it('getVoiceProfile 合法音色应正确匹配', () => {
    expect(getVoiceProfile('warm').voiceId).toBe('warm')
    expect(getVoiceProfile('female').name).toBeTruthy()
  })

  it('isValidVoiceId 应正确判断', () => {
    expect(isValidVoiceId('warm')).toBe(true)
    expect(isValidVoiceId('male')).toBe(true)
    expect(isValidVoiceId(null)).toBe(false)
    expect(isValidVoiceId('nope')).toBe(false)
  })

  it('save/loadVoicePreference 应持久化合法音色', () => {
    saveVoicePreference('cold')
    expect(loadVoicePreference()).toBe('cold')
  })

  it('loadVoicePreference 非法存储应回退默认', () => {
    localStorage.setItem('voice-preference', 'hacker')
    expect(loadVoicePreference()).toBe(DEFAULT_VOICE_ID)
  })
})
