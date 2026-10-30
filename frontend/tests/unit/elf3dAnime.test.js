import {
  getAnimeElf3DPalette,
  getAnimeElfHairPartDescriptors,
  getAnimeElfLimbAction,
  getAction3DTransform,
  normalizeGaze,
  getIdleStage,
  ANIME_3D_FALLBACK_PALETTE
} from '../../src/utils/elf3dConfig'
import { animeElfCharacters } from '../../src/data/animeElfData'

describe('elf3dConfig 二次元少女 3D 纯函数', () => {
  describe('getAnimeElf3DPalette 调色板规范化', () => {
    it('完整 palette 原样透传', () => {
      const p = animeElfCharacters[0].palette
      const out = getAnimeElf3DPalette(p)
      expect(out.skin).toBe(p.skin)
      expect(out.hair).toBe(p.hair)
      expect(out.eyes).toBe(p.eyes)
      expect(out.outfit).toBe(p.outfit)
      expect(out.ribbon).toBe(p.ribbon)
      expect(out.blush).toBe(p.blush)
    })
    it('空/缺省 palette 用兜底值补齐', () => {
      const out = getAnimeElf3DPalette({})
      expect(out.skin).toBe(ANIME_3D_FALLBACK_PALETTE.skin)
      expect(out.hair).toBe(ANIME_3D_FALLBACK_PALETTE.hair)
      expect(out.eyes).toBe(ANIME_3D_FALLBACK_PALETTE.eyes)
    })
    it('null/undefined 不崩溃', () => {
      const out = getAnimeElf3DPalette(null)
      expect(out.hair).toBe(ANIME_3D_FALLBACK_PALETTE.hair)
    })
    it('eyesHighlight 优先取 eyesHighlight 字段', () => {
      const out = getAnimeElf3DPalette({ eyesHighlight: '#abcdef' })
      expect(out.eyeHighlight).toBe('#abcdef')
    })
  })

  describe('getAnimeElfHairPartDescriptors 发型部位', () => {
    it('双马尾含左右马尾', () => {
      const parts = getAnimeElfHairPartDescriptors('twin')
      expect(parts).toContain('twin-l')
      expect(parts).toContain('twin-r')
      expect(parts).toContain('back')
      expect(parts).toContain('bangs')
    })
    it('长发含两侧鬓发', () => {
      const parts = getAnimeElfHairPartDescriptors('long')
      expect(parts).toContain('side-l')
      expect(parts).toContain('side-r')
    })
    it('高马尾含 ponytail', () => {
      expect(getAnimeElfHairPartDescriptors('ponytail')).toContain('ponytail')
    })
    it('卷发含 curly-l/r', () => {
      const parts = getAnimeElfHairPartDescriptors('curly')
      expect(parts).toContain('curly-l')
      expect(parts).toContain('curly-r')
    })
    it('短发/未知只含 back+bangs', () => {
      expect(getAnimeElfHairPartDescriptors('short')).toEqual(['back', 'bangs'])
      expect(getAnimeElfHairPartDescriptors('xxx')).toEqual(['back', 'bangs'])
    })
    it('古风盘发 gufeng-bun 含发髻与两侧鬓发', () => {
      const parts = getAnimeElfHairPartDescriptors('gufeng-bun')
      expect(parts).toContain('bun')
      expect(parts).toContain('side-l')
      expect(parts).toContain('side-r')
      expect(parts).toContain('back')
      expect(parts).toContain('bangs')
    })
    it('全部 7 个角色发型都能映射到非空部位列表', () => {
      for (const c of animeElfCharacters) {
        const parts = getAnimeElfHairPartDescriptors(c.hairstyle)
        expect(parts.length).toBeGreaterThan(0)
        expect(parts).toContain('back')
        expect(parts).toContain('bangs')
      }
    })
  })

  describe('getAnimeElfLimbAction 四肢动作姿态', () => {
    it('wave：右臂上举并随进度摆动', () => {
      const a = getAnimeElfLimbAction('wave', 0)
      expect(a.rightArmZ).toBeGreaterThan(1.5)
      expect(a.rightArmZ).toBeLessThan(2.7)
    })
    it('dance：双臂反向摆动 + 身体起伏', () => {
      const a = getAnimeElfLimbAction('dance', 0.25)
      expect(a.leftArmZ).not.toBeCloseTo(a.rightArmZ, 1)
      expect(a.bodyY).toBeGreaterThan(0)
    })
    it('jump/happy：双臂上举', () => {
      const a = getAnimeElfLimbAction('jump', 0.5)
      expect(a.leftArmZ).toBeGreaterThan(0.5)
      expect(a.rightArmZ).toBeGreaterThan(0.5)
      expect(a.bodyY).toBeCloseTo(0.3, 5) // 抛物线顶点
    })
    it('walk：左右腿反向摆动', () => {
      const a = getAnimeElfLimbAction('walk', 0.25)
      expect(a.leftLegZ * a.rightLegZ).toBeLessThan(0) // 一正一负
    })
    it('idle/未知：四肢归零', () => {
      const a = getAnimeElfLimbAction('idle', 0.5)
      expect(a.leftArmZ).toBe(0)
      expect(a.rightArmZ).toBe(0)
      expect(a.leftLegZ).toBe(0)
      expect(a.rightLegZ).toBe(0)
    })
    it('返回值包含全部 6 个字段', () => {
      const a = getAnimeElfLimbAction('dance', 0.1)
      expect(a).toHaveProperty('leftArmZ')
      expect(a).toHaveProperty('rightArmZ')
      expect(a).toHaveProperty('leftLegZ')
      expect(a).toHaveProperty('rightLegZ')
      expect(a).toHaveProperty('bodyRotZ')
      expect(a).toHaveProperty('bodyY')
    })
  })

  describe('getAction3DTransform 动作变换（含走路步态）', () => {
    it('walk：身体前倾 + 随进度起伏', () => {
      const a = getAction3DTransform('walk', 0.25)
      expect(a.rotation.x).toBeGreaterThan(0) // 前倾
      expect(a.position.y).toBeGreaterThan(0) // 上起伏
      const b = getAction3DTransform('walk', 0.75)
      expect(b.position.y).toBeGreaterThan(0)
    })
    it('walk：双峰步态（每步两次起伏，谷底在 0.375/0.875 相位）', () => {
      // 新真实步态：bob=(sin(progress*4π)*0.5+0.5)*0.09
      const valley = getAction3DTransform('walk', 0.375)
      const peak = getAction3DTransform('walk', 0.125)
      expect(Math.abs(valley.position.y)).toBeLessThan(0.02)   // 谷底≈0
      expect(peak.position.y).toBeGreaterThan(0.05)            // 峰≈0.09
      // 半周期（0.5）处于两次起伏之间的过渡位（0.045），非 0
      const half = getAction3DTransform('walk', 0.5)
      expect(half.position.y).toBeGreaterThan(0.02)
      expect(half.position.y).toBeLessThan(0.08)
    })
    it('walk：左右摇摆幅度受限（不夸张）', () => {
      const a = getAction3DTransform('walk', 0.125)
      expect(Math.abs(a.rotation.z)).toBeLessThan(0.2)
    })
    it('jump：抛物线顶点最高', () => {
      const a = getAction3DTransform('jump', 0.5)
      const b = getAction3DTransform('jump', 0.1)
      expect(a.position.y).toBeGreaterThan(b.position.y)
      expect(a.position.y).toBeCloseTo(1.5, 5) // 4*0.25*1.5
    })
    it('jump：起跳/落地为 0', () => {
      expect(getAction3DTransform('jump', 0).position.y).toBe(0)
      expect(getAction3DTransform('jump', 1).position.y).toBe(0)
    })
  })

  describe('normalizeGaze 眼神跟随归一化', () => {
    const rect = { x: 100, y: 100, width: 120, height: 160 } // 中心 (160, 180)
    it('鼠标在精灵中心 → 注视 0,0（正对屏幕）', () => {
      const g = normalizeGaze(160, 180, rect)
      expect(g.x).toBeCloseTo(0, 5)
      expect(g.y).toBeCloseTo(0, 5)
    })
    it('鼠标在中心右侧 → x>0（朝右看）', () => {
      const g = normalizeGaze(250, 180, rect) // 右偏 90px
      expect(g.x).toBeGreaterThan(0.9)
      expect(g.y).toBeCloseTo(0, 5)
    })
    it('鼠标在中心上方 → y<0（朝上看）', () => {
      const g = normalizeGaze(160, 90, rect) // 上偏 90px
      expect(g.y).toBeLessThan(-0.9)
    })
    it('输出始终钳制在 [-1,1]', () => {
      const g = normalizeGaze(99999, -99999, rect)
      expect(g.x).toBe(1)
      expect(g.y).toBe(-1)
      const g2 = normalizeGaze(-99999, 99999, rect)
      expect(g2.x).toBe(-1)
      expect(g2.y).toBe(1)
    })
    it('空 rect 不崩溃 → 0,0', () => {
      expect(normalizeGaze(10, 10, null)).toEqual({ x: 0, y: 0 })
    })
  })

  describe('getIdleStage 待机阶段判定', () => {
    it('未到阈值 → 清醒(0)', () => {
      expect(getIdleStage(30000)).toBe(0)
      expect(getIdleStage(59999)).toBe(0)
    })
    it('超过瞌睡阈值 → 瞌睡(1)', () => {
      expect(getIdleStage(60000)).toBe(1)
      expect(getIdleStage(119999)).toBe(1)
    })
    it('超过睡着阈值 → 睡着(2)', () => {
      expect(getIdleStage(120000)).toBe(2)
      expect(getIdleStage(999999)).toBe(2)
    })
    it('自定义阈值生效', () => {
      expect(getIdleStage(20000, { snoozeMs: 10000, sleepMs: 30000 })).toBe(1)
      expect(getIdleStage(40000, { snoozeMs: 10000, sleepMs: 30000 })).toBe(2)
    })
    it('负数/0 → 清醒', () => {
      expect(getIdleStage(-5)).toBe(0)
      expect(getIdleStage(0)).toBe(0)
    })
  })
})
