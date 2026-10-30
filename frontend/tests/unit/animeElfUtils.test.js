import {
  getAnimeExpression,
  isEyesClosed,
  getBlinkInterval,
  pickRandom,
  getIdleRandomAction,
  getRandomInteraction,
  getExpressionMouth,
  ANIME_INTERACTION_LINES,
  ANIME_IDLE_RANDOM_ACTIONS,
  ANIME_CLICK_EXPRESSIONS,
  ANIME_CLICK_ACTIONS,
  ANIME_CLICK_EFFECTS
} from '../../src/utils/animeElfUtils'

describe('animeElfUtils 二次元桌宠纯函数', () => {
  describe('getAnimeExpression 表情映射（回归）', () => {
    it('happy 应映射为开心眼+张嘴', () => {
      const e = getAnimeExpression('happy')
      expect(e.eyes).toBe('happy')
      expect(e.mouth).toBe('open')
      expect(e.brow).toBe('raise')
    })
    it('sleep 应闭眼', () => {
      expect(isEyesClosed('sleep')).toBe(true)
      expect(isEyesClosed('idle')).toBe(false)
    })
    it('未知状态回退 idle', () => {
      expect(getAnimeExpression('whatever')).toEqual(getAnimeExpression('idle'))
    })
  })

  describe('getBlinkInterval 随机眨眼间隔', () => {
    it('默认应落在 2000~6000ms 区间内', () => {
      for (let i = 0; i < 50; i++) {
        const v = getBlinkInterval()
        expect(v).toBeGreaterThanOrEqual(2000)
        expect(v).toBeLessThan(6000)
      }
    })
    it('注入随机源应可复现（rand=0 → 下界）', () => {
      expect(getBlinkInterval(2000, 6000, () => 0)).toBe(2000)
      expect(getBlinkInterval(2000, 6000, () => 0.5)).toBe(4000)
    })
    it('min>max 时自动交换并返回下界', () => {
      expect(getBlinkInterval(6000, 2000, () => 0)).toBe(2000)
    })
  })

  describe('pickRandom 随机选取', () => {
    it('空数组应返回 null', () => {
      expect(pickRandom([])).toBeNull()
      expect(pickRandom(null)).toBeNull()
    })
    it('注入 rand=0 取首项，rand=0.999 取末项', () => {
      const list = ['a', 'b', 'c']
      expect(pickRandom(list, () => 0)).toBe('a')
      expect(pickRandom(list, () => 0.999)).toBe('c')
    })
  })

  describe('getIdleRandomAction 随机待机动作', () => {
    it('结果必在白名单内', () => {
      for (let i = 0; i < 40; i++) {
        expect(ANIME_IDLE_RANDOM_ACTIONS).toContain(getIdleRandomAction())
      }
    })
    it('注入 rand 可复现', () => {
      expect(getIdleRandomAction(() => 0)).toBe(ANIME_IDLE_RANDOM_ACTIONS[0])
    })
  })

  describe('getRandomInteraction 随机互动组合', () => {
    it('应包含 line/expression/action/effect 四个字段', () => {
      const c = getRandomInteraction(() => 0.5)
      expect(c).toHaveProperty('line')
      expect(c).toHaveProperty('expression')
      expect(c).toHaveProperty('action')
      expect(c).toHaveProperty('effect')
    })
    it('各字段应落在对应资源池内', () => {
      for (let i = 0; i < 30; i++) {
        const c = getRandomInteraction()
        expect(ANIME_INTERACTION_LINES).toContain(c.line)
        expect(ANIME_CLICK_EXPRESSIONS).toContain(c.expression)
        expect(ANIME_CLICK_ACTIONS).toContain(c.action)
        expect(ANIME_CLICK_EFFECTS).toContain(c.effect)
      }
    })
    it('台词库不少于 10 条', () => {
      expect(ANIME_INTERACTION_LINES.length).toBeGreaterThanOrEqual(10)
    })
  })

  describe('getExpressionMouth 嘴型映射', () => {
    it('happy→open, surprised→o, sad→pout, thinking→flat, sleep→sleep, normal→smile', () => {
      expect(getExpressionMouth('happy')).toBe('open')
      expect(getExpressionMouth('surprised')).toBe('o')
      expect(getExpressionMouth('sad')).toBe('pout')
      expect(getExpressionMouth('thinking')).toBe('flat')
      expect(getExpressionMouth('sleep')).toBe('sleep')
      expect(getExpressionMouth('normal')).toBe('smile')
    })
    it('未知表情回退 smile', () => {
      expect(getExpressionMouth('')).toBe('smile')
      expect(getExpressionMouth(undefined)).toBe('smile')
    })
  })
})
