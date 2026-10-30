/**
 * 角色创建/编辑表单纯逻辑（标签解析、字段校验）。
 */

/**
 * 解析标签输入（支持逗号/顿号/空格分隔，去空去重，限制数量）。
 * @param {string} input 用户输入，如 "治愈, 陪伴 日常"
 * @param {number} max 最多标签数
 * @returns {string[]}
 */
export function parseTagInput(input, max = 8) {
  if (!input) return []
  const tags = input
    .split(/[,，、\s]+/)
    .map((t) => t.trim())
    .filter(Boolean)
    .filter((t, i, arr) => arr.indexOf(t) === i)
  return tags.slice(0, max)
}

/**
 * 校验角色表单，返回错误信息（空串表示通过）。
 * @param {{name:string, prompt:string}} form
 * @returns {string}
 */
export function validateCharacterForm(form) {
  if (!form || !form.name || form.name.trim().length < 2 || form.name.trim().length > 20) {
    return '角色名称需为 2-20 个字符'
  }
  if (!form.prompt || form.prompt.trim().length < 10) {
    return '角色描述至少需要 10 个字符'
  }
  return ''
}

/**
 * 开场白为空时回退为默认提示（详情/卡片摘要处理）。
 * @param {string} greeting
 * @param {string} name
 * @returns {string}
 */
export function fallbackGreeting(greeting, name) {
  if (greeting && greeting.trim()) return greeting.trim()
  return `你好，我是${name || '新角色'}，很高兴认识你～`
}
