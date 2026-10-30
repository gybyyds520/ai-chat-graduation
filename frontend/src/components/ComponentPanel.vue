<template>
  <aside class="w-64 bg-slate-800/90 border-r border-white/10 flex flex-col">
    <div class="p-4 border-b border-white/10">
      <h2 class="text-sm font-medium text-white/80 uppercase tracking-wider">组件库</h2>
    </div>
    
    <div class="flex-1 p-4 overflow-auto">
      <div class="space-y-3">
        <div 
          v-for="component in components" 
          :key="component.type"
          class="component-card p-4 flex flex-col items-center text-center cursor-pointer hover:bg-blue-500/20 transition-all rounded-lg"
          draggable="true"
          @dragstart="handleDragStart($event, component.type)"
          @click="$emit('add-widget', component.type)"
        >
          <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500/20 to-purple-500/20 flex items-center justify-center mb-3">
            <svg class="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path v-if="component.type === 'clock'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
              <path v-else-if="component.type === 'weather'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z"/>
              <path v-else-if="component.type === 'calendar'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
              <path v-else-if="component.type === 'notes'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
            </svg>
          </div>
          <span class="text-white text-sm font-medium">{{ component.name }}</span>
          <span class="text-white/50 text-xs mt-1">{{ component.description }}</span>
        </div>
      </div>
    </div>
    
    <div class="p-4 border-t border-white/10">
      <div class="bg-blue-500/10 border border-blue-500/30 rounded-lg p-3">
        <p class="text-blue-300 text-xs flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          点击或拖拽添加到画布
        </p>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref } from 'vue'

defineEmits(['add-widget'])

const components = ref([
  { type: 'clock', name: '时钟', description: '显示当前时间' },
  { type: 'weather', name: '天气', description: '显示天气信息' },
  { type: 'calendar', name: '日历', description: '显示日历' },
  { type: 'notes', name: '便签', description: '记录便签' },
  { type: 'systemMonitor', name: '系统监控', description: '监控系统资源' }
])

function handleDragStart(event, type) {
  event.dataTransfer.setData('widget-type', type)
  event.dataTransfer.effectAllowed = 'copy'
}
</script>
