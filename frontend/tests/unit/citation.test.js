import {
  parseCitationRefs,
  isSafeUrl,
  buildCitationText,
  tokenizeAnswer
} from '../../src/utils/citation.js'

describe('citation.js 纯逻辑', () => {
  test('parseCitationRefs 解析 [n] 引用模式并去重保序', () => {
    const text = '人工智能发展迅速[1]，机器学习[2]也很重要，再次引用[1]。'
    expect(parseCitationRefs(text)).toEqual([1, 2])
    expect(parseCitationRefs('没有引用的文本')).toEqual([])
    expect(parseCitationRefs('')).toEqual([])
    expect(parseCitationRefs(null)).toEqual([])
  })

  test('isSafeUrl 校验 URL 安全性：仅允许 http/https', () => {
    expect(isSafeUrl('https://example.com/a')).toBe(true)
    expect(isSafeUrl('http://example.com/b')).toBe(true)
    expect(isSafeUrl('javascript:alert(1)')).toBe(false)
    expect(isSafeUrl('data:text/html,<h1>x</h1>')).toBe(false)
    expect(isSafeUrl('')).toBe(false)
    expect(isSafeUrl(null)).toBe(false)
    expect(isSafeUrl('not a url')).toBe(false)
  })

  test('buildCitationText 将引用对象转换为纯文本', () => {
    const text = buildCitationText({
      index: 1,
      title: '人工智能综述',
      source: 'baidu',
      url: 'https://a.com/1'
    })
    expect(text).toContain('[1]')
    expect(text).toContain('人工智能综述')
    expect(text).toContain('baidu')
    expect(text).toContain('https://a.com/1')
    expect(buildCitationText(null)).toBe('')
  })

  test('tokenizeAnswer 将文本切分为普通片段与引用上标', () => {
    const tokens = tokenizeAnswer('人工智能[1]很重要。')
    const refs = tokens.filter(t => t.type === 'ref')
    expect(refs.length).toBe(1)
    expect(refs[0].index).toBe(1)
    expect(tokens.some(t => t.type === 'text' && t.value.includes('人工智能'))).toBe(true)
  })
})
