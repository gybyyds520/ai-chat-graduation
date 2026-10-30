import {
  isOnline,
  displayFriendName,
  recommendReason,
  ONLINE_THRESHOLD_MS
} from '../../src/utils/social'

describe('好友社交纯逻辑', () => {
  describe('isOnline', () => {
    const now = 1000000
    it('5 分钟内活跃视为在线', () => {
      expect(isOnline(new Date(now - ONLINE_THRESHOLD_MS + 1000).toISOString(), now)).toBe(true)
    })
    it('超过 5 分钟视为离线', () => {
      expect(isOnline(new Date(now - ONLINE_THRESHOLD_MS - 1000).toISOString(), now)).toBe(false)
    })
    it('无活跃时间或非法值视为离线', () => {
      expect(isOnline(null, now)).toBe(false)
      expect(isOnline('not-a-date', now)).toBe(false)
    })
  })

  describe('displayFriendName', () => {
    it('有备注优先备注', () => {
      expect(displayFriendName({ remark: '我闺蜜', username: 'alice' })).toBe('我闺蜜')
    })
    it('无备注用用户名', () => {
      expect(displayFriendName({ username: 'bob' })).toBe('bob')
      expect(displayFriendName({ nickname: '小刘' })).toBe('小刘')
      expect(displayFriendName({})).toBe('好友')
    })
  })

  describe('recommendReason', () => {
    it('无共同角色给默认文案', () => {
      expect(recommendReason([])).toBe('你可能认识的人')
    })
    it('一个共同角色', () => {
      expect(recommendReason(['温柔助手'])).toBe('你们都在玩「温柔助手」这个角色')
    })
    it('两个共同角色', () => {
      expect(recommendReason(['温柔助手', '学霸'])).toBe(
        '你们都在玩「温柔助手」和「学霸」'
      )
    })
    it('三个及以上汇总', () => {
      expect(recommendReason(['a', 'b', 'c'])).toBe('你们都在玩「a」「b」等 3 个角色')
    })
  })
})
