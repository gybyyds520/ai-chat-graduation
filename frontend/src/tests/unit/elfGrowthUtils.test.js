/**
 * elfGrowthUtils.test.js — 市场级升级新增纯函数单元测试
 *
 * 覆盖：行走步态相位、跳跃三段曲线、待机电呼吸、时段感知、久坐提醒等级、
 *       连续点击连招、头部点击判定、配饰槽位目录。不依赖 DOM/three。
 */

import {
  getWalkPhase,
  getJumpCurve,
  getIdleBreath,
  getTimePeriod,
  getInactivityLevel,
  isComboClick,
  isHeadTap,
  ELF_ACCESSORY_SLOTS
} from '../../utils/elf3dConfig'

describe('getWalkPhase 行走步伐相位', () => {
  test('左右腿反相摆动，身体随步伐起伏', () => {
    const a = getWalkPhase(0)
    const b = getWalkPhase(0.5)
    // 左右腿幅度相反
    expect(a.leftLeg).toBeCloseTo(-a.rightLeg, 5)
    // 摆角幅度不超过振幅
    expect(Math.abs(b.leftLeg)).toBeLessThanOrEqual(0.45 + 1e-6)
    // 不同相位产生不同摆角
    expect(b.leftLeg).not.toBeCloseTo(a.leftLeg, 5)
    // bodyBob 恒为非负（起伏量）
    expect(a.bodyBob).toBeGreaterThanOrEqual(0)
  })

  test('频率参数生效', () => {
    const slow = getWalkPhase(1, 1)
    const fast = getWalkPhase(1, 4)
    expect(fast.leftLeg).not.toBe(slow.leftLeg)
  })
})

describe('getJumpCurve 跳跃三段曲线', () => {
  test('progress=0.5 达到抛物线最高点', () => {
    const mid = getJumpCurve(0.5)
    const start = getJumpCurve(0)
    const end = getJumpCurve(1)
    expect(mid.positionY).toBeGreaterThan(start.positionY)
    expect(mid.positionY).toBeGreaterThan(end.positionY)
    expect(start.positionY).toBeCloseTo(0, 5)
    expect(end.positionY).toBeCloseTo(0, 5)
  })

  test('预备下蹲阶段 scaleY<1，空中阶段 scaleY>1', () => {
    const crouch = getJumpCurve(0.07)
    const air = getJumpCurve(0.35)
    expect(crouch.scaleY).toBeLessThan(1)
    expect(air.scaleY).toBeGreaterThan(1)
  })

  test('progress 钳制在 0~1', () => {
    expect(getJumpCurve(-1).positionY).toBeGreaterThanOrEqual(0)
    expect(getJumpCurve(2).positionY).toBeCloseTo(0, 5)
  })
})

describe('getIdleBreath 待机呼吸', () => {
  test('正弦缩放围绕 1 波动', () => {
    const b1 = getIdleBreath(0)
    const b2 = getIdleBreath(1.3 / 4)
    expect(b1.scaleY).toBeCloseTo(1, 2)
    expect(b2.scaleY).not.toBeCloseTo(1, 3)
    expect(b2.scaleY).toBeGreaterThan(1)
  })
})

describe('getTimePeriod 时段感知', () => {
  test('白天 8-18 为 day', () => {
    expect(getTimePeriod(12)).toBe('day')
    expect(getTimePeriod(8)).toBe('day')
    expect(getTimePeriod(17)).toBe('day')
  })
  test('夜晚 22-6 为 night', () => {
    expect(getTimePeriod(23)).toBe('night')
    expect(getTimePeriod(2)).toBe('night')
    expect(getTimePeriod(5)).toBe('night')
  })
  test('傍晚 18-22 / 清晨 6-8', () => {
    expect(getTimePeriod(20)).toBe('evening')
    expect(getTimePeriod(7)).toBe('morning')
  })
})

describe('getInactivityLevel 久坐提醒', () => {
  test('分级阈值', () => {
    expect(getInactivityLevel(5)).toBe(0)
    expect(getInactivityLevel(20)).toBe(1)
    expect(getInactivityLevel(45)).toBe(2)
  })
})

describe('isComboClick 连续点击连招', () => {
  test('3 次及以上触发', () => {
    expect(isComboClick(2)).toBe(false)
    expect(isComboClick(3)).toBe(true)
  })
})

describe('isHeadTap 头部点击判定', () => {
  test('上半 45% 视为摸头', () => {
    expect(isHeadTap(30, 200)).toBe(true)
    expect(isHeadTap(150, 200)).toBe(false)
    expect(isHeadTap(0, 0)).toBe(false)
  })
})

describe('ELF_ACCESSORY_SLOTS 配饰槽位目录', () => {
  test('三个槽位各有 3-5 款且默认 none', () => {
    expect(Object.keys(ELF_ACCESSORY_SLOTS)).toEqual(['hat', 'glasses', 'bag'])
    for (const slot of Object.keys(ELF_ACCESSORY_SLOTS)) {
      const items = ELF_ACCESSORY_SLOTS[slot]
      expect(items.length).toBeGreaterThanOrEqual(3)
      expect(items.length).toBeLessThanOrEqual(5)
      expect(items[0].id).toBe('none')
    }
  })
})
