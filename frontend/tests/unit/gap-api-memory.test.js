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
  listMemories,
  getMemoryStats,
  getMemoryDetail,
  createMemory,
  updateMemory,
  deleteMemory,
  toggleMemory,
  listMemoryItems,
  updateMemoryItem,
  deleteMemoryItem,
  getMemorySettings,
  updateMemorySettings,
  pinMessageToMemory,
  listMemoryItemsByCategory,
  batchDeleteMemories,
  exportMemories
} from '../../src/api/memory'

describe('gap: api/memory 端点契约', () => {
  beforeEach(() => vi.clearAllMocks())

  it('listMemories 走 GET /api/memory/list 并以 params 传参', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    const params = { page: 1, size: 10, memoryType: 'fact' }
    await listMemories(params)
    expect(mocks.get).toHaveBeenCalledWith('/api/memory/list', { params })
  })

  it('getMemoryStats 走 GET /api/memory/stats', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getMemoryStats()
    expect(mocks.get).toHaveBeenCalledWith('/api/memory/stats')
  })

  it('getMemoryDetail 走 GET /api/memory/{id}', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getMemoryDetail('m-1')
    expect(mocks.get).toHaveBeenCalledWith('/api/memory/m-1')
  })

  it('createMemory 走 POST /api/memory 透传 body', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    const body = { content: '喜欢猫', memoryType: 'preference' }
    await createMemory(body)
    expect(mocks.post).toHaveBeenCalledWith('/api/memory', body)
  })

  it('updateMemory 走 PUT /api/memory/{id} 透传 data', async () => {
    mocks.put.mockResolvedValue({ code: 200 })
    await updateMemory('m-2', { importance: 5 })
    expect(mocks.put).toHaveBeenCalledWith('/api/memory/m-2', { importance: 5 })
  })

  it('deleteMemory 走 DELETE /api/memory/{id}', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await deleteMemory('m-3')
    expect(mocks.delete).toHaveBeenCalledWith('/api/memory/m-3')
  })

  it('toggleMemory 走 POST /api/memory/{id}/toggle', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await toggleMemory('m-4')
    expect(mocks.post).toHaveBeenCalledWith('/api/memory/m-4/toggle')
  })

  it('listMemoryItems 走 GET /api/memory/items 并带 params', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await listMemoryItems({ page: 2, size: 5 })
    expect(mocks.get).toHaveBeenCalledWith('/api/memory/items', { params: { page: 2, size: 5 } })
  })

  it('updateMemoryItem 走 PUT /api/memory/items/{id}', async () => {
    mocks.put.mockResolvedValue({ code: 200 })
    await updateMemoryItem('i-1', { content: '新内容' })
    expect(mocks.put).toHaveBeenCalledWith('/api/memory/items/i-1', { content: '新内容' })
  })

  it('deleteMemoryItem 走 DELETE /api/memory/items/{id}', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await deleteMemoryItem('i-2')
    expect(mocks.delete).toHaveBeenCalledWith('/api/memory/items/i-2')
  })

  it('getMemorySettings 走 GET /api/memory/settings', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getMemorySettings()
    expect(mocks.get).toHaveBeenCalledWith('/api/memory/settings')
  })

  it('updateMemorySettings 走 PUT /api/memory/settings 并包 {enabled}', async () => {
    mocks.put.mockResolvedValue({ code: 200 })
    await updateMemorySettings(true)
    expect(mocks.put).toHaveBeenCalledWith('/api/memory/settings', { enabled: true })
  })

  it('pinMessageToMemory 走 POST /api/memory/pin 透传 data', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    const body = { content: '记住这句话', source: 'chat' }
    await pinMessageToMemory(body)
    expect(mocks.post).toHaveBeenCalledWith('/api/memory/pin', body)
  })

  it('listMemoryItemsByCategory 走 GET /api/memory/items 并带 category params', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await listMemoryItemsByCategory({ category: 'FACT' })
    expect(mocks.get).toHaveBeenCalledWith('/api/memory/items', { params: { category: 'FACT' } })
  })

  it('batchDeleteMemories 走 DELETE /api/memory/batch 并以 data 字段包 ids', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await batchDeleteMemories([1, 2, 3])
    expect(mocks.delete).toHaveBeenCalledWith('/api/memory/batch', { data: { ids: [1, 2, 3] } })
  })

  it('exportMemories 走 GET /api/memory/export', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await exportMemories()
    expect(mocks.get).toHaveBeenCalledWith('/api/memory/export')
  })
})
