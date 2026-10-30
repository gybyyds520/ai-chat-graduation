<template>
  <div class="elf-container">
    <div class="elf-header">
      <button class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回</span>
      </button>
      <h2>AI小精灵</h2>
      <p>你的智能生活助手</p>
    </div>

    <div class="elf-main" v-if="elf">
      <div class="elf-card">
        <div class="elf-avatar">
          <img :src="elf.avatar || defaultAvatar" :alt="elf.name" />
          <div class="elf-level">Lv.{{ elf.level }}</div>
          <div class="experience-bar">
            <div class="experience-fill" :style="{ width: (elf.experience % 100) + '%' }"></div>
          </div>
        </div>
        <div class="elf-info">
          <h3>{{ elf.name }}</h3>
          <p class="elf-personality">{{ getPersonalityText(elf.personality) }}</p>
          <div class="elf-theme">
            <span class="theme-tag" :class="elf.theme">{{ getThemeText(elf.theme) }}</span>
          </div>
        </div>
        <div class="elf-actions">
          <el-button size="small" @click="openSettings">设置</el-button>
          <el-button size="small" type="primary" @click="addExp">互动 +10 经验</el-button>
        </div>
      </div>

      <!-- 关系养成 -->
      <div class="relationship-card">
        <div class="rel-head">
          <span class="rel-title">关系：{{ relationship.relationshipTitle || '陌生' }}</span>
          <span class="rel-level">Lv.{{ relationship.relationshipLevel || 1 }}</span>
        </div>
        <div class="rel-row">
          <span class="rel-label">好感度</span>
          <el-progress
            :percentage="relProgress"
            :status="(relationship.affection || 0) >= 100 ? 'success' : ''"
          />
          <span class="rel-value">{{ relationship.affection || 0 }}/100</span>
        </div>
        <div class="rel-row">
          <span class="rel-label">亲密度</span>
          <span class="rel-value">{{ relationship.intimacy || 0 }}</span>
          <span class="rel-next" v-if="!relationship.isMaxLevel">
            再获 {{ relationship.nextLevelAffection || 0 }} 好感可升级
          </span>
          <span class="rel-next max" v-else>已达最高关系</span>
        </div>
        <div class="rel-actions">
          <el-button size="small" @click="doInteract('chat')">聊天</el-button>
          <el-button size="small" type="warning" @click="doInteract('feed')">投喂</el-button>
          <el-button size="small" type="success" @click="doInteract('play')">玩耍</el-button>
          <el-button size="small" @click="loadRelationship">刷新</el-button>
          <el-button size="small" type="primary" @click="claimDailyReward">每日奖励</el-button>
        </div>
        <div class="interaction-timeline">
          <div v-for="log in interactionHistory" :key="log.id" class="timeline-item">
            <span class="t-type">{{ interactionTypeLabel(log.interactionType) }}</span>
            <span class="t-points">+{{ log.pointsGained }} 好感</span>
            <span class="t-time">{{ formatDateTime(log.createdAt) }}</span>
          </div>
          <div v-if="interactionHistory.length === 0" class="t-empty">暂无互动记录</div>
        </div>
      </div>

      <div class="elf-tabs">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="闹钟" name="alarm">
            <div class="tab-content">
              <div class="add-btn">
                <el-button type="primary" @click="openAlarmModal">添加闹钟</el-button>
              </div>
              <div class="alarm-list">
                <div v-for="alarm in alarms" :key="alarm.id" class="alarm-item">
                  <div class="alarm-info">
                    <span class="alarm-time">{{ alarm.time }}</span>
                    <span class="alarm-title">{{ alarm.title }}</span>
                    <span class="alarm-repeat">{{ getRepeatText(alarm.repeatDays) }}</span>
                  </div>
                  <div class="alarm-actions">
                    <el-switch v-model="alarm.enabled" @change="toggleAlarm(alarm)" />
                    <el-button size="small" @click="deleteAlarm(alarm.id)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="日程" name="schedule">
            <div class="tab-content">
              <div class="add-btn">
                <el-button type="primary" @click="openScheduleModal">添加日程</el-button>
              </div>
              <div class="schedule-list">
                <div v-for="schedule in schedules" :key="schedule.id" class="schedule-item">
                  <div class="schedule-info">
                    <span class="schedule-title">{{ schedule.title }}</span>
                    <span class="schedule-time">{{ formatDateTime(schedule.startTime) }}</span>
                    <span v-if="schedule.location" class="schedule-location">{{ schedule.location }}</span>
                  </div>
                  <div class="schedule-actions">
                    <el-checkbox v-model="schedule.completed" @change="updateSchedule(schedule)" />
                    <el-button size="small" @click="deleteSchedule(schedule.id)">删除</el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="睡眠" name="sleep">
            <div class="tab-content">
              <div class="sleep-form">
                <el-form :model="sleepForm">
                  <el-form-item label="睡觉时间">
                    <el-time-select v-model="sleepForm.sleepTime" :picker-options="{ start: '18:00', step: '00:30', end: '02:00' }" />
                  </el-form-item>
                  <el-form-item label="起床时间">
                    <el-time-select v-model="sleepForm.wakeTime" :picker-options="{ start: '05:00', step: '00:30', end: '12:00' }" />
                  </el-form-item>
                  <el-form-item label="睡眠质量">
                    <el-rate v-model="sleepForm.quality" :max="10" show-score />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="recordSleep">记录睡眠</el-button>
                  </el-form-item>
                </el-form>
              </div>
              <div v-if="sleepRecords.length > 0" class="sleep-summary">
                <h4>睡眠统计</h4>
                <div class="summary-stats">
                  <div class="stat-item">
                    <span class="stat-label">平均时长</span>
                    <span class="stat-value">{{ sleepSummary.avgDuration || 0 }}小时</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">平均质量</span>
                    <span class="stat-value">{{ sleepSummary.avgQuality || 0 }}分</span>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <div v-else class="create-elf">
      <div class="create-card">
        <div class="create-avatar">✨</div>
        <h3>创建你的小精灵</h3>
        <el-input v-model="newElfName" placeholder="给小精灵起个名字" />
        <div class="theme-options">
          <span v-for="theme in themes" :key="theme.value" 
                :class="['theme-option', { selected: newElfTheme === theme.value }]"
                @click="newElfTheme = theme.value">
            {{ theme.label }}
          </span>
        </div>
        <el-button type="primary" @click="createElf">创建</el-button>
      </div>
    </div>

    <el-dialog title="闹钟设置" v-model="alarmModalVisible">
      <el-form :model="alarmForm">
        <el-form-item label="标题" required>
          <el-input v-model="alarmForm.title" />
        </el-form-item>
        <el-form-item label="时间" required>
          <el-time-picker v-model="alarmForm.time" />
        </el-form-item>
        <el-form-item label="重复">
          <el-select v-model="alarmForm.repeatDays">
            <el-option label="每天" value="daily" />
            <el-option label="工作日" value="workday" />
            <el-option label="周末" value="weekend" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alarmModalVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAlarm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog title="日程设置" v-model="scheduleModalVisible">
      <el-form :model="scheduleForm">
        <el-form-item label="标题" required>
          <el-input v-model="scheduleForm.title" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-time-picker v-model="scheduleForm.startTime" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-time-picker v-model="scheduleForm.endTime" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="scheduleForm.location" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleModalVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSchedule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

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

const elf = ref(null)
const activeTab = ref('alarm')
const alarms = ref([])
const schedules = ref([])
const sleepRecords = ref([])
const sleepSummary = ref({})

const newElfName = ref('')
const newElfTheme = ref('default')
const themes = [
  { label: '默认', value: 'default' },
  { label: '可爱', value: 'cute' },
  { label: '暗黑', value: 'dark' },
  { label: '梦幻', value: 'fantasy' }
]

const alarmModalVisible = ref(false)
const alarmForm = ref({ title: '', time: '', repeatDays: 'daily' })

const scheduleModalVisible = ref(false)
const scheduleForm = ref({ title: '', startTime: '', endTime: '', location: '' })

const sleepForm = ref({ sleepTime: '22:00', wakeTime: '07:00', quality: 7 })

// 关系养成
const relationship = ref({})
const interactionHistory = ref([])

const relProgress = computed(() => {
  const a = relationship.value.affection || 0
  return Math.max(0, Math.min(100, Math.round(a)))
})

const interactionTypeLabel = (type) => {
  const map = { chat: '聊天', feed: '投喂', play: '玩耍', sleep: '睡觉', daily_reward: '每日奖励' }
  return map[type] || type
}

const loadRelationship = async () => {
  if (!elf.value) return
  try {
    const res = await request.get(`/api/elf/${elf.value.id}/relationship`)
    if (res.code === 200) relationship.value = res.data || {}
    const hist = await request.get(`/api/elf/${elf.value.id}/interactions`, { params: { page: 1, size: 10 } })
    if (hist.code === 200) interactionHistory.value = hist.data?.records || []
  } catch (e) { console.error(e) }
}

const doInteract = async (interactionType) => {
  if (!elf.value) return
  try {
    await request.post(`/api/elf/${elf.value.id}/interact`, { interactionType })
    ElMessage.success('互动成功')
    loadRelationship()
  } catch (e) {
    ElMessage.error('互动失败')
  }
}

const claimDailyReward = async () => {
  if (!elf.value) return
  try {
    const res = await request.post(`/api/elf/${elf.value.id}/daily-reward`)
    if (res.code === 200) {
      ElMessage.success(res.data?.message || '已领取每日奖励')
      loadRelationship()
    }
  } catch (e) {
    ElMessage.error('领取失败')
  }
}

const defaultAvatar = 'https://api.dicebear.com/7.x/bottts/svg?seed=elf'

const getUserId = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.id || user.userId
}

const getPersonalityText = (personality) => {
  const map = { friendly: '友好', shy: '害羞', energetic: '活泼', calm: '冷静' }
  return map[personality] || personality
}

const getThemeText = (theme) => {
  const map = { default: '默认', cute: '可爱', dark: '暗黑', fantasy: '梦幻' }
  return map[theme] || theme
}

const getRepeatText = (repeat) => {
  const map = { daily: '每天', workday: '工作日', weekend: '周末' }
  return map[repeat] || repeat
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const d = new Date(dateTime)
  return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${d.getMinutes().toString().padStart(2, '0')}`
}

const loadElf = async () => {
  const userId = getUserId()
  if (!userId) return
  try {
    const response = await request.get('/api/elf/get', { params: { userId } })
    if (response.code === 200) {
      elf.value = response.data
      loadAlarms()
      loadSchedules()
      loadSleep()
      loadRelationship()
    }
  } catch (e) {
    console.error(e)
  }
}

const loadAlarms = async () => {
  if (!elf.value) return
  try {
    const response = await request.get('/api/elf/alarms', { params: { elfId: elf.value.id } })
    if (response.code === 200) {
      alarms.value = response.data
    }
  } catch (e) { console.error(e) }
}

const loadSchedules = async () => {
  if (!elf.value) return
  try {
    const response = await request.get('/api/elf/schedules', { params: { elfId: elf.value.id } })
    if (response.code === 200) {
      schedules.value = response.data
    }
  } catch (e) { console.error(e) }
}

const loadSleep = async () => {
  if (!elf.value) return
  try {
    const records = await request.get('/api/elf/sleep/records', { params: { elfId: elf.value.id } })
    if (records.code === 200) sleepRecords.value = records.data
    
    const summary = await request.get('/api/elf/sleep/summary', { params: { elfId: elf.value.id } })
    if (summary.code === 200) sleepSummary.value = summary.data
  } catch (e) { console.error(e) }
}

const createElf = async () => {
  const userId = getUserId()
  if (!userId || !newElfName.value) {
    ElMessage.warning('请输入小精灵名称')
    return
  }
  try {
    const response = await request.post('/api/elf/create', {
      userId, name: newElfName.value, theme: newElfTheme.value
    })
    if (response.code === 200) {
      ElMessage.success('小精灵创建成功！')
      elf.value = response.data
    }
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

const addExp = async () => {
  if (!elf.value) return
  try {
    await request.post(`/api/elf/${elf.value.id}/experience`, { experience: 10 })
    elf.value.experience += 10
    elf.value.level = 1 + Math.floor(elf.value.experience / 100)
    ElMessage.success('经验已增加！')
  } catch (e) { console.error(e) }
}

const openAlarmModal = () => {
  alarmForm.value = { title: '', time: '', repeatDays: 'daily' }
  alarmModalVisible.value = true
}

const saveAlarm = async () => {
  if (!alarmForm.value.title || !alarmForm.value.time) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await request.post('/api/elf/alarm', {
      elfId: elf.value.id, userId: getUserId(),
      title: alarmForm.value.title, time: alarmForm.value.time,
      repeatDays: alarmForm.value.repeatDays
    })
    alarmModalVisible.value = false
    loadAlarms()
    ElMessage.success('闹钟添加成功')
  } catch (e) { console.error(e) }
}

const toggleAlarm = async (alarm) => {
  try {
    await request.put(`/api/elf/alarm/${alarm.id}`, { enabled: alarm.enabled })
  } catch (e) { console.error(e) }
}

const deleteAlarm = async (id) => {
  try {
    await request.delete(`/api/elf/alarm/${id}`)
    loadAlarms()
    ElMessage.success('已删除')
  } catch (e) { console.error(e) }
}

const openScheduleModal = () => {
  scheduleForm.value = { title: '', startTime: '', endTime: '', location: '' }
  scheduleModalVisible.value = true
}

const saveSchedule = async () => {
  if (!scheduleForm.value.title || !scheduleForm.value.startTime) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await request.post('/api/elf/schedule', {
      elfId: elf.value.id, userId: getUserId(),
      title: scheduleForm.value.title,
      startTime: scheduleForm.value.startTime,
      endTime: scheduleForm.value.endTime,
      location: scheduleForm.value.location
    })
    scheduleModalVisible.value = false
    loadSchedules()
    ElMessage.success('日程添加成功')
  } catch (e) { console.error(e) }
}

const updateSchedule = async (schedule) => {
  try {
    await request.put(`/api/elf/schedule/${schedule.id}`, { completed: schedule.completed })
  } catch (e) { console.error(e) }
}

const deleteSchedule = async (id) => {
  try {
    await request.delete(`/api/elf/schedule/${id}`)
    loadSchedules()
    ElMessage.success('已删除')
  } catch (e) { console.error(e) }
}

const recordSleep = async () => {
  try {
    await request.post('/api/elf/sleep', {
      elfId: elf.value.id, userId: getUserId(),
      sleepTime: sleepForm.value.sleepTime,
      wakeTime: sleepForm.value.wakeTime,
      quality: sleepForm.value.quality
    })
    loadSleep()
    ElMessage.success('睡眠记录成功')
  } catch (e) { console.error(e) }
}

const openSettings = () => {
  ElMessage.info('设置功能开发中')
}

onMounted(() => {
  loadElf()
})
</script>

<style scoped>
.relationship-card {
  background: var(--bg-secondary, #fff);
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
  margin: 12px 0;
}
.rel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.rel-title { font-weight: 600; font-size: 15px; }
.rel-level { color: #409eff; font-weight: 600; }
.rel-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 6px 0;
}
.rel-label { width: 50px; color: #909399; font-size: 13px; }
.rel-value { font-size: 13px; color: #606266; }
.rel-next { margin-left: auto; font-size: 12px; color: #e6a23c; }
.rel-next.max { color: #67c23a; }
.rel-actions { margin: 10px 0; display: flex; gap: 8px; flex-wrap: wrap; }
.interaction-timeline { margin-top: 8px; }
.timeline-item {
  display: flex;
  gap: 12px;
  padding: 6px 0;
  border-bottom: 1px dashed #ebeef5;
  font-size: 13px;
}
.t-type { color: #409eff; width: 50px; }
.t-points { color: #67c23a; }
.t-time { margin-left: auto; color: #909399; }
.t-empty { color: #909399; font-size: 13px; padding: 6px 0; }
.elf-container {
  min-height: 100vh;
  background: var(--bg-primary);
  padding: var(--spacing-xl);
}

.elf-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
  position: relative;
}

.elf-header h2 {
  margin: 0 0 var(--spacing-sm) 0;
  color: var(--text-primary);
}

.elf-header p {
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

.elf-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-xl);
  display: flex;
  align-items: center;
  gap: var(--spacing-xl);
  box-shadow: var(--shadow-md);
}

.elf-avatar {
  position: relative;
}

.elf-avatar img {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
}

.elf-level {
  position: absolute;
  bottom: 5px;
  right: 5px;
  background: var(--color-primary);
  color: white;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.experience-bar {
  width: 100%;
  height: 6px;
  background: var(--border-primary);
  border-radius: 3px;
  margin-top: 8px;
  overflow: hidden;
}

.experience-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 3px;
  transition: width 0.3s;
}

.elf-info {
  flex: 1;
}

.elf-info h3 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: var(--text-primary);
}

.elf-personality {
  margin: 0 0 8px 0;
  color: var(--text-secondary);
}

.theme-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  background: var(--bg-hover);
}

.theme-tag.cute { background: #ffe4e6; color: #d63384; }
.theme-tag.dark { background: #343a40; color: #adb5bd; }
.theme-tag.fantasy { background: #e6d5ff; color: #6f42c1; }

.elf-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.elf-tabs {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.tab-content {
  padding: var(--spacing-xl);
}

.add-btn {
  margin-bottom: var(--spacing-md);
}

.alarm-list, .schedule-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.alarm-item, .schedule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.alarm-time {
  font-size: 24px;
  font-weight: bold;
  color: var(--text-primary);
  margin-right: var(--spacing-md);
}

.alarm-title {
  font-size: 16px;
  color: var(--text-primary);
  margin-right: var(--spacing-md);
}

.alarm-repeat {
  font-size: 12px;
  color: var(--text-muted);
  background: var(--bg-hover);
  padding: 2px 8px;
  border-radius: 10px;
}

.schedule-title {
  font-size: 16px;
  color: var(--text-primary);
  margin-right: var(--spacing-md);
}

.schedule-time {
  font-size: 14px;
  color: var(--text-secondary);
  margin-right: var(--spacing-md);
}

.schedule-location {
  font-size: 12px;
  color: var(--text-muted);
}

.create-elf {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 50vh;
}

.create-card {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xxl);
  text-align: center;
  box-shadow: var(--shadow-lg);
}

.create-avatar {
  font-size: 64px;
  margin-bottom: var(--spacing-md);
}

.create-card h3 {
  margin: 0 0 var(--spacing-md) 0;
}

.theme-options {
  display: flex;
  gap: var(--spacing-sm);
  margin: var(--spacing-md) 0;
  justify-content: center;
}

.theme-option {
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  background: var(--bg-hover);
  transition: all var(--transition-fast);
}

.theme-option.selected {
  background: var(--color-primary);
  color: white;
}

.sleep-form {
  max-width: 400px;
  margin: 0 auto;
  padding: var(--spacing-xl);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.sleep-summary {
  margin-top: var(--spacing-xl);
}

.sleep-summary h4 {
  margin: 0 0 var(--spacing-md) 0;
  color: var(--text-primary);
}

.summary-stats {
  display: flex;
  gap: var(--spacing-xl);
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--text-primary);
}
</style>