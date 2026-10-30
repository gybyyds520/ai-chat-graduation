/**
 * 角色聊天体验纯逻辑（与 UI 解耦，便于单测）。
 * 覆盖：开场白展示、打字机节奏、基于人设的快捷回复、删除占位。
 */

/**
 * 生成角色开场白消息对象（新角色首次对话时展示）。
 * @param {object} character 角色实体，需含 name/greeting
 * @returns {{senderType:string, content:string, createTime:string, isGreeting:boolean}|null}
 */
export function buildGreetingMessage(character) {
  if (!character) return null
  const greeting = (character.greeting || '').trim()
  if (!greeting) return null
  return {
    senderType: 'ai',
    content: greeting,
    createTime: new Date().toISOString(),
    isGreeting: true
  }
}

/**
 * 判断消息气泡是否为系统提示（用于区分气泡样式）。
 * @param {{senderType?:string, isGreeting?:boolean, isSystem?:boolean}} msg
 * @returns {boolean}
 */
export function isSystemBubble(msg) {
  return !!msg && (msg.senderType === 'system' || msg.isSystem === true)
}

/**
 * 判断消息是否为 AI 消息（含开场白）。
 * @param {{senderType?:string}} msg
 * @returns {boolean}
 */
export function isAiBubble(msg) {
  return !!msg && msg.senderType === 'ai'
}

/**
 * 根据人设关键词生成 3 条快捷回复建议。
 * 规则：命中 personality 关键词返回对应口吻，未命中返回默认建议。
 * @param {string} personality 人设/性格描述文本
 * @returns {string[]} 长度为 3 的建议数组
 */
export function generateQuickReplies(personality) {
  const text = (personality || '').toLowerCase()
  const has = (...keys) => keys.some((k) => text.includes(k))

  if (has('温柔', '治愈', '体贴')) {
    return ['你今天过得怎么样？', '能多和我说说你自己吗？', '有什么我可以帮你的吗？']
  }
  if (has('傲娇', '口是心非')) {
    return ['哼，才不是特意找你', '少得意忘形了', '你……今天还好吗？']
  }
  if (has('活泼', '元气', '开朗')) {
    return ['有什么好玩的事吗？', '快带我去看看！', '今天一起加油吧！']
  }
  if (has('高冷', '冷静', '酷')) {
    return ['嗯，然后呢', '说重点', '我在听']
  }
  if (has('腹黑', '狡黠', '坏心眼')) {
    return ['你觉得我会相信吗？', '这提议真有趣呢', '呵呵，有意思']
  }
  if (has('天然呆', '呆萌', '迷糊')) {
    return ['咦？是这样吗？', '我好像忘了什么……', '那个，我们接下来干嘛？']
  }
  if (has('幽默', '搞笑', '风趣')) {
    return ['讲个笑话听听？', '你最近遇到什么趣事？', '模仿个段子呗']
  }
  return ['你好呀~', '给我讲个故事吧', '我们今天聊点什么？']
}

/**
 * 删除消息后展示的占位文本。
 * @returns {string}
 */
export function deletedMessagePlaceholder() {
  return '消息已删除'
}

/**
 * 打字机节奏：根据已流式到达的全文本与经过时间，计算"应展示"的字符数。
 * 让 SSE 逐字到达在视觉上更自然——按固定速率吐字，而不是一次性渲染。
 * @param {string} fullText 已缓冲的完整回复文本
 * @param {number} elapsedMs 从开始吐字到现在经过的毫秒数
 * @param {number} charsPerSecond 每秒吐字数（默认 18，更自然）
 * @returns {number} 应展示的字符个数（不超过全文长度）
 */
export function typewriterVisibleCount(fullText, elapsedMs, charsPerSecond = 18) {
  const total = typeof fullText === 'string' ? fullText.length : 0
  if (total <= 0) return 0
  const cps = Number(charsPerSecond) > 0 ? Number(charsPerSecond) : 18
  const visible = Math.floor(((Number(elapsedMs) || 0) / 1000) * cps)
  return Math.max(0, Math.min(total, visible))
}

/**
 * 取开场白片段（广场卡片展示前 N 字）。
 * @param {string} greeting
 * @param {number} maxLen 默认 50
 * @returns {string}
 */
export function greetingSnippet(greeting, maxLen = 50) {
  const g = (greeting || '').trim()
  if (!g) return ''
  const n = Number(maxLen) > 0 ? Number(maxLen) : 50
  return g.length > n ? g.slice(0, n) + '…' : g
}
