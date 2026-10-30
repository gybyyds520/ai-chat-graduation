import {
  collectColumns,
  escapeCsvCell,
  toCsv,
  toJson,
  exportData,
  buildFileName
} from '../../src/utils/dataExport'

describe('dataExport 数据导出', () => {
  it('collectColumns 取并集并保持顺序', () => {
    const rows = [{ a: 1, b: 2 }, { b: 3, c: 4 }, { a: 5 }]
    expect(collectColumns(rows)).toEqual(['a', 'b', 'c'])
  })

  it('escapeCsvCell 逗号/引号/换行转义', () => {
    expect(escapeCsvCell('a,b')).toBe('"a,b"')
    expect(escapeCsvCell('a"b')).toBe('"a""b"')
    expect(escapeCsvCell('a\nb')).toBe('"a\nb"')
    expect(escapeCsvCell('plain')).toBe('plain')
  })

  it('toCsv 生成表头与数据行', () => {
    const rows = [{ name: '张三', age: 18 }, { name: '李四', age: 20 }]
    const csv = toCsv(rows)
    expect(csv).toContain('name,age')
    expect(csv).toContain('张三,18')
    expect(csv).toContain('李四,20')
  })

  it('toCsv 使用自定义 headers 映射', () => {
    const rows = [{ name: '张三', age: 18 }]
    const csv = toCsv(rows, { headers: { name: '姓名', age: '年龄' } })
    expect(csv).toContain('姓名,年龄')
  })

  it('toCsv 含逗号字段正确包裹', () => {
    const rows = [{ name: '张,三', city: '福州' }]
    const csv = toCsv(rows)
    expect(csv).toContain('"张,三",福州')
  })

  it('toCsv 空数据无列时返回空串', () => {
    expect(toCsv([])).toBe('')
  })

  it('toJson 输出数组', () => {
    const rows = [{ a: 1 }]
    const j = toJson(rows)
    expect(JSON.parse(j)).toEqual([{ a: 1 }])
  })

  it('exportData 按 format 分发', () => {
    const rows = [{ a: 1 }]
    expect(exportData(rows, 'csv')).toContain('a')
    expect(exportData(rows, 'json')).toContain('"a": 1')
  })

  it('buildFileName 带时间戳与扩展名', () => {
    const name = buildFileName('csv', new Date(2026, 8, 24, 10, 30, 5))
    expect(name).toBe('data_export_20260924_103005.csv')
    expect(buildFileName('json')).toContain('.json')
  })
})
