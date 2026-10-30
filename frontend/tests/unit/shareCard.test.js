import { computeCardLayout, wrapTextCount, SHARE_CARD_WIDTH } from '../../src/utils/shareCard'

describe('对话分享卡片布局纯逻辑', () => {
  it('空消息应只含头部+底部高度', () => {
    const layout = computeCardLayout([])
    expect(layout.height).toBeGreaterThan(100)
    expect(layout.bubbleHeights).toEqual([])
  })

  it('单条消息应产出一行气泡高度', () => {
    const layout = computeCardLayout([{ role: 'assistant', content: '你好呀' }])
    expect(layout.bubbleHeights.length).toBe(1)
    expect(layout.bubbleHeights[0]).toBeGreaterThanOrEqual(56)
  })

  it('多条消息高度累加', () => {
    const a = computeCardLayout([{ role: 'user', content: '嗨' }])
    const b = computeCardLayout([{ role: 'user', content: '嗨' }, { role: 'assistant', content: '你好' }])
    expect(b.height).toBeGreaterThan(a.height)
  })

  it('宽度默认 600', () => {
    expect(computeCardLayout([]).width).toBe(SHARE_CARD_WIDTH)
  })

  it('wrapTextCount：短文本一行', () => {
    expect(wrapTextCount('你好', 400, 15)).toBe(1)
  })

  it('wrapTextCount：长文本多行', () => {
    const long = '这是一段比较长的中文内容用来测试折行逻辑是否能够正确计算出行数目的情况'.repeat(3)
    expect(wrapTextCount(long, 200, 15)).toBeGreaterThan(1)
  })

  it('wrapTextCount：空文本返回 1', () => {
    expect(wrapTextCount('', 200, 15)).toBe(1)
  })
})
