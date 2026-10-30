import {
  parseMentionedCharacters,
  shouldTriggerAi,
  stripMentions
} from '../../src/utils/groupChatInput'

describe('群聊 @提及 纯逻辑', () => {
  const aiMembers = [
    { characterId: 1, characterName: '小助手' },
    { characterId: 2, characterName: 'Alice' }
  ]

  it('解析 @中文角色名', () => {
    expect(parseMentionedCharacters('@小助手 你好', aiMembers)).toEqual([1])
  })

  it('解析多个 @ 角色，保持顺序去重', () => {
    expect(parseMentionedCharacters('@Alice @小助手 大家说说', aiMembers)).toEqual([2, 1])
  })

  it('无前缀边界：@Alice 不应命中 @Alice2', () => {
    const members = [{ characterId: 2, characterName: 'Alice' }, { characterId: 9, characterName: 'Alice2' }]
    expect(parseMentionedCharacters('@Alice 你好', members)).toEqual([2])
  })

  it('无提及返回空数组', () => {
    expect(parseMentionedCharacters('大家好', aiMembers)).toEqual([])
  })

  it('空入参安全', () => {
    expect(parseMentionedCharacters('', aiMembers)).toEqual([])
    expect(parseMentionedCharacters('x', [])).toEqual([])
  })

  it('shouldTriggerAi：有 @ 即触发', () => {
    expect(shouldTriggerAi('@小助手 hi', [1])).toBe(true)
  })

  it('shouldTriggerAi：/ai 触发词也触发', () => {
    expect(shouldTriggerAi('/ai 聊点啥', [])).toBe(true)
  })

  it('shouldTriggerAi：普通消息不触发', () => {
    expect(shouldTriggerAi('大家好', [])).toBe(false)
  })

  it('stripMentions：去除 @ 前缀', () => {
    expect(stripMentions('@小助手 今天天气怎么样')).toBe('今天天气怎么样')
  })
})
