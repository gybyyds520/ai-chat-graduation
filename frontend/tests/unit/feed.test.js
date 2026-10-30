import {
  parseMultiPaths,
  formatFileSize,
  getEffectivenessColor,
  getEffectivenessLabel,
  parseFeedFeedback,
  hasKnowledgeBaseLink,
  getKnowledgePointCount
} from '../../src/utils/feed'

describe('feed AI 投喂纯逻辑工具', () => {
  describe('parseMultiPaths', () => {
    it('逗号分隔字符串应正确转数组', () => {
      expect(parseMultiPaths('/upload/a.txt,/upload/b.md,/upload/c.pdf')).toEqual([
        '/upload/a.txt',
        '/upload/b.md',
        '/upload/c.pdf'
      ])
    })

    it('空字符串应返回空数组', () => {
      expect(parseMultiPaths('')).toEqual([])
    })

    it('null / undefined 应返回空数组', () => {
      expect(parseMultiPaths(null)).toEqual([])
      expect(parseMultiPaths(undefined)).toEqual([])
    })

    it('单元素字符串应返回单元素数组', () => {
      expect(parseMultiPaths('/upload/only.png')).toEqual(['/upload/only.png'])
    })

    it('应去除首尾空白并过滤空元素', () => {
      expect(parseMultiPaths(' a ,  b ,')).toEqual(['a', 'b'])
    })
  })

  describe('formatFileSize', () => {
    it('0 字节应格式化为 0 B', () => {
      expect(formatFileSize(0)).toBe('0 B')
    })

    it('小于 1KB 应显示 B', () => {
      expect(formatFileSize(512)).toBe('512 B')
    })

    it('KB 级应正确换算', () => {
      expect(formatFileSize(1024)).toBe('1 KB')
    })

    it('MB 级应正确换算', () => {
      expect(formatFileSize(1024 * 1024)).toBe('1 MB')
    })
  })

  describe('getEffectivenessColor', () => {
    it('>=80 应返回 success', () => {
      expect(getEffectivenessColor(85).type).toBe('success')
      expect(getEffectivenessColor(100).type).toBe('success')
    })

    it('60-79 应返回 warning', () => {
      expect(getEffectivenessColor(70).type).toBe('warning')
      expect(getEffectivenessColor(60).type).toBe('warning')
    })

    it('<60 应返回 danger', () => {
      expect(getEffectivenessColor(50).type).toBe('danger')
      expect(getEffectivenessColor(0).type).toBe('danger')
    })
  })

  describe('getEffectivenessLabel', () => {
    it('>=80 应为优秀', () => {
      expect(getEffectivenessLabel(90)).toBe('优秀')
    })

    it('60-79 应为良好', () => {
      expect(getEffectivenessLabel(75)).toBe('良好')
      expect(getEffectivenessLabel(60)).toBe('良好')
    })

    it('40-59 应为一般', () => {
      expect(getEffectivenessLabel(50)).toBe('一般')
      expect(getEffectivenessLabel(40)).toBe('一般')
    })

    it('<40 应为较差', () => {
      expect(getEffectivenessLabel(30)).toBe('较差')
      expect(getEffectivenessLabel(0)).toBe('较差')
    })
  })

  describe('hasKnowledgeBaseLink', () => {
    it('documentId 非空应返回 true', () => {
      expect(hasKnowledgeBaseLink({ documentId: 42 })).toBe(true)
      expect(hasKnowledgeBaseLink({ documentId: 'doc-abc' })).toBe(true)
    })

    it('documentId 为 null / undefined 应返回 false', () => {
      expect(hasKnowledgeBaseLink({ documentId: null })).toBe(false)
      expect(hasKnowledgeBaseLink({})).toBe(false)
    })

    it('空对象 / null 应返回 false', () => {
      expect(hasKnowledgeBaseLink({})).toBe(false)
      expect(hasKnowledgeBaseLink(null)).toBe(false)
    })
  })

  describe('parseFeedFeedback', () => {
    it('应把文本作为 summary 返回', () => {
      const r = parseFeedFeedback('投喂效果良好。知识点已入库。')
      expect(r.summary).toBe('投喂效果良好。知识点已入库。')
      expect(Array.isArray(r.points)).toBe(true)
      expect(r.points.length).toBeGreaterThan(0)
    })

    it('空输入应返回空结构', () => {
      expect(parseFeedFeedback('')).toEqual({ summary: '', points: [] })
      expect(parseFeedFeedback(null)).toEqual({ summary: '', points: [] })
    })
  })

  describe('getKnowledgePointCount', () => {
    it('数组形式应返回长度', () => {
      expect(getKnowledgePointCount({ knowledgePoints: ['a', 'b', 'c'] })).toBe(3)
    })

    it('逗号分隔字符串应返回解析数量', () => {
      expect(getKnowledgePointCount({ knowledgePoints: 'x,y' })).toBe(2)
    })

    it('缺失应返回 0', () => {
      expect(getKnowledgePointCount({})).toBe(0)
      expect(getKnowledgePointCount(null)).toBe(0)
    })
  })
})
