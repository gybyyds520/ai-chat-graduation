/**
 * elf3dConfig.test.js — elf3dConfig 纯函数模块单元测试
 *
 * 测试角色配置 → 3D 构建参数的映射逻辑，不依赖 three.js / DOM。
 */

import {
  getElf3DGeometry,
  getElfMaterial,
  getOutfitAccessories,
  getAction3DTransform,
  getAnimeElfLimbAction,
  getDefault3DConfig
} from '../../utils/elf3dConfig'

// ─── 测试用 mock 数据 ───

const mockSphereCharacter = {
  id: 'ball',
  name: '球球',
  category: 'basic',
  baseColor: '#4ade80',
  shape: 'sphere',
  scale: { body: 0.8, head: 0.3, eyes: 0.12 }
}

const mockCubeCharacter = {
  id: 'cube',
  name: '方块',
  category: 'basic',
  baseColor: '#60a5fa',
  shape: 'cube',
  scale: { body: 0.7, head: 0.25, eyes: 0.1 }
}

const mockStarCharacter = {
  id: 'star',
  name: '星星',
  category: 'basic',
  baseColor: '#fbbf24',
  shape: 'star',
  scale: { body: 0.85, head: 0.28, eyes: 0.11 }
}

const mockDiamondCharacter = {
  id: 'diamond',
  name: '钻石',
  category: 'basic',
  baseColor: '#a78bfa',
  shape: 'diamond',
  scale: { body: 0.75, head: 0.24, eyes: 0.09 }
}

const mockZodiacCharacter = {
  id: 'zodiac-rat',
  name: '鼠小灵',
  category: 'zodiac',
  baseColor: '#8b7355',
  shape: 'humanoid',
  scale: { body: 0.6, head: 0.35, eyes: 0.15 }
}

const mockMythCharacter = {
  id: 'myth-qilin',
  name: '麒麟瑞',
  category: 'myth',
  baseColor: '#ffd700',
  shape: 'humanoid',
  scale: { body: 0.85, head: 0.33, eyes: 0.11 }
}

const mockSkinWithColor = {
  id: 'custom',
  name: '自定义',
  color: '#ff0000',
  emissive: '#00ff00',
  colors: { body: null, glow: null }
}

const mockSkinWithColorsObj = {
  id: 'neon',
  name: '霓虹',
  colors: { body: null, eyes: '#00ffff', glow: '#00ffff', highlight: '#ffffff' }
}

const mockOutfitHat = {
  id: 'hat',
  name: '帽子',
  type: 'accessory',
  color: '#f59e0b'
}

const mockOutfitNone = {
  id: 'none',
  name: '无',
  type: 'none',
  color: null
}

// ─── 测试用例 ───

describe('elf3dConfig 精灵 3D 配置纯函数', () => {

  // ─── getElf3DGeometry ───

  describe('getElf3DGeometry 几何体映射', () => {

    it('1. shape=sphere 返回 sphere 类型', () => {
      const result = getElf3DGeometry(mockSphereCharacter)
      expect(result.type).toBe('sphere')
      expect(result.params.radius).toBe(1)
    })

    it('2. shape=cube 返回 box 类型', () => {
      const result = getElf3DGeometry(mockCubeCharacter)
      expect(result.type).toBe('box')
      expect(result.params.width).toBe(1.2)
      expect(result.params.height).toBe(1.2)
      expect(result.params.depth).toBe(1.2)
    })

    it('3. shape=star 返回 extrude 类型', () => {
      const result = getElf3DGeometry(mockStarCharacter)
      expect(result.type).toBe('extrude')
      expect(result.params.shape).toBe('star')
      expect(result.params.depth).toBe(0.4)
    })

    it('4. shape=heart 返回 extrude 类型', () => {
      const heartChar = { ...mockSphereCharacter, shape: 'heart' }
      const result = getElf3DGeometry(heartChar)
      expect(result.type).toBe('extrude')
      expect(result.params.shape).toBe('heart')
      expect(result.params.depth).toBe(0.3)
    })

    it('5. shape=diamond 返回 octahedron 类型', () => {
      const result = getElf3DGeometry(mockDiamondCharacter)
      expect(result.type).toBe('octahedron')
      expect(result.params.radius).toBe(1)
    })

    it('6. shape=humanoid 返回 humanoid 类型', () => {
      const result = getElf3DGeometry(mockZodiacCharacter)
      expect(result.type).toBe('humanoid')
      expect(result.params.bodyHeight).toBe(1.2)
      expect(result.params.headRadius).toBe(0.5)
    })

    it('7. category=myth 即使 shape 非 humanoid 也返回 humanoid', () => {
      // 构造一个 myth 类别但 shape=sphere 的边缘 case
      const mythSphere = { ...mockMythCharacter, shape: 'sphere' }
      const result = getElf3DGeometry(mythSphere)
      expect(result.type).toBe('humanoid')
    })

    it('8. character 为 null 时回退 sphere', () => {
      const result = getElf3DGeometry(null)
      expect(result.type).toBe('sphere')
      expect(result.params.radius).toBe(1)
    })

    it('9. scale.body 正确传递到结果 scale', () => {
      const result = getElf3DGeometry(mockSphereCharacter)
      expect(result.scale.x).toBe(0.8)
      expect(result.scale.y).toBe(0.8)
      expect(result.scale.z).toBe(0.8)
    })
  })

  // ─── getElfMaterial ───

  describe('getElfMaterial 材质映射', () => {

    it('10. 无 skin 时使用 character.baseColor', () => {
      const result = getElfMaterial(mockSphereCharacter)
      expect(result.color).toBe('#4ade80')
      expect(result.emissive).toBe('#000000')
      expect(result.emissiveIntensity).toBe(0)
    })

    it('11. 有 skin 且 skin.color 存在时使用 skin.color', () => {
      const result = getElfMaterial(mockSphereCharacter, mockSkinWithColor)
      expect(result.color).toBe('#ff0000')
      expect(result.emissive).toBe('#00ff00')
      expect(result.emissiveIntensity).toBeGreaterThan(0)
    })

    it('12. 有 skin 且 skin.colors.glow 存在时使用 glow 作为 emissive', () => {
      const result = getElfMaterial(mockSphereCharacter, mockSkinWithColorsObj)
      expect(result.emissive).toBe('#00ffff')
      expect(result.emissiveIntensity).toBe(0.5)
    })

    it('13. roughness 默认 0.5, metalness 默认 0.1', () => {
      const result = getElfMaterial(mockSphereCharacter)
      expect(result.roughness).toBe(0.5)
      expect(result.metalness).toBe(0.1)
    })
  })

  // ─── getOutfitAccessories ───

  describe('getOutfitAccessories 服装饰品', () => {

    it('14. outfit.id=hat 返回头顶挂饰', () => {
      const result = getOutfitAccessories(mockOutfitHat)
      expect(result.length).toBe(1)
      expect(result[0].type).toBe('hat')
      expect(result[0].position.y).toBeGreaterThan(0)
      expect(result[0].geometry.type).toBe('cone')
      expect(result[0].color).toBe('#f59e0b')
    })

    it('15. outfit.id=none 返回空数组', () => {
      const result = getOutfitAccessories(mockOutfitNone)
      expect(result).toEqual([])
    })

    it('16. outfit.id=crown 返回头顶圆柱环', () => {
      const crown = { id: 'crown', name: '皇冠', type: 'accessory', color: '#fbbf24' }
      const result = getOutfitAccessories(crown)
      expect(result.length).toBe(1)
      expect(result[0].type).toBe('crown')
      expect(result[0].geometry.type).toBe('cylinder')
    })

    it('17. outfit.id=glasses 返回两个挂饰', () => {
      const glasses = { id: 'glasses', name: '眼镜', type: 'accessory', color: '#1f2937' }
      const result = getOutfitAccessories(glasses)
      expect(result.length).toBe(2)
      expect(result[0].type).toBe('glasses-left')
      expect(result[1].type).toBe('glasses-right')
    })

    it('18. outfit.id=wings 返回两个翅膀挂饰', () => {
      const wings = { id: 'wings', name: '翅膀', type: 'accessory', color: '#e0f2fe' }
      const result = getOutfitAccessories(wings)
      expect(result.length).toBe(2)
      expect(result[0].geometry.type).toBe('box')
      expect(result[1].geometry.type).toBe('box')
    })

    it('19. outfit.id=scarf 返回圆环挂饰', () => {
      const scarf = { id: 'scarf', name: '围巾', type: 'clothing', color: '#ef4444' }
      const result = getOutfitAccessories(scarf)
      expect(result.length).toBe(1)
      expect(result[0].type).toBe('scarf')
      expect(result[0].geometry.type).toBe('torus')
    })

    it('20. outfit 为 null 返回空数组', () => {
      const result = getOutfitAccessories(null)
      expect(result).toEqual([])
    })
  })

  // ─── getAction3DTransform ───

  describe('getAction3DTransform 动作变换', () => {

    it('21. jump 在 progress=0.5 时 y 最高', () => {
      const mid = getAction3DTransform('jump', 0.5)
      const start = getAction3DTransform('jump', 0)
      const end = getAction3DTransform('jump', 1)
      expect(mid.position.y).toBeGreaterThan(start.position.y)
      expect(mid.position.y).toBeGreaterThan(end.position.y)
      expect(start.position.y).toBe(0)
      expect(end.position.y).toBe(0)
    })

    it('22. breath scale 有轻微脉冲', () => {
      const t0 = getAction3DTransform('breath', 0)
      const t05 = getAction3DTransform('breath', 0.25)
      expect(t0.scale.x).toBeCloseTo(1.0, 2)
      // 0.25 * 2π = π/2, sin(π/2) = 1, pulse = 1.0 + 0.02 = 1.02
      expect(t05.scale.x).toBeCloseTo(1.02, 2)
    })

    it('23. dance rotation.y 随 progress 变化', () => {
      const t0 = getAction3DTransform('dance', 0)
      const t025 = getAction3DTransform('dance', 0.25)
      expect(t0.rotation.y).toBeCloseTo(0, 2)
      expect(t025.rotation.y).toBeCloseTo(0.3, 2)
    })

    it('24. sleep 返回倾斜 + 压扁', () => {
      const result = getAction3DTransform('sleep')
      expect(result.rotation.z).toBe(0.2)
      expect(result.scale.y).toBe(0.85)
    })

    it('25. crouch 返回 scale.y = 0.7', () => {
      const result = getAction3DTransform('crouch')
      expect(result.scale.y).toBe(0.7)
    })

    it('26. lie 返回 rotation.z = 1.2 + scale.y = 0.5', () => {
      const result = getAction3DTransform('lie')
      expect(result.rotation.z).toBe(1.2)
      expect(result.scale.y).toBe(0.5)
    })

    it('27. sway rotation.z 在 ±0.05 之间', () => {
      const t0 = getAction3DTransform('sway', 0)
      const t025 = getAction3DTransform('sway', 0.25)
      expect(t0.rotation.z).toBeCloseTo(0, 2)
      expect(t025.rotation.z).toBeCloseTo(0.05, 2)
    })

    it('28. idle 返回默认变换（全 0 / 全 1）', () => {
      const result = getAction3DTransform('idle')
      expect(result.position).toEqual({ x: 0, y: 0, z: 0 })
      expect(result.rotation).toEqual({ x: 0, y: 0, z: 0 })
      expect(result.scale).toEqual({ x: 1, y: 1, z: 1 })
    })

    it('29. wave rotation.z 随 progress 正弦变化', () => {
      const t0 = getAction3DTransform('wave', 0)
      const t025 = getAction3DTransform('wave', 0.25)
      expect(t0.rotation.z).toBeCloseTo(0, 2)
      expect(t025.rotation.z).toBeCloseTo(0.3, 2)
    })

    // ─── 游戏角色经典动作（V3 全集）───
    it('30. bow 鞠躬在 progress=0.5 时前倾最大且微蹲', () => {
      const mid = getAction3DTransform('bow', 0.5)
      const start = getAction3DTransform('bow', 0)
      const end = getAction3DTransform('bow', 1)
      expect(mid.rotation.x).toBeGreaterThan(start.rotation.x)
      expect(mid.rotation.x).toBeGreaterThan(end.rotation.x)
      expect(mid.rotation.x).toBeCloseTo(1.05, 2)
      expect(mid.position.y).toBeCloseTo(-0.15, 2)
    })

    it('31. nod 点头头部连点两次（rotation.x 正弦）', () => {
      const t0 = getAction3DTransform('nod', 0)
      const t0125 = getAction3DTransform('nod', 0.125) // sin(π/2)=1
      expect(t0.rotation.x).toBeCloseTo(0, 2)
      expect(t0125.rotation.x).toBeCloseTo(0.12, 2)
    })

    it('32. shake-head 摇头 rotation.y 左右摆动', () => {
      const t0 = getAction3DTransform('shake-head', 0)
      const t025 = getAction3DTransform('shake-head', 0.25)
      expect(t0.rotation.y).toBeCloseTo(0, 2)
      expect(Math.abs(t025.rotation.y)).toBeGreaterThan(0.1)
    })

    it('33. clap 鼓掌伴随上下蹦跳', () => {
      const t0 = getAction3DTransform('clap', 0)
      const t0625 = getAction3DTransform('clap', 0.0625) // sin(π/2)
      expect(t0.position.y).toBeCloseTo(0, 2)
      expect(t0625.position.y).toBeGreaterThan(0.05)
    })

    it('34. cheer 欢呼 position.y 蹦跳 + rotation.y 摆动', () => {
      const t0 = getAction3DTransform('cheer', 0)
      const t0125 = getAction3DTransform('cheer', 0.125) // |sin(π/2)|=1
      expect(t0.position.y).toBeCloseTo(0, 2)
      expect(t0125.position.y).toBeGreaterThan(0.2)
      expect(Math.abs(t0125.rotation.y)).toBeGreaterThan(0.1)
    })

    it('35. heart 比心歪头（rotation.z=0.1）且踮脚', () => {
      const t0 = getAction3DTransform('heart', 0)
      expect(t0.rotation.z).toBeCloseTo(0.1, 2)
      expect(t0.position.y).toBeGreaterThan(0.05)
    })

    it('36. kiss 飞吻前段贴嘴后段前伸（position.z 后段增大）', () => {
      const front = getAction3DTransform('kiss', 0.2)
      const back = getAction3DTransform('kiss', 0.8)
      expect(front.position.z).toBeCloseTo(0, 2)
      expect(back.position.z).toBeGreaterThan(0.1)
    })

    it('37. salute 敬礼身体微昂（rotation.x 轻微负值）', () => {
      const t = getAction3DTransform('salute')
      expect(t.rotation.x).toBeCloseTo(-0.06, 2)
    })

    it('38. stretch 伸懒腰 mid 上提且后仰', () => {
      const mid = getAction3DTransform('stretch', 0.5)
      expect(mid.position.y).toBeGreaterThan(0.1)
      expect(mid.rotation.x).toBeLessThan(0)
    })

    it('39. scared 害怕下蹲后缩发抖', () => {
      const t = getAction3DTransform('scared')
      expect(t.position.y).toBeCloseTo(-0.12, 2)
      expect(t.position.z).toBeCloseTo(-0.1, 2)
      expect(t.scale.y).toBeCloseTo(0.94, 2)
    })

    it('40. angry 生气前倾 + 跺脚起伏', () => {
      const t = getAction3DTransform('angry')
      expect(t.rotation.x).toBeCloseTo(0.2, 2)
      expect(t.position.y).toBeGreaterThanOrEqual(0)
    })

    it('41. cry 伤心低头缩肩', () => {
      const t = getAction3DTransform('cry')
      expect(t.rotation.x).toBeCloseTo(0.14, 2)
      expect(t.scale.y).toBeCloseTo(0.95, 2)
    })

    it('42. spin 转圈 rotation.y 走满一整圈', () => {
      const t0 = getAction3DTransform('spin', 0)
      const t1 = getAction3DTransform('spin', 1)
      expect(t0.rotation.y).toBeCloseTo(0, 2)
      expect(t1.rotation.y).toBeCloseTo(Math.PI * 2, 2)
    })

    it('43. victory 胜利右拳上举微跳', () => {
      const t0 = getAction3DTransform('victory', 0)
      const t0625 = getAction3DTransform('victory', 0.0625)
      expect(t0.rotation.x).toBeCloseTo(-0.1, 2)
      expect(t0625.position.y).toBeGreaterThan(0.05)
    })

    it('44. run-fast 快跑前倾更大且高频起伏', () => {
      const t = getAction3DTransform('run-fast', 0.125)
      expect(t.rotation.x).toBeCloseTo(0.34, 2)
      expect(t.position.y).toBeGreaterThan(0.05)
    })

    it('44b. walk 真实步态：前倾 + 双峰起伏 + 躯干扭动', () => {
      // progress=0.25 → step=π/2：双峰起伏峰点（bob=(sin(π)*0.5+0.5)*0.09=0.045）
      const t = getAction3DTransform('walk', 0.25)
      expect(t.rotation.x).toBeCloseTo(0.13, 2)         // 走路前倾
      expect(t.position.y).toBeCloseTo(0.045, 2)        // 每步双峰起伏
      expect(t.rotation.y).toBeCloseTo(0.06, 2)         // 躯干随步扭动
      // 步态周期对称：0 与 0.5 相位旋转相反
      const t0 = getAction3DTransform('walk', 0)
      const t05 = getAction3DTransform('walk', 0.5)
      expect(t0.rotation.y).toBeCloseTo(0, 2)
      expect(t05.rotation.y).toBeCloseTo(0, 2)
    })

    it('45. 未知动作回退默认变换', () => {
      const result = getAction3DTransform('not-exist-action', 0.5)
      expect(result.position).toEqual({ x: 0, y: 0, z: 0 })
      expect(result.scale).toEqual({ x: 1, y: 1, z: 1 })
    })
  })

  // ─── getAnimeElfLimbAction 四肢/头部姿态 ───

  describe('getAnimeElfLimbAction 四肢与头部姿态', () => {

    it('46. 未知动作回退 base（全 0）', () => {
      const limb = getAnimeElfLimbAction('not-exist', 0.5)
      expect(limb.leftArmZ).toBe(0)
      expect(limb.rightArmZ).toBe(0)
      expect(limb.bodyRotZ).toBe(0)
      expect(limb.headRotX).toBeUndefined()
    })

    it('47. cheer 欢呼双臂上举（|z|>2）', () => {
      const limb = getAnimeElfLimbAction('cheer', 0)
      expect(Math.abs(limb.leftArmZ)).toBeGreaterThan(2)
      expect(Math.abs(limb.rightArmZ)).toBeGreaterThan(2)
    })

    it('48. salute 敬礼右臂高举、左臂贴裤缝', () => {
      const limb = getAnimeElfLimbAction('salute', 0)
      expect(limb.rightArmZ).toBeGreaterThan(2.5)
      expect(limb.leftArmZ).toBeLessThan(0.1)
    })

    it('49. nod 点头带 headRotX 头部俯仰', () => {
      const limb = getAnimeElfLimbAction('nod', 0.125) // sin(π/2)=1
      expect(limb.headRotX).toBeGreaterThan(0.2)
    })

    it('50. kiss 前段贴嘴（rightArmZ 大 + 前臂弯）后段抛出（rightArmZ 小）', () => {
      const front = getAnimeElfLimbAction('kiss', 0.2)
      const back = getAnimeElfLimbAction('kiss', 0.7)
      expect(front.rightArmZ).toBeGreaterThan(2)
      expect(front.rightForearmZ).toBeGreaterThan(1)
      expect(back.rightArmZ).toBeLessThan(1)
      expect(back.rightArmX).toBeGreaterThan(0.5)
    })

    it('51. scared 害怕双手抱头（前臂大弯）', () => {
      const limb = getAnimeElfLimbAction('scared', 0)
      expect(Math.abs(limb.leftArmZ)).toBeGreaterThan(2)
      expect(limb.leftForearmZ).toBeGreaterThan(1)
      expect(Math.abs(limb.rightForearmZ)).toBeGreaterThan(1)
    })

    it('52. run-fast 快跑高频摆臂摆腿', () => {
      const limb = getAnimeElfLimbAction('run-fast', 0.125)
      expect(Math.abs(limb.leftLegZ)).toBeGreaterThan(0.5)
      expect(Math.abs(limb.rightLegZ)).toBeGreaterThan(0.5)
      expect(Math.abs(limb.leftArmZ)).toBeGreaterThan(0.4)
    })

    it('53. walk 步态摆臂（前臂带肘弯）', () => {
      const limb = getAnimeElfLimbAction('walk', 0.25)
      expect(Math.abs(limb.leftArmZ)).toBeGreaterThan(0)
    })
  })

  // ─── getDefault3DConfig ───

  describe('getDefault3DConfig 默认配置', () => {

    it('30. mode 默认 3d', () => {
      const config = getDefault3DConfig()
      expect(config.mode).toBe('3d')
    })

    it('31. autoRotate 默认 true', () => {
      const config = getDefault3DConfig()
      expect(config.autoRotate).toBe(true)
    })

    it('32. 包含所有必需字段', () => {
      const config = getDefault3DConfig()
      expect(config).toHaveProperty('size')
      expect(config).toHaveProperty('rotateSpeed')
      expect(config).toHaveProperty('shadowEnabled')
      expect(config).toHaveProperty('backgroundColor')
      expect(config.rotateSpeed).toBe(0.005)
      expect(config.shadowEnabled).toBe(true)
      expect(config.backgroundColor).toBe(0x1a1a2e)
    })
  })
})
