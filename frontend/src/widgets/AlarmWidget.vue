<template>
  <div class="alarm-widget">
    <div class="alarm-header">
      <h3 class="alarm-title">⏰ 闹钟</h3>
      <button class="add-alarm-btn" @click="showAddModal = true">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
      </button>
    </div>

    <div class="alarm-list">
      <div 
        v-for="alarm in alarms" 
        :key="alarm.id"
        class="alarm-item"
        :class="{ disabled: !alarm.enabled, triggered: alarm.id === triggeredAlarmId }"
      >
        <div class="alarm-left">
          <button class="toggle-btn" @click="toggleAlarm(alarm.id)">
            <div class="toggle-circle" :class="{ active: alarm.enabled }"></div>
          </button>
          <div class="alarm-info">
            <span class="alarm-time">{{ alarm.time }}</span>
            <span v-if="alarm.label" class="alarm-label">{{ alarm.label }}</span>
            <span class="alarm-repeat">{{ getRepeatText(alarm) }}</span>
          </div>
        </div>
        <div class="alarm-right">
          <button class="action-btn edit" @click="editAlarm(alarm)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
          </button>
          <button class="action-btn delete" @click="deleteAlarm(alarm.id)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 6h18M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
            </svg>
          </button>
        </div>
      </div>

      <div v-if="alarms.length === 0" class="empty-alarm">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/>
          <polyline points="12 6 12 12 16 14"/>
        </svg>
        <p>暂无闹钟</p>
        <button @click="showAddModal = true">添加闹钟</button>
      </div>
    </div>

    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ isEditing ? '编辑闹钟' : '添加闹钟' }}</h3>
          <button class="modal-close" @click="closeModal">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <div class="form-group">
            <label>时间</label>
            <input 
              type="time" 
              v-model="formData.time"
              class="time-input"
              required
            />
          </div>

          <div class="form-group">
            <label>标签</label>
            <input 
              type="text" 
              v-model="formData.label"
              placeholder="输入闹钟标签..."
              class="text-input"
            />
          </div>

          <div class="form-group">
            <label>重复模式</label>
            <div class="repeat-options">
              <button 
                v-for="option in repeatOptions" 
                :key="option.value"
                @click="formData.repeat = option.value"
                :class="{ active: formData.repeat === option.value }"
                class="repeat-btn"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <div v-if="formData.repeat === 'custom'" class="form-group">
            <label>选择重复日期</label>
            <div class="day-options">
              <button 
                v-for="day in dayOptions" 
                :key="day.value"
                @click="toggleDay(day.value)"
                :class="{ active: formData.repeatDays.includes(day.value) }"
                class="day-btn"
              >
                {{ day.label }}
              </button>
            </div>
          </div>

          <div class="form-group">
            <label>铃声</label>
            <select v-model="formData.ringtone" class="select-input">
              <option v-for="ringtone in ringtones" :key="ringtone.id" :value="ringtone.id">
                {{ ringtone.name }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label>音量: {{ formData.volume }}%</label>
            <input 
              type="range" 
              min="0" 
              max="100" 
              v-model="formData.volume"
              class="range-input"
            />
          </div>

          <div class="form-group">
            <label class="checkbox-label">
              <input 
                type="checkbox" 
                v-model="formData.snoozeEnabled"
              />
              <span>启用贪睡</span>
            </label>
          </div>

          <div v-if="formData.snoozeEnabled" class="form-group">
            <label>贪睡间隔（分钟）</label>
            <select v-model="formData.snoozeInterval" class="select-input">
              <option :value="5">5分钟</option>
              <option :value="10">10分钟</option>
              <option :value="15">15分钟</option>
              <option :value="30">30分钟</option>
            </select>
          </div>
        </div>

        <div class="modal-footer">
          <button class="modal-btn" @click="closeModal">取消</button>
          <button class="modal-btn primary" @click="saveAlarm">保存</button>
        </div>
      </div>
    </div>

    <div v-if="showTriggeredModal" class="triggered-overlay">
      <div class="triggered-modal">
        <div class="triggered-icon">⏰</div>
        <h3>闹钟响了！</h3>
        <p>{{ triggeredAlarm?.label || triggeredAlarm?.time }}</p>
        <div class="triggered-actions">
          <button v-if="triggeredAlarm?.snoozeEnabled" class="snooze-btn" @click="snoozeAlarm">贪睡 {{ triggeredAlarm.snoozeInterval }} 分钟</button>
          <button class="dismiss-btn" @click="dismissAlarm">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import alarmManager from '../utils/alarmManager'

const alarms = ref([])
const showAddModal = ref(false)
const showEditModal = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const showTriggeredModal = ref(false)
const triggeredAlarmId = ref(null)
const triggeredAlarm = ref(null)

const repeatOptions = [
  { value: 'once', label: '一次' },
  { value: 'daily', label: '每天' },
  { value: 'weekdays', label: '工作日' },
  { value: 'weekend', label: '周末' },
  { value: 'custom', label: '自定义' }
]

const dayOptions = [
  { value: 'mon', label: '一' },
  { value: 'tue', label: '二' },
  { value: 'wed', label: '三' },
  { value: 'thu', label: '四' },
  { value: 'fri', label: '五' },
  { value: 'sat', label: '六' },
  { value: 'sun', label: '日' }
]

const ringtones = alarmManager.getAvailableRingtones()

const formData = reactive({
  time: '08:00',
  label: '',
  repeat: 'once',
  repeatDays: [],
  ringtone: 'classic',
  volume: 80,
  snoozeEnabled: true,
  snoozeInterval: 5
})

function loadAlarms() {
  alarms.value = alarmManager.getAlarms()
}

function getRepeatText(alarm) {
  const map = {
    once: '只响一次',
    daily: '每天',
    weekdays: '工作日',
    weekend: '周末',
    custom: alarm.repeatDays.map(d => {
      const dayMap = { mon: '一', tue: '二', wed: '三', thu: '四', fri: '五', sat: '六', sun: '日' }
      return dayMap[d]
    }).join('、') || '自定义'
  }
  return map[alarm.repeat] || ''
}

function toggleAlarm(id) {
  alarmManager.toggleAlarm(id)
  loadAlarms()
}

function deleteAlarm(id) {
  if (confirm('确定要删除这个闹钟吗？')) {
    alarmManager.deleteAlarm(id)
    loadAlarms()
  }
}

function editAlarm(alarm) {
  isEditing.value = true
  editingId.value = alarm.id
  showEditModal.value = true
  
  Object.assign(formData, {
    time: alarm.time,
    label: alarm.label || '',
    repeat: alarm.repeat,
    repeatDays: [...(alarm.repeatDays || [])],
    ringtone: alarm.ringtone,
    volume: alarm.volume,
    snoozeEnabled: alarm.snoozeEnabled,
    snoozeInterval: alarm.snoozeInterval
  })
}

function closeModal() {
  showAddModal.value = false
  showEditModal.value = false
  isEditing.value = false
  editingId.value = null
  
  Object.assign(formData, {
    time: '08:00',
    label: '',
    repeat: 'once',
    repeatDays: [],
    ringtone: 'classic',
    volume: 80,
    snoozeEnabled: true,
    snoozeInterval: 5
  })
}

function toggleDay(day) {
  const index = formData.repeatDays.indexOf(day)
  if (index === -1) {
    formData.repeatDays.push(day)
  } else {
    formData.repeatDays.splice(index, 1)
  }
}

function saveAlarm() {
  if (!formData.time) {
    alert('请选择时间')
    return
  }

  if (formData.repeat === 'custom' && formData.repeatDays.length === 0) {
    alert('请至少选择一个重复日期')
    return
  }

  if (isEditing.value && editingId.value) {
    alarmManager.updateAlarm(editingId.value, { ...formData })
  } else {
    alarmManager.createAlarm({ ...formData })
  }

  loadAlarms()
  closeModal()
}

function handleAlarmTriggered(alarm) {
  triggeredAlarmId.value = alarm.id
  triggeredAlarm.value = alarm
  showTriggeredModal.value = true
}

function snoozeAlarm() {
  if (triggeredAlarm.value) {
    alarmManager.snoozeAlarm(triggeredAlarm.value)
  }
  showTriggeredModal.value = false
  triggeredAlarmId.value = null
}

function dismissAlarm() {
  alarmManager.stopRingtone()
  showTriggeredModal.value = false
  triggeredAlarmId.value = null
  triggeredAlarm.value = null
}

let unsubscribe = null

onMounted(() => {
  loadAlarms()
  unsubscribe = alarmManager.subscribe((event, data) => {
    if (event === 'alarmTriggered') {
      handleAlarmTriggered(data)
    } else {
      loadAlarms()
    }
  })
})

onUnmounted(() => {
  if (unsubscribe) {
    unsubscribe()
  }
})
</script>

<style scoped>
.alarm-widget {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  color: #ffffff;
}

.alarm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.alarm-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.add-alarm-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.add-alarm-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.alarm-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.alarm-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  transition: all 0.2s;
}

.alarm-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.alarm-item.disabled {
  opacity: 0.5;
}

.alarm-item.triggered {
  background: rgba(245, 158, 11, 0.2);
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(245, 158, 11, 0); }
}

.alarm-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.toggle-btn {
  width: 40px;
  height: 24px;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
}

.toggle-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.toggle-circle {
  width: 20px;
  height: 20px;
  background: #ffffff;
  border-radius: 50%;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: transform 0.2s;
}

.toggle-circle.active {
  transform: translateX(16px);
  background: #4f46e5;
}

.alarm-info {
  flex: 1;
}

.alarm-time {
  display: block;
  font-size: 18px;
  font-weight: 600;
}

.alarm-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 2px;
}

.alarm-repeat {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 2px;
}

.alarm-right {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.action-btn.delete:hover {
  background: rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.empty-alarm {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  color: rgba(255, 255, 255, 0.5);
}

.empty-alarm svg {
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-alarm p {
  margin: 0 0 16px 0;
  font-size: 14px;
}

.empty-alarm button {
  padding: 8px 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: transparent;
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.empty-alarm button:hover {
  background: rgba(255, 255, 255, 0.1);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  width: 400px;
  background: #1e293b;
  border-radius: 16px;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.modal-close {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 6px;
}

.time-input,
.text-input,
.select-input {
  width: 100%;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: #ffffff;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.time-input:focus,
.text-input:focus,
.select-input:focus {
  border-color: #4f46e5;
}

.time-input::-webkit-calendar-picker-indicator {
  filter: invert(1);
}

.select-input option {
  background: #1e293b;
  color: #ffffff;
}

.range-input {
  width: 100%;
  height: 6px;
  -webkit-appearance: none;
  appearance: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
  outline: none;
}

.range-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  background: #4f46e5;
  border-radius: 50%;
  cursor: pointer;
}

.range-input::-moz-range-thumb {
  width: 18px;
  height: 18px;
  background: #4f46e5;
  border-radius: 50%;
  cursor: pointer;
  border: none;
}

.repeat-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.repeat-btn {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.repeat-btn:hover {
  border-color: rgba(255, 255, 255, 0.4);
}

.repeat-btn.active {
  background: #4f46e5;
  border-color: #4f46e5;
  color: #ffffff;
}

.day-options {
  display: flex;
  gap: 6px;
}

.day-btn {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.day-btn:hover {
  border-color: rgba(255, 255, 255, 0.4);
}

.day-btn.active {
  background: #4f46e5;
  border-color: #4f46e5;
  color: #ffffff;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox-label input {
  width: 18px;
  height: 18px;
  accent-color: #4f46e5;
}

.checkbox-label span {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.modal-btn:not(.primary) {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.modal-btn:not(.primary):hover {
  background: rgba(255, 255, 255, 0.2);
}

.modal-btn.primary {
  background: #4f46e5;
  color: #ffffff;
}

.modal-btn.primary:hover {
  opacity: 0.9;
}

.triggered-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.triggered-modal {
  text-align: center;
  padding: 40px;
  background: #1e293b;
  border-radius: 20px;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from { transform: scale(0.8); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.triggered-icon {
  font-size: 64px;
  margin-bottom: 16px;
  animation: bounce 1s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.triggered-modal h3 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
}

.triggered-modal p {
  margin: 0 0 24px 0;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
}

.triggered-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.snooze-btn,
.dismiss-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.snooze-btn {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.snooze-btn:hover {
  background: rgba(245, 158, 11, 0.3);
}

.dismiss-btn {
  background: #4f46e5;
  color: #ffffff;
}

.dismiss-btn:hover {
  opacity: 0.9;
}
</style>