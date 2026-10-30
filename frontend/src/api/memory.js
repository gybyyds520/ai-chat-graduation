import request from '@/utils/request'

/**
 * AI 记忆 API 封装（/api/memory）。
 * 后端所有端点均 requireUserId，归属校验在服务端完成。
 */

// 分页查询记忆：page/size/memoryType/category/isActive
export function listMemories(params) {
  return request.get('/api/memory/list', { params })
}

// 记忆统计
export function getMemoryStats() {
  return request.get('/api/memory/stats')
}

// 记忆详情
export function getMemoryDetail(memoryId) {
  return request.get(`/api/memory/${memoryId}`)
}

// 创建记忆：{ content, source?, memoryType?, importance?, category? }
export function createMemory(data) {
  return request.post('/api/memory', data)
}

// 更新记忆：{ content?, memoryType?, importance?, category?, isActive? }
export function updateMemory(memoryId, data) {
  return request.put(`/api/memory/${memoryId}`, data)
}

// 删除记忆
export function deleteMemory(memoryId) {
  return request.delete(`/api/memory/${memoryId}`)
}

// 切换生效状态
export function toggleMemory(memoryId) {
  return request.post(`/api/memory/${memoryId}/toggle`)
}

// ---------- M1 / A3：记忆可视化管理约定路径 ----------

// 记忆条目列表（分页）：/api/memory/items
export function listMemoryItems(params) {
  return request.get('/api/memory/items', { params })
}

// 编辑单条记忆：PUT /api/memory/items/{id}
export function updateMemoryItem(memoryId, data) {
  return request.put(`/api/memory/items/${memoryId}`, data)
}

// 删除单条记忆：DELETE /api/memory/items/{id}
export function deleteMemoryItem(memoryId) {
  return request.delete(`/api/memory/items/${memoryId}`)
}

// 读取记忆全局开关：GET /api/memory/settings
export function getMemorySettings() {
  return request.get('/api/memory/settings')
}

// 更新记忆全局开关：PUT /api/memory/settings  body:{enabled}
export function updateMemorySettings(enabled) {
  return request.put('/api/memory/settings', { enabled })
}

// 对话中钉选消息加入长期记忆：POST /api/memory/pin  body:{content, source?}
export function pinMessageToMemory(data) {
  return request.post('/api/memory/pin', data)
}

// ---------- 市场级升级：分类浏览 / 批量删除 / 导出 ----------

// 按分类浏览记忆：GET /api/memory/items?category=FACT|PREFERENCE|EVENT|RELATIONSHIP
export function listMemoryItemsByCategory(params) {
  return request.get('/api/memory/items', { params })
}

// 批量删除记忆：DELETE /api/memory/batch  body:{ ids:[...] }
export function batchDeleteMemories(ids) {
  return request.delete('/api/memory/batch', { data: { ids } })
}

// 导出当前用户全部生效记忆：GET /api/memory/export -> JSON 数组
export function exportMemories() {
  return request.get('/api/memory/export')
}
