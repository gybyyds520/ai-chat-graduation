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
  getNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
  deleteNotification,
  clearAllNotifications,
  getNotificationSettings,
  updateNotificationSettings
} from '../../src/api/notification'

describe('gap: api/notification 端点契约', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getNotifications 默认 type=all 不附加 type 参数', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getNotifications(1001)
    expect(mocks.get).toHaveBeenCalledWith(expect.stringMatching(/^\/api\/notification\/list\?/))
    const url = mocks.get.mock.calls[0][0]
    expect(url).toContain('userId=1001')
    expect(url).toContain('page=1')
    expect(url).toContain('size=20')
    expect(url).not.toMatch(/type=/)
  })

  it('getNotifications 指定 type 时追加 type 参数', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getNotifications(1001, 2, 15, 'like')
    const url = mocks.get.mock.calls[0][0]
    expect(url).toContain('userId=1001')
    expect(url).toContain('page=2')
    expect(url).toContain('size=15')
    expect(url).toContain('type=like')
  })

  it('getUnreadCount 走 GET /api/notification/unread/count', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getUnreadCount(1001)
    expect(mocks.get).toHaveBeenCalledWith('/api/notification/unread/count?userId=1001')
  })

  it('markAsRead 走 POST /api/notification/read/{id}', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await markAsRead(1001, 'n-9')
    expect(mocks.post).toHaveBeenCalledWith('/api/notification/read/n-9?userId=1001')
  })

  it('markAllAsRead 走 POST /api/notification/read/all', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await markAllAsRead(1001)
    expect(mocks.post).toHaveBeenCalledWith('/api/notification/read/all?userId=1001')
  })

  it('deleteNotification 走 DELETE /api/notification/{id}', async () => {
    mocks.delete.mockResolvedValue({ code: 200 })
    await deleteNotification(1001, 'n-3')
    expect(mocks.delete).toHaveBeenCalledWith('/api/notification/n-3?userId=1001')
  })

  it('clearAllNotifications 走 POST /api/notification/clear-all', async () => {
    mocks.post.mockResolvedValue({ code: 200 })
    await clearAllNotifications(1001)
    expect(mocks.post).toHaveBeenCalledWith('/api/notification/clear-all?userId=1001')
  })

  it('getNotificationSettings 走 GET /api/notification/settings', async () => {
    mocks.get.mockResolvedValue({ code: 200 })
    await getNotificationSettings(1001)
    expect(mocks.get).toHaveBeenCalledWith('/api/notification/settings?userId=1001')
  })

  it('updateNotificationSettings 走 PUT /api/notification/settings 透传 settings', async () => {
    mocks.put.mockResolvedValue({ code: 200 })
    const settings = { likeEnabled: true, commentEnabled: false, shareEnabled: true, systemEnabled: false }
    await updateNotificationSettings(1001, settings)
    expect(mocks.put).toHaveBeenCalledWith('/api/notification/settings?userId=1001', settings)
  })

  it('默认导出暴露全部函数', () => {
    expect(typeof api.getNotifications).toBe('function')
    expect(typeof api.updateNotificationSettings).toBe('function')
  })
})
