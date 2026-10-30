<template>
  <div class="h-full flex flex-col" :style="widgetStyle">
    <div class="flex items-center justify-between px-3 py-2 border-b border-white/10">
      <span class="text-xs opacity-70" :style="{ color: textColor }">便签</span>
      <button @click="clearNotes" class="text-xs opacity-50 hover:opacity-100 transition-opacity" :style="{ color: textColor }">清空</button>
    </div>
    
    <div class="flex-1 p-3 overflow-auto">
      <textarea
        v-model="noteContent"
        @input="handleInput"
        class="w-full h-full resize-none bg-transparent outline-none text-sm leading-relaxed"
        :style="{ color: textColor }"
        placeholder="在这里输入笔记..."
      />
    </div>
    
    <div class="px-3 py-2 border-t border-white/10 flex items-center justify-between">
      <span class="text-xs opacity-50" :style="{ color: textColor }">{{ noteContent.length }} 字</span>
      <span v-if="saved" class="text-xs opacity-50 text-green-400">已保存</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { createLogger } from '../utils/logger'

const logger = createLogger('NotesWidget')

const props = defineProps({
  config: { type: Object, default: () => ({ content: '', autoSave: true }) },
  style: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update'])

const noteContent = ref(props.config.content || '')
const saved = ref(true)
let saveTimer = null

const textColor = computed(() => props.style?.textColor || '#ffffff')
const widgetStyle = computed(() => ({
  backgroundColor: props.style?.backgroundColor || 'rgba(30, 30, 46, 0.8)',
  borderRadius: props.style?.borderRadius || '12px',
  border: `1px solid ${props.style?.borderColor || 'rgba(255, 255, 255, 0.1)'}`
}))

function handleInput() {
  saved.value = false
  logger.debug('NOTE_INPUT', 'Note content changed', { length: noteContent.value.length })
  
  if (props.config.autoSave) {
    if (saveTimer) clearTimeout(saveTimer)
    saveTimer = setTimeout(() => {
      emit('update', { config: { ...props.config, content: noteContent.value } })
      saved.value = true
      logger.info('NOTE_SAVE', 'Note auto-saved')
    }, 1000)
  }
}

function clearNotes() {
  logger.info('NOTE_CLEAR', 'Clearing notes')
  noteContent.value = ''
  handleInput()
}

watch(() => props.config.content, (newContent) => {
  if (newContent !== noteContent.value) {
    logger.debug('NOTE_SYNC', 'Syncing note content from props')
    noteContent.value = newContent
  }
})

onMounted(() => {
  logger.info('LIFECYCLE', 'NotesWidget mounted', { autoSave: props.config.autoSave, contentLength: noteContent.value.length })
})

onUnmounted(() => {
  if (saveTimer) clearTimeout(saveTimer)
  logger.info('LIFECYCLE', 'NotesWidget unmounted')
})
</script>