import {
  calculateNextTarget,
  calculateNextTargetIn,
  calculateMovement,
  checkEdgeHit,
  getWalkAnimationPhase,
  getRandomWait,
  getStrollPause,
  getDeliberateMs,
  getSpeedWave
} from '../../composables/useElfMovement'

describe('useElfMovement 纯函数：移动节奏（去机械化优化）', () => {
  describe('getRandomWait 到达后休息间隔', () => {
    it('1. 默认范围应为 15000~45000ms（走动不频繁，休息为主）', () => {
      // 注入随机源 0 / 0.5 / 1 验证边界
      expect(getRandomWait(undefined, undefined, () => 0)).toBe(15000)
      expect(getRandomWait(undefined, undefined, () => 1)).toBe(45000)
      expect(getRandomWait(undefined, undefined, () => 0.5)).toBe(30000)
    })

    it('2. 结果始终落在指定区间内', () => {
      for (let i = 0; i < 200; i++) {
        const v = getRandomWait(5000, 12000, () => i / 200)
        expect(v).toBeGreaterThanOrEqual(5000)
        expect(v).toBeLessThanOrEqual(12000)
        expect(Number.isInteger(v)).toBe(true)
      }
    })

    it('3. min >= max 时自动交换并落在范围内（防呆）', () => {
      // 实现为 Math.min/max 交换，故 8000/3000 → 实际区间 3000~8000
      const v = getRandomWait(8000, 3000, () => 0.9)
      expect(v).toBeGreaterThanOrEqual(3000)
      expect(v).toBeLessThanOrEqual(8000)
    })
  })

  describe('getStrollPause 中途驻足计划（走走停停）', () => {
    it('4. 默认计划：行走 3000~6000ms 后驻足 1200~2600ms（走短歇长）', () => {
      const p0 = getStrollPause(undefined, undefined, undefined, undefined, () => 0)
      expect(p0).toEqual({ walkMs: 3000, pauseMs: 1200 })
      const p1 = getStrollPause(undefined, undefined, undefined, undefined, () => 1)
      expect(p1).toEqual({ walkMs: 6000, pauseMs: 2600 })
      const pm = getStrollPause(undefined, undefined, undefined, undefined, () => 0.5)
      expect(pm.walkMs).toBe(4500)
      expect(pm.pauseMs).toBe(1900)
    })

    it('5. 驻足时长必须小于行走时长（不会走一下就停很久）', () => {
      for (let i = 0; i < 100; i++) {
        const p = getStrollPause(undefined, undefined, undefined, undefined, () => i / 100)
        expect(p.pauseMs).toBeLessThan(p.walkMs)
      }
    })

    it('6. 自定义参数生效', () => {
      const p = getStrollPause(1000, 2000, 200, 500, () => 0.5)
      expect(p.walkMs).toBe(1500)
      expect(p.pauseMs).toBe(350)
    })
  })

  describe('getDeliberateMs 休息结束"回神"时长（去机械化）', () => {
    it('7. 默认范围 800~2200ms，边界可注入', () => {
      expect(getDeliberateMs(undefined, undefined, () => 0)).toBe(800)
      expect(getDeliberateMs(undefined, undefined, () => 1)).toBe(2200)
      expect(getDeliberateMs(undefined, undefined, () => 0.5)).toBe(1500)
    })

    it('8. 结果始终落在区间内且为整数', () => {
      for (let i = 0; i < 100; i++) {
        const v = getDeliberateMs(600, 1800, () => i / 100)
        expect(v).toBeGreaterThanOrEqual(600)
        expect(v).toBeLessThanOrEqual(1800)
        expect(Number.isInteger(v)).toBe(true)
      }
    })

    it('9. min >= max 时自动交换（防呆）', () => {
      const v = getDeliberateMs(3000, 1000, () => 0.9)
      expect(v).toBeGreaterThanOrEqual(1000)
      expect(v).toBeLessThanOrEqual(3000)
    })
  })

  describe('getSpeedWave 速度波动系数（去匀速机械感）', () => {
    it('10. 系数始终落在 0.85~1.15 区间', () => {
      for (let i = 0; i < 200; i++) {
        const w = getSpeedWave(i * 137, 0.5)
        expect(w).toBeGreaterThanOrEqual(0.85)
        expect(w).toBeLessThanOrEqual(1.15)
      }
    })

    it('11. 曲线平滑连续：相邻采样差异不超过单周期内合理幅度', () => {
      const a = getSpeedWave(1000, 0)
      const b = getSpeedWave(1016, 0) // 16ms 间隔
      expect(Math.abs(b - a)).toBeLessThan(0.1)
    })

    it('12. 不同相位偏移产生不同系数（避免全局同步波动）', () => {
      const w1 = getSpeedWave(5000, 0)
      const w2 = getSpeedWave(5000, Math.PI)
      expect(Math.abs(w1 - w2)).toBeGreaterThan(0.01)
    })
  })

  describe('既有移动纯函数回归（未被破坏）', () => {
    it('13. calculateNextTarget 随机点落在边距内', () => {
      const t = calculateNextTarget(0, 0, 800, 600, () => 0.5)
      expect(t.x).toBeGreaterThanOrEqual(40)
      expect(t.x).toBeLessThanOrEqual(760)
      expect(t.y).toBeGreaterThanOrEqual(40)
      expect(t.y).toBeLessThanOrEqual(560)
    })

    it('13b. calculateNextTargetIn 支持 minX/minY（大精灵完整留在可视区）', () => {
      const b = { minX: 96, minY: 96, maxX: 733, maxY: 774 }
      const t = calculateNextTargetIn(b, () => 0.5)
      expect(t.x).toBeGreaterThanOrEqual(136)
      expect(t.x).toBeLessThanOrEqual(693)
      expect(t.y).toBeGreaterThanOrEqual(136)
      expect(t.y).toBeLessThanOrEqual(734)
    })

    it('13c. calculateNextTargetIn 视口过小时仍返回合法点（不 NaN）', () => {
      const b = { minX: 200, minY: 200, maxX: 210, maxY: 210 }
      const t = calculateNextTargetIn(b, () => 0.5)
      expect(Number.isFinite(t.x)).toBe(true)
      expect(Number.isFinite(t.y)).toBe(true)
      expect(t.x).toBeGreaterThanOrEqual(200)
    })

    it('14. calculateMovement 朝目标移动且到达后精确停止', () => {
      let pos = { x: 0, y: 0 }
      for (let i = 0; i < 1000; i++) {
        const r = calculateMovement(pos.x, pos.y, 100, 100, 50, 0.1)
        pos = { x: r.x, y: r.y }
        if (r.arrived) break
      }
      expect(pos.x).toBe(100)
      expect(pos.y).toBe(100)
    })

    it('15. checkEdgeHit 边缘检测', () => {
      expect(checkEdgeHit(10, 50, 800, 600, 12)).toBe(true)
      expect(checkEdgeHit(50, 50, 800, 600, 12)).toBe(false)
      expect(checkEdgeHit(790, 590, 800, 600, 12)).toBe(true)
    })

    it('16. getWalkAnimationPhase 连续相位（供 3D 步态）', () => {
      const p1 = getWalkAnimationPhase(10)
      const p2 = getWalkAnimationPhase(40)
      expect(Math.abs(p1.swing)).toBeCloseTo(Math.SQRT1_2, 5)
      expect(p2.swing).toBeCloseTo(0, 1)
      expect(p2.phase).toBeGreaterThan(p1.phase)
    })
  })
})
