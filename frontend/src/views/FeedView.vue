<template>
  <div class="feed-module">
    <!-- 头部 -->
    <div class="feed-header">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <div class="header-content">
        <h2 class="feed-title">🧠 AI 投喂中心</h2>
        <p class="feed-desc">向AI模型投喂训练数据，帮助模型学习和成长</p>
      </div>
      <div class="header-actions">
        <el-button
          size="small"
          @click="logViewerVisible = true"
          :icon="Clock"
          type="info"
          plain
        >
          主题日志
        </el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="feed-content">
      <!-- 左侧：投喂表单 -->
      <div class="feed-form-section">
        <div class="section-card">
          <h3 class="section-title">📤 投喂内容</h3>
          
          <!-- 内容类型选择 -->
          <div class="form-group">
            <label class="form-label">内容类型</label>
            <div class="content-type-selector">
              <button 
                v-for="type in contentTypes" 
                :key="type.value"
                :class="['type-btn', { active: formData.contentType === type.value }]"
                @click="formData.contentType = type.value"
              >
                <span class="type-icon">{{ type.icon }}</span>
                <span class="type-name">{{ type.label }}</span>
              </button>
            </div>
          </div>

          <!-- 文本输入 -->
          <div v-if="formData.contentType === 'text'" class="form-group">
            <label class="form-label">文本内容 <span class="required">*</span></label>
            <textarea
              v-model="formData.textContent"
              :rows="8"
              placeholder="请输入要投喂的文本内容..."
              :maxlength="maxTextLength"
              :disabled="isSubmitting"
              class="custom-textarea"
            ></textarea>
            <div class="form-hint">支持最大 {{ maxTextLength }} 字符</div>
            <div class="word-count">{{ formData.textContent.length }} / {{ maxTextLength }}</div>
          </div>

          <!-- 文件上传 -->
          <div v-else-if="formData.contentType === 'file'" class="form-group">
            <label class="form-label">上传文件 <span class="required">*</span></label>
            <el-upload
              class="upload-demo"
              :action="uploadUrl"
              :file-list="uploadFileList"
              :accept="allowedFileTypes"
              :max-file-size="maxFileSize"
              :before-upload="beforeFileUpload"
              :on-success="onFileUploadSuccess"
              :on-error="onFileUploadError"
              :on-progress="onUploadProgress"
              :disabled="isSubmitting"
              :headers="uploadHeaders"
              drag
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                拖拽文件到此处上传，或<em>点击选择</em>
              </div>
              <div class="el-upload__tip">
                支持格式：{{ allowedFileTypes.replace(/\./g, '').split(',').join('、') }}，最大 {{ maxFileSize }}MB
              </div>
            </el-upload>
          </div>

          <!-- 图片上传 -->
          <div v-else-if="formData.contentType === 'image'" class="form-group">
            <label class="form-label">上传图片 <span class="required">*</span></label>
            <el-upload
              class="upload-demo"
              :action="uploadUrl"
              :file-list="imageFileList"
              accept="image/jpeg,image/png,image/gif,image/webp"
              :max-file-size="maxImageSize"
              :before-upload="beforeImageUpload"
              :on-success="onImageUploadSuccess"
              :on-error="onImageUploadError"
              :on-progress="onUploadProgress"
              :disabled="isSubmitting"
              :headers="uploadHeaders"
              list-type="picture-card"
            >
              <el-icon class="el-icon--upload"><Plus /></el-icon>
            </el-upload>
            <div class="form-hint">支持格式：JPG、PNG、GIF、WebP，最大 {{ maxImageSize }}MB</div>
          </div>
        </div>

        <!-- 投喂参数 -->
        <div class="section-card">
          <h3 class="section-title">⚙️ 投喂参数</h3>
          
          <div class="form-row">
            <div class="form-group flex-1">
              <label class="form-label">优先级</label>
              <el-select 
                v-model="formData.priority" 
                :disabled="isSubmitting"
                class="priority-select"
              >
                <el-option label="低" value="low" />
                <el-option label="中" value="medium" />
                <el-option label="高" value="high" />
                <el-option label="紧急" value="urgent" />
              </el-select>
            </div>
            
            <div class="form-group flex-1">
              <label class="form-label">处理模式</label>
              <el-select 
                v-model="formData.processMode" 
                :disabled="isSubmitting"
                class="mode-select"
              >
                <el-option label="快速处理" value="fast" />
                <el-option label="标准处理" value="standard" />
                <el-option label="深度处理" value="deep" />
              </el-select>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">标签（可选）</label>
            <el-input
              v-model="formData.tags"
              placeholder="用逗号分隔多个标签"
              :disabled="isSubmitting"
            />
            <div class="form-hint">例如：技术文档, 产品说明, 知识库</div>
          </div>

          <div class="form-group">
            <label class="form-label">备注（可选）</label>
            <el-textarea
              v-model="formData.remark"
              :rows="3"
              placeholder="添加备注信息..."
              :disabled="isSubmitting"
            />
          </div>
        </div>

        <!-- 提交按钮 -->
        <div class="submit-section">
          <el-button 
            type="primary" 
            :loading="isSubmitting"
            :disabled="!canSubmit || isSubmitting"
            class="submit-btn"
            @click="submitFeed"
          >
            {{ isSubmitting ? '提交中...' : '🚀 开始投喂' }}
          </el-button>
          <el-button 
            :disabled="isSubmitting"
            @click="resetForm"
          >
            重置表单
          </el-button>
        </div>
      </div>

      <!-- 右侧：状态和历史 -->
      <div class="feed-status-section">
        <!-- 当前状态 -->
        <div class="section-card status-card">
          <h3 class="section-title">📊 当前状态</h3>
          
          <div class="status-grid">
            <div class="status-item">
              <div class="status-value">{{ pendingCount }}</div>
              <div class="status-label">待处理</div>
            </div>
            <div class="status-item">
              <div class="status-value processing">{{ processingCount }}</div>
              <div class="status-label">处理中</div>
            </div>
            <div class="status-item">
              <div class="status-value success">{{ successCount }}</div>
              <div class="status-label">已完成</div>
            </div>
            <div class="status-item">
              <div class="status-value error">{{ failedCount }}</div>
              <div class="status-label">失败</div>
            </div>
          </div>

          <!-- 实时进度 -->
          <div v-if="currentTask" class="current-task">
            <div class="task-header">
              <span class="task-title">当前处理任务</span>
              <span class="task-id">#{{ currentTask.id }}</span>
            </div>
            <div class="task-info">
              <span class="task-type">{{ getContentTypeLabel(currentTask.contentType) }}</span>
              <span class="task-priority" :class="`priority-${currentTask.priority}`">
                {{ getPriorityLabel(currentTask.priority) }}
              </span>
            </div>
            <el-progress 
              :percentage="currentTask.progress" 
              :status="getProgressStatus(currentTask.progress)"
            />
            <div class="task-time">
              预计剩余：{{ currentTask.estimatedTime }}
            </div>
          </div>
        </div>

        <!-- 投喂历史 -->
        <div class="section-card history-card">
          <div class="section-header">
            <h3 class="section-title">📜 投喂历史</h3>
            <button class="refresh-btn" @click="refreshHistory">
              <el-icon><Refresh /></el-icon>
            </button>
          </div>
          
          <div class="history-list">
            <div
              v-for="item in feedHistory"
              :key="item.id"
              class="history-item"
              :class="`status-${item.status}`"
            >
              <div class="history-icon" @click="viewTaskDetail(item.id)">
                {{ getContentIcon(item.contentType) }}
              </div>
              <div class="history-content" @click="viewTaskDetail(item.id)">
                <div class="history-title">{{ item.title || '未命名内容' }}</div>
                <div class="history-meta">
                  <span class="history-type">{{ getContentTypeLabel(item.contentType) }}</span>
                  <span class="history-date">{{ formatDate(item.createdAt) }}</span>
                </div>
              </div>
              <div class="history-status">
                <span :class="`status-badge ${item.status}`">
                  {{ getStatusLabel(item.status) }}
                </span>
              </div>
              <div class="history-actions">
                <el-popconfirm
                  title="确认删除这条投喂记录？"
                  confirm-button="删除"
                  cancel-button="取消"
                  @confirm="deleteFeedTask(item.id)"
                >
                  <template #reference>
                    <el-button size="small" type="danger" plain text>删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>

            <!-- 分页加载更多 / 状态 -->
            <div v-if="feedHistory.length > 0" class="history-pager">
              <el-button
                v-if="historyHasMore"
                size="small"
                type="primary"
                plain
                :loading="historyLoading"
                @click="loadMoreHistory"
              >
                {{ historyLoading ? '加载中...' : '加载更多' }}
              </el-button>
              <span v-else-if="!historyLoading" class="no-more">没有更多了</span>
              <span v-if="historyLoading && !historyHasMore" class="no-more">加载中...</span>
            </div>

            <div v-if="feedHistory.length === 0" class="empty-history">
              <el-icon><Clock /></el-icon>
              <p>暂无投喂记录</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 结果反馈弹窗 -->
    <el-dialog 
      v-model="resultDialogVisible" 
      :title="resultData.success ? '🎉 投喂成功' : '❌ 投喂失败'"
      width="500px"
    >
      <div class="result-content">
        <div :class="['result-icon', { success: resultData.success, error: !resultData.success }]">
          {{ resultData.success ? '✅' : '❌' }}
        </div>
        <div class="result-message">{{ resultData.message }}</div>
        <div v-if="resultData.success && resultData.data" class="result-details">
          <h4>处理结果</h4>
          <div class="detail-row">
            <span class="detail-label">任务ID</span>
            <span class="detail-value">{{ resultData.data.taskId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">处理耗时</span>
            <span class="detail-value">{{ resultData.data.duration }}ms</span>
          </div>

          <!-- 学习效果：进度条 + 等级 -->
          <div class="result-effectiveness">
            <div class="effectiveness-head">
              <span class="detail-label">学习效果（{{ resultEffectivenessMeta.label }}）</span>
              <span class="detail-value">{{ resultEffectivenessMeta.value }}%</span>
            </div>
            <el-progress
              :percentage="resultEffectivenessMeta.value"
              :status="resultEffectivenessMeta.color.type === 'success' ? 'success' : (resultEffectivenessMeta.color.type === 'danger' ? 'exception' : '')"
              :stroke-width="14"
            />
          </div>

          <!-- 知识库入库标识 -->
          <div v-if="resultHasKb" class="kb-banner">
            ✅ 已进入知识库，可在知识库问答中使用
          </div>

          <!-- 知识点数量 -->
          <div v-if="resultKpCount > 0" class="kb-banner info">
            🧩 已提取 {{ resultKpCount }} 个知识点
          </div>

          <!-- feedback 全文 -->
          <div v-if="resultFeedbackParsed.summary" class="feedback-block">
            <div class="detail-label">学习反馈</div>
            <p class="feedback-text">{{ resultFeedbackParsed.summary }}</p>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="resultDialogVisible = false">关闭</el-button>
        <el-button 
          v-if="resultData.success" 
          type="primary" 
          @click="viewTaskDetail(resultData.data.taskId)"
        >
          查看详情
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog 
      v-model="detailDialogVisible" 
      title="投喂详情"
      width="600px"
    >
      <div v-if="selectedTask" class="detail-content">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label>任务ID</label>
              <span>{{ selectedTask.id }}</span>
            </div>
            <div class="detail-item">
              <label>内容类型</label>
              <span>{{ getContentTypeLabel(selectedTask.contentType) }}</span>
            </div>
            <div class="detail-item">
              <label>优先级</label>
              <span :class="`priority-${selectedTask.priority}`">{{ getPriorityLabel(selectedTask.priority) }}</span>
            </div>
            <div class="detail-item">
              <label>处理模式</label>
              <span>{{ getProcessModeLabel(selectedTask.processMode) }}</span>
            </div>
          </div>
        </div>
        
        <div class="detail-section">
          <h4>处理状态</h4>
          <div class="progress-container">
            <el-progress
              :percentage="selectedTask.progress"
              :status="getProgressStatus(selectedTask.progress)"
              :format="(p) => `${p}%`"
            />
            <div class="progress-info">
              <span>{{ getStatusLabel(selectedTask.status) }}</span>
              <span>{{ selectedTask.progress }}%</span>
            </div>
          </div>
        </div>

        <!-- 文件列表 -->
        <div v-if="detailFileList.length > 0" class="detail-section">
          <h4>关联文件（{{ detailFileList.length }}）</h4>
          <ul class="file-list">
            <li v-for="(f, idx) in detailFileList" :key="idx" class="file-list-item">
              <el-icon><Document /></el-icon>
              <span class="file-name" :title="f.name">{{ f.name }}</span>
              <el-button size="small" type="primary" plain @click="openFilePreview(f.path)">
                预览/下载
              </el-button>
            </li>
          </ul>
        </div>

        <!-- 图片预览 -->
        <div v-if="detailImageList.length > 0" class="detail-section">
          <h4>关联图片（{{ detailImageList.length }}）</h4>
          <div class="image-preview-grid">
            <div v-for="(img, idx) in detailImageList" :key="idx" class="image-preview-item">
              <img :src="img.path" :alt="img.name" @click="openFilePreview(img.path)" />
              <div class="image-caption">{{ img.name }}</div>
            </div>
          </div>
        </div>

        <!-- 知识库 / 知识点 -->
        <div v-if="detailHasKb || detailKpCount > 0" class="detail-section">
          <h4>知识库</h4>
          <div class="kb-tags">
            <el-tag v-if="detailHasKb" type="success" size="small">✅ 已进入知识库</el-tag>
            <el-tag v-if="detailKpCount > 0" type="primary" size="small">
              🧩 已提取 {{ detailKpCount }} 个知识点
            </el-tag>
          </div>
        </div>

        <div class="detail-section">
          <h4>标签</h4>
          <div class="tags-container">
            <el-tag 
              v-for="tag in (selectedTask.tags || []).split(',')" 
              :key="tag"
              size="small"
            >
              {{ tag.trim() }}
            </el-tag>
            <span v-if="!selectedTask.tags" class="no-tags">无标签</span>
          </div>
        </div>

        <div v-if="selectedTask.remark" class="detail-section">
          <h4>备注</h4>
          <p class="remark-text">{{ selectedTask.remark }}</p>
        </div>

        <div class="detail-section">
          <h4>时间信息</h4>
          <div class="time-grid">
            <div class="time-item">
              <label>创建时间</label>
              <span>{{ formatDateTime(selectedTask.createdAt) }}</span>
            </div>
            <div class="time-item">
              <label>开始处理</label>
              <span>{{ selectedTask.startedAt ? formatDateTime(selectedTask.startedAt) : '-' }}</span>
            </div>
            <div class="time-item">
              <label>完成时间</label>
              <span>{{ selectedTask.completedAt ? formatDateTime(selectedTask.completedAt) : '-' }}</span>
            </div>
            <div class="time-item">
              <label>处理耗时</label>
              <span>{{ selectedTask.duration ? selectedTask.duration + 'ms' : '-' }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 主题日志查看器 -->
    <ThemeLogViewer v-model="logViewerVisible" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Plus, Refresh, Clock, ArrowLeft, Document } from '@element-plus/icons-vue'
import ThemeLogViewer from '@/components/ThemeLogViewer.vue'
import {
  parseMultiPaths,
  formatFileSize,
  getEffectivenessColor,
  getEffectivenessLabel,
  parseFeedFeedback,
  hasKnowledgeBaseLink,
  getKnowledgePointCount
} from '@/utils/feed'
import {
  logThemeOnMounted,
  logThemeSwitchTrigger,
  logThemeBefore,
  logThemeAfter,
  logThemeSwitchComplete
} from '@/utils/themeLogger'

const router = useRouter()
const componentName = 'FeedView'

// 主题切换相关
let themeSwitchStartTime = null
let beforeThemeVariables = null

// 监听主题切换事件
const handleThemeSwitchStart = () => {
  themeSwitchStartTime = Date.now()
  beforeThemeVariables = logThemeBefore(componentName)
  logThemeSwitchTrigger(componentName, 'localStorage_change')
}

const handleThemeSwitchEnd = () => {
  if (themeSwitchStartTime && beforeThemeVariables) {
    const duration = Date.now() - themeSwitchStartTime
    logThemeAfter(componentName, beforeThemeVariables)
    logThemeSwitchComplete(componentName, duration)
    themeSwitchStartTime = null
    beforeThemeVariables = null
  }
}

// 监听 localStorage 变化
const storageListener = (event) => {
  if (event.key === 'theme') {
    handleThemeSwitchStart()
    // 延迟记录切换后的状态，确保 DOM 已更新
    setTimeout(handleThemeSwitchEnd, 350)
  }
}

// 主题日志查看器
const logViewerVisible = ref(false)

// 返回上一页
const goBack = () => {
  const currentRoute = router.currentRoute.value
  const fromRoute = router.options.history.state.value?.back
  
  if (fromRoute && fromRoute.fullPath) {
    router.back()
  } else {
    // 如果没有历史记录，默认返回聊天页面
    router.push('/chat')
  }
}

// 内容类型配置
const contentTypes = [
  { value: 'text', label: '文本', icon: '📝' },
  { value: 'file', label: '文件', icon: '📁' },
  { value: 'image', label: '图片', icon: '🖼️' }
]

// 表单数据
const formData = ref({
  contentType: 'text',
  textContent: '',
  priority: 'medium',
  processMode: 'standard',
  tags: '',
  remark: ''
})

// 上传相关
const uploadFileList = ref([])
const imageFileList = ref([])
const uploadUrl = '/api/feed/upload'
const uploadStartTime = ref(null)
const uploadProgress = ref({})

// 上传请求头（携带 JWT 鉴权 token，否则 POST /api/feed/upload 会被 SecurityConfig 拦截返回 401）
const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + (localStorage.getItem('token') || '')
}))

// 限制配置
const maxTextLength = 10000
const maxFileSize = 50
const maxImageSize = 10
const allowedFileTypes = '.txt,.md,.json,.csv,.pdf'

// 状态
const isSubmitting = ref(false)
const resultDialogVisible = ref(false)
const resultData = ref({
  success: false,
  message: '',
  data: null
})

// 日志相关
const feedLogs = ref([])
const maxLogs = 50

// 添加日志
const addFeedLog = (level, operation, message, params = {}) => {
  const log = {
    id: 'log-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9),
    timestamp: new Date().toISOString(),
    level,
    operation,
    message,
    params
  }
  feedLogs.value.unshift(log)
  if (feedLogs.value.length > maxLogs) {
    feedLogs.value.pop()
  }
  // 输出到控制台
  const color = level === 'error' ? '\x1b[31m' : level === 'warning' ? '\x1b[33m' : level === 'success' ? '\x1b[32m' : '\x1b[34m'
  console.log(`${color}[${log.timestamp}] [${log.operation}] ${log.message}\x1b[0m`, params)
}

// 详情弹窗
const detailDialogVisible = ref(false)
const selectedTask = ref(null)

// 当前处理任务
const currentTask = ref(null)

// 统计数据(来自后端 /api/feed/stats 真实统计，初始为 0)
const pendingCount = ref(0)
const processingCount = ref(0)
const successCount = ref(0)
const failedCount = ref(0)

// 投喂历史（真实分页）
const feedHistory = ref([])
const historyPage = ref(1)
const historySize = ref(10)
const historyTotal = ref(0)
const historyHasMore = ref(false)
const historyLoading = ref(false)

// 计算属性：是否可以提交
const canSubmit = computed(() => {
  if (formData.value.contentType === 'text') {
    return formData.value.textContent.trim().length > 0
  } else if (formData.value.contentType === 'file') {
    return uploadFileList.value.length > 0
  } else if (formData.value.contentType === 'image') {
    return imageFileList.value.length > 0
  }
  return false
})

// 加载投喂历史（真实分页）
// append=true 时追加到列表尾部（加载更多）；false 时重置为第一页
const loadFeedHistory = async (append = false) => {
  if (historyLoading.value) return
  if (append && !historyHasMore.value) return

  historyLoading.value = true
  try {
    const targetPage = append ? historyPage.value + 1 : 1
    const response = await request.get('/api/feed/history', {
      params: { page: targetPage, size: historySize.value }
    })
    // request 拦截器已返回 R 体 {code, message, data}
    if (response.code === 200) {
      const data = response.data || {}
      const list = data.list || []
      if (append) {
        // 简单去重：按 id 过滤已存在项
        const existIds = new Set(feedHistory.value.map((it) => it.id))
        feedHistory.value = feedHistory.value.concat(list.filter((it) => !existIds.has(it.id)))
      } else {
        feedHistory.value = list
      }
      historyPage.value = data.page || targetPage
      historyTotal.value = data.total ?? list.length
      historyHasMore.value = data.hasMore === true
    }
  } catch (error) {
    addFeedLog('error', 'load_history_failed', '加载历史记录失败', { errorMessage: error.message })
  } finally {
    historyLoading.value = false
  }
}

// 加载更多历史
const loadMoreHistory = () => {
  loadFeedHistory(true)
}

// 刷新历史（重置到第一页）
const refreshHistory = () => {
  loadFeedHistory(false)
}

// 加载真实统计数据
const loadStats = async () => {
  try {
    const response = await request.get('/api/feed/stats')
    if (response.code === 200 && response.data) {
      pendingCount.value = response.data.pending || 0
      processingCount.value = response.data.processing || 0
      successCount.value = response.data.success || 0
      failedCount.value = response.data.failed || 0
      currentTask.value = response.data.currentTask || null
    }
  } catch (error) {
    addFeedLog('error', 'load_stats_failed', '加载统计数据失败', { errorMessage: error.message })
  }
}

// 文件上传前校验
const beforeFileUpload = (file) => {
  // 记录文件上传开始
  uploadStartTime.value = Date.now()
  addFeedLog('info', 'upload_start', `开始上传文件: ${file.name}`, {
    fileName: file.name,
    fileSize: formatFileSize(file.size),
    fileSizeBytes: file.size,
    fileType: file.type,
    priority: formData.value.priority,
    processMode: formData.value.processMode
  })
  
  const isAllowed = allowedFileTypes.includes(file.name.substring(file.name.lastIndexOf('.')))
  if (!isAllowed) {
    addFeedLog('error', 'upload_validation_failed', `文件类型不支持: ${file.name}`, {
      fileName: file.name,
      reason: '不支持的文件格式',
      allowedTypes: allowedFileTypes
    })
    ElMessage.error('不支持的文件格式')
    return false
  }
  
  // 检查文件大小
  if (file.size > maxFileSize * 1024 * 1024) {
    addFeedLog('error', 'upload_validation_failed', `文件大小超过限制: ${file.name}`, {
      fileName: file.name,
      fileSize: formatFileSize(file.size),
      maxSize: `${maxFileSize}MB`,
      reason: '文件过大'
    })
    ElMessage.error(`文件大小超过 ${maxFileSize}MB 限制`)
    return false
  }
  
  addFeedLog('info', 'upload_validation_passed', `文件验证通过: ${file.name}`, {
    fileName: file.name,
    fileSize: formatFileSize(file.size)
  })
  return true
}

const beforeImageUpload = (file) => {
  // 记录文件上传开始
  uploadStartTime.value = Date.now()
  addFeedLog('info', 'upload_start', `开始上传图片: ${file.name}`, {
    fileName: file.name,
    fileSize: formatFileSize(file.size),
    fileSizeBytes: file.size,
    fileType: file.type,
    priority: formData.value.priority,
    processMode: formData.value.processMode
  })
  
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    addFeedLog('error', 'upload_validation_failed', `文件类型不是图片: ${file.name}`, {
      fileName: file.name,
      fileType: file.type,
      reason: '不是图片文件'
    })
    ElMessage.error('请上传图片文件')
    return false
  }
  
  // 检查文件大小
  if (file.size > maxImageSize * 1024 * 1024) {
    addFeedLog('error', 'upload_validation_failed', `图片大小超过限制: ${file.name}`, {
      fileName: file.name,
      fileSize: formatFileSize(file.size),
      maxSize: `${maxImageSize}MB`,
      reason: '图片过大'
    })
    ElMessage.error(`图片大小超过 ${maxImageSize}MB 限制`)
    return false
  }
  
  addFeedLog('info', 'upload_validation_passed', `图片验证通过: ${file.name}`, {
    fileName: file.name,
    fileSize: formatFileSize(file.size)
  })
  return true
}

// 文件上传进度处理
const onUploadProgress = (event, file) => {
  if (event.lengthComputable) {
    const percent = Math.round((event.loaded / event.total) * 100)
    const speed = event.timeStamp > 0 && uploadStartTime.value ? 
      ((event.loaded / (event.timeStamp - uploadStartTime.value)) * 1000 / 1024).toFixed(2) : '0'
    
    uploadProgress.value[file.name] = {
      percent,
      speed: `${speed} KB/s`,
      loaded: formatFileSize(event.loaded),
      total: formatFileSize(event.total)
    }
    
    // 每10%记录一次进度
    if (percent % 10 === 0 && !uploadProgress.value[file.name].loggedPercent) {
      uploadProgress.value[file.name].loggedPercent = percent
      addFeedLog('info', 'upload_progress', `文件上传进度: ${file.name} - ${percent}%`, {
        fileName: file.name,
        percent,
        speed: `${speed} KB/s`,
        loaded: formatFileSize(event.loaded),
        total: formatFileSize(event.total)
      })
    }
  }
}

// 文件上传成功
const onFileUploadSuccess = (response, file) => {
  const uploadTime = Date.now() - uploadStartTime.value
  if (response.code === 200) {
    // 将上传成功的文件同步到 uploadFileList，使 canSubmit 计算属性感知到已有文件
    uploadFileList.value.push({
      name: file.name,
      size: file.size,
      uid: file.uid,
      response: response
    })
    addFeedLog('success', 'upload_complete', `文件上传成功: ${file.name}`, {
      fileName: file.name,
      fileSize: formatFileSize(file.size),
      uploadTime: `${(uploadTime / 1000).toFixed(2)}s`,
      responseData: response.data
    })
    ElMessage.success('文件上传成功')
  } else {
    addFeedLog('error', 'upload_failed', `文件上传失败: ${file.name}`, {
      fileName: file.name,
      uploadTime: `${(uploadTime / 1000).toFixed(2)}s`,
      errorMessage: response.message
    })
    ElMessage.error('文件上传失败: ' + response.message)
  }
}

const onImageUploadSuccess = (response, file) => {
  const uploadTime = Date.now() - uploadStartTime.value
  if (response.code === 200) {
    // 将上传成功的图片同步到 imageFileList，使 canSubmit 计算属性感知到已有图片
    imageFileList.value.push({
      name: file.name,
      size: file.size,
      uid: file.uid,
      url: response.data?.filePath || file.url,
      response: response
    })
    addFeedLog('success', 'upload_complete', `图片上传成功: ${file.name}`, {
      fileName: file.name,
      fileSize: formatFileSize(file.size),
      uploadTime: `${(uploadTime / 1000).toFixed(2)}s`,
      responseData: response.data
    })
    ElMessage.success('图片上传成功')
  } else {
    addFeedLog('error', 'upload_failed', `图片上传失败: ${file.name}`, {
      fileName: file.name,
      uploadTime: `${(uploadTime / 1000).toFixed(2)}s`,
      errorMessage: response.message
    })
    ElMessage.error('图片上传失败: ' + response.message)
  }
}

// 文件上传失败
const onFileUploadError = (error, file) => {
  const uploadTime = Date.now() - uploadStartTime.value
  addFeedLog('error', 'upload_error', `文件上传错误: ${file.name}`, {
    fileName: file.name,
    uploadTime: `${(uploadTime / 1000).toFixed(2)}s`,
    errorMessage: error.message,
    errorStack: error.stack
  })
  ElMessage.error('文件上传失败：' + error.message)
}

const onImageUploadError = (error, file) => {
  const uploadTime = Date.now() - uploadStartTime.value
  addFeedLog('error', 'upload_error', `图片上传错误: ${file.name}`, {
    fileName: file.name,
    uploadTime: `${(uploadTime / 1000).toFixed(2)}s`,
    errorMessage: error.message,
    errorStack: error.stack
  })
  ElMessage.error('图片上传失败：' + error.message)
}

// 提交投喂
const submitFeed = async () => {
  if (!canSubmit.value) {
    addFeedLog('warning', 'submit_validation_failed', '提交验证失败：未填写或上传投喂内容')
    ElMessage.warning('请填写或上传投喂内容')
    return
  }

  const submitStartTime = Date.now()
  
  // 记录任务提交
  addFeedLog('info', 'submit_start', '开始提交投喂任务', {
    contentType: formData.value.contentType,
    priority: formData.value.priority,
    processMode: formData.value.processMode,
    tags: formData.value.tags,
    remark: formData.value.remark,
    textLength: formData.value.contentType === 'text' ? formData.value.textContent.length : 0,
    fileCount: formData.value.contentType === 'file' ? uploadFileList.value.length : 0,
    imageCount: formData.value.contentType === 'image' ? imageFileList.value.length : 0,
    submitTime: new Date().toISOString()
  })

  isSubmitting.value = true

  try {
    const payload = {
      contentType: formData.value.contentType,
      priority: formData.value.priority,
      processMode: formData.value.processMode,
      tags: formData.value.tags,
      remark: formData.value.remark
    }

    if (formData.value.contentType === 'text') {
      payload.content = formData.value.textContent
    } else if (formData.value.contentType === 'file') {
      // 契约对齐：后端按 filePath 读取文件内容，从 upload 响应的 data.filePath 取，而非 id
      payload.filePaths = uploadFileList.value
        .map((f) => f.response?.data?.filePath)
        .filter(Boolean)
      payload.fileNames = uploadFileList.value.map(
        (f) => f.response?.data?.fileName || f.name
      )
    } else if (formData.value.contentType === 'image') {
      payload.imagePaths = imageFileList.value
        .map((f) => f.response?.data?.filePath)
        .filter(Boolean)
      payload.imageNames = imageFileList.value.map(
        (f) => f.response?.data?.fileName || f.name
      )
    }

    // 更新当前任务状态
    currentTask.value = {
      id: 'FEED-PROC-' + Date.now(),
      contentType: payload.contentType,
      priority: payload.priority,
      progress: 0,
      estimatedTime: '提交中...'
    }

    // 调用后端API
    const response = await request.post('/api/feed/submit', payload)
    
    if (response.code === 200) {
      const submitDuration = Date.now() - submitStartTime
      
      // 更新进度为完成
      currentTask.value.progress = 100
      currentTask.value.estimatedTime = '已完成'
      
      // 记录任务完成
      addFeedLog('success', 'submit_complete', '投喂任务提交成功', {
        taskId: response.data.taskId,
        duration: `${(submitDuration / 1000).toFixed(2)}s`,
        effectiveness: `${response.data.effectiveness}%`,
        feedback: response.data.feedback,
        resultData: response.data
      })

      resultData.value = {
        success: true,
        message: response.message,
        data: response.data
      }
      resultDialogVisible.value = true
      
      // 记录结果反馈展示
      addFeedLog('info', 'result_display', '显示投喂结果反馈', {
        success: true,
        message: response.message,
        taskId: response.data.taskId
      })
      
      // 重置表单
      resetForm()
      // 刷新历史与统计(实时反映最新投喂状态)
      loadFeedHistory()
      loadStats()
    } else {
      throw new Error(response.message)
    }
  } catch (error) {
    const submitDuration = Date.now() - submitStartTime
    
    // 记录任务失败
    addFeedLog('error', 'submit_failed', '投喂任务提交失败', {
      errorMessage: error.message,
      duration: `${(submitDuration / 1000).toFixed(2)}s`,
      stack: error.stack
    })

    resultData.value = {
      success: false,
      message: error.message || '投喂失败，请稍后重试',
      data: null
    }
    resultDialogVisible.value = true
    
    // 记录结果反馈展示
    addFeedLog('info', 'result_display', '显示投喂失败反馈', {
      success: false,
      message: error.message
    })
  } finally {
    isSubmitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  formData.value = {
    contentType: 'text',
    textContent: '',
    priority: 'medium',
    processMode: 'standard',
    tags: '',
    remark: ''
  }
  uploadFileList.value = []
  imageFileList.value = []
}

// 查看任务详情
const viewTaskDetail = async (taskId) => {
  resultDialogVisible.value = false
  try {
    const response = await request.get(`/api/feed/task/${taskId}`)
    if (response.code === 200) {
      selectedTask.value = response.data
      detailDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取任务详情失败：' + error.message)
  }
}

// 删除单条投喂记录
const deleteFeedTask = async (taskId) => {
  try {
    const response = await request.delete(`/api/feed/task/${taskId}`)
    if (response.code === 200) {
      feedHistory.value = feedHistory.value.filter((it) => it.id !== taskId)
      historyTotal.value = Math.max(0, historyTotal.value - 1)
      ElMessage.success('删除成功')
      addFeedLog('success', 'task_deleted', '删除投喂记录成功', { taskId })
      // 若当前详情弹窗正是被删除的任务，则关闭
      if (selectedTask.value && selectedTask.value.id === taskId) {
        detailDialogVisible.value = false
        selectedTask.value = null
      }
      // 刷新统计
      loadStats()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    ElMessage.error('删除失败：' + error.message)
    addFeedLog('error', 'task_delete_failed', '删除投喂记录失败', {
      taskId,
      errorMessage: error.message
    })
  }
}

// ---- 详情弹窗解析（纯逻辑，复用 utils/feed）----
// 选中任务关联的文件列表 [{ name, path }]
const detailFileList = computed(() => {
  if (!selectedTask.value) return []
  const paths = parseMultiPaths(selectedTask.value.filePaths)
  const names = parseMultiPaths(selectedTask.value.fileNames)
  return paths.map((p, i) => ({ path: p, name: names[i] || p }))
})
// 选中任务关联的图片列表 [{ name, path }]
const detailImageList = computed(() => {
  if (!selectedTask.value) return []
  const paths = parseMultiPaths(selectedTask.value.imagePaths)
  const names = parseMultiPaths(selectedTask.value.imageNames)
  return paths.map((p, i) => ({ path: p, name: names[i] || p }))
})
// 选中任务是否已入知识库
const detailHasKb = computed(() => hasKnowledgeBaseLink(selectedTask.value))
// 选中任务知识点数量
const detailKpCount = computed(() => getKnowledgePointCount(selectedTask.value))

// ---- 结果弹窗解析 ----
// 本次成功提交结果是否已入知识库
const resultHasKb = computed(() =>
  resultData.value?.success && resultData.value?.data
    ? hasKnowledgeBaseLink(resultData.value.data)
    : false
)
// 本次成功提交结果的知识点数量
const resultKpCount = computed(() =>
  resultData.value?.data ? getKnowledgePointCount(resultData.value.data) : 0
)
// 本次 effectiveness 颜色/标签
const resultEffectivenessMeta = computed(() => {
  const eff = Number(resultData.value?.data?.effectiveness)
  return {
    value: isNaN(eff) ? 0 : eff,
    color: getEffectivenessColor(eff),
    label: getEffectivenessLabel(eff)
  }
})
// 本次 feedback 结构化
const resultFeedbackParsed = computed(() =>
  parseFeedFeedback(resultData.value?.data?.feedback)
)

// 在新窗口预览/下载文件（filePath 为后端可访问 URL）
const openFilePreview = (filePath) => {
  if (!filePath) return
  window.open(filePath, '_blank')
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 获取标签
const getContentTypeLabel = (type) => {
  const found = contentTypes.find(t => t.value === type)
  return found ? found.label : type
}

const getContentIcon = (type) => {
  const found = contentTypes.find(t => t.value === type)
  return found ? found.icon : '📄'
}

const getPriorityLabel = (priority) => {
  const labels = {
    low: '低',
    medium: '中',
    high: '高',
    urgent: '紧急'
  }
  return labels[priority] || priority
}

const getProcessModeLabel = (mode) => {
  const labels = {
    fast: '快速处理',
    standard: '标准处理',
    deep: '深度处理'
  }
  return labels[mode] || mode
}

const getStatusLabel = (status) => {
  const labels = {
    pending: '待处理',
    processing: '处理中',
    completed: '已完成',
    failed: '失败'
  }
  return labels[status] || status
}

const getProgressStatus = (progress) => {
  if (progress >= 100) return 'success'
  if (progress > 0) return ''
  return 'exception'
}

// 初始化
onMounted(() => {
  // 记录组件挂载时的主题状态
  logThemeOnMounted(componentName)

  // 添加 localStorage 监听器
  window.addEventListener('storage', storageListener)

  // 加载真实统计数据与投喂历史(不再使用 Mock 数据)
  loadStats()
  loadFeedHistory()
})

// 清理
onUnmounted(() => {
  // 移除 localStorage 监听器
  window.removeEventListener('storage', storageListener)
})
</script>

<style>
.feed-module {
  min-height: 100vh;
  padding: 24px;
  background: var(--feed-bg-gradient);
}

.feed-header {
  text-align: center;
  margin-bottom: 24px;
  color: var(--feed-text-color);
  position: relative;
}

.header-actions {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
}

.back-btn {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--feed-btn-bg);
  border: none;
  border-radius: 8px;
  color: var(--feed-text-color);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  background: var(--feed-btn-hover-bg);
}

.back-btn:active {
  transform: translateY(-50%) scale(0.98);
}

.feed-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
}

.feed-desc {
  font-size: 14px;
  opacity: 0.9;
}

.feed-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.section-card {
  background: var(--card-bg);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--shadow-lg);
  margin-bottom: 24px;
  border: 1px solid var(--border-primary);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 20px;
  color: var(--text-primary);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.refresh-btn {
  padding: 6px 12px;
  border: none;
  background: var(--bg-tertiary);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  color: var(--text-primary);
}

.refresh-btn:hover {
  background: var(--bg-hover);
}

.form-group {
  margin-bottom: 20px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-row .form-group {
  flex: 1;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.form-label .required {
  color: var(--color-error);
}

.form-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}

.content-type-selector {
  display: flex;
  gap: 12px;
}

.type-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border: 2px solid var(--border-primary);
  border-radius: 12px;
  background: var(--card-bg);
  cursor: pointer;
  transition: all 0.2s;
}

.type-btn:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.type-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.type-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.type-name {
  font-size: 14px;
  color: var(--text-secondary);
}

.submit-section {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.submit-btn {
  padding: 12px 32px;
  font-size: 14px;
  font-weight: 500;
}

/* 状态卡片 */
.status-card {
  margin-bottom: 24px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.status-item {
  text-align: center;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: 12px;
}

.status-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.status-value.processing {
  color: var(--color-primary);
}

.status-value.success {
  color: var(--color-success);
}

.status-value.error {
  color: var(--color-error);
}

.status-label {
  font-size: 12px;
  color: var(--text-muted);
}

.current-task {
  background: var(--feed-bg-gradient);
  border-radius: 12px;
  padding: 20px;
  color: var(--feed-text-color);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.task-title {
  font-weight: 600;
}

.task-id {
  font-size: 12px;
  opacity: 0.8;
}

.task-info {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.task-type, .task-priority {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  background: var(--feed-btn-bg);
}

.task-time {
  margin-top: 12px;
  font-size: 12px;
  opacity: 0.8;
}

/* 历史记录 */
.history-list {
  max-height: 400px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--border-primary);
  transition: background 0.2s;
}

.history-item:last-child {
  border-bottom: none;
}

.history-item:hover {
  background: var(--bg-hover);
}

.history-icon {
  font-size: 24px;
  margin-right: 12px;
}

.history-content {
  flex: 1;
}

.history-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.history-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

.history-status {
  margin-left: auto;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}

.status-badge.pending {
  background: var(--color-warning-light);
  color: var(--color-warning);
}

.status-badge.processing {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.status-badge.completed {
  background: var(--color-success-light);
  color: var(--color-success);
}

.status-badge.failed {
  background: var(--color-error-light);
  color: var(--color-error);
}

.empty-history {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}

.empty-history p {
  margin-top: 8px;
}

/* 结果弹窗 */
.result-content {
  text-align: center;
  padding: 24px;
}

.result-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.result-icon.success {
  color: var(--color-success);
}

.result-icon.error {
  color: var(--color-error);
}

.result-message {
  font-size: 16px;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.result-details {
  background: var(--bg-tertiary);
  border-radius: 12px;
  padding: 20px;
  text-align: left;
}

.result-details h4 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-primary);
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.detail-value {
  color: var(--text-primary);
  font-weight: 500;
  font-size: 13px;
}

/* 详情弹窗 */
.detail-content {
  padding: 8px;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h4 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 12px;
  background: var(--bg-tertiary);
  border-radius: 8px;
}

.detail-item label {
  color: var(--text-secondary);
  font-size: 13px;
}

.detail-item span {
  color: var(--text-primary);
  font-weight: 500;
  font-size: 13px;
}

.detail-item span.priority-low { color: var(--color-success); }
.detail-item span.priority-medium { color: var(--color-warning); }
.detail-item span.priority-high { color: var(--color-error); }
.detail-item span.priority-urgent { color: var(--color-error); }

.progress-container {
  background: var(--bg-tertiary);
  border-radius: 8px;
  padding: 16px;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.no-tags {
  color: var(--text-muted);
  font-size: 13px;
}

.remark-text {
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.time-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.time-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg-tertiary);
  border-radius: 8px;
}

.time-item label {
  color: var(--text-secondary);
  font-size: 12px;
}

.time-item span {
  color: var(--text-primary);
  font-size: 12px;
}

/* 历史记录项操作区与分页 */
.history-actions {
  margin-left: 8px;
  display: flex;
  align-items: center;
}

.history-icon, .history-content {
  cursor: pointer;
}

.history-pager {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px 0 4px;
}

.no-more {
  font-size: 12px;
  color: var(--text-muted);
}

/* 结果弹窗：学习效果进度条 / 知识库横幅 / 反馈 */
.result-effectiveness {
  padding: 10px 0;
}

.effectiveness-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.kb-banner {
  margin: 10px 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--color-success-light);
  color: var(--color-success);
  font-size: 13px;
  text-align: left;
}

.kb-banner.info {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.feedback-block {
  margin-top: 12px;
  text-align: left;
}

.feedback-text {
  margin-top: 6px;
  padding: 10px 12px;
  background: var(--card-bg);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* 详情弹窗：文件 / 图片 / 知识库 */
.file-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.file-list-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: var(--bg-tertiary);
  border-radius: 8px;
  margin-bottom: 6px;
}

.file-list-item .file-name {
  flex: 1;
  font-size: 13px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.image-preview-item img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--border-primary);
}

.image-caption {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 自定义文本域 */
.custom-textarea {
  width: 100%;
  padding: 12px 16px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-primary);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary);
  font-family: var(--font-family-body);
  resize: vertical;
  transition: all 0.2s ease;
  line-height: 1.6;
}

.custom-textarea::placeholder {
  color: var(--text-muted);
}

.custom-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-glow);
}

.custom-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.word-count {
  text-align: right;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

/* 响应式 */
@media (max-width: 1024px) {
  .feed-content {
    grid-template-columns: 1fr;
  }
  
  .status-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .form-row {
    flex-direction: column;
  }
}
</style>