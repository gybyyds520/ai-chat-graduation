<template>
  <div class="calendar-widget">
    <div class="calendar-header">
      <button @click="prevMonth" class="nav-btn">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </button>
      <div class="calendar-title" :style="{ color: textColor }">{{ currentYear }}年{{ currentMonth }}月</div>
      <button @click="nextMonth" class="nav-btn">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
        </svg>
      </button>
    </div>
    
    <div class="weekdays">
      <div v-for="day in weekDays" :key="day" class="weekday" :style="{ color: textColor }">{{ day }}</div>
    </div>
    
    <div class="calendar-grid">
      <div 
        v-for="(date, index) in calendarDates" 
        :key="index"
        class="calendar-day"
        :class="[
          date.isCurrentMonth ? '' : 'other-month',
          date.isToday && config.highlightToday ? 'today' : '',
          !config.showWeekend && date.isWeekend ? 'hidden-day' : ''
        ]"
        :style="{ color: getDayColor(date) }"
      >
        {{ date.day }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { createLogger } from '../utils/logger'

const logger = createLogger('CalendarWidget')

const props = defineProps({
  config: { type: Object, default: () => ({ showWeekend: true, highlightToday: true }) },
  style: { type: Object, default: () => ({}) }
})

const currentDate = ref(new Date())
const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const textColor = computed(() => props.style?.color || props.style?.textColor || '#ffffff')
const currentYear = computed(() => currentDate.value.getFullYear())
const currentMonth = computed(() => currentDate.value.getMonth() + 1)

function getDayColor(date) {
  if (date.isToday && props.config.highlightToday) {
    return '#3b82f6'
  }
  if (!date.isCurrentMonth) {
    return 'rgba(255, 255, 255, 0.3)'
  }
  if (!props.config.showWeekend && date.isWeekend) {
    return 'rgba(255, 255, 255, 0.3)'
  }
  return textColor.value
}

const calendarDates = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  const dates = []
  const startDay = firstDay.getDay()
  const prevMonthLastDay = new Date(year, month, 0).getDate()
  
  for (let i = startDay - 1; i >= 0; i--) {
    const day = prevMonthLastDay - i
    const date = new Date(year, month - 1, day)
    dates.push({ day, isCurrentMonth: false, isToday: false, isWeekend: date.getDay() === 0 || date.getDay() === 6 })
  }
  
  for (let i = 1; i <= lastDay.getDate(); i++) {
    const date = new Date(year, month, i)
    const dateToCompare = new Date(year, month, i)
    dateToCompare.setHours(0, 0, 0, 0)
    dates.push({ day: i, isCurrentMonth: true, isToday: dateToCompare.getTime() === today.getTime(), isWeekend: date.getDay() === 0 || date.getDay() === 6 })
  }
  
  const remaining = 42 - dates.length
  for (let i = 1; i <= remaining; i++) {
    const date = new Date(year, month + 1, i)
    dates.push({ day: i, isCurrentMonth: false, isToday: false, isWeekend: date.getDay() === 0 || date.getDay() === 6 })
  }
  
  return dates
})

function prevMonth() {
  logger.info('CALENDAR_NAV', 'Navigating to previous month')
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  currentDate.value = new Date(year, month - 1, 1)
}

function nextMonth() {
  logger.info('CALENDAR_NAV', 'Navigating to next month')
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  currentDate.value = new Date(year, month + 1, 1)
}

onMounted(() => {
  logger.info('LIFECYCLE', 'CalendarWidget mounted', { year: currentYear.value, month: currentMonth.value })
})
</script>

<style scoped>
.calendar-widget {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 12px;
  box-sizing: border-box;
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.nav-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: inherit;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.nav-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.calendar-title {
  font-size: 14px;
  font-weight: 600;
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 11px;
  opacity: 0.6;
  padding: 4px 0;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  flex: 1;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 24px;
}

.calendar-day:hover {
  background: rgba(255, 255, 255, 0.1);
}

.calendar-day.other-month {
  opacity: 0.3;
}

.calendar-day.today {
  background: rgba(59, 130, 246, 0.3);
  font-weight: 600;
}

.calendar-day.hidden-day {
  opacity: 0.3;
  pointer-events: none;
}

@media (max-width: 300px) {
  .calendar-widget {
    padding: 8px;
  }
  
  .calendar-title {
    font-size: 12px;
  }
  
  .weekday {
    font-size: 10px;
  }
  
  .calendar-day {
    font-size: 11px;
  }
}
</style>