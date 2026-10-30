import request from '@/utils/request'

// M3-A5：当前用户每日额度使用情况
// data = { membershipLevel, features: { ai_chat: {used,limit,remaining}, voice_call:..., image_gen:..., doc_upload:... } }
export function getCurrentQuota () {
  return request.get('/api/quota/current')
}

// M3-A9：删除整段 AI 对话（清空该会话消息），前端需二次确认
export function deleteChatSession (sessionId) {
  return request.delete(`/api/chat/session/${sessionId}`)
}

// M3-A9：删除单条 AI 对话消息
export function deleteChatMessage (messageId) {
  return request.delete(`/api/chat/message/${messageId}`)
}

// M3-A9：导出我的全部个人数据（资料 + 对话 + 帖子，JSON）
export function exportMyData () {
  return request.get('/api/user/data/export')
}

export default {
  getCurrentQuota,
  deleteChatSession,
  deleteChatMessage,
  exportMyData
}
