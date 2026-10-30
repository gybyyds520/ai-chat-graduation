import { describe, it, expect } from 'vitest'
import { ShareManager } from '../../src/utils/shareManager'

describe('gap: utils/shareManager 角色/聊天打包分享', () => {
  it('packCharacter 抛出非对象时抛错', () => {
    const sm = new ShareManager()
    expect(() => sm.packCharacter(null)).toThrow('Invalid character data')
    expect(() => sm.packCharacter('str')).toThrow('Invalid character data')
  })

  it('packCharacter 输出基础字段', () => {
    const sm = new ShareManager()
    const out = sm.packCharacter({ id: 'c1', name: 'Alice' })
    expect(out.type).toBe('character')
    expect(out.version).toBe('1.0.0')
    expect(out.character.id).toBe('c1')
    expect(out.character.name).toBe('Alice')
    expect(out.character.modelType).toBe('generic')
  })

  it('packCharacter includeTrainingData 时统计样本与 token', () => {
    const sm = new ShareManager()
    const out = sm.packCharacter({
      id: 'c2', name: 'Bob',
      trainingData: [
        { tokens: 100, topics: ['a'], timestamp: 1 },
        { tokens: 200, topics: ['b'], timestamp: 2 }
      ]
    }, true)
    expect(out.trainingDataSummary.sampleCount).toBe(2)
    expect(out.trainingDataSummary.totalTokens).toBe(300)
    expect(out.trainingDataSummary.topics).toContain('a')
  })

  it('packCharacter includeInteractionFeatures 时输出性格字段', () => {
    const sm = new ShareManager()
    const out = sm.packCharacter({
      id: 'c3', name: 'Cara',
      interactionFeatures: { responseStyle: 'sassy', avgResponseLength: 200 }
    }, false, true)
    expect(out.interactionFeatures.responseStyle).toBe('sassy')
    expect(out.interactionFeatures.avgResponseLength).toBe(200)
  })

  it('packChatHistory 非数组时抛错', () => {
    const sm = new ShareManager()
    expect(() => sm.packChatHistory(null)).toThrow('Invalid messages data')
  })

  it('packChatHistory 默认保留全部消息', () => {
    const sm = new ShareManager()
    const msgs = [{ content: 'hi', senderId: 1, senderName: 'A' }]
    const out = sm.packChatHistory(msgs)
    expect(out.messageCount).toBe(1)
    expect(out.messages[0].content).toBe('hi')
  })

  it('packChatHistory maxMessages 截断只留最后 N 条', () => {
    const sm = new ShareManager()
    const msgs = [1, 2, 3, 4, 5].map(i => ({ content: `m${i}` }))
    const out = sm.packChatHistory(msgs, { maxMessages: 2 })
    expect(out.messageCount).toBe(2)
    expect(out.messages[0].content).toBe('m4')
    expect(out.messages[1].content).toBe('m5')
  })

  it('packChatHistory anonymize=true 时剥离 senderId/senderName', () => {
    const sm = new ShareManager()
    const msgs = [{ content: 'secret', senderId: 9, senderName: 'X' }]
    const out = sm.packChatHistory(msgs, { anonymize: true })
    expect(out.messages[0].senderId).toBeUndefined()
    expect(out.messages[0].senderName).toBeUndefined()
  })

  it('compressData 返回非空 base64 字符串', () => {
    const sm = new ShareManager()
    const compressed = sm.compressData({ hello: 'world', nums: [1, 2, 3] })
    expect(typeof compressed).toBe('string')
    expect(compressed.length).toBeGreaterThan(0)
  })

  it('decompressData 对非压缩字符串抛错', () => {
    const sm = new ShareManager()
    expect(() => sm.decompressData('not-a-real-compressed-string')).toThrow()
  })

  it('encryptData 无密码时抛错', () => {
    const sm = new ShareManager()
    expect(() => sm.encryptData({ a: 1 }, '')).toThrow('Password is required')
  })

  it('encryptData/decryptData 同密码往返一致', () => {
    const sm = new ShareManager()
    const data = { secret: 'xyz' }
    const enc = sm.encryptData(data, 'pwd123')
    const dec = sm.decryptData(enc, 'pwd123')
    expect(dec).toEqual(data)
  })

  it('decryptData 错误密码得到乱码或抛错', () => {
    const sm = new ShareManager()
    const enc = sm.encryptData({ a: 'hello' }, 'right')
    expect(() => sm.decryptData(enc, 'wrong')).toThrow()
  })

  it('回归：大 payload（10 万+字符 JSON）compressData 不再抛 RangeError', () => {
    const sm = new ShareManager()
    // 构造 10 万元素数组，压缩后字节数远超 0x8000 单次 fromCharCode.apply 上限
    const big = {
      label: 'large-payload-regression',
      items: Array.from({ length: 100000 }, (_, i) => ({
        id: i,
        name: `item-${i}`,
        value: i * 1.5,
        tags: ['a', 'b', 'c']
      }))
    }
    expect(JSON.stringify(big).length).toBeGreaterThan(100000)
    // 修复前：String.fromCharCode(...new Uint8Array(bigBuf)) 会抛
    // "Maximum call stack size exceeded" / RangeError。修复后分段拼接应正常返回 base64。
    let compressed
    expect(() => { compressed = sm.compressData(big) }).not.toThrow()
    expect(typeof compressed).toBe('string')
    expect(compressed.length).toBeGreaterThan(0)
    // base64 字符集合法性
    expect(compressed).toMatch(/^[A-Za-z0-9+/]+={0,2}$/)
  })

  it('回归：decompressData 对合法小负载 base64 往返无损（pako v3 兼容已修复为 TextDecoder）', () => {
    const sm = new ShareManager()
    // 修复前：pako v3 inflate({to:'string'}) 被忽略返回 Uint8Array，JSON.parse 失败；
    // 已改为主代码 inflate 后用 TextDecoder 解码，现应无损往返。
    const original = { hello: 'world', nested: { a: [1, 2, 3] }, text: '中文内容测试' }
    const small = sm.compressData(original)
    const restored = sm.decompressData(small)
    expect(restored).toEqual(original)
  })

  it('回归：大 payload compress → decompress 往返无损', () => {
    const sm = new ShareManager()
    const big = {
      label: 'large-roundtrip',
      items: Array.from({ length: 5000 }, (_, i) => ({ id: i, name: `n${i}` }))
    }
    const compressed = sm.compressData(big)
    const restored = sm.decompressData(compressed)
    expect(restored).toEqual(big)
  })
})
