<template>
  <div class="announcement-container">
    <button class="back-btn" @click="goBack">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M19 12H5M12 19l-7-7 7-7"/>
      </svg>
      返回
    </button>

    <div class="page-header">
      <h1 class="page-title">系统公告</h1>
      <p class="page-subtitle">查看最新的系统通知和重要消息</p>
    </div>

    <div class="announcement-list">
      <div 
        v-for="announcement in announcements" 
        :key="announcement.id"
        :class="['announcement-item', { active: selectedId === announcement.id }]"
        @click="selectAnnouncement(announcement)"
      >
        <div class="announcement-header">
          <div class="priority-badge" :class="getPriorityClass(announcement.priority)">
            {{ getPriorityText(announcement.priority) }}
          </div>
          <span class="announcement-status" :class="announcement.status.toLowerCase()">
            {{ announcement.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </span>
        </div>
        <h3 class="announcement-title">{{ announcement.title }}</h3>
        <p class="announcement-preview">{{ getPreview(announcement.content) }}</p>
        <div class="announcement-meta">
          <span class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            {{ formatDate(announcement.publishTime || announcement.createdAt) }}
          </span>
          <span class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            {{ announcement.author || '系统' }}
          </span>
          <span class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z"/>
              <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
            </svg>
            {{ announcement.viewCount || 0 }}
          </span>
        </div>
      </div>

      <div v-if="announcements.length === 0" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
          <polyline points="10 9 9 9 8 9"/>
        </svg>
        <p>暂无公告</p>
      </div>
    </div>

    <div v-if="selectedAnnouncement" class="announcement-detail">
      <div class="detail-header">
        <div class="priority-badge" :class="getPriorityClass(selectedAnnouncement.priority)">
          {{ getPriorityText(selectedAnnouncement.priority) }}
        </div>
        <span class="detail-status" :class="selectedAnnouncement.status.toLowerCase()">
          {{ selectedAnnouncement.status === 'PUBLISHED' ? '已发布' : '草稿' }}
        </span>
      </div>
      <h2 class="detail-title">{{ selectedAnnouncement.title }}</h2>
      <div class="detail-meta">
        <span>{{ selectedAnnouncement.author || '系统' }}</span>
        <span>{{ formatDate(selectedAnnouncement.publishTime || selectedAnnouncement.createdAt) }}</span>
        <span>浏览 {{ selectedAnnouncement.viewCount || 0 }} 次</span>
      </div>
      <div class="detail-content" v-html="sanitizeHtml(selectedAnnouncement.content)"></div>
      <div v-if="selectedAnnouncement.startTime || selectedAnnouncement.endTime" class="detail-time">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
          <line x1="16" y1="2" x2="16" y2="6"/>
          <line x1="8" y1="2" x2="8" y2="6"/>
          <line x1="3" y1="10" x2="21" y2="10"/>
        </svg>
        <span v-if="selectedAnnouncement.startTime">生效时间：{{ formatDateTime(selectedAnnouncement.startTime) }}</span>
        <span v-if="selectedAnnouncement.endTime">到期时间：{{ formatDateTime(selectedAnnouncement.endTime) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import logger from '@/utils/logger'

const router = useRouter()

const announcements = ref([])
const selectedId = ref(null)
const selectedAnnouncement = ref(null)

const loadAnnouncements = async () => {
  try {
    const response = await request.get('/api/admin/announcements/public/active')
    if (response.code === 200 && response.data) {
      announcements.value = response.data
      if (announcements.value.length > 0) {
        selectAnnouncement(announcements.value[0])
      }
    }
  } catch (error) {
    logger.error('加载公告列表失败:', error)
  }
}

const selectAnnouncement = (announcement) => {
  selectedId.value = announcement.id
  selectedAnnouncement.value = announcement
}

const getPriorityClass = (priority) => {
  if (priority === 1) return 'priority-high'
  if (priority === 2) return 'priority-medium'
  return 'priority-low'
}

const getPriorityText = (priority) => {
  if (priority === 1) return '重要'
  if (priority === 2) return '一般'
  return '普通'
}

const getPreview = (content) => {
  if (!content) return ''
  const text = content.replace(/<[^>]*>/g, '')
  return text.length > 100 ? text.substring(0, 100) + '...' : text
}

// 安全修复（P1）：公告内容以 HTML 渲染，先剥离脚本/事件属性/javascript: 链接，
// 防止存储型 XSS（即使管理员账号被攻破，也无法借公告投毒其他用户）
const sanitizeHtml = (html) => {
  if (!html) return ''
  let safe = String(html)
  // 移除 script/style/iframe/object/embed 标签及其内容
  safe = safe.replace(/<script[\s\S]*?<\/script>/gi, '')
  safe = safe.replace(/<style[\s\S]*?<\/style>/gi, '')
  safe = safe.replace(/<iframe[\s\S]*?<\/iframe>/gi, '')
  safe = safe.replace(/<object[\s\S]*?<\/object>/gi, '')
  safe = safe.replace(/<embed[\s\S]*?>/gi, '')
  // 移除所有 on* 事件属性
  safe = safe.replace(/\son\w+\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)/gi, '')
  // 移除 javascript:/vbscript: 协议链接
  safe = safe.replace(/(href|src|xlink:href|action)\s*=\s*["']?\s*javascript:[^"'>\s]*/gi, '$1="#"')
  safe = safe.replace(/(href|src|xlink:href|action)\s*=\s*["']?\s*vbscript:[^"'>\s]*/gi, '$1="#"')
  return safe
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const goBack = () => {
  router.push('/profile')
}

onMounted(() => {
  loadAnnouncements()
})
</script>

<style scoped>
.announcement-container {
  min-height: 100vh;
  background: var(--bg-secondary);
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
}

.page-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 50%, #60a5fa 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

.announcement-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  max-width: 800px;
  margin: 0 auto;
  flex: 1;
}

.announcement-item {
  background: var(--gradient-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 2px solid transparent;
}

.announcement-item:hover {
  transform: translateX(4px);
  border-color: var(--color-primary);
}

.announcement-item.active {
  border-color: var(--color-primary);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(59, 130, 246, 0.05) 100%);
}

.announcement-header {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.priority-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.priority-high {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.priority-medium {
  background: rgba(234, 179, 8, 0.15);
  color: #eab308;
}

.priority-low {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.announcement-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.announcement-status.published {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.announcement-status.draft {
  background: rgba(156, 163, 175, 0.15);
  color: #9ca3af;
}

.announcement-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.announcement-preview {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: var(--spacing-sm);
}

.announcement-meta {
  display: flex;
  gap: var(--spacing-lg);
  font-size: 12px;
  color: var(--text-muted);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.empty-state {
  text-align: center;
  padding: var(--spacing-3xl);
  color: var(--text-muted);
}

.empty-state svg {
  margin-bottom: var(--spacing-md);
  opacity: 0.5;
}

.empty-state p {
  font-size: 14px;
}

.announcement-detail {
  max-width: 800px;
  margin: var(--spacing-xl) auto 0;
  background: var(--gradient-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
}

.detail-header {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
}

.detail-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.detail-status.published {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.detail-status.draft {
  background: rgba(156, 163, 175, 0.15);
  color: #9ca3af;
}

.detail-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--spacing-md);
}

.detail-meta {
  display: flex;
  gap: var(--spacing-xl);
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-primary);
}

.detail-content {
  font-size: 16px;
  color: var(--text-primary);
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-content :deep(p) {
  margin-bottom: var(--spacing-md);
}

.detail-content :deep(h2) {
  font-size: 20px;
  font-weight: 600;
  margin: var(--spacing-xl) 0 var(--spacing-lg);
  color: var(--text-primary);
}

.detail-content :deep(h3) {
  font-size: 18px;
  font-weight: 600;
  margin: var(--spacing-lg) 0 var(--spacing-md);
  color: var(--text-primary);
}

.detail-content :deep(ul),
.detail-content :deep(ol) {
  margin-left: var(--spacing-lg);
  margin-bottom: var(--spacing-md);
}

.detail-content :deep(li) {
  margin-bottom: var(--spacing-sm);
}

.detail-content :deep(strong) {
  font-weight: 600;
}

.detail-time {
  margin-top: var(--spacing-xl);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--border-primary);
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  font-size: 14px;
  color: var(--text-muted);
}

.back-btn {
  position: absolute;
  top: var(--spacing-lg);
  left: var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  z-index: 100;
}

.back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

@media (min-width: 1024px) {
  .announcement-container {
    flex-direction: row;
    gap: var(--spacing-xl);
  }
  
  .announcement-list {
    max-width: 400px;
    flex: none;
    height: calc(100vh - 100px);
    overflow-y: auto;
  }
  
  .announcement-detail {
    margin: var(--spacing-lg) 0 0;
    flex: 1;
    height: calc(100vh - 100px);
    overflow-y: auto;
  }
}
</style>