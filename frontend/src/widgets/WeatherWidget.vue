<template>
  <div class="h-full flex flex-col items-center justify-center p-3">
    <div class="text-center w-full">
      <div class="text-4xl mb-2">{{ weatherIcon }}</div>
      <div class="text-3xl font-bold" :style="{ color: textColor }">
        {{ temperature }}°{{ unitSymbol }}
      </div>
      <div 
        class="text-sm mt-1 opacity-70 cursor-pointer hover:opacity-100 transition-opacity" 
        :style="{ color: textColor }"
        @click="showCitySelector = true"
      >
        {{ config.city }}
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="inline ml-1">
          <path d="M19 9l-7 7-7-7"/>
        </svg>
      </div>
      <div class="text-sm mt-2 opacity-70" :style="{ color: textColor }">
        {{ description }}
      </div>
    </div>

    <div v-if="showCitySelector" class="city-selector-overlay" @click.self="showCitySelector = false">
      <div class="city-selector">
        <div class="selector-header">
          <h4>选择城市</h4>
          <button @click="showCitySelector = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="city-list">
          <button 
            v-for="city in cityList" 
            :key="city"
            @click="selectCity(city)"
            :class="{ active: config.city === city }"
          >
            {{ city }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { createLogger } from '../utils/logger'

const logger = createLogger('WeatherWidget')

const props = defineProps({
  config: { type: Object, default: () => ({ city: 'Beijing', unit: 'celsius' }) },
  style: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:config'])

const temperature = ref(26)
const description = ref('晴朗')
const weatherIcon = ref('☀️')
const showCitySelector = ref(false)
let timer = null

const textColor = computed(() => props.style?.color || props.style?.textColor || '#ffffff')
const unitSymbol = computed(() => props.config.unit === 'celsius' ? 'C' : 'F')

const cityList = [
  'Beijing', 'Shanghai', 'Guangzhou', 'Shenzhen', 
  'Chengdu', 'Hangzhou', 'Wuhan', 'Nanjing',
  'Chongqing', 'Tianjin', 'Xi\'an', 'Suzhou'
]

const weatherData = [
  { temp: 26, desc: '晴朗', icon: '☀️' },
  { temp: 24, desc: '多云', icon: '⛅' },
  { temp: 22, desc: '阴天', icon: '☁️' },
  { temp: 20, desc: '小雨', icon: '🌧️' },
  { temp: 28, desc: '晴转多云', icon: '🌤️' }
]

function selectCity(city) {
  emit('update:config', { ...props.config, city })
  showCitySelector.value = false
  updateWeather()
}

function updateWeather() {
  const data = weatherData[Math.floor(Math.random() * weatherData.length)]
  temperature.value = data.temp
  description.value = data.desc
  weatherIcon.value = data.icon
  logger.debug('WEATHER_UPDATE', 'Weather data updated', { city: props.config.city, temp: temperature.value, desc: description.value })
}

onMounted(() => {
  logger.info('LIFECYCLE', 'WeatherWidget mounted', { city: props.config.city, unit: props.config.unit })
  updateWeather()
  timer = setInterval(updateWeather, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  logger.info('LIFECYCLE', 'WeatherWidget unmounted')
})
</script>

<style scoped>
.city-selector-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.city-selector {
  width: 280px;
  background: #1e293b;
  border-radius: 12px;
  overflow: hidden;
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.selector-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.selector-header button {
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

.city-list {
  max-height: 250px;
  overflow-y: auto;
}

.city-list button {
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
}

.city-list button:hover {
  background: rgba(255, 255, 255, 0.1);
}

.city-list button.active {
  background: rgba(79, 70, 229, 0.3);
  color: #ffffff;
}
</style>