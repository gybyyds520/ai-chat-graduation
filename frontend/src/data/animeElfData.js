/**
 * animeElfData.js — 二次元桌宠女角色数据
 *
 * 在原有"几何/生肖/山海经"精灵之外新增的二次元美少女形象体系。
 * 每个角色为参数化 Q 版立绘：发色/瞳色/服装/发型/配件全部可配置，
 * 由 AnimeElfSprite.vue 渲染为可动画 SVG 立绘（眨眼/呼吸/头发飘动/表情切换）。
 *
 * 纯数据模块，无运行时依赖，可独立测试。
 */

export const ANIME_HAIRSTYLES = [
  'gufeng-bun',  // 古风盘发（发髻+鬓发，青黛专属）
  'twin',      // 双马尾
  'long',      // 长直发
  'bob',       // 波波头
  'ponytail',  // 高马尾
  'curly',     // 卷发
  'short'      // 短发
]

export const ANIME_ACCESSORIES = [
  'none',
  'jade-hairpin',  // 玉簪（古风，青黛专属）
  'bow',          // 蝴蝶结
  'cat-ears',     // 猫耳
  'ribbon',       // 发带
  'headphones',   // 耳机
  'crown',        // 皇冠
  'flower',       // 头花
  'star-hairpin'  // 星星发夹
]

/**
 * 7 位二次元女角色（健康向 Q 版形象；首位青黛为默认形象，古风美人）
 */
export const animeElfCharacters = [
  {
    id: 'anime-qingdai',
    name: '青黛',
    category: 'anime',
    description: '浅青绿汉服的古典美人，云鬓盘发、玉簪轻挽',
    personality: '温婉典雅',
    emoji: '🌿',
    baseColor: '#5bb8a8',
    shape: 'anime-girl',
    specialEffect: 'glow',
    hairstyle: 'gufeng-bun',
    accessory: 'jade-hairpin',
    scale: { body: 0.85, head: 0.4 },
    palette: {
      hair: '#3f4a44',
      hairHighlight: '#8aa99c',
      eyes: '#4e9e8a',
      eyesHighlight: '#a8d8c8',
      skin: '#ffe9dc',
      skinShadow: '#f5c6ad',
      outfit: '#5bb8a8',
      outfitAccent: '#ffd9a0',
      ribbon: '#d9ece4',
      blush: '#ffb3c8'
    },
    lines: [
      '青衫云鬓，愿与君同赏风月。',
      '玉簪轻挽，此间清欢只为你。',
      '山色空蒙，我陪你看云卷云舒。',
      '汉家衣袂，与你共度岁月静好。'
    ]
  },
  {
    id: 'anime-yingnai',
    name: '樱奈',
    category: 'anime',
    description: '元气满满的粉色双马尾少女，喜欢唱歌',
    personality: '元气可爱',
    emoji: '🌸',
    baseColor: '#ff6f9c',
    shape: 'anime-girl',
    specialEffect: 'sparkle',
    hairstyle: 'twin',
    accessory: 'bow',
    scale: { body: 0.85, head: 0.4 },
    palette: {
      hair: '#ff9ec7',
      hairHighlight: '#ffd6e8',
      eyes: '#ff6f9c',
      eyesHighlight: '#ffc2d9',
      skin: '#ffe9dc',
      skinShadow: '#f5c6ad',
      outfit: '#ffffff',
      outfitAccent: '#ff4d8f',
      ribbon: '#ff4d8f',
      blush: '#ff9dbb'
    },
    lines: [
      '今天也要元气满满哦！',
      '嘿嘿，戳我干嘛~',
      '主人，陪我唱首歌嘛！',
      '粉色的心情，最配今天的我啦！'
    ]
  },
  {
    id: 'anime-xingye',
    name: '星野',
    category: 'anime',
    description: '银白长直发的温柔少女，气质清冷',
    personality: '温柔清冷',
    emoji: '🌙',
    baseColor: '#7c8cf8',
    shape: 'anime-girl',
    specialEffect: 'glow',
    hairstyle: 'long',
    accessory: 'ribbon',
    scale: { body: 0.85, head: 0.4 },
    palette: {
      hair: '#e8eaf6',
      hairHighlight: '#ffffff',
      eyes: '#7c8cf8',
      eyesHighlight: '#b9c2ff',
      skin: '#ffe9dc',
      skinShadow: '#f5c6ad',
      outfit: '#a5c8ff',
      outfitAccent: '#5b7cfa',
      ribbon: '#5b7cfa',
      blush: '#ffb3c8'
    },
    lines: [
      '安静的时候，我也在陪着你哦。',
      '想安静一会儿，可以靠着我。',
      '月光真美，像你一样。',
      '别担心，我一直都在。'
    ]
  },
  {
    id: 'anime-taotao',
    name: '桃桃',
    category: 'anime',
    description: '活力四射的橙色短发少女，元气小太阳',
    personality: '活泼开朗',
    emoji: '🍑',
    baseColor: '#ff9f43',
    shape: 'anime-girl',
    specialEffect: 'sun',
    hairstyle: 'short',
    accessory: 'flower',
    scale: { body: 0.85, head: 0.4 },
    palette: {
      hair: '#ffb067',
      hairHighlight: '#ffe0b3',
      eyes: '#c67b3d',
      eyesHighlight: '#ffd9a8',
      skin: '#ffe9dc',
      skinShadow: '#f5c6ad',
      outfit: '#ff9f43',
      outfitAccent: '#e67e22',
      ribbon: '#ff6f3c',
      blush: '#ffb37a'
    },
    lines: [
      '太阳这么棒，出来玩嘛！',
      '我是你的小太阳，永远发光~',
      '嘿嘿，元气值满格！',
      '走，去晒晒太阳吧！'
    ]
  },
  {
    id: 'anime-lulu',
    name: '露露',
    category: 'anime',
    description: '蓝色波波头的安静少女，爱看书',
    personality: '文静乖巧',
    emoji: '💙',
    baseColor: '#3d8bfd',
    shape: 'anime-girl',
    specialEffect: 'water',
    hairstyle: 'bob',
    accessory: 'headphones',
    scale: { body: 0.85, head: 0.4 },
    palette: {
      hair: '#7ec8ff',
      hairHighlight: '#d6f0ff',
      eyes: '#3d8bfd',
      eyesHighlight: '#9ecbff',
      skin: '#ffe9dc',
      skinShadow: '#f5c6ad',
      outfit: '#6db7ff',
      outfitAccent: '#2f6fe0',
      ribbon: '#2f6fe0',
      blush: '#ffb3c8'
    },
    lines: [
      '嘘，我在看书呢，一起吗？',
      '安静地待着，也是一种陪伴。',
      '这本书的故事，我讲给你听呀。',
      '耳机戴好，世界就是我的啦。'
    ]
  },
  {
    id: 'anime-xiami',
    name: '夏弥',
    category: 'anime',
    description: '栗色高马尾的活泼少女，运动全能',
    personality: '阳光活力',
    emoji: '🌻',
    baseColor: '#b5793f',
    shape: 'anime-girl',
    specialEffect: 'wind',
    hairstyle: 'ponytail',
    accessory: 'star-hairpin',
    scale: { body: 0.85, head: 0.4 },
    palette: {
      hair: '#a9713f',
      hairHighlight: '#d9b48f',
      eyes: '#4e9e5f',
      eyesHighlight: '#a5d8a8',
      skin: '#ffe9dc',
      skinShadow: '#f5c6ad',
      outfit: '#f0c987',
      outfitAccent: '#b5793f',
      ribbon: '#d9822b',
      blush: '#ffb37a'
    },
    lines: [
      '今天也要跑起来呀！',
      '运动完超有精神的！',
      '来，跟我做一组拉伸！',
      '阳光、汗水，都是青春！'
    ]
  },
  {
    id: 'anime-yuejian',
    name: '月见',
    category: 'anime',
    description: '紫色卷发的神秘少女，喜欢看星星',
    personality: '神秘优雅',
    emoji: '✨',
    baseColor: '#8e5fd0',
    shape: 'anime-girl',
    specialEffect: 'rainbow',
    hairstyle: 'curly',
    accessory: 'cat-ears',
    scale: { body: 0.85, head: 0.4 },
    palette: {
      hair: '#b98ae0',
      hairHighlight: '#e2ccf7',
      eyes: '#8e5fd0',
      eyesHighlight: '#c9a5f0',
      skin: '#ffe9dc',
      skinShadow: '#f5c6ad',
      outfit: '#c9a2f0',
      outfitAccent: '#7e4fbf',
      ribbon: '#7e4fbf',
      blush: '#e8b6e0'
    },
    lines: [
      '夜空里，星星都在听我们说话。',
      '喵~ 我是神秘的小猫娘哦。',
      '月亮升起时，我会更想你。',
      '嘘，让我看看今天的星象~'
    ]
  }
]

/**
 * 获取全部二次元角色（别名导出，语义清晰）
 */
export const elfAnimeCharacters = animeElfCharacters

/**
 * 按 id 查找二次元角色
 * @param {string} id
 * @returns {Object|undefined}
 */
export function getAnimeCharacterById(id) {
  return animeElfCharacters.find((c) => c.id === id)
}

/**
 * 判断角色是否为二次元女角色
 * @param {Object|null|undefined} character
 * @returns {boolean}
 */
export function isAnimeCharacter(character) {
  return Boolean(character && character.category === 'anime')
}

/**
 * 校验二次元角色数据完整性（供测试与运行时防御使用）
 * @param {Object} character
 * @returns {{ valid: boolean, errors: string[] }}
 */
export function validateAnimeCharacter(character) {
  const errors = []
  if (!character) {
    return { valid: false, errors: ['角色为空'] }
  }
  if (typeof character.id !== 'string' || !character.id.startsWith('anime-')) {
    errors.push('id 必须以 anime- 开头')
  }
  if (!character.name || typeof character.name !== 'string') {
    errors.push('name 缺失')
  }
  if (character.category !== 'anime') {
    errors.push('category 必须为 anime')
  }
  if (!character.palette || typeof character.palette !== 'object') {
    errors.push('palette 缺失')
  } else {
    const hexRe = /^#[0-9a-fA-F]{6}$/
    for (const key of ['hair', 'eyes', 'skin', 'outfit', 'outfitAccent', 'ribbon', 'blush']) {
      if (!hexRe.test(character.palette[key] || '')) {
        errors.push(`palette.${key} 必须是 6 位十六进制颜色`)
      }
    }
  }
  if (!ANIME_HAIRSTYLES.includes(character.hairstyle)) {
    errors.push(`hairstyle 非法: ${character.hairstyle}`)
  }
  if (!ANIME_ACCESSORIES.includes(character.accessory)) {
    errors.push(`accessory 非法: ${character.accessory}`)
  }
  return { valid: errors.length === 0, errors }
}
