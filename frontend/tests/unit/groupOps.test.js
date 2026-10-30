import {
  GROUP_ROLE,
  canEditGroupInfo,
  canKickMember,
  canTransferOwner,
  confirmDisband,
  formatAnnouncement
} from '../../src/utils/groupOps'

describe('群聊运营纯逻辑', () => {
  describe('权限校验', () => {
    it('仅群主可编辑群信息', () => {
      expect(canEditGroupInfo(GROUP_ROLE.OWNER)).toBe(true)
      expect(canEditGroupInfo(GROUP_ROLE.ADMIN)).toBe(false)
      expect(canEditGroupInfo(GROUP_ROLE.MEMBER)).toBe(false)
    })
    it('群主和管理员可踢人', () => {
      expect(canKickMember(GROUP_ROLE.OWNER)).toBe(true)
      expect(canKickMember(GROUP_ROLE.ADMIN)).toBe(true)
      expect(canKickMember(GROUP_ROLE.MEMBER)).toBe(false)
    })
    it('仅群主可转让群主', () => {
      expect(canTransferOwner(GROUP_ROLE.OWNER)).toBe(true)
      expect(canTransferOwner(GROUP_ROLE.ADMIN)).toBe(false)
    })
  })

  describe('confirmDisband 解散二次确认', () => {
    it('输入与群名完全一致才通过', () => {
      expect(confirmDisband('AI爱好者群', 'AI爱好者群')).toBe(true)
    })
    it('不一致或空则拒绝', () => {
      expect(confirmDisband('AI爱好者群', 'AI爱好者')).toBe(false)
      expect(confirmDisband('AI爱好者群', '')).toBe(false)
      expect(confirmDisband('', 'x')).toBe(false)
    })
  })

  describe('formatAnnouncement', () => {
    it('超长截断加省略号', () => {
      expect(formatAnnouncement('欢迎大家加入本群，请遵守群规文明交流', 10)).toBe(
        '欢迎大家加入本群，请…'
      )
    })
    it('空公告返回空串', () => {
      expect(formatAnnouncement('')).toBe('')
    })
  })
})
