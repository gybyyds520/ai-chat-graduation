import {
  parseSSEBuffer,
  getOfflineReply,
  buildChatStreamUrl,
  ELF_OFFLINE_REPLIES
} from '../../src/utils/elfChatSse'

describe('elfChatSse 对话 SSE 解析与离线降级', () => {
  describe('parseSSEBuffer SSE 帧解析', () => {
    it('解析 message 命名事件 token', () => {
      const raw = 'event: message\ndata: 你好\n\n'
      const { events, remaining } = parseSSEBuffer(raw)
      expect(events).toHaveLength(1)
      expect(events[0].type).toBe('message')
      expect(events[0].data).toBe('你好')
      expect(remaining).toBe('')
    })

    it('多个事件逐个解析', () => {
      const raw = 'event: message\ndata: 你\n\nevent: message\ndata: 好\n\n'
      const { events } = parseSSEBuffer(raw)
      expect(events).toHaveLength(2)
      expect(events[0].data).toBe('你')
      expect(events[1].data).toBe('好')
    })

    it('error 事件类型正确识别', () => {
      const raw = 'event: error\ndata: 后端不可用\n\n'
      const { events } = parseSSEBuffer(raw)
      expect(events).toHaveLength(1)
      expect(events[0].type).toBe('error')
      expect(events[0].data).toBe('后端不可用')
    })

    it('不完整帧留在 remaining，下次拼接解析', () => {
      const first = 'event: message\ndata: 你好\n\nevent: message\ndata: 世'
      const r1 = parseSSEBuffer(first)
      expect(r1.events).toHaveLength(1)
      expect(r1.events[0].data).toBe('你好')
      // 第二个帧未结束 → 留在 remaining
      expect(r1.remaining).toContain('世')

      const r2 = parseSSEBuffer(r1.remaining + '界\n\n')
      expect(r2.events).toHaveLength(1)
      expect(r2.events[0].data).toBe('世界')
      expect(r2.remaining).toBe('')
    })

    it('多行 data 用换行拼接', () => {
      const raw = 'event: message\ndata: 第一行\ndata: 第二行\n\n'
      const { events } = parseSSEBuffer(raw)
      expect(events[0].data).toBe('第一行\n第二行')
    })

    it('CRLF 换行也能解析', () => {
      const raw = 'event: message\r\ndata: hi\r\n\r\n'
      const { events } = parseSSEBuffer(raw)
      expect(events).toHaveLength(1)
      expect(events[0].data).toBe('hi')
    })

    it('空输入返回空事件', () => {
      expect(parseSSEBuffer('')).toEqual({ events: [], remaining: '' })
      expect(parseSSEBuffer(undefined)).toEqual({ events: [], remaining: '' })
    })

    it('忽略 id/retry/注释行', () => {
      const raw = 'id: 42\nretry: 3000\n: keep-alive\nevent: message\ndata: ok\n\n'
      const { events } = parseSSEBuffer(raw)
      expect(events).toHaveLength(1)
      expect(events[0].data).toBe('ok')
    })
  })

  describe('getOfflineReply 离线回复', () => {
    it('返回值必在预置台词池内', () => {
      for (let i = 0; i < 30; i++) {
        expect(ELF_OFFLINE_REPLIES).toContain(getOfflineReply('你好'))
      }
    })
    it('台词池不少于 12 条', () => {
      expect(ELF_OFFLINE_REPLIES.length).toBeGreaterThanOrEqual(12)
    })
    it('注入 rand=0 取首条', () => {
      expect(getOfflineReply('x', () => 0)).toBe(ELF_OFFLINE_REPLIES[0])
    })
  })

  describe('buildChatStreamUrl 流式 URL', () => {
    it('包含全部查询参数', () => {
      const url = buildChatStreamUrl({
        sessionId: 's1',
        userId: '7',
        characterId: '3',
        message: '你好'
      })
      expect(url).toContain('/api/chat/stream?')
      expect(url).toContain('sessionId=s1')
      expect(url).toContain('userId=7')
      expect(url).toContain('characterId=3')
      expect(url).toContain('message=')
    })
    it('缺省参数用兜底值', () => {
      const url = buildChatStreamUrl({})
      expect(url).toContain('sessionId=elf-offline')
      expect(url).toContain('userId=0')
    })
    it('携带 token 时追加到 query（SSE 认证约定）', () => {
      const url = buildChatStreamUrl({ userId: '7', characterId: '3', message: 'hi', token: 'abc.def' })
      expect(url).toContain('token=abc.def')
    })
    it('无 token 时不带 token 参数', () => {
      const url = buildChatStreamUrl({ userId: '7', characterId: '3', message: 'hi' })
      expect(url).not.toContain('token=')
    })
  })
})
