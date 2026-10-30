/**
 * 图表主题颜色工具函数
 * 用于在主题切换时动态获取图表颜色
 */

// 亮色主题颜色
const lightColors = {
  textPrimary: '#374151',
  textSecondary: '#6b7280',
  borderColor: '#e5e7eb',
  splitLine: '#f3f4f6',
  chart1: '#6366f1',
  chart2: '#22c55e',
  chart3: '#8b5cf6',
  chart4: '#f59e0b',
  tooltipBg: 'rgba(255, 255, 255, 0.95)'
}

// 暗色主题颜色
const darkColors = {
  textPrimary: '#f9fafb',
  textSecondary: '#9ca3af',
  borderColor: '#374151',
  splitLine: '#1f2937',
  chart1: '#818cf8',
  chart2: '#4ade80',
  chart3: '#a78bfa',
  chart4: '#fbbf24',
  tooltipBg: 'rgba(17, 24, 39, 0.95)'
}

/**
 * 获取当前主题的图表颜色
 */
export function getChartColors() {
  const theme = document.documentElement.getAttribute('data-theme') || 'light'
  return theme === 'dark' ? darkColors : lightColors
}

/**
 * 获取指定颜色
 */
export function getChartColor(key) {
  const colors = getChartColors()
  return colors[key] || colors.textPrimary
}

/**
 * 获取ECharts配置的主题颜色
 */
export function getEChartsThemeConfig() {
  const colors = getChartColors()
  return {
    tooltip: {
      backgroundColor: colors.tooltipBg,
      borderColor: colors.borderColor,
      textStyle: { color: colors.textPrimary }
    },
    legend: {
      textStyle: { color: colors.textSecondary }
    },
    xAxis: {
      axisLine: { lineStyle: { color: colors.borderColor } },
      axisLabel: { color: colors.textSecondary }
    },
    yAxis: {
      axisLine: { lineStyle: { color: colors.borderColor } },
      axisLabel: { color: colors.textSecondary },
      splitLine: { lineStyle: { color: colors.splitLine } }
    },
    colors: [colors.chart1, colors.chart2, colors.chart3, colors.chart4]
  }
}

/**
 * 更新图表主题
 * @param {Object} chart - ECharts实例
 * @param {Object} option - 图表配置
 */
export function updateChartTheme(chart, option) {
  if (!chart) return
  
  const colors = getChartColors()
  
  // 更新tooltip颜色
  if (option.tooltip) {
    option.tooltip.backgroundColor = colors.tooltipBg
    option.tooltip.borderColor = colors.borderColor
    if (option.tooltip.textStyle) {
      option.tooltip.textStyle.color = colors.textPrimary
    }
  }
  
  // 更新legend颜色
  if (option.legend && option.legend.textStyle) {
    option.legend.textStyle.color = colors.textSecondary
  }
  
  // 更新xAxis颜色
  if (option.xAxis) {
    if (option.xAxis.axisLine) {
      option.xAxis.axisLine.lineStyle = option.xAxis.axisLine.lineStyle || {}
      option.xAxis.axisLine.lineStyle.color = colors.borderColor
    }
    if (option.xAxis.axisLabel) {
      option.xAxis.axisLabel.color = colors.textSecondary
    }
  }
  
  // 更新yAxis颜色
  if (option.yAxis) {
    if (option.yAxis.axisLine) {
      option.yAxis.axisLine.lineStyle = option.yAxis.axisLine.lineStyle || {}
      option.yAxis.axisLine.lineStyle.color = colors.borderColor
    }
    if (option.yAxis.axisLabel) {
      option.yAxis.axisLabel.color = colors.textSecondary
    }
    if (option.yAxis.splitLine) {
      option.yAxis.splitLine.lineStyle = option.yAxis.splitLine.lineStyle || {}
      option.yAxis.splitLine.lineStyle.color = colors.splitLine
    }
  }
  
  return option
}

// 默认导出
export default {
  getChartColors,
  getChartColor,
  getEChartsThemeConfig,
  updateChartTheme
}