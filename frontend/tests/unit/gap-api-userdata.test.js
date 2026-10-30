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
  getUserDataRecords,
  getUserDataRecordDetail,
  deleteUserDataRecord,
  batchDeleteUserDataRecords,
  getUserDataStats,
  getUserDataTypes,
  getDataPoolConfig,
  toggleAutoSave,
  exportUserData,
  checkDuplicates,
  removeDuplicates
} from '../../src/api/userData'

describe('gap: api/userData (data-pool) 端点契约', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getUserDataRecords 走 GET /api/admin/data-pool/records 并带 params', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    const params = { page: 1, size: 20, userId: 42 }
    await getUserDataRecords(params)
    expect(mocks.get).toHaveBeenCalledWith('/api/admin/data-pool/records', { params })
  })

  it('getUserDataRecordDetail 走 GET /api/admin/data-pool/records/{id}', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getUserDataRecordDetail(7)
    expect(mocks.get).toHaveBeenCalledWith('/api/admin/data-pool/records/7')
  })

  it('deleteUserDataRecord 走 DELETE /api/admin/data-pool/records/{id}', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await deleteUserDataRecord(8)
    expect(mocks.delete).toHaveBeenCalledWith('/api/admin/data-pool/records/8')
  })

  it('batchDeleteUserDataRecords 走 DELETE /api/admin/data-pool/records/batch body 为纯数组', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await batchDeleteUserDataRecords([10, 20, 30])
    expect(mocks.delete).toHaveBeenCalledWith('/api/admin/data-pool/records/batch', { data: [10, 20, 30] })
  })

  it('getUserDataStats 走 GET /api/admin/data-pool/stats', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getUserDataStats()
    expect(mocks.get).toHaveBeenCalledWith('/api/admin/data-pool/stats')
  })

  it('getUserDataTypes 走 GET /api/admin/data-pool/types', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getUserDataTypes()
    expect(mocks.get).toHaveBeenCalledWith('/api/admin/data-pool/types')
  })

  it('getDataPoolConfig 走 GET /api/admin/data-pool/config', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getDataPoolConfig()
    expect(mocks.get).toHaveBeenCalledWith('/api/admin/data-pool/config')
  })

  it('toggleAutoSave 走 POST /api/admin/data-pool/auto-save/toggle 并包 {enabled}', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await toggleAutoSave(true)
    expect(mocks.post).toHaveBeenCalledWith('/api/admin/data-pool/auto-save/toggle', { enabled: true })
  })

  it('exportUserData 走 GET /api/admin/data-pool/export 并带 params', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    const params = { format: 'json' }
    await exportUserData(params)
    expect(mocks.get).toHaveBeenCalledWith('/api/admin/data-pool/export', { params })
  })

  it('checkDuplicates 走 GET /api/admin/data-pool/duplicates/check', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await checkDuplicates()
    expect(mocks.get).toHaveBeenCalledWith('/api/admin/data-pool/duplicates/check')
  })

  it('removeDuplicates 走 POST /api/admin/data-pool/duplicates/remove', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await removeDuplicates()
    expect(mocks.post).toHaveBeenCalledWith('/api/admin/data-pool/duplicates/remove')
  })

  it('默认导出包含全部命名函数', () => {
    expect(typeof api.getUserDataRecords).toBe('function')
    expect(typeof api.removeDuplicates).toBe('function')
  })
})
