import {
  parseAgreementSection,
  countAgreementClauses,
  isAgreementAccepted,
  normalizeChineseNumber,
  USER_AGREEMENT_TEXT,
  PRIVACY_POLICY_TEXT
} from '../../src/utils/legalDocs'

describe('legalDocs 法律文档解析', () => {
  describe('parseAgreementSection', () => {
    it('空输入应返回空结构', () => {
      const empty = { title: '', preamble: '', clauses: [], total: 0 }
      expect(parseAgreementSection('')).toEqual(empty)
      expect(parseAgreementSection(null)).toEqual(empty)
      expect(parseAgreementSection(undefined)).toEqual(empty)
      expect(parseAgreementSection('   \n  \n')).toEqual(empty)
    })

    it('应解析标题、前言与条款', () => {
      const content = `用户协议

这是前言段落。

第一条 服务条款
用户应遵守本协议。

第二条 账号使用
用户应妥善保管账号。
请定期修改密码。`
      const result = parseAgreementSection(content)
      expect(result.title).toBe('用户协议')
      expect(result.preamble).toContain('这是前言段落')
      expect(result.total).toBe(2)
      expect(result.clauses[0].index).toBe(1)
      expect(result.clauses[0].title).toBe('服务条款')
      expect(result.clauses[0].body).toBe('用户应遵守本协议。')
      expect(result.clauses[1].index).toBe(2)
      expect(result.clauses[1].title).toBe('账号使用')
      // 多条正文应被换行拼接
      expect(result.clauses[1].body).toBe('用户应妥善保管账号。\n请定期修改密码。')
    })

    it('应支持中文数字序号（第十、第二十一）', () => {
      const content = `测试协议

第一条 一
正文一。

第十条 十
正文十。

第二十一条 二十一
正文二十一。`
      const result = parseAgreementSection(content)
      expect(result.clauses.map((c) => c.index)).toEqual([1, 10, 21])
      expect(result.clauses[2].title).toBe('二十一')
    })

    it('真实用户协议文本应解析出 12 条且序号连续', () => {
      const result = parseAgreementSection(USER_AGREEMENT_TEXT)
      expect(result.title).toBe('用户协议')
      expect(result.total).toBe(12)
      expect(result.clauses[0].index).toBe(1)
      expect(result.clauses[11].index).toBe(12)
      expect(result.clauses[0].title.length).toBeGreaterThan(0)
      expect(result.clauses[0].body.length).toBeGreaterThan(0)
    })

    it('真实隐私政策文本应解析出 11 条', () => {
      const result = parseAgreementSection(PRIVACY_POLICY_TEXT)
      expect(result.title).toBe('隐私政策')
      expect(result.total).toBe(11)
      expect(result.clauses[10].index).toBe(11)
    })
  })

  describe('countAgreementClauses', () => {
    it('应返回条款总数', () => {
      expect(countAgreementClauses(USER_AGREEMENT_TEXT)).toBe(12)
      expect(countAgreementClauses(PRIVACY_POLICY_TEXT)).toBe(11)
      expect(countAgreementClauses('')).toBe(0)
    })
  })

  describe('isAgreementAccepted', () => {
    it('仅 true / "true" 视为已同意', () => {
      expect(isAgreementAccepted(true)).toBe(true)
      expect(isAgreementAccepted('true')).toBe(true)
      expect(isAgreementAccepted(false)).toBe(false)
      expect(isAgreementAccepted(undefined)).toBe(false)
      expect(isAgreementAccepted('')).toBe(false)
    })
  })

  describe('normalizeChineseNumber', () => {
    it('阿拉伯数字直接解析', () => {
      expect(normalizeChineseNumber('5')).toBe(5)
      expect(normalizeChineseNumber('12')).toBe(12)
    })

    it('中文数字正确归一', () => {
      expect(normalizeChineseNumber('一')).toBe(1)
      expect(normalizeChineseNumber('十')).toBe(10)
      expect(normalizeChineseNumber('十一')).toBe(11)
      expect(normalizeChineseNumber('二十')).toBe(20)
      expect(normalizeChineseNumber('二十一')).toBe(21)
    })
  })
})
