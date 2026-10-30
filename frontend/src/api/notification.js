import request from '@/utils/request'

/**
 * 学生端「通知中心」API 封装（/api/notification）。
 * 响应为统一 R{code,message,data}，request 拦截器已直接返回 R 体，
 * 调用处用 res.code === 200 判断成功。
 */

// 通知列表（服务端分页 + 服务端类型筛选）
// userId: number, page: number, size: number, type: 'all'|'like'|'comment'|'share'|'system'
// 返回 data: { records: [...], total, page, size, hasMore }
export function getNotifications (userId, page = 1, size = 20, type = 'all') {
  const params = new URLSearchParams()
  params.append('userId', userId)
  params.append('page', page)
  params.append('size', size)
  if (type && type !== 'all') {
    params.append('type', type)
  }
  return request.get(`/api/notification/list?${params.toString()}`)
}

// 未读通知数：data = number
export function getUnreadCount (userId) {
  return request.get(`/api/notification/unread/count?userId=${userId}`)
}

// 标记单条已读
export function markAsRead (userId, notificationId) {
  return request.post(`/api/notification/read/${notificationId}?userId=${userId}`)
}

// 全部已读
export function markAllAsRead (userId) {
  return request.post(`/api/notification/read/all?userId=${userId}`)
}

// 删除单条通知
export function deleteNotification (userId, notificationId) {
  return request.delete(`/api/notification/${notificationId}?userId=${userId}`)
}

// 清空全部通知（批量软删除），返回 data: { success, deletedCount }
export function clearAllNotifications (userId) {
  return request.post(`/api/notification/clear-all?userId=${userId}`)
}

// 获取通知设置：data = { likeEnabled, commentEnabled, shareEnabled, systemEnabled }
export function getNotificationSettings (userId) {
  return request.get(`/api/notification/settings?userId=${userId}`)
}

// 更新通知设置，body: { likeEnabled, commentEnabled, shareEnabled, systemEnabled }
export function updateNotificationSettings (userId, settings) {
  return request.put(`/api/notification/settings?userId=${userId}`, settings)
}

export default {
  getNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
  deleteNotification,
  clearAllNotifications,
  getNotificationSettings,
  updateNotificationSettings
}
