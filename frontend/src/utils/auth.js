/**
 * 当前登录用户工具。
 *
 * 登录成功后前端仅持久化 `token` 与 `user`（JSON 字符串，见 LoginView.handleLogin），
 * 并未单独持久化 `userId`。历史页面直接 `localStorage.getItem('userId') || '1'`
 * 会取到默认值 1 导致与真实登录用户不符、后端 401 强制跳登录页（通知/隐私设置页 bug 根因）。
 * 统一通过本工具从 `user` 对象解析当前用户信息。
 */

/**
 * 解析当前登录用户对象（localStorage 'user' 的 JSON）。解析失败或缺失返回 null。
 */
export function getCurrentUser() {
  try {
    const raw = localStorage.getItem('user')
    if (!raw) return null
    const user = JSON.parse(raw)
    return user && typeof user === 'object' ? user : null
  } catch (e) {
    return null
  }
}

/**
 * 获取当前登录用户 ID；未登录或解析失败返回 null。
 */
export function getCurrentUserId() {
  const user = getCurrentUser()
  const id = user && (user.id !== undefined && user.id !== null ? user.id : user.userId)
  return id !== undefined && id !== null ? Number(id) : null
}

/**
 * 获取当前登录用户昵称；未登录或缺失返回 null。
 */
export function getCurrentUserNickname() {
  const user = getCurrentUser()
  return user ? (user.nickname || user.userName || null) : null
}

export default { getCurrentUser, getCurrentUserId, getCurrentUserNickname }
