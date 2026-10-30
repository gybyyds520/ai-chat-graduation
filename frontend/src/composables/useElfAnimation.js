import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getIdleRandomAction } from '@/utils/animeElfUtils'

export function useElfAnimation() {
  const currentState = ref('idle')
  const previousState = ref('idle')
  const animationProgress = ref(0)
  const isTransitioning = ref(false)
  const breathPhase = ref(0)
  const blinkPhase = ref(0)
  const swayPhase = ref(0)
  const emotionPhase = ref(0)
  // 随机待机动作（yawn/stretch/look-around/blink/sway），供立绘做瞬时表情
  const idleAction = ref('')

  let animationFrameId = null
  let lastTime = 0
  let transitionStartTime = 0
  let transitionDuration = 0
  let targetState = null
  let idleActionTimer = null
  let idleActionClearTimer = null
  
  const statePriorities = {
    sleep: 10,
    sad: 8,
    surprised: 7,
    thinking: 6,
    happy: 5,
    jump: 4,
    dance: 4,
    wave: 3,
    walk: 2,
    run: 2,
    crouch: 2,
    lie: 2,
    sit: 2,
    feed: 2,
    pat: 2,
    drag_follow: 2,
    idle: 1,
    stand: 0
  }
  
  const stateAnimations = {
    stand: {
      breath: { amplitude: 0.02, frequency: 0.01 },
      blink: { interval: 3000, duration: 200 },
      sway: { amplitude: 0.02, frequency: 0.005 }
    },
    idle: {
      breath: { amplitude: 0.03, frequency: 0.015 },
      blink: { interval: 2500, duration: 150 },
      sway: { amplitude: 0.03, frequency: 0.008 },
      float: { amplitude: 0.05, frequency: 0.005 }
    },
    sleep: {
      breath: { amplitude: 0.05, frequency: 0.005 },
      blink: { interval: 0, duration: 0 },
      sway: { amplitude: 0.01, frequency: 0.002 },
      eyesClosed: true
    },
    happy: {
      breath: { amplitude: 0.04, frequency: 0.02 },
      blink: { interval: 1500, duration: 100 },
      sway: { amplitude: 0.04, frequency: 0.01 }
    },
    sad: {
      breath: { amplitude: 0.01, frequency: 0.008 },
      blink: { interval: 4000, duration: 300 },
      sway: { amplitude: 0.01, frequency: 0.003 }
    },
    thinking: {
      breath: { amplitude: 0.02, frequency: 0.01 },
      blink: { interval: 4000, duration: 200 },
      sway: { amplitude: 0.02, frequency: 0.005 },
      headTilt: { amplitude: 0.05, frequency: 0.01 }
    },
    surprised: {
      breath: { amplitude: 0.03, frequency: 0.02 },
      blink: { interval: 0, duration: 0 },
      sway: { amplitude: 0.01, frequency: 0.002 }
    },
    jump: {
      breath: { amplitude: 0.0, frequency: 0 },
      blink: { interval: 0, duration: 0 },
      sway: { amplitude: 0.0, frequency: 0 }
    },
    dance: {
      breath: { amplitude: 0.06, frequency: 0.03 },
      blink: { interval: 1000, duration: 100 },
      sway: { amplitude: 0.06, frequency: 0.02 }
    },
    wave: {
      breath: { amplitude: 0.02, frequency: 0.01 },
      blink: { interval: 0, duration: 0 },
      sway: { amplitude: 0.01, frequency: 0.003 }
    },
    walk: {
      breath: { amplitude: 0.02, frequency: 0.02 },
      blink: { interval: 3000, duration: 150 },
      sway: { amplitude: 0.03, frequency: 0.02 }
    },
    run: {
      breath: { amplitude: 0.03, frequency: 0.03 },
      blink: { interval: 2000, duration: 120 },
      sway: { amplitude: 0.05, frequency: 0.03 }
    },
    crouch: {
      breath: { amplitude: 0.02, frequency: 0.01 },
      blink: { interval: 3000, duration: 200 },
      sway: { amplitude: 0.01, frequency: 0.003 }
    },
    lie: {
      breath: { amplitude: 0.03, frequency: 0.008 },
      blink: { interval: 0, duration: 0 },
      sway: { amplitude: 0.0, frequency: 0 }
    },
    sit: {
      breath: { amplitude: 0.02, frequency: 0.01 },
      blink: { interval: 3000, duration: 200 },
      sway: { amplitude: 0.01, frequency: 0.003 }
    },
    feed: {
      breath: { amplitude: 0.02, frequency: 0.015 },
      blink: { interval: 3000, duration: 150 },
      sway: { amplitude: 0.01, frequency: 0.005 }
    },
    pat: {
      breath: { amplitude: 0.03, frequency: 0.02 },
      blink: { interval: 0, duration: 0 },
      sway: { amplitude: 0.02, frequency: 0.02 },
      eyesClosed: true
    },
    drag_follow: {
      breath: { amplitude: 0.02, frequency: 0.02 },
      blink: { interval: 2000, duration: 120 },
      sway: { amplitude: 0.03, frequency: 0.02 }
    }
  }
  
  const currentAnimation = computed(() => {
    return stateAnimations[currentState.value] || stateAnimations.stand
  })
  
  const transformStyle = computed(() => {
    const anim = currentAnimation.value
    const progress = animationProgress.value
    
    let translateY = 0
    let scaleY = 1
    let scaleX = 1
    let rotateZ = 0
    let rotateX = 0
    let rotateY = 0
    
    if (anim.breath) {
      const breathOffset = Math.sin(breathPhase.value) * anim.breath.amplitude
      scaleY = 1 + breathOffset
      translateY = -breathOffset * 10
    }
    
    if (anim.float) {
      translateY += Math.sin(swayPhase.value) * anim.float.amplitude * 20
    }
    
    if (anim.sway) {
      rotateZ = Math.sin(swayPhase.value) * anim.sway.amplitude * 10
    }
    
    if (anim.headTilt) {
      rotateX = Math.sin(swayPhase.value) * anim.headTilt.amplitude * 15
      rotateY = Math.cos(swayPhase.value) * anim.headTilt.amplitude * 15
    }
    
    if (currentState.value === 'jump') {
      const jumpProgress = progress
      const jumpHeight = 42
      translateY = -jumpHeight * Math.sin(jumpProgress * Math.PI)
      // 起跳前压缩、空中拉长、落地回弹
      if (jumpProgress < 0.15) {
        scaleY = 1 - (0.15 - jumpProgress) * 0.9 // 预备下蹲
      } else if (jumpProgress < 0.5) {
        scaleY = 1 + (jumpProgress - 0.15) * 0.25 // 空中拉长
      } else {
        scaleY = 1 - (jumpProgress - 0.5) * 0.1 // 落地微压
      }
    }

    if (currentState.value === 'walk' || currentState.value === 'run') {
      const speed = currentState.value === 'run' ? 2.2 : 1.2
      translateY += -Math.abs(Math.sin(swayPhase.value * speed)) * 6
      rotateZ = Math.sin(swayPhase.value * speed) * 2.5
    }
    
    if (currentState.value === 'dance') {
      translateY = -20 * Math.sin(swayPhase.value * 2)
      rotateZ = 15 * Math.sin(swayPhase.value * 3)
      scaleX = 1 + 0.1 * Math.sin(swayPhase.value * 4)
    }
    
    if (currentState.value === 'wave') {
      rotateZ = 20 * Math.sin(swayPhase.value * 6)
    }
    
    if (currentState.value === 'crouch') {
      translateY = 15
      scaleY = 0.85
      scaleX = 1.1
    }
    
    if (currentState.value === 'lie') {
      translateY = 20
      rotateX = 90
      scaleY = 0.6
      scaleX = 1.2
    }

    if (currentState.value === 'sit') {
      // 坐下：身体下沉 + 腿部弯曲（压扁）
      translateY = 18
      scaleY = 0.78
      scaleX = 1.08
    }

    if (currentState.value === 'feed') {
      // 进食：身体前倾 + 咀嚼微颤（下颌起伏）
      rotateX = -6
      translateY = Math.sin(swayPhase.value * 6) * 2
      scaleY = 0.95
    }

    if (currentState.value === 'pat') {
      // 摸摸头：闭眼微笑 + 身体微颤
      translateY = Math.sin(swayPhase.value * 8) * 1.5
      scaleY = 1 + Math.sin(swayPhase.value * 8) * 0.015
    }

    if (currentState.value === 'drag_follow') {
      // 拖拽跟随：身体前倾 + 手臂张开保持平衡（左右微晃）
      rotateX = 8
      rotateZ = Math.sin(swayPhase.value * 4) * 4
      scaleY = 0.95
    }

    if (isTransitioning.value) {
      const t = progress
      const easeT = t * t * (3 - 2 * t)
      translateY = easeT * translateY
      scaleX = 1 + (scaleX - 1) * easeT
      scaleY = 1 + (scaleY - 1) * easeT
      rotateZ = rotateZ * easeT
    }
    
    return {
      transform: `translateY(${translateY}px) scaleX(${scaleX}) scaleY(${scaleY}) rotateZ(${rotateZ}deg) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`,
      transformOrigin: 'center bottom'
    }
  })
  
  const eyesStyle = computed(() => {
    const anim = currentAnimation.value
    
    if (anim.eyesClosed || currentState.value === 'sleep') {
      return { transform: 'scaleY(0.1)' }
    }
    
    if (currentState.value === 'surprised') {
      return { transform: 'scale(1.3)', borderRadius: '40%' }
    }
    
    const blinkProgress = blinkPhase.value
    if (blinkProgress > 0 && blinkProgress < 1) {
      const blinkEase = blinkProgress < 0.5 
        ? blinkProgress * 2 
        : 1 - (blinkProgress - 0.5) * 2
      return { transform: `scaleY(${1 - blinkEase * 0.9})` }
    }
    
    return {}
  })
  
  const mouthStyle = computed(() => {
    switch (currentState.value) {
      case 'happy':
        return { borderRadius: '0 0 50% 50%', height: '14px' }
      case 'sad':
        return { borderRadius: '50% 50% 0 0', height: '8px' }
      case 'surprised':
        return { borderRadius: '50%', width: '24px', height: '24px' }
      case 'thinking':
        return { borderRadius: '0 0 40% 40%', height: '10px' }
      case 'sleep':
        return { borderRadius: '0', height: '2px', transform: 'rotate(-10deg)' }
      default:
        return {}
    }
  })
  
  function setState(newState, duration = 300) {
    if (newState === currentState.value) return
    
    const currentPriority = statePriorities[currentState.value] || 0
    const newPriority = statePriorities[newState] || 0
    
    if (newPriority < currentPriority && currentState.value !== 'stand') {
      return
    }
    
    if (isTransitioning.value && targetState) {
      previousState.value = targetState
    } else {
      previousState.value = currentState.value
    }
    
    targetState = newState
    isTransitioning.value = true
    transitionDuration = duration
    transitionStartTime = performance.now()
    animationProgress.value = 0
  }
  
  function completeTransition() {
    isTransitioning.value = false
    currentState.value = targetState
    targetState = null
    animationProgress.value = 1
  }
  
  function update(deltaTime) {
    breathPhase.value += deltaTime * 0.001
    swayPhase.value += deltaTime * 0.0008
    emotionPhase.value += deltaTime * 0.002
    
    const anim = currentAnimation.value
    
    if (anim.blink && anim.blink.interval > 0) {
      const blinkTime = Date.now() % anim.blink.interval
      if (blinkTime < anim.blink.duration) {
        blinkPhase.value = blinkTime / anim.blink.duration
      } else {
        blinkPhase.value = 0
      }
    }
    
    if (isTransitioning.value && targetState) {
      const elapsed = performance.now() - transitionStartTime
      animationProgress.value = Math.min(elapsed / transitionDuration, 1)
      
      if (animationProgress.value >= 1) {
        completeTransition()
      }
    }
  }
  
  function animate(timestamp) {
    if (!lastTime) lastTime = timestamp
    const deltaTime = timestamp - lastTime
    lastTime = timestamp
    
    update(deltaTime)
    
    animationFrameId = requestAnimationFrame(animate)
  }
  
  function start() {
    if (!animationFrameId) {
      lastTime = 0
      animationFrameId = requestAnimationFrame(animate)
    }
  }
  
  function stop() {
    if (animationFrameId) {
      cancelAnimationFrame(animationFrameId)
      animationFrameId = null
    }
  }
  
  function reset() {
    currentState.value = 'idle'
    previousState.value = 'idle'
    animationProgress.value = 0
    isTransitioning.value = false
    breathPhase.value = 0
    blinkPhase.value = 0
    swayPhase.value = 0
    emotionPhase.value = 0
    idleAction.value = ''
    targetState = null
  }
  
  function triggerAction(actionId, duration = 0) {
    const actionConfig = {
      jump: { state: 'jump', duration: 600 },
      dance: { state: 'dance', duration: 1000 },
      wave: { state: 'wave', duration: 600 },
      happy: { state: 'happy', duration: 800 },
      sad: { state: 'sad', duration: 800 },
      surprised: { state: 'surprised', duration: 500 },
      thinking: { state: 'thinking', duration: 1000 },
      walk: { state: 'walk', duration: 900 },
      run: { state: 'run', duration: 700 },
      sleep: { state: 'sleep', duration: 0 },
      idle: { state: 'idle', duration: 0 },
      stand: { state: 'stand', duration: 300 },
      crouch: { state: 'crouch', duration: 300 },
      lie: { state: 'lie', duration: 400 },
      sit: { state: 'sit', duration: 1500 },
      feed: { state: 'feed', duration: 1800 },
      pat: { state: 'pat', duration: 1200 },
      drag_follow: { state: 'drag_follow', duration: 0 }
    }
    
    const config = actionConfig[actionId]
    if (!config) return
    
    setState(config.state, 200)
    
    if (config.duration > 0) {
      setTimeout(() => {
        setState('stand', 300)
      }, config.duration)
    }
  }
  
  /**
   * 随机待机动作：每 10~30 秒触发一次（仅 idle 时生效），瞬时提示立绘做小动作
   */
  function scheduleIdleAction() {
    clearTimeout(idleActionTimer)
    const delay = 10000 + Math.random() * 20000 // 10~30s
    idleActionTimer = setTimeout(() => {
      // 仅在 idle/stand 低强度状态下做待机小动作，不打断互动
      if (currentState.value === 'idle' || currentState.value === 'stand') {
        const action = getIdleRandomAction()
        idleAction.value = action
        clearTimeout(idleActionClearTimer)
        idleActionClearTimer = setTimeout(() => { idleAction.value = '' }, 2000)
      }
      scheduleIdleAction()
    }, delay)
  }

  onMounted(() => {
    start()
    scheduleIdleAction()
  })

  onUnmounted(() => {
    stop()
    if (idleActionTimer) clearTimeout(idleActionTimer)
    if (idleActionClearTimer) clearTimeout(idleActionClearTimer)
  })

  return {
    currentState,
    previousState,
    isTransitioning,
    animationProgress,
    idleAction,
    transformStyle,
    eyesStyle,
    mouthStyle,
    setState,
    triggerAction,
    start,
    stop,
    reset
  }
}