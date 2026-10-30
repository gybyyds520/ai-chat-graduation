/**
 * 语音消息气泡与通话波形纯逻辑（便于单元测试）。
 *
 * - 语音气泡时长格式化（秒 -> "0:05" / "1:23"）；
 * - 生成波形柱状图高度数组（确定性伪随机，按 seed 稳定）。
 */

/**
 * 语音时长格式化为 mm:ss。
 * @param {number} seconds 秒
 * @returns {string}
 */
export function formatVoiceDuration(seconds) {
  const s = Number(seconds)
  if (isNaN(s) || s <= 0) return '0:00'
  const total = Math.round(s)
  const m = Math.floor(total / 60)
  const sec = total % 60
  return `${m}:${String(sec).padStart(2, '0')}`
}

/**
 * 生成通话/语音波形柱高数组。
 * 用确定性伪随机（mulberry32）按 seed 生成，保证同一音频每次渲染高度一致，
 * 便于 CSS 动画与单元测试断言。
 *
 * @param {number} count 柱子数量，默认 24
 * @param {number} seed 随机种子（可用音频时长/url 字符串散列），默认 42
 * @param {number} minHeight 最小高度 px，默认 6
 * @param {number} maxHeight 最大高度 px，默认 28
 * @returns {number[]}
 */
export function waveformHeights(count = 24, seed = 42, minHeight = 6, maxHeight = 28) {
  const n = Math.max(1, Math.min(120, Math.floor(count) || 24))
  const range = Math.max(1, maxHeight - minHeight)
  const rng = mulberry32(Math.floor(seed) || 1)
  const heights = []
  for (let i = 0; i < n; i++) {
    // 中间略高、两侧略低，模拟语音波形包络
    const envelope = 1 - Math.abs(i - (n - 1) / 2) / ((n - 1) / 2 + 1)
    const base = rng() * range
    const h = minHeight + base * (0.4 + 0.6 * envelope)
    heights.push(Math.round(h))
  }
  return heights
}

/**
 * 由字符串生成稳定数值种子（简单散列）。
 * @param {string} str
 * @returns {number}
 */
export function seedFromString(str) {
  const s = String(str == null ? '' : str)
  let h = 2166136261
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}

/**
 * mulberry32 确定性 PRNG。
 */
function mulberry32(seed) {
  let a = seed >>> 0
  return function () {
    a |= 0
    a = (a + 0x6D2B79F5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

export default {
  formatVoiceDuration,
  waveformHeights,
  seedFromString
}
