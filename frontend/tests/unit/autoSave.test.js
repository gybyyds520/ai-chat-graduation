import AutoSaveManager from '../../src/utils/autoSave'

describe('autoSave 自动保存', () => {
  let manager

  beforeEach(() => {
    localStorage.clear()
    global.fetch = vi.fn(() => Promise.resolve({ json: async () => ({ code: 200 }) }))
    manager = new AutoSaveManager()
    manager.setUserId('user-1')
  })

  afterEach(() => {
    manager.stopAutoSave()
  })

  it('captureData 应生成待保存项', () => {
    const item = manager.captureData('chat_message', { text: 'hi' })
    expect(item.id).toBeDefined()
    expect(item.dataType).toBe('chat_message')
    expect(item.status).toBe('pending')
    expect(item.userId).toBe('user-1')
  })

  it('captureChatMessage / captureUserAction 应封装类型', () => {
    const m = manager.captureChatMessage({ role: 'user', content: 'x' })
    expect(m.dataType).toBe('chat_message')
    const a = manager.captureUserAction('click')
    expect(a.dataType).toBe('user_action')
  })

  it('captureData 攒满 10 条应自动触发保存', async () => {
    for (let i = 0; i < 10; i++) manager.captureData('chat', { i })
    await new Promise(r => setTimeout(r, 0))
    const stored = JSON.parse(localStorage.getItem(manager.localStorageKey) || '[]')
    expect(stored.length).toBe(10)
  })

  it('manualSave 在线时应上传并写本地', async () => {
    manager.captureData('chat', { a: 1 })
    await manager.manualSave()
    expect(global.fetch).toHaveBeenCalled()
    expect(manager.getSaveStatus()).toBe('success')
    const stored = JSON.parse(localStorage.getItem(manager.localStorageKey) || '[]')
    expect(stored.length).toBe(1)
  })

  it('离线时应进入上传队列而非请求', async () => {
    Object.defineProperty(navigator, 'onLine', { value: false, configurable: true })
    manager.captureData('chat', { a: 1 })
    await manager.manualSave()
    expect(global.fetch).not.toHaveBeenCalled()
    const queue = manager.getUploadQueue()
    expect(queue.length).toBe(1)
    Object.defineProperty(navigator, 'onLine', { value: true, configurable: true })
  })

  it('uploadToServer 成功应返回 true', async () => {
    const ok = await manager.uploadToServer([{ x: 1 }])
    expect(ok).toBe(true)
  })

  it('uploadToServer 失败应返回 false 并入队', async () => {
    global.fetch = vi.fn(() => Promise.reject(new Error('network')))
    const ok = await manager.uploadToServer([{ x: 1 }])
    expect(ok).toBe(false)
    expect(manager.getUploadQueue().length).toBe(1)
  })

  it('空数据上传应直接返回 undefined', async () => {
    expect(await manager.uploadToServer([])).toBeUndefined()
  })

  it('clearLocalData / clearUploadQueue 应清空', () => {
    manager.saveToLocalStorage([{ a: 1 }])
    manager.addToUploadQueue([{ b: 2 }])
    manager.clearLocalData()
    manager.clearUploadQueue()
    expect(manager.getLocalData()).toEqual([])
    expect(manager.getUploadQueue()).toEqual([])
  })

  it('本地数据损坏时 getLocalData 应兜底为空数组', () => {
    localStorage.setItem(manager.localStorageKey, '{{bad json')
    expect(manager.getLocalData()).toEqual([])
  })

  it('getPendingCount 应统计待保存数量', () => {
    manager.captureData('chat', { a: 1 })
    manager.captureData('chat', { b: 2 })
    expect(manager.getPendingCount()).toBe(2)
  })

  it('手动连续调用 manualSave 应防重入', async () => {
    manager.isSaving = true
    await manager.manualSave()
    // 重入时直接返回，不应改变状态
    expect(manager.isSaving).toBe(true)
  })
})
