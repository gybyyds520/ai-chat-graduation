<template>
  <div class="h-full flex flex-col items-center justify-center">
    <div class="text-center">
      <div class="text-4xl font-bold font-mono" :style="{ color: textColor }">
        {{ currentTime }}
      </div>
      <div v-if="config.showDate" class="text-sm mt-2 opacity-70" :style="{ color: textColor }">
        {{ currentDate }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { createLogger } from '../utils/logger'

const logger = createLogger('ClockWidget')

const props = defineProps({
  config: { type: Object, default: () => ({ format: '24h', showDate: true }) },
  style: { type: Object, default: () => ({}) }
})

const currentTime = ref('')
const currentDate = ref('')
let timer = null

const textColor = computed(() => props.style?.color || props.style?.textColor || '#ffffff')

function updateTime() {
  const now = new Date()
  let hours = now.getHours()
  let period = ''
  
  if (props.config.format === '12h') {
    period = hours >= 12 ? ' PM' : ' AM'
    hours = hours % 12 || 12
  }
  
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  
  currentTime.value = `${String(hours).padStart(2, '0')}:${minutes}:${seconds}${period}`
  
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const day = now.getDate()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  currentDate.value = `${year}年${month}月${day}日 ${weekDays[now.getDay()]}`
  
  logger.debug('TIME_UPDATE', 'Time updated', { time: currentTime.value, date: currentDate.value })
}

onMounted(() => {
  logger.info('LIFECYCLE', 'ClockWidget mounted', { format: props.config.format, showDate: props.config.showDate })
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  logger.info('LIFECYCLE', 'ClockWidget unmounted')
})
</script>