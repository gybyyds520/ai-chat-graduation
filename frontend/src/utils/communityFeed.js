/**
 * 社区/内容发现纯逻辑（与 UI 解耦，便于单测）。
 * 覆盖：话题标签、置顶/精华排序、字数统计、评论楼中楼、点赞动画状态。
 */

/** 可选话题标签 */
export const TOPIC_TAGS = ['日常', '提问', '分享', '吐槽', '攻略', '闲聊']

/**
 * 按话题标签过滤帖子。空标签表示不过滤。
 * @param {Array} posts 帖子列表，帖子可有 topicTag 字段
 * @param {string} tag 话题标签
 * @returns {Array}
 */
export function filterByTopic(posts, tag) {
  if (!posts) return []
  if (!tag) return [...posts]
  return posts.filter((p) => (p.topicTag || '') === tag)
}

/**
 * 帖子信息流排序：置顶优先 → 其上次之 → 按创建时间倒序。
 * 不修改原数组。
 * @param {Array} posts 帖子列表，含 isPinned/isFeatured/createdAt/id
 * @returns {Array}
 */
export function sortPostsForFeed(posts) {
  if (!posts) return []
  const ts = (p) => {
    if (!p) return 0
    if (p.createdAt) return new Date(p.createdAt).getTime() || 0
    if (p.createTime) return new Date(p.createTime).getTime() || 0
    return p.id || 0
  }
  return [...posts].sort((a, b) => {
    const pa = a && a.isPinned ? 1 : 0
    const pb = b && b.isPinned ? 1 : 0
    if (pa !== pb) return pb - pa
    const fa = a && a.isFeatured ? 1 : 0
    const fb = b && b.isFeatured ? 1 : 0
    if (fa !== fb) return fb - fa
    return ts(b) - ts(a)
  })
}

/**
 * 发帖字数统计（中文按字符计，去掉首尾空白）。
 * @param {string} text
 * @returns {number}
 */
export function countPostWords(text) {
  if (!text) return 0
  return String(text).trim().length
}

/**
 * 将扁平评论列表（含 parentCommentId）组织为楼中楼树。
 * 顶层评论挂 children 数组；被回复的评论若不在列表中则挂到对应父节点。
 * @param {Array} comments 扁平评论，含 id/parentCommentId
 * @returns {Array} 顶层评论（带 children）
 */
export function buildCommentTree(comments) {
  if (!comments || !comments.length) return []
  const map = new Map()
  const roots = []
  comments.forEach((c) => {
    map.set(c.id, { ...c, children: [] })
  })
  comments.forEach((c) => {
    const node = map.get(c.id)
    const parentId = c.parentCommentId || c.parentId
    if (parentId && map.has(parentId)) {
      map.get(parentId).children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

/**
 * 计算点赞按钮动画状态类名。
 * @param {boolean} liked 当前是否已点赞
 * @returns {string}
 */
export function likeAnimationClass(liked) {
  return liked ? 'like-btn liked like-pop' : 'like-btn'
}

/**
 * 判断当前用户是否为管理员/群主（用于置顶/精华按钮权限）。
 * @param {object} ctx 上下文，含 isOwner
 * @returns {boolean}
 */
export function canModeratePost(ctx) {
  return !!(ctx && ctx.isOwner)
}
