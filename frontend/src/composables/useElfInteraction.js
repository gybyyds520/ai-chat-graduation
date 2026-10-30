import { ref, onMounted, onUnmounted } from 'vue'
import { elfInteractions } from '@/data/elfData'
import { getRandomInteraction } from '@/utils/animeElfUtils'

// 特效类型 → 悬浮 emoji
export const ELF_EFFECT_EMOJI = {
  heart: '❤️',
  star: '⭐',
  note: '🎵',
  zzz: '💤'
}

export function useElfInteraction() {
  const activeDialog = ref(null)
  const activeEmotion = ref(null)
  const lastInteractionTime = ref(0)
  const interactionHistory = ref([])
  const isInteracting = ref(false)
  
  const cooldownTimers = {}
  const triggerListeners = {}
  
  const interactionTypes = {
    time: { check: checkTimeTrigger },
    mouse: { check: checkMouseTrigger },
    system: { check: checkSystemTrigger },
    environment: { check: checkEnvironmentTrigger },
    custom: { check: checkCustomTrigger }
  }
  
  function checkTimeTrigger(interaction, now) {
    const triggers = interaction.triggers
    for (const trigger of triggers) {
      if (trigger.hour) {
        const [start, end] = trigger.hour
        const hour = now.getHours()
        if (start < end) {
          if (hour >= start && hour < end) return true
        } else {
          if (hour >= start || hour < end) return true
        }
      }
      
      if (trigger.dayOfWeek !== undefined) {
        const day = now.getDay()
        if (trigger.dayOfWeek.includes(day)) return true
      }
      
      if (trigger.date) {
        const [month, date] = trigger.date
        if (now.getMonth() + 1 === month && now.getDate() === date) return true
      }
    }
    return false
  }
  
  function checkMouseTrigger(interaction, event) {
    const triggers = interaction.triggers
    for (const trigger of triggers) {
      if (trigger.event && event && event.type === trigger.event) {
        return true
      }
    }
    return false
  }
  
  function checkSystemTrigger(interaction, event) {
    const triggers = interaction.triggers
    for (const trigger of triggers) {
      if (trigger.event === 'startup') {
        return event && event.type === 'startup'
      }
      
      if (trigger.event === 'idle') {
        return event && event.type === 'idle'
      }
      
      if (trigger.event === 'battery') {
        if (event && event.type === 'battery') {
          const [min, max] = trigger.level || [0, 20]
          return event.level >= min && event.level <= max
        }
      }
    }
    return false
  }
  
  function checkEnvironmentTrigger(interaction, event) {
    const triggers = interaction.triggers
    for (const trigger of triggers) {
      if (trigger.weather && event && event.weather === trigger.weather) {
        return true
      }
    }
    return false
  }
  
  function checkCustomTrigger(interaction, event) {
    const triggers = interaction.triggers
    for (const trigger of triggers) {
      if (trigger.event && event && event.type === trigger.event) {
        return true
      }
    }
    return false
  }
  
  function isOnCooldown(interactionId) {
    if (!cooldownTimers[interactionId]) return false
    return Date.now() < cooldownTimers[interactionId]
  }
  
  function setCooldown(interaction) {
    if (interaction.cooldown) {
      cooldownTimers[interaction.id] = Date.now() + interaction.cooldown
    }
  }
  
  function showDialog(content, duration = 3000) {
    activeDialog.value = { content, duration }
    
    setTimeout(() => {
      if (activeDialog.value?.content === content) {
        activeDialog.value = null
      }
    }, duration)
  }
  
  function showEmotion(emoji, duration = 1500) {
    activeEmotion.value = { emoji, duration }
    
    setTimeout(() => {
      if (activeEmotion.value?.emoji === emoji) {
        activeEmotion.value = null
      }
    }, duration)
  }

  /**
   * 点击精灵 → 随机互动组合（台词 + 表情 + 动作 + 特效）
   * @param {(actionId: string) => void} onAction 驱动动作状态机
   * @returns {{ line: string, expression: string, action: string, effect: string, effectEmoji: string }}
   */
  function triggerRandomInteraction(onAction) {
    const combo = getRandomInteraction()
    showDialog(combo.line, 3200)
    if (onAction) {
      onAction(combo.action)
    }
    const effectEmoji = ELF_EFFECT_EMOJI[combo.effect] || '✨'
    showEmotion(effectEmoji, 1600)
    return { ...combo, effectEmoji }
  }
  
  function executeResponse(response, onAction) {
    switch (response.type) {
      case 'dialog':
        showDialog(response.content, response.duration)
        break
      case 'action':
        if (onAction) onAction(response.actionId)
        break
      case 'emotion':
        showEmotion(response.emoji)
        break
      case 'effect':
        if (onAction) onAction(response.effectId)
        break
    }
  }
  
  function executeResponses(responses, onAction) {
    isInteracting.value = true
    
    let delay = 0
    responses.forEach((response, index) => {
      setTimeout(() => {
        executeResponse(response, onAction)
        
        if (index === responses.length - 1) {
          setTimeout(() => {
            isInteracting.value = false
          }, 500)
        }
      }, delay)
      
      if (response.type === 'dialog' && response.duration) {
        delay += response.duration
      } else {
        delay += 200
      }
    })
  }
  
  function triggerInteraction(interactionId, event = null, onAction) {
    const interaction = elfInteractions.find(i => i.id === interactionId)
    if (!interaction) return false
    
    if (isOnCooldown(interaction.id)) return false
    
    const checker = interactionTypes[interaction.type]?.check
    if (!checker || !checker(interaction, event)) return false
    
    const now = new Date()
    const timeSinceLast = now.getTime() - lastInteractionTime.value
    
    if (timeSinceLast < 2000) return false
    
    setCooldown(interaction)
    lastInteractionTime.value = now.getTime()
    interactionHistory.value.push({
      id: interaction.id,
      time: now.getTime(),
      type: interaction.type
    })
    
    if (interactionHistory.value.length > 50) {
      interactionHistory.value = interactionHistory.value.slice(-50)
    }
    
    executeResponses(interaction.responses, onAction)
    
    return true
  }
  
  function checkTimeInteractions(emit) {
    const now = new Date()
    
    elfInteractions.forEach(interaction => {
      if (interaction.type !== 'time') return
      if (isOnCooldown(interaction.id)) return
      
      const checker = interactionTypes.time.check
      if (checker(interaction, now)) {
        const timeSinceLast = now.getTime() - lastInteractionTime.value
        if (timeSinceLast > 300000) {
          triggerInteraction(interaction.id, now, emit)
        }
      }
    })
  }
  
  function simulateEnvironment(event, emit) {
    elfInteractions.forEach(interaction => {
      if (interaction.type !== 'environment') return
      if (isOnCooldown(interaction.id)) return
      
      const checker = interactionTypes.environment.check
      if (checker(interaction, event)) {
        triggerInteraction(interaction.id, event, emit)
      }
    })
  }
  
  function handleSystemEvent(event, onAction) {
    elfInteractions.forEach(interaction => {
      if (interaction.type !== 'system') return
      if (isOnCooldown(interaction.id)) return
      
      const checker = interactionTypes.system.check
      if (checker(interaction, event)) {
        triggerInteraction(interaction.id, event, onAction)
      }
    })
  }
  
  function handleMouseEvent(event, onAction) {
    elfInteractions.forEach(interaction => {
      if (interaction.type !== 'mouse') return
      if (isOnCooldown(interaction.id)) return
      
      const checker = interactionTypes.mouse.check
      if (checker(interaction, event)) {
        triggerInteraction(interaction.id, event, onAction)
      }
    })
  }
  
  function registerTrigger(type, callback) {
    if (!triggerListeners[type]) {
      triggerListeners[type] = []
    }
    triggerListeners[type].push(callback)
    return () => {
      triggerListeners[type] = triggerListeners[type].filter(cb => cb !== callback)
    }
  }
  
  function unregisterAllTriggers() {
    Object.keys(triggerListeners).forEach(key => {
      triggerListeners[key] = []
    })
  }
  
  let timeCheckInterval = null
  
  onMounted(() => {
    timeCheckInterval = setInterval(() => {
      checkTimeInteractions()
    }, 60000)
  })
  
  onUnmounted(() => {
    if (timeCheckInterval) {
      clearInterval(timeCheckInterval)
    }
    unregisterAllTriggers()
  })
  
  return {
    activeDialog,
    activeEmotion,
    isInteracting,
    interactionHistory,
    triggerInteraction,
    checkTimeInteractions,
    simulateEnvironment,
    handleSystemEvent,
    handleMouseEvent,
    registerTrigger,
    showDialog,
    showEmotion,
    triggerRandomInteraction
  }
}