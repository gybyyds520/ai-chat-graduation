import {
  MEMBERSHIP_TIERS,
  getTier,
  canUpgradeFrom,
  usageStatus
} from '../../src/data/membership'

describe('会员分级与配额纯逻辑', () => {
  describe('等级配置', () => {
    it('三档配额应与后端一致', () => {
      expect(MEMBERSHIP_TIERS.FREE.quotas.ai_chat).toBe(20)
      expect(MEMBERSHIP_TIERS.FREE.quotas.image_gen).toBe(0)
      expect(MEMBERSHIP_TIERS.BASIC.quotas.ai_chat).toBe(100)
      expect(MEMBERSHIP_TIERS.BASIC.quotas.doc_upload).toBe(3)
      expect(MEMBERSHIP_TIERS.PREMIUM.quotas.ai_chat).toBe(500)
      expect(MEMBERSHIP_TIERS.PREMIUM.quotas.image_gen).toBe(20)
    })

    it('getTier 非法值应回退 FREE', () => {
      expect(getTier(undefined)).toBe(MEMBERSHIP_TIERS.FREE)
      expect(getTier('VIP')).toBe(MEMBERSHIP_TIERS.FREE)
      expect(getTier('PREMIUM').label).toBe('高级版')
    })
  })

  describe('升级路径', () => {
    it('应严格递增', () => {
      expect(canUpgradeFrom('FREE', 'BASIC')).toBe(true)
      expect(canUpgradeFrom('BASIC', 'PREMIUM')).toBe(true)
      expect(canUpgradeFrom('PREMIUM', 'BASIC')).toBe(false)
      expect(canUpgradeFrom('FREE', 'FREE')).toBe(false)
    })
  })

  describe('用量计算', () => {
    it('未超限应可用且剩余正确', () => {
      const s = usageStatus('FREE', 'ai_chat', 5)
      expect(s.limit).toBe(20)
      expect(s.used).toBe(5)
      expect(s.remaining).toBe(15)
      expect(s.available).toBe(true)
      expect(s.percent).toBe(25)
    })

    it('FREE 图片生成应不可用', () => {
      const s = usageStatus('FREE', 'image_gen', 0)
      expect(s.limit).toBe(0)
      expect(s.available).toBe(false)
    })

    it('达到上限应不可用', () => {
      const s = usageStatus('BASIC', 'voice_call', 20)
      expect(s.remaining).toBe(0)
      expect(s.available).toBe(false)
      expect(s.percent).toBe(100)
    })
  })
})
