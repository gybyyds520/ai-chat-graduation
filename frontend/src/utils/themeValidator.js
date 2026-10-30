/**
 * 主题切换验证脚本
 * 用于测试和验证所有页面的主题切换效果
 */

// 验证结果存储
const validationResults = []

/**
 * 验证CSS变量是否存在且有值
 */
function validateCssVariables() {
  const requiredVariables = [
    '--bg-primary', '--bg-secondary', '--bg-tertiary', '--bg-hover',
    '--text-primary', '--text-secondary', '--text-tertiary', '--text-muted', '--text-inverse',
    '--border-primary', '--border-secondary', '--border-focus',
    '--color-primary', '--color-success', '--color-warning', '--color-error',
    '--card-bg', '--sidebar-bg', '--navbar-bg', '--modal-bg'
  ]

  const computedStyle = getComputedStyle(document.documentElement)
  const results = []

  requiredVariables.forEach(variable => {
    const value = computedStyle.getPropertyValue(variable).trim()
    results.push({
      variable,
      value,
      isValid: value !== '',
      message: value ? '✓ 变量已定义' : '✗ 变量未定义'
    })
  })

  return results
}

/**
 * 验证元素样式是否正确响应主题
 */
function validateElementTheming() {
  const elementsToCheck = [
    { selector: 'body', property: 'backgroundColor', name: '页面背景' },
    { selector: '.admin-container', property: 'backgroundColor', name: 'Admin容器背景' },
    { selector: '.feed-module', property: 'background', name: 'Feed模块背景' },
    { selector: '.chat-view', property: 'backgroundColor', name: '聊天视图背景' },
    { selector: '.stat-card', property: 'backgroundColor', name: '统计卡片背景' },
    { selector: '.sidebar', property: 'backgroundColor', name: '侧边栏背景' },
    { selector: '.menu-item', property: 'color', name: '菜单项文字' },
    { selector: '.btn-primary', property: 'backgroundColor', name: '主按钮背景' },
    { selector: '.el-input__inner', property: 'backgroundColor', name: '输入框背景' },
    { selector: 'h1', property: 'color', name: '标题文字颜色' },
    { selector: 'p', property: 'color', name: '段落文字颜色' },
    { selector: '.el-card', property: 'backgroundColor', name: 'Element卡片背景' },
    { selector: '.el-button--primary', property: 'backgroundColor', name: 'Element主按钮背景' }
  ]

  const results = []

  elementsToCheck.forEach(check => {
    const element = document.querySelector(check.selector)
    if (element) {
      const computedStyle = getComputedStyle(element)
      const value = computedStyle[check.property]
      results.push({
        selector: check.selector,
        name: check.name,
        property: check.property,
        value,
        isValid: value && value !== 'transparent',
        message: value && value !== 'transparent' ? '✓ 样式已应用' : '✗ 样式未正确应用'
      })
    } else {
      results.push({
        selector: check.selector,
        name: check.name,
        property: check.property,
        value: '元素未找到',
        isValid: false,
        message: '⚠ 元素不存在（可能不在当前页面）'
      })
    }
  })

  return results
}

/**
 * 验证主题切换功能
 */
function validateThemeSwitch() {
  const results = []
  
  // 检查主题属性
  const theme = document.documentElement.getAttribute('data-theme')
  const hasLightClass = document.documentElement.classList.contains('light')
  const hasDarkClass = document.documentElement.classList.contains('dark')

  results.push({
    check: 'data-theme 属性',
    value: theme || '未设置',
    isValid: theme === 'light' || theme === 'dark',
    message: theme ? `✓ 当前主题: ${theme}` : '✗ 主题未设置'
  })

  results.push({
    check: 'light 类',
    value: hasLightClass ? '存在' : '不存在',
    isValid: true,
    message: hasLightClass ? '✓ light类已添加' : '✓ light类未添加（正常）'
  })

  results.push({
    check: 'dark 类',
    value: hasDarkClass ? '存在' : '不存在',
    isValid: true,
    message: hasDarkClass ? '✓ dark类已添加' : '✓ dark类未添加（正常）'
  })

  // 检查localStorage存储
  const storedTheme = localStorage.getItem('theme')
  results.push({
    check: 'localStorage 存储',
    value: storedTheme || '未存储',
    isValid: storedTheme === 'light' || storedTheme === 'dark',
    message: storedTheme ? `✓ 已存储: ${storedTheme}` : '✗ 未存储'
  })

  return results
}

/**
 * 执行完整验证
 */
export function runFullValidation() {
  console.log('========================================')
  console.log('主题切换验证脚本')
  console.log('========================================')

  const results = {
    cssVariables: validateCssVariables(),
    elementTheming: validateElementTheming(),
    themeSwitch: validateThemeSwitch()
  }

  // 输出CSS变量验证结果
  console.log('\n【CSS变量验证】')
  console.log('────────────────────────────────────────')
  results.cssVariables.forEach(result => {
    console.log(`${result.message} - ${result.variable}: ${result.value}`)
  })

  // 输出元素样式验证结果
  console.log('\n【元素样式验证】')
  console.log('────────────────────────────────────────')
  results.elementTheming.forEach(result => {
    console.log(`${result.message} - ${result.name} (${result.selector}): ${result.value}`)
  })

  // 输出主题切换验证结果
  console.log('\n【主题切换验证】')
  console.log('────────────────────────────────────────')
  results.themeSwitch.forEach(result => {
    console.log(`${result.message}`)
  })

  // 统计结果
  const allResults = [
    ...results.cssVariables,
    ...results.elementTheming,
    ...results.themeSwitch
  ]

  const passed = allResults.filter(r => r.isValid).length
  const total = allResults.length
  const percentage = ((passed / total) * 100).toFixed(1)

  console.log('\n【验证统计】')
  console.log('────────────────────────────────────────')
  console.log(`通过: ${passed}/${total} (${percentage}%)`)

  if (passed === total) {
    console.log('✅ 所有验证通过！')
  } else {
    console.log('⚠ 部分验证未通过，请检查上述项目')
  }

  validationResults.push({
    timestamp: new Date().toISOString(),
    theme: document.documentElement.getAttribute('data-theme'),
    results,
    passed,
    total,
    percentage
  })

  return { results, passed, total, percentage }
}

/**
 * 切换主题并验证
 */
export function switchThemeAndValidate(targetTheme = 'dark') {
  console.log(`\n切换到 ${targetTheme} 主题...`)
  
  // 模拟主题切换
  document.documentElement.setAttribute('data-theme', targetTheme)
  localStorage.setItem('theme', targetTheme)
  
  if (targetTheme === 'dark') {
    document.documentElement.classList.add('dark')
    document.documentElement.classList.remove('light')
  } else {
    document.documentElement.classList.add('light')
    document.documentElement.classList.remove('dark')
  }

  // 等待DOM更新
  setTimeout(() => {
    runFullValidation()
  }, 300)
}

/**
 * 获取验证历史
 */
export function getValidationHistory() {
  return validationResults
}

/**
 * 导出验证报告
 */
export function exportValidationReport() {
  const report = {
    exportTime: new Date().toISOString(),
    totalValidations: validationResults.length,
    results: validationResults
  }

  const json = JSON.stringify(report, null, 2)
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)

  const a = document.createElement('a')
  a.href = url
  a.download = `theme-validation-report-${Date.now()}.json`
  a.click()

  URL.revokeObjectURL(url)
}

// 自动运行验证（如果在浏览器环境中）
if (typeof window !== 'undefined') {
  // 在页面加载完成后自动运行验证
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', runFullValidation)
  } else {
    runFullValidation()
  }
}

// 默认导出
export default {
  runFullValidation,
  switchThemeAndValidate,
  getValidationHistory,
  exportValidationReport,
  validateCssVariables,
  validateElementTheming,
  validateThemeSwitch
}