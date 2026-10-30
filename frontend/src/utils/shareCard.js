/**
 * 对话分享为图片卡片（M2 ⑧）。
 *
 * 不引入 html2canvas 等外部依赖：用原生 Canvas API 绘制对话气泡 + 头像 + 时间，
 * 底部标注「AI 角色名 · App 名」，导出 PNG 或写入剪贴板。
 *
 * 纯布局函数 computeCardLayout 与 DOM 解耦，便于单元测试。
 */

export const SHARE_CARD_WIDTH = 600
export const SHARE_CARD_PADDING = 24
export const SHARE_CARD_APP_NAME = 'AI 对话'

/**
 * 计算分享卡片布局（纯逻辑）。
 * @param {{role:'user'|'assistant', content:string, time?:string}[]} messages
 * @param {number} width 卡片宽度
 * @returns {{height:number, rows:number[], bubbleHeights:number[]}}
 */
export function computeCardLayout(messages, width = SHARE_CARD_WIDTH) {
  const w = width || SHARE_CARD_WIDTH
  const pad = SHARE_CARD_PADDING
  const headerH = 72
  const footerH = 56
  const gap = 18
  const bubbleMaxWidth = w - pad * 2 - 60 // 预留头像 44 + 间距
  const fontSize = 15
  const lineHeight = 22

  const bubbleHeights = (messages || []).map((m) => {
    const text = (m && m.content) || ''
    const lines = wrapTextCount(text, bubbleMaxWidth, fontSize)
    const timeH = 18
    return Math.max(56, lines * lineHeight + 16 + timeH)
  })

  const bodyHeight = bubbleHeights.reduce((sum, h) => sum + h, 0) + gap * Math.max(0, bubbleHeights.length - 1)
  const height = headerH + bodyHeight + footerH + pad
  return { width: w, height, headerH, footerH, bubbleHeights, bodyHeight }
}

/** 估算文本在给定宽度下折行后的行数（中文按字符宽度近似）。 */
export function wrapTextCount(text, maxWidth, fontSize = 15) {
  if (!text) return 1
  // 中文全角约 fontSize 宽，英文约 fontSize*0.55
  const chars = String(text).split('')
  let line = 1
  let lineWidth = 0
  const perLine = maxWidth / fontSize
  let visualWidth = 0
  for (const ch of chars) {
    const cw = /[\u4e00-\u9fa5]/.test(ch) ? 1 : 0.55
    visualWidth += cw
    if (visualWidth > perLine) {
      line++
      visualWidth = cw
    }
  }
  return Math.max(1, line)
}

/**
 * 在给定 canvas 上绘制分享卡片。
 * @param {HTMLCanvasElement} canvas
 * @param {{role:string, content:string, time?:string}[]} messages
 * @param {{characterName?:string, appName?:string}} meta
 */
export function drawShareCard(canvas, messages, meta = {}) {
  const ctx = canvas.getContext('2d')
  const layout = computeCardLayout(messages)
  const w = layout.width
  const h = layout.height
  canvas.width = w
  canvas.height = h

  // 背景
  ctx.fillStyle = '#f8fafc'
  roundRect(ctx, 0, 0, w, h, 16)
  ctx.fill()

  const pad = SHARE_CARD_PADDING
  let y = layout.headerH

  // 头部：角色名
  ctx.fillStyle = '#0f172a'
  ctx.font = 'bold 18px sans-serif'
  ctx.textBaseline = 'middle'
  ctx.fillText(meta.characterName || 'AI 对话', pad, 30)
  ctx.fillStyle = '#94a3b8'
  ctx.font = '12px sans-serif'
  ctx.fillText('与 ' + (meta.characterName || 'AI 角色') + ' 的对话', pad, 52)

  // 消息气泡
  ;(messages || []).forEach((m, i) => {
    const isUser = m.role === 'user'
    const bh = layout.bubbleHeights[i]
    const bubbleW = w - pad * 2 - 60
    const x = isUser ? w - pad - bubbleW : pad + 44

    // 头像圆点
    ctx.beginPath()
    ctx.arc(isUser ? pad + 22 : x + bubbleW + 22, y + bh / 2, 18, 0, Math.PI * 2)
    ctx.fillStyle = isUser ? '#4f46e5' : '#0ea5e9'
    ctx.fill()

    // 气泡
    ctx.fillStyle = isUser ? '#4f46e5' : '#ffffff'
    roundRect(ctx, x, y, bubbleW, bh - 18, 12)
    ctx.fill()

    // 文本
    ctx.fillStyle = isUser ? '#ffffff' : '#1e293b'
    ctx.font = '15px sans-serif'
    ctx.textBaseline = 'top'
    drawWrappedText(ctx, m.content || '', x + 14, y + 12, bubbleW - 28, 22)

    // 时间
    ctx.fillStyle = '#94a3b8'
    ctx.font = '11px sans-serif'
    ctx.textBaseline = 'alphabetic'
    ctx.fillText(m.time || '', x, y + bh - 4)

    y += bh + 18
  })

  // 底部
  ctx.fillStyle = '#64748b'
  ctx.font = '12px sans-serif'
  ctx.textBaseline = 'middle'
  ctx.fillText((meta.characterName || 'AI 角色') + ' · ' + (meta.appName || SHARE_CARD_APP_NAME), pad, h - 28)

  return canvas
}

/** 导出为 PNG dataURL。 */
export function exportCardPng(canvas) {
  return canvas.toDataURL('image/png')
}

function roundRect(ctx, x, y, w, h, r) {
  const radius = Math.min(r, w / 2, h / 2)
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.arcTo(x + w, y, x + w, y + h, radius)
  ctx.arcTo(x + w, y + h, x, y + h, radius)
  ctx.arcTo(x, y + h, x, y, radius)
  ctx.arcTo(x, y, x + w, y, radius)
  ctx.closePath()
}

function drawWrappedText(ctx, text, x, y, maxWidth, lineHeight) {
  if (!text) return
  const chars = String(text).split('')
  let line = ''
  let cy = y
  for (const ch of chars) {
    const test = line + ch
    if (ctx.measureText(test).width > maxWidth && line) {
      ctx.fillText(line, x, cy)
      line = ch
      cy += lineHeight
    } else {
      line = test
    }
  }
  if (line) ctx.fillText(line, x, cy)
}
