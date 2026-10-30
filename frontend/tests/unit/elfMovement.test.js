import {
  calculateNextTarget,
  calculateMovement,
  checkEdgeHit,
  getWalkAnimationPhase,
  getRandomWait
} from '../../src/composables/useElfMovement'

describe('useElfMovement 自由移动纯函数', () => {
  describe('calculateNextTarget 随机目标点', () => {
    it('返回点必须落在可视范围 [margin, maxX-margin] x [margin, maxY-margin] 内', () => {
      for (let i = 0; i < 80; i++) {
        const t = calculateNextTarget(100, 100, 800, 600)
        expect(t.x).toBeGreaterThanOrEqual(40)
        expect(t.x).toBeLessThanOrEqual(760)
        expect(t.y).toBeGreaterThanOrEqual(40)
        expect(t.y).toBeLessThanOrEqual(560)
      }
    })
    it('注入 rand=0 取左下角边距点', () => {
      const t = calculateNextTarget(0, 0, 800, 600, () => 0)
      expect(t.x).toBe(40)
      expect(t.y).toBe(40)
    })
    it('注入 rand=0.999 取右上角边距点', () => {
      const t = calculateNextTarget(0, 0, 800, 600, () => 0.999)
      expect(t.x).toBeLessThanOrEqual(760)
      expect(t.y).toBeLessThanOrEqual(560)
    })
    it('可视区小于边距时不产生 NaN/负数', () => {
      const t = calculateNextTarget(0, 0, 10, 10, () => 0.5, 40)
      expect(Number.isFinite(t.x)).toBe(true)
      expect(Number.isFinite(t.y)).toBe(true)
    })
  })

  describe('calculateMovement 朝目标移动', () => {
    it('朝目标方向移动一步，距离缩短', () => {
      const r = calculateMovement(0, 0, 100, 0, 50, 1) // 50px/s * 1s = 50px
      expect(r.x).toBeCloseTo(50, 5)
      expect(r.y).toBe(0)
      expect(r.arrived).toBe(false)
      expect(r.moved).toBeCloseTo(50, 5)
    })
    it('斜向移动：dx/dy 按比例', () => {
      const r = calculateMovement(0, 0, 100, 100, Math.hypot(100, 100), 1)
      // 一步覆盖全部距离 → 到达
      expect(r.arrived).toBe(true)
      expect(r.x).toBeCloseTo(100, 5)
      expect(r.y).toBeCloseTo(100, 5)
    })
    it('步长足以覆盖剩余距离时精确停在目标点', () => {
      const r = calculateMovement(90, 0, 100, 0, 1000, 0.1)
      expect(r.arrived).toBe(true)
      expect(r.x).toBe(100)
      expect(r.y).toBe(0)
    })
    it('已在目标点时立即到达', () => {
      const r = calculateMovement(100, 100, 100, 100, 50, 0.016)
      expect(r.arrived).toBe(true)
    })
    it('dt<=0 或 speed<=0 时不移动', () => {
      const r = calculateMovement(0, 0, 100, 0, 0, 1)
      expect(r.x).toBe(0)
      expect(r.y).toBe(0)
    })
  })

  describe('checkEdgeHit 碰边检测', () => {
    it('中心在范围外/边缘 → true', () => {
      expect(checkEdgeHit(0, 300, 800, 600, 0)).toBe(true)
      expect(checkEdgeHit(800, 300, 800, 600, 0)).toBe(true)
      expect(checkEdgeHit(400, 0, 800, 600, 0)).toBe(true)
      expect(checkEdgeHit(400, 600, 800, 600, 0)).toBe(true)
    })
    it('中心在中部 → false', () => {
      expect(checkEdgeHit(400, 300, 800, 600, 0)).toBe(false)
    })
    it('margin 生效：margin 内也算碰边', () => {
      expect(checkEdgeHit(50, 300, 800, 600, 60)).toBe(true)
      expect(checkEdgeHit(200, 200, 800, 600, 60)).toBe(false)
    })
  })

  describe('getWalkAnimationPhase 走路相位', () => {
    it('累计距离为 0 → swing=0, bob=0', () => {
      const p = getWalkAnimationPhase(0)
      expect(p.swing).toBeCloseTo(0, 5)
      expect(p.bob).toBeCloseTo(0, 5)
    })
    it('四分之一步幅（10px）→ swing=√2/2', () => {
      const p = getWalkAnimationPhase(10) // phase = (10/40)*π = π/4 → sin=0.707
      expect(p.swing).toBeCloseTo(Math.SQRT1_2, 5)
    })
    it('半步幅（20px）→ swing=1（相位 π/2）', () => {
      const p = getWalkAnimationPhase(20) // phase = (20/40)*π = π/2 → sin=1
      expect(p.swing).toBeCloseTo(1, 5)
    })
    it('一个完整步幅 → swing=0（完成一个周期回到零）', () => {
      const p = getWalkAnimationPhase(40) // phase = (40/40)*π = π → sin(π)=0
      expect(p.swing).toBeCloseTo(0, 5)
    })
    it('swing 始终在 [-1,1]，bob 在 [0,1]', () => {
      for (let d = 0; d < 400; d += 5) {
        const p = getWalkAnimationPhase(d)
        expect(p.swing).toBeGreaterThanOrEqual(-1.0001)
        expect(p.swing).toBeLessThanOrEqual(1.0001)
        expect(p.bob).toBeGreaterThanOrEqual(-0.0001)
        expect(p.bob).toBeLessThanOrEqual(1.0001)
      }
    })
    it('phase 为连续弧度（供 3D 渲染器驱动完整步态）', () => {
      const p0 = getWalkAnimationPhase(0)
      const p1 = getWalkAnimationPhase(10)
      const p2 = getWalkAnimationPhase(20)
      expect(p0.phase).toBe(0)
      expect(p1.phase).toBeCloseTo(Math.PI / 4, 5)
      expect(p2.phase).toBeCloseTo(Math.PI / 2, 5)
      // 相位连续递增（同一 swing 值可对应不同相位，必须暴露 phase 本身）
      expect(p2.phase).toBeGreaterThan(p1.phase)
      expect(p1.phase).toBeGreaterThan(p0.phase)
    })
    it('一个完整步幅 → phase=π（走完一个周期）', () => {
      const p = getWalkAnimationPhase(40)
      expect(p.phase).toBeCloseTo(Math.PI, 5)
      expect(p.swing).toBeCloseTo(0, 5)
    })
  })

  describe('getRandomWait 随机等待', () => {
    it('默认落在 15000~45000ms（走动不频繁，去机械化）', () => {
      for (let i = 0; i < 40; i++) {
        const w = getRandomWait()
        expect(w).toBeGreaterThanOrEqual(15000)
        expect(w).toBeLessThan(45000)
      }
    })
    it('注入 rand=0 取下界', () => {
      expect(getRandomWait(2000, 5000, () => 0)).toBe(2000)
    })
    it('min>max 自动交换', () => {
      expect(getRandomWait(5000, 2000, () => 0)).toBe(2000)
    })
    it('到达后等待明显长于行走单步（生命节奏）', () => {
      const w = getRandomWait()
      expect(w).toBeGreaterThanOrEqual(15000)
      expect(Number.isInteger(w)).toBe(true)
    })
  })
})
