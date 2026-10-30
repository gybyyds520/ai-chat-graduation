import {
  PLANS,
  MEMBER_HIGHLIGHTS,
  buildComparisonRows,
  isOverQuota,
  checkUpgradeNeeded
} from '../../src/utils/membershipPlans'

describe('membershipPlans 会员权益对比', () => {
  it('PLANS 含免费版与会员版', () => {
    expect(PLANS.free.name).toBe('免费版')
    expect(PLANS.member.name).toBe('会员版')
  })

  it('buildComparisonRows 覆盖四项额度', () => {
    const rows = buildComparisonRows()
    expect(rows.map(r => r.key)).toEqual(['messages', 'voiceMinutes', 'imageGeneration', 'kbFiles'])
  })

  it('会员版在各项均不劣于免费版', () => {
    const rows = buildComparisonRows()
    for (const r of rows) {
      expect(['member', 'tie']).toContain(r.better)
    }
  })

  it('messages 会员不限 -> better=member', () => {
    const row = buildComparisonRows().find(r => r.key === 'messages')
    expect(row.better).toBe('member')
    expect(row.member).toContain('不限')
  })

  it('isOverQuota 达到阈值即超额', () => {
    expect(isOverQuota('messages', 50, PLANS.free)).toBe(true)
    expect(isOverQuota('messages', 49, PLANS.free)).toBe(false)
  })

  it('isOverQuota 会员不限额度永不超额', () => {
    expect(isOverQuota('messages', 99999, PLANS.member)).toBe(false)
  })

  it('isOverQuota 非法用量不超额', () => {
    expect(isOverQuota('messages', 'abc', PLANS.free)).toBe(false)
  })

  it('checkUpgradeNeeded 检测超额项', () => {
    const r = checkUpgradeNeeded({ messages: 60, kbFiles: 3 }, PLANS.free)
    expect(r.needUpgrade).toBe(true)
    expect(r.exceeded).toContain('messages')
    expect(r.exceeded).not.toContain('kbFiles')
  })

  it('checkUpgradeNeeded 未超额返回 needUpgrade=false', () => {
    const r = checkUpgradeNeeded({ messages: 10, voiceMinutes: 5, imageGeneration: 1, kbFiles: 1 }, PLANS.free)
    expect(r.needUpgrade).toBe(false)
  })

  it('MEMBER_HIGHLIGHTS 非空数组', () => {
    expect(Array.isArray(MEMBER_HIGHLIGHTS)).toBe(true)
    expect(MEMBER_HIGHLIGHTS.length).toBeGreaterThan(0)
  })
})
