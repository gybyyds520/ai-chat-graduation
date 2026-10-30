import {
  researchStatusText,
  isResearchFinished,
  estimateProgress,
  formatSummary
} from '../../src/utils/research.js'

describe('research.js 深度研究纯逻辑', () => {
  test('researchStatusText 状态码转换为中文描述', () => {
    expect(researchStatusText('searching')).toBe('检索中')
    expect(researchStatusText('completed')).toBe('已完成')
    expect(researchStatusText('failed')).toBe('失败')
    expect(researchStatusText('round2')).toBe('第二轮检索')
    expect(researchStatusText('unknown')).toBe('unknown')
  })

  test('isResearchFinished 判断终态', () => {
    expect(isResearchFinished('completed')).toBe(true)
    expect(isResearchFinished('failed')).toBe(true)
    expect(isResearchFinished('searching')).toBe(false)
    expect(isResearchFinished('pending')).toBe(false)
  })

  test('estimateProgress 进度随轮次递增且终态为100', () => {
    expect(estimateProgress('completed', 3, 3)).toBe(100)
    expect(estimateProgress('failed', 1, 3)).toBe(100)
    const p1 = estimateProgress('searching', 1, 3)
    const p3 = estimateProgress('integrating', 3, 3)
    expect(p1).toBeLessThan(p3)
    expect(p1).toBeGreaterThanOrEqual(0)
    expect(p3).toBeLessThanOrEqual(100)
  })

  test('formatSummary 压缩空白并截断', () => {
    expect(formatSummary('关于  AI  的应用')).toBe('关于 AI 的应用')
    expect(formatSummary(null)).toBe('')
    expect(formatSummary('一二三四五六七八九十', 5)).toBe('一二三四五…')
  })
})
