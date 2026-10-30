<template>
  <canvas ref="canvasRef" class="particle-canvas"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref(null)
let animationId = null
let particles = []
let mouseParticles = []
let enabled = false
let canvas = null
let ctx = null
// 视口 CSS 像素尺寸（粒子出生/边界以此为准；canvas 后备存储按 dpr 放大）
let viewWidth = 0
let viewHeight = 0

// 桌面端默认 60 粒子 + 10 鼠标粒子；触摸设备（移动端）减半以省电
const isTouch = typeof window !== 'undefined' && (('ontouchstart' in window) || navigator.maxTouchPoints > 0)
const particleCount = isTouch ? 28 : 60
const mouseParticleCount = isTouch ? 0 : 10
const colors = ['#4f46e5', '#7c3aed', '#06b6d4', '#10b981', '#f59e0b', '#ec4899']

// P1-1：JS 粒子循环不受 CSS @media (prefers-reduced-motion) 管控，需单独用 matchMedia 监听
let motionQuery = null
let motionChangeHandler = null

// P1-4：mousemove 高频事件合帧（一次 rAF 只提交一批粒子）+ 鼠标粒子硬上限
let pendingMousePos = null
let mouseEmitScheduled = false
const MAX_MOUSE_PARTICLES = 200

// P2-3：resize 防抖
let resizeTimer = null
const RESIZE_DEBOUNCE_MS = 180

class Particle {
  constructor(canvas, isMouseParticle = false) {
    this.isMouseParticle = isMouseParticle
    if (isMouseParticle) {
      this.x = -100
      this.y = -100
      this.size = Math.random() * 2 + 1
      this.speedX = 0
      this.speedY = 0
      this.life = 0
      this.maxLife = 60
    } else {
      this.x = Math.random() * viewWidth
      this.y = Math.random() * viewHeight
      this.size = Math.random() * 3 + 1
      this.speedX = (Math.random() - 0.5) * 0.8
      this.speedY = (Math.random() - 0.5) * 0.8
    }
    this.color = colors[Math.floor(Math.random() * colors.length)]
    this.opacity = Math.random() * 0.5 + 0.2
    this.canvas = canvas
  }

  update() {
    if (this.isMouseParticle) {
      this.life++
      this.x += this.speedX
      this.y += this.speedY
      this.speedX *= 0.98
      this.speedY *= 0.98
      this.opacity = Math.max(0, 0.8 * (1 - this.life / this.maxLife))
    } else {
      this.x += this.speedX
      this.y += this.speedY
      // 轻微正弦漂移：让粒子轨迹更鲜活（不与边界判断冲突）
      this.twinklePhase = (this.twinklePhase || Math.random() * Math.PI * 2) + 0.04
      this.twinkle = 0.6 + 0.4 * Math.sin(this.twinklePhase)

      if (this.x < 0 || this.x > viewWidth) this.speedX *= -1
      if (this.y < 0 || this.y > viewHeight) this.speedY *= -1
    }
  }

  draw(ctx) {
    ctx.save()
    ctx.globalAlpha = this.isMouseParticle ? this.opacity : this.opacity * (this.twinkle || 1)
    ctx.fillStyle = this.color
    ctx.shadowBlur = this.isMouseParticle ? 12 : (this.twinkle ? 18 : 15)
    ctx.shadowColor = this.color
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.isMouseParticle ? this.size : this.size * (0.8 + 0.5 * (this.twinkle || 1)), 0, Math.PI * 2)
    ctx.fill()
    ctx.restore()
  }

  isDead() {
    return this.isMouseParticle && this.life >= this.maxLife
  }
}

const initParticles = () => {
  particles = []
  mouseParticles = []
  for (let i = 0; i < particleCount; i++) {
    particles.push(new Particle(canvas))
  }
}

const animate = () => {
  if (!enabled) return
  ctx.clearRect(0, 0, viewWidth, viewHeight)

  // 更新并绘制鼠标粒子
  mouseParticles = mouseParticles.filter(p => !p.isDead())
  mouseParticles.forEach(particle => {
    particle.update()
    particle.draw(ctx)
  })

  // 更新并绘制普通粒子
  particles.forEach(particle => {
    particle.update()
    particle.draw(ctx)
  })

  drawConnections()

  animationId = requestAnimationFrame(animate)
}

const drawConnections = () => {
  const maxDistance = 150
  const isDark = document.documentElement.getAttribute('data-theme') !== 'light'

  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < maxDistance) {
        ctx.save()
        ctx.globalAlpha = (isDark ? 0.16 : 0.12) * (1 - distance / maxDistance)
        ctx.strokeStyle = '#6366f1'
        ctx.lineWidth = 1
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.stroke()
        ctx.restore()
      }
    }
  }
}

const startLoop = () => {
  if (enabled || !canvas || !ctx) return
  enabled = true
  animate()
}

const stopLoop = () => {
  enabled = false
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
}

const handleMouseMove = (e) => {
  if (!canvas || mouseParticleCount === 0 || !enabled) return
  pendingMousePos = { x: e.clientX, y: e.clientY }
  if (mouseEmitScheduled) return
  mouseEmitScheduled = true
  // 合帧：每帧最多提交一批鼠标粒子，避免高频 mousemove 无界堆积
  requestAnimationFrame(() => {
    mouseEmitScheduled = false
    const pos = pendingMousePos
    pendingMousePos = null
    if (!pos || !enabled) return
    // 硬上限：超出时丢弃最老的粒子，防止快速划过鼠标时数组无限增长
    while (mouseParticles.length >= MAX_MOUSE_PARTICLES) mouseParticles.shift()
    for (let i = 0; i < mouseParticleCount; i++) {
      const particle = new Particle(canvas, true)
      particle.x = pos.x
      particle.y = pos.y
      particle.speedX = (Math.random() - 0.5) * 4
      particle.speedY = (Math.random() - 0.5) * 4
      particle.life = 0
      mouseParticles.push(particle)
    }
  })
}

// P2-1：按 devicePixelRatio 放大后备存储，避免高分屏模糊；绘制坐标仍用 CSS 像素
const resizeCanvas = () => {
  if (!canvas) return
  const dpr = window.devicePixelRatio || 1
  viewWidth = window.innerWidth
  viewHeight = window.innerHeight
  canvas.width = Math.floor(viewWidth * dpr)
  canvas.height = Math.floor(viewHeight * dpr)
  canvas.style.width = viewWidth + 'px'
  canvas.style.height = viewHeight + 'px'
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  initParticles()
}

// P2-3：resize 事件高频触发，防抖后再重建画布与粒子
const debouncedResize = () => {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    resizeTimer = null
    resizeCanvas()
  }, RESIZE_DEBOUNCE_MS)
}

onMounted(() => {
  canvas = canvasRef.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  resizeCanvas()

  // P1-1：尊重系统“减少动态效果”偏好。CSS 媒体查询管不到 requestAnimationFrame，
  // 这里在 JS 层判定：命中则不启动动画循环；并监听变化动态启停。
  if (window.matchMedia) {
    motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    motionChangeHandler = (e) => {
      if (e.matches) stopLoop()
      else startLoop()
    }
    motionQuery.addEventListener('change', motionChangeHandler)
    if (motionQuery.matches) {
      enabled = false
    } else {
      startLoop()
    }
  } else {
    startLoop()
  }

  window.addEventListener('resize', debouncedResize)
  if (mouseParticleCount > 0) {
    document.addEventListener('mousemove', handleMouseMove)
  }
})

onUnmounted(() => {
  stopLoop()
  if (resizeTimer) {
    clearTimeout(resizeTimer)
    resizeTimer = null
  }
  window.removeEventListener('resize', debouncedResize)
  if (mouseParticleCount > 0) {
    document.removeEventListener('mousemove', handleMouseMove)
  }
  if (motionQuery && motionChangeHandler) {
    motionQuery.removeEventListener('change', motionChangeHandler)
  }
  mouseEmitScheduled = false
  pendingMousePos = null
})
</script>

<style scoped>
.particle-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
  opacity: 0.55;
}
@media (max-width: 768px) {
  .particle-canvas {
    opacity: 0.35;
  }
}
</style>
