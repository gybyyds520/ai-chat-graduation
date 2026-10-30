import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import {
  getChartColors,
  getChartColor,
  getEChartsThemeConfig,
  updateChartTheme
} from '../../src/utils/chartTheme'

describe('gap: utils/chartTheme 图表主题', () => {
  beforeEach(() => {
    document.documentElement.removeAttribute('data-theme')
  })
  afterEach(() => {
    document.documentElement.removeAttribute('data-theme')
  })

  it('默认（无 data-theme）返回亮色主题颜色', () => {
    const c = getChartColors()
    expect(c.chart1).toBe('#6366f1')
    expect(c.textPrimary).toBe('#374151')
  })

  it('data-theme=dark 返回暗色主题颜色', () => {
    document.documentElement.setAttribute('data-theme', 'dark')
    const c = getChartColors()
    expect(c.chart1).toBe('#818cf8')
    expect(c.textPrimary).toBe('#f9fafb')
  })

  it('getChartColor 返回对应 key 颜色', () => {
    expect(getChartColor('chart2')).toBe('#22c55e')
  })

  it('getChartColor 未知 key 回退到 textPrimary', () => {
    expect(getChartColor('not-exist')).toBe('#374151')
  })

  it('getChartColor 在 dark 模式下回退到暗色 textPrimary', () => {
    document.documentElement.setAttribute('data-theme', 'dark')
    expect(getChartColor('nope')).toBe('#f9fafb')
  })

  it('getEChartsThemeConfig 返回完整结构（亮色）', () => {
    const cfg = getEChartsThemeConfig()
    expect(cfg.colors).toEqual(['#6366f1', '#22c55e', '#8b5cf6', '#f59e0b'])
    expect(cfg.tooltip.backgroundColor).toBe('rgba(255, 255, 255, 0.95)')
    expect(cfg.yAxis.splitLine.lineStyle.color).toBe('#f3f4f6')
  })

  it('getEChartsThemeConfig dark 模式使用暗色 tooltipBg', () => {
    document.documentElement.setAttribute('data-theme', 'dark')
    const cfg = getEChartsThemeConfig()
    expect(cfg.tooltip.backgroundColor).toBe('rgba(17, 24, 39, 0.95)')
  })

  it('updateChartTheme 传入 null chart 直接返回不抛错', () => {
    expect(() => updateChartTheme(null, {})).not.toThrow()
  })

  it('updateChartTheme 会把 option.tooltip/legend/xAxis/yAxis 颜色改写为当前主题', () => {
    const fakeChart = { setOption: vi.fn() }
    const option = {
      tooltip: { textStyle: {} },
      legend: { textStyle: {} },
      xAxis: { axisLine: {}, axisLabel: {} },
      yAxis: { axisLine: {}, axisLabel: {}, splitLine: {} }
    }
    updateChartTheme(fakeChart, option)
    expect(option.tooltip.backgroundColor).toBe('rgba(255, 255, 255, 0.95)')
    expect(option.tooltip.textStyle.color).toBe('#374151')
    expect(option.legend.textStyle.color).toBe('#6b7280')
    expect(option.xAxis.axisLine.lineStyle.color).toBe('#e5e7eb')
    expect(option.yAxis.splitLine.lineStyle.color).toBe('#f3f4f6')
  })

  it('updateChartTheme dark 模式改写为暗色值', () => {
    document.documentElement.setAttribute('data-theme', 'dark')
    const fakeChart = { setOption: vi.fn() }
    const option = {
      tooltip: {},
      legend: { textStyle: {} },
      xAxis: {},
      yAxis: {}
    }
    updateChartTheme(fakeChart, option)
    expect(option.tooltip.backgroundColor).toBe('rgba(17, 24, 39, 0.95)')
  })
})
