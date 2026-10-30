import {
  filterByCategory,
  parseTags,
  sortCharacters,
  formatUseCount,
  canLoadMore,
  ALL_CATEGORIES
} from '../../src/utils/plaza'

describe('角色广场纯逻辑', () => {
  describe('filterByCategory', () => {
    it('全部/空分类应返回全部', () => {
      const list = [{ category: '治愈' }, { category: '知识' }]
      expect(filterByCategory(list, ALL_CATEGORIES)).toHaveLength(2)
      expect(filterByCategory(list, '')).toHaveLength(2)
    })

    it('指定分类应只保留匹配项', () => {
      const list = [{ category: '治愈' }, { category: '知识' }, { category: '治愈' }]
      const r = filterByCategory(list, '治愈')
      expect(r).toHaveLength(2)
      expect(r.every((c) => c.category === '治愈')).toBe(true)
    })
  })

  describe('sortCharacters', () => {
    it('hot 应按 useCount 降序', () => {
      const list = [{ id: 1, useCount: 5 }, { id: 2, useCount: 100 }, { id: 3, useCount: 40 }]
      const r = sortCharacters(list, 'hot')
      expect(r.map((c) => c.id)).toEqual([2, 3, 1])
    })

    it('rating 应按评分降序', () => {
      const list = [{ id: 1, rating: 3 }, { id: 2, rating: 5 }, { id: 3, rating: 4 }]
      const r = sortCharacters(list, 'rating')
      expect(r.map((c) => c.id)).toEqual([2, 3, 1])
    })

    it('created 应按 id 倒序', () => {
      const list = [{ id: 1 }, { id: 3 }, { id: 2 }]
      expect(sortCharacters(list, 'created').map((c) => c.id)).toEqual([3, 2, 1])
    })
  })

  describe('parseTags / formatUseCount', () => {
    it('parseTags 应去空去重并支持中英文逗号', () => {
      expect(parseTags('治愈,陪伴，治愈')).toEqual(['治愈', '陪伴'])
      expect(parseTags('')).toEqual([])
    })

    it('formatUseCount 应格式化大数字', () => {
      expect(formatUseCount(0)).toBe('0')
      expect(formatUseCount(850)).toBe('850')
      expect(formatUseCount(1234)).toBe('1.2k')
      expect(formatUseCount(23456)).toBe('2.3w')
    })
  })

  describe('canLoadMore', () => {
    it('当前页未覆盖完总数应返回 true', () => {
      expect(canLoadMore(25, 1, 12)).toBe(true)
      expect(canLoadMore(25, 2, 12)).toBe(true)
    })

    it('当前页覆盖完总数应返回 false', () => {
      expect(canLoadMore(24, 2, 12)).toBe(false)
      expect(canLoadMore(0, 1, 12)).toBe(false)
    })
  })
})
