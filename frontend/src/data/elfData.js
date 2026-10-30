import { animeElfCharacters } from './animeElfData'

export const elfCharacters = [
  // 二次元女角色（桌宠立绘体系，置于列表头部作为默认形象）
  ...animeElfCharacters,
  {
    id: 'ball',
    name: '球球',
    category: 'basic',
    description: '可爱的球形精灵，圆滚滚的身体',
    emoji: '🔵',
    baseColor: '#4ade80',
    shape: 'sphere',
    scale: { body: 0.8, head: 0.3, eyes: 0.12 },
    specialEffect: null,
    personality: '活泼可爱'
  },
  {
    id: 'cube',
    name: '方块',
    category: 'basic',
    description: '方正的立方体精灵，稳重可靠',
    emoji: '🟦',
    baseColor: '#60a5fa',
    shape: 'cube',
    scale: { body: 0.7, head: 0.25, eyes: 0.1 },
    specialEffect: null,
    personality: '稳重可靠'
  },
  {
    id: 'star',
    name: '星星',
    category: 'basic',
    description: '闪耀的星形精灵，充满活力',
    emoji: '⭐',
    baseColor: '#fbbf24',
    shape: 'star',
    scale: { body: 0.85, head: 0.28, eyes: 0.11 },
    specialEffect: 'sparkle',
    personality: '活泼开朗'
  },
  {
    id: 'heart',
    name: '心心',
    category: 'basic',
    description: '温暖的心形精灵，充满爱意',
    emoji: '❤️',
    baseColor: '#f472b6',
    shape: 'heart',
    scale: { body: 0.8, head: 0.26, eyes: 0.1 },
    specialEffect: 'glow',
    personality: '温柔体贴'
  },
  {
    id: 'diamond',
    name: '钻石',
    category: 'basic',
    description: '璀璨的菱形精灵，高贵优雅',
    emoji: '💎',
    baseColor: '#a78bfa',
    shape: 'diamond',
    scale: { body: 0.75, head: 0.24, eyes: 0.09 },
    specialEffect: 'shine',
    personality: '高贵优雅'
  },
  {
    id: 'zodiac-rat',
    name: '鼠小灵',
    category: 'zodiac',
    description: '聪明机灵的鼠精灵，十二生肖之首',
    emoji: '🐀',
    baseColor: '#8b7355',
    shape: 'humanoid',
    scale: { body: 0.6, head: 0.35, eyes: 0.15 },
    specialEffect: 'glow',
    personality: '聪明机灵',
    features: ['尖耳朵', '长尾巴', '小巧身材'],
    zodiacSign: 'Rat',
    element: 'Water',
    traits: ['聪明', '灵活', '机敏']
  },
  {
    id: 'zodiac-ox',
    name: '牛大力',
    category: 'zodiac',
    description: '勤劳踏实的牛精灵，力量与耐力的象征',
    emoji: '🐂',
    baseColor: '#8b4513',
    shape: 'humanoid',
    scale: { body: 0.9, head: 0.3, eyes: 0.1 },
    specialEffect: 'steam',
    personality: '勤劳踏实',
    features: ['牛角', '健壮身体', '牛蹄'],
    zodiacSign: 'Ox',
    element: 'Earth',
    traits: ['勤劳', '坚韧', '稳重']
  },
  {
    id: 'zodiac-tiger',
    name: '虎威猛',
    category: 'zodiac',
    description: '勇猛威风的虎精灵，百兽之王',
    emoji: '🐅',
    baseColor: '#ff8c00',
    shape: 'humanoid',
    scale: { body: 0.85, head: 0.32, eyes: 0.13 },
    specialEffect: 'roar',
    personality: '勇猛霸气',
    features: ['虎纹', '尖牙', '虎尾'],
    zodiacSign: 'Tiger',
    element: 'Wood',
    traits: ['勇敢', '自信', '领袖']
  },
  {
    id: 'zodiac-rabbit',
    name: '兔萌萌',
    category: 'zodiac',
    description: '温柔可爱的兔精灵，象征吉祥如意',
    emoji: '🐇',
    baseColor: '#f5f5dc',
    shape: 'humanoid',
    scale: { body: 0.7, head: 0.33, eyes: 0.14 },
    specialEffect: 'float',
    personality: '温柔可爱',
    features: ['长耳朵', '短尾巴', '绒毛'],
    zodiacSign: 'Rabbit',
    element: 'Wood',
    traits: ['温柔', '敏捷', '善良']
  },
  {
    id: 'zodiac-dragon',
    name: '龙傲天',
    category: 'zodiac',
    description: '威武霸气的龙精灵，祥瑞之兽',
    emoji: '🐉',
    baseColor: '#c0392b',
    shape: 'humanoid',
    scale: { body: 0.95, head: 0.35, eyes: 0.12 },
    specialEffect: 'fire',
    personality: '威严霸气',
    features: ['龙角', '龙鳞', '龙须', '龙尾'],
    zodiacSign: 'Dragon',
    element: 'Earth',
    traits: ['威严', '智慧', '神秘']
  },
  {
    id: 'zodiac-snake',
    name: '蛇玲珑',
    category: 'zodiac',
    description: '神秘优雅的蛇精灵，智慧的象征',
    emoji: '🐍',
    baseColor: '#27ae60',
    shape: 'humanoid',
    scale: { body: 0.75, head: 0.28, eyes: 0.11 },
    specialEffect: 'glow',
    personality: '神秘优雅',
    features: ['蛇纹', '鳞片', '细长身体'],
    zodiacSign: 'Snake',
    element: 'Fire',
    traits: ['智慧', '神秘', '优雅']
  },
  {
    id: 'zodiac-horse',
    name: '马奔腾',
    category: 'zodiac',
    description: '热情奔放的马精灵，自由的象征',
    emoji: '🐎',
    baseColor: '#8b4513',
    shape: 'humanoid',
    scale: { body: 0.88, head: 0.32, eyes: 0.12 },
    specialEffect: 'speed',
    personality: '热情奔放',
    features: ['马耳', '马尾', '马蹄'],
    zodiacSign: 'Horse',
    element: 'Fire',
    traits: ['热情', '自由', '敏捷']
  },
  {
    id: 'zodiac-goat',
    name: '羊咩咩',
    category: 'zodiac',
    description: '温顺善良的羊精灵，祥和的象征',
    emoji: '🐐',
    baseColor: '#f5deb3',
    shape: 'humanoid',
    scale: { body: 0.72, head: 0.34, eyes: 0.13 },
    specialEffect: 'cloud',
    personality: '温顺善良',
    features: ['羊角', '羊毛', '羊蹄'],
    zodiacSign: 'Goat',
    element: 'Earth',
    traits: ['善良', '温顺', '艺术']
  },
  {
    id: 'zodiac-monkey',
    name: '猴机灵',
    category: 'zodiac',
    description: '聪明活泼的猴精灵，机智的象征',
    emoji: '🐒',
    baseColor: '#d2691e',
    shape: 'humanoid',
    scale: { body: 0.7, head: 0.3, eyes: 0.14 },
    specialEffect: 'jump',
    personality: '聪明活泼',
    features: ['猴耳', '猴尾', '灵活四肢'],
    zodiacSign: 'Monkey',
    element: 'Metal',
    traits: ['聪明', '灵活', '幽默']
  },
  {
    id: 'zodiac-rooster',
    name: '鸡威武',
    category: 'zodiac',
    description: '勤劳勇敢的鸡精灵，光明的象征',
    emoji: '🐓',
    baseColor: '#dc143c',
    shape: 'humanoid',
    scale: { body: 0.78, head: 0.36, eyes: 0.12 },
    specialEffect: 'sun',
    personality: '勤劳勇敢',
    features: ['鸡冠', '鸡喙', '鸡尾'],
    zodiacSign: 'Rooster',
    element: 'Metal',
    traits: ['勤劳', '自信', '正直']
  },
  {
    id: 'zodiac-dog',
    name: '狗忠诚',
    category: 'zodiac',
    description: '忠诚友善的狗精灵，忠诚的象征',
    emoji: '🐕',
    baseColor: '#654321',
    shape: 'humanoid',
    scale: { body: 0.75, head: 0.31, eyes: 0.13 },
    specialEffect: 'pulse',
    personality: '忠诚友善',
    features: ['狗耳', '狗鼻', '狗尾'],
    zodiacSign: 'Dog',
    element: 'Earth',
    traits: ['忠诚', '友善', '勇敢']
  },
  {
    id: 'zodiac-pig',
    name: '猪胖胖',
    category: 'zodiac',
    description: '憨厚可爱的猪精灵，福气的象征',
    emoji: '🐷',
    baseColor: '#ffb6c1',
    shape: 'humanoid',
    scale: { body: 0.82, head: 0.33, eyes: 0.1 },
    specialEffect: 'sparkle',
    personality: '憨厚可爱',
    features: ['猪鼻', '猪耳', '圆胖身材'],
    zodiacSign: 'Pig',
    element: 'Water',
    traits: ['善良', '乐观', '知足']
  },
  {
    id: 'myth-kunpeng',
    name: '鲲鹏飞',
    category: 'myth',
    description: '《山海经》神鸟，鲲化为鹏，展翅九万里',
    emoji: '🦅',
    baseColor: '#0066cc',
    shape: 'humanoid',
    scale: { body: 0.9, head: 0.32, eyes: 0.12 },
    specialEffect: 'wind',
    personality: '志存高远',
    features: ['鹏翼', '鸟喙', '羽毛'],
    mythSource: '《庄子·逍遥游》',
    abilities: ['翱翔', '变化', '御风']
  },
  {
    id: 'myth-qilin',
    name: '麒麟瑞',
    category: 'myth',
    description: '《山海经》瑞兽，太平盛世的象征',
    emoji: '🦄',
    baseColor: '#ffd700',
    shape: 'humanoid',
    scale: { body: 0.85, head: 0.33, eyes: 0.11 },
    specialEffect: 'rainbow',
    personality: '祥瑞吉祥',
    features: ['鹿角', '牛尾', '鱼鳞', '马蹄'],
    mythSource: '《山海经·西山经》',
    abilities: ['辟邪', '祈福', '祥瑞']
  },
  {
    id: 'myth-taotie',
    name: '饕餮贪',
    category: 'myth',
    description: '《山海经》凶兽，贪吃的象征',
    emoji: '👹',
    baseColor: '#8b0000',
    shape: 'humanoid',
    scale: { body: 0.95, head: 0.4, eyes: 0.15 },
    specialEffect: 'fire',
    personality: '贪吃豪爽',
    features: ['大嘴', '尖牙', '凶眼'],
    mythSource: '《山海经·北山经》',
    abilities: ['吞噬', '力量', '威慑']
  },
  {
    id: 'myth-jiaolong',
    name: '蛟龙潜',
    category: 'myth',
    description: '《山海经》神龙，能兴风作浪',
    emoji: '🐲',
    baseColor: '#008000',
    shape: 'humanoid',
    scale: { body: 0.88, head: 0.34, eyes: 0.12 },
    specialEffect: 'water',
    personality: '神秘强大',
    features: ['龙角', '龙爪', '龙鳞'],
    mythSource: '《山海经·中山经》',
    abilities: ['控水', '飞行', '变化']
  },
  {
    id: 'myth-baihu',
    name: '白虎啸',
    category: 'myth',
    description: '《山海经》神兽，西方之神，主杀伐',
    emoji: '🐯',
    baseColor: '#ffffff',
    shape: 'humanoid',
    scale: { body: 0.92, head: 0.35, eyes: 0.13 },
    specialEffect: 'lightning',
    personality: '威严勇猛',
    features: ['虎纹', '白须', '利爪'],
    mythSource: '《山海经·西次四经》',
    abilities: ['速度', '力量', '威严']
  }
]

export const elfActions = [
  {
    id: 'stand',
    name: '站立',
    description: '精灵站立的基础姿势',
    animation: {
      type: 'stand',
      duration: 200,
      params: { bodyY: 0, rotationZ: 0, scaleY: 1 }
    }
  },
  {
    id: 'crouch',
    name: '蹲下',
    description: '精灵蹲下的姿势',
    animation: {
      type: 'crouch',
      duration: 300,
      params: { bodyY: -0.3, rotationZ: 0, scaleY: 0.7 }
    }
  },
  {
    id: 'lie',
    name: '趴着',
    description: '精灵趴着的姿势',
    animation: {
      type: 'lie',
      duration: 400,
      params: { bodyY: -0.5, rotationZ: Math.PI / 2, scaleY: 0.5 }
    }
  },
  {
    id: 'wave',
    name: '挥手',
    description: '精灵挥手打招呼',
    animation: {
      type: 'wave',
      duration: 600,
      params: { armSwing: true, waveCount: 3 }
    }
  },
  {
    id: 'dance',
    name: '跳舞',
    description: '精灵欢快地跳舞',
    animation: {
      type: 'dance',
      duration: 1000,
      params: { jump: true, spin: true, danceCount: 5 }
    }
  },
  {
    id: 'jump',
    name: '跳跃',
    description: '精灵向上跳跃',
    animation: {
      type: 'jump',
      duration: 600,
      params: { jumpHeight: 1.5, gravity: true }
    }
  },
  {
    id: 'idle',
    name: '空闲',
    description: '精灵空闲时的小动作',
    animation: {
      type: 'idle',
      duration: 2000,
      params: { float: true, sway: true, blink: true }
    }
  },
  {
    id: 'sleep',
    name: '睡觉',
    description: '精灵进入睡眠状态',
    animation: {
      type: 'sleep',
      duration: 500,
      params: { bodyY: -0.4, eyesClosed: true, breathing: true }
    }
  },
  {
    id: 'happy',
    name: '开心',
    description: '精灵开心的表情',
    animation: {
      type: 'happy',
      duration: 400,
      params: { scaleX: 1.1, scaleY: 1.1, eyesOpen: true }
    }
  },
  {
    id: 'sad',
    name: '难过',
    description: '精灵难过的表情',
    animation: {
      type: 'sad',
      duration: 400,
      params: { scaleY: 0.9, bodyY: -0.1, eyesClosed: true }
    }
  },
  {
    id: 'surprised',
    name: '惊讶',
    description: '精灵惊讶的表情',
    animation: {
      type: 'surprised',
      duration: 300,
      params: { scaleX: 1.15, eyesWide: true }
    }
  },
  {
    id: 'thinking',
    name: '思考',
    description: '精灵思考的姿势',
    animation: {
      type: 'thinking',
      duration: 800,
      params: { headTilt: true, eyesBlink: true }
    }
  },
  {
    id: 'sit',
    name: '坐下',
    description: '精灵乖巧地坐下休息',
    animation: {
      type: 'sit',
      duration: 1500,
      params: { bodyY: -0.45, legsFold: true, eyesBlink: true }
    }
  },
  {
    id: 'feed',
    name: '喂食',
    description: '喂精灵吃好吃的（饱腹+，爱心特效）',
    animation: {
      type: 'feed',
      duration: 1800,
      params: { eating: true, sparkle: true, mouthOpen: true }
    }
  },
  {
    id: 'pat',
    name: '摸摸头',
    description: '温柔地摸摸精灵的头（亲密度+，爱心特效）',
    animation: {
      type: 'pat',
      duration: 1200,
      params: { headTap: true, eyesHappy: true }
    }
  },
  {
    id: 'run',
    name: '奔跑',
    description: '精灵欢快地跑起来',
    animation: {
      type: 'run',
      duration: 700,
      params: { speed: 2.2, armsSwing: true }
    }
  }
]

export const elfSkins = [
  {
    id: 'default',
    name: '默认',
    description: '精灵的默认外观',
    colors: { body: null, eyes: '#1f2937', mouth: '#1f2937', glow: null, highlight: '#ffffff' },
    texture: 'none'
  },
  {
    id: 'neon',
    name: '霓虹',
    description: '炫彩霓虹风格',
    colors: { body: null, eyes: '#00ffff', mouth: '#ff00ff', glow: '#00ffff', highlight: '#ffffff' },
    texture: 'neon'
  },
  {
    id: 'pastel',
    name: '糖果',
    description: '柔和的糖果色系',
    colors: { body: null, eyes: '#6b7280', mouth: '#9ca3af', glow: null, highlight: '#fef3c7' },
    texture: 'smooth'
  },
  {
    id: 'fire',
    name: '火焰',
    description: '热情如火的风格',
    colors: { body: null, eyes: '#1f2937', mouth: '#1f2937', glow: '#ff6b35', highlight: '#fcd34d' },
    texture: 'gradient'
  },
  {
    id: 'ice',
    name: '冰霜',
    description: '清凉冰霜风格',
    colors: { body: null, eyes: '#1e40af', mouth: '#1e3a8a', glow: '#60a5fa', highlight: '#e0f2fe' },
    texture: 'shiny'
  },
  {
    id: 'dark',
    name: '暗夜',
    description: '神秘暗夜风格',
    colors: { body: null, eyes: '#a78bfa', mouth: '#7c3aed', glow: '#4c1d95', highlight: '#e9d5ff' },
    texture: 'dark'
  },
  {
    id: 'gold',
    name: '黄金',
    description: '华丽金色风格',
    colors: { body: null, eyes: '#1f2937', mouth: '#1f2937', glow: '#fbbf24', highlight: '#fef3c7' },
    texture: 'shiny'
  },
  {
    id: 'forest',
    name: '森林',
    description: '清新森林风格',
    colors: { body: null, eyes: '#14532d', mouth: '#166534', glow: '#22c55e', highlight: '#dcfce7' },
    texture: 'natural'
  }
]

export const elfOutfits = [
  { id: 'none', name: '无', description: '不穿任何服装', type: 'none', color: null, emoji: '◻️' },
  { id: 'hat', name: '帽子', description: '可爱的小帽子', type: 'accessory', color: '#f59e0b', emoji: '🎩' },
  { id: 'glasses', name: '眼镜', description: '时尚的眼镜', type: 'accessory', color: '#1f2937', emoji: '🕶️' },
  { id: 'scarf', name: '围巾', description: '温暖的围巾', type: 'clothing', color: '#ef4444', emoji: '🧣' },
  { id: 'bow', name: '蝴蝶结', description: '漂亮的蝴蝶结', type: 'accessory', color: '#ec4899', emoji: '🎀' },
  { id: 'crown', name: '皇冠', description: '华丽的皇冠', type: 'accessory', color: '#fbbf24', emoji: '👑' },
  { id: 'wings', name: '翅膀', description: '天使翅膀', type: 'accessory', color: '#e0f2fe', emoji: '🪽' },
  { id: 'coat', name: '外套', description: '帅气的外套', type: 'clothing', color: '#3b82f6', emoji: '🧥' },
  { id: 'robe', name: '长袍', description: '神秘的长袍', type: 'clothing', color: '#7c3aed', emoji: '🧙' },
  { id: 'armor', name: '铠甲', description: '威武的铠甲', type: 'clothing', color: '#6b7280', emoji: '🛡️' }
]

export const elfInteractions = [
  {
    id: 'greeting-morning',
    type: 'time',
    triggers: [{ hour: [6, 12] }],
    responses: [
      { type: 'dialog', content: '☀️ 早上好！新的一天开始了~', duration: 3000 },
      { type: 'action', actionId: 'wave' },
      { type: 'emotion', emoji: '🌞' }
    ],
    cooldown: 3600000
  },
  {
    id: 'greeting-afternoon',
    type: 'time',
    triggers: [{ hour: [12, 18] }],
    responses: [
      { type: 'dialog', content: '🌤️ 下午好！工作辛苦了~', duration: 3000 },
      { type: 'action', actionId: 'happy' },
      { type: 'emotion', emoji: '💪' }
    ],
    cooldown: 3600000
  },
  {
    id: 'greeting-evening',
    type: 'time',
    triggers: [{ hour: [18, 22] }],
    responses: [
      { type: 'dialog', content: '🌙 晚上好！放松一下吧~', duration: 3000 },
      { type: 'action', actionId: 'idle' },
      { type: 'emotion', emoji: '🌃' }
    ],
    cooldown: 3600000
  },
  {
    id: 'greeting-night',
    type: 'time',
    triggers: [{ hour: [22, 6] }],
    responses: [
      { type: 'dialog', content: '😴 夜深了，该休息了哦~', duration: 3000 },
      { type: 'action', actionId: 'sleep' },
      { type: 'emotion', emoji: '🌚' }
    ],
    cooldown: 3600000
  },
  {
    id: 'mouse-hover',
    type: 'mouse',
    triggers: [{ event: 'mouseenter' }],
    responses: [
      { type: 'action', actionId: 'wave' },
      { type: 'dialog', content: '👋 你好呀！', duration: 2000 }
    ],
    cooldown: 5000
  },
  {
    id: 'mouse-click',
    type: 'mouse',
    triggers: [{ event: 'click' }],
    responses: [
      { type: 'action', actionId: 'jump' },
      { type: 'emotion', emoji: '✨' }
    ],
    cooldown: 3000
  },
  {
    id: 'system-startup',
    type: 'system',
    triggers: [{ event: 'startup' }],
    responses: [
      { type: 'dialog', content: '🚀 我启动啦！很高兴见到你~', duration: 3000 },
      { type: 'action', actionId: 'happy' },
      { type: 'emotion', emoji: '🎉' }
    ],
    cooldown: 1800000
  },
  {
    id: 'system-idle',
    type: 'system',
    triggers: [{ event: 'idle', duration: 60000 }],
    responses: [
      { type: 'dialog', content: '😌 你在忙什么呢？', duration: 2000 },
      { type: 'action', actionId: 'thinking' },
      { type: 'emotion', emoji: '💭' }
    ],
    cooldown: 300000
  },
  {
    id: 'battery-low',
    type: 'system',
    triggers: [{ event: 'battery', level: [0, 20] }],
    responses: [
      { type: 'dialog', content: '🔋 电量不足了，记得充电哦！', duration: 3000 },
      { type: 'action', actionId: 'sad' },
      { type: 'emotion', emoji: '😰' }
    ],
    cooldown: 1800000
  },
  {
    id: 'weather-sunny',
    type: 'environment',
    triggers: [{ weather: 'sunny' }],
    responses: [
      { type: 'dialog', content: '☀️ 今天天气真好！', duration: 2500 },
      { type: 'action', actionId: 'happy' },
      { type: 'emotion', emoji: '🌞' }
    ],
    cooldown: 7200000
  },
  {
    id: 'weather-rainy',
    type: 'environment',
    triggers: [{ weather: 'rainy' }],
    responses: [
      { type: 'dialog', content: '🌧️ 下雨了，记得带伞~', duration: 2500 },
      { type: 'action', actionId: 'sad' },
      { type: 'emotion', emoji: '☔' }
    ],
    cooldown: 7200000
  },
  {
    id: 'weekend',
    type: 'time',
    triggers: [{ dayOfWeek: [0, 6] }],
    responses: [
      { type: 'dialog', content: '🎉 周末快乐！好好休息吧~', duration: 3000 },
      { type: 'action', actionId: 'dance' },
      { type: 'emotion', emoji: '🎊' }
    ],
    cooldown: 86400000
  },
  {
    id: 'custom-1',
    type: 'custom',
    triggers: [{ event: 'user-interaction' }],
    responses: [
      { type: 'dialog', content: '😊 谢谢你陪我玩！', duration: 2500 },
      { type: 'action', actionId: 'wave' },
      { type: 'emotion', emoji: '❤️' }
    ],
    cooldown: 10000
  },
  {
    id: 'custom-2',
    type: 'custom',
    triggers: [{ event: 'achievement' }],
    responses: [
      { type: 'dialog', content: '🏆 恭喜你达成成就！', duration: 3000 },
      { type: 'action', actionId: 'jump' },
      { type: 'emotion', emoji: '🌟' }
    ],
    cooldown: 1800000
  }
]

export const specialEffects = {
  sparkle: {
    name: '闪烁',
    animation: 'sparkle',
    duration: 1000,
    particles: true
  },
  glow: {
    name: '发光',
    animation: 'glow',
    duration: 2000,
    particles: false
  },
  fire: {
    name: '火焰',
    animation: 'fire',
    duration: 1500,
    particles: true
  },
  float: {
    name: '漂浮',
    animation: 'float',
    duration: 3000,
    particles: false
  },
  steam: {
    name: '蒸汽',
    animation: 'steam',
    duration: 1000,
    particles: true
  },
  roar: {
    name: '咆哮',
    animation: 'roar',
    duration: 800,
    particles: false
  },
  shine: {
    name: '闪耀',
    animation: 'shine',
    duration: 1500,
    particles: true
  },
  speed: {
    name: '速度',
    animation: 'speed',
    duration: 1000,
    particles: true
  },
  cloud: {
    name: '云朵',
    animation: 'cloud',
    duration: 2000,
    particles: true
  },
  jump: {
    name: '跳跃',
    animation: 'jump',
    duration: 600,
    particles: false
  },
  sun: {
    name: '阳光',
    animation: 'sun',
    duration: 2000,
    particles: true
  },
  pulse: {
    name: '心跳',
    animation: 'pulse',
    duration: 1500,
    particles: false
  },
  wind: {
    name: '风',
    animation: 'wind',
    duration: 1500,
    particles: true
  },
  rainbow: {
    name: '彩虹',
    animation: 'rainbow',
    duration: 3000,
    particles: true
  },
  water: {
    name: '水',
    animation: 'water',
    duration: 1500,
    particles: true
  },
  lightning: {
    name: '闪电',
    animation: 'lightning',
    duration: 500,
    particles: true
  }
}