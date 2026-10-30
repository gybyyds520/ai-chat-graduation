<template>
  <aside class="w-72 bg-slate-800/90 border-l border-white/10 flex flex-col">
    <div class="p-4 border-b border-white/10">
      <h2 class="text-sm font-medium text-white/80 uppercase tracking-wider">属性设置</h2>
    </div>
    
    <div v-if="!widget" class="flex-1 flex flex-col items-center justify-center text-slate-400 p-6">
      <svg class="w-12 h-12 mb-3 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122"/>
      </svg>
      <p class="text-sm">选择一个小部件</p>
      <p class="text-xs mt-1">查看和编辑属性</p>
    </div>
    
    <div v-else class="flex-1 overflow-auto p-4 space-y-6">
      <div>
        <h3 class="text-sm font-medium text-white/70 mb-3 flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"/>
          </svg>
          基本设置
        </h3>
        
        <div class="grid grid-cols-2 gap-3 mb-3">
          <div>
            <label class="text-xs text-white/60 block mb-1">宽度</label>
            <input 
              type="number" 
              :value="widget.width"
              @input="updateProperty('width', Number($event.target.value))"
              class="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
          <div>
            <label class="text-xs text-white/60 block mb-1">高度</label>
            <input 
              type="number" 
              :value="widget.height"
              @input="updateProperty('height', Number($event.target.value))"
              class="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
        </div>
        
        <div class="grid grid-cols-2 gap-3 mb-3">
          <div>
            <label class="text-xs text-white/60 block mb-1">X</label>
            <input 
              type="number" 
              :value="widget.x"
              @input="updateProperty('x', Number($event.target.value))"
              class="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
          <div>
            <label class="text-xs text-white/60 block mb-1">Y</label>
            <input 
              type="number" 
              :value="widget.y"
              @input="updateProperty('y', Number($event.target.value))"
              class="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm focus:border-blue-500 focus:outline-none"
            />
          </div>
        </div>
        
        <div class="mb-3">
          <label class="text-xs text-white/60 block mb-2">透明度: {{ widget.transparency }}%</label>
          <input 
            type="range" 
            min="10" 
            max="100" 
            :value="widget.transparency"
            @input="updateProperty('transparency', Number($event.target.value))"
            class="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer"
          />
        </div>
        
        <div class="flex items-center justify-between">
          <span class="text-sm text-white/70">始终置顶</span>
          <button 
            @click="updateProperty('alwaysOnTop', !widget.alwaysOnTop)"
            :class="['w-12 h-6 rounded-full transition-colors relative', widget.alwaysOnTop ? 'bg-blue-600' : 'bg-white/20']"
          >
            <span :class="['absolute top-1 w-4 h-4 bg-white rounded-full transition-transform', widget.alwaysOnTop ? 'left-7' : 'left-1']" />
          </button>
        </div>
      </div>
      
      <div>
        <h3 class="text-sm font-medium text-white/70 mb-3 flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01"/>
          </svg>
          样式设置
        </h3>
        
        <div class="flex items-center justify-between mb-3">
          <span class="text-sm text-white/70">背景颜色</span>
          <input 
            type="color" 
            :value="widget.style?.textColor || '#ffffff'"
            @input="updateStyle('textColor', $event.target.value)"
            class="w-10 h-10 rounded-lg cursor-pointer border-0"
          />
        </div>
        
        <div>
          <label class="text-xs text-white/60 block mb-2">圆角: {{ parseInt(widget.style?.borderRadius) || 12 }}px</label>
          <input 
            type="range" 
            min="0" 
            max="30" 
            :value="parseInt(widget.style?.borderRadius) || 12"
            @input="updateStyle('borderRadius', $event.target.value + 'px')"
            class="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer"
          />
        </div>
      </div>
      
      <div v-if="widgetConfigOptions.length > 0">
        <h3 class="text-sm font-medium text-white/70 mb-3">功能配置</h3>
        <div class="space-y-3">
          <div v-for="option in widgetConfigOptions" :key="option.key">
            <div v-if="option.type === 'boolean'" class="flex items-center justify-between">
              <span class="text-sm text-white/70">{{ option.label }}</span>
              <button 
                @click="updateConfig(option.key, !widget.config?.[option.key])"
                :class="['w-12 h-6 rounded-full transition-colors relative', widget.config?.[option.key] ? 'bg-blue-600' : 'bg-white/20']"
              >
                <span :class="['absolute top-1 w-4 h-4 bg-white rounded-full transition-transform', widget.config?.[option.key] ? 'left-7' : 'left-1']" />
              </button>
            </div>
            <div v-else-if="option.type === 'input'">
              <label class="text-xs text-white/60 block mb-1">{{ option.label }}</label>
              <input 
                type="text" 
                :value="widget.config?.[option.key] || option.default"
                @input="updateConfig(option.key, $event.target.value)"
                class="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm focus:border-blue-500 focus:outline-none"
              />
            </div>
            <div v-else-if="option.type === 'select'">
              <label class="text-xs text-white/60 block mb-1">{{ option.label }}</label>
              <select 
                :value="widget.config?.[option.key] || option.default"
                @change="updateConfig(option.key, $event.target.value)"
                class="w-full px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-white text-sm focus:border-blue-500 focus:outline-none"
              >
                <option v-for="opt in option.options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ widget: Object })
const emit = defineEmits(['update'])

const widgetConfigOptions = computed(() => {
  if (!props.widget) return []
  
  const configs = {
    clock: [
      { key: 'format', type: 'select', label: '时间格式', default: '24h', options: [
        { value: '24h', label: '24小时制' },
        { value: '12h', label: '12小时制' }
      ]},
      { key: 'showDate', type: 'boolean', label: '显示日期', default: true }
    ],
    weather: [
      { key: 'city', type: 'input', label: '城市', default: 'Beijing' },
      { key: 'unit', type: 'select', label: '温度单位', default: 'celsius', options: [
        { value: 'celsius', label: '摄氏度' },
        { value: 'fahrenheit', label: '华氏度' }
      ]}
    ],
    calendar: [
      { key: 'showWeekend', type: 'boolean', label: '显示周末', default: true },
      { key: 'highlightToday', type: 'boolean', label: '高亮今天', default: true }
    ],
    notes: [
      { key: 'content', type: 'input', label: '内容', default: '在这里输入笔记...' },
      { key: 'autoSave', type: 'boolean', label: '自动保存', default: true }
    ],
    systemMonitor: [
      { key: 'showCpu', type: 'boolean', label: '显示CPU', default: true },
      { key: 'showMemory', type: 'boolean', label: '显示内存', default: true },
      { key: 'showNetwork', type: 'boolean', label: '显示网络', default: false }
    ]
  }
  
  return configs[props.widget.type] || []
})

function updateProperty(key, value) {
  emit('update', { [key]: value })
}

function updateStyle(key, value) {
  emit('update', { style: { ...props.widget.style, [key]: value } })
}

function updateConfig(key, value) {
  emit('update', { config: { ...props.widget.config, [key]: value } })
}
</script>