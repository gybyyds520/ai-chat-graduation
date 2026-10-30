/**
 * 群聊运营纯逻辑（与 UI 解耦，便于单测）。
 * 覆盖：群信息编辑权限、踢人/转让权限、解散二次确认（输入群名）、公告置顶展示。
 */

/** 群成员角色 */
export const GROUP_ROLE = {
  OWNER: 'owner',
  ADMIN: 'admin',
  MEMBER: 'member'
}

/**
 * 是否可编辑群名称/头像/公告（仅群主）。
 * @param {string} role
 * @returns {boolean}
 */
export function canEditGroupInfo(role) {
  return role === GROUP_ROLE.OWNER
}

/**
 * 是否可踢人（群主或管理员）。
 * @param {string} role
 * @returns {boolean}
 */
export function canKickMember(role) {
  return role === GROUP_ROLE.OWNER || role === GROUP_ROLE.ADMIN
}

/**
 * 是否可转让群主（仅当前群主）。
 * @param {string} role
 * @returns {boolean}
 */
export function canTransferOwner(role) {
  return role === GROUP_ROLE.OWNER
}

/**
 * 解散群聊二次确认：必须输入与群名完全一致的文本。
 * @param {string} groupName 群名
 * @param {string} input 用户输入
 * @returns {boolean}
 */
export function confirmDisband(groupName, input) {
  const g = (groupName || '').trim()
  const i = (input || '').trim()
  return !!g && g === i
}

/**
 * 群公告置顶展示文案（截断，可关闭）。
 * @param {string} announcement
 * @param {number} maxLen
 * @returns {string}
 */
export function formatAnnouncement(announcement, maxLen = 60) {
  const a = (announcement || '').trim()
  if (!a) return ''
  const n = Number(maxLen) > 0 ? Number(maxLen) : 60
  return a.length > n ? a.slice(0, n) + '…' : a
}
