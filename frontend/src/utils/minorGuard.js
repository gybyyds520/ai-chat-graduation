/**
 * 未成年保护 / 年龄声明纯逻辑（便于单元测试，不依赖 DOM / 网络）。
 *
 * 与后端 MinorProtectionServiceImpl 规则保持一致：
 * - 成年门槛 ADULT_AGE = 18；
 * - 夜间窗口 NIGHT_START = 22:00，NIGHT_END = 06:00；
 * - 年龄自声明范围 0-120（0/未声明 = 不触发未成年保护）。
 *
 * 前端只负责：注册时收集年龄声明、给出友好提示、夜间拦截提示文案；
 * 真正的拦截由后端 MinorProtectionService 强制执行，前端仅做体验层提示。
 */

export const ADULT_AGE = 18
export const MIN_AGE = 0
export const MAX_AGE = 120
export const NIGHT_START_HOUR = 22
export const NIGHT_END_HOUR = 6

/**
 * 校验并分类自声明年龄。
 * @param {number|string|null|undefined} rawAge 注册表单中的年龄
 * @returns {{age:number|null, valid:boolean, inRange:boolean, isMinor:boolean, message:string}}
 *   - age: 归一化后的整数年龄（无法解析时为 null）
 *   - valid: 是否通过前端校验（可提交给后端）
 *   - inRange: 是否落在 0-120
 *   - isMinor: 是否判定为未成年人（1-17）
 *   - message: 给用户的提示文案
 */
export function classifyDeclaredAge(rawAge) {
  // 未填写：视为不声明，按成年用户处理（与后端 age=null 语义一致）
  if (rawAge === null || rawAge === undefined || rawAge === '') {
    return {
      age: null,
      valid: true,
      inRange: true,
      isMinor: false,
      message: ''
    }
  }

  const age = Number(rawAge)
  if (!Number.isInteger(age) || Number.isNaN(age)) {
    return {
      age: null,
      valid: false,
      inRange: false,
      isMinor: false,
      message: '年龄必须是整数'
    }
  }

  if (age < MIN_AGE || age > MAX_AGE) {
    return {
      age,
      valid: false,
      inRange: false,
      isMinor: false,
      message: `年龄需在 ${MIN_AGE}-${MAX_AGE} 之间`
    }
  }

  // 0 视为未声明
  if (age === 0) {
    return {
      age: null,
      valid: true,
      inRange: true,
      isMinor: false,
      message: ''
    }
  }

  const isMinor = age < ADULT_AGE
  return {
    age,
    valid: true,
    inRange: true,
    isMinor,
    message: isMinor
      ? '您声明为未成年人，注册后将自动开启未成年保护模式（夜间 22:00-06:00 不可使用，内容加强过滤）。'
      : ''
  }
}

/**
 * 判断给定小时是否处于未成年夜间禁言窗口 [22:00, 24:00) ∪ [00:00, 06:00)。
 * @param {number} hour 0-23
 * @returns {boolean}
 */
export function isNightRestrictedHour(hour) {
  const h = Number(hour)
  if (!Number.isInteger(h) || h < 0 || h > 23) return false
  return h >= NIGHT_START_HOUR || h < NIGHT_END_HOUR
}

/**
 * 判断当前时刻是否处于夜间窗口（默认用本机时间）。
 * @param {Date} [now]
 * @returns {boolean}
 */
export function isNowNightRestricted(now = new Date()) {
  return isNightRestrictedHour(now.getHours())
}

/**
 * 根据后端 minor-mode/status 返回结构，生成友好的状态说明文案。
 * @param {{isMinor?:boolean, minorModeActive?:boolean, nightRestricted?:boolean,
 *          nightStartTime?:string, nightEndTime?:string}} status
 * @returns {{active:boolean, text:string}}
 */
export function describeMinorModeStatus(status = {}) {
  if (!status || !status.minorModeActive) {
    return { active: false, text: '未成年保护模式未开启' }
  }
  if (status.nightRestricted) {
    return {
      active: true,
      text: `未成年保护模式已开启：当前为夜间时段（${status.nightStartTime || '22:00'}-${status.nightEndTime || '06:00'}），AI 对话与夜间使用已被限制。`
    }
  }
  return {
    active: true,
    text: `未成年保护模式已开启：敏感内容加强过滤，夜间 ${status.nightStartTime || '22:00'}-${status.nightEndTime || '06:00'} 不可使用。`
  }
}

export default {
  ADULT_AGE,
  classifyDeclaredAge,
  isNightRestrictedHour,
  isNowNightRestricted,
  describeMinorModeStatus
}
