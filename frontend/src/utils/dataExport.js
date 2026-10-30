/**
 * 数据池 / 用户数据导出纯逻辑（便于单元测试）。
 *
 * - 把对象数组序列化为 CSV（带表头、字段转义、BOM 便于 Excel 打开）；
 * - 序列化为 JSON（pretty print）；
 * - 生成 Blob / 触发下载的 helper 由视图层调用，本模块只产出字符串。
 */

/**
 * 取所有行的并集字段作为表头（保持首次出现顺序）。
 */
export function collectColumns(rows) {
  const cols = []
  const seen = new Set()
  if (!Array.isArray(rows)) return cols
  for (const row of rows) {
    if (!row || typeof row !== 'object') continue
    for (const key of Object.keys(row)) {
      if (!seen.has(key)) {
        seen.add(key)
        cols.push(key)
      }
    }
  }
  return cols
}

/**
 * 转义单个 CSV 单元格：含逗号/引号/换行时用双引号包裹，内部双引号双写。
 */
export function escapeCsvCell(value) {
  if (value === null || value === undefined) return ''
  let s = String(value)
  if (/[",\n\r]/.test(s)) {
    s = '"' + s.replace(/"/g, '""') + '"'
  }
  return s
}

/**
 * 对象数组 -> CSV 字符串。
 * @param {Array} rows 数据行
 * @param {object} opts { columns 自定义列（缺省自动并集）, headers 列中文名映射 {col:label} }
 * @returns {string} 带 BOM 的 CSV
 */
export function toCsv(rows, opts = {}) {
  const columns = Array.isArray(opts.columns) && opts.columns.length
    ? opts.columns
    : collectColumns(rows)
  if (!columns.length) return ''

  const headers = columns.map(c => escapeCsvCell((opts.headers && opts.headers[c]) ? opts.headers[c] : c))
  const lines = [headers.join(',')]
  if (Array.isArray(rows)) {
    for (const row of rows) {
      if (!row || typeof row !== 'object') continue
      const cells = columns.map(c => escapeCsvCell(formatCellValue(row[c])))
      lines.push(cells.join(','))
    }
  }
  // 加 BOM 让 Excel 正确识别 UTF-8
  return '﻿' + lines.join('\r\n')
}

/**
 * 字段值格式化：对象转 JSON，其余原样字符串化。
 */
function formatCellValue(v) {
  if (v === null || v === undefined) return ''
  if (typeof v === 'object') {
    try { return JSON.stringify(v) } catch (e) { return String(v) }
  }
  return v
}

/**
 * 对象数组 -> JSON 字符串（pretty）。
 */
export function toJson(rows, opts = {}) {
  const data = Array.isArray(rows) ? rows : []
  return JSON.stringify(opts.pretty === false ? data : data, null, 2)
}

/**
 * 根据 format 选择导出格式。
 * @param {'csv'|'json'} format
 */
export function exportData(rows, format = 'csv', opts = {}) {
  if (format === 'json') return toJson(rows, opts)
  return toCsv(rows, opts)
}

/**
 * 生成导出文件名：data_export_YYYYMMDD_HHmmss.csv
 */
export function buildFileName(format = 'csv', now = new Date()) {
  const d = now instanceof Date ? now : new Date(now)
  const p = n => String(n).padStart(2, '0')
  const stamp = `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}_${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}`
  return `data_export_${stamp}.${format === 'json' ? 'json' : 'csv'}`
}

export default {
  collectColumns,
  escapeCsvCell,
  toCsv,
  toJson,
  exportData,
  buildFileName
}
