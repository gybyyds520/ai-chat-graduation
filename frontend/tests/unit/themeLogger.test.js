import {
  getThemeVariables,
  compareThemeVariables,
  logThemeBefore,
  logThemeAfter,
  logThemeOnMounted,
  logThemeSwitchTrigger,
  logThemeSwitchComplete,
  getLogHistory,
  clearLogHistory,
  setLoggingEnabled,
  LogLevel
} from '../../src/utils/themeLogger'

describe('themeLogger 主题日志', () => {
  beforeEach(() => {
    clearLogHistory()
    setLoggingEnabled(true)
  })

  it('getThemeVariables 应返回变量映射对象', () => {
    const vars = getThemeVariables()
    expect(vars['bg-primary']).toBeDefined()
    expect(vars['color-primary']).toBeDefined()
  })

  it('compareThemeVariables 应区分变化与未变化', () => {
    const before = { a: '#fff', b: '#000' }
    const after = { a: '#111', b: '#000' }
    const { changes, unchanged } = compareThemeVariables(before, after)
    expect(changes.a).toEqual({ before: '#fff', after: '#111' })
    expect(unchanged.b).toBe('#000')
  })

  it('compareThemeVariables 无差异时 changes 为空', () => {
    const same = { a: '#1' }
    const { changes } = compareThemeVariables(same, same)
    expect(Object.keys(changes).length).toBe(0)
  })

  it('logThemeBefore 应记录并返回当前变量', () => {
    const vars = logThemeBefore('CompA')
    expect(vars).toBeDefined()
    expect(getLogHistory().length).toBeGreaterThan(0)
  })

  it('logThemeAfter 应对比并返回 comparison', () => {
    const before = logThemeBefore('CompB')
    const { comparison } = logThemeAfter('CompB', before)
    expect(comparison).toBeDefined()
    expect(comparison.changes).toBeDefined()
  })

  it('logThemeOnMounted 应记录挂载事件', () => {
    logThemeOnMounted('CompC')
    const history = getLogHistory()
    expect(history.at(-1).action).toBe('COMPONENT_MOUNTED')
  })

  it('logThemeSwitchTrigger / Complete 应记录', () => {
    logThemeSwitchTrigger('CompD', 'button')
    logThemeSwitchComplete('CompD', 120)
    const history = getLogHistory()
    expect(history.at(-2).action).toBe('THEME_SWITCH_TRIGGER')
    expect(history.at(-1).action).toBe('THEME_SWITCH_COMPLETE')
  })

  it('setLoggingEnabled(false) 后不应再记录', () => {
    setLoggingEnabled(false)
    clearLogHistory()
    logThemeSwitchTrigger('CompE', 'btn')
    expect(getLogHistory().length).toBe(0)
    setLoggingEnabled(true)
  })

  it('LogLevel 常量应完整', () => {
    expect(LogLevel.DEBUG).toBe('DEBUG')
    expect(LogLevel.ERROR).toBe('ERROR')
  })
})
