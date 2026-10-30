import { describe, it, expect } from 'vitest'
import { generateLocalAvatarSvg, generateAvatarUrl, VALID_STYLES } from '../../src/utils/localAvatar'

describe('localAvatar 本地头像生成器', () => {
  it('输出为 SVG data URI', () => {
    const out = generateLocalAvatarSvg('小明', 'avataaars')
    expect(out).toMatch(/^data:image\/svg\+xml;base64,/)
  })

  it('6 种预设风格全部可用且互相不同', () => {
    expect(VALID_STYLES.sort()).toEqual(
      ['avataaars', 'bottts', 'bottts-neutral', 'jdenticon', 'gridy', 'micah'].sort()
    )
    const outs = new Set(VALID_STYLES.map(s => generateLocalAvatarSvg('测试角色', s)))
    expect(outs.size).toBe(6)
  })

  it('同一 seed + 同一风格输出完全确定', () => {
    expect(generateLocalAvatarSvg('爱丽丝', 'bottts')).toBe(generateLocalAvatarSvg('爱丽丝', 'bottts'))
  })

  it('不同 seed 输出不同（颜色/形状由名字决定）', () => {
    const a = generateLocalAvatarSvg('角色甲', 'avataaars')
    const b = generateLocalAvatarSvg('角色乙', 'avataaars')
    expect(a).not.toBe(b)
  })

  it('未知风格回退为渐变字母头像（不抛错）', () => {
    const out = generateLocalAvatarSvg('某人', 'unknown-style')
    expect(out).toMatch(/^data:image\/svg\+xml;base64,/)
  })

  it('空 seed 不抛错', () => {
    expect(() => generateLocalAvatarSvg('')).not.toThrow()
    expect(() => generateLocalAvatarSvg(null)).not.toThrow()
    expect(() => generateLocalAvatarSvg(undefined)).not.toThrow()
  })

  it('generateAvatarUrl 兼容入口返回同一本地 URL', () => {
    expect(generateAvatarUrl('角色', 'gridy')).toBe(generateLocalAvatarSvg('角色', 'gridy'))
    // 默认风格
    expect(generateAvatarUrl('角色')).toBe(generateLocalAvatarSvg('角色', 'avataaars'))
  })
})
