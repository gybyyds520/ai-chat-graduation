/**
 * AI 精灵养成纯逻辑：关系等级阈值、称号映射。
 * 与后端 AiElfServiceImpl 中的常量保持一致。
 * 等级阈值：1级0、2级10、3级25、4级45、5级70、6级100（满级）。
 */

export const LEVEL_THRESHOLDS = [0, 10, 25, 45, 70, 100]
export const LEVEL_TITLES = ['陌生', '熟悉', '朋友', '好友', '亲密', '挚友']
export const MAX_LEVEL = LEVEL_THRESHOLDS.length

/**
 * 根据好感度计算关系等级（1-6）。
 * @param {number} affection 好感度 0-100
 * @returns {number} 等级 1-6
 */
export function levelFromAffection(affection) {
  const a = Number(affection) || 0
  let level = 1
  for (let i = 0; i < LEVEL_THRESHOLDS.length; i++) {
    if (a >= LEVEL_THRESHOLDS[i]) level = i + 1
  }
  return level
}

/** 等级(1-6) -> 称号；越界回退"陌生"/"挚友"。 */
export function titleOfLevel(level) {
  const idx = Math.max(0, Math.min(LEVEL_TITLES.length - 1, level - 1))
  return LEVEL_TITLES[idx]
}

/**
 * 根据好感度计算称号（便捷方法）。
 * @param {number} affection
 * @returns {string}
 */
export function titleFromAffection(affection) {
  return titleOfLevel(levelFromAffection(affection))
}

/**
 * 当前等级区间内进度 0-100（用于进度条）。
 * @param {number} affection
 * @param {number} nextLevelThreshold 下一等级所需好感度（后端返回 nextLevelAffection）
 * @returns {number} 0-100
 */
export function affectionProgress(affection) {
  const a = Math.max(0, Math.min(100, Number(affection) || 0))
  // 当前等级区间宽度
  const level = levelFromAffection(a)
  const lo = LEVEL_THRESHOLDS[level - 1]
  const hi = LEVEL_THRESHOLDS[level] !== undefined ? LEVEL_THRESHOLDS[level] : 100
  if (hi === lo) return 100
  return Math.round(((a - lo) / (hi - lo)) * 100)
}
