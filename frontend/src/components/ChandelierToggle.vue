<template>
  <div 
    class="chandelier-toggle-container"
    :class="{ 'is-dragging': isDragging, 'is-movable': draggable }"
    :style="containerStyle"
    @mousedown="handleMouseDown"
    @touchstart="handleTouchStart"
  >
    <button 
      class="chandelier-toggle" 
      @click="handleClick" 
      :title="currentTheme === 'dark' ? '切换到白天模式' : '切换到夜晚模式'"
      :class="{ 'is-dark': currentTheme === 'dark', 'size-sm': size === 'sm' }"
    >
      <div class="chandelier-glow" :class="{ lit: currentTheme !== 'dark' }"></div>
      <div class="chandelier-chain"></div>
      <div class="chandelier-body">
        <div class="chandelier-crown"></div>
        <div class="chandelier-bowl" :class="{ off: currentTheme === 'dark' }">
          <div class="bowl-inner"></div>
          <div class="bowl-rim"></div>
        </div>
        <div class="chandelier-crystals">
          <div class="crystal" v-for="i in 6" :key="i" :style="{ '--delay': i * 0.1 + 's' }"></div>
        </div>
        <div class="bulb-container">
          <div class="bulb" :class="{ off: currentTheme === 'dark' }">
            <div class="bulb-glow"></div>
          </div>
        </div>
      </div>
      <div class="chandelier-shadow" :class="{ active: currentTheme !== 'dark' }"></div>
    </button>
    <div v-if="draggable && !isDragging" class="move-indicator">
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="5 9 2 12 5 15"/>
        <polyline points="19 9 22 12 19 15"/>
        <line x1="2" y1="12" x2="22" y2="12"/>
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useTheme } from '@/composables/useTheme'

const props = defineProps<{
  size?: 'sm' | 'md'
  draggable?: boolean
}>()

const { currentTheme, toggleTheme } = useTheme()

const isDragging = ref(false)
const position = ref({ x: 0, y: 0 })
const startPosition = ref({ x: 0, y: 0 })
const mouseStart = ref({ x: 0, y: 0 })
const hasMoved = ref(false)
const dragThreshold = 5
// 移动端触摸下 preventDefault 可能吞掉后续 click：tap 时手动触发并抑制重复 click
const suppressClick = ref(false)

const setDefaultPosition = () => {
  position.value = {
    x: window.innerWidth - 70,
    y: 16
  }
}

const POSITION_STORAGE_KEY = 'chandelier-toggle-position'

const containerStyle = computed(() => {
  if (!props.draggable) {
    return {}
  }
  return {
    left: `${position.value.x}px`,
    top: `${position.value.y}px`
  }
})

const loadPosition = () => {
  if (!props.draggable) return
  try {
    const stored = localStorage.getItem(POSITION_STORAGE_KEY)
    if (stored) {
      const pos = JSON.parse(stored)
      position.value = pos
    } else {
      setDefaultPosition()
    }
  } catch (e) {
    console.warn('Failed to load position:', e)
    setDefaultPosition()
  }
}

const savePosition = () => {
  if (!props.draggable) return
  try {
    localStorage.setItem(POSITION_STORAGE_KEY, JSON.stringify(position.value))
  } catch (e) {
    console.warn('Failed to save position:', e)
  }
}

const handleMouseDown = (e: MouseEvent) => {
  if (!props.draggable || e.button !== 0) return
  
  e.preventDefault()
  startPosition.value = { ...position.value }
  mouseStart.value = { x: e.clientX, y: e.clientY }
  hasMoved.value = false
  
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

const handleMouseMove = (e: MouseEvent) => {
  if (!props.draggable) return
  
  const dx = e.clientX - mouseStart.value.x
  const dy = e.clientY - mouseStart.value.y
  
  if (!hasMoved.value) {
    const distance = Math.sqrt(dx * dx + dy * dy)
    if (distance > dragThreshold) {
      hasMoved.value = true
      isDragging.value = true
    }
  }
  
  if (isDragging.value) {
    let newX = startPosition.value.x + dx
    let newY = startPosition.value.y + dy
    
    const maxX = window.innerWidth - 60
    const maxY = window.innerHeight - 80
    
    newX = Math.max(16, Math.min(newX, maxX))
    newY = Math.max(16, Math.min(newY, maxY))
    
    position.value = { x: newX, y: newY }
  }
}

const handleMouseUp = () => {
  if (!props.draggable) return
  
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  
  if (isDragging.value) {
    isDragging.value = false
    savePosition()
  }
}

const handleTouchStart = (e: TouchEvent) => {
  if (!props.draggable || e.touches.length !== 1) return
  
  e.preventDefault()
  const touch = e.touches[0]
  startPosition.value = { ...position.value }
  mouseStart.value = { x: touch.clientX, y: touch.clientY }
  hasMoved.value = false
  
  document.addEventListener('touchmove', handleTouchMove)
  document.addEventListener('touchend', handleTouchEnd)
}

const handleTouchMove = (e: TouchEvent) => {
  if (!props.draggable || e.touches.length !== 1) return
  
  const touch = e.touches[0]
  const dx = touch.clientX - mouseStart.value.x
  const dy = touch.clientY - mouseStart.value.y
  
  if (!hasMoved.value) {
    const distance = Math.sqrt(dx * dx + dy * dy)
    if (distance > dragThreshold) {
      hasMoved.value = true
      isDragging.value = true
    }
  }
  
  if (isDragging.value) {
    let newX = startPosition.value.x + dx
    let newY = startPosition.value.y + dy
    
    const maxX = window.innerWidth - 60
    const maxY = window.innerHeight - 80
    
    newX = Math.max(16, Math.min(newX, maxX))
    newY = Math.max(16, Math.min(newY, maxY))
    
    position.value = { x: newX, y: newY }
  }
}

const handleTouchEnd = () => {
  if (!props.draggable) return
  
  document.removeEventListener('touchmove', handleTouchMove)
  document.removeEventListener('touchend', handleTouchEnd)
  
  if (isDragging.value) {
    isDragging.value = false
    savePosition()
  } else if (!hasMoved.value) {
    // 纯 tap：手动触发主题切换（部分移动端浏览器 touchstart preventDefault 会吞掉 click）
    toggleTheme()
    suppressClick.value = true
    setTimeout(() => { suppressClick.value = false }, 350)
  }
}

const handleClick = () => {
  if (suppressClick.value) {
    suppressClick.value = false
    return
  }
  if (!hasMoved.value) {
    toggleTheme()
  }
}

const handleResize = () => {
  const maxX = window.innerWidth - 60
  const maxY = window.innerHeight - 80
  
  position.value.x = Math.min(position.value.x, maxX)
  position.value.y = Math.min(position.value.y, maxY)
}

onMounted(() => {
  if (props.draggable) {
    loadPosition()
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  document.removeEventListener('touchmove', handleTouchMove)
  document.removeEventListener('touchend', handleTouchEnd)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.chandelier-toggle-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  cursor: grab;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.chandelier-toggle-container.is-movable.is-dragging {
  cursor: grabbing;
  transform: scale(1.15);
  z-index: 99999;
  padding: 10px;
  background: var(--card-bg);
  border: 2px solid var(--color-primary);
  border-radius: 16px;
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.1);
}

.chandelier-toggle-container.is-movable {
  position: fixed;
  z-index: 9999;
  padding: 8px;
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-radius: 12px;
  box-shadow: var(--shadow-md);
}

.chandelier-toggle {
  position: relative;
  width: 40px;
  height: 52px;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  transition: transform 0.2s ease;
  outline: none;
}

.chandelier-toggle:hover {
  transform: scale(1.1);
}

.chandelier-toggle:active {
  transform: scale(0.95);
}

.chandelier-glow {
  position: absolute;
  top: -16px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 60px;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.4) 0%, rgba(251, 191, 36, 0.1) 40%, transparent 70%);
  opacity: 0;
  transition: opacity 0.5s ease;
  pointer-events: none;
  z-index: 1;
}

.chandelier-glow.lit {
  opacity: 1;
  animation: glow-pulse 2s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% {
    opacity: 1;
    transform: translateX(-50%) scale(1);
  }
  50% {
    opacity: 0.7;
    transform: translateX(-50%) scale(1.15);
  }
}

.chandelier-chain {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 8px;
  background: linear-gradient(180deg, #d4af37, #b8860b);
  border-radius: 2px 2px 0 0;
}

.chandelier-body {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.chandelier-crown {
  width: 16px;
  height: 8px;
  background: linear-gradient(180deg, #f0d78c, #d4af37);
  border-radius: 4px 4px 0 0;
  margin-top: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.chandelier-bowl {
  position: relative;
  width: 32px;
  height: 16px;
  background: linear-gradient(180deg, #fff8dc 0%, #ffd700 50%, #daa520 100%);
  border-radius: 20px 20px 12px 12px;
  margin-top: -2px;
  box-shadow: 
    0 4px 12px rgba(255, 215, 0, 0.5),
    inset 0 -4px 8px rgba(218, 165, 32, 0.4);
  transition: all 0.4s ease;
}

.chandelier-bowl.off {
  background: linear-gradient(180deg, #4a4a4a 0%, #2d2d2d 50%, #1a1a1a 100%);
  box-shadow: 
    0 2px 6px rgba(0, 0, 0, 0.3),
    inset 0 -2px 4px rgba(0, 0, 0, 0.3);
}

.bowl-inner {
  position: absolute;
  top: 2px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 6px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
}

.chandelier-bowl.off .bowl-inner {
  background: rgba(255, 255, 255, 0.05);
}

.bowl-rim {
  position: absolute;
  top: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 28px;
  height: 4px;
  background: linear-gradient(90deg, #b8860b, #ffd700, #b8860b);
  border-radius: 2px;
}

.chandelier-bowl.off .bowl-rim {
  background: linear-gradient(90deg, #333, #555, #333);
}

.chandelier-crystals {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 36px;
  display: flex;
  justify-content: space-between;
}

.crystal {
  width: 4px;
  height: 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.6) 50%, rgba(255, 215, 0, 0.4) 100%);
  clip-path: polygon(50% 0%, 100% 30%, 100% 100%, 0% 100%, 0% 30%);
  animation: crystal-swing 3s ease-in-out infinite;
  animation-delay: var(--delay);
}

.chandelier-toggle.is-dark .crystal {
  background: linear-gradient(180deg, rgba(100, 100, 100, 0.6) 0%, rgba(60, 60, 60, 0.4) 50%, rgba(40, 40, 40, 0.3) 100%);
}

@keyframes crystal-swing {
  0%, 100% {
    transform: rotate(-5deg);
  }
  50% {
    transform: rotate(5deg);
  }
}

.bulb-container {
  position: absolute;
  top: 28px;
  left: 50%;
  transform: translateX(-50%);
}

.bulb {
  position: relative;
  width: 16px;
  height: 20px;
  background: linear-gradient(135deg, #fff 0%, #ffffe0 50%, #fffacd 100%);
  border-radius: 50% 50% 45% 45%;
  box-shadow: 
    0 0 20px rgba(255, 255, 200, 0.8),
    0 0 40px rgba(255, 215, 0, 0.6),
    inset 0 -6px 10px rgba(255, 200, 0, 0.4);
  transition: all 0.4s ease;
}

.bulb.off {
  background: linear-gradient(135deg, #3a3a3a 0%, #2d2d2d 50%, #1f1f1f 100%);
  box-shadow: 
    0 0 4px rgba(100, 100, 100, 0.3),
    inset 0 -3px 6px rgba(0, 0, 0, 0.4);
}

.bulb-glow {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 24px;
  background: radial-gradient(circle, rgba(255, 255, 200, 0.6) 0%, rgba(255, 215, 0, 0.3) 40%, transparent 70%);
  opacity: 1;
  transition: opacity 0.4s ease;
}

.bulb.off .bulb-glow {
  opacity: 0;
}

.chandelier-shadow {
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 4px;
  background: radial-gradient(ellipse, rgba(0, 0, 0, 0.3) 0%, transparent 70%);
  opacity: 0;
  transition: opacity 0.4s ease;
}

.chandelier-shadow.active {
  opacity: 1;
}

.move-indicator {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  border-radius: 50%;
  color: white;
  opacity: 0.7;
  transition: opacity 0.2s ease;
  pointer-events: none;
}

.chandelier-toggle-container:hover .move-indicator {
  opacity: 1;
}

@media (max-width: 768px) {
  .chandelier-toggle {
    width: 32px;
    height: 42px;
  }
  
  .chandelier-glow {
    width: 48px;
    height: 48px;
    top: -12px;
  }
  
  .chandelier-chain {
    width: 3px;
    height: 6px;
  }
  
  .chandelier-crown {
    width: 12px;
    height: 6px;
  }
  
  .chandelier-bowl {
    width: 26px;
    height: 14px;
  }
  
  .bowl-inner {
    width: 16px;
    height: 5px;
  }
  
  .bowl-rim {
    width: 22px;
    height: 3px;
  }
  
  .chandelier-crystals {
    width: 28px;
    top: 16px;
  }
  
  .crystal {
    width: 3px;
    height: 10px;
  }
  
  .bulb-container {
    top: 22px;
  }
  
  .bulb {
    width: 12px;
    height: 16px;
  }
  
  .bulb-glow {
    width: 18px;
    height: 18px;
    top: -6px;
  }
  
  .move-indicator {
    width: 16px;
    height: 16px;
    top: -6px;
    right: -6px;
  }
  
  .move-indicator svg {
    width: 10px;
    height: 10px;
  }
}

.chandelier-toggle.size-sm {
  width: 28px;
  height: 36px;
}

.chandelier-toggle.size-sm .chandelier-glow {
  width: 40px;
  height: 40px;
  top: -10px;
}

.chandelier-toggle.size-sm .chandelier-chain {
  width: 3px;
  height: 6px;
}

.chandelier-toggle.size-sm .chandelier-crown {
  width: 10px;
  height: 5px;
}

.chandelier-toggle.size-sm .chandelier-bowl {
  width: 22px;
  height: 12px;
}

.chandelier-toggle.size-sm .bowl-inner {
  width: 14px;
  height: 4px;
}

.chandelier-toggle.size-sm .bowl-rim {
  width: 18px;
  height: 3px;
}

.chandelier-toggle.size-sm .chandelier-crystals {
  width: 24px;
  top: 14px;
}

.chandelier-toggle.size-sm .crystal {
  width: 3px;
  height: 8px;
}

.chandelier-toggle.size-sm .bulb-container {
  top: 18px;
}

.chandelier-toggle.size-sm .bulb {
  width: 10px;
  height: 14px;
}

.chandelier-toggle.size-sm .bulb-glow {
  width: 16px;
  height: 16px;
  top: -5px;
}

.chandelier-toggle.size-sm .chandelier-shadow {
  width: 22px;
  bottom: -5px;
}
</style>