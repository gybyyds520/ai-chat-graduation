import baseLogger, { createLogger } from '../../src/utils/logger'

describe('logger 日志', () => {
  beforeEach(() => {
    vi.spyOn(console, 'debug').mockImplementation(() => {})
    vi.spyOn(console, 'info').mockImplementation(() => {})
    vi.spyOn(console, 'warn').mockImplementation(() => {})
    vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.spyOn(console, 'log').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('createLogger 应返回分级日志对象', () => {
    const log = createLogger('TestMod')
    expect(typeof log.debug).toBe('function')
    expect(typeof log.info).toBe('function')
    expect(typeof log.warn).toBe('function')
    expect(typeof log.error).toBe('function')
  })

  it('开发环境下 debug 应输出到 console.debug', () => {
    const log = createLogger('TestMod')
    log.debug('hello')
    expect(console.debug).toHaveBeenCalled()
    expect(console.debug.mock.calls[0][0]).toContain('TestMod')
  })

  it('info 应输出到 console.info', () => {
    const log = createLogger('ModB')
    log.info('info msg')
    expect(console.info).toHaveBeenCalledWith(expect.stringContaining('ModB'), 'info msg')
  })

  it('warn 应输出到 console.warn', () => {
    const log = createLogger('ModC')
    log.warn('warn msg')
    expect(console.warn).toHaveBeenCalled()
  })

  it('error 应输出到 console.error', () => {
    const log = createLogger('ModD')
    log.error('boom')
    expect(console.error).toHaveBeenCalled()
  })

  it('always 应无条件输出到 console.log', () => {
    const log = createLogger('ModE')
    log.always('forced')
    expect(console.log).toHaveBeenCalled()
  })

  it('默认导出 baseLogger 应具备全部分级方法', () => {
    expect(typeof baseLogger.debug).toBe('function')
    expect(typeof baseLogger.error).toBe('function')
    expect(typeof baseLogger.always).toBe('function')
  })

  it('error 不应抛异常', () => {
    const log = createLogger('ModF')
    expect(() => log.error('err', { a: 1 })).not.toThrow()
  })
})
