<template>
  <!-- 全局用户端背景层：default（主题默认）/ mountain（青绿山水）/ video（入场视频山云同款）
       z-index: -1 置于页面内容之下、body 背景之上；页面根容器已透明化，背景自然露出 -->
  <div
    class="user-bg-layer"
    :class="[`bg-${bgMode}`, { 'bg-dark': isDark, 'bg-light': !isDark }]"
  ></div>

  <!-- 快捷工具（右下角悬浮）：随时更换背景 + 重播主页入场动画（独立顶层，不被背景层影响） -->
  <div class="bg-quick-tools" v-if="enabled">
    <div class="bg-tools-panel" v-if="panelOpen">
      <div class="bg-panel-title">选择背景</div>
      <button
        v-for="opt in bgOptions"
        :key="opt.id"
        class="bg-option"
        :class="{ active: bgMode === opt.id }"
        @click="selectBg(opt.id)"
      >
        <span class="bg-option-dot" :style="{ background: opt.dot }"></span>
        <span>{{ opt.label }}</span>
      </button>
      <div class="bg-panel-divider"></div>
      <button class="bg-option replay" @click="replayEntrance">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polygon points="5 3 19 12 5 21 5 3" />
        </svg>
        <span>重播开场动画</span>
      </button>
    </div>
    <button
      class="bg-tool-btn"
      :class="{ active: panelOpen }"
      @click="panelOpen = !panelOpen"
      :title="'更换背景（当前：' + currentBgLabel + '）'"
    >
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="3" y="3" width="18" height="18" rx="2" />
        <circle cx="8.5" cy="8.5" r="1.5" />
        <path d="M21 15l-5-5L5 21" />
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useTheme } from '@/composables/useTheme'

const BG_KEY = 'app-bg-mode'
const route = useRoute()
const { currentTheme } = useTheme()

const isDark = computed(() => currentTheme.value === 'dark')

const bgOptions = [
  { id: 'default', label: '主题默认背景', dot: 'linear-gradient(135deg, #64748b, #94a3b8)' },
  { id: 'mountain', label: '青绿山水 · 山云', dot: 'linear-gradient(135deg, #2d6a4f, #74c69d)' },
  { id: 'video', label: '视频山云 · 云霭蝴蝶', dot: 'linear-gradient(135deg, #6b7f8f, #cfe0e8)' }
]

const bgMode = ref('default')
try {
  const stored = localStorage.getItem(BG_KEY)
  if (stored && bgOptions.some(o => o.id === stored)) bgMode.value = stored
} catch (e) { /* ignore */ }

const panelOpen = ref(false)

const currentBgLabel = computed(() => {
  const opt = bgOptions.find(o => o.id === bgMode.value)
  return opt ? opt.label : '主题默认背景'
})

function selectBg(id) {
  bgMode.value = id
  try { localStorage.setItem(BG_KEY, id) } catch (e) { /* ignore */ }
  panelOpen.value = false
}

function replayEntrance() {
  panelOpen.value = false
  // 通知当前页面（如主页 ChatView）重播入场动画；无监听页面时静默
  window.dispatchEvent(new CustomEvent('replay-entrance'))
}

// 仅在登录后的用户端页面显示快捷工具；公开页（落地/登录/注册/忘记密码/协议）不显示
const PUBLIC_ROUTES = ['landing', 'login', 'register', 'forgotPassword', 'agreement']
const enabled = computed(() => !PUBLIC_ROUTES.includes(route.name))
</script>

<style scoped>
/* 背景层：置于页面内容之下（z-index: -1）、body 背景之上；默认透明（露出页面主题背景） */
.user-bg-layer {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  opacity: 0;
  background-size: cover;
  background-position: center;
  transition: opacity 0.6s ease, filter 0.6s ease;
}

.user-bg-layer.bg-mountain {
  background-image: url('/backgrounds/mountain-cloud.jpg');
}

.user-bg-layer.bg-video {
  background-image: url('/backgrounds/video-scene.jpg');
}

.user-bg-layer.bg-mountain,
.user-bg-layer.bg-video {
  opacity: 0.82;
}

/* 昼夜联动：黑夜压暗降饱和（一键白天黑夜按钮切换后色调同步变化），白天明亮 */
.user-bg-layer.bg-dark {
  filter: brightness(0.5) saturate(0.78) contrast(1.06);
}

.user-bg-layer:not(.bg-dark).bg-mountain,
.user-bg-layer:not(.bg-dark).bg-video {
  filter: brightness(1.0) saturate(1.05);
}

/* 快捷工具（右下角悬浮） */
.bg-quick-tools {
  position: fixed;
  right: 22px;
  bottom: 88px;
  z-index: 8000;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  pointer-events: auto;
}

.bg-tool-btn {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  border: 1px solid var(--border-primary);
  background: var(--card-bg);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-md);
  transition: all 0.25s;
}

.bg-tool-btn:hover,
.bg-tool-btn.active {
  color: var(--color-primary);
  border-color: var(--color-primary);
  transform: translateY(-2px);
}

.bg-tools-panel {
  min-width: 190px;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid var(--border-primary);
  background: var(--card-bg);
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bg-panel-title {
  font-size: 12px;
  color: var(--text-tertiary);
  letter-spacing: 1px;
  padding: 2px 4px 6px;
}

.bg-option {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  text-align: left;
  transition: background 0.2s;
}

.bg-option:hover {
  background: var(--bg-hover);
}

.bg-option.active {
  background: var(--bg-tertiary);
  color: var(--color-primary);
  font-weight: 600;
}

.bg-option-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.35);
}

.bg-option.replay {
  color: var(--color-primary);
}

.bg-panel-divider {
  height: 1px;
  background: var(--border-primary);
  margin: 6px 4px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .bg-quick-tools {
    right: 14px;
    bottom: 84px;
  }
  .bg-tool-btn {
    width: 40px;
    height: 40px;
  }
}
</style>
