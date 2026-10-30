/**
 * 登录 / 注册表单校验（纯函数，便于单测）。
 *
 * 历史上这些校验内联在 LoginView / RegisterView 中，无法被单测覆盖。
 * 这里把"成功/失败/异常"三分支与提示文案抽成纯函数，视图只负责触发与提示。
 * 文案与原内联实现保持一致，避免视觉回归。
 */

export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export const USERNAME_MIN = 3
export const USERNAME_MAX = 20
export const PASSWORD_MIN = 8
export const PASSWORD_MAX = 20

/** 邮箱格式是否合法。空串视为不合法。 */
export function isValidEmail(email) {
  if (email === null || email === undefined) return false
  return EMAIL_RE.test(String(email).trim())
}

/** 密码强度：必须同时包含字母和数字。 */
export function isStrongPassword(password) {
  if (!password) return false
  return /[a-zA-Z]/.test(password) && /\d/.test(password)
}

/** 用户名长度是否在 3-20 之间。 */
export function isUsernameLengthValid(username) {
  if (!username) return false
  const len = String(username).length
  return len >= USERNAME_MIN && len <= USERNAME_MAX
}

/**
 * 登录前校验：用户名/密码非空。
 * @returns {{valid: boolean, message: string}} valid=false 时 message 为提示文案
 */
export function validateLoginCredentials({ username, password } = {}) {
  if (!username || !password) {
    return { valid: false, message: '请填写用户名/邮箱和密码' }
  }
  return { valid: true, message: '' }
}

/**
 * 注册前校验。
 * @param {{username:string,email:string,password:string,confirmPassword:string,agreed:boolean}} form
 * @param {{valid:boolean,message:string,age:number|null}} [ageCheck] 来自 minorGuard.classifyDeclaredAge
 * @returns {{valid: boolean, message: string}}
 */
export function validateRegisterInput(form = {}, ageCheck = { valid: true, message: '', age: null }) {
  const { username = '', email = '', password = '', confirmPassword = '', agreed = false } = form

  if (!agreed) {
    return { valid: false, message: '请先阅读并同意《用户协议》和《隐私政策》' }
  }

  if (!username || !email || !password || !confirmPassword) {
    return { valid: false, message: '请填写所有必填字段' }
  }

  if (!isUsernameLengthValid(username)) {
    return { valid: false, message: '用户名长度必须在3-20个字符之间' }
  }

  if (!isValidEmail(email)) {
    return { valid: false, message: '请输入有效的邮箱地址' }
  }

  const pwdLen = String(password).length
  if (pwdLen < PASSWORD_MIN || pwdLen > PASSWORD_MAX) {
    return { valid: false, message: '密码长度必须在8-20个字符之间' }
  }

  if (!isStrongPassword(password)) {
    return { valid: false, message: '密码必须包含字母和数字' }
  }

  if (password !== confirmPassword) {
    return { valid: false, message: '两次输入的密码不一致' }
  }

  if (!ageCheck.valid) {
    return { valid: false, message: ageCheck.message || '年龄输入无效' }
  }

  return { valid: true, message: '' }
}

export default {
  EMAIL_RE,
  isValidEmail,
  isStrongPassword,
  isUsernameLengthValid,
  validateLoginCredentials,
  validateRegisterInput
}
