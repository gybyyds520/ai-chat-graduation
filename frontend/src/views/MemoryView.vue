<template>
  <div class="memory-view">
    <div class="memory-header">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <div>
        <h2>AI 记忆</h2>
        <p class="subtitle">管理 AI 沉淀的长期记忆，记忆会在聊天时按重要性注入上下文</p>
      </div>
      <div class="header-tools">
        <el-select v-model="sourceFilter" placeholder="全部来源" clearable class="source-select" @change="fetchList">
          <el-option v-for="opt in sourceOptions" :key="opt.value || 'all'" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索记忆内容…"
          clearable
          class="search-input"
          @keyup.enter="fetchList"
          @clear="fetchList"
        >
          <template #append>
            <el-button @click="fetchList">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </el-button>
          </template>
        </el-input>
        <el-button @click="onExport" :icon="Download">导出</el-button>
        <el-button type="primary" @click="openCreate">添加记忆</el-button>
      </div>
    </div>

    <!-- 分类筛选 Tab（市场级升级：按 FACT/PREFERENCE/EVENT/RELATIONSHIP 浏览） -->
    <div class="category-tabs">
      <div
        v-for="opt in categoryOptions"
        :key="opt.value || 'all'"
        class="cat-tab"
        :class="{ active: currentCategory === opt.value }"
        @click="onCategoryChange(opt.value)"
      >
        {{ opt.label }}
      </div>
    </div>

    <!-- 全局记忆开关 -->
    <div class="global-switch-bar">
      <span class="global-switch-label">跨会话长期记忆</span>
      <el-switch
        v-model="memoryEnabled"
        @change="onGlobalToggle"
        active-text="开启"
        inactive-text="关闭"
        :active-value="true"
        :inactive-value="false"
      />
      <span class="global-switch-hint">{{ memoryEnabled ? 'AI 会在对话中参考以下记忆' : '已关闭：AI 不再读取或注入以下记忆' }}</span>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-bar">
      <el-card shadow="never" class="stat-card">
        <div class="stat-num">{{ stats.total ?? 0 }}</div>
        <div class="stat-label">总记忆数</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-num">{{ activeCount }}</div>
        <div class="stat-label">生效中</div>
      </el-card>
      <div class="type-chips">
        <el-tag v-for="(count, type) in stats.typeCounts" :key="type" size="small" class="chip">
          {{ typeLabel(type) }} {{ count }}
        </el-tag>
      </div>
    </div>

    <div class="memory-body">
      <!-- 左侧类型筛选 -->
      <aside class="filter-panel">
        <div
          v-for="opt in typeOptions"
          :key="opt.value || 'all'"
          class="filter-item"
          :class="{ active: currentType === opt.value }"
          @click="onFilterChange(opt.value)"
        >
          {{ opt.label }}
        </div>
      </aside>

      <!-- 右侧记忆列表 -->
      <section class="memory-list">
        <!-- 批量操作栏 -->
        <div v-if="selectedIds.length > 0" class="batch-bar">
          <span>已选 {{ selectedIds.length }} 条</span>
          <el-button size="small" @click="clearSelection">取消选择</el-button>
          <el-button size="small" type="danger" @click="askBatchDelete">批量删除</el-button>
        </div>
        <el-empty v-if="!loading && records.length === 0" description="暂无记忆，点击右上角添加第一条" />
        <div v-else class="card-list">
          <div v-for="m in records" :key="m.id" class="memory-card">
            <div class="card-head">
              <el-checkbox
                :model-value="selectedIds.includes(m.id)"
                @change="(val) => toggleSelect(m.id, val)"
              />
              <el-tag :type="typeTagType(m.memoryType)" size="small">{{ typeLabel(m.memoryType) }}</el-tag>
              <el-tag v-if="m.category && isEnumCategory(m.category)" size="small" type="warning" effect="plain">
                {{ categoryLabel(m.category) }}
              </el-tag>
              <span class="stars">{{ importanceStars(m.importance) }}</span>
              <span class="time">{{ formatDateTime(m.createdAt) }}</span>
            </div>
            <div class="card-content" :class="{ expanded: expandedId === m.id }">
              {{ m.content }}
            </div>
            <div class="card-foot">
              <el-tag v-if="m.category" size="small" type="info" effect="plain">{{ m.category }}</el-tag>
              <span class="foot-spacer" />
              <span class="src">来源：{{ sourceLabel(m.source) }}</span>
              <span v-if="m.lastUsedAt" class="used-at">最近使用 {{ formatDateTime(m.lastUsedAt) }}</span>
              <el-switch
                :model-value="m.isActive"
                @change="() => onToggle(m)"
                size="small"
                active-text="生效"
              />
              <el-button size="small" @click="openEdit(m)">编辑</el-button>
              <el-button size="small" type="danger" @click="askDelete(m)">删除</el-button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑记忆' : '添加记忆'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="记录关于用户的事实/偏好" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.memoryType" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value || 'other'" />
          </el-select>
        </el-form-item>
        <el-form-item label="重要性">
          <el-rate v-model="form.importance" :max="5" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="可选，如：工作/饮食" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 删除确认 -->
    <el-dialog v-model="deleteVisible" title="删除记忆" width="360px">
      <p>确定删除这条记忆吗？删除后不可恢复。</p>
      <template #footer>
        <el-button @click="deleteVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete">删除</el-button>
      </template>
    </el-dialog>

    <!-- 批量删除确认 -->
    <el-dialog v-model="batchDeleteVisible" title="批量删除记忆" width="380px">
      <p>确定删除选中的 {{ selectedIds.length }} 条记忆吗？删除后不可恢复。</p>
      <template #footer>
        <el-button @click="batchDeleteVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmBatchDelete">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  listMemories as apiList,
  getMemoryStats as apiStats,
  createMemory as apiCreate,
  updateMemory as apiUpdate,
  deleteMemory as apiDelete,
  toggleMemory as apiToggle,
  getMemorySettings as apiGetSettings,
  updateMemorySettings as apiUpdateSettings,
  batchDeleteMemories as apiBatchDelete,
  exportMemories as apiExport
} from '@/api/memory'
import { typeLabel, sourceLabel, importanceStars, typeTagType, formatDateTime, buildMemorySearchParams, categoryLabel, MEMORY_CATEGORY_MAP } from '@/utils/memoryUtils'
import { MEMORY_TYPE_OPTIONS, MEMORY_SOURCE_OPTIONS, MEMORY_CATEGORY_OPTIONS } from '@/utils/memoryUtils'

const loading = ref(false)
const router = useRouter()
const goBack = () => {
  const fromRoute = router.options.history.state.value?.back
  if (fromRoute && fromRoute.fullPath) {
    router.back()
  } else {
    router.push('/chat')
  }
}
const records = ref([])
const stats = ref({ total: 0, typeCounts: {} })
const currentType = ref('')
const currentCategory = ref('')
const keyword = ref('')
const sourceFilter = ref('')
const memoryEnabled = ref(true)

const typeOptions = MEMORY_TYPE_OPTIONS
const sourceOptions = MEMORY_SOURCE_OPTIONS
const categoryOptions = MEMORY_CATEGORY_OPTIONS

// 批量选择
const selectedIds = ref([])
const batchDeleteVisible = ref(false)

const expandedId = ref(null)

const statsList = ref([])

async function fetchList() {
  loading.value = true
  try {
    const params = buildMemorySearchParams(keyword.value, sourceFilter.value, currentType.value, 1, 50)
    if (currentCategory.value) params.category = currentCategory.value
    const res = await apiList(params)
    records.value = res.data?.records || []
  } catch (e) {
    ElMessage.error('加载记忆失败')
  } finally {
    loading.value = false
  }
}

function onCategoryChange(value) {
  currentCategory.value = value
  clearSelection()
  fetchList()
}

function isEnumCategory(cat) {
  return Object.prototype.hasOwnProperty.call(MEMORY_CATEGORY_MAP, cat)
}

function toggleSelect(id, checked) {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter((x) => x !== id)
  }
}

function clearSelection() {
  selectedIds.value = []
}

function askBatchDelete() {
  if (selectedIds.value.length === 0) return
  batchDeleteVisible.value = true
}

async function confirmBatchDelete() {
  try {
    await apiBatchDelete(selectedIds.value)
    ElMessage.success('批量删除成功')
    batchDeleteVisible.value = false
    clearSelection()
    fetchList()
    fetchStats()
  } catch (e) {
    ElMessage.error('批量删除失败')
  }
}

async function onExport() {
  try {
    const res = await apiExport()
    const data = res.data || []
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'ai_memory_export.json'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('已导出 ' + data.length + ' 条记忆')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

async function fetchStats() {
  try {
    const res = await apiStats()
    stats.value = res.data || { total: 0, typeCounts: {} }
  } catch (e) {
    /* 统计加载失败不阻塞列表 */
  }
}

async function fetchSettings() {
  try {
    const res = await apiGetSettings()
    memoryEnabled.value = res.data?.enabled !== false
  } catch (e) {
    memoryEnabled.value = true
  }
}

async function onGlobalToggle(val) {
  try {
    await apiUpdateSettings(val)
    ElMessage.success(val ? '已开启跨会话记忆' : '已关闭跨会话记忆，AI 将不再引用')
  } catch (e) {
    memoryEnabled.value = !val
    ElMessage.error('设置保存失败')
  }
}

const activeCount = computed(() => {
  return records.value.filter((m) => m.isActive).length
})

function onFilterChange(value) {
  currentType.value = value
  fetchList()
}

// 弹窗
const dialogVisible = ref(false)
const editing = ref(false)
const form = ref({ content: '', memoryType: 'preference', importance: 3, category: '' })

function openCreate() {
  editing.value = false
  form.value = { content: '', memoryType: 'preference', importance: 3, category: '' }
  dialogVisible.value = true
}

function openEdit(m) {
  editing.value = true
  form.value = {
    content: m.content,
    memoryType: m.memoryType,
    importance: m.importance,
    category: m.category || ''
  }
  editingId.value = m.id
  dialogVisible.value = true
}

const editingId = ref(null)

async function onSubmit() {
  if (!form.value.content || !form.value.content.trim()) {
    ElMessage.warning('请填写记忆内容')
    return
  }
  try {
    if (editing.value) {
      await apiUpdate(editingId.value, { ...form.value })
      ElMessage.success('已更新')
    } else {
      await apiCreate({ ...form.value })
      ElMessage.success('已添加')
    }
    dialogVisible.value = false
    fetchList()
    fetchStats()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const deleteVisible = ref(false)
const deletingId = ref(null)

function askDelete(m) {
  deletingId.value = m.id
  deleteVisible.value = true
}

async function confirmDelete() {
  try {
    await apiDelete(deletingId.value)
    ElMessage.success('已删除')
    deleteVisible.value = false
    fetchList()
    fetchStats()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

async function onToggle(m) {
  try {
    await apiToggle(m.id)
    m.isActive = !m.isActive
  } catch (e) {
    ElMessage.error('切换失败')
  }
}

onMounted(() => {
  fetchList()
  fetchStats()
  fetchSettings()
})
</script>

<style scoped>
.memory-view {
  padding: 20px;
  max-width: 1100px;
  margin: 0 auto;
}
.memory-header {
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
.header-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}
.source-select {
  width: 130px;
}
.search-input {
  width: 240px;
}
.used-at {
  color: #b1b3b8;
  font-size: 12px;
  margin-right: 4px;
}
.memory-header h2 {
  margin: 0;
}
.subtitle {
  color: #909399;
  margin: 4px 0 0;
  font-size: 13px;
}
.stats-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin: 16px 0;
}
.global-switch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 4px 0 8px;
  padding: 10px 14px;
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 8px;
}
.global-switch-label {
  font-weight: 600;
  color: #303133;
}
.global-switch-hint {
  color: #909399;
  font-size: 12px;
}
.stat-card {
  width: 110px;
  text-align: center;
}
.stat-num {
  font-size: 24px;
  font-weight: 600;
}
.stat-label {
  color: #909399;
  font-size: 12px;
}
.type-chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.memory-body {
  display: flex;
  gap: 16px;
}
.filter-panel {
  width: 140px;
  border-right: 1px solid #ebeef5;
  padding-right: 12px;
}
.filter-item {
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  color: #606266;
}
.filter-item.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}
.memory-list {
  flex: 1;
}
.memory-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 10px;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.stars {
  color: #e6a23c;
  letter-spacing: 2px;
}
.time {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}
.card-content {
  margin: 8px 0;
  color: #303133;
}
.card-foot {
  display: flex;
  align-items: center;
  gap: 8px;
}
.foot-spacer {
  flex: 1;
}
.src {
  color: #909399;
  font-size: 12px;
}
.category-tabs {
  display: flex;
  gap: 8px;
  margin: 12px 0 4px;
  flex-wrap: wrap;
}
.cat-tab {
  padding: 6px 14px;
  border-radius: 16px;
  background: #f4f4f5;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.cat-tab.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin-bottom: 10px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 8px;
  font-size: 13px;
  color: #e6a23c;
}
</style>
