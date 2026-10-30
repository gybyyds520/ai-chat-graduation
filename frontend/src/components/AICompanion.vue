<template>
  <div class="ai-companion">
    <!-- 3D精灵容器 -->
    <div ref="companionContainer" class="companion-3d"></div>
    
    <!-- 状态面板 -->
    <div class="companion-status">
      <div class="status-indicator" :class="mood"></div>
      <span class="status-text">{{ statusText }}</span>
    </div>

    <!-- 交互提示 -->
    <div v-if="showHint" class="interaction-hint">
      {{ currentHint }}
    </div>

    <!-- 表情气泡 -->
    <transition name="bubble">
      <div v-if="showEmotionBubble" class="emotion-bubble">
        {{ emotionIcon }}
      </div>
    </transition>

    <!-- 控制面板 -->
    <div class="companion-controls">
      <button @click="togglePet" :title="isActive ? '休息' : '唤醒'">
        {{ isActive ? '💤' : '🌟' }}
      </button>
      <button @click="playAnimation('wave')" title="挥手">👋</button>
      <button @click="playAnimation('dance')" title="跳舞">💃</button>
      <button @click="playAnimation('jump')" title="跳跃">🦘</button>
      <button @click="changeMood" title="换心情">🎭</button>
    </div>

    <!-- 摄像头预览（用于手势识别） -->
    <div v-if="showCamera" class="camera-preview">
      <video ref="videoRef" autoplay playsinline></video>
      <canvas ref="canvasRef"></canvas>
      <button @click="closeCamera" class="close-camera">✕</button>
    </div>

    <!-- 摄像头按钮 -->
    <button v-if="!showCamera && gestureEnabled" @click="openCamera" class="camera-btn" title="开启手势控制">
      🖐️
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import * as THREE from 'three'
import gestureRecognitionService from '@/utils/gestureRecognition.js'

const companionContainer = ref(null)
const videoRef = ref(null)
const canvasRef = ref(null)

// 3D场景相关
let scene, camera, renderer, companion
let animationId = null

// 状态
const isActive = ref(true)
const mood = ref('happy') // happy, sad, excited, sleepy, thinking
const showHint = ref(false)
const showEmotionBubble = ref(false)
const emotionIcon = ref('')
const currentHint = ref('')
const showCamera = ref(false)
const gestureEnabled = ref(true)

// 心情配置
const moodConfig = {
  happy: { emoji: '😊', color: 0x4ade80, scale: 1.0 },
  sad: { emoji: '😢', color: 0x60a5fa, scale: 0.95 },
  excited: { emoji: '🤩', color: 0xfbbf24, scale: 1.1 },
  sleepy: { emoji: '😴', color: 0xa78bfa, scale: 0.9 },
  thinking: { emoji: '🤔', color: 0xf472b6, scale: 1.0 }
}

// 计算属性
const statusText = computed(() => {
  const moods = {
    happy: '心情愉悦',
    sad: '有点失落',
    excited: '兴奋不已',
    sleepy: '昏昏欲睡',
    thinking: '正在思考'
  }
  return moods[mood.value]
})

// 初始化3D场景
const init3DScene = () => {
  const container = companionContainer.value
  
  // 创建场景
  scene = new THREE.Scene()
  
  // 创建相机
  camera = new THREE.PerspectiveCamera(
    50,
    container.clientWidth / container.clientHeight,
    0.1,
    1000
  )
  camera.position.set(0, 0, 5)

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({ 
    alpha: true, 
    antialias: true 
  })
  renderer.setSize(container.clientWidth, container.clientHeight)
  renderer.setPixelRatio(window.devicePixelRatio)
  container.appendChild(renderer.domElement)

  // 添加光源
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
  scene.add(ambientLight)

  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
  directionalLight.position.set(5, 5, 5)
  scene.add(directionalLight)

  // 创建AI精灵
  createCompanion()

  // 开始动画循环
  animate()

  // 响应式调整
  window.addEventListener('resize', onWindowResize)
}

// 创建AI精灵
const createCompanion = () => {
  // 创建精灵组
  companion = new THREE.Group()

  // 身体 - 圆润的球体
  const bodyGeometry = new THREE.SphereGeometry(0.8, 32, 32)
  const bodyMaterial = new THREE.MeshPhongMaterial({ 
    color: moodConfig[mood.value].color,
    shininess: 100
  })
  const body = new THREE.Mesh(bodyGeometry, bodyMaterial)
  companion.add(body)

  // 眼睛
  const eyeGeometry = new THREE.SphereGeometry(0.12, 16, 16)
  const eyeMaterial = new THREE.MeshPhongMaterial({ color: 0x1f2937 })
  
  const leftEye = new THREE.Mesh(eyeGeometry, eyeMaterial)
  leftEye.position.set(-0.25, 0.15, 0.65)
  companion.add(leftEye)

  const rightEye = new THREE.Mesh(eyeGeometry, eyeMaterial)
  rightEye.position.set(0.25, 0.15, 0.65)
  companion.add(rightEye)

  // 眼睛高光
  const highlightGeometry = new THREE.SphereGeometry(0.04, 8, 8)
  const highlightMaterial = new THREE.MeshBasicMaterial({ color: 0xffffff })
  
  const leftHighlight = new THREE.Mesh(highlightGeometry, highlightMaterial)
  leftHighlight.position.set(-0.22, 0.2, 0.75)
  companion.add(leftHighlight)

  const rightHighlight = new THREE.Mesh(highlightGeometry, highlightMaterial)
  rightHighlight.position.set(0.28, 0.2, 0.75)
  companion.add(rightHighlight)

  // 嘴巴
  const mouthGeometry = new THREE.TorusGeometry(0.15, 0.02, 8, 16, Math.PI)
  const mouthMaterial = new THREE.MeshPhongMaterial({ color: 0x1f2937 })
  const mouth = new THREE.Mesh(mouthGeometry, mouthMaterial)
  mouth.position.set(0, -0.2, 0.7)
  mouth.rotation.z = Math.PI
  companion.add(mouth)

  // 添加光晕效果
  const glowGeometry = new THREE.SphereGeometry(1.0, 32, 32)
  const glowMaterial = new THREE.MeshBasicMaterial({
    color: moodConfig[mood.value].color,
    transparent: true,
    opacity: 0.1
  })
  const glow = new THREE.Mesh(glowGeometry, glowMaterial)
  companion.add(glow)

  scene.add(companion)
}

// 动画循环
const animate = () => {
  animationId = requestAnimationFrame(animate)

  if (companion && isActive.value) {
    // 轻微上下浮动
    companion.position.y = Math.sin(Date.now() * 0.002) * 0.1
    
    // 轻微左右摇摆
    companion.rotation.y = Math.sin(Date.now() * 0.001) * 0.1
    
    // 根据心情调整缩放
    const targetScale = moodConfig[mood.value].scale
    companion.scale.setScalar(
      THREE.MathUtils.lerp(companion.scale.x, targetScale, 0.05)
    )
  }

  renderer.render(scene, camera)
}

// 窗口大小调整
const onWindowResize = () => {
  if (!companionContainer.value) return
  
  camera.aspect = companionContainer.value.clientWidth / companionContainer.value.clientHeight
  camera.updateProjectionMatrix()
  renderer.setSize(
    companionContainer.value.clientWidth,
    companionContainer.value.clientHeight
  )
}

// 播放动画
const playAnimation = (animationType) => {
  if (!companion) return

  const originalY = companion.position.y
  const originalRotation = companion.rotation.z

  switch (animationType) {
    case 'wave':
      // 挥手动画
      let waveCount = 0
      const waveInterval = setInterval(() => {
        if (waveCount >= 6) {
          companion.rotation.z = originalRotation
          clearInterval(waveInterval)
        } else {
          companion.rotation.z = Math.sin(Date.now() * 0.02) * 0.5
          waveCount++
        }
      }, 100)
      showEmotion('👋')
      break

    case 'dance':
      // 跳舞动画
      let danceCount = 0
      const danceInterval = setInterval(() => {
        if (danceCount >= 10) {
          companion.position.y = originalY
          clearInterval(danceInterval)
        } else {
          companion.position.y = originalY + Math.abs(Math.sin(Date.now() * 0.01)) * 0.5
          companion.rotation.y += 0.2
          danceCount++
        }
      }, 100)
      showEmotion('💃')
      break

    case 'jump':
      // 跳跃动画
      const jumpHeight = 1.5
      const jumpDuration = 600
      const startTime = Date.now()
      
      const jumpInterval = setInterval(() => {
        const elapsed = Date.now() - startTime
        if (elapsed >= jumpDuration) {
          companion.position.y = originalY
          clearInterval(jumpInterval)
        } else {
          // 抛物线运动
          const progress = elapsed / jumpDuration
          companion.position.y = originalY + jumpHeight * Math.sin(progress * Math.PI)
        }
      }, 16)
      showEmotion('🦘')
      break
  }
}

// 切换活跃状态
const togglePet = () => {
  isActive.value = !isActive.value
  if (companion) {
    companion.visible = isActive.value
  }
  showEmotion(isActive.value ? '🌟' : '💤')
}

// 改变心情
const changeMood = () => {
  const moods = Object.keys(moodConfig)
  const currentIndex = moods.indexOf(mood.value)
  const nextIndex = (currentIndex + 1) % moods.length
  mood.value = moods[nextIndex]
  showEmotion(moodConfig[mood.value].emoji)
}

// 显示表情气泡
const showEmotion = (emoji) => {
  emotionIcon.value = emoji
  showEmotionBubble.value = true
  setTimeout(() => {
    showEmotionBubble.value = false
  }, 2000)
}

// 打开摄像头
const openCamera = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ 
      video: { width: 320, height: 240 } 
    })
    
    if (videoRef.value) {
      videoRef.value.srcObject = stream
      await videoRef.value.play()
      
      showCamera.value = true
      
      // 初始化手势识别
      if (canvasRef.value) {
        canvasRef.value.width = 320
        canvasRef.value.height = 240
        
        await gestureRecognitionService.init(videoRef.value, canvasRef.value)
        
        // 注册手势回调
        gestureRecognitionService.onGesture('wave', () => {
          playAnimation('wave')
        })
        gestureRecognitionService.onGesture('fist', () => {
          showEmotion('✊')
        })
        gestureRecognitionService.onGesture('point', () => {
          showEmotion('👆')
        })
        
        gestureRecognitionService.start()
      }
    }
  } catch (error) {
    console.error('无法访问摄像头:', error)
    alert('无法访问摄像头，请检查权限设置')
  }
}

// 关闭摄像头
const closeCamera = () => {
  if (videoRef.value && videoRef.value.srcObject) {
    const tracks = videoRef.value.srcObject.getTracks()
    tracks.forEach(track => track.stop())
    videoRef.value.srcObject = null
  }
  
  gestureRecognitionService.stop()
  showCamera.value = false
}

onMounted(() => {
  init3DScene()
  
  // 显示欢迎提示
  setTimeout(() => {
    currentHint.value = '👋 你好！我是你的AI伙伴'
    showHint.value = true
    setTimeout(() => {
      showHint.value = false
    }, 3000)
  }, 1000)
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  
  if (renderer) {
    renderer.dispose()
  }
  
  closeCamera()
  window.removeEventListener('resize', onWindowResize)
})
</script>

<style scoped>
.ai-companion {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
}

.companion-3d {
  width: 200px;
  height: 200px;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  background: radial-gradient(circle at center, rgba(79, 70, 229, 0.1), transparent);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.companion-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.status-indicator.happy { background: #4ade80; }
.status-indicator.sad { background: #60a5fa; }
.status-indicator.excited { background: #fbbf24; }
.status-indicator.sleepy { background: #a78bfa; }
.status-indicator.thinking { background: #f472b6; }

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.1); }
}

.status-text {
  font-size: 12px;
  color: #374151;
  font-weight: 500;
}

.interaction-hint {
  position: absolute;
  bottom: 220px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 20px;
  background: rgba(79, 70, 229, 0.9);
  color: white;
  border-radius: 20px;
  font-size: 14px;
  white-space: nowrap;
  animation: fadeInOut 3s ease-in-out;
}

@keyframes fadeInOut {
  0%, 100% { opacity: 0; transform: translateX(-50%) translateY(10px); }
  20%, 80% { opacity: 1; transform: translateX(-50%) translateY(0); }
}

.emotion-bubble {
  position: absolute;
  top: -40px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 32px;
  animation: bounce 0.5s ease-in-out;
}

@keyframes bounce {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(-10px); }
}

.bubble-enter-active,
.bubble-leave-active {
  transition: all 0.3s ease;
}

.bubble-enter-from,
.bubble-leave-to {
  opacity: 0;
  transform: translateX(-50%) scale(0.5);
}

.companion-controls {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.companion-controls button {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.companion-controls button:hover {
  background: rgba(79, 70, 229, 0.1);
  transform: scale(1.1);
}

.camera-preview {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 320px;
  height: 240px;
  background: black;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.camera-preview video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera-preview canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.close-camera {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.camera-btn {
  position: fixed;
  bottom: 240px;
  right: 20px;
  width: 50px;
  height: 50px;
  border: none;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: white;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.4);
  transition: all 0.2s ease;
}

.camera-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.5);
}
</style>
