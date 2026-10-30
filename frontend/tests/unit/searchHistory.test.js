import {
  loadHistory,
  saveHistory,
  pushHistory,
  recordSearch,
  removeHistoryItem,
  clearHistory,
  suggest
} from '../../src/utils/searchHistory'

// 内存 Storage，避免污染真实 localStorage
function makeMemoryStorage() {
  const map = new Map()
  return {
    getItem: k => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, String(v)),
    removeItem: k => map.delete(k),
    _map: map
  }
}

describe('searchHistory 搜索历史与建议', () => {
  it('空存储读取返回空数组', () => {
    const st = makeMemoryStorage()
    expect(loadHistory(st, 'k')).toEqual([])
  })

  it('非法 JSON 容错返回空数组', () => {
    const st = makeMemoryStorage()
    st.setItem('k', '{oops')
    expect(loadHistory(st, 'k')).toEqual([])
  })

  it('pushHistory 新搜索置顶', () => {
    const next = pushHistory('天气', ['新闻', '股票'])
    expect(next[0]).toBe('天气')
    expect(next.length).toBe(3)
  })

  it('重复搜索去重并置顶', () => {
    const next = pushHistory('股票', ['新闻', '股票', '天气'])
    expect(next).toEqual(['股票', '新闻', '天气'])
  })

  it('超过 max 截断到 10 条', () => {
    const base = Array.from({ length: 12 }, (_, i) => `q${i}`)
    const next = pushHistory('新', base, 10)
    expect(next.length).toBe(10)
    expect(next[0]).toBe('新')
  })

  it('空查询不写入', () => {
    expect(pushHistory('   ', ['a'])).toEqual(['a'])
    expect(pushHistory('', ['a'])).toEqual(['a'])
  })

  it('recordSearch 持久化并返回', () => {
    const st = makeMemoryStorage()
    const next = recordSearch('火锅', st, 'k')
    expect(next).toEqual(['火锅'])
    expect(JSON.parse(st.getItem('k'))).toEqual(['火锅'])
  })

  it('removeHistoryItem 删除指定项', () => {
    const st = makeMemoryStorage()
    recordSearch('a', st, 'k'); recordSearch('b', st, 'k')
    const next = removeHistoryItem('a', st, 'k')
    expect(next).toEqual(['b'])
  })

  it('clearHistory 清空', () => {
    const st = makeMemoryStorage()
    recordSearch('a', st, 'k')
    clearHistory(st, 'k')
    expect(loadHistory(st, 'k')).toEqual([])
  })

  it('suggest 空输入返回最近历史', () => {
    const list = ['苹果', '香蕉', '橙子']
    expect(suggest('', list, { historyLength: 2 })).toEqual(['苹果', '香蕉'])
  })

  it('suggest 前缀匹配优先于包含匹配', () => {
    const list = ['天气预报', '北京天气', '天气', '今天吃什么']
    const r = suggest('天气', list)
    // 以"天气"开头的排在前面
    expect(r).toContain('天气预报')
    expect(r[0]).toBe('天气预报')
  })

  it('suggest 排除完全相同项', () => {
    const list = ['天气预报', '天气']
    const r = suggest('天气', list)
    expect(r).not.toContain('天气')
  })

  it('suggest 不区分大小写', () => {
    const list = ['OpenAI 是什么', 'openchat 怎么用']
    const r = suggest('open', list)
    expect(r.length).toBeGreaterThan(0)
  })

  it('suggest 无命中返回空', () => {
    expect(suggest('xyz不存在', ['苹果'])).toEqual([])
  })
})
