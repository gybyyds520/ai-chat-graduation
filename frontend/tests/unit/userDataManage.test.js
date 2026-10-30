import {
  getDataTypeLabel,
  getStatusLabel,
  getBackupStatusLabel,
  getIntegrityPercent,
  getIntegrityGrade,
  getIntegrityTagType,
  getStatusTagType,
  getBackupStatusTagType,
  truncateContent,
  maskPhone,
  maskEmail,
  maskSensitive,
  buildRecordQueryParams
} from '../../src/utils/userDataManage'

describe('userDataManage 用户数据管理纯逻辑', () => {
  describe('数据类型显示名映射', () => {
    it('已知类型应映射为中文名称', () => {
      expect(getDataTypeLabel('social_post')).toBe('社区帖子')
      expect(getDataTypeLabel('ai_chat')).toBe('AI对话')
      expect(getDataTypeLabel('ai_elf_interaction')).toBe('桌宠交互')
      expect(getDataTypeLabel('web_search')).toBe('网页搜索')
    })

    it('未知类型应原样返回', () => {
      expect(getDataTypeLabel('some_unknown_type')).toBe('some_unknown_type')
    })

    it('空值应返回 -', () => {
      expect(getDataTypeLabel('')).toBe('-')
      expect(getDataTypeLabel(null)).toBe('-')
    })
  })

  describe('完整性评分归一化与等级', () => {
    it('0~1 制分数应换算为百分制', () => {
      expect(getIntegrityPercent(0.5)).toBe(50)
      expect(getIntegrityPercent(1)).toBe(100)
      expect(getIntegrityPercent(0)).toBe(0)
    })

    it('百分制分数应保持不变', () => {
      expect(getIntegrityPercent(85)).toBe(85)
      expect(getIntegrityPercent(60)).toBe(60)
    })

    it('空值/非法值应返回 0', () => {
      expect(getIntegrityPercent(null)).toBe(0)
      expect(getIntegrityPercent(undefined)).toBe(0)
      expect(getIntegrityPercent('abc')).toBe(0)
    })

    it('等级判断边界', () => {
      expect(getIntegrityGrade(90)).toBe('excellent') // >=80
      expect(getIntegrityGrade(80)).toBe('excellent')
      expect(getIntegrityGrade(79)).toBe('good') // 60~79
      expect(getIntegrityGrade(60)).toBe('good')
      expect(getIntegrityGrade(59)).toBe('fair') // 40~59
      expect(getIntegrityGrade(40)).toBe('fair')
      expect(getIntegrityGrade(39)).toBe('poor') // <40
      expect(getIntegrityGrade(0.5)).toBe('fair') // 0.5=>50 => fair
    })
  })

  describe('完整性评分对应颜色', () => {
    it('优秀绿 / 良好黄 / 一般黄(橙) / 较差红', () => {
      expect(getIntegrityTagType(90)).toBe('success')
      expect(getIntegrityTagType(70)).toBe('warning')
      expect(getIntegrityTagType(50)).toBe('warning')
      expect(getIntegrityTagType(30)).toBe('danger')
    })
  })

  describe('状态/备份状态标签', () => {
    it('状态显示名与颜色', () => {
      expect(getStatusLabel('VALIDATED')).toBe('已验证')
      expect(getStatusLabel('ARCHIVED')).toBe('已归档')
      expect(getStatusTagType('DELETED')).toBe('danger')
      expect(getStatusTagType('UNKNOWN')).toBe('info')
    })

    it('备份状态显示名与颜色', () => {
      expect(getBackupStatusLabel('BACKED_UP')).toBe('已备份')
      expect(getBackupStatusLabel('PENDING_BACKUP')).toBe('待备份')
      expect(getBackupStatusTagType('FAILED')).toBe('danger')
    })
  })

  describe('内容摘要截断', () => {
    it('短文本应原样返回', () => {
      expect(truncateContent('hello', 40)).toBe('hello')
    })

    it('超长文本应截断并加省略号', () => {
      const long = 'a'.repeat(100)
      const result = truncateContent(long, 40)
      expect(result.length).toBe(43) // 40 + '...'
      expect(result.endsWith('...')).toBe(true)
    })

    it('默认长度为 40', () => {
      expect(truncateContent('x'.repeat(50)).length).toBe(43)
    })

    it('空值应返回空串', () => {
      expect(truncateContent(null)).toBe('')
      expect(truncateContent(undefined)).toBe('')
    })
  })

  describe('敏感信息掩码', () => {
    it('手机号应掩码为 138****5678', () => {
      expect(maskPhone('我的电话是13812345678')).toBe('我的电话是138****5678')
    })

    it('邮箱应掩码为 z***@example.com', () => {
      expect(maskEmail('zhangsan@example.com')).toBe('z***@example.com')
    })

    it('maskSensitive 应同时处理手机号与邮箱', () => {
      const text = '联系13812345678 或 zhangsan@example.com'
      const result = maskSensitive(text)
      expect(result).toContain('138****5678')
      expect(result).toContain('z***@example.com')
    })

    it('无敏感信息应原样返回', () => {
      expect(maskSensitive('普通文本')).toBe('普通文本')
      expect(maskSensitive('')).toBe('')
    })
  })

  describe('筛选参数构建', () => {
    it('空筛选只传 page/size', () => {
      const params = buildRecordQueryParams({ page: 1, size: 20 })
      expect(params).toEqual({ page: 1, size: 20 })
      expect(params.userId).toBeUndefined()
      expect(params.dataType).toBeUndefined()
      expect(params.keyword).toBeUndefined()
    })

    it('非空筛选应传入对应参数', () => {
      const params = buildRecordQueryParams({
        page: 2,
        size: 50,
        userId: 42,
        dataType: 'ai_chat',
        keyword: '你好'
      })
      expect(params).toEqual({
        page: 2,
        size: 50,
        userId: 42,
        dataType: 'ai_chat',
        keyword: '你好'
      })
    })

    it('空字符串/空 userId 不应传入', () => {
      const params = buildRecordQueryParams({
        page: 1,
        size: 10,
        userId: '',
        dataType: '',
        keyword: ''
      })
      expect(params).toEqual({ page: 1, size: 10 })
    })

    it('默认 page/size 为 1/20', () => {
      const params = buildRecordQueryParams({})
      expect(params.page).toBe(1)
      expect(params.size).toBe(20)
    })
  })
})
