<template>
  <div class="theme-log-viewer">
    <el-drawer
      v-model="visible"
      title="主题切换日志查看器"
      direction="rtl"
      size="60%"
      :before-close="handleClose"
    >
      <template #header>
        <div class="drawer-header">
          <span>主题切换日志查看器</span>
          <div class="header-actions">
            <el-button size="small" @click="exportLogs" type="primary">
              导出日志
            </el-button>
            <el-button size="small" @click="clearLogs" type="danger">
              清除日志
            </el-button>
          </div>
        </div>
      </template>

      <div class="log-controls">
        <el-select v-model="selectedLevel" placeholder="日志级别" size="small" style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="DEBUG" value="DEBUG" />
          <el-option label="INFO" value="INFO" />
          <el-option label="WARN" value="WARN" />
          <el-option label="ERROR" value="ERROR" />
        </el-select>

        <el-select v-model="selectedComponent" placeholder="组件" size="small" style="width: 150px">
          <el-option label="全部" value="" />
          <el-option label="FeedView" value="FeedView" />
          <el-option label="AdminView" value="AdminView" />
        </el-select>

        <el-select v-model="selectedAction" placeholder="操作类型" size="small" style="width: 180px">
          <el-option label="全部" value="" />
          <el-option label="组件挂载" value="COMPONENT_MOUNTED" />
          <el-option label="切换触发" value="THEME_SWITCH_TRIGGER" />
          <el-option label="切换前" value="THEME_SWITCH_BEFORE" />
          <el-option label="切换后" value="THEME_SWITCH_AFTER" />
          <el-option label="切换完成" value="THEME_SWITCH_COMPLETE" />
        </el-select>

        <el-button size="small" @click="refreshLogs">刷新</el-button>
      </div>

      <div class="log-stats">
        <el-tag size="small" type="info">总日志数: {{ filteredLogs.length }}</el-tag>
        <el-tag size="small" type="success">INFO: {{ getLogCount('INFO') }}</el-tag>
        <el-tag size="small" type="warning">WARN: {{ getLogCount('WARN') }}</el-tag>
        <el-tag size="small" type="danger">ERROR: {{ getLogCount('ERROR') }}</el-tag>
      </div>

      <div class="log-list">
        <div
          v-for="(log, index) in filteredLogs"
          :key="index"
          :class="['log-item', `log-${log.level.toLowerCase()}`]"
          @click="showLogDetail(log)"
        >
          <div class="log-header">
            <span class="log-level">{{ log.level }}</span>
            <span class="log-component">{{ log.component }}</span>
            <span class="log-action">{{ log.action }}</span>
            <span class="log-time">{{ formatTime(log.timestamp) }}</span>
          </div>
          <div v-if="log.theme" class="log-theme">
            <span class="theme-label">主题:</span>
            <el-tag :type="log.theme === 'dark' ? 'info' : 'warning'" size="small">
              {{ log.theme }}
            </el-tag>
          </div>
          <div v-if="log.duration" class="log-duration">
            <span class="duration-label">耗时:</span>
            <span class="duration-value">{{ log.duration }}</span>
          </div>
          <div v-if="log.changedCount !== undefined" class="log-changes">
            <span class="changes-label">变量变化:</span>
            <el-tag type="success" size="small">{{ log.changedCount }} 个变化</el-tag>
            <el-tag type="info" size="small">{{ log.unchangedCount }} 个未变</el-tag>
          </div>
        </div>

        <el-empty v-if="filteredLogs.length === 0" description="暂无日志记录" />
      </div>

      <!-- 日志详情弹窗 -->
      <el-dialog
        v-model="detailVisible"
        title="日志详情"
        width="70%"
        :append-to-body="true"
      >
        <div class="log-detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="时间戳">
              {{ selectedLog?.timestamp }}
            </el-descriptions-item>
            <el-descriptions-item label="级别">
              <el-tag :type="getLevelType(selectedLog?.level)">
                {{ selectedLog?.level }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="组件">
              {{ selectedLog?.component }}
            </el-descriptions-item>
            <el-descriptions-item label="操作">
              {{ selectedLog?.action }}
            </el-descriptions-item>
            <el-descriptions-item v-if="selectedLog?.theme" label="主题">
              <el-tag :type="selectedLog.theme === 'dark' ? 'info' : 'warning'">
                {{ selectedLog.theme }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item v-if="selectedLog?.duration" label="耗时">
              {{ selectedLog.duration }}
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="selectedLog?.variables" class="variables-section">
            <h4>CSS 变量值</h4>
            <div class="variables-grid">
              <div
                v-for="(value, key) in selectedLog.variables"
                :key="key"
                class="variable-item"
              >
                <span class="variable-name">{{ key }}</span>
                <span class="variable-value">{{ value }}</span>
              </div>
            </div>
          </div>

          <div v-if="selectedLog?.changes" class="changes-section">
            <h4>变量变化详情</h4>
            <div class="changes-grid">
              <div
                v-for="(change, key) in selectedLog.changes"
                :key="key"
                class="change-item"
              >
                <span class="change-name">{{ key }}</span>
                <div class="change-values">
                  <span class="change-before">前: {{ change.before }}</span>
                  <span class="change-after">后: {{ change.after }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="raw-data-section">
            <h4>原始数据</h4>
            <pre class="raw-json">{{ JSON.stringify(selectedLog, null, 2) }}</pre>
          </div>
        </div>
      </el-dialog>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  getLogHistory,
  clearLogHistory,
  exportLogsAsJSON
} from '@/utils/themeLogger'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(props.modelValue)
const detailVisible = ref(false)
const selectedLog = ref(null)
const selectedLevel = ref('')
const selectedComponent = ref('')
const selectedAction = ref('')
const logs = ref([])

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    refreshLogs()
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const filteredLogs = computed(() => {
  return logs.value.filter(log => {
    if (selectedLevel.value && log.level !== selectedLevel.value) return false
    if (selectedComponent.value && log.component !== selectedComponent.value) return false
    if (selectedAction.value && log.action !== selectedAction.value) return false
    return true
  })
})

const refreshLogs = () => {
  logs.value = getLogHistory()
}

const clearLogs = () => {
  clearLogHistory()
  refreshLogs()
}

const exportLogs = () => {
  exportLogsAsJSON()
}

const showLogDetail = (log) => {
  selectedLog.value = log
  detailVisible.value = true
}

const handleClose = () => {
  visible.value = false
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour12: false }) + '.' +
         String(date.getMilliseconds()).padStart(3, '0')
}

const getLevelType = (level) => {
  const types = {
    'DEBUG': 'info',
    'INFO': 'success',
    'WARN': 'warning',
    'ERROR': 'danger'
  }
  return types[level] || 'info'
}

const getLogCount = (level) => {
  return logs.value.filter(log => log.level === level).length
}
</script>

<style scoped>
.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.log-controls {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: 8px;
}

.log-stats {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: 8px;
}

.log-list {
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

.log-item {
  padding: 12px;
  margin-bottom: 8px;
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.log-item:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.log-header {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
}

.log-level {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  min-width: 60px;
  text-align: center;
}

.log-debug .log-level {
  background: var(--color-info-light);
  color: var(--color-info);
}

.log-info .log-level {
  background: var(--color-success-light);
  color: var(--color-success);
}

.log-warn .log-level {
  background: var(--color-warning-light);
  color: var(--color-warning);
}

.log-error .log-level {
  background: var(--color-error-light);
  color: var(--color-error);
}

.log-component {
  font-weight: 500;
  color: var(--text-primary);
}

.log-action {
  color: var(--text-secondary);
}

.log-time {
  margin-left: auto;
  color: var(--text-muted);
  font-size: 12px;
}

.log-theme,
.log-duration,
.log-changes {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
}

.theme-label,
.duration-label,
.changes-label {
  color: var(--text-secondary);
}

.duration-value {
  color: var(--color-primary);
  font-weight: 500;
}

.log-detail {
  padding: 16px;
}

.variables-section,
.changes-section,
.raw-data-section {
  margin-top: 24px;
}

.variables-section h4,
.changes-section h4,
.raw-data-section h4 {
  margin-bottom: 12px;
  color: var(--text-primary);
}

.variables-grid,
.changes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.variable-item,
.change-item {
  padding: 8px;
  background: var(--bg-tertiary);
  border-radius: 4px;
  font-size: 12px;
}

.variable-name,
.change-name {
  display: block;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.variable-value {
  color: var(--text-primary);
  word-break: break-all;
}

.change-values {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.change-before {
  color: var(--text-muted);
}

.change-after {
  color: var(--color-primary);
  font-weight: 500;
}

.raw-data-section {
  margin-top: 24px;
}

.raw-json {
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: 8px;
  overflow-x: auto;
  font-size: 12px;
  color: var(--text-primary);
  max-height: 300px;
  overflow-y: auto;
}
</style>