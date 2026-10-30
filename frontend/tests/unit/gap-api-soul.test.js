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

import {
  getSoulOverview,
  listSoulExperiences,
  resetSoul,
  evolveSoul,
  getSoulPrompt
} from '../../src/api/soul'

describe('gap: api/soul 端点契约', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getSoulOverview 走 GET /api/soul/{characterId}', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getSoulOverview('char-1')
    expect(mocks.get).toHaveBeenCalledWith('/api/soul/char-1')
  })

  it('listSoulExperiences 走 GET /api/soul/{id}/experiences 并带 params', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    const params = { page: 1, size: 20 }
    await listSoulExperiences('char-2', params)
    expect(mocks.get).toHaveBeenCalledWith('/api/soul/char-2/experiences', { params })
  })

  it('resetSoul 走 POST /api/soul/{id}/reset', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await resetSoul('char-3')
    expect(mocks.post).toHaveBeenCalledWith('/api/soul/char-3/reset')
  })

  it('evolveSoul 走 POST /api/soul/{id}/evolve 透传 data', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    const data = { trigger: 'test' }
    await evolveSoul('char-4', data)
    expect(mocks.post).toHaveBeenCalledWith('/api/soul/char-4/evolve', data)
  })

  it('getSoulPrompt 走 GET /api/soul/{id}/prompt', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getSoulPrompt('char-5')
    expect(mocks.get).toHaveBeenCalledWith('/api/soul/char-5/prompt')
  })
})
