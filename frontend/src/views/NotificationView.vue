<template>
  <div class="notification-container">
    <div class="notification-header">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <h1>通知中心</h1>
      <div class="header-actions">
        <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }} 条未读</span>
        <button @click="handleRefresh" class="icon-btn" title="刷新">
          <el-icon><Refresh /></el-icon>
        </button>
        <button @click="openSettings" class="icon-btn" title="通知设置">
          <el-icon><Setting /></el-icon>
        </button>
        <button @click="markAllAsRead" class="btn-mark-all">全部已读</button>
        <button @click="handleClearAll" class="btn-clear-all">清空</button>
      </div>
    </div>

    <div class="notification-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab-btn', { active: activeTab === tab.key }]"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
        <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <div class="notifications-list">
      <!-- 首次加载骨架 -->
      <div v-if="loading && notifications.length === 0" class="loading-state">
        <div class="skeleton-item" v-for="i in 4" :key="i"></div>
      </div>

      <!-- 加载失败 -->
      <div v-else-if="loadError && notifications.length === 0" class="error-state">
        <p>加载失败，请重试</p>
        <button @click="loadFirstPage" class="retry-btn">重试</button>
      </div>

      <!-- 时间分组 -->
      <template v-else>
        <div v-for="group in groupedNotifications" :key="group.label" class="date-group">
          <div class="group-title">{{ group.label }}</div>
          <div
            v-for="notification in group.items"
            :key="notification.id"
            :class="['notification-item', { unread: !notification.isRead }]"
            @click="handleItemClick(notification)"
          >
            <div class="notification-icon">
              <span>{{ getNotificationIcon(notification.type) }}</span>
            </div>
            <div class="notification-content">
              <div class="notification-title">
                <span class="actor-name">{{ notification.actorName }}</span>
                <span>{{ getActionText(notification.type) }}</span>
              </div>
              <div class="notification-message">{{ notification.message }}</div>
              <div class="notification-time">{{ formatNotificationTime(notification.createdAt) }}</div>
            </div>
            <button @click.stop="handleDelete(notification)" class="delete-btn">删除</button>
          </div>
        </div>

        <!-- 空态 -->
        <div v-if="groupedNotifications.length === 0" class="empty-state">
          <span>📭</span>
          <p>暂无通知</p>
        </div>

        <!-- 触底加载哨兵 -->
        <div ref="sentinelEl" class="sentinel"></div>

        <!-- 加载更多 / 没有更多 -->
        <div v-if="loadingMore" class="load-more-tip">加载中...</div>
        <div v-else-if="!hasMore && notifications.length > 0" class="load-more-tip end">没有更多了</div>
      </template>
    </div>

    <!-- 清空确认弹窗 -->
    <el-dialog v-model="clearDialogVisible" title="清空通知" width="320px" append-to-body>
      <p>确定要清空所有通知吗？此操作不可恢复。</p>
      <template #footer>
        <el-button @click="clearDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmClearAll">确定清空</el-button>
      </template>
    </el-dialog>

    <!-- 通知设置弹窗 -->
    <el-dialog v-model="settingsVisible" title="通知设置" width="360px" append-to-body>
      <div v-if="settingsLoading" class="settings-loading">加载中...</div>
      <div v-else class="settings-list">
        <div class="setting-row">
          <span>接收点赞通知</span>
          <el-switch v-model="settings.likeEnabled" />
        </div>
        <div class="setting-row">
          <span>接收评论通知</span>
          <el-switch v-model="settings.commentEnabled" />
        </div>
        <div class="setting-row">
          <span>接收分享通知</span>
          <el-switch v-model="settings.shareEnabled" />
        </div>
        <div class="setting-row">
          <span>接收系统通知</span>
          <el-switch v-model="settings.systemEnabled" />
        </div>
      </div>
      <template #footer>
        <el-button @click="settingsVisible = false">取消</el-button>
        <el-button type="primary" :loading="settingsSaving" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, onDeactivated, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Refresh, Setting } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { getCurrentUserId } from '@/utils/auth'
import {
  getNotifications,
  getUnreadCount,
  markAsRead as apiMarkAsRead,
  markAllAsRead as apiMarkAllAsRead,
  deleteNotification as apiDeleteNotification,
  clearAllNotifications as apiClearAll,
  getNotificationSettings,
  updateNotificationSettings
} from '@/api/notification'
import {
  groupByDate,
  formatNotificationTime,
  getNotificationIcon,
  getActionText,
  canJump,
  getTargetRoute
} from '@/utils/notification'

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

// ===== 状态 =====
const notifications = ref([])
const unreadCount = ref(0)
const activeTab = ref('all')
const page = ref(1)
const size = ref(20)
const hasMore = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const loadError = ref(false)

// 设置弹窗
const settingsVisible = ref(false)
const settingsLoading = ref(false)
const settingsSaving = ref(false)
const settings = ref({
  likeEnabled: true,
  commentEnabled: true,
  shareEnabled: true,
  systemEnabled: true
})

// 清空确认弹窗
const clearDialogVisible = ref(false)

// 触底加载哨兵
const sentinelEl = ref(null)
let observer = null
let pollingTimer = null

// ===== Tabs（服务端筛选，count 用未读统计） =====
const tabs = computed(() => [
  { key: 'all', label: '全部', count: unreadCount.value },
  { key: 'like', label: '点赞', count: tabUnread.value.like || 0 },
  { key: 'comment', label: '评论', count: tabUnread.value.comment || 0 },
  { key: 'share', label: '分享', count: tabUnread.value.share || 0 },
  { key: 'system', label: '系统', count: tabUnread.value.system || 0 }
])
const tabUnread = ref({})

// ===== 时间分组 =====
const groupedNotifications = computed(() => groupByDate(notifications.value))

// ===== 加载列表 =====
async function fetchUnreadCount () {
  const userId = getCurrentUserId()
  if (!userId) return
  try {
    const res = await getUnreadCount(userId)
    if (res.code === 200) {
      unreadCount.value = Number(res.data) || 0
    }
  } catch (e) {
    // 静默失败，不打断用户
  }
}

async function loadFirstPage () {
  const userId = getCurrentUserId()
  if (!userId) return
  loading.value = true
  loadError.value = false
  page.value = 1
  try {
    const res = await getNotifications(userId, page.value, size.value, activeTab.value)
    if (res.code === 200) {
      const data = res.data || {}
      notifications.value = data.records || []
      hasMore.value = !!data.hasMore
    } else {
      loadError.value = true
    }
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
  fetchUnreadCount()
}

async function loadMore () {
  const userId = getCurrentUserId()
  if (!userId || loadingMore.value || loading.value || !hasMore.value) return
  loadingMore.value = true
  const nextPage = page.value + 1
  try {
    const res = await getNotifications(userId, nextPage, size.value, activeTab.value)
    if (res.code === 200) {
      const data = res.data || {}
      notifications.value = notifications.value.concat(data.records || [])
      page.value = nextPage
      hasMore.value = !!data.hasMore
    }
  } catch (e) {
    // 加载更多失败不弹错，下次滚动再试
  } finally {
    loadingMore.value = false
  }
}

// ===== Tab 切换 =====
function switchTab (key) {
  if (activeTab.value === key) return
  activeTab.value = key
  loadFirstPage()
}

// ===== 刷新 =====
function handleRefresh () {
  loadFirstPage()
  ElMessage.success('已刷新')
}

// ===== 点击通知：标已读 + 跳转 =====
async function handleItemClick (notification) {
  if (!notification) return
  // 1. 标记已读
  if (!notification.isRead) {
    const userId = getCurrentUserId()
    try {
      await apiMarkAsRead(userId, notification.id)
      notification.isRead = true
      if (unreadCount.value > 0) unreadCount.value--
    } catch (e) {
      // 标已读失败仍允许跳转
    }
  }
  // 2. 跳转（M4 ⑭：按通知类型路由——好友申请→/friends，点赞评论→社区帖子，AI提醒→/chat，系统公告→公告详情）
  if (canJump(notification)) {
    const routePath = getTargetRoute(notification)
    if (routePath) {
      const resolved = router.resolve(routePath)
      if (resolved.matched.length > 0) {
        router.push(routePath)
      }
      // 路由不存在则仅标已读，不报错
    }
  }
}

// ===== 全部已读 =====
async function markAllAsRead () {
  const userId = getCurrentUserId()
  if (!userId) return
  try {
    await apiMarkAllAsRead(userId)
    notifications.value.forEach(n => { n.isRead = true })
    unreadCount.value = 0
    ElMessage.success('已全部标为已读')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// ===== 删除单条 =====
async function handleDelete (notification) {
  try {
    await ElMessageBox.confirm('确定删除这条通知吗？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return // 用户取消
  }
  const userId = getCurrentUserId()
  try {
    await apiDeleteNotification(userId, notification.id)
    if (!notification.isRead && unreadCount.value > 0) unreadCount.value--
    notifications.value = notifications.value.filter(n => n.id !== notification.id)
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// ===== 清空全部 =====
function handleClearAll () {
  clearDialogVisible.value = true
}

async function confirmClearAll () {
  clearDialogVisible.value = false
  const userId = getCurrentUserId()
  if (!userId) return
  try {
    await apiClearAll(userId)
    notifications.value = []
    unreadCount.value = 0
    hasMore.value = false
    ElMessage.success('已清空')
  } catch (e) {
    ElMessage.error('清空失败')
  }
}

// ===== 通知设置 =====
async function openSettings () {
  settingsVisible.value = true
  settingsLoading.value = true
  const userId = getCurrentUserId()
  try {
    const res = await getNotificationSettings(userId)
    if (res.code === 200 && res.data) {
      settings.value = {
        likeEnabled: !!res.data.likeEnabled,
        commentEnabled: !!res.data.commentEnabled,
        shareEnabled: !!res.data.shareEnabled,
        systemEnabled: !!res.data.systemEnabled
      }
    }
  } catch (e) {
    // 加载失败用默认值
  } finally {
    settingsLoading.value = false
  }
}

async function saveSettings () {
  const userId = getCurrentUserId()
  settingsSaving.value = true
  try {
    const res = await updateNotificationSettings(userId, settings.value)
    if (res.code === 200) {
      ElMessage.success('设置已保存')
      settingsVisible.value = false
    } else {
      ElMessage.error('保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    settingsSaving.value = false
  }
}

// ===== 触底加载（IntersectionObserver） =====
function setupObserver () {
  if (!sentinelEl.value) return
  observer = new IntersectionObserver(entries => {
    if (entries[0].isIntersecting) {
      loadMore()
    }
  }, { rootMargin: '200px' })
  observer.observe(sentinelEl.value)
}

// ===== 生命周期 =====
function startPolling () {
  stopPolling()
  pollingTimer = setInterval(() => {
    fetchUnreadCount()
  }, 30000)
}

function stopPolling () {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

function onWindowFocus () {
  fetchUnreadCount()
}

onMounted(async () => {
  await loadFirstPage()
  await nextTick()
  setupObserver()
  startPolling()
  window.addEventListener('focus', onWindowFocus)
})

onActivated(() => {
  fetchUnreadCount()
  startPolling()
})

onDeactivated(() => {
  stopPolling()
})

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
  stopPolling()
  window.removeEventListener('focus', onWindowFocus)
})

// 暴露给模板的工具函数（保持与原有命名一致的外部行为）
// getNotificationIcon / getActionText / formatNotificationTime 已从 utils 引入
</script>

<style scoped>
.notification-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.notification-header h1 {
  font-size: 24px;
  font-weight: bold;
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
}
.back-btn:hover {
  background: rgba(0, 0, 0, 0.1);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.unread-badge {
  background: #ff4d4f;
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
}
.icon-btn:hover {
  background: rgba(0, 0, 0, 0.1);
}

.btn-mark-all {
  background: #667eea;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
}
.btn-mark-all:hover {
  opacity: 0.9;
}

.btn-clear-all {
  background: #fff;
  color: #ff4d4f;
  border: 1px solid #ff4d4f;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
}
.btn-clear-all:hover {
  background: #fff1f0;
}

.notification-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  background: #f5f5f5;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.tab-count {
  background: rgba(255, 255, 255, 0.3);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.date-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.group-title {
  font-size: 13px;
  color: #999;
  font-weight: 600;
  padding-left: 4px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: white;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.notification-item.unread {
  background: #fefefe;
  border-left: 4px solid #667eea;
}

.notification-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
}

.notification-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.actor-name {
  font-weight: bold;
  color: #333;
}

.notification-message {
  color: #666;
  font-size: 14px;
  margin-bottom: 4px;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

.delete-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 4px;
}

.delete-btn:hover {
  color: #ff4d4f;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state span {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-item {
  height: 72px;
  border-radius: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.error-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.retry-btn {
  margin-top: 12px;
  padding: 8px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
}

.sentinel {
  height: 10px;
}

.load-more-tip {
  text-align: center;
  padding: 12px;
  color: #999;
  font-size: 13px;
}

.load-more-tip.end {
  color: #bbb;
}

.settings-loading {
  text-align: center;
  padding: 20px;
  color: #999;
}

.settings-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.setting-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
