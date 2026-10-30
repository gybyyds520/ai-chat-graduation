/**
 * animeElfUtils.js — 二次元桌宠纯函数工具
 *
 * 职责：
 *  1. 把精灵动画状态机状态（idle/happy/sad/surprised/thinking/sleep 等）
 *     映射为二次元立绘的表情参数（眼睛/嘴巴/眉毛形状）
 *  2. 计算眨眼 transform（由父级 blink 相位驱动，也可用于测试）
 *  3. 二次元角色 → 兼容 elfData 结构的转换（供 3D 渲染器/皮肤系统兜底）
 *
 * 纯函数模块，无 DOM 依赖，可独立测试。
 */
import { animeElfCharacters } from '@/data/animeElfData'

/**
 * 表情映射表：状态 → { eyes, mouth, brow }
 * eyes: open | happy | sad | wide | sleepy | closed
 * mouth: smile | open | sad | o | flat | sleep | pout
 * brow: normal | raise | furrow
 */
export const ANIME_EXPRESSION_MAP = {
  idle: { eyes: 'open', mouth: 'smile', brow: 'normal' },
  stand: { eyes: 'open', mouth: 'smile', brow: 'normal' },
  happy: { eyes: 'happy', mouth: 'open', brow: 'raise' },
  sad: { eyes: 'sad', mouth: 'pout', brow: 'furrow' },
  surprised: { eyes: 'wide', mouth: 'o', brow: 'raise' },
  thinking: { eyes: 'sleepy', mouth: 'flat', brow: 'furrow' },
  sleep: { eyes: 'closed', mouth: 'sleep', brow: 'normal' },
  jump: { eyes: 'wide', mouth: 'open', brow: 'raise' },
  dance: { eyes: 'happy', mouth: 'open', brow: 'raise' },
  wave: { eyes: 'happy', mouth: 'smile', brow: 'raise' },
  crouch: { eyes: 'open', mouth: 'smile', brow: 'normal' },
  lie: { eyes: 'closed', mouth: 'flat', brow: 'normal' }
}

/**
 * 获取状态对应的表情参数（未知状态回退 idle）
 * @param {string} state
 * @returns {{ eyes: string, mouth: string, brow: string }}
 */
export function getAnimeExpression(state) {
  return ANIME_EXPRESSION_MAP[state] || ANIME_EXPRESSION_MAP.idle
}

/**
 * 眨眼 transform：
 *  - sleep / lie / eyes=closed 时眼睛闭合（scaleY 0.08）
 *  - 其余状态按 blinkPhase(0~1) 计算眨眼压缩，0 或 1 表示完全睁开
 * @param {number} blinkPhase 0~1
 * @param {string} state
 * @returns {string} transform 值
 */
export function getBlinkTransform(blinkPhase, state) {
  const expr = getAnimeExpression(state)
  if (expr.eyes === 'closed') {
    return 'scaleY(0.08)'
  }
  if (expr.eyes === 'happy' || expr.eyes === 'sad' || expr.eyes === 'wide') {
    return 'scaleY(1)'
  }
  const p = Math.max(0, Math.min(1, blinkPhase || 0))
  if (p <= 0 || p >= 1) {
    return 'scaleY(1)'
  }
  const ease = p < 0.5 ? p * 2 : 1 - (p - 0.5) * 2
  return `scaleY(${1 - ease * 0.88})`
}

/**
 * 判断某个表情是否是"闭合眼"（用于是否禁用眨眼动画）
 * @param {string} state
 * @returns {boolean}
 */
export function isEyesClosed(state) {
  const expr = getAnimeExpression(state)
  return expr.eyes === 'closed'
}

/**
 * 二次元角色 → elfData 兼容结构（供 3D 渲染器 / 皮肤系统兜底）。
 * 二次元角色强制 2D 渲染时本函数通常用不到，但保留以兼容设计器数据流。
 * @param {Object} char
 * @returns {Object}
 */
export function toElfCompatibleCharacter(char) {
  return {
    id: char.id,
    name: char.name,
    category: 'anime',
    description: char.description || '',
    personality: char.personality || '',
    emoji: char.emoji || '🌸',
    baseColor: char.baseColor || (char.palette && char.palette.eyes) || '#ff6f9c',
    shape: 'anime-girl',
    scale: char.scale || { body: 0.85, head: 0.4 },
    specialEffect: char.specialEffect || null
  }
}

/**
 * 便捷：返回全部二次元角色（防御性拷贝，避免调用方污染共享数据）
 * @returns {Object[]}
 */
export function getAllAnimeCharacters() {
  return animeElfCharacters.map((c) => ({ ...c, palette: { ...c.palette } }))
}

/* ==========================================================================
 * 桌宠化：随机互动 / 待机动作 / 口型 纯函数
 * 全部支持注入 rand 随机源（默认 Math.random），便于确定性测试。
 * ======================================================================== */

/** 点击互动台词库（≥10 条） */
export const ANIME_INTERACTION_LINES = [
  '嘿嘿，戳我干嘛~',
  '今天也要元气满满哦！',
  '陪我玩一会儿嘛？',
  '你回来啦！',
  '在想什么呢？',
  '工作辛苦了，休息一下吧~',
  '我一直在这里陪着你哦',
  '要不要跳支舞？',
  '嘻嘻，被你发现啦',
  '今天天气真好呢',
  '摸摸头~',
  '别累坏了自己呀'
]

/** 休息期悠闲台词库（长休息时偶尔冒一句，赋予"活人感"） */
export const ANIME_REST_LINES = [
  '今天的云好漂亮呀~',
  '就这样发发呆，也不错呢~',
  '主人也在忙吗？我一直陪着你哦',
  '呼——好放松~',
  '数着窗外的光，慢慢等时间走~',
  '嗯…接下来做点什么好呢？',
  '风轻轻的，好舒服~',
  '嘘…我在想事情呢~'
]

/** 待机随机动作白名单 */
export const ANIME_IDLE_RANDOM_ACTIONS = [
  'yawn',        // 打哈欠
  'stretch',     // 伸懒腰
  'look-around',// 转头
  'blink',       // 眨眼
  'sway',        // 摆身体
  'nod',         // 点头
  'shake-head'   // 摇头
]

/** 点击可触发的表情 / 动作 / 特效池 */
export const ANIME_CLICK_EXPRESSIONS = ['happy', 'surprised', 'thinking']
export const ANIME_CLICK_ACTIONS = [
  'wave', 'dance', 'jump', 'bow', 'clap', 'cheer', 'heart',
  'kiss', 'salute', 'spin', 'victory', 'nod', 'shake-head',
  'scared', 'angry', 'cry'
]
export const ANIME_CLICK_EFFECTS = ['heart', 'star', 'note']

/**
 * 游戏角色经典动作菜单（快捷菜单"动作表演"面板）。
 * 每项含 id / emoji / label / 时长（ms）。
 * 时长用于 3D 渲染器 playAction(action, duration) 的动作推进。
 */
export const ANIME_ACT_MENU = [
  { id: 'wave', emoji: '👋', label: '挥手', duration: 1800 },
  { id: 'bow', emoji: '🙇', label: '鞠躬', duration: 1400 },
  { id: 'salute', emoji: '🫡', label: '敬礼', duration: 1500 },
  { id: 'nod', emoji: '👍', label: '点头', duration: 1200 },
  { id: 'shake-head', emoji: '🙅', label: '摇头', duration: 1400 },
  { id: 'clap', emoji: '👏', label: '鼓掌', duration: 1800 },
  { id: 'cheer', emoji: '🎉', label: '欢呼', duration: 2000 },
  { id: 'heart', emoji: '💗', label: '比心', duration: 1800 },
  { id: 'kiss', emoji: '💋', label: '飞吻', duration: 2200 },
  { id: 'spin', emoji: '🌀', label: '转圈', duration: 1600 },
  { id: 'victory', emoji: '✌️', label: '胜利', duration: 1800 },
  { id: 'dance', emoji: '💃', label: '跳舞', duration: 2600 },
  { id: 'jump', emoji: '⤴️', label: '跳跃', duration: 900 },
  { id: 'stretch', emoji: '🥱', label: '伸懒腰', duration: 2000 },
  { id: 'scared', emoji: '😱', label: '害怕', duration: 1500 },
  { id: 'angry', emoji: '😠', label: '生气', duration: 1600 },
  { id: 'cry', emoji: '😢', label: '伤心', duration: 2000 },
  { id: 'happy', emoji: '😊', label: '开心', duration: 1600 }
]

/** 随机动作池（快捷菜单"随机动作"），覆盖游戏经典动作 */
export const ANIME_RANDOM_ACTIONS = [
  'dance', 'jump', 'wave', 'happy', 'bow', 'clap', 'cheer', 'heart',
  'kiss', 'salute', 'spin', 'victory', 'nod', 'shake-head', 'stretch'
]

/**
 * 从列表中随机取一项（纯函数，可注入随机源）
 * @param {Array} list
 * @param {() => number} rand
 * @returns {*}
 */
export function pickRandom(list, rand = Math.random) {
  if (!Array.isArray(list) || list.length === 0) return null
  return list[Math.floor(rand() * list.length)]
}

/**
 * 随机眨眼间隔（毫秒）。默认 2~6 秒。
 * @param {number} min
 * @param {number} max
 * @param {() => number} rand
 * @returns {number}
 */
export function getBlinkInterval(min = 2000, max = 6000, rand = Math.random) {
  const lo = Math.min(min, max)
  const hi = Math.max(min, max)
  if (hi <= lo) return lo
  return Math.floor(lo + rand() * (hi - lo))
}

/**
 * 随机待机动作（结果必在白名单 ANIME_IDLE_RANDOM_ACTIONS 内）
 * @param {() => number} rand
 * @returns {string}
 */
export function getIdleRandomAction(rand = Math.random) {
  return pickRandom(ANIME_IDLE_RANDOM_ACTIONS, rand) || 'blink'
}

/**
 * 随机互动组合：台词 + 表情 + 动作 + 特效（多重随机，非固定单一）
 * @param {() => number} rand
 * @returns {{ line: string, expression: string, action: string, effect: string }}
 */
export function getRandomInteraction(rand = Math.random) {
  return {
    line: pickRandom(ANIME_INTERACTION_LINES, rand),
    expression: pickRandom(ANIME_CLICK_EXPRESSIONS, rand),
    action: pickRandom(ANIME_CLICK_ACTIONS, rand),
    effect: pickRandom(ANIME_CLICK_EFFECTS, rand)
  }
}

/**
 * 根据情绪表情关键词返回嘴型 shape 名
 * @param {string} expression normal|happy|sad|surprised|thinking|sleep
 * @returns {string} smile|open|pout|o|flat|sleep
 */
export function getExpressionMouth(expression) {
  const map = {
    normal: 'smile',
    happy: 'open',
    sad: 'pout',
    surprised: 'o',
    thinking: 'flat',
    sleep: 'sleep'
  }
  return map[expression] || 'smile'
}

/* ==========================================================================
 * 角色化台词：优先取角色专属台词池，缺省回退通用池
 * ======================================================================== */

/**
 * 取角色的个性台词（角色有 lines 池则优先，否则回退通用池）。
 * 纯函数，可注入随机源，便于确定性测试。
 * @param {Object|null} character 二次元角色对象
 * @param {string[]} fallbackLines 通用台词池（如 ANIME_INTERACTION_LINES）
 * @param {() => number} rand 随机源，默认 Math.random
 * @returns {string} 台词文本
 */
export function getCharacterLine(character, fallbackLines = ANIME_INTERACTION_LINES, rand = Math.random) {
  const pool = (character && Array.isArray(character.lines) && character.lines.length > 0)
    ? character.lines
    : fallbackLines
  return pickRandom(pool, rand) || ''
}

/**
 * 根据养成状态生成"需求提示"（饿了/困了/想被摸）。
 * 仅在对应需求标志为真时输出；无需求返回 null。
 * @param {{ hungry?: boolean, lethargic?: boolean, affectionate?: boolean }} growth 养成状态
 * @param {() => number} rand 随机源，默认 Math.random
 * @returns {string|null} 提示台词或 null
 */
export function getNeedHintLine(growth = {}, rand = Math.random) {
  if (!growth || typeof growth !== 'object') return null
  const HUNGRY_LINES = ['主人，我肚子饿啦～想吃好吃的', '咕噜噜…有点饿了', '有什么好吃的吗？']
  const LETHARGIC_LINES = ['好困呀…想睡一会儿', '眼睛有点睁不开了…', '让我打个盹嘛~']
  const AFFECTIONATE_LINES = ['主人，摸摸头好不好～', '想被你抱抱', '今天也最喜欢你了！']
  if (growth.hungry) return pickRandom(HUNGRY_LINES, rand)
  if (growth.lethargic) return pickRandom(LETHARGIC_LINES, rand)
  if (growth.affectionate) return pickRandom(AFFECTIONATE_LINES, rand)
  return null
}
