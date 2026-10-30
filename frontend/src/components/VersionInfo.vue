<template>
  <div class="version-info">
    <div class="version-badge" :style="{ background: versionColor + '20', color: versionColor, borderColor: versionColor + '40' }">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
        <circle cx="8.5" cy="7" r="4"/>
        <line x1="20" y1="8" x2="20" y2="14"/>
        <line x1="23" y1="11" x2="17" y2="11"/>
      </svg>
      <span>{{ versionLabel }}</span>
    </div>

    <div v-if="countdown" class="countdown-container">
      <div class="countdown-label">升级倒计时</div>
      <div class="countdown-timer">
        <div class="countdown-item">
          <span class="countdown-value">{{ countdown.days }}</span>
          <span class="countdown-unit">天</span>
        </div>
        <span class="countdown-separator">:</span>
        <div class="countdown-item">
          <span class="countdown-value">{{ String(countdown.hours).padStart(2, '0') }}</span>
          <span class="countdown-unit">时</span>
        </div>
        <span class="countdown-separator">:</span>
        <div class="countdown-item">
          <span class="countdown-value">{{ String(countdown.minutes).padStart(2, '0') }}</span>
          <span class="countdown-unit">分</span>
        </div>
        <span class="countdown-separator">:</span>
        <div class="countdown-item">
          <span class="countdown-value">{{ String(countdown.seconds).padStart(2, '0') }}</span>
          <span class="countdown-unit">秒</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import versionService from '../utils/versionService'

const versionLabel = ref('')
const versionColor = ref('#6b7280')
const countdown = ref(null)

const updateVersionInfo = () => {
  versionLabel.value = versionService.getVersionTypeLabel()
  versionColor.value = versionService.getVersionTypeColor()
  countdown.value = versionService.getCountdown()
}

let unsubscribe = null

onMounted(() => {
  updateVersionInfo()
  
  if (versionService.hasUpgradeDeadline()) {
    versionService.startCountdown()
  }
  
  unsubscribe = versionService.subscribe((event, data) => {
    if (event === 'countdownUpdate') {
      countdown.value = data
    } else {
      updateVersionInfo()
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
.version-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.version-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid;
}

.countdown-container {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.countdown-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
}

.countdown-timer {
  display: flex;
  align-items: center;
  gap: 4px;
}

.countdown-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 36px;
}

.countdown-value {
  font-size: 16px;
  font-weight: 700;
  color: #f59e0b;
  font-family: 'Monaco', 'Consolas', monospace;
}

.countdown-unit {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}

.countdown-separator {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.3);
  margin-top: -8px;
}
</style>