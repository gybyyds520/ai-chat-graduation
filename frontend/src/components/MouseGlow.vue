<template>
  <div v-if="visible" class="mouse-glow" :style="{ left: x + 'px', top: y + 'px', opacity }"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

// 仅桌面端（有精确指针）启用：移动端无鼠标，光晕无意义且耗性能
const supportsFinePointer = typeof window !== 'undefined' &&
  window.matchMedia && window.matchMedia('(pointer: fine)').matches

const visible = ref(false)
const x = ref(-200)
const y = ref(-200)
const opacity = ref(0)
let rafId = null
let targetX = -200
let targetY = -200
let curX = -200
let curY = -200
let lastMoveAt = 0

function handleMouseMove(e) {
  targetX = e.clientX
  targetY = e.clientY
  lastMoveAt = Date.now()
  visible.value = true
}

function tick() {
  // 平滑跟随
  curX += (targetX - curX) * 0.18
  curY += (targetY - curY) * 0.18
  x.value = curX
  y.value = curY

  // 静止超过 1.2s 后淡出
  opacity.value = Date.now() - lastMoveAt < 1200 ? 0.5 : Math.max(0, opacity.value - 0.02)
  rafId = requestAnimationFrame(tick)
}

function handleMouseLeave() {
  visible.value = false
}

onMounted(() => {
  if (!supportsFinePointer) return
  window.addEventListener('mousemove', handleMouseMove, { passive: true })
  document.documentElement.addEventListener('mouseleave', handleMouseLeave)
  rafId = requestAnimationFrame(tick)
})

onUnmounted(() => {
  if (rafId) cancelAnimationFrame(rafId)
  window.removeEventListener('mousemove', handleMouseMove)
  document.documentElement.removeEventListener('mouseleave', handleMouseLeave)
})
</script>

<style scoped>
.mouse-glow {
  position: fixed;
  width: 340px;
  height: 340px;
  margin-left: -170px;
  margin-top: -170px;
  border-radius: 50%;
  pointer-events: none;
  z-index: 9998;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.10) 0%, rgba(139, 92, 246, 0.06) 35%, transparent 70%);
  transition: opacity 0.3s ease;
  will-change: left, top;
}
</style>
