import {
  typeLabel,
  sourceLabel,
  importanceStars,
  importanceLabel,
  formatDateTime,
  typeTagType,
  buildMemorySearchParams,
  categoryLabel,
  MEMORY_TYPE_MAP,
  MEMORY_SOURCE_MAP,
  MEMORY_TYPE_OPTIONS,
  MEMORY_SOURCE_OPTIONS,
  MEMORY_CATEGORY_MAP,
  MEMORY_CATEGORY_OPTIONS
} from '../../src/utils/memoryUtils'

describe('memoryUtils AI 记忆纯逻辑', () => {
  describe('typeLabel 类型映射', () => {
    it('应将各类型映射为中文标签', () => {
      expect(typeLabel('fact')).toBe('事实')
      expect(typeLabel('preference')).toBe('偏好')
      expect(typeLabel('event')).toBe('事件')
      expect(typeLabel('personality')).toBe('性格')
      expect(typeLabel('other')).toBe('其他')
    })

    it('未知类型应回退为其他', () => {
      expect(typeLabel('unknown')).toBe('其他')
      expect(typeLabel(undefined)).toBe('其他')
    })

    it('类型映射覆盖全部 5 类', () => {
      expect(Object.keys(MEMORY_TYPE_MAP).sort()).toEqual(
        ['event', 'fact', 'other', 'personality', 'preference'].sort()
      )
    })
  })

  describe('importanceStars 星级渲染', () => {
    it('应按重要性输出对应数量实心星', () => {
      expect(importanceStars(1)).toBe('★☆☆☆☆')
      expect(importanceStars(3)).toBe('★★★☆☆')
      expect(importanceStars(5)).toBe('★★★★★')
    })

    it('越界值应被钳制在 1-5', () => {
      expect(importanceStars(0)).toBe('★☆☆☆☆')
      expect(importanceStars(99)).toBe('★★★★★')
    })

    it('非法值应回退为 3 星', () => {
      expect(importanceStars('abc')).toBe('★★★☆☆')
    })
  })

  describe('formatDateTime 日期格式化', () => {
    it('应格式化为 YYYY-MM-DD HH:mm', () => {
      expect(formatDateTime(new Date(2026, 8, 14, 9, 5))).toBe('2026-09-14 09:05')
    })

    it('空值应返回占位符', () => {
      expect(formatDateTime(null)).toBe('—')
    })
  })

  describe('sourceLabel / typeTagType', () => {
    it('来源映射', () => {
      expect(sourceLabel('chat')).toBe('聊天沉淀')
      expect(sourceLabel('manual')).toBe('手动添加')
      expect(sourceLabel(undefined)).toBe('其他')
    })

    it('标签类型映射', () => {
      expect(typeTagType('event')).toBe('warning')
      expect(typeTagType('personality')).toBe('danger')
    })

    it('重要性中文描述', () => {
      expect(importanceLabel(5)).toBe('很高')
      expect(importanceLabel(1)).toBe('很低')
    })
  })

  describe('来源筛选与下拉项', () => {
    it('来源映射覆盖全部 4 类', () => {
      expect(Object.keys(MEMORY_SOURCE_MAP).sort()).toEqual(
        ['chat', 'manual', 'profile', 'system'].sort()
      )
      expect(sourceLabel('profile')).toBe('资料')
      expect(sourceLabel('system')).toBe('系统')
    })

    it('来源下拉首项为全部', () => {
      expect(MEMORY_SOURCE_OPTIONS[0]).toEqual({ value: '', label: '全部来源' })
      expect(MEMORY_SOURCE_OPTIONS.some((o) => o.value === 'chat')).toBe(true)
      expect(MEMORY_SOURCE_OPTIONS.some((o) => o.value === 'manual')).toBe(true)
    })

    it('类型下拉首项为全部', () => {
      expect(MEMORY_TYPE_OPTIONS[0]).toEqual({ value: '', label: '全部' })
      expect(MEMORY_TYPE_OPTIONS.some((o) => o.value === 'event')).toBe(true)
    })
  })

  describe('buildMemorySearchParams 查询参数构建', () => {
    it('全部过滤为空时只保留 page/size', () => {
      const p = buildMemorySearchParams('', '', '', 1, 50)
      expect(p).toEqual({ page: 1, size: 50 })
      expect(p).not.toHaveProperty('keyword')
      expect(p).not.toHaveProperty('source')
      expect(p).not.toHaveProperty('memoryType')
      expect(p).not.toHaveProperty('userId')
    })

    it('keyword 非空时应包含 keyword 并去除首尾空白', () => {
      const p = buildMemorySearchParams('  生日  ', '', '', 1, 50)
      expect(p.keyword).toBe('生日')
      expect(p.source).toBeUndefined()
      expect(p.memoryType).toBeUndefined()
    })

    it('source 非空时应包含 source', () => {
      const p = buildMemorySearchParams('', 'chat', '', 2, 20)
      expect(p.source).toBe('chat')
      expect(p.page).toBe(2)
      expect(p.size).toBe(20)
      expect(p.keyword).toBeUndefined()
    })

    it('type 非空时应映射为 memoryType', () => {
      const p = buildMemorySearchParams('', '', 'preference', 1, 50)
      expect(p.memoryType).toBe('preference')
    })

    it('纯空白过滤值应被忽略，多个过滤可同时存在', () => {
      const p = buildMemorySearchParams('   ', '   ', '', 1, 50)
      expect(p).toEqual({ page: 1, size: 50 })
      const multi = buildMemorySearchParams('宠物', 'manual', 'fact', 3, 10)
      expect(multi).toEqual({ page: 3, size: 10, keyword: '宠物', source: 'manual', memoryType: 'fact' })
    })
  })

  describe('市场级升级：自动分类标签', () => {
    it('categoryLabel 应将四类枚举映射为中文', () => {
      expect(categoryLabel('FACT')).toBe('事实')
      expect(categoryLabel('PREFERENCE')).toBe('偏好')
      expect(categoryLabel('EVENT')).toBe('事件')
      expect(categoryLabel('RELATIONSHIP')).toBe('关系')
    })

    it('未知/空分类应回退原值或空串', () => {
      expect(categoryLabel('饮食')).toBe('饮食')
      expect(categoryLabel('')).toBe('')
      expect(categoryLabel(undefined)).toBe('')
    })

    it('分类映射覆盖全部 4 类', () => {
      expect(Object.keys(MEMORY_CATEGORY_MAP).sort()).toEqual(
        ['EVENT', 'FACT', 'PREFERENCE', 'RELATIONSHIP'].sort()
      )
    })

    it('分类 Tab 首项为全部，含 4 个枚举', () => {
      expect(MEMORY_CATEGORY_OPTIONS[0]).toEqual({ value: '', label: '全部分类' })
      expect(MEMORY_CATEGORY_OPTIONS.filter((o) => o.value).map((o) => o.value).sort())
        .toEqual(['EVENT', 'FACT', 'PREFERENCE', 'RELATIONSHIP'].sort())
    })
  })
})
