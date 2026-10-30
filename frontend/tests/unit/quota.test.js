import { vi, describe, it, expect, beforeEach } from 'vitest'

// mock HTTP 封装，拦截 request 实例上的 get/post/delete
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

import {
  getCurrentQuota,
  deleteChatSession,
  deleteChatMessage,
  exportMyData
} from '../../src/api/quota'

describe('quota API 端点契约', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getCurrentQuota 应 GET /api/quota/current', async () => {
    mocks.get.mockResolvedValue({ code: 200, data: { features: {} } })
    await getCurrentQuota()
    expect(mocks.get).toHaveBeenCalledWith('/api/quota/current')
  })

  it('deleteChatSession 应 DELETE /api/chat/session/{id}', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await deleteChatSession('sess-123')
    expect(mocks.delete).toHaveBeenCalledWith('/api/chat/session/sess-123')
  })

  it('deleteChatMessage 应 DELETE /api/chat/message/{id}', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await deleteChatMessage('msg-9')
    expect(mocks.delete).toHaveBeenCalledWith('/api/chat/message/msg-9')
  })

  it('exportMyData 应 GET /api/user/data/export', async () => {
    mocks.get.mockResolvedValue({ code: 200, data: {} })
    await exportMyData()
    expect(mocks.get).toHaveBeenCalledWith('/api/user/data/export')
  })
})
