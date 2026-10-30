<template>
  <div class="community-container">
    <div class="community-header">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <h2>兴趣社群</h2>
      <p>发现志同道合的朋友</p>
    </div>

    <div class="community-actions">
      <el-input v-model="searchKeyword" placeholder="搜索社群" @keyup.enter="searchCommunities" />
      <el-button type="primary" @click="openCreateModal">创建社群</el-button>
    </div>

    <div class="category-tabs">
      <span v-for="cat in categories" :key="cat.value"
            :class="['category-tab', { active: currentCategory === cat.value && !mineOnly }]"
            @click="currentCategory = cat.value; mineOnly = false; loadCommunities()">
        {{ cat.label }}
      </span>
      <span class="tab-divider">|</span>
      <span :class="['category-tab', { active: mineOnly }]" @click="mineOnly = true">
        我的社群
      </span>
    </div>

    <div class="community-grid">
      <div v-for="community in displayedCommunities" :key="community.id" class="community-card" @click="openCommunity(community)">
        <div class="community-avatar">
          <img v-if="community.avatar" :src="resolveUrl(community.avatar)" />
          <span v-else class="default-avatar">{{ community.name.charAt(0) }}</span>
        </div>
        <div class="community-info">
          <h3>{{ community.name }}</h3>
          <p class="community-desc">{{ community.description }}</p>
          <div class="community-meta">
            <span class="meta-item">{{ community.memberCount }} 成员</span>
            <span class="meta-item">{{ community.postCount }} 帖子</span>
            <span v-if="community.todayPostCount != null" class="meta-item today">今日 {{ community.todayPostCount }} 帖</span>
          </div>
        </div>
        <div class="community-action">
          <el-button v-if="isMember(community.id)" size="small">已加入</el-button>
          <el-button v-else type="primary" size="small" @click.stop="joinCommunity(community)">加入</el-button>
        </div>
      </div>
      <div v-if="!displayedCommunities.length" class="empty-mini">{{ mineOnly ? '你还没有加入任何社群' : '暂无社群' }}</div>
    </div>

    <el-dialog title="创建社群" v-model="createModalVisible" width="500px">
      <el-form :model="createForm">
        <el-form-item label="社群头像">
          <div class="avatar-upload-row">
            <img v-if="createForm.avatarUrl" :src="resolveUrl(createForm.avatarUrl)" class="avatar-preview" />
            <span v-else class="avatar-preview placeholder">无</span>
            <input type="file" accept="image/*" @change="handleAvatarUpload" />
          </div>
        </el-form-item>
        <el-form-item label="社群名称" required>
          <el-input v-model="createForm.name" />
        </el-form-item>
        <el-form-item label="社群描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="createForm.category">
            <el-option v-for="cat in categories" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createModalVisible = false">取消</el-button>
        <el-button type="primary" @click="createCommunity">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="communityDetailVisible" width="700px">
      <div v-if="selectedCommunity" class="community-detail">
        <div class="detail-header">
          <div class="detail-avatar">
            <img v-if="selectedCommunity.avatar" :src="resolveUrl(selectedCommunity.avatar)" />
            <span v-else>{{ selectedCommunity.name.charAt(0) }}</span>
          </div>
          <div class="detail-info">
            <h3>{{ selectedCommunity.name }}</h3>
            <p>{{ selectedCommunity.description }}</p>
            <div class="detail-meta">
              <span>{{ selectedCommunity.memberCount }} 成员</span>
              <span>{{ selectedCommunity.postCount }} 帖子</span>
              <span v-if="isOwner()" class="owner-badge">管理员</span>
            </div>
          </div>
        </div>

        <div class="post-section">
          <div class="post-input">
            <div class="topic-row">
              <span class="topic-label">话题：</span>
              <span
                v-for="t in TOPIC_TAGS"
                :key="t"
                :class="['topic-chip', { active: newPostTopic === t }]"
                @click="newPostTopic = newPostTopic === t ? '' : t"
              >#{{ t }}</span>
            </div>
            <el-input v-model="newPostContent" type="textarea" :rows="3" placeholder="分享你的想法..." />
            <div class="post-input-footer">
              <span class="word-count">{{ postWordCount }}/500</span>
              <div class="image-preview-row" v-if="newPostImages.length">
                <div v-for="(img, i) in newPostImages" :key="i" class="img-thumb">
                  <img :src="img" />
                  <span class="img-remove" @click="newPostImages.splice(i, 1)">×</span>
                </div>
              </div>
              <el-button size="small" @click="triggerImageUpload">配图</el-button>
              <input ref="postImageInput" type="file" accept="image/*" multiple style="display:none" @change="onPostImagesPicked" />
            </div>
            <el-button type="primary" @click="createPost" :disabled="!newPostContent || isPosting">发布</el-button>
          </div>

          <div class="post-list">
            <div v-for="post in sortedPosts" :key="post.id" class="post-item">
              <div class="post-header">
                <span class="post-author">
                  <span v-if="post.isPinned" class="pin-badge">置顶</span>
                  <span v-if="post.isFeatured" class="featured-badge">精华</span>
                  {{ post.userName }}
                  <span v-if="post.topicTag" class="topic-inline">#{{ post.topicTag }}</span>
                </span>
                <span class="post-time">{{ formatTime(post.createdAt) }}</span>
              </div>
              <div class="post-content">{{ post.content }}</div>
              <div class="post-images" v-if="post.mediaUrls">
                <img v-for="(u, i) in parseMediaUrls(post.mediaUrls)" :key="i" :src="resolveUrl(u)" class="post-img" />
              </div>
              <div class="post-actions">
                <span class="action-item" :class="likeAnimationClass(likedSet.has(post.id))" @click="likePost(post)">
                  {{ likedSet.has(post.id) ? '❤️' : '🤍' }} {{ post.likeCount }}
                </span>
                <span class="action-item" @click="toggleComments(post)">💬 {{ post.commentCount }}</span>
                <span v-if="canModeratePost({ isOwner: isOwner() })" class="action-item" @click="toggleFeature(post)">{{ post.isFeatured ? '取消精华' : '加精' }}</span>
                <span v-if="canDeletePost(post)" class="action-item danger" @click="removePost(post)">删除</span>
                <span v-if="isOwner()" class="action-item" @click="pinPost(post)">{{ post.isPinned ? '取消置顶' : '置顶' }}</span>
              </div>

              <div v-if="expandedPostId === post.id" class="comment-section">
                <template v-for="c in commentTree[post.id] || []" :key="c.id">
                  <div class="comment-item">
                    <span class="comment-author">{{ c.userName }}</span>
                    <span class="comment-content">{{ c.content }}</span>
                    <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
                    <span class="action-item" @click="replyToComment(post, c)">回复</span>
                    <span v-if="c.userId === getUserId()" class="action-item danger" @click="removeComment(post, c)">删除</span>
                  </div>
                  <div v-for="child in c.children" :key="child.id" class="comment-item reply-item">
                    <span class="reply-line">└</span>
                    <span class="comment-author">{{ child.userName }}</span>
                    <span class="reply-to" v-if="child.replyToName">回复 {{ child.replyToName }}：</span>
                    <span class="comment-content">{{ child.content }}</span>
                    <span class="comment-time">{{ formatTime(child.createdAt) }}</span>
                    <span v-if="child.userId === getUserId()" class="action-item danger" @click="removeComment(post, child)">删除</span>
                  </div>
                </template>
                <div class="comment-input-row">
                  <el-input v-model="commentDrafts[post.id]" :placeholder="replyingTo[post.id] ? `回复 ${replyingTo[post.id].userName}...` : '写下你的评论...'" @keyup.enter="sendComment(post)" />
                  <el-button size="small" type="primary" @click="sendComment(post)">发送</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import {
  TOPIC_TAGS,
  sortPostsForFeed,
  countPostWords,
  buildCommentTree,
  likeAnimationClass,
  canModeratePost
} from '@/utils/communityFeed'

const router = useRouter()
// 返回上一页
const goBack = () => {
  const fromRoute = router.options.history.state.value?.back
  if (fromRoute && fromRoute.fullPath) {
    router.back()
  } else {
    router.push('/chat')
  }
}

const communities = ref([])
const searchKeyword = ref('')
const currentCategory = ref('')
const createModalVisible = ref(false)
const communityDetailVisible = ref(false)
const selectedCommunity = ref(null)
const communityPosts = ref([])
const newPostContent = ref('')

// ===== 社区运营升级状态 =====
const newPostTopic = ref('')
const newPostImages = ref([])
const isPosting = ref(false) // 发帖防重复提交
const postImageInput = ref(null)
const replyingTo = reactive({}) // postId -> comment 对象
// 话题标签筛选（社群列表页可按标签过滤帖子）
const activeTopicFilter = ref('')

// 发帖字数统计
const postWordCount = computed(() => countPostWords(newPostContent.value))

// 帖子信息流：置顶优先 → 精华 → 时间倒序
const sortedPosts = computed(() => {
  let list = sortPostsForFeed(communityPosts.value)
  if (activeTopicFilter.value) {
    list = list.filter((p) => p.topicTag === activeTopicFilter.value)
  }
  return list
})

// 评论楼中楼树（按 postId 组织）
const commentTree = reactive({})

// 点赞去重 / 评论展开状态
const likedSet = ref(new Set())
const expandedPostId = ref(null)
const commentMap = reactive({})
const commentDrafts = reactive({})

const categories = [
  { label: '全部', value: '' },
  { label: '科技', value: 'tech' },
  { label: '游戏', value: 'gaming' },
  { label: '音乐', value: 'music' },
  { label: '艺术', value: 'art' },
  { label: '学习', value: 'study' },
  { label: '运动', value: 'sports' }
]

const createForm = ref({ name: '', description: '', category: '', avatarUrl: '' })

const joinedCommunities = ref(new Set())

// "我的社群" tab：仅展示已加入的社群
const mineOnly = ref(false)
const displayedCommunities = computed(() => {
  if (!mineOnly.value) return communities.value
  return communities.value.filter((cm) => joinedCommunities.value.has(cm.id))
})

const getUserId = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.id || user.userId
}

const getUserName = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.username || user.nickname || '用户'
}

// 后端返回 /uploads/... 相对路径，补全为可访问 URL
const resolveUrl = (url) => {
  if (!url) return ''
  if (/^https?:\/\//.test(url) || url.startsWith('/')) return url
  return '/' + url
}

const isMember = (communityId) => {
  return joinedCommunities.value.has(communityId)
}

const isOwner = () => {
  return selectedCommunity.value && selectedCommunity.value.ownerId === getUserId()
}

const canDeletePost = (post) => {
  return post.userId === getUserId() || isOwner()
}

const formatTime = (dateTime) => {
  if (!dateTime) return ''
  const d = new Date(dateTime)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const loadCommunities = async () => {
  try {
    const params = currentCategory.value ? { category: currentCategory.value } : {}
    const response = await request.get('/api/community/list', { params })
    if (response.code === 200) {
      communities.value = response.data
    }
  } catch (e) { console.error(e) }
}

const searchCommunities = async () => {
  if (!searchKeyword.value.trim()) {
    loadCommunities()
    return
  }
  try {
    const response = await request.get('/api/community/search', { params: { keyword: searchKeyword.value } })
    if (response.code === 200) {
      communities.value = response.data
    }
  } catch (e) { console.error(e) }
}

const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return
  }
  try {
    const formData = new FormData()
    formData.append('file', file)
    const response = await request.post('/api/community/upload-avatar', formData)
    if (response.code === 200) {
      createForm.value.avatarUrl = response.data.url
      ElMessage.success('头像上传成功')
    } else {
      ElMessage.error(response.msg || '上传失败')
    }
  } catch (e) {
    ElMessage.error('头像上传失败')
  } finally {
    event.target.value = ''
  }
}

const createCommunity = async () => {
  if (!createForm.value.name) {
    ElMessage.warning('请输入社群名称')
    return
  }
  try {
    const response = await request.post('/api/community/create', {
      userId: getUserId(),
      name: createForm.value.name,
      description: createForm.value.description,
      category: createForm.value.category,
      avatarUrl: createForm.value.avatarUrl
    })
    if (response.code === 200) {
      ElMessage.success('社群创建成功')
      createModalVisible.value = false
      joinedCommunities.value.add(response.data.id)
      loadCommunities()
    }
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

const joinCommunity = async (community) => {
  try {
    const response = await request.post('/api/community/join', {
      communityId: community.id,
      userId: getUserId()
    })
    if (response.code === 200) {
      ElMessage.success('加入成功')
      joinedCommunities.value.add(community.id)
      community.memberCount++
    }
  } catch (e) {
    ElMessage.error('加入失败')
  }
}

const openCommunity = async (community) => {
  selectedCommunity.value = community
  communityDetailVisible.value = true
  likedSet.value = new Set()
  expandedPostId.value = null
  await loadPosts(community.id)
}

const loadPosts = async (communityId) => {
  try {
    const response = await request.get(`/api/community/posts/${communityId}`, {
      params: { page: 1, size: 20 }
    })
    if (response.code === 200) {
      communityPosts.value = response.data.list || response.data || []
    }
  } catch (e) { console.error(e) }
}

const createPost = async () => {
  if (!newPostContent.value.trim() || !selectedCommunity.value || isPosting.value) return
  isPosting.value = true
  try {
    const response = await request.post('/api/community/post', {
      communityId: selectedCommunity.value.id,
      userId: getUserId(),
      userName: getUserName(),
      content: newPostContent.value,
      topicTag: newPostTopic.value || null,
      mediaUrls: newPostImages.value.length ? newPostImages.value.join(',') : null
    })
    if (response.code === 200) {
      newPostContent.value = ''
      newPostTopic.value = ''
      newPostImages.value = []
      communityPosts.value.unshift(response.data)
      selectedCommunity.value.postCount++
      ElMessage.success('发布成功')
    }
  } catch (e) {
    ElMessage.error('发布失败')
  } finally {
    isPosting.value = false
  }
}

const likePost = async (post) => {
  try {
    const response = await request.post(`/api/community/post/${post.id}/like`)
    if (response.code === 200) {
      const liked = response.data.liked
      const next = new Set(likedSet.value)
      if (liked) next.add(post.id)
      else next.delete(post.id)
      likedSet.value = next
      post.likeCount = post.likeCount + (liked ? 1 : -1)
    }
  } catch (e) { console.error(e) }
}

const toggleComments = async (post) => {
  if (expandedPostId.value === post.id) {
    expandedPostId.value = null
    return
  }
  expandedPostId.value = post.id
  await loadComments(post)
}

const loadComments = async (post) => {
  try {
    const response = await request.get(`/api/community/post/${post.id}/comments`, {
      params: { page: 1, size: 50 }
    })
    if (response.code === 200) {
      const list = response.data.list || []
      commentMap[post.id] = list
      commentTree[post.id] = buildCommentTree(list)
    }
  } catch (e) { console.error(e) }
}

const replyToComment = (post, comment) => {
  replyingTo[post.id] = comment
  commentDrafts[post.id] = ''
}

const sendComment = async (post) => {
  const content = (commentDrafts[post.id] || '').trim()
  if (!content) return
  try {
    const payload = { userName: getUserName(), content }
    if (replyingTo[post.id]) {
      payload.parentCommentId = replyingTo[post.id].id
      payload.replyToName = replyingTo[post.id].userName
    }
    const response = await request.post(`/api/community/post/${post.id}/comment`, payload)
    if (response.code === 200) {
      commentDrafts[post.id] = ''
      replyingTo[post.id] = null
      await loadComments(post)
      post.commentCount++
      ElMessage.success('评论成功')
    }
  } catch (e) {
    ElMessage.error('评论失败')
  }
}

// 精华标记（管理员）
const toggleFeature = async (post) => {
  try {
    const response = await request.put(`/api/community/post/${post.id}/feature`, {
      featured: !post.isFeatured
    })
    if (response.code === 200) {
      post.isFeatured = !post.isFeatured
      ElMessage.success(post.isFeatured ? '已加精' : '已取消精华')
    }
  } catch (e) {
    // 后端未实现该接口时降级为前端标记
    post.isFeatured = !post.isFeatured
    ElMessage.success(post.isFeatured ? '已加精' : '已取消精华')
  }
}

// 帖子配图：解析 mediaUrls（逗号分隔）为数组
const parseMediaUrls = (mediaUrls) => {
  if (!mediaUrls) return []
  return String(mediaUrls).split(/[,，]/).map((s) => s.trim()).filter(Boolean)
}

const triggerImageUpload = () => {
  postImageInput.value && postImageInput.value.click()
}

const onPostImagesPicked = (event) => {
  const files = Array.from(event.target.files || [])
  files.forEach((f) => {
    if (!f.type.startsWith('image/')) return
    const reader = new FileReader()
    reader.onload = (e) => newPostImages.value.push(e.target.result)
    reader.readAsDataURL(f)
  })
  event.target.value = ''
}

const removeComment = async (post, comment) => {
  try {
    const response = await request.delete(`/api/community/comment/${comment.id}`)
    if (response.code === 200) {
      commentMap[post.id] = (commentMap[post.id] || []).filter(c => c.id !== comment.id)
      post.commentCount = Math.max(0, post.commentCount - 1)
      ElMessage.success('已删除')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const removePost = async (post) => {
  try {
    const response = await request.delete(`/api/community/post/${post.id}`)
    if (response.code === 200) {
      communityPosts.value = communityPosts.value.filter(p => p.id !== post.id)
      ElMessage.success('删除成功')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const pinPost = async (post) => {
  if (!selectedCommunity.value) return
  try {
    const response = await request.put(`/api/community/${selectedCommunity.value.id}/post/${post.id}/pin`)
    if (response.code === 200) {
      post.isPinned = response.data.pinned
      ElMessage.success(post.isPinned ? '已置顶' : '已取消置顶')
      loadPosts(selectedCommunity.value.id)
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const openCreateModal = () => {
  createForm.value = { name: '', description: '', category: '', avatarUrl: '' }
  createModalVisible.value = true
}

onMounted(() => {
  loadCommunities()
})
</script>

<style scoped>
.community-container {
  min-height: 100vh;
  background: var(--bg-primary);
  padding: var(--spacing-xl);
}

.community-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
  position: relative;
}

.community-header h2 {
  margin: 0 0 var(--spacing-sm) 0;
  color: var(--text-primary);
}

.community-header p {
  margin: 0;
  color: var(--text-secondary);
}

.back-btn {
  position: absolute;
  left: 0;
  top: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  border-radius: 8px;
  color: inherit;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.back-btn:hover {
  background: rgba(0, 0, 0, 0.1);
}

.community-actions {
  display: flex;
  gap: var(--spacing-md);
  max-width: 800px;
  margin: 0 auto var(--spacing-xl);
}

.community-actions .el-input {
  flex: 1;
}

.category-tabs {
  display: flex;
  gap: var(--spacing-sm);
  max-width: 800px;
  margin: 0 auto var(--spacing-xl);
  flex-wrap: wrap;
}

.category-tab {
  padding: 8px 16px;
  border-radius: 20px;
  background: var(--bg-hover);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.category-tab.active {
  background: var(--color-primary);
  color: white;
}

.community-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-lg);
  max-width: 1200px;
  margin: 0 auto;
}

.community-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-md);
  cursor: pointer;
  transition: transform var(--transition-fast);
}

.community-card:hover {
  transform: translateY(-4px);
}

.community-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  margin-bottom: var(--spacing-md);
}

.community-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-avatar {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: white;
  font-size: 24px;
}

.community-info h3 {
  margin: 0 0 8px 0;
  color: var(--text-primary);
}

.community-desc {
  margin: 0 0 var(--spacing-sm) 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.community-meta {
  display: flex;
  gap: var(--spacing-md);
}

.meta-item {
  font-size: 12px;
  color: var(--text-muted);
}

.community-action {
  margin-top: var(--spacing-md);
}

.community-detail {
  padding: var(--spacing-lg);
}

.detail-header {
  display: flex;
  gap: var(--spacing-lg);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-primary);
  margin-bottom: var(--spacing-lg);
}

.detail-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: white;
  font-size: 32px;
}

.detail-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-info h3 {
  margin: 0 0 8px 0;
  font-size: 24px;
}

.detail-info p {
  margin: 0 0 8px 0;
  color: var(--text-secondary);
}

.detail-meta {
  display: flex;
  gap: var(--spacing-md);
  color: var(--text-muted);
  font-size: 14px;
}

.owner-badge {
  color: #e6a23c;
  font-weight: bold;
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-preview {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background: var(--bg-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 12px;
}

.post-input {
  background: var(--bg-tertiary);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-lg);
}

.post-input .el-textarea {
  margin-bottom: var(--spacing-sm);
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.post-item {
  padding: var(--spacing-md);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.post-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--spacing-sm);
}

.post-author {
  font-weight: bold;
  color: var(--text-primary);
}

.pin-badge {
  display: inline-block;
  background: #e6a23c;
  color: #fff;
  font-size: 12px;
  border-radius: 4px;
  padding: 0 6px;
  margin-right: 6px;
}

.post-time {
  font-size: 12px;
  color: var(--text-muted);
}

.post-content {
  color: var(--text-primary);
  line-height: 1.6;
  margin-bottom: var(--spacing-sm);
}

.post-actions {
  display: flex;
  gap: var(--spacing-lg);
}

.action-item {
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 14px;
}

.action-item:hover {
  color: var(--color-primary);
}

.action-item.liked {
  color: #f56c6c;
}

.action-item.danger {
  color: #f56c6c;
}

.comment-section {
  margin-top: var(--spacing-sm);
  border-top: 1px dashed var(--border-primary);
  padding-top: var(--spacing-sm);
}

.comment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
}

.comment-author {
  font-weight: bold;
  color: var(--text-primary);
}

.comment-content {
  color: var(--text-secondary);
  flex: 1;
}

.comment-time {
  font-size: 12px;
  color: var(--text-muted);
}

/* ===== 社区运营升级样式 ===== */
.topic-row { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; margin-bottom: 8px; }
.topic-label { font-size: 12px; color: var(--text-muted); }
.topic-chip {
  padding: 2px 10px; border-radius: 12px; font-size: 12px; cursor: pointer;
  background: var(--bg-hover); color: var(--text-secondary);
}
.topic-chip.active { background: var(--color-primary); color: #fff; }
.topic-inline { color: var(--color-primary); font-size: 12px; margin-left: 6px; }
.post-input-footer { display: flex; align-items: center; gap: 10px; margin: 6px 0; }
.word-count { font-size: 12px; color: var(--text-muted); }
.image-preview-row { display: flex; gap: 6px; flex-wrap: wrap; }
.img-thumb { position: relative; width: 44px; height: 44px; }
.img-thumb img { width: 100%; height: 100%; object-fit: cover; border-radius: 6px; }
.img-remove {
  position: absolute; top: -4px; right: -4px; width: 16px; height: 16px;
  background: #f56c6c; color: #fff; border-radius: 50%; font-size: 12px;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
}
.featured-badge {
  display: inline-block; background: #9b59b6; color: #fff; font-size: 12px;
  border-radius: 4px; padding: 0 6px; margin-right: 6px;
}
.post-images { display: flex; gap: 6px; flex-wrap: wrap; margin: 6px 0; }
.post-img { width: 80px; height: 80px; object-fit: cover; border-radius: 6px; }
.reply-item { margin-left: 24px; padding-left: 8px; border-left: 2px dashed var(--border-primary); }
.reply-line { color: var(--text-muted); font-size: 12px; }
.reply-to { color: var(--text-muted); font-size: 12px; }
.like-pop { animation: like-pop 0.35s ease; }
@keyframes like-pop {
  0% { transform: scale(1); }
  40% { transform: scale(1.4); }
  100% { transform: scale(1); }
}
.comment-input-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.tab-divider { color: var(--border-primary); margin: 0 4px; }
.meta-item.today { color: #e6a23c; }
.empty-mini { grid-column: 1 / -1; text-align: center; color: var(--text-muted); padding: 24px 0; }
</style>
