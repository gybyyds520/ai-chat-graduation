import {
  formatVoiceDuration,
  waveformHeights,
  seedFromString
} from '../../src/utils/voiceBubble'

describe('voiceBubble 语音气泡与波形', () => {
  it('formatVoiceDuration 秒转 mm:ss', () => {
    expect(formatVoiceDuration(5)).toBe('0:05')
    expect(formatVoiceDuration(83)).toBe('1:23')
    expect(formatVoiceDuration(0)).toBe('0:00')
    expect(formatVoiceDuration(-3)).toBe('0:00')
    expect(formatVoiceDuration(3661)).toBe('61:01')
  })

  it('waveformHeights 返回指定数量柱子', () => {
    const h = waveformHeights(24, 42)
    expect(h.length).toBe(24)
  })

  it('waveformHeights 高度落在 [min,max] 区间', () => {
    const h = waveformHeights(30, 7, 6, 28)
    for (const v of h) {
      expect(v).toBeGreaterThanOrEqual(4)
      expect(v).toBeLessThanOrEqual(30)
    }
  })

  it('waveformHeights 同种子结果确定性一致', () => {
    const a = waveformHeights(20, 99)
    const b = waveformHeights(20, 99)
    expect(a).toEqual(b)
  })

  it('waveformHeights 不同种子结果不同', () => {
    const a = waveformHeights(20, 1)
    const b = waveformHeights(20, 2)
    expect(a).not.toEqual(b)
  })

  it('waveformHeights count 钳制', () => {
    expect(waveformHeights(0).length).toBe(24) // 0 回退默认
    expect(waveformHeights(-5).length).toBe(1) // 负数钳到最小 1
    expect(waveformHeights(9999).length).toBe(120)
  })

  it('seedFromString 稳定且非负', () => {
    expect(seedFromString('abc')).toBe(seedFromString('abc'))
    expect(seedFromString('abc')).toBeGreaterThanOrEqual(0)
    expect(seedFromString('')).toBeGreaterThanOrEqual(0)
  })
})
