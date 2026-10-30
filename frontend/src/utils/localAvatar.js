/**
 * 本地确定性头像生成器（离线可用，不依赖任何外部 API）。
 *
 * 背景：创建 AI 角色时若用户未上传图片，avatar 字段为空，界面此前显示
 * 与角色无关的字母色块（按角色 id 生成），观感上"随机生成"。
 * 现在按 seed（通常为角色名）+ style 确定性生成本地 SVG data URI：
 *   - 同一角色任何时候生成结果完全一致（确定性）；
 *   - 6 种预设风格真实生效且互不相同（avataaars/bottts/bottts-neutral/jdenticon/gridy/micah）；
 *   - 全程本地生成，不依赖 DiceBear 等外部网络。
 */

const hashCode = (str) => {
  let hash = 0
  if (!str) return 0
  for (let i = 0; i < str.length; i++) {
    hash = ((hash << 5) - hash) + str.charCodeAt(i)
    hash |= 0
  }
  return hash
}

const pick = (seed, salt, arr) => {
  const h = hashCode(seed + ':' + salt)
  return arr[Math.abs(h) % arr.length]
}

const getLetter = (seed) => {
  if (!seed) return '?'
  const cleaned = String(seed).replace(/[^a-zA-Z\u4e00-\u9fa5]/g, '')
  if (cleaned.length === 0) return '?'
  const lastChar = cleaned[cleaned.length - 1]
  if (/[\u4e00-\u9fa5]/.test(lastChar)) {
    return lastChar.substring(0, 1)
  }
  return lastChar.toUpperCase()
}

const SKIN = ['#F8D5C0', '#EAB89A', '#D9A882', '#C68642', '#8D5524', '#FFE0BD']
const HAIR = ['#2C222B', '#4A3728', '#6B4226', '#A05C2C', '#C98A3D', '#E8C396', '#1B1B1B', '#7A4A2B']
const BODY = ['#667EEA', '#764BA2', '#4ECDC4', '#45B7D1', '#FF6B6B', '#F7DC6F', '#96CEB4', '#BB8FCE', '#F39C12', '#5DADE2']
const BOT_COLORS = ['#667EEA', '#764BA2', '#4ECDC4', '#45B7D1', '#FF6B6B', '#F7DC6F', '#96CEB4', '#BB8FCE', '#F39C12', '#5DADE2', '#2ECC71', '#E74C3C']

const letter = (seed) => getLetter(seed)

const svgEncode = (svg) => `data:image/svg+xml;base64,${btoa(unescape(encodeURIComponent(svg)))}`

/* ============ 风格实现 ============ */

// 人像：圆脸 + 发型 + 表情
function styleAvataaars(seed) {
  const skin = pick(seed, 'skin', SKIN)
  const hair = pick(seed, 'hair', HAIR)
  const shirt = pick(seed, 'shirt', BODY)
  const hairStyle = pick(seed, 'hairStyle', ['round', 'spiky', 'bob', 'bald'])
  const size = 200
  let hairSvg = ''
  if (hairStyle === 'spiky') {
    hairSvg = `<polygon points="100,30 85,85 115,85" fill="${hair}"/><polygon points="70,45 78,95 100,85" fill="${hair}"/><polygon points="130,45 122,95 100,85" fill="${hair}"/>`
  } else if (hairStyle === 'bob') {
    hairSvg = `<path d="M100,28 a55,52 0 0 1 55,55 l0,12 a95,25 0 0 0 -110,0 l0,-12 a55,52 0 0 1 55,-55 z" fill="${hair}"/>`
  } else if (hairStyle === 'bald') {
    hairSvg = `<circle cx="100" cy="52" r="34" fill="${skin}" stroke="${hair}" stroke-width="6"/>`
  } else {
    hairSvg = `<path d="M100,26 a56,50 0 0 1 56,50 l0,6 a56,60 0 0 0 -112,0 l0,-6 a56,50 0 0 1 56,-50 z" fill="${hair}"/>`
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <circle cx="100" cy="100" r="100" fill="#F5F5F7"/>
  <path d="M30,200 Q30,150 60,140 L140,140 Q170,150 170,200 Z" fill="${shirt}"/>
  <circle cx="100" cy="100" r="56" fill="${skin}"/>
  ${hairSvg}
  <circle cx="82" cy="98" r="4" fill="#333"/>
  <circle cx="118" cy="98" r="4" fill="#333"/>
  <path d="M88,120 Q100,132 112,120" stroke="#C96A5B" stroke-width="4" fill="none" stroke-linecap="round"/>
  <circle cx="82" cy="98" r="8" fill="none" stroke="#333" stroke-width="1.5" opacity="0.25"/>
  <circle cx="118" cy="98" r="8" fill="none" stroke="#333" stroke-width="1.5" opacity="0.25"/>
</svg>`
}

// 机器人：方头 + 天线 + 眼睛
function styleBottts(seed) {
  const main = pick(seed, 'main', BOT_COLORS)
  const accent = pick(seed, 'accent', BOT_COLORS)
  const size = 200
  const eyeY = 96
  const mouth = pick(seed, 'mouth', ['grin', 'flat', 'open'])
  let mouthSvg = ''
  if (mouth === 'grin') {
    mouthSvg = `<path d="M82,130 Q100,146 118,130" stroke="${accent}" stroke-width="5" fill="none" stroke-linecap="round"/>`
  } else if (mouth === 'open') {
    mouthSvg = `<rect x="88" y="124" width="24" height="14" rx="7" fill="${accent}"/>`
  } else {
    mouthSvg = `<line x1="86" y1="132" x2="114" y2="132" stroke="${accent}" stroke-width="5" stroke-linecap="round"/>`
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <circle cx="100" cy="100" r="100" fill="#F5F5F7"/>
  <line x1="100" y1="20" x2="100" y2="52" stroke="${accent}" stroke-width="6" stroke-linecap="round"/>
  <circle cx="100" cy="20" r="8" fill="${accent}"/>
  <rect x="52" y="52" width="96" height="104" rx="20" fill="${main}"/>
  <rect x="52" y="52" width="96" height="104" rx="20" fill="none" stroke="${accent}" stroke-width="4"/>
  <circle cx="80" cy="${eyeY}" r="9" fill="#fff"/>
  <circle cx="120" cy="${eyeY}" r="9" fill="#fff"/>
  <circle cx="80" cy="${eyeY}" r="4" fill="#222"/>
  <circle cx="120" cy="${eyeY}" r="4" fill="#222"/>
  ${mouthSvg}
  <line x1="52" y1="140" x2="36" y2="156" stroke="${accent}" stroke-width="6" stroke-linecap="round"/>
  <line x1="148" y1="140" x2="164" y2="156" stroke="${accent}" stroke-width="6" stroke-linecap="round"/>
</svg>`
}

// 中性机器人：圆头 + 极简
function styleBotttsNeutral(seed) {
  const main = pick(seed, 'main', BOT_COLORS)
  const size = 200
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <circle cx="100" cy="100" r="100" fill="#F5F5F7"/>
  <circle cx="100" cy="88" r="52" fill="${main}"/>
  <circle cx="100" cy="88" r="52" fill="none" stroke="#222" stroke-width="4" opacity="0.15"/>
  <line x1="76" y1="86" x2="92" y2="86" stroke="#fff" stroke-width="5" stroke-linecap="round"/>
  <line x1="108" y1="86" x2="124" y2="86" stroke="#fff" stroke-width="5" stroke-linecap="round"/>
  <line x1="88" y1="112" x2="112" y2="112" stroke="#fff" stroke-width="5" stroke-linecap="round"/>
  <line x1="100" y1="28" x2="100" y2="42" stroke="#222" stroke-width="4" opacity="0.2" stroke-linecap="round"/>
  <circle cx="100" cy="24" r="5" fill="#222" opacity="0.2"/>
</svg>`
}

// 几何对称图案
function styleJdenticon(seed) {
  const c1 = pick(seed, 'c1', BOT_COLORS)
  const c2 = pick(seed, 'c2', BOT_COLORS)
  const bg = pick(seed, 'bg', ['#F5F5F7', '#EEF1F6', '#F0EDF5'])
  const size = 200
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <rect width="200" height="200" rx="100" fill="${bg}"/>
  <rect x="66" y="66" width="68" height="68" rx="10" fill="${c1}"/>
  <circle cx="100" cy="100" r="24" fill="${c2}"/>
  <rect x="40" y="40" width="30" height="30" rx="6" fill="${c2}" opacity="0.85"/>
  <rect x="130" y="40" width="30" height="30" rx="6" fill="${c2}" opacity="0.85"/>
  <rect x="40" y="130" width="30" height="30" rx="6" fill="${c2}" opacity="0.85"/>
  <rect x="130" y="130" width="30" height="30" rx="6" fill="${c2}" opacity="0.85"/>
</svg>`
}

// 网格点阵
function styleGridy(seed) {
  const c1 = pick(seed, 'c1', BOT_COLORS)
  const c2 = pick(seed, 'c2', BOT_COLORS)
  const size = 200
  let cells = ''
  const on = []
  const h = hashCode(seed + ':grid')
  for (let i = 0; i < 5; i++) {
    for (let j = 0; j < 5; j++) {
      const k = i * 5 + j
      const bit = ((h >> (k % 24)) & 1)
      if (bit === 1) on.push([i, j])
    }
  }
  for (const [i, j] of on) {
    cells += `<rect x="${40 + j * 30}" y="${40 + i * 30}" width="24" height="24" rx="7" fill="${(i + j) % 2 === 0 ? c1 : c2}"/>`
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <rect width="200" height="200" rx="100" fill="#F5F5F7"/>
  ${cells}
</svg>`
}

// 半身像：头 + 头发 + 肩膀
function styleMicah(seed) {
  const skin = pick(seed, 'skin', SKIN)
  const hair = pick(seed, 'hair', HAIR)
  const shirt = pick(seed, 'shirt', BODY)
  const size = 200
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <circle cx="100" cy="100" r="100" fill="#F5F5F7"/>
  <path d="M24,200 Q28,150 62,138 L138,138 Q172,150 176,200 Z" fill="${shirt}"/>
  <path d="M28,200 Q30,162 58,146 L66,170 Q38,178 36,200 Z" fill="#fff" opacity="0.35"/>
  <circle cx="100" cy="96" r="48" fill="${skin}"/>
  <path d="M100,40 a52,46 0 0 1 52,50 l0,8 a52,56 0 0 0 -104,0 l0,-8 a52,46 0 0 1 52,-50 z" fill="${hair}"/>
  <circle cx="85" cy="94" r="3.5" fill="#333"/>
  <circle cx="115" cy="94" r="3.5" fill="#333"/>
  <path d="M90,116 Q100,124 110,116" stroke="#C96A5B" stroke-width="3.5" fill="none" stroke-linecap="round"/>
  <path d="M52,140 Q64,130 78,136" stroke="#fff" stroke-width="4" fill="none" stroke-linecap="round" opacity="0.6"/>
</svg>`
}

// 兜底：渐变圆 + 首字母（保留原视觉）
function styleFallback(seed) {
  const pair = pick(seed, 'color', BODY)
  const ch = letter(seed)
  const size = 200
  const h = hashCode(seed)
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
  <defs>
    <linearGradient id="grad-${h}" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:${pair};stop-opacity:1" />
      <stop offset="100%" style="stop-color:${pair};stop-opacity:0.75" />
    </linearGradient>
  </defs>
  <circle cx="100" cy="100" r="100" fill="url(#grad-${h})"/>
  <text x="100" y="100" dominant-baseline="central" text-anchor="middle" fill="white" font-size="80" font-family="Arial, sans-serif" font-weight="bold">${ch}</text>
</svg>`
}

const STYLE_FACTORIES = {
  avataaars: styleAvataaars,
  bottts: styleBottts,
  'bottts-neutral': styleBotttsNeutral,
  jdenticon: styleJdenticon,
  gridy: styleGridy,
  micah: styleMicah
}

export const VALID_STYLES = Object.keys(STYLE_FACTORIES)

/**
 * 生成确定性本地头像 SVG data URI。
 * @param {string} seed 头像种子（角色名/用户标识），决定颜色与形状
 * @param {string} [style] 风格，默认 avataaars；未知风格回退为渐变+首字母
 * @returns {string} data:image/svg+xml;base64,...
 */
export const generateLocalAvatarSvg = (seed = 'default', style = 'avataaars') => {
  const factory = STYLE_FACTORIES[style] || styleFallback
  return svgEncode(factory(String(seed ?? 'default')))
}

/**
 * 兼容入口：生成可作 <img src> 的本地头像 URL。
 */
export const generateAvatarUrl = (seed, style = 'avataaars') => {
  return generateLocalAvatarSvg(seed, style)
}
