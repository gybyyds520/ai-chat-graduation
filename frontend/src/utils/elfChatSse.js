/**
 * elfChatSse.js — 桌宠对话 SSE 解析与离线降级纯函数
 *
 * 职责：
 *  1. 把 fetch ReadableStream 累计到的 SSE 文本切块解析为事件数组
 *     （后端 ChatController 使用命名事件：message=token / error / image）
 *  2. 提供离线模式预置台词与随机回复（无后端 / Ollama 不可用时降级）
 *
 * 纯函数模块，不依赖 DOM，可在 jsdom/node 下独立测试。
 */

/**
 * 离线模式预置回复（≥12 条，健康向）
 */
export const ELF_OFFLINE_REPLIES = [
  '（离线模式）我在哦，你说的话我都记在心里啦~',
  '（离线模式）现在脑子有点卡壳，不过陪你聊天我还是很开心的！',
  '（离线模式）你今天过得怎么样呀？',
  '（离线模式）休息一下，喝口水再继续吧~',
  '（离线模式）我一直在这里陪着你哦。',
  '（离线模式）嘻嘻，虽然我现在没办法联网，但听你说话就很开心啦。',
  '（离线模式）要不要活动一下肩颈？久坐对身体不好哦。',
  '（离线模式）你最近有什么开心的事想分享吗？',
  '（离线模式）我在认真听呢，慢慢说~',
  '（离线模式）别太累着自己呀，我会心疼的。',
  '（离线模式）等我连上大脑，就能陪你聊更多啦！',
  '（离线模式）摸摸头，一切都会顺利的~'
]

/**
 * 解析一段 SSE 文本缓冲区，返回完整事件与未消费的残留。
 *
 * SSE 帧以空行（\n\n 或 \r\n\r\n）分隔；每帧可含 event: 与 data: 行。
 * data 可能跨行，多行 data 用 \n 拼接。
 *
 * @param {string} buffer - 累计到的 SSE 文本
 * @returns {{ events: Array<{ type: string, data: string }>, remaining: string }}
 *   type: 'message' | 'error' | 'image' | '其他命名事件'
 */
export function parseSSEBuffer(buffer) {
  const result = { events: [], remaining: '' }
  if (typeof buffer !== 'string' || buffer.length === 0) {
    return result
  }

  // 统一换行为 \n
  const normalized = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n')

  // 以空行切分帧
  const frames = normalized.split(/\n\n+/)
  // 最后一段可能是不完整帧 → 作为 remaining
  result.remaining = frames.pop() || ''

  for (const frame of frames) {
    if (!frame.trim()) continue
    let eventType = 'message' // SSE 默认事件名
    const dataLines = []
    for (const line of frame.split('\n')) {
      if (line.startsWith('event:')) {
        eventType = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).replace(/^ /, ''))
      }
      // 忽略 id: / retry: / 注释行(:)
    }
    if (dataLines.length > 0) {
      result.events.push({ type: eventType, data: dataLines.join('\n') })
    }
  }

  return result
}

/**
 * 从离线台词池随机选一条回复。
 * @param {string} [userInput] 用户输入（保留参数，便于将来做关键词匹配）
 * @param {() => number} [rand=Math.random]
 * @returns {string}
 */
export function getOfflineReply(userInput, rand = Math.random) {
  const pool = ELF_OFFLINE_REPLIES
  return pool[Math.floor(rand() * pool.length)]
}

/**
 * 构造对话流请求 URL（纯函数，便于测试与降级）。
 * @param {object} params
 * @param {string|number} params.sessionId
 * @param {string|number} params.userId
 * @param {string|number} params.characterId
 * @param {string} params.message
 * @param {string} [params.token] 登录令牌（SSE 不支持自定义 header，按后端约定走 query）
 * @returns {string}
 */
export function buildChatStreamUrl(params) {
  const p = params || {}
  const q = new URLSearchParams()
  q.set('sessionId', String(p.sessionId || 'elf-offline'))
  q.set('userId', String(p.userId || '0'))
  q.set('characterId', String(p.characterId || '0'))
  q.set('message', String(p.message || ''))
  if (p.token) {
    q.set('token', String(p.token))
  }
  return `/api/chat/stream?${q.toString()}`
}
