import { describe, it, expect, beforeEach } from 'vitest'
import mpLogger from '../../src/utils/miniprogramLogger'

describe('gap: utils/miniprogramLogger 小程序日志（单例）', () => {
  beforeEach(() => {
    mpLogger.logs = []
    mpLogger.enabled = true
    mpLogger.logLevel = 'debug'
    mpLogger.maxLogs = 1000
  })

  it('默认启用且级别为 debug', () => {
    expect(mpLogger.enabled).toBe(true)
    expect(mpLogger.logLevel).toBe('debug')
  })

  it('getTimestamp 返回 YYYY-MM-DD HH:mm:ss.ms 格式', () => {
    const ts = mpLogger.getTimestamp()
    expect(ts).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}$/)
  })

  it('formatMessage 拼接 level/operation/description 并附加 data', () => {
    const msg = mpLogger.formatMessage('info', 'Login', '用户登录', { uid: 1 })
    expect(msg).toContain('[INFO]')
    expect(msg).toContain('[Login]')
    expect(msg).toContain('用户登录')
    expect(msg).toContain('"uid": 1')
  })

  it('debug 级别记录到 logs', () => {
    mpLogger.debug('Op', 'desc')
    expect(mpLogger.logs.length).toBe(1)
    expect(mpLogger.logs[0].level).toBe('debug')
  })

  it('设置 logLevel=info 后 debug 不记录', () => {
    mpLogger.setLogLevel('info')
    mpLogger.logs = [] // setLogLevel 自身会写一条 info，清空后再测
    mpLogger.debug('Op', 'should not record')
    expect(mpLogger.logs.length).toBe(0)
    mpLogger.info('Op', 'should record')
    expect(mpLogger.logs.length).toBe(1)
  })

  it('setEnabled(false) 后所有级别都不记录', () => {
    mpLogger.setEnabled(false)
    mpLogger.info('x', 'y')
    mpLogger.warn('x', 'y')
    mpLogger.error('x', 'y')
    expect(mpLogger.logs.length).toBe(0)
  })

  it('warn/error 都写入 logs 数组', () => {
    mpLogger.warn('W', 'warn desc')
    mpLogger.error('E', 'err desc')
    expect(mpLogger.logs.map(l => l.level)).toEqual(['warn', 'error'])
  })

  it('日志超过 maxLogs 时淘汰最旧一条', () => {
    mpLogger.maxLogs = 3
    for (let i = 0; i < 5; i++) mpLogger.info('Op', `msg-${i}`)
    expect(mpLogger.logs.length).toBe(3)
    expect(mpLogger.logs[0].description).toBe('msg-2')
    expect(mpLogger.logs[2].description).toBe('msg-4')
  })

  it('logTouchEvent 记录 Touch 操作', () => {
    mpLogger.logTouchEvent({ type: 'touchstart' }, 'start', { x: 1 })
    expect(mpLogger.logs.length).toBe(1)
    expect(mpLogger.logs[0].operation).toBe('Touch')
  })

  it('exportLogs 返回合法 JSON', () => {
    mpLogger.info('Op', 'desc')
    const out = mpLogger.exportLogs()
    expect(() => JSON.parse(out)).not.toThrow()
    expect(JSON.parse(out).length).toBe(1)
  })
})
