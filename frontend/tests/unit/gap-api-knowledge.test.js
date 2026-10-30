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
  listDocuments,
  getDocument,
  deleteDocument,
  reparseDocument,
  askInDocument,
  askAcrossDocuments
} from '../../src/api/knowledge'

describe('gap: api/knowledge 端点契约', () => {
  beforeEach(() => vi.clearAllMocks())

  it('listDocuments 默认 page=1 size=20 拼接 querystring', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await listDocuments()
    expect(mocks.get).toHaveBeenCalledWith('/api/knowledge/list?page=1&size=20')
  })

  it('listDocuments 透传自定义分页参数', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await listDocuments(3, 50)
    expect(mocks.get).toHaveBeenCalledWith('/api/knowledge/list?page=3&size=50')
  })

  it('getDocument 走 GET /api/knowledge/{id}', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getDocument('doc-7')
    expect(mocks.get).toHaveBeenCalledWith('/api/knowledge/doc-7')
  })

  it('deleteDocument 走 DELETE /api/knowledge/{id}', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await deleteDocument('doc-9')
    expect(mocks.delete).toHaveBeenCalledWith('/api/knowledge/doc-9')
  })

  it('reparseDocument 走 POST /api/knowledge/{id}/reparse', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await reparseDocument('doc-3')
    expect(mocks.post).toHaveBeenCalledWith('/api/knowledge/doc-3/reparse')
  })

  it('askInDocument 走 POST /api/knowledge/{id}/ask 并包 query/topK', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await askInDocument('doc-1', '什么是X', 5)
    expect(mocks.post).toHaveBeenCalledWith('/api/knowledge/doc-1/ask', { query: '什么是X', topK: 5 })
  })

  it('askInDocument topK 默认 3', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await askInDocument('doc-2', 'q')
    expect(mocks.post).toHaveBeenCalledWith('/api/knowledge/doc-2/ask', { query: 'q', topK: 3 })
  })

  it('askAcrossDocuments 走 POST /api/knowledge/ask 并透传 documentIds', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await askAcrossDocuments('全局问题', ['a', 'b'], 7)
    expect(mocks.post).toHaveBeenCalledWith('/api/knowledge/ask', { query: '全局问题', documentIds: ['a', 'b'], topK: 7 })
  })

  it('askAcrossDocuments documentIds 默认 null', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await askAcrossDocuments('q2')
    expect(mocks.post).toHaveBeenCalledWith('/api/knowledge/ask', { query: 'q2', documentIds: null, topK: 3 })
  })

  it('默认导出暴露全部命名函数', () => {
    expect(typeof api.listDocuments).toBe('function')
    expect(typeof api.askAcrossDocuments).toBe('function')
    expect(typeof api.uploadDocument).toBe('function')
  })
})
