import {
  progressPercent,
  formatFileSize,
  docStatusLabel,
  docStatusType,
  canReparse,
  buildAskBody
} from '../../src/utils/kbUpload'

describe('kbUpload 知识库上传与列表展示', () => {
  it('progressPercent 正常计算', () => {
    expect(progressPercent(50, 100)).toBe(50)
    expect(progressPercent(100, 100)).toBe(100)
    expect(progressPercent(0, 100)).toBe(0)
  })

  it('progressPercent total=0 不除零', () => {
    expect(progressPercent(0, 0)).toBe(0)
    expect(progressPercent(10, 0)).toBe(50)
  })

  it('progressPercent 钳制到 0-100', () => {
    expect(progressPercent(150, 100)).toBe(100)
    expect(progressPercent(-5, 100)).toBe(0)
  })

  it('formatFileSize 各级单位', () => {
    expect(formatFileSize(512)).toBe('512 B')
    expect(formatFileSize(2048)).toBe('2 KB')
    expect(formatFileSize(5 * 1024 * 1024)).toBe('5 MB')
    expect(formatFileSize(0)).toBe('0 B')
  })

  it('docStatusLabel 状态中文', () => {
    expect(docStatusLabel('ready')).toBe('就绪')
    expect(docStatusLabel('processing')).toBe('解析中')
    expect(docStatusLabel('failed')).toBe('失败')
    expect(docStatusLabel('xxx')).toBe('未知')
  })

  it('docStatusType Element 标签类型', () => {
    expect(docStatusType('ready')).toBe('success')
    expect(docStatusType('processing')).toBe('warning')
    expect(docStatusType('failed')).toBe('danger')
  })

  it('canReparse 处理中不可重新解析', () => {
    expect(canReparse('ready')).toBe(true)
    expect(canReparse('failed')).toBe(true)
    expect(canReparse('processing')).toBe(false)
  })

  it('buildAskBody 空 documentIds 视为全部', () => {
    const body = buildAskBody('什么是 RAG', null, 3)
    expect(body.query).toBe('什么是 RAG')
    expect(body.documentIds).toBeNull()
  })

  it('buildAskBody 指定文档清洗非法 ID', () => {
    const body = buildAskBody('问题', [1, 2, 'abc', -3, 5], 5)
    expect(body.documentIds).toEqual([1, 2, 5])
    expect(body.topK).toBe(5)
  })

  it('buildAskBody 空查询 trim', () => {
    expect(buildAskBody('  ', [1]).query).toBe('')
  })
})
