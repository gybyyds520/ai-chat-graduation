import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { versionService } from '../../src/utils/versionService'

describe('gap: utils/versionService 版本服务', () => {
  beforeEach(() => {
    localStorage.clear()
    versionService.versionInfo = versionService.loadVersionInfo()
  })
  afterEach(() => {
    localStorage.clear()
  })

  it('默认版本类型为 free', () => {
    expect(versionService.getVersionType()).toBe('free')
  })

  it('getVersionTypeLabel 映射中文标签', () => {
    versionService.setVersionType('pro')
    expect(versionService.getVersionTypeLabel()).toBe('专业版')
    versionService.setVersionType('premium')
    expect(versionService.getVersionTypeLabel()).toBe('高级版')
    versionService.setVersionType('enterprise')
    expect(versionService.getVersionTypeLabel()).toBe('企业版')
  })

  it('未知 versionType 原样返回', () => {
    versionService.setVersionType('unknown-xyz')
    expect(versionService.getVersionTypeLabel()).toBe('unknown-xyz')
  })

  it('getVersionTypeColor 映射配色', () => {
    versionService.setVersionType('pro')
    expect(versionService.getVersionTypeColor()).toBe('#3b82f6')
    versionService.setVersionType('premium')
    expect(versionService.getVersionTypeColor()).toBe('#f59e0b')
  })

  it('未知 versionType 颜色回退 #6b7280', () => {
    versionService.setVersionType('weird')
    expect(versionService.getVersionTypeColor()).toBe('#6b7280')
  })

  it('setVersionType 持久化到 localStorage', () => {
    versionService.setVersionType('pro')
    const saved = JSON.parse(localStorage.getItem('app-version-info'))
    expect(saved.versionType).toBe('pro')
  })

  it('loadVersionInfo 合并已保存的自定义字段', () => {
    localStorage.setItem('app-version-info', JSON.stringify({ versionType: 'enterprise', currentVersion: '9.9.9' }))
    const info = versionService.loadVersionInfo()
    expect(info.versionType).toBe('enterprise')
    expect(info.currentVersion).toBe('9.9.9')
    expect(info.upgradeTargetVersion).toBe('2.0.0') // 默认字段仍在
  })

  it('hasUpgradeDeadline 无 deadline 返回 falsy', () => {
    expect(versionService.hasUpgradeDeadline()).toBeFalsy()
  })

  it('hasUpgradeDeadline 设置未来时间戳返回 true', () => {
    versionService.setUpgradeDeadline(Date.now() + 86400000)
    expect(versionService.hasUpgradeDeadline()).toBe(true)
  })

  it('hasUpgradeDeadline 过期时间戳返回 false', () => {
    versionService.setUpgradeDeadline(Date.now() - 1000)
    expect(versionService.hasUpgradeDeadline()).toBe(false)
  })

  it('getCountdown 无 deadline 返回 null', () => {
    expect(versionService.getCountdown()).toBeNull()
  })

  it('getCountdown 已过期返回 expired:true 且各字段为 0', () => {
    versionService.setUpgradeDeadline(Date.now() - 5000)
    const c = versionService.getCountdown()
    expect(c.expired).toBe(true)
    expect(c.days).toBe(0)
    expect(c.hours).toBe(0)
    expect(c.minutes).toBe(0)
    expect(c.seconds).toBe(0)
  })

  it('getCountdown 未来时间正确拆出天/时/分/秒', () => {
    const future = Date.now() + (2 * 86400 + 3 * 3600 + 4 * 60 + 5) * 1000
    versionService.setUpgradeDeadline(future)
    const c = versionService.getCountdown()
    expect(c.expired).toBe(false)
    expect(c.days).toBe(2)
    expect(c.hours).toBe(3)
    expect(c.minutes).toBe(4)
    // 秒数受运行时耗时影响，允许 4~5 秒
    expect([4, 5]).toContain(c.seconds)
  })

  it('clearUpgradeDeadline 清空 deadline', () => {
    versionService.setUpgradeDeadline(Date.now() + 86400000)
    versionService.clearUpgradeDeadline()
    expect(versionService.hasUpgradeDeadline()).toBeFalsy()
    expect(versionService.getCountdown()).toBeNull()
  })

  it('subscribe/notifyListeners 触发监听器', () => {
    const events = []
    const unsub = versionService.subscribe((event, data) => events.push([event, data]))
    versionService.setVersionType('pro')
    expect(events.length).toBeGreaterThan(0)
    expect(events[0][0]).toBe('versionTypeChanged')
    expect(events[0][1]).toBe('pro')
    unsub()
  })
})
