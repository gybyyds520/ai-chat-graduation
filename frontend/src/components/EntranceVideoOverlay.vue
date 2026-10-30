<template>
  <Teleport to="body">
    <div v-if="visible" class="entrance-overlay">
    <video
      ref="videoRef"
      class="entrance-video"
      :src="videoSrc"
      preload="auto"
      playsinline
      webkit-playsinline
      @ended="onEnded"
      @click="tryEnableSound"
    />
    <div v-if="soundBlocked" class="entrance-sound-tip" @click="tryEnableSound">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M11 5 6 9H2v6h4l5 4z" />
        <path d="M15.54 8.46a5 5 0 0 1 0 7.07" />
        <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
      </svg>
      <span>点击开启声音</span>
    </div>

    <div class="entrance-topbar">
      <span class="entrance-brand">AI Chat · 欢迎回来</span>
      <button class="entrance-skip" @click="skip">跳过 »</button>
    </div>

    <div class="entrance-footer">
      <button v-if="ended" class="entrance-enter" @click="finish">进入主页</button>
      <span v-else class="entrance-hint">入场动画 · {{ remaining }}s</span>
    </div>

    <div class="entrance-progress">
      <div class="entrance-progress-bar" :style="{ width: progress + '%' }"></div>
    </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'

const props = defineProps({
  videoSrc: { type: String, required: true },
  // 播放标记 key：同一 key 在本会话（sessionStorage）内只播放一次
  storageKey: { type: String, default: 'entrance_video_played' },
  // 自动关闭延迟（秒）：播放结束后停留多久再自动进入
  autoCloseDelay: { type: Number, default: 2.5 }
})

const emit = defineEmits(['finish'])

const visible = ref(false)
const ended = ref(false)
const soundBlocked = ref(false)
const videoRef = ref(null)
const remaining = ref(15)
const totalDuration = ref(15)
let timer = null
let finishTimer = null

const progress = computed(() => {
  if (totalDuration.value <= 0) return 0
  return Math.min(100, Math.max(0, ((totalDuration.value - remaining.value) / totalDuration.value) * 100))
})

function hasPlayed() {
  try {
    return sessionStorage.getItem(props.storageKey) === '1'
  } catch (e) {
    return false
  }
}

function markPlayed() {
  try {
    sessionStorage.setItem(props.storageKey, '1')
  } catch (e) { /* ignore */ }
}

function show() {
  visible.value = true
  ended.value = false
  remaining.value = totalDuration.value
  let retried = false
  const tryPlay = () => {
    const v = videoRef.value
    if (!v) return
    v.muted = false
    v.play().then(() => {
      soundBlocked.value = false
    }).catch(() => {
      // 浏览器阻止带声音自动播放：回退为静音播放 + 提示开启声音
      soundBlocked.value = true
      v.muted = true
      v.play().catch(() => { /* 仍失败则等用户点击 */ })
    })
  }
  // 等待 DOM 渲染完成后再播放（nextTick 保证 ref 已绑定）
  nextTick(() => {
    tryPlay()
    // 兜底重试：媒体尚未就绪等时序问题可能导致首次 play 静默失败
    setTimeout(() => {
      const v = videoRef.value
      if (v && v.paused && v.currentTime === 0 && !retried) {
        retried = true
        tryPlay()
      }
    }, 600)
  })
  startTimer()
}

function tryEnableSound() {
  const v = videoRef.value
  if (!v) return
  v.muted = false
  v.play().then(() => {
    soundBlocked.value = false
  }).catch(() => { /* ignore */ })
}

function startTimer() {
  stopTimer()
  timer = setInterval(() => {
    remaining.value = Math.max(0, remaining.value - 0.1)
    if (remaining.value <= 0) stopTimer()
  }, 100)
}

function stopTimer() {
  if (timer) { clearInterval(timer); timer = null }
}

function onEnded() {
  ended.value = true
  stopTimer()
  remaining.value = 0
  markPlayed()
  // 播放结束后短暂停留，然后自动进入主页
  finishTimer = setTimeout(() => finish(), props.autoCloseDelay * 1000)
}

function skip() {
  markPlayed()
  finish()
}

function finish() {
  if (finishTimer) { clearTimeout(finishTimer); finishTimer = null }
  stopTimer()
  visible.value = false
  emit('finish')
}

function init() {
  const v = videoRef.value
  if (v) {
    v.addEventListener('loadedmetadata', () => {
      if (v.duration && v.duration > 0) {
        totalDuration.value = v.duration
        remaining.value = v.duration
      }
    })
  }
}

onMounted(() => {
  init()
  // 仅当本会话未播放过时才展示
  if (!hasPlayed()) show()
})

onUnmounted(() => {
  stopTimer()
  if (finishTimer) { clearTimeout(finishTimer); finishTimer = null }
})

defineExpose({ show, skip, finish })
</script>

<style scoped>
.entrance-overlay {
  position: fixed;
  inset: 0;
  z-index: 200000;
  background: #05070f;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: default;
  user-select: none;
}

.entrance-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  /* 全程全身构图：cover 全屏填充无黑边，不放大裁切（人物全身在视频内完整可见） */
  transform: scale(1);
  background: transparent;
  outline: none;
}

/* 静音提示 */
.entrance-sound-tip {
  position: absolute;
  bottom: 18%;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 999px;
  background: rgba(20, 24, 40, 0.72);
  border: 1px solid rgba(126, 200, 160, 0.45);
  color: #b9f6d0;
  font-size: 13px;
  cursor: pointer;
  animation: soundPulse 2s ease-in-out infinite;
}

/* 顶栏 */
.entrance-topbar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background: linear-gradient(to bottom, rgba(5, 7, 15, 0.55), transparent);
  z-index: 2;
}

.entrance-brand {
  color: rgba(220, 235, 255, 0.85);
  font-size: 14px;
  letter-spacing: 2px;
  font-weight: 500;
  text-shadow: 0 0 12px rgba(90, 200, 160, 0.35);
}

.entrance-skip {
  border: 1px solid rgba(160, 220, 190, 0.4);
  background: rgba(16, 22, 36, 0.55);
  color: #d8f7e6;
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.25s;
}

.entrance-skip:hover {
  background: rgba(50, 160, 120, 0.35);
  border-color: rgba(160, 220, 190, 0.8);
}

/* 底部 */
.entrance-footer {
  position: absolute;
  bottom: 34px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2;
}

.entrance-enter {
  border: none;
  background: linear-gradient(135deg, #10b981, #0ea5a9);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  padding: 10px 36px;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(16, 185, 129, 0.4);
  animation: enterGlow 1.4s ease-in-out infinite;
  transition: transform 0.2s;
}

.entrance-enter:hover {
  transform: scale(1.05);
}

.entrance-hint {
  color: rgba(200, 220, 240, 0.65);
  font-size: 12px;
  letter-spacing: 1px;
}

/* 进度条 */
.entrance-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: rgba(255, 255, 255, 0.08);
  z-index: 3;
}

.entrance-progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #34d399, #22d3ee);
  box-shadow: 0 0 10px rgba(52, 211, 153, 0.7);
  transition: width 0.1s linear;
}

@keyframes soundPulse {
  0%, 100% { opacity: 0.75; }
  50% { opacity: 1; }
}

@keyframes enterGlow {
  0%, 100% { box-shadow: 0 8px 24px rgba(16, 185, 129, 0.4); }
  50% { box-shadow: 0 8px 34px rgba(16, 185, 129, 0.65); }
}

/* 移动端适配：竖屏时视频 contain 显示全宽 */
@media (max-width: 768px) {
  .entrance-brand { font-size: 12px; }
  .entrance-skip { font-size: 12px; padding: 5px 12px; }
  .entrance-enter { font-size: 14px; padding: 9px 30px; }
}
</style>
