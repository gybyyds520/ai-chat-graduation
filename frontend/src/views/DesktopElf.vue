<template>
  <div class="elf-designer">
    <!-- 角色化 toast 提示（替代原生 alert，符合桌宠门禁：无系统原生弹窗） -->
    <transition name="toast-fade">
      <div v-if="toastVisible" class="elf-toast" :class="{ success: toastType === 'success', error: toastType === 'error' }">
        <span class="toast-emoji">{{ toastType === 'error' ? '😿' : '✨' }}</span>
        <span class="toast-text">{{ toastMessage }}</span>
      </div>
    </transition>
    <header class="elf-header">
      <div class="header-left">
        <button class="header-btn" @click="goBack">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
          <span>返回</span>
        </button>
        <div class="logo-wrapper">
          <div class="logo-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2a10 10 0 100 20 10 10 0 000-20z"/>
              <path d="M12 6v6l4 2"/>
            </svg>
          </div>
          <span class="logo-text">桌面小精灵</span>
        </div>
      </div>
      <div class="header-right">
        <button class="header-btn" @click="importConfig">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          <span>导入配置</span>
        </button>
        <button class="header-btn" @click="exportConfig">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          <span>导出配置</span>
        </button>
        <button class="header-btn primary" @click="saveAllWidgets">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3"/>
            <path d="M15 3h6v6"/>
            <path d="M10 14L3 21"/>
            <path d="M14 10L21 3"/>
          </svg>
          <span>保存全部</span>
        </button>
      </div>
    </header>

    <!-- 顶部 Tab 切换：组件设计 / 精灵养成 -->
    <div class="elf-tabs-bar">
      <button
        class="elf-tab"
        :class="{ active: activeTab === 'design' }"
        @click="activeTab = 'design'"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="3" width="7" height="7" rx="1"/>
          <rect x="14" y="3" width="7" height="7" rx="1"/>
          <rect x="14" y="14" width="7" height="7" rx="1"/>
          <rect x="3" y="14" width="7" height="7" rx="1"/>
        </svg>
        <span>组件设计</span>
      </button>
      <button
        class="elf-tab"
        :class="{ active: activeTab === 'grow' }"
        @click="activeTab = 'grow'"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 2L2 7l10 5 10-5-10-5z"/>
          <path d="M2 17l10 5 10-5"/>
          <path d="M2 12l10 5 10-5"/>
        </svg>
        <span>精灵养成</span>
      </button>
    </div>

    <!-- Tab 1: 组件设计（原有 widget 画布设计器） -->
    <div v-show="activeTab === 'design'" class="elf-body">
      <aside class="elf-sidebar">
        <h3 class="sidebar-title">组件库</h3>
        <div class="widget-list">
          <div 
            v-for="widget in availableWidgets" 
            :key="widget.type"
            class="widget-item"
            draggable="true"
            @dragstart="handleDragStart($event, widget.type)"
            @click="addWidget(widget.type)"
          >
            <div class="widget-icon" :style="{ background: widget.color }">
              <span>{{ widget.icon }}</span>
            </div>
            <div class="widget-info">
              <span class="widget-name">{{ widget.name }}</span>
              <span class="widget-desc">{{ widget.desc }}</span>
            </div>
          </div>
        </div>
      </aside>

      <main class="elf-canvas">
        <div class="canvas-container">
          <div 
            class="canvas-area"
            @drop="handleDrop"
            @dragover.prevent
            @dragenter.prevent
          >
            <div v-if="widgets.length === 0" class="empty-state">
              <div class="empty-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" stroke-dasharray="4 4"/>
                  <circle cx="9" cy="9" r="2"/>
                  <path d="M21 15l-5-5L5 21"/>
                </svg>
              </div>
              <p class="empty-title">开始设计你的桌面小精灵</p>
              <p class="empty-desc">从左侧拖拽组件到画布，或点击组件快速添加</p>
            </div>

            <div 
              v-for="widget in widgets" 
              :key="widget.id"
              class="widget-wrapper"
              :class="{ selected: selectedWidget?.id === widget.id }"
              :style="{
                left: widget.x + 'px',
                top: widget.y + 'px',
                width: widget.width + 'px',
                height: widget.height + 'px',
                opacity: widget.transparency / 100
              }"
              @mousedown="startDrag(widget, $event)"
              @click="selectWidget(widget)"
            >
              <div 
                class="widget-content"
                :style="{
                  backgroundColor: widget.style?.backgroundColor || 'rgba(30, 30, 46, 0.8)',
                  borderRadius: widget.style?.borderRadius || '12px',
                  border: `1px solid ${widget.style?.borderColor || 'rgba(255, 255, 255, 0.1)'}`
                }"
              >
                <component 
                  :is="getWidgetComponent(widget.type)" 
                  :config="widget.config"
                  :style="{ color: widget.style?.textColor || '#ffffff' }"
                />
              </div>
              
              <div v-if="selectedWidget?.id === widget.id" class="widget-controls">
                <button class="control-btn delete" @click.stop="removeWidget(widget.id)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 6h18M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>

      <aside class="elf-properties">
        <h3 class="properties-title">属性面板</h3>

        <!-- 精灵形象设置 -->
        <div class="elf-appearance-section">
          <h4 class="section-title">精灵形象</h4>

          <div class="property-block">
            <label class="block-label">角色</label>
            <div class="mini-grid">
              <div
                v-for="c in elfCharacters"
                :key="c.id"
                class="mini-card"
                :class="{ selected: elfCharacterId === c.id }"
                @click="elfCharacterId = c.id"
                :title="c.name"
              >
                <span class="mini-emoji">
                  <AnimeElfSprite
                    v-if="c.category === 'anime'"
                    :character="c"
                    :state="'idle'"
                    :uid="c.id"
                    class="mini-anime-preview"
                  />
                  <template v-else>{{ c.emoji }}</template>
                </span>
              </div>
            </div>
          </div>

          <div class="property-block">
            <label class="block-label">皮肤</label>
            <div class="mini-grid">
              <div
                v-for="s in elfSkins"
                :key="s.id"
                class="mini-card skin-dot"
                :class="{ selected: elfSkinId === s.id }"
                :style="{ background: s.colors.glow || '#4ade80' }"
                @click="elfSkinId = s.id"
                :title="s.name"
              ></div>
            </div>
          </div>

          <div class="property-block">
            <label class="block-label">换装</label>
            <div class="mini-grid">
              <div
                v-for="o in elfOutfits"
                :key="o.id"
                class="mini-card"
                :class="{ selected: elfOutfitId === o.id }"
                @click="elfOutfitId = o.id"
                :title="o.name"
              >
                <span class="mini-emoji">{{ o.emoji }}</span>
              </div>
            </div>
          </div>

          <div class="property-block">
            <label class="block-label">渲染模式</label>
            <div class="mode-row">
              <label class="mode-option">
                <input type="radio" value="3d" v-model="elfMode" />
                <span>3D</span>
              </label>
              <label class="mode-option">
                <input type="radio" value="2d" v-model="elfMode" />
                <span>2D</span>
              </label>
            </div>
          </div>

          <div class="property-block">
            <label class="block-label">自定义形象（图片优先于角色/3D 显示）</label>
            <div class="custom-image-row">
              <div v-if="elfCustomImage" class="custom-image-thumb">
                <img :src="elfCustomImage" alt="自定义形象预览" draggable="false" />
              </div>
              <label class="custom-image-upload">
                {{ elfCustomImage ? '更换图片' : '上传图片' }}
                <input type="file" accept="image/*" @change="onDesignerImageChange" />
              </label>
              <button v-if="elfCustomImage" class="custom-image-clear" @click="elfCustomImage = null">清除</button>
            </div>
            <span class="block-hint">上传后精灵立绘将优先使用该图片（3D 模式自动切换为 2D 显示）</span>
          </div>

          <div class="property-block">
            <label class="block-label">尺寸缩放：{{ elfSize.toFixed(1) }}x</label>
            <input type="range" min="0.5" max="2" step="0.1" v-model.number="elfSize" class="full-range" />
            <div class="size-presets">
              <button
                v-for="preset in elfSizePresets"
                :key="preset.label"
                class="size-preset-btn"
                :class="{ active: Math.abs(elfSize - preset.value) < 0.05 }"
                @click="elfSize = preset.value"
              >{{ preset.label }}</button>
            </div>
          </div>

          <div class="property-block">
            <label class="toggle-row">
              <input type="checkbox" v-model="elfVisible" />
              <span>桌面显示精灵</span>
            </label>
            <label class="toggle-row">
              <input type="checkbox" v-model="elfAutoRotate" />
              <span>自动旋转</span>
            </label>
            <label class="toggle-row">
              <input type="checkbox" v-model="elfBubble" />
              <span>气泡对话</span>
            </label>
            <label class="toggle-row">
              <input type="checkbox" v-model="elfIdle" />
              <span>空闲动画</span>
            </label>
          </div>

          <button class="apply-elf-btn" @click="applyElfAppearance">保存并应用到桌面</button>
        </div>

        <!-- 预设模板 -->
        <div class="elf-templates-section">
          <h4 class="section-title">预设模板</h4>
          <div class="template-list">
            <div
              v-for="tpl in elfTemplates"
              :key="tpl.id"
              class="template-card"
              @click="applyTemplate(tpl)"
            >
              <span class="template-icon">{{ tpl.icon }}</span>
              <div class="template-info">
                <span class="template-name">{{ tpl.name }}</span>
                <span class="template-desc">{{ tpl.desc }}</span>
              </div>
              <span class="template-apply">套用</span>
            </div>
          </div>
        </div>

        <div v-if="selectedWidget" class="properties-content">
          <div class="property-section">
            <h4 class="section-title">位置与大小</h4>
            <div class="property-row">
              <label>X</label>
              <input type="number" :value="selectedWidget.x" @input="updateWidgetProperty('x', $event.target.value)" />
            </div>
            <div class="property-row">
              <label>Y</label>
              <input type="number" :value="selectedWidget.y" @input="updateWidgetProperty('y', $event.target.value)" />
            </div>
            <div class="property-row">
              <label>宽度</label>
              <input type="number" :value="selectedWidget.width" @input="updateWidgetProperty('width', $event.target.value)" />
            </div>
            <div class="property-row">
              <label>高度</label>
              <input type="number" :value="selectedWidget.height" @input="updateWidgetProperty('height', $event.target.value)" />
            </div>
          </div>

          <div class="property-section">
            <h4 class="section-title">样式</h4>
            <div class="property-row">
              <label>透明度</label>
              <input type="range" min="10" max="100" :value="selectedWidget.transparency" @input="updateWidgetProperty('transparency', $event.target.value)" />
              <span>{{ selectedWidget.transparency }}%</span>
            </div>
            <div class="property-row">
              <label>背景色</label>
              <input type="color" :value="hexFromRgba(selectedWidget.style?.backgroundColor)" @input="updateWidgetStyle('backgroundColor', $event.target.value)" />
            </div>
            <div class="property-row">
              <label>边框色</label>
              <input type="color" :value="hexFromRgba(selectedWidget.style?.borderColor)" @input="updateWidgetStyle('borderColor', $event.target.value)" />
            </div>
            <div class="property-row">
              <label>文字色</label>
              <input type="color" :value="selectedWidget.style?.textColor || '#ffffff'" @input="updateWidgetStyle('textColor', $event.target.value)" />
            </div>
            <div class="property-row">
              <label>圆角</label>
              <input type="range" min="0" max="24" :value="parseInt(selectedWidget.style?.borderRadius) || 12" @input="updateWidgetStyle('borderRadius', $event.target.value + 'px')" />
              <span>{{ parseInt(selectedWidget.style?.borderRadius) || 12 }}px</span>
            </div>
          </div>

          <div class="property-section">
            <h4 class="section-title">组件类型</h4>
            <div class="type-badge">{{ getWidgetTypeName(selectedWidget.type) }}</div>
          </div>
        </div>

        <div v-else class="properties-empty">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M9 12l2 2 4-4"/>
            <circle cx="12" cy="12" r="10"/>
          </svg>
          <p>选择一个组件以编辑属性</p>
        </div>
      </aside>
    </div>

    <!-- Tab 2: 精灵养成（嵌入 AI 小精灵核心能力） -->
    <div v-if="activeTab === 'grow'" class="elf-grow-panel">
      <AiElfView />
    </div>

    <div v-if="showImportModal" class="modal-overlay" @click.self="showImportModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>导入配置</h3>
          <button class="modal-close" @click="showImportModal = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6L6 18M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <textarea 
            v-model="importJson"
            class="config-textarea"
            placeholder="粘贴配置JSON..."
          />
        </div>
        <div class="modal-footer">
          <button class="modal-btn" @click="showImportModal = false">取消</button>
          <button class="modal-btn primary" @click="doImportConfig">导入</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AiElfView from './AiElfView.vue'
import ClockWidget from '../widgets/ClockWidget.vue'
import WeatherWidget from '../widgets/WeatherWidget.vue'
import CalendarWidget from '../widgets/CalendarWidget.vue'
import NotesWidget from '../widgets/NotesWidget.vue'
import SystemMonitorWidget from '../widgets/SystemMonitorWidget.vue'
import AlarmWidget from '../widgets/AlarmWidget.vue'
import MusicPlayerWidget from '../widgets/MusicPlayerWidget.vue'
import { elfCharacters, elfSkins, elfOutfits } from '../data/elfData'
import AnimeElfSprite from '../components/elf/AnimeElfSprite.vue'
import { getElfConfig, saveElfConfig } from '../utils/elfBridge'
import { validateElfImageFile, readElfImageDataURL } from '../utils/elfImageUtils'
import { createLogger } from '../utils/logger'

const router = useRouter()

// 顶部 Tab：design=组件设计，grow=精灵养成
const activeTab = ref('design')

const logger = createLogger('DesktopElf')

const widgets = ref([])
const selectedWidget = ref(null)
const dragState = ref({
  widget: null,
  startX: 0,
  startY: 0,
  offsetX: 0,
  offsetY: 0
})
const showImportModal = ref(false)
const importJson = ref('')

// ─── 精灵形象设置（与桌面小精灵共享配置桥接） ───
const elfCharacterId = ref(null)
const elfSkinId = ref('default')
const elfOutfitId = ref('none')
const elfMode = ref('3d')
const elfSize = ref(1.0)
// 四档快捷预设（技能门禁：迷你/小/标准/大）
const elfSizePresets = [
  { label: '迷你', value: 0.6 },
  { label: '小', value: 0.8 },
  { label: '标准', value: 1.0 },
  { label: '大', value: 1.4 }
]

// 角色化 toast 提示（替代原生 alert，2.2s 自动消失）
const toastVisible = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
let toastTimer = null
function showToast(message, type = 'success') {
  toastMessage.value = message
  toastType.value = type
  toastVisible.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastVisible.value = false }, 2200)
}
const elfAutoRotate = ref(true)
const elfBubble = ref(true)
const elfIdle = ref(true)
const elfVisible = ref(true)
// 自定义形象：用户上传图片作为精灵立绘（优先于内置角色渲染）
const elfCustomImage = ref(null)

// 预设模板：一键套用替换当前 widgets
const elfTemplates = [
  {
    id: 'workbench',
    name: '工作台',
    desc: '时钟 + 笔记 + 系统监控',
    icon: '🖥️',
    widgetTypes: ['clock', 'notes', 'systemMonitor']
  },
  {
    id: 'entertainment',
    name: '娱乐',
    desc: '音乐 + 日历 + 天气',
    icon: '🎮',
    widgetTypes: ['music', 'calendar', 'weather']
  },
  {
    id: 'minimal',
    name: '极简',
    desc: '仅时钟',
    icon: '🍃',
    widgetTypes: ['clock']
  }
]

const availableWidgets = [
  { type: 'clock', name: '时钟', desc: '显示时间和日期', icon: '🕐', color: 'linear-gradient(135deg, #6366f1, #8b5cf6)' },
  { type: 'weather', name: '天气', desc: '实时天气信息', icon: '🌤️', color: 'linear-gradient(135deg, #06b6d4, #22d3ee)' },
  { type: 'calendar', name: '日历', desc: '月度日历视图', icon: '📅', color: 'linear-gradient(135deg, #f59e0b, #fbbf24)' },
  { type: 'notes', name: '笔记', desc: '快速备忘录', icon: '📝', color: 'linear-gradient(135deg, #10b981, #34d399)' },
  { type: 'systemMonitor', name: '系统监控', desc: 'CPU/内存状态', icon: '💻', color: 'linear-gradient(135deg, #ec4899, #f472b6)' },
  { type: 'alarm', name: '闹钟', desc: '闹钟提醒管理', icon: '⏰', color: 'linear-gradient(135deg, #ef4444, #f87171)' },
  { type: 'music', name: '音乐', desc: '音乐播放器', icon: '🎵', color: 'linear-gradient(135deg, #10b981, #34d399)' }
]

const widgetComponents = {
  clock: ClockWidget,
  weather: WeatherWidget,
  calendar: CalendarWidget,
  notes: NotesWidget,
  systemMonitor: SystemMonitorWidget,
  alarm: AlarmWidget,
  music: MusicPlayerWidget
}

function getWidgetComponent(type) {
  return widgetComponents[type] || ClockWidget
}

function getWidgetTypeName(type) {
  const found = availableWidgets.find(w => w.type === type)
  return found ? found.name : type
}

function hexFromRgba(rgba) {
  if (!rgba || rgba.startsWith('#')) return rgba
  const match = rgba.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/)
  if (match) {
    return '#' + [1, 2, 3].map(i => parseInt(match[i]).toString(16).padStart(2, '0')).join('')
  }
  return '#ffffff'
}

onMounted(() => {
  logger.info('INIT', 'DesktopElf component mounted')
  const saved = localStorage.getItem('desktop-elf-widgets')
  if (saved) {
    try {
      widgets.value = JSON.parse(saved)
      logger.info('CONFIG_LOAD', 'Successfully loaded widgets from localStorage', { count: widgets.value.length })
    } catch (e) {
      logger.error('CONFIG_LOAD', 'Failed to load widgets from localStorage', { error: e.message })
    }
  }
  // 加载共享精灵形象配置
  try {
    const cfg = getElfConfig()
    elfCharacterId.value = cfg.characterId || null
    elfSkinId.value = cfg.skinId || 'default'
    elfOutfitId.value = cfg.outfitId || 'none'
    elfMode.value = cfg.mode || '3d'
    elfSize.value = typeof cfg.size === 'number' ? cfg.size : 1.0
    elfAutoRotate.value = cfg.autoRotate !== false
    elfBubble.value = cfg.bubbleEnabled !== false
    elfIdle.value = cfg.idleAnimation !== false
    elfVisible.value = cfg.visible !== false
    elfCustomImage.value = cfg.customImage || null
  } catch (e) {
    logger.warn('ELF_CONFIG_LOAD', 'Failed to load elf appearance config', { error: e.message })
  }
})

function addWidget(type) {
  logger.info('WIDGET_ADD', `Adding widget of type: ${type}`)
  const newWidget = {
    id: Date.now().toString(),
    type: type,
    x: 100 + widgets.value.length * 60,
    y: 100 + widgets.value.length * 60,
    width: getDefaultWidth(type),
    height: getDefaultHeight(type),
    transparency: 85,
    style: getDefaultStyle(type),
    config: getDefaultConfig(type)
  }
  
  widgets.value.push(newWidget)
  selectedWidget.value = newWidget
  saveWidgets()
}

function getDefaultWidth(type) {
  const sizes = { clock: 200, weather: 220, calendar: 280, notes: 300, systemMonitor: 320, alarm: 280, music: 320 }
  return sizes[type] || 200
}

function getDefaultHeight(type) {
  const sizes = { clock: 180, weather: 160, calendar: 320, notes: 250, systemMonitor: 280, alarm: 300, music: 380 }
  return sizes[type] || 200
}

function getDefaultStyle(type) {
  return {
    backgroundColor: 'rgba(30, 41, 59, 0.85)',
    borderColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: '12px',
    textColor: '#ffffff',
    fontSize: '14px'
  }
}

function getDefaultConfig(type) {
  const configs = {
    clock: { format: '24h', showDate: true, timezone: 'local' },
    weather: { city: 'Beijing', unit: 'celsius', refreshInterval: 30 },
    calendar: { showWeekend: true, highlightToday: true, showLunar: false },
    notes: { content: '在这里输入笔记...', autoSave: true },
    systemMonitor: { showCpu: true, showMemory: true, showNetwork: false },
    alarm: { notifications: true, soundEnabled: true },
    music: { autoPlay: false, shuffle: false }
  }
  return configs[type] || {}
}

function selectWidget(widget) {
  selectedWidget.value = widget
}

function startDrag(widget, event) {
  selectedWidget.value = widget
  dragState.value = {
    widget: widget,
    startX: event.clientX,
    startY: event.clientY,
    offsetX: widget.x,
    offsetY: widget.y
  }
  
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

function onDrag(event) {
  if (!dragState.value.widget) return
  
  const deltaX = event.clientX - dragState.value.startX
  const deltaY = event.clientY - dragState.value.startY
  
  const widget = widgets.value.find(w => w.id === dragState.value.widget.id)
  if (widget) {
    const canvasElement = document.querySelector('.canvas-area')
    if (canvasElement) {
      const rect = canvasElement.getBoundingClientRect()
      const maxX = rect.width - widget.width - 8
      const maxY = rect.height - widget.height - 8
      widget.x = Math.max(0, Math.min(maxX, dragState.value.offsetX + deltaX))
      widget.y = Math.max(0, Math.min(maxY, dragState.value.offsetY + deltaY))
    } else {
      widget.x = Math.max(0, dragState.value.offsetX + deltaX)
      widget.y = Math.max(0, dragState.value.offsetY + deltaY)
    }
    saveWidgets()
  }
}

function stopDrag() {
  dragState.value = { widget: null, startX: 0, startY: 0, offsetX: 0, offsetY: 0 }
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

function handleDragStart(event, type) {
  event.dataTransfer.setData('widget-type', type)
}

function handleDrop(event) {
  const type = event.dataTransfer.getData('widget-type')
  if (type) {
    const rect = event.currentTarget.getBoundingClientRect()
    const x = event.clientX - rect.left - 100
    const y = event.clientY - rect.top - 100
    
    addWidget(type)
    
    const lastWidget = widgets.value[widgets.value.length - 1]
    lastWidget.x = Math.max(0, x)
    lastWidget.y = Math.max(0, y)
    saveWidgets()
  }
}

function updateWidgetProperty(key, value) {
  if (selectedWidget.value) {
    selectedWidget.value[key] = parseInt(value) || 0
    saveWidgets()
  }
}

function updateWidgetStyle(key, value) {
  if (selectedWidget.value) {
    if (!selectedWidget.value.style) {
      selectedWidget.value.style = {}
    }
    selectedWidget.value.style[key] = value
    saveWidgets()
  }
}

function removeWidget(id) {
  const index = widgets.value.findIndex(w => w.id === id)
  if (index !== -1) {
    widgets.value.splice(index, 1)
    if (selectedWidget.value?.id === id) {
      selectedWidget.value = null
    }
    saveWidgets()
  }
}

function saveWidgets() {
  localStorage.setItem('desktop-elf-widgets', JSON.stringify(widgets.value))
  // 同步到共享配置桥接，供桌面精灵"组件"tab 读取
  try {
    saveElfConfig({ widgets: widgets.value })
  } catch (e) {
    logger.warn('BRIDGE_SYNC', 'Failed to sync widgets to elfBridge', { error: e.message })
  }
}

function saveAllWidgets() {
  saveWidgets()
  showToast('小精灵帮你把组件都收好啦 ✨')
}

/**
 * 保存精灵形象设置并应用到桌面（写入共享配置桥接）
 */
function applyElfAppearance() {
  try {
    saveElfConfig({
      visible: elfVisible.value,
      characterId: elfCharacterId.value,
      skinId: elfSkinId.value,
      outfitId: elfOutfitId.value,
      mode: elfMode.value,
      size: elfSize.value,
      autoRotate: elfAutoRotate.value,
      bubbleEnabled: elfBubble.value,
      idleAnimation: elfIdle.value,
      customImage: elfCustomImage.value || null
    })
    // 同时同步当前 widgets
    saveElfConfig({ widgets: widgets.value })
    showToast('形象已焕新，快去桌面看看我吧 ✨')
  } catch (e) {
    showToast('保存失败了：' + e.message, 'error')
  }
}

/** 设计器上传自定义形象：校验 → 压缩为 dataURL → 暂存（点"保存并应用"后写入共享配置） */
async function onDesignerImageChange(e) {
  const file = e.target && e.target.files && e.target.files[0]
  try { e.target.value = '' } catch (err) {}
  const check = validateElfImageFile(file)
  if (!check.ok) {
    const msg = check.reason === 'not_image'
      ? '请选择图片文件哦～'
      : check.reason === 'too_large'
        ? '图片太大了，请选择 5MB 以内的图片～'
        : '请先选择一张图片～'
    showToast(msg, 'error')
    return
  }
  try {
    const dataUrl = await readElfImageDataURL(file)
    elfCustomImage.value = dataUrl
    showToast('形象预览已更新，记得点保存 ✨')
  } catch (err) {
    logger.warn('ELF_IMAGE_READ', 'Failed to read elf custom image', { error: err.message })
    showToast('图片读取失败，换一张试试吧', 'error')
  }
}

/**
 * 一键套用预设模板：替换当前 widgets 为模板配置
 */
function applyTemplate(template) {
  const newWidgets = template.widgetTypes.map((type, i) => ({
    id: 'tpl-' + Date.now() + '-' + i,
    type: type,
    x: 40 + i * 260,
    y: 60 + (i % 2) * 260,
    width: getDefaultWidth(type),
    height: getDefaultHeight(type),
    transparency: 85,
    style: getDefaultStyle(type),
    config: getDefaultConfig(type)
  }))
  widgets.value = newWidgets
  saveWidgets()
  logger.info('TEMPLATE_APPLIED', `Applied template: ${template.name}`, { count: newWidgets.length })
}

function exportConfig() {
  const config = JSON.stringify(widgets.value, null, 2)
  const blob = new Blob([config], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `desktop-elf-config-${Date.now()}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

function importConfig() {
  showImportModal.value = true
}

function doImportConfig() {
  try {
    const config = JSON.parse(importJson.value)
    if (Array.isArray(config)) {
      widgets.value = config
      saveWidgets()
      showImportModal.value = false
      importJson.value = ''
      showToast('配置导入成功，桌面已焕新 ✨')
    } else {
      showToast('这份配置不太对哦，再检查一下格式吧', 'error')
    }
  } catch (e) {
    showToast('配置解析失败了，请确认是有效的 JSON', 'error')
  }
}

function goBack() {
  router.push('/chat')
}
</script>

<style scoped>
.elf-designer {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  color: var(--text-primary);
}

.elf-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--card-border);
}

.header-left {
  display: flex;
  align-items: center;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  background: var(--gradient-text-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: var(--bg-tertiary);
  border: none;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.header-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.header-btn.primary {
  background: var(--gradient-primary);
  color: white;
}

.header-btn.primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* ─── 顶部 Tab 切换栏 ─── */
.elf-tabs-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--card-border);
}

.elf-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.elf-tab:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.elf-tab.active {
  background: var(--gradient-primary);
  color: white;
  font-weight: 500;
}

/* ─── 精灵养成面板（嵌入 AiElfView） ─── */
.elf-grow-panel {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-primary);
}

.elf-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.elf-sidebar {
  width: 260px;
  padding: 20px;
  background: var(--card-bg);
  border-right: 1px solid var(--card-border);
  overflow-y: auto;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 16px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.widget-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.widget-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.widget-item:hover {
  background: var(--bg-hover);
  transform: translateX(4px);
}

.widget-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.widget-info {
  flex: 1;
}

.widget-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.widget-desc {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.elf-canvas {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: auto;
}

.canvas-container {
  width: 100%;
  max-width: 1200px;
  height: 100%;
  max-height: calc(100vh - 120px);
}

.canvas-area {
  width: 100%;
  height: 100%;
  min-height: 600px;
  background: var(--bg-tertiary);
  border-radius: 16px;
  border: 2px dashed var(--border-primary);
  position: relative;
  overflow: hidden;
}

.canvas-area:hover {
  border-color: var(--color-primary);
}

.empty-state {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.empty-icon {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
}

.widget-wrapper {
  position: absolute;
  cursor: move;
  transition: box-shadow 0.2s;
}

.widget-wrapper.selected {
  outline: 2px solid var(--color-primary);
  outline-offset: 4px;
}

.widget-content {
  width: 100%;
  height: 100%;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.widget-controls {
  position: absolute;
  top: -8px;
  right: -8px;
  display: flex;
  gap: 4px;
}

.control-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.control-btn.delete {
  background: var(--color-error);
  color: white;
}

.control-btn.delete:hover {
  transform: scale(1.1);
}

.elf-properties {
  width: 280px;
  padding: 20px;
  background: var(--card-bg);
  border-left: 1px solid var(--card-border);
  overflow-y: auto;
}

.properties-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 16px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* ─── 精灵形象设置区 ─── */
.elf-appearance-section,
.elf-templates-section {
  padding: 14px;
  background: var(--bg-tertiary);
  border-radius: 10px;
  margin-bottom: 16px;
}

.property-block {
  margin-bottom: 14px;
}

.block-label {
  display: block;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 6px;
}

.mini-card {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  border-radius: 8px;
  background: var(--bg-primary);
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.15s ease;
}

.mini-card:hover {
  background: var(--bg-hover);
}

.mini-card.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.mini-emoji {
  font-size: 18px;
  line-height: 1;
}

.mini-anime-preview {
  width: 34px;
  height: 44px;
  display: block;
  overflow: visible;
}

.skin-dot {
  height: 36px;
}

.mode-row {
  display: flex;
  gap: 12px;
}

.mode-option {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-primary);
  cursor: pointer;
}

/* 自定义形象上传 */
.custom-image-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.custom-image-thumb {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.2);
}
.custom-image-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.custom-image-upload {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 14px;
  border: 1px solid var(--primary, #4f46e5);
  background: var(--primary, #4f46e5);
  color: #fff;
  border-radius: 18px;
  font-size: 13px;
  cursor: pointer;
  transition: opacity 0.2s ease;
}
.custom-image-upload:hover {
  opacity: 0.85;
}
.custom-image-upload input {
  display: none;
}
.custom-image-clear {
  padding: 8px 14px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: transparent;
  color: var(--text-primary);
  border-radius: 18px;
  font-size: 13px;
  cursor: pointer;
}
.custom-image-clear:hover {
  background: rgba(255, 255, 255, 0.08);
}
.block-hint {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-secondary, rgba(255, 255, 255, 0.55));
}

.full-range {
  width: 100%;
}

/* 尺寸四档快捷预设按钮 */
.size-presets {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  margin-top: 8px;
}
.size-preset-btn {
  padding: 6px 0;
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.12));
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-primary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.size-preset-btn:hover {
  background: rgba(255, 255, 255, 0.12);
}
.size-preset-btn.active {
  border-color: #4f46e5;
  background: rgba(79, 70, 229, 0.25);
  color: #fff;
}

.toggle-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-primary);
  cursor: pointer;
  margin-bottom: 6px;
}

.toggle-row:last-child {
  margin-bottom: 0;
}

.apply-elf-btn {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 8px;
  background: var(--gradient-primary);
  color: white;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.apply-elf-btn:hover {
  opacity: 0.9;
}

/* ─── 预设模板 ─── */
.template-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.template-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: var(--bg-primary);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.template-card:hover {
  background: var(--bg-hover);
  transform: translateX(3px);
}

.template-icon {
  font-size: 20px;
}

.template-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.template-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.template-desc {
  font-size: 11px;
  color: var(--text-muted);
}

.template-apply {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 12px;
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.properties-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.property-section {
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: 10px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.property-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.property-row:last-child {
  margin-bottom: 0;
}

.property-row label {
  width: 60px;
  font-size: 13px;
  color: var(--text-secondary);
}

.property-row input[type="number"] {
  flex: 1;
  padding: 6px 10px;
  background: var(--bg-primary);
  border: 1px solid var(--border-primary);
  border-radius: 6px;
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
}

.property-row input[type="number"]:focus {
  border-color: var(--color-primary);
}

.property-row input[type="range"] {
  flex: 1;
}

.property-row input[type="color"] {
  width: 40px;
  height: 32px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.property-row span {
  font-size: 12px;
  color: var(--text-muted);
  min-width: 40px;
  text-align: right;
}

.type-badge {
  display: inline-block;
  padding: 6px 12px;
  background: var(--color-primary-light);
  color: var(--color-primary);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.properties-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--text-muted);
}

.properties-empty svg {
  margin-bottom: 12px;
  opacity: 0.5;
}

.properties-empty p {
  font-size: 14px;
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
  width: 500px;
  background: var(--card-bg);
  border-radius: 16px;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--card-border);
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--bg-tertiary);
  border-radius: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-close:hover {
  background: var(--bg-hover);
}

.modal-body {
  padding: 24px;
}

.config-textarea {
  width: 100%;
  height: 200px;
  padding: 12px;
  background: var(--bg-primary);
  border: 1px solid var(--card-border);
  border-radius: 10px;
  color: var(--text-primary);
  font-family: monospace;
  font-size: 13px;
  resize: none;
  outline: none;
}

.config-textarea:focus {
  border-color: var(--color-primary);
}

.config-textarea::placeholder {
  color: var(--text-muted);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--card-border);
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
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.modal-btn:not(.primary):hover {
  background: var(--bg-hover);
}

.modal-btn.primary {
  background: var(--gradient-primary);
  color: white;
}

.modal-btn.primary:hover {
  opacity: 0.9;
}

@media (max-width: 1024px) {
  .elf-sidebar {
    width: 200px;
  }
  
  .elf-properties {
    width: 220px;
  }
}

@media (max-width: 768px) {
  .elf-body {
    flex-direction: column;
  }
  
  .elf-sidebar,
  .elf-properties {
    width: 100%;
    border: none;
    border-bottom: 1px solid var(--card-border);
    padding: 16px;
  }
  
  .widget-list {
    flex-direction: row;
    flex-wrap: wrap;
  }
  
  .widget-item {
    width: calc(50% - 5px);
  }
}

/* 角色化 toast 提示（替代原生 alert） */
.elf-toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 24px;
  background: rgba(30, 41, 59, 0.96);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
  font-size: 13px;
  color: #fff;
  white-space: nowrap;
  max-width: calc(100vw - 32px);
}
.elf-toast.success { border-color: rgba(79, 70, 229, 0.4); }
.elf-toast.error { border-color: rgba(239, 68, 68, 0.5); }
.toast-emoji { font-size: 15px; }
.toast-text { overflow: hidden; text-overflow: ellipsis; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translateX(-50%) translateY(-12px); }
</style>