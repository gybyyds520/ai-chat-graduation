/**
 * elfOutfitSets.js — 二次元桌宠 3D 换装套装数据（换装小游戏体验）
 *
 * 每套套装 = 上衣 + 下装 + 鞋 + 袜子 + 发饰 + 完整配色，
 * 由 useElf3DRenderer 的 buildOutfit3D 程序化构建为真实 3D 服装部件。
 *
 * 支持三种换装能力：
 * 1. 多套预设服装：一键切换完整套装（7 套）
 * 2. 单件自由混搭：top/bottom/shoes/socks/accessory 各自独立选择
 * 3. 服装换色：每部件颜色可覆盖（见 ELF_OUTFIT_COLOR_PALETTES）
 *
 * 纯数据模块，无运行时依赖，可独立测试。
 */

export const ELF_OUTFIT_TOP_TYPES = [
  'shirt',      // 学院衬衫（白衬衫+袖口+领结）
  'puff',       // 泡泡袖甜心上衣
  'tshirt',     // 运动 T 恤（短袖+领口）
  'yukata',     // 和风浴衣（V 领+腰带）
  'pajama',     // 宽松睡衣（纽扣+睡帽由配件提供）
  'sailor',     // 水手服（大翻领+条纹领巾）
  'dress',      // 连衣裙（公主风收腰长裙）
  'hanfu'       // 汉服（交领右衽+宽袖+腰带，古风）
]

export const ELF_OUTFIT_BOTTOM_TYPES = [
  'pleated-skirt', // 百褶裙
  'puff-skirt',    // 蓬蓬裙（双层）
  'shorts',        // 运动短裤
  'straight',      // 和风长下摆
  'pants',         // 睡裤
  'pencil',        // 铅笔裙（包臀修身）
  'flared',        // A 字裙（自然外扩）
  'long-skirt'     // 古风长裙（曳地裙摆）
]

export const ELF_OUTFIT_SHOE_TYPES = [
  'leather',   // 学院小皮鞋
  'maryjane',  // 玛丽珍鞋
  'sneakers',  // 运动鞋
  'clogs',     // 木屐
  'slippers',  // 毛绒拖鞋
  'pumps'      // 高跟鞋（细跟）
]

export const ELF_OUTFIT_SOCK_TYPES = ['long', 'short', 'none']

/** 发饰类型：蝴蝶结/发带/星星发夹/皇冠/花环/古风发簪/无 */
export const ELF_OUTFIT_ACCESSORY_TYPES = ['bow', 'ribbon', 'star', 'crown', 'flower', 'hairpin', 'none']

/** 可独立混搭/换色的部件键（单件混搭 + 换色共用） */
export const ELF_OUTFIT_PART_KEYS = ['top', 'bottom', 'shoes', 'socks', 'accessory']

/**
 * 每部件换色色板（供换装面板"服装换色"使用）。
 * 每部件 6 个颜色；颜色均为合法 6 位十六进制。
 */
export const ELF_OUTFIT_COLOR_PALETTES = {
  top: ['#ffffff', '#ffb7d5', '#38bdf8', '#7c6ac2', '#a5b4fc', '#ff8a5c'],
  bottom: ['#3f5bd9', '#ff8fb8', '#1e293b', '#7c6ac2', '#a5b4fc', '#2dd4a8'],
  shoes: ['#2b2b3a', '#8b5a2b', '#e2e8f0', '#a8754a', '#f9a8d4', '#f43f5e'],
  socks: ['#f6f8fc', '#ffffff', '#f1f5f9', '#e0e7ff', '#ffd6e8', '#334155'],
  accessory: ['#ff4d8f', '#ffffff', '#ffd54f', '#c084fc', '#38bdf8', '#f97316'],
  trim: ['#ffffff', '#ffe9f3', '#0ea5e9', '#f3e9ff', '#c7d2fe', '#fde68a']
}

/**
 * 7 套完整套装（颜色均为合法 6 位十六进制）
 */
export const elfOutfitSets = [
  {
    id: 'school',
    name: '学院校服',
    emoji: '🎀',
    description: '经典学院风：白衬衫、领结与百褶裙',
    top: 'shirt',
    bottom: 'pleated-skirt',
    shoes: 'leather',
    socks: 'long',
    accessory: 'bow',
    colors: {
      top: '#ffffff',
      topTrim: '#eef1f7',
      bottom: '#3f5bd9',
      accent: '#ff4d8f',
      shoes: '#2b2b3a',
      socks: '#f6f8fc',
      trim: '#ffffff',
      accessory: '#ff4d8f'
    }
  },
  {
    id: 'sweet',
    name: '甜心洋装',
    emoji: '👗',
    description: '粉嫩泡泡袖与双层蓬蓬裙',
    top: 'puff',
    bottom: 'puff-skirt',
    shoes: 'maryjane',
    socks: 'short',
    accessory: 'flower',
    colors: {
      top: '#ffb7d5',
      topTrim: '#ffd6e8',
      bottom: '#ff8fb8',
      accent: '#ffffff',
      shoes: '#8b5a2b',
      socks: '#ffffff',
      trim: '#ffe9f3',
      accessory: '#ffffff'
    }
  },
  {
    id: 'sport',
    name: '运动活力',
    emoji: '⚡',
    description: '清爽 T 恤与运动短裤，随时出发',
    top: 'tshirt',
    bottom: 'shorts',
    shoes: 'sneakers',
    socks: 'short',
    accessory: 'ribbon',
    colors: {
      top: '#38bdf8',
      topTrim: '#e0f2fe',
      bottom: '#1e293b',
      accent: '#ffffff',
      shoes: '#e2e8f0',
      socks: '#f1f5f9',
      trim: '#0ea5e9',
      accessory: '#38bdf8'
    }
  },
  {
    id: 'yukata',
    name: '和风浴衣',
    emoji: '🏮',
    description: '典雅浴衣配腰带，温柔和风',
    top: 'yukata',
    bottom: 'straight',
    shoes: 'clogs',
    socks: 'none',
    accessory: 'flower',
    colors: {
      top: '#7c6ac2',
      topTrim: '#a594e0',
      bottom: '#7c6ac2',
      accent: '#ffd54f',
      shoes: '#a8754a',
      socks: '#ffffff',
      trim: '#f3e9ff',
      accessory: '#ffd54f'
    }
  },
  {
    id: 'pajama',
    name: '睡衣软萌',
    emoji: '🌙',
    description: '宽松睡衣与睡裤，困困的也很可爱',
    top: 'pajama',
    bottom: 'pants',
    shoes: 'slippers',
    socks: 'none',
    accessory: 'bow',
    colors: {
      top: '#a5b4fc',
      topTrim: '#e0e7ff',
      bottom: '#a5b4fc',
      accent: '#ffffff',
      shoes: '#f9a8d4',
      socks: '#ffffff',
      trim: '#c7d2fe',
      accessory: '#ffffff'
    }
  },
  {
    id: 'princess',
    name: '公主裙',
    emoji: '👑',
    description: '皇冠与蓬蓬礼服裙，星光闪闪',
    top: 'dress',
    bottom: 'flared',
    shoes: 'pumps',
    socks: 'none',
    accessory: 'crown',
    colors: {
      top: '#f9e9ff',
      topTrim: '#f3d6ff',
      bottom: '#d8b4fe',
      accent: '#ffd54f',
      shoes: '#d8b4fe',
      socks: '#ffffff',
      trim: '#f0abfc',
      accessory: '#ffd54f'
    }
  },
  {
    id: 'gothic',
    name: '哥特洛丽塔',
    emoji: '🖤',
    description: '暗黑荷叶边与长裙，神秘优雅',
    top: 'sailor',
    bottom: 'pencil',
    shoes: 'pumps',
    socks: 'long',
    accessory: 'star',
    colors: {
      top: '#1e1e2e',
      topTrim: '#3a3a52',
      bottom: '#2b2b3a',
      accent: '#e11d48',
      shoes: '#11111a',
      socks: '#1e1e2e',
      trim: '#57577a',
      accessory: '#e11d48'
    }
  },
  {
    id: 'gufeng',
    name: '古风华服',
    emoji: '🏮',
    description: '青绿汉服与曳地长裙，发簪轻挽古典风韵',
    top: 'hanfu',
    bottom: 'long-skirt',
    shoes: 'leather',
    socks: 'none',
    accessory: 'hairpin',
    colors: {
      top: '#5bb8a8',
      topTrim: '#d9ece4',
      bottom: '#7fc8b8',
      accent: '#ffd9a0',
      shoes: '#a8754a',
      socks: '#f6f8fc',
      trim: '#e8f2ee',
      accessory: '#ffd9a0'
    }
  }
]

/** 默认套装（角色初始） */
export const DEFAULT_OUTFIT_SET = elfOutfitSets[0]

/**
 * 单件混搭：以预设套装为基底，替换某部件款式。
 * 返回可被渲染器 resolveOutfitSet 直接消费的自定义套装对象。
 * @param {Object} baseSet 基底套装（预设套装或已有自定义）
 * @param {string} part 部件键（ELF_OUTFIT_PART_KEYS 之一）
 * @param {string} type 目标款式
 * @returns {Object} 自定义套装对象
 */
export function buildMixOutfit(baseSet, part, type) {
  const base = baseSet || DEFAULT_OUTFIT_SET
  return {
    top: base.top,
    bottom: base.bottom,
    shoes: base.shoes,
    socks: base.socks,
    accessory: base.accessory,
    colors: { ...(base.colors || {}) },
    [part]: type
  }
}

/**
 * 服装换色：覆盖某部件颜色（不修改基底套装数据）。
 * @param {Object} customOutfit 自定义套装对象（buildMixOutfit 产物或手动构造）
 * @param {string} part 部件键
 * @param {string} color 6 位十六进制颜色
 * @returns {Object} 新的自定义套装对象（不改原对象）
 */
export function applyColorOverride(customOutfit, part, color) {
  const base = customOutfit || DEFAULT_OUTFIT_SET
  return {
    top: base.top,
    bottom: base.bottom,
    shoes: base.shoes,
    socks: base.socks,
    accessory: base.accessory,
    colors: { ...(base.colors || {}), [part]: color }
  }
}

/**
 * 服装换色：还原某部件为套装默认色（删除覆盖）。
 * @param {Object} customOutfit 自定义套装对象
 * @param {string} part 部件键
 * @returns {Object} 新的自定义套装对象（不改原对象）
 */
export function removeColorOverride(customOutfit, part) {
  if (!customOutfit || !customOutfit.colors) return customOutfit
  const colors = { ...customOutfit.colors }
  delete colors[part]
  return { ...customOutfit, colors }
}
