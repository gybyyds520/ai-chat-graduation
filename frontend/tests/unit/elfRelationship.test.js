import {
  LEVEL_THRESHOLDS,
  LEVEL_TITLES,
  levelFromAffection,
  titleOfLevel,
  titleFromAffection,
  affectionProgress
} from '../../src/utils/elfRelationship'

describe('elfRelationship AI 精灵养成纯逻辑', () => {
  describe('levelFromAffection 等级阈值计算', () => {
    it('0 好感应为 1 级陌生', () => {
      expect(levelFromAffection(0)).toBe(1)
    })

    it('10/25/45/70/100 边界对应升级', () => {
      expect(levelFromAffection(9)).toBe(1)
      expect(levelFromAffection(10)).toBe(2)
      expect(levelFromAffection(24)).toBe(2)
      expect(levelFromAffection(25)).toBe(3)
      expect(levelFromAffection(45)).toBe(4)
      expect(levelFromAffection(70)).toBe(5)
      expect(levelFromAffection(100)).toBe(6)
    })

    it('负数/非法输入应回退 1 级', () => {
      expect(levelFromAffection(-5)).toBe(1)
      expect(levelFromAffection(undefined)).toBe(1)
    })
  })

  describe('titleOfLevel 称号映射', () => {
    it('各级称号正确', () => {
      expect(titleOfLevel(1)).toBe('陌生')
      expect(titleOfLevel(2)).toBe('熟悉')
      expect(titleOfLevel(3)).toBe('朋友')
      expect(titleOfLevel(4)).toBe('好友')
      expect(titleOfLevel(5)).toBe('亲密')
      expect(titleOfLevel(6)).toBe('挚友')
    })

    it('越界应钳制到边界称号', () => {
      expect(titleOfLevel(0)).toBe('陌生')
      expect(titleOfLevel(99)).toBe('挚友')
    })

    it('titleFromAffection 由好感度直推称号', () => {
      expect(titleFromAffection(30)).toBe('朋友')
      expect(titleFromAffection(100)).toBe('挚友')
    })
  })

  describe('affectionProgress 进度', () => {
    it('区间内进度百分比', () => {
      // 3 级区间 [25,45)，35 好感 => 50%
      expect(affectionProgress(35)).toBe(50)
    })

    it('满级应返回 100', () => {
      expect(affectionProgress(100)).toBe(100)
    })
  })

  it('阈值与称号数组长度一致', () => {
    expect(LEVEL_THRESHOLDS.length).toBe(LEVEL_TITLES.length)
  })
})
