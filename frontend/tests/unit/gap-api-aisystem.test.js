import { vi, describe, it, expect, beforeEach } from 'vitest'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn()
}))

vi.mock('../../src/utils/request', () => ({
  default: {
    get: (...a) => mocks.get(...a),
    post: (...a) => mocks.post(...a),
    put: (...a) => mocks.put(...a),
    delete: (...a) => mocks.delete(...a)
  }
}))

import api, {
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
} from '../../src/api/aiSystem'

describe('gap: api/aiSystem 端点契约', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getAiSystemList 走 GET /api/aisystem/list', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getAiSystemList()
    expect(mocks.get).toHaveBeenCalledWith('/api/aisystem/list')
  })

  it('getAiSystemDetail 走 GET /api/aisystem/{id}', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getAiSystemDetail(42)
    expect(mocks.get).toHaveBeenCalledWith('/api/aisystem/42')
  })

  it('registerAiSystem 走 POST /api/aisystem/register 并透传 body', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    const body = { systemName: 'x', systemType: 'chat', rateLimit: 10 }
    await registerAiSystem(body)
    expect(mocks.post).toHaveBeenCalledWith('/api/aisystem/register', body)
  })

  it('updateAiSystemStatus 走 PUT /api/aisystem/{id}/status 并包 {status}', async () => {
    mocks.put.mockResolvedValue({ code: 200 })
    await updateAiSystemStatus(7, 1)
    expect(mocks.put).toHaveBeenCalledWith('/api/aisystem/7/status', { status: 1 })
  })

  it('updateAiSystemRatelimit 走 PUT /api/aisystem/{id}/ratelimit 并映射参数名', async () => {
    mocks.put.mockResolvedValue({ code: 200 })
    await updateAiSystemRatelimit(9, 50)
    expect(mocks.put).toHaveBeenCalledWith('/api/aisystem/9/ratelimit', { rateLimit: 50 })
  })

  it('regenerateAiSystemKey 走 POST /api/aisystem/{id}/regenerate-key', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await regenerateAiSystemKey(3)
    expect(mocks.post).toHaveBeenCalledWith('/api/aisystem/3/regenerate-key')
  })

  it('deleteAiSystem 走 DELETE /api/aisystem/{id}', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await deleteAiSystem(5)
    expect(mocks.delete).toHaveBeenCalledWith('/api/aisystem/5')
  })

  it('getAiSystemPermissions 走 GET /api/aisystem/{id}/permissions', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getAiSystemPermissions(11)
    expect(mocks.get).toHaveBeenCalledWith('/api/aisystem/11/permissions')
  })

  it('grantPermission 走 POST /api/aisystem/permission/grant 透传 data', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    const body = { role: 'admin' }
    await grantPermission(body)
    expect(mocks.post).toHaveBeenCalledWith('/api/aisystem/permission/grant', body)
  })

  it('revokePermission 走 DELETE /api/aisystem/permission/{permissionId}', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await revokePermission(88)
    expect(mocks.delete).toHaveBeenCalledWith('/api/aisystem/permission/88')
  })

  it('getOllamaStatus 走 GET /api/aisystem/ollama/status', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getOllamaStatus()
    expect(mocks.get).toHaveBeenCalledWith('/api/aisystem/ollama/status')
  })

  it('getOllamaModels 走 GET /api/aisystem/ollama/models', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getOllamaModels()
    expect(mocks.get).toHaveBeenCalledWith('/api/aisystem/ollama/models')
  })

  it('pullOllamaModel 走 POST /api/aisystem/ollama/pull 并包 {name}', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await pullOllamaModel('llama3')
    expect(mocks.post).toHaveBeenCalledWith('/api/aisystem/ollama/pull', { name: 'llama3' })
  })

  it('deleteOllamaModel 走 DELETE 并 encodeURIComponent 模型名', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await deleteOllamaModel('my model v2')
    expect(mocks.delete).toHaveBeenCalledWith('/api/aisystem/ollama/models/my%20model%20v2')
  })

  it('restartOllama 走 POST /api/aisystem/ollama/restart', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await restartOllama()
    expect(mocks.post).toHaveBeenCalledWith('/api/aisystem/ollama/restart')
  })

  it('getAiConfig 走 GET /api/aisystem/config', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getAiConfig()
    expect(mocks.get).toHaveBeenCalledWith('/api/aisystem/config')
  })

  it('updateAiConfig 走 PUT /api/aisystem/config 透传 body', async () => {
    mocks.put.mockResolvedValue({ code: 200 })
    const body = { temperature: 0.5 }
    await updateAiConfig(body)
    expect(mocks.put).toHaveBeenCalledWith('/api/aisystem/config', body)
  })

  it('getAiSystemStats 走 GET /api/aisystem/stats', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getAiSystemStats()
    expect(mocks.get).toHaveBeenCalledWith('/api/aisystem/stats')
  })

  it('默认导出包含全部命名函数', () => {
    expect(typeof api.getAiSystemList).toBe('function')
    expect(typeof api.updateAiConfig).toBe('function')
    expect(typeof api.getAiSystemStats).toBe('function')
  })
})
