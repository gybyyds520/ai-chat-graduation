<template>
  <div class="h-full p-4" :style="widgetStyle">
    <div class="space-y-4">
      <div v-if="config.showCpu" class="space-y-1">
        <div class="flex items-center justify-between text-sm">
          <span class="flex items-center gap-2" :style="{ color: textColor }">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
            </svg>
            CPU
          </span>
          <span :style="{ color: textColor }">{{ cpuUsage }}%</span>
        </div>
        <div class="h-2 bg-white/10 rounded-full overflow-hidden">
          <div class="h-full bg-gradient-to-r from-blue-500 to-blue-400 transition-all duration-300" :style="{ width: cpuUsage + '%' }" />
        </div>
      </div>
      
      <div v-if="config.showMemory" class="space-y-1">
        <div class="flex items-center justify-between text-sm">
          <span class="flex items-center gap-2" :style="{ color: textColor }">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"/>
            </svg>
            内存
          </span>
          <span :style="{ color: textColor }">{{ memoryUsage }}%</span>
        </div>
        <div class="h-2 bg-white/10 rounded-full overflow-hidden">
          <div class="h-full bg-gradient-to-r from-green-500 to-green-400 transition-all duration-300" :style="{ width: memoryUsage + '%' }" />
        </div>
      </div>
      
      <div v-if="config.showNetwork" class="space-y-1">
        <div class="flex items-center justify-between text-sm">
          <span class="flex items-center gap-2" :style="{ color: textColor }">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
            </svg>
            网络
          </span>
          <span :style="{ color: textColor }">{{ networkSpeed }}</span>
        </div>
        <div class="h-2 bg-white/10 rounded-full overflow-hidden">
          <div class="h-full bg-gradient-to-r from-purple-500 to-purple-400 transition-all duration-300" :style="{ width: networkUsage + '%' }" />
        </div>
      </div>
      
      <div class="pt-2 border-t border-white/10">
        <div class="text-xs opacity-60" :style="{ color: textColor }">系统: {{ osInfo }}</div>
        <div class="text-xs opacity-60 mt-1" :style="{ color: textColor }">运行时间: {{ uptime }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { createLogger } from '../utils/logger'

const logger = createLogger('SystemMonitorWidget')

const props = defineProps({
  config: { type: Object, default: () => ({ showCpu: true, showMemory: true, showNetwork: false }) },
  style: { type: Object, default: () => ({}) }
})

const cpuUsage = ref(42)
const memoryUsage = ref(65)
const networkUsage = ref(30)
const networkSpeed = ref('12.5 MB/s')
const osInfo = ref('Windows 10')
const uptime = ref('2小时 30分')
let timer = null

const textColor = computed(() => props.style?.textColor || '#ffffff')
const widgetStyle = computed(() => ({
  backgroundColor: props.style?.backgroundColor || 'rgba(30, 30, 46, 0.8)',
  borderRadius: props.style?.borderRadius || '12px',
  border: `1px solid ${props.style?.borderColor || 'rgba(255, 255, 255, 0.1)'}`
}))

function updateSystemInfo() {
  cpuUsage.value = Math.floor(Math.random() * 50) + 20
  memoryUsage.value = Math.floor(Math.random() * 30) + 50
  
  if (props.config.showNetwork) {
    networkUsage.value = Math.floor(Math.random() * 60) + 10
    networkSpeed.value = (Math.random() * 50 + 5).toFixed(1) + ' MB/s'
  }
  
  const parts = uptime.value.split(' ')
  let hours = parseInt(parts[0])
  let minutes = parseInt(parts[2])
  minutes++
  if (minutes >= 60) {
    minutes = 0
    hours++
  }
  uptime.value = `${hours}小时 ${minutes}分`
  
  logger.debug('SYS_UPDATE', 'System info updated', { cpu: cpuUsage.value, memory: memoryUsage.value, network: networkUsage.value })
}

onMounted(() => {
  osInfo.value = navigator.platform || 'Unknown'
  logger.info('LIFECYCLE', 'SystemMonitorWidget mounted', { os: osInfo.value, showCpu: props.config.showCpu, showMemory: props.config.showMemory, showNetwork: props.config.showNetwork })
  updateSystemInfo()
  timer = setInterval(updateSystemInfo, 2000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  logger.info('LIFECYCLE', 'SystemMonitorWidget unmounted')
})
</script>