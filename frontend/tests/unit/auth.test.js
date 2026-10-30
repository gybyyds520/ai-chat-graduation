import { getCurrentUser, getCurrentUserId, getCurrentUserNickname } from '../../src/utils/auth'

describe('auth 当前登录用户工具', () => {
  const USER_KEY = 'user'

  afterEach(() => {
    localStorage.clear()
  })

  describe('getCurrentUser', () => {
    it('无 user 存储应返回 null', () => {
      expect(getCurrentUser()).toBeNull()
    })

    it('损坏的 JSON 应返回 null 而非抛错', () => {
      localStorage.setItem(USER_KEY, '{broken json')
      expect(getCurrentUser()).toBeNull()
    })

    it('有效 user 对象应解析返回', () => {
      localStorage.setItem(USER_KEY, JSON.stringify({ id: 82, nickname: '爱丽丝' }))
      expect(getCurrentUser().id).toBe(82)
      expect(getCurrentUser().nickname).toBe('爱丽丝')
    })
  })

  describe('getCurrentUserId', () => {
    it('未登录返回 null', () => {
      expect(getCurrentUserId()).toBeNull()
    })

    it('从 user.id 解析数字 id', () => {
      localStorage.setItem(USER_KEY, JSON.stringify({ id: 82, nickname: '爱丽丝' }))
      expect(getCurrentUserId()).toBe(82)
    })

    it('兼容 userId 字段与字符串 id', () => {
      localStorage.setItem(USER_KEY, JSON.stringify({ userId: '83', nickname: '鲍勃' }))
      expect(getCurrentUserId()).toBe(83)
    })

    it('user 存在但无 id 时返回 null（避免默认 1 导致 401）', () => {
      localStorage.setItem(USER_KEY, JSON.stringify({ nickname: '无id' }))
      expect(getCurrentUserId()).toBeNull()
    })
  })

  describe('getCurrentUserNickname', () => {
    it('返回昵称，缺失时回退 userName', () => {
      localStorage.setItem(USER_KEY, JSON.stringify({ id: 1, userName: '戴夫' }))
      expect(getCurrentUserNickname()).toBe('戴夫')
    })

    it('未登录返回 null', () => {
      expect(getCurrentUserNickname()).toBeNull()
    })
  })
})
