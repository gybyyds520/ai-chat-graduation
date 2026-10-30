import request from '@/utils/request'

/**
 * 管理端「AI 系统管理」API 封装（/api/aisystem）。
 * 全部端点后端要求 ADMIN 角色；响应为统一 R{code,message,data}，
 * request 拦截器已直接返回 R 体，调用处用 res.code === 200 判断。
 */

// 系统列表：data = { records, total } 或数组，由后端决定
export function getAiSystemList () {
  return request.get('/api/aisystem/list')
}

// 系统详情
export function getAiSystemDetail (id) {
  return request.get(`/api/aisystem/${id}`)
}

// 注册新系统：body { systemName, systemType, description, rateLimit }
export function registerAiSystem (data) {
  return request.post('/api/aisystem/register', data)
}

// 切换系统状态：body { status }（1=启用 0=禁用）
export function updateAiSystemStatus (id, status) {
  return request.put(`/api/aisystem/${id}/status`, { status })
}

// 更新速率限制：body { rateLimit }
export function updateAiSystemRatelimit (id, ratelimit) {
  return request.put(`/api/aisystem/${id}/ratelimit`, { rateLimit: ratelimit })
}

// 重新生成密钥
export function regenerateAiSystemKey (id) {
  return request.post(`/api/aisystem/${id}/regenerate-key`)
}

// 删除系统
export function deleteAiSystem (id) {
  return request.delete(`/api/aisystem/${id}`)
}

// 系统权限列表
export function getAiSystemPermissions (id) {
  return request.get(`/api/aisystem/${id}/permissions`)
}

// 授予权限
export function grantPermission (data) {
  return request.post('/api/aisystem/permission/grant', data)
}

// 撤销权限
export function revokePermission (permissionId) {
  return request.delete(`/api/aisystem/permission/${permissionId}`)
}

// Ollama 状态：data = { available, version, modelCount, models }
export function getOllamaStatus () {
  return request.get('/api/aisystem/ollama/status')
}

// Ollama 模型列表：data = [{ name, size, modifiedAt, digest }]
export function getOllamaModels () {
  return request.get('/api/aisystem/ollama/models')
}

// 拉取模型：body { name }
export function pullOllamaModel (name) {
  return request.post('/api/aisystem/ollama/pull', { name })
}

// 删除模型
export function deleteOllamaModel (name) {
  return request.delete(`/api/aisystem/ollama/models/${encodeURIComponent(name)}`)
}

// 重启 Ollama
export function restartOllama () {
  return request.post('/api/aisystem/ollama/restart')
}

// 读取 AI 全局配置
export function getAiConfig () {
  return request.get('/api/aisystem/config')
}

// 更新 AI 全局配置
export function updateAiConfig (data) {
  return request.put('/api/aisystem/config', data)
}

// 使用统计：data = { totalSystems, activeSystems, totalCalls, todayCalls, topSystems }
export function getAiSystemStats () {
  return request.get('/api/aisystem/stats')
}

export default {
  getAiSystemList,
  getAiSystemDetail,
  registerAiSystem,
  updateAiSystemStatus,
  updateAiSystemRatelimit,
  regenerateAiSystemKey,
  deleteAiSystem,
  getAiSystemPermissions,
  grantPermission,
  revokePermission,
  getOllamaStatus,
  getOllamaModels,
  pullOllamaModel,
  deleteOllamaModel,
  restartOllama,
  getAiConfig,
  updateAiConfig,
  getAiSystemStats
}
