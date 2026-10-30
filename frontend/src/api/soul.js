import request from '@/utils/request'

/**
 * AI 灵魂引擎 API 封装（/api/soul）。
 * 后端所有端点均 requireUserId，归属校验在服务端完成。
 */

// 灵魂状态快照（含演化版本/心情/态度/性格权重/信念/统计）
export function getSoulOverview(characterId) {
  return request.get(`/api/soul/${characterId}`)
}

// 灵魂经历流水（分页，按时间倒序）
export function listSoulExperiences(characterId, params) {
  return request.get(`/api/soul/${characterId}/experiences`, { params })
}

// 重置灵魂：清空经历并恢复初始状态
export function resetSoul(characterId) {
  return request.post(`/api/soul/${characterId}/reset`)
}

// 手动触发一次灵魂演化（演示/测试）
export function evolveSoul(characterId, data) {
  return request.post(`/api/soul/${characterId}/evolve`, data)
}

// 灵魂注入预览：查看当前灵魂以什么形态注入 prompt
export function getSoulPrompt(characterId) {
  return request.get(`/api/soul/${characterId}/prompt`)
}
