/**
 * elf3dConfig.js — 精灵 3D 渲染纯函数配置模块
 *
 * 将角色配置映射为 three.js 构建参数（几何体类型/尺寸/颜色/饰品/动画关键帧）。
 * 本模块为纯函数，不依赖 DOM/three.js，可独立在 jsdom/node 环境测试。
 */

// ─── 7. 桌面宠物高级互动纯函数 ───────────────────────────────────
// 眼神跟随/待机状态等互动逻辑的纯函数部分，可在 jsdom/node 下独立测试。

/**
 * 把鼠标坐标相对精灵中心的偏移归一化为 -1~1 的注视方向。
 * @param {number} mouseX 鼠标绝对 x
 * @param {number} mouseY 鼠标绝对 y
 * @param {{x:number, y:number, width:number, height:number}} rect 精灵容器矩形
 * @param {number} [range=90] 注视生效范围（px，超出后按最大偏移钳制）
 * @returns {{ x:number, y:number }}
 *   x/y 均为 -1~1：x>0 朝右看、y>0 朝下看；0 表示正对屏幕
 */
export function normalizeGaze(mouseX, mouseY, rect, range = 90) {
  if (!rect || typeof rect.x !== 'number' || typeof rect.width !== 'number') {
    return { x: 0, y: 0 }
  }
  const cx = rect.x + rect.width / 2
  const cy = rect.y + rect.height / 2
  const r = Math.max(1, range)
  return {
    x: Math.max(-1, Math.min(1, (mouseX - cx) / r)),
    y: Math.max(-1, Math.min(1, (mouseY - cy) / r))
  }
}

/**
 * 根据无操作时长计算待机阶段。
 * @param {number} elapsedMs 距上次交互的毫秒数
 * @param {object} [opts]
 * @param {number} [opts.snoozeMs=60000] 打瞌睡阈值（毫秒）
 * @param {number} [opts.sleepMs=120000] 睡着阈值（毫秒）
 * @returns {number} 0=清醒 / 1=瞌睡（点头打盹）/ 2=睡着（sleep + zZ）
 */
export function getIdleStage(elapsedMs, opts = {}) {
  const snooze = opts.snoozeMs || 60000
  const sleep = opts.sleepMs || 120000
  const el = Math.max(0, elapsedMs || 0)
  if (el >= sleep) return 2
  if (el >= snooze) return 1
  return 0
}

// ─── 1. 几何体映射 ───────────────────────────────────────────────

/**
 * 根据角色 shape/category 返回 3D 几何体描述。
 * @param {object} character - elfCharacters 数组中的一项
 * @returns {{ type: string, params: object, scale: {x:number,y:number,z:number} }}
 */
export function getElf3DGeometry(character) {
  if (!character) {
    return {
      type: 'sphere',
      params: { radius: 1 },
      scale: { x: 1, y: 1, z: 1 }
    }
  }

  const shape = character.shape
  const category = character.category
  const s = character.scale?.body || 1

  // humanoid 形状 或 zodiac/myth(legend) 类别 → 人形
  if (shape === 'humanoid' || category === 'zodiac' || category === 'myth' || category === 'legend') {
    return {
      type: 'humanoid',
      params: { bodyHeight: 1.2, headRadius: 0.5 },
      scale: { x: s, y: s, z: s }
    }
  }

  switch (shape) {
    case 'sphere':
      return {
        type: 'sphere',
        params: { radius: 1 },
        scale: { x: s, y: s, z: s }
      }
    case 'cube':
      return {
        type: 'box',
        params: { width: 1.2, height: 1.2, depth: 1.2 },
        scale: { x: s, y: s, z: s }
      }
    case 'star':
      return {
        type: 'extrude',
        params: { shape: 'star', depth: 0.4 },
        scale: { x: s, y: s, z: s }
      }
    case 'heart':
      return {
        type: 'extrude',
        params: { shape: 'heart', depth: 0.3 },
        scale: { x: s, y: s, z: s }
      }
    case 'diamond':
      return {
        type: 'octahedron',
        params: { radius: 1 },
        scale: { x: s, y: s, z: s }
      }
    default:
      return {
        type: 'sphere',
        params: { radius: 1 },
        scale: { x: s, y: s, z: s }
      }
  }
}

// ─── 2. 材质映射 ───────────────────────────────────────────────

/**
 * 根据角色和皮肤返回材质参数。
 * @param {object} character - 角色对象
 * @param {object} [skin] - 皮肤对象（可选）
 * @returns {{ color: string, emissive: string, emissiveIntensity: number, roughness: number, metalness: number }}
 */
export function getElfMaterial(character, skin) {
  const baseColor = character?.baseColor || '#ffffff'

  // color 优先级：skin.color > skin.colors.body > character.baseColor
  let color = baseColor
  if (skin) {
    if (skin.color) {
      color = skin.color
    } else if (skin.colors && skin.colors.body) {
      color = skin.colors.body
    }
  }

  // emissive 优先级：skin.emissive > skin.colors.glow > '#000000'
  let emissive = '#000000'
  if (skin) {
    if (skin.emissive) {
      emissive = skin.emissive
    } else if (skin.colors && skin.colors.glow) {
      emissive = skin.colors.glow
    }
  }

  return {
    color,
    emissive,
    emissiveIntensity: emissive !== '#000000' ? 0.5 : 0,
    roughness: 0.5,
    metalness: 0.1
  }
}

// ─── 3. 服装饰品映射 ────────────────────────────────────────────

/**
 * 根据 outfit 返回挂饰数组。
 * @param {object} outfit - elfOutfits 数组中的一项
 * @returns {Array<{ type: string, position: {x:number,y:number,z:number}, geometry: {type:string, params:object}, color: string }>}
 */
export function getOutfitAccessories(outfit) {
  if (!outfit || outfit.id === 'none' || outfit.type === 'none') {
    return []
  }

  const color = outfit.color || '#cccccc'
  const accessories = []

  switch (outfit.id) {
    case 'hat':
      // 圆锥挂在头顶
      accessories.push({
        type: 'hat',
        position: { x: 0, y: 1.2, z: 0 },
        geometry: { type: 'cone', params: { radius: 0.3, height: 0.5 } },
        color
      })
      break

    case 'crown':
      // 小圆柱环挂在头顶
      accessories.push({
        type: 'crown',
        position: { x: 0, y: 1.1, z: 0 },
        geometry: { type: 'cylinder', params: { radiusTop: 0.25, radiusBottom: 0.3, height: 0.15 } },
        color
      })
      break

    case 'glasses':
      // 两个小圆柱挂在眼前
      accessories.push({
        type: 'glasses-left',
        position: { x: -0.25, y: 0.1, z: 0.8 },
        geometry: { type: 'cylinder', params: { radius: 0.12, height: 0.05 } },
        color
      })
      accessories.push({
        type: 'glasses-right',
        position: { x: 0.25, y: 0.1, z: 0.8 },
        geometry: { type: 'cylinder', params: { radius: 0.12, height: 0.05 } },
        color
      })
      break

    case 'wings':
      // 两个扁平盒子挂在背后
      accessories.push({
        type: 'wings-left',
        position: { x: -0.8, y: 0.3, z: -0.3 },
        geometry: { type: 'box', params: { width: 0.5, height: 0.05, depth: 0.6 } },
        color
      })
      accessories.push({
        type: 'wings-right',
        position: { x: 0.8, y: 0.3, z: -0.3 },
        geometry: { type: 'box', params: { width: 0.5, height: 0.05, depth: 0.6 } },
        color
      })
      break

    case 'scarf':
      // 圆环挂在脖子
      accessories.push({
        type: 'scarf',
        position: { x: 0, y: 0.6, z: 0 },
        geometry: { type: 'torus', params: { radius: 0.35, tube: 0.08 } },
        color
      })
      break

    default:
      // bow / coat / robe / armor 等暂不生成 3D 饰品
      break
  }

  return accessories
}

// ─── 4. 动作 → 3D 变换映射 ──────────────────────────────────────

/**
 * 根据动作名和进度(0~1)返回 3D 变换。
 * @param {string} action - 动作 id
 * @param {number} [progress=0] - 动画进度 0~1
 * @returns {{ position: {x:number,y:number,z:number}, rotation: {x:number,y:number,z:number}, scale: {x:number,y:number,z:number} }}
 */
export function getAction3DTransform(action, progress = 0) {
  const twoPi = Math.PI * 2
  const DEFAULT = {
    position: { x: 0, y: 0, z: 0 },
    rotation: { x: 0, y: 0, z: 0 },
    scale: { x: 1, y: 1, z: 1 }
  }

  switch (action) {
    case 'breath': {
      const pulse = 1.0 + 0.02 * Math.sin(progress * twoPi)
      return {
        position: { x: 0, y: 0, z: 0 },
        rotation: { x: 0, y: 0, z: 0 },
        scale: { x: pulse, y: pulse, z: pulse }
      }
    }

    case 'jump': {
      // 抛物线：y = 4 * p * (1-p) * 1.5
      const y = 4 * progress * (1 - progress) * 1.5
      return {
        position: { x: 0, y, z: 0 },
        rotation: { x: 0, y: 0, z: 0 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'walk':
    case 'run': {
      // 真实步态：每步双峰起伏（重心随支撑腿交替升降）+ 前倾 + 左右摇摆 + 轻微躯干扭动
      const step = progress * twoPi
      const bob = (Math.sin(step * 2) * 0.5 + 0.5) * 0.09
      return {
        position: { x: 0, y: bob, z: 0 },
        rotation: { x: 0.13, y: Math.sin(step) * 0.06, z: Math.sin(step) * 0.05 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'dance': {
      return {
        position: { x: 0, y: Math.abs(Math.sin(progress * twoPi)) * 0.3, z: 0 },
        rotation: { x: 0, y: Math.sin(progress * twoPi) * 0.3, z: 0 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'wave': {
      // 手臂 z 轴摆动，用 rotation.z 模拟
      return {
        position: { x: 0, y: 0, z: 0 },
        rotation: { x: 0, y: 0, z: Math.sin(progress * twoPi) * 0.3 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'sleep': {
      // 倾斜 + 轻微压扁
      return {
        position: { x: 0, y: -0.3, z: 0 },
        rotation: { x: 0, y: 0, z: 0.2 },
        scale: { x: 1, y: 0.85, z: 1 }
      }
    }

    case 'crouch': {
      return {
        position: { x: 0, y: -0.2, z: 0 },
        rotation: { x: 0, y: 0, z: 0 },
        scale: { x: 1, y: 0.7, z: 1 }
      }
    }

    case 'lie': {
      return {
        position: { x: 0, y: -0.4, z: 0 },
        rotation: { x: 0, y: 0, z: 1.2 },
        scale: { x: 1, y: 0.5, z: 1 }
      }
    }

    case 'sway': {
      return {
        position: { x: 0, y: 0, z: 0 },
        rotation: { x: 0, y: 0, z: Math.sin(progress * twoPi) * 0.05 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'sit': {
      return {
        position: { x: 0, y: -0.22, z: 0 },
        rotation: { x: 0, y: 0, z: 0 },
        scale: { x: 1.06, y: 0.74, z: 1 }
      }
    }

    case 'feed': {
      return {
        position: { x: 0, y: Math.sin(progress * twoPi * 2) * 0.02, z: 0.05 },
        rotation: { x: -0.12, y: 0, z: 0 },
        scale: { x: 1, y: 0.96, z: 1 }
      }
    }

    // ─── 游戏角色经典动作（V3 全集） ───
    case 'bow': {
      // 鞠躬：前倾 90° 起身回正 + 微蹲
      const p = Math.sin(progress * Math.PI)
      return {
        position: { x: 0, y: -0.15 * p, z: 0 },
        rotation: { x: p * 1.05, y: 0, z: 0 },
        scale: { x: 1, y: 1 - p * 0.08, z: 1 }
      }
    }

    case 'nod': {
      // 点头：头部连点两下（整体微倾 + 头部旋转由 limb 驱动）
      return {
        position: { x: 0, y: 0, z: 0 },
        rotation: { x: Math.sin(progress * twoPi * 2) * 0.12, y: 0, z: 0 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'shake-head': {
      // 摇头：身体伴随头部左右摆动
      return {
        position: { x: 0, y: 0, z: 0 },
        rotation: { x: 0, y: Math.sin(progress * twoPi * 3) * 0.16, z: 0 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'clap': {
      // 鼓掌：双臂胸前快速开合 + 开心蹦跳
      const s = Math.sin(progress * twoPi * 4)
      return {
        position: { x: 0, y: Math.abs(s) * 0.12, z: 0 },
        rotation: { x: 0, y: 0, z: 0 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'cheer': {
      // 欢呼：双臂上举交替 + 原地蹦跳
      const s = Math.sin(progress * twoPi * 2)
      return {
        position: { x: 0, y: Math.abs(s) * 0.35, z: 0 },
        rotation: { x: 0, y: Math.sin(progress * twoPi) * 0.22, z: 0 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'heart': {
      // 比心：双臂胸前合拢成心 + 歪头 + 踮脚
      const s = Math.sin(progress * twoPi * 2)
      return {
        position: { x: 0, y: 0.06 + Math.abs(s) * 0.05, z: 0 },
        rotation: { x: 0, y: 0, z: 0.1 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'kiss': {
      // 飞吻：先手贴嘴，再向前抛出
      const p = progress < 0.45 ? 0 : Math.sin(((progress - 0.45) / 0.55) * Math.PI)
      return {
        position: { x: 0, y: 0.04, z: p * 0.15 },
        rotation: { x: 0.06, y: 0, z: 0 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'salute': {
      // 敬礼：右手举额 + 身体挺直微昂头
      return {
        position: { x: 0, y: 0, z: 0 },
        rotation: { x: -0.06, y: 0, z: 0 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'stretch': {
      // 伸懒腰：双臂上举后仰 + 打哈欠 + 回落
      const p = Math.sin(progress * Math.PI)
      return {
        position: { x: 0, y: p * 0.18, z: 0 },
        rotation: { x: -p * 0.28, y: 0, z: 0 },
        scale: { x: 1, y: 1 + p * 0.06, z: 1 }
      }
    }

    case 'scared': {
      // 害怕：双手抱头 + 身体后缩 + 抖动 + 下蹲
      const shake = Math.sin(progress * twoPi * 6) * 0.045
      return {
        position: { x: 0, y: -0.12, z: -0.1 },
        rotation: { x: 0.12, y: 0, z: shake },
        scale: { x: 0.96, y: 0.94, z: 1 }
      }
    }

    case 'angry': {
      // 生气：身体前倾 + 跺脚（腿由 limb 驱动）+ 微抖
      return {
        position: { x: 0, y: Math.abs(Math.sin(progress * twoPi * 4)) * 0.06, z: 0 },
        rotation: { x: 0.2, y: 0, z: Math.sin(progress * twoPi * 4) * 0.03 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'cry': {
      // 伤心：低头缩肩 + 双手交替擦眼（由 limb 驱动）
      return {
        position: { x: 0, y: -0.05, z: 0 },
        rotation: { x: 0.14, y: 0, z: 0 },
        scale: { x: 0.96, y: 0.95, z: 1 }
      }
    }

    case 'spin': {
      // 转圈：原地整圈旋转 + 开心蹦跳（覆盖 autoRotate）
      return {
        position: { x: 0, y: Math.abs(Math.sin(progress * twoPi * 2)) * 0.2, z: 0 },
        rotation: { x: 0, y: progress * twoPi, z: 0 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'victory': {
      // 胜利：右拳上举 + 左臂叉腰 + 挺胸微跳
      const s = Math.sin(progress * twoPi * 2)
      return {
        position: { x: 0, y: Math.abs(s) * 0.18, z: 0 },
        rotation: { x: -0.1, y: 0, z: 0 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'run-fast': {
      // 快跑冲刺：大幅前倾 + 高频起伏
      const bob = Math.abs(Math.sin(progress * twoPi * 2)) * 0.16
      return {
        position: { x: 0, y: bob, z: 0 },
        rotation: { x: 0.34, y: 0, z: Math.sin(progress * twoPi * 2) * 0.1 },
        scale: { x: 1, y: 1, z: 1 }
      }
    }

    case 'pat': {
      const shiver = Math.sin(progress * twoPi * 3) * 0.02
      return {
        position: { x: 0, y: shiver, z: 0 },
        rotation: { x: 0, y: 0, z: shiver },
        scale: { x: 1, y: 1 + Math.sin(progress * twoPi * 3) * 0.015, z: 1 }
      }
    }

    case 'drag_follow': {
      return {
        position: { x: 0, y: 0, z: 0.05 },
        rotation: { x: 0.16, y: 0, z: Math.sin(progress * twoPi) * 0.06 },
        scale: { x: 1, y: 0.96, z: 1 }
      }
    }
    case 'blink':
      // 2D 层处理，3D 用缩放模拟——这里不做变换
      return { ...DEFAULT }

    case 'idle':
    case 'stand':
    case 'happy':
    case 'sad':
    case 'surprised':
    case 'thinking':
    default:
      return { ...DEFAULT }
  }
}

// ─── 5. 默认 3D 配置 ───────────────────────────────────────────

/**
 * 返回默认 3D 渲染配置。
 * @returns {{ mode: string, size: number, autoRotate: boolean, rotateSpeed: number, shadowEnabled: boolean, backgroundColor: number }}
 */
export function getDefault3DConfig() {
  return {
    mode: '3d',
    size: 1.0,
    autoRotate: true,
    rotateSpeed: 0.005,
    shadowEnabled: true,
    backgroundColor: 0x1a1a2e
  }
}

// ─── 6. 二次元卡通少女 3D 建模纯函数参数 ───────────────────────
// 这些函数只输出"颜色/部位描述/动作姿态"等纯数据，不依赖 three.js，
// 便于在 jsdom / node 环境独立测试。真正的几何体构建在 useElf3DRenderer 内完成。

/** 二次元少女兜底调色板（palette 缺字段时补齐） */
export const ANIME_3D_FALLBACK_PALETTE = Object.freeze({
  skin: '#ffe9dc',
  hair: '#ff9ec7',
  eyes: '#ff6f9c',
  eyeHighlight: '#ffffff',
  outfit: '#ffffff',
  outfitAccent: '#ff4d8f',
  ribbon: '#ff4d8f',
  blush: '#ff9dbb',
  brow: '#4a3a2f'
})

/**
 * 规范化二次元少女 3D 材质调色板：补齐缺省字段。
 * @param {object} palette - animeElfData 中的 palette
 * @returns {{ skin:string, hair:string, eyes:string, eyeHighlight:string, outfit:string, outfitAccent:string, ribbon:string, blush:string, brow:string }}
 */
export function getAnimeElf3DPalette(palette) {
  const p = palette && typeof palette === 'object' ? palette : {}
  return {
    skin: p.skin || ANIME_3D_FALLBACK_PALETTE.skin,
    hair: p.hair || ANIME_3D_FALLBACK_PALETTE.hair,
    eyes: p.eyes || ANIME_3D_FALLBACK_PALETTE.eyes,
    eyeHighlight: p.eyesHighlight || p.eyeHighlight || ANIME_3D_FALLBACK_PALETTE.eyeHighlight,
    outfit: p.outfit || ANIME_3D_FALLBACK_PALETTE.outfit,
    outfitAccent: p.outfitAccent || ANIME_3D_FALLBACK_PALETTE.outfitAccent,
    ribbon: p.ribbon || ANIME_3D_FALLBACK_PALETTE.ribbon,
    blush: p.blush || ANIME_3D_FALLBACK_PALETTE.blush,
    brow: p.brow || ANIME_3D_FALLBACK_PALETTE.brow
  }
}

/**
 * 根据发型返回需要构建的头发部位描述列表（纯数据）。
 * @param {string} hairstyle - twin/long/bob/ponytail/curly/short
 * @returns {string[]} 部位名数组
 */
export function getAnimeElfHairPartDescriptors(hairstyle) {
  switch (hairstyle) {
    case 'gufeng-bun':
      // 古风盘发：后发 + 刘海 + 发髻 + 两侧鬓发（青黛专属）
      return ['back', 'bangs', 'bun', 'side-l', 'side-r']
    case 'twin':
      return ['back', 'bangs', 'side-l', 'side-r', 'twin-l', 'twin-r']
    case 'long':
      return ['back', 'bangs', 'side-l', 'side-r']
    case 'ponytail':
      return ['back', 'bangs', 'ponytail']
    case 'curly':
      return ['back', 'bangs', 'curly-l', 'curly-r']
    case 'bob':
      return ['back', 'bangs', 'side-l', 'side-r']
    case 'short':
    default:
      return ['back', 'bangs']
  }
}

/**
 * 二次元少女四肢动作姿态（纯函数）。
 * 返回左右手臂/腿绕 z 轴的旋转角（弧度）与身体偏移，供渲染器驱动骨骼组。
 * @param {string} action - 动作 id
 * @param {number} [progress=0] - 动画进度 0~1
 * @returns {{ leftArmZ:number, rightArmZ:number, leftLegZ:number, rightLegZ:number, bodyRotZ:number, bodyY:number, headRotX?:number, headRotY?:number, headRotZ?:number }}
 */
export function getAnimeElfLimbAction(action, progress = 0) {
  const twoPi = Math.PI * 2
  const base = {
    leftArmZ: 0,
    rightArmZ: 0,
    leftLegZ: 0,
    rightLegZ: 0,
    bodyRotZ: 0,
    bodyY: 0
  }
  switch (action) {
    case 'wave':
      // 右臂上举并左右摆动
      return { ...base, rightArmZ: 2.2 + Math.sin(progress * twoPi) * 0.45 }
    case 'dance':
      return {
        ...base,
        leftArmZ: Math.sin(progress * twoPi) * 0.6,
        rightArmZ: -Math.sin(progress * twoPi) * 0.6,
        bodyRotZ: Math.sin(progress * twoPi) * 0.12,
        bodyY: Math.abs(Math.sin(progress * twoPi)) * 0.15
      }
    case 'happy':
    case 'jump':
      // 双臂上举 + 抛物线
      return { ...base, leftArmZ: 0.9, rightArmZ: 0.9, bodyY: 4 * progress * (1 - progress) * 0.3 }
    case 'walk':
    case 'run':
      // 真实步态：腿前后摆动幅度大于臂；臂自然小幅摆 + 身体随步微扭
      return {
        ...base,
        leftLegZ: Math.sin(progress * twoPi) * 0.45,
        rightLegZ: -Math.sin(progress * twoPi) * 0.45,
        leftArmZ: -Math.sin(progress * twoPi) * 0.26,
        rightArmZ: Math.sin(progress * twoPi) * 0.26,
        bodyRotZ: Math.sin(progress * twoPi) * 0.03
      }
    case 'feed':
      // 手送食物到嘴 + 咀嚼（双臂举到嘴前小幅晃动）
      return {
        ...base,
        leftArmZ: 1.1 + Math.sin(progress * twoPi * 2) * 0.1,
        rightArmZ: 1.1 - Math.sin(progress * twoPi * 2) * 0.1,
        bodyY: Math.sin(progress * twoPi * 2) * 0.02
      }
    case 'pat':
      // 被摸头：双臂轻抬到头顶附近，身体微颤
      return {
        ...base,
        leftArmZ: 0.5 + Math.sin(progress * twoPi * 3) * 0.08,
        rightArmZ: -0.5 - Math.sin(progress * twoPi * 3) * 0.08
      }
    case 'sit':
      // 坐下：腿部弯曲收起
      return { ...base, leftLegZ: -0.6, rightLegZ: -0.6, bodyY: -0.18 }
    case 'drag_follow':
      // 手臂张开保持平衡
      return { ...base, leftArmZ: 0.7, rightArmZ: -0.7, bodyRotZ: Math.sin(progress * twoPi) * 0.08 }
    case 'sleep':
      return { ...base, bodyRotZ: 0.2 }
    case 'idle':
    case 'stand':
    default:
      return base

    // ─── 游戏角色经典动作（V3 全集）四肢/头部姿态 ───
    case 'bow':
      // 鞠躬：双臂自然下垂并略后摆，腿并拢
      return { ...base, leftArmZ: 0.08, rightArmZ: -0.08, bodyY: -0.12 }

    case 'nod':
      // 点头：头部上下连点（headRotX 由渲染器应用）
      return { ...base, headRotX: Math.sin(progress * twoPi * 2) * 0.28, bodyY: -0.02 }

    case 'shake-head':
      // 摇头：头部左右摆动
      return { ...base, headRotY: Math.sin(progress * twoPi * 3) * 0.5, bodyY: 0.02 }

    case 'clap':
      // 鼓掌：双臂胸前快速开合
      return {
        ...base,
        leftArmZ: 1.0 + Math.sin(progress * twoPi * 4) * 0.4,
        rightArmZ: -1.0 - Math.sin(progress * twoPi * 4) * 0.4,
        headRotX: 0.06
      }

    case 'cheer':
      // 欢呼：双臂上举交替挥舞 + 身体微转
      return {
        ...base,
        leftArmZ: 2.3 + Math.sin(progress * twoPi * 2) * 0.45,
        rightArmZ: 2.3 - Math.sin(progress * twoPi * 2) * 0.45,
        bodyRotZ: Math.sin(progress * twoPi * 2) * 0.08,
        bodyY: Math.abs(Math.sin(progress * twoPi * 2)) * 0.1
      }

    case 'heart':
      // 比心：双臂弯到胸前合拢（心形）+ 歪头
      return {
        ...base,
        leftArmZ: 1.55 + Math.sin(progress * twoPi * 2) * 0.06,
        rightArmZ: -1.55 - Math.sin(progress * twoPi * 2) * 0.06,
        leftForearmZ: 0.55,
        rightForearmZ: -0.55,
        headRotZ: 0.12
      }

    case 'kiss':
      // 飞吻：先右手贴嘴（前段），再向前抛出（后段）
      return progress < 0.45
        ? { ...base, rightArmZ: 2.45, rightForearmZ: 1.1, leftArmZ: 0.1, headRotZ: -0.1 }
        : { ...base, rightArmZ: 0.45, rightArmX: 0.6, rightForearmZ: 0.3, leftArmZ: 0.1, bodyY: 0.03 }

    case 'salute':
      // 敬礼：右手举到额侧 + 左手贴裤缝
      return { ...base, rightArmZ: 2.75, rightForearmZ: 0.12, leftArmZ: 0.06, headRotY: -0.08 }

    case 'stretch':
      // 伸懒腰：双臂上举过头外张 + 后仰 + 打哈欠低头
      return {
        ...base,
        leftArmZ: -2.55,
        rightArmZ: 2.55,
        leftForearmZ: -0.35,
        rightForearmZ: 0.35,
        headRotX: -0.12,
        bodyY: 0.16
      }

    case 'scared':
      // 害怕：双手抱头 + 身体后缩 + 发抖
      return {
        ...base,
        leftArmZ: 2.15,
        rightArmZ: -2.15,
        leftForearmZ: 1.35,
        rightForearmZ: -1.35,
        leftLegZ: 0.12,
        rightLegZ: -0.12,
        headRotX: 0.16,
        bodyRotZ: Math.sin(progress * twoPi * 6) * 0.03
      }

    case 'angry':
      // 生气：双手叉腰 + 跺脚 + 低头瞪眼
      return {
        ...base,
        leftArmZ: 1.15,
        rightArmZ: -1.15,
        leftForearmZ: 0.5,
        rightForearmZ: -0.5,
        leftLegZ: Math.sin(progress * twoPi * 4) * 0.22,
        rightLegZ: -Math.sin(progress * twoPi * 4) * 0.22,
        headRotX: -0.18
      }

    case 'cry':
      // 伤心：低头 + 双手交替擦眼
      return {
        ...base,
        leftArmZ: 2.0 + Math.sin(progress * twoPi * 3) * 0.18,
        rightArmZ: -2.0 - Math.sin(progress * twoPi * 3) * 0.18,
        leftForearmZ: 1.0,
        rightForearmZ: -1.0,
        headRotX: 0.22
      }

    case 'spin':
      // 转圈：双臂平举平衡 + 蹦跳
      return {
        ...base,
        leftArmZ: 1.0,
        rightArmZ: -1.0,
        bodyY: Math.abs(Math.sin(progress * twoPi * 2)) * 0.12
      }

    case 'victory':
      // 胜利：右拳上举 + 左臂叉腰 + 挺胸
      return { ...base, rightArmZ: 2.8, leftArmZ: -1.05, bodyY: 0.08, headRotX: -0.1 }

    case 'run-fast':
      // 快跑：高频大幅摆臂摆腿
      return {
        ...base,
        leftLegZ: Math.sin(progress * twoPi * 2) * 0.65,
        rightLegZ: -Math.sin(progress * twoPi * 2) * 0.65,
        leftArmZ: -Math.sin(progress * twoPi * 2) * 0.55,
        rightArmZ: Math.sin(progress * twoPi * 2) * 0.55,
        bodyRotZ: Math.sin(progress * twoPi * 2) * 0.12,
        bodyY: Math.abs(Math.sin(progress * twoPi * 2)) * 0.12
      }
  }
}

// --- 8. market upgrade: action / environment pure functions (node-testable) ---
export function getWalkPhase(step, frequency = 2.4) {
  const t = (step || 0) * (frequency || 2.4)
  const phase = t * Math.PI
  return {
    leftLeg: Math.sin(phase) * 0.45,
    rightLeg: -Math.sin(phase) * 0.45,
    bodyBob: Math.abs(Math.sin(phase)) * 0.09,
    armSwing: -Math.sin(phase) * 0.35
  }
}

export function getJumpCurve(progress = 0) {
  const p = Math.max(0, Math.min(1, progress))
  const positionY = 4 * p * (1 - p) * 0.55
  let scaleY
  if (p < 0.15) {
    scaleY = 1 - p * 0.9
  } else if (p < 0.5) {
    scaleY = 1 + (p - 0.15) * 0.35
  } else {
    const t = (p - 0.5) / 0.5
    scaleY = 1.15 - t * 0.25 + Math.sin(t * Math.PI) * 0.03
  }
  return { positionY, scaleY, squash: p >= 0.85 ? Math.sin(((p - 0.85) / 0.15) * Math.PI) * 0.12 : 0 }
}

export function getIdleBreath(time = 0, period = 1.3) {
  const phase = (time || 0) / (period || 1.3) * Math.PI * 2
  const b = Math.sin(phase)
  return { scaleY: 1 + b * 0.02, translateY: -b * 0.02 }
}

export function getTimePeriod(hour) {
  const h = Number.isFinite(hour) ? hour : new Date().getHours()
  if (h >= 8 && h < 18) return 'day'
  if (h >= 18 && h < 22) return 'evening'
  if (h >= 22 || h < 6) return 'night'
  return 'morning'
}

export function getInactivityLevel(idleMinutes) {
  const m = Math.max(0, idleMinutes || 0)
  if (m >= 30) return 2
  if (m >= 10) return 1
  return 0
}

export function isComboClick(count, required = 3) {
  return (count || 0) >= required
}

export function isHeadTap(offsetY, containerHeight) {
  if (!containerHeight || containerHeight <= 0) return false
  return offsetY / containerHeight < 0.45
}

export const ELF_ACCESSORY_SLOTS = Object.freeze({
  hat: [
    { id: 'none', name: '无' },
    { id: 'beret', name: '贝雷帽', color: '#d94f6a' },
    { id: 'cap', name: '棒球帽', color: '#4f86c6' },
    { id: 'crown', name: '小皇冠', color: '#f2c14e' },
    { id: 'bow', name: '蝴蝶结发箍', color: '#ff9ec7' }
  ],
  glasses: [
    { id: 'none', name: '无' },
    { id: 'round', name: '圆框眼镜', color: '#3a3345' },
    { id: 'shades', name: '墨镜', color: '#222222' },
    { id: 'star', name: '星星眼镜', color: '#f2c14e' }
  ],
  bag: [
    { id: 'none', name: '无' },
    { id: 'backpack', name: '小书包', color: '#7fb069' },
    { id: 'tote', name: '手提袋', color: '#e0a458' },
    { id: 'crossbody', name: '斜挎包', color: '#b56576' }
  ]
})