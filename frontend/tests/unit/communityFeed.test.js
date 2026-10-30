import {
  TOPIC_TAGS,
  filterByTopic,
  sortPostsForFeed,
  countPostWords,
  buildCommentTree,
  likeAnimationClass,
  canModeratePost
} from '../../src/utils/communityFeed'

describe('社区内容发现纯逻辑', () => {
  describe('TOPIC_TAGS', () => {
    it('应包含常用话题标签', () => {
      expect(TOPIC_TAGS).toEqual(expect.arrayContaining(['日常', '提问', '分享']))
    })
  })

  describe('filterByTopic', () => {
    it('空标签返回全部', () => {
      const posts = [{ topicTag: '日常' }, { topicTag: '提问' }]
      expect(filterByTopic(posts, '')).toHaveLength(2)
      expect(filterByTopic(posts, null)).toHaveLength(2)
    })
    it('指定标签过滤', () => {
      const posts = [{ topicTag: '日常' }, { topicTag: '提问' }, { topicTag: '日常' }]
      expect(filterByTopic(posts, '日常')).toHaveLength(2)
    })
  })

  describe('sortPostsForFeed', () => {
    it('置顶优先于普通帖', () => {
      const posts = [
        { id: 1, isPinned: false, createdAt: '2024-01-03' },
        { id: 2, isPinned: true, createdAt: '2024-01-01' },
        { id: 3, isPinned: false, createdAt: '2024-01-02' }
      ]
      const r = sortPostsForFeed(posts)
      expect(r[0].id).toBe(2)
    })
    it('同置顶状态下精华优先，再按时间倒序', () => {
      const posts = [
        { id: 1, isPinned: false, isFeatured: false, createdAt: '2024-01-01' },
        { id: 2, isPinned: false, isFeatured: true, createdAt: '2024-01-02' },
        { id: 3, isPinned: false, isFeatured: false, createdAt: '2024-01-03' }
      ]
      const r = sortPostsForFeed(posts)
      expect(r[0].id).toBe(2)
      expect(r[1].id).toBe(3)
      expect(r[2].id).toBe(1)
    })
    it('不修改原数组', () => {
      const posts = [{ id: 2, isPinned: true }, { id: 1, isPinned: false }]
      sortPostsForFeed(posts)
      expect(posts[0].id).toBe(2)
    })
  })

  describe('countPostWords', () => {
    it('统计 trim 后字符数', () => {
      expect(countPostWords('  你好世界  ')).toBe(4)
      expect(countPostWords('')).toBe(0)
      expect(countPostWords(null)).toBe(0)
    })
  })

  describe('buildCommentTree 楼中楼', () => {
    it('按 parentCommentId 嵌套', () => {
      const comments = [
        { id: 1, parentCommentId: null, content: '顶层' },
        { id: 2, parentCommentId: 1, content: '回复1' },
        { id: 3, parentCommentId: 1, content: '回复2' },
        { id: 4, parentCommentId: 2, content: '回复回复1' }
      ]
      const tree = buildCommentTree(comments)
      expect(tree).toHaveLength(1)
      expect(tree[0].children).toHaveLength(2)
      expect(tree[0].children[0].children).toHaveLength(1)
    })
    it('父评论缺失时作为顶层', () => {
      const comments = [{ id: 1, parentCommentId: 999 }]
      expect(buildCommentTree(comments)).toHaveLength(1)
    })
  })

  describe('likeAnimationClass', () => {
    it('已点赞返回 like-pop 动画类', () => {
      expect(likeAnimationClass(true)).toContain('like-pop')
      expect(likeAnimationClass(false)).not.toContain('like-pop')
    })
  })

  describe('canModeratePost', () => {
    it('群主/管理员可置顶精华', () => {
      expect(canModeratePost({ isOwner: true })).toBe(true)
      expect(canModeratePost({ isOwner: false })).toBe(false)
    })
  })
})
