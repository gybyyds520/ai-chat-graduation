<template>
  <div class="soul-panel" v-if="soulState">
    <div class="soul-header" @click="collapsed = !collapsed">
      <span class="soul-icon">✨</span>
      <span class="soul-title">{{ characterName }}的灵魂</span>
      <span class="soul-emotion" :class="emotionClass">{{ emotionText }}</span>
      <span class="soul-collapse">{{ collapsed ? '▸' : '▾' }}</span>
    </div>

    <div class="soul-body" v-if="!collapsed">
      <div class="soul-metrics">
        <div class="soul-metric">
          <span class="metric-label">心情</span>
          <div class="metric-bar">
            <div class="metric-fill mood" :style="{ width: moodPercent + '%' }"></div>
          </div>
          <span class="metric-value">{{ soulState.moodScore }}/100</span>
        </div>
        <div class="soul-metric">
          <span class="metric-label">对你</span>
          <div class="metric-bar">
            <div class="metric-fill attitude" :style="{ width: attitudePercent + '%' }"></div>
          </div>
          <span class="metric-value">{{ attitudeText }}</span>
        </div>
      </div>

      <div class="soul-beliefs" v-if="soulState.coreBeliefs">
        <div class="belief-label">核心信念 · 她最近在想</div>
        <div class="belief-content">{{ soulState.coreBeliefs }}</div>
      </div>

      <div class="soul-version">
        灵魂已演化 <b>{{ soulState.version || 1 }}</b> 次
        <span class="soul-updated">{{ updatedText }}</span>
      </div>

      <div class="soul-actions">
        <el-button size="small" type="primary" plain @click="onEvolve">触发一次演化</el-button>
        <el-button size="small" type="danger" plain @click="onReset">重置灵魂</el-button>
      </div>

      <div class="soul-experiences" v-if="experiences.length">
        <div class="exp-title">经历与感受</div>
        <div v-for="(exp, i) in experiences.slice(0, 3)" :key="i" class="exp-item">
          <div class="exp-experience">「{{ exp.experience }}」</div>
          <div class="exp-feeling" v-if="exp.feeling">💗 {{ exp.feeling }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSoulOverview, listSoulExperiences, resetSoul, evolveSoul } from '@/api/soul'

const props = defineProps({
  characterId: { type: [Number, String], default: null },
  characterName: { type: String, default: 'AI' }
})

const soulState = ref(null)
const experiences = ref([])
const collapsed = ref(false)

const emotionClass = computed(() => {
  const e = soulState.value?.currentEmotion || 'neutral'
  return `emotion-${e}`
})

const emotionText = computed(() => {
  const map = {
    neutral: '平静', happy: '开心', sad: '低落',
    angry: '有些生气', surprised: '惊讶', fearful: '有些担忧', love: '被爱的温暖'
  }
  return map[soulState.value?.currentEmotion] || '平静'
})

const moodPercent = computed(() => Math.max(0, Math.min(100, soulState.value?.moodScore ?? 60)))

const attitudePercent = computed(() => {
  const a = soulState.value?.attitudeScore ?? 0
  return Math.max(0, Math.min(100, (a + 100) / 2))
})

const attitudeText = computed(() => {
  const a = soulState.value?.attitudeScore ?? 0
  if (a >= 70) return '亲密无间'
  if (a >= 40) return '亲切友好'
  if (a >= 0) return '礼貌友好'
  if (a >= -40) return '有些疏离'
  return '戒备'
})

const updatedText = computed(() => {
  const t = soulState.value?.updatedAt
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
})

async function load() {
  if (!props.characterId) return
  try {
    const res = await getSoulOverview(props.characterId)
    if (res?.code === 200 || res?.data) {
      const data = res.data || res
      soulState.value = data.soulState || data
      if (data.experienceCount != null && !Array.isArray(data)) {
        // 同时拉经历流水
      }
    }
  } catch (e) {
    // 灵魂引擎是增强功能：加载失败静默，不影响对话页
  }
  try {
    const r = await listSoulExperiences(props.characterId, { page: 1, size: 5 })
    if (r?.code === 200 && r.data?.list) {
      experiences.value = r.data.list
    } else if (r?.data?.list) {
      experiences.value = r.data.list
    }
  } catch (e) {
    // 静默
  }
}

async function onEvolve() {
  try {
    await evolveSoul(props.characterId, { message: '（手动触发一次灵魂演化）' })
    ElMessage.success('灵魂已演化')
    await load()
  } catch (e) {
    ElMessage.warning('演化失败（不影响对话）')
  }
}

async function onReset() {
  try {
    await ElMessageBox.confirm('确定重置该角色的灵魂吗？将清空全部经历与演化记录。', '重置灵魂', {
      confirmButtonText: '重置',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await resetSoul(props.characterId)
    ElMessage.success('灵魂已重置')
    await load()
  } catch (e) {
    // 用户取消或失败，静默
  }
}

watch(() => props.characterId, () => {
  soulState.value = null
  experiences.value = []
  collapsed.value = false
  load()
}, { immediate: true })

defineExpose({ load })
</script>

<style scoped>
.soul-panel {
  margin: 0 0 12px;
  border: 1px solid rgba(124, 58, 237, 0.25);
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.06), rgba(79, 70, 229, 0.04));
  overflow: hidden;
}
.soul-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  user-select: none;
}
.soul-icon { font-size: 14px; }
.soul-title { font-size: 13px; font-weight: 600; color: #4f46e5; flex: 1; }
.soul-emotion {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(79, 70, 229, 0.1);
  color: #4f46e5;
}
.soul-emotion.emotion-happy { background: rgba(245, 158, 11, 0.12); color: #d97706; }
.soul-emotion.emotion-sad { background: rgba(59, 130, 246, 0.12); color: #2563eb; }
.soul-emotion.emotion-angry { background: rgba(239, 68, 68, 0.12); color: #dc2626; }
.soul-emotion.emotion-love { background: rgba(236, 72, 153, 0.12); color: #db2777; }
.soul-collapse { color: #9ca3af; font-size: 12px; }
.soul-body { padding: 4px 12px 12px; }
.soul-metrics { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; }
.soul-metric { display: flex; align-items: center; gap: 8px; }
.metric-label { font-size: 12px; color: #6b7280; width: 36px; }
.metric-bar { flex: 1; height: 6px; background: rgba(0,0,0,0.06); border-radius: 3px; overflow: hidden; }
.metric-fill { height: 100%; border-radius: 3px; transition: width 0.4s; }
.metric-fill.mood { background: linear-gradient(90deg, #8b5cf6, #4f46e5); }
.metric-fill.attitude { background: linear-gradient(90deg, #f59e0b, #ef4444); }
.metric-value { font-size: 11px; color: #6b7280; min-width: 52px; text-align: right; }
.soul-beliefs { margin-bottom: 10px; }
.belief-label { font-size: 11px; color: #9ca3af; margin-bottom: 3px; }
.belief-content { font-size: 12px; color: #374151; line-height: 1.5; }
.soul-version { font-size: 11px; color: #9ca3af; margin-bottom: 8px; }
.soul-updated { margin-left: 6px; }
.soul-actions { display: flex; gap: 8px; margin-bottom: 10px; }
.soul-experiences { border-top: 1px dashed rgba(124,58,237,0.2); padding-top: 8px; }
.exp-title { font-size: 11px; color: #9ca3af; margin-bottom: 6px; }
.exp-item { margin-bottom: 8px; }
.exp-experience { font-size: 12px; color: #4b5563; line-height: 1.4; }
.exp-feeling { font-size: 12px; color: #db2777; margin-top: 2px; }
</style>
