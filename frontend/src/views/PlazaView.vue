<template>
  <div class="plaza-container">
    <div class="plaza-header">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <div>
        <h2>角色广场</h2>
        <p>发现并与有趣的 AI 角色开始对话</p>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索角色名称、描述或标签"
        clearable
        @keyup.enter="onSearch"
        @clear="loadList"
      >
        <template #append>
          <el-button @click="onSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- 分类标签栏 -->
    <div class="category-bar">
      <span
        v-for="cat in categoriesWithAll"
        :key="cat"
        :class="['cat-tag', { active: category === cat }]"
        @click="selectCategory(cat)"
      >{{ cat }}</span>
    </div>

    <!-- 排序切换栏 -->
    <div class="sort-bar">
      <span class="sort-label">排序：</span>
      <span
        v-for="opt in sortOptions"
        :key="opt.value"
        :class="['sort-tag', { active: sortBy === opt.value }]"
        @click="changeSort(opt.value)"
      >{{ opt.label }}</span>
    </div>

    <div class="plaza-body">
      <!-- 角色卡片网格 -->
      <div class="card-grid">
        <div v-for="c in list" :key="c.id" class="char-card" @click="openDetail(c)">
          <img :src="c.avatar || getLocalAvatar(c)" class="card-avatar" />
          <div class="card-name">{{ c.name }}</div>
          <div class="card-desc">{{ c.description || c.prompt || '这位角色还没有介绍' }}</div>
          <div v-if="greetingSnippet(c.greeting)" class="card-greeting">
            💬 {{ greetingSnippet(c.greeting) }}
          </div>
          <div class="card-tags">
            <span v-for="t in parseTags(c.tags)" :key="t" class="tag">#{{ t }}</span>
          </div>
          <div class="card-footer">
            <span class="use-count">{{ formatUseCount(c.useCount) }} 次对话</span>
            <el-button type="primary" size="small" @click.stop="startChat(c)">开始对话</el-button>
          </div>
        </div>
        <div v-if="!list.length" class="empty">暂无公开角色，换个分类试试</div>
        <div v-if="list.length && hasMore" class="load-more">
          <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
        </div>
        <div v-else-if="list.length && !hasMore" class="no-more">已经到底啦～</div>
      </div>

      <!-- 热门排行侧边栏 -->
      <aside class="hot-side">
        <h3>🔥 热门排行</h3>
        <div v-for="(h, i) in hot" :key="h.id" class="hot-item" @click="openDetail(h)">
          <span class="rank" :class="{ top: i < 3 }">{{ i + 1 }}</span>
          <span class="hot-name">{{ h.name }}</span>
          <span class="hot-count">{{ formatUseCount(h.useCount) }}</span>
        </div>
      </aside>
    </div>

    <!-- 角色详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="current?.name" width="520px">
      <div v-if="current" class="detail">
        <img :src="current.avatar || getLocalAvatar(current)" class="detail-avatar" />
        <p class="detail-creator">by {{ current.creatorNickname || '匿名用户' }}</p>
        <p class="detail-desc">{{ current.description || current.prompt }}</p>
        <p v-if="current.personality" class="detail-section">
          <b>性格：</b>{{ current.personality }}
        </p>
        <p v-if="current.backstory" class="detail-section">
          <b>背景故事：</b>{{ current.backstory }}
        </p>
        <p v-if="current.greeting" class="detail-greeting">
          <b>开场白：</b>{{ current.greeting }}
        </p>
        <p v-if="current.exampleDialogue" class="detail-section">
          <b>对话示例：</b>{{ current.exampleDialogue }}
        </p>
        <div class="card-tags">
          <span v-for="t in parseTags(current.tags)" :key="t" class="tag">#{{ t }}</span>
        </div>
        <p class="detail-meta">使用 {{ formatUseCount(current.useCount) }} 次 · 评分 {{ current.rating || 0 }}</p>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="startChat(current)">开始对话</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { parseTags, formatUseCount, ALL_CATEGORIES } from '@/utils/plaza'
import { greetingSnippet } from '@/utils/chatExperience'
import { generateLocalAvatarSvg } from '@/utils/localAvatar'

const getLocalAvatar = (c) => generateLocalAvatarSvg(c?.name || 'default', c?.avatarStyle || 'avataaars')

const router = useRouter()
const goBack = () => {
  const fromRoute = router.options.history.state.value?.back
  if (fromRoute && fromRoute.fullPath) {
    router.back()
  } else {
    router.push('/chat')
  }
}

const list = ref([])
const hot = ref([])
const categories = ref([])
const category = ref(ALL_CATEGORIES)
const keyword = ref('')
const detailVisible = ref(false)
const current = ref(null)

const categoriesWithAll = ref([ALL_CATEGORIES])

// 排序 / 分页状态
const sortBy = ref('hot')
const sortOptions = [
  { value: 'hot', label: '热门' },
  { value: 'created', label: '最新' },
  { value: 'rating', label: '最多使用' }
]
const page = ref(1)
const hasMore = ref(true)
const loadingMore = ref(false)
const PAGE_SIZE = 12

const loadList = async (reset = true) => {
  try {
    if (reset) {
      page.value = 1
      list.value = []
    }
    const cat = category.value === ALL_CATEGORIES ? '' : category.value
    const res = await request.get('/api/plaza/list', {
      params: { category: cat || undefined, sortBy: sortBy.value, page: page.value, size: PAGE_SIZE }
    })
    if (res.code === 200) {
      const data = res.data || {}
      const items = data.list || []
      list.value = reset ? items : [...list.value, ...items]
      hasMore.value = data.hasMore != null ? !!data.hasMore : items.length >= PAGE_SIZE
    }
  } catch (e) {
    ElMessage.error('加载角色失败')
  }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    page.value += 1
    await loadList(false)
  } finally {
    loadingMore.value = false
  }
}

const changeSort = (val) => {
  if (sortBy.value === val) return
  sortBy.value = val
  loadList(true)
}

const loadHot = async () => {
  try {
    const res = await request.get('/api/plaza/hot', { params: { limit: 10 } })
    if (res.code === 200) hot.value = res.data || []
  } catch (e) { /* 忽略 */ }
}

const loadCategories = async () => {
  try {
    const res = await request.get('/api/plaza/categories')
    if (res.code === 200) categories.value = res.data || []
    categoriesWithAll.value = [ALL_CATEGORIES, ...categories.value]
  } catch (e) { /* 忽略 */ }
}

const onSearch = async () => {
  if (!keyword.value.trim()) return loadList(true)
  try {
    const res = await request.get('/api/plaza/search', { params: { keyword: keyword.value.trim(), page: 1, size: PAGE_SIZE } })
    if (res.code === 200) {
      list.value = res.data.list || []
      hasMore.value = !!res.data.hasMore
      page.value = 1
    }
  } catch (e) {
    ElMessage.error('搜索失败')
  }
}

const selectCategory = (cat) => {
  category.value = cat
  loadList(true)
}

const openDetail = async (c) => {
  current.value = c
  detailVisible.value = true
  // 拉取完整详情（backstory/personality/exampleDialogue/创建者昵称）
  try {
    const res = await request.get(`/api/plaza/${c.id}`)
    if (res.code === 200 && res.data && res.data.found) {
      current.value = { ...c, ...(res.data.character || {}), creatorNickname: res.data.creatorNickname }
    }
  } catch (e) { /* 忽略，使用卡片已有数据 */ }
}

const startChat = async (c) => {
  if (!c) return
  try {
    await request.post(`/api/plaza/${c.id}/use`)
  } catch (e) { /* 未登录时仅提示 */ }
  router.push({ path: '/chat', query: { characterId: c.id } })
}

onMounted(() => {
  loadList()
  loadHot()
  loadCategories()
})
</script>

<style scoped>
.plaza-container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
}
.plaza-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.back-btn {
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
  flex-shrink: 0;
}
.back-btn:hover {
  background: rgba(0, 0, 0, 0.1);
}
.plaza-header h2 { margin: 0 0 4px; }
.plaza-header p { color: var(--text-secondary); margin: 0 0 16px; }
.search-bar { margin-bottom: 12px; }
.category-bar { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.cat-tag {
  padding: 4px 12px; border-radius: 14px; cursor: pointer;
  background: var(--bg-tertiary); border: 1px solid var(--border-primary); font-size: 13px;
}
.cat-tag.active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
.sort-bar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.sort-label { color: var(--text-secondary); font-size: 13px; }
.sort-tag {
  padding: 3px 12px; border-radius: 12px; cursor: pointer; font-size: 13px;
  background: var(--bg-tertiary); border: 1px solid var(--border-primary);
}
.sort-tag.active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
.load-more { grid-column: 1 / -1; text-align: center; margin-top: 8px; }
.no-more { grid-column: 1 / -1; text-align: center; color: var(--text-muted); font-size: 13px; padding: 8px 0; }
.detail-creator { color: var(--text-muted); font-size: 13px; margin: 4px 0 0; }
.detail-section { width: 100%; text-align: left; color: var(--text-secondary); font-size: 14px; line-height: 1.6; }
.detail-section b { color: var(--text-primary); }
.plaza-body { display: flex; gap: 20px; }
.card-grid { flex: 1; display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
.char-card {
  background: var(--card-bg); border: 1px solid var(--border-primary);
  border-radius: 10px; padding: 14px; cursor: pointer; transition: box-shadow .2s;
}
.char-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.08); }
.card-avatar { width: 56px; height: 56px; border-radius: 50%; object-fit: cover; }
.avatar-fallback {
  display: flex; align-items: center; justify-content: center;
  background: var(--color-primary); color: #fff; font-size: 24px;
}
.card-name { font-weight: 600; margin: 8px 0 4px; }
.card-desc { font-size: 13px; color: var(--text-secondary); height: 38px; overflow: hidden; }
.card-greeting {
  font-size: 12px; color: var(--text-muted); margin: 4px 0;
  background: var(--bg-tertiary); padding: 4px 6px; border-radius: 4px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.card-tags { margin: 6px 0; }
.tag { font-size: 12px; color: var(--color-primary); margin-right: 6px; }
.card-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.use-count { font-size: 12px; color: var(--text-muted); }
.empty { color: var(--text-muted); }
.hot-side { width: 220px; flex-shrink: 0; }
.hot-side h3 { margin-top: 0; }
.hot-item { display: flex; align-items: center; gap: 8px; padding: 6px 0; cursor: pointer; font-size: 14px; }
.rank { width: 20px; font-weight: bold; color: var(--text-muted); }
.rank.top { color: #e6a23c; }
.hot-name { flex: 1; }
.hot-count { color: var(--text-muted); font-size: 12px; }
.detail { display: flex; flex-direction: column; align-items: center; }
.detail-avatar { width: 80px; height: 80px; border-radius: 50%; object-fit: cover; }
.detail-desc { text-align: center; color: var(--text-secondary); }
.detail-greeting { width: 100%; background: var(--bg-tertiary); padding: 8px; border-radius: 6px; }
.detail-meta { color: var(--text-muted); font-size: 13px; }
</style>
