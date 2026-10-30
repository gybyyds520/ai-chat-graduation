import { describe, it, expect, beforeEach } from 'vitest'
import { TestDataCleaner } from '../../src/utils/testDataCleaner'

describe('gap: utils/testDataCleaner 测试数据清理', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('getCleanupTargets 返回全部 6 个清理目标', () => {
    const c = new TestDataCleaner()
    const targets = c.getCleanupTargets()
    expect(targets).toContain('localStorage')
    expect(targets).toContain('sessionStorage')
    expect(targets).toContain('Cookies')
    expect(targets.length).toBe(6)
  })

  it('isTestKey 识别 test_/mock_/debug_/temp_/sandbox_ 等下划线前缀', () => {
    const c = new TestDataCleaner()
    expect(c.isTestKey('test_abc')).toBe(true)
    expect(c.isTestKey('mock_data')).toBe(true)
    expect(c.isTestKey('debug_x')).toBe(true)
    expect(c.isTestKey('temp_y')).toBe(true)
    expect(c.isTestKey('sandbox_z')).toBe(true)
    expect(c.isTestKey('validator_1')).toBe(true)
  })

  it('isTestKey 不命中业务 key 或无下划线的前缀', () => {
    const c = new TestDataCleaner()
    expect(c.isTestKey('token')).toBe(false)
    expect(c.isTestKey('user-profile')).toBe(false)
    expect(c.isTestKey('app-theme-mode')).toBe(false)
    expect(c.isTestKey('debug1')).toBe(false)
  })

  it('isTestName 大小写不敏感地匹配 test/mock/debug/temp/sandbox 开头', () => {
    const c = new TestDataCleaner()
    expect(c.isTestName('TestDB')).toBe(true)
    expect(c.isTestName('MOCK_CACHE')).toBe(true)
    expect(c.isTestName('debug-log')).toBe(true)
    expect(c.isTestName('real-data')).toBe(false)
  })

  it('cleanLocalStorage 仅删除测试 key 保留业务 key', () => {
    localStorage.setItem('test_tmp', '1')
    localStorage.setItem('mock_abc', '2')
    localStorage.setItem('token', 'should-keep')
    const c = new TestDataCleaner()
    c.cleanLocalStorage()
    expect(localStorage.getItem('test_tmp')).toBeNull()
    expect(localStorage.getItem('mock_abc')).toBeNull()
    expect(localStorage.getItem('token')).toBe('should-keep')
  })

  it('cleanSessionStorage 仅删除测试 key', () => {
    sessionStorage.setItem('test_sess', 'x')
    sessionStorage.setItem('normal', 'keep')
    const c = new TestDataCleaner()
    c.cleanSessionStorage()
    expect(sessionStorage.getItem('test_sess')).toBeNull()
    expect(sessionStorage.getItem('normal')).toBe('keep')
  })

  it('cleanWindowSession 清空 window.__modelInstances', () => {
    window.__modelInstances = new Map([['a', 1]])
    const c = new TestDataCleaner()
    c.cleanWindowSession()
    expect(window.__modelInstances.size).toBe(0)
  })
})
