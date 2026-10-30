<template>
  <div class="group-chat-page">
    <!-- 左：我的群列表 -->
    <aside class="gc-sidebar">
      <div class="gc-sidebar-header">
        <h3>群聊</h3>
        <el-button size="small" @click="goBack">返回</el-button>
      </div>
      <div class="gc-group-list">
        <div v-if="groups.length === 0" class="gc-empty">暂无群聊</div>
        <div
          v-for="g in groups"
          :key="g.id"
          :class="['gc-group-item', { active: currentGroupId === g.id }]"
          @click="openGroup(g)"
        >
          <div class="gc-group-name">{{ g.name }}</div>
          <div class="gc-group-sub">群号 {{ g.id }}</div>
        </div>
      </div>
    </aside>

    <!-- 中：消息流 -->
    <section class="gc-main" v-if="currentGroupId">
      <header class="gc-header">
        <div class="gc-header-title">{{ currentGroup?.name || '群聊' }}</div>
        <button class="gc-btn" @click="openMemberPanel">成员 {{ aiMembers.length + humanMembers.length }}</button>
        <button class="gc-btn" @click="showAddAi = true">+ 添加AI角色</button>
      </header>

      <div class="gc-messages" ref="msgList">
        <div
          v-for="(m, i) in messages"
          :key="m.id || i"
          :class="['gc-msg', { mine: String(m.userId) === String(userId) }]"
        >
          <div class="gc-avatar" :style="avatarStyle(m)">{{ avatarText(m) }}</div>
          <div class="gc-bubble-wrap">
            <div class="gc-sender">
              {{ m.userName || '未知' }}
              <span v-if="isAiMessage(m)" class="gc-ai-badge">🤖 AI</span>
            </div>
            <div class="gc-bubble">{{ m.content }}</div>
          </div>
        </div>
        <div v-if="aiThinking" class="gc-typing">AI 正在思考…</div>
      </div>

      <!-- @提及候选 -->
      <div v-if="mentionCandidates.length > 0" class="gc-mention-box">
        <div
          v-for="c in mentionCandidates"
          :key="c.characterId"
          class="gc-mention-item"
          @click="applyMention(c)"
        >🤖 {{ c.characterName }}</div>
      </div>

      <footer class="gc-input">
        <el-input
          v-model="draft"
          placeholder="@AI角色 可触发它发言；/ai 让全员聊聊"
          @keyup.enter="send"
          @input="onInput"
        />
        <el-button type="primary" @click="send">发送</el-button>
      </footer>
    </section>

    <section class="gc-main gc-empty-page" v-else>
      <p>从左侧选择一个群聊开始</p>
    </section>

    <!-- 右：成员面板 -->
    <aside v-if="memberPanelOpen" class="gc-members" @click.self="memberPanelOpen = false">
      <div class="gc-members-card">
        <div class="gc-members-header">
          <span>群成员</span>
          <button @click="memberPanelOpen = false">×</button>
        </div>
        <div class="gc-members-section">
          <div class="gc-members-title">人类成员</div>
          <div v-for="m in humanMembers" :key="'h' + m.userId" class="gc-member-row">
            <span class="gc-member-dot"></span>{{ m.username || ('用户' + m.userId) }}
          </div>
          <div v-if="humanMembers.length === 0" class="gc-members-empty">暂无</div>
        </div>
        <div class="gc-members-section">
          <div class="gc-members-title">AI 角色</div>
          <div v-for="a in aiMembers" :key="'a' + a.characterId" class="gc-member-row ai">
            <span class="gc-member-robot">🤖</span>{{ characterNameOf(a.characterId) }}
            <button class="gc-remove" @click="removeAi(a.characterId)">移除</button>
          </div>
          <div v-if="aiMembers.length === 0" class="gc-members-empty">未添加 AI 角色</div>
        </div>
      </div>
    </aside>

    <!-- 添加 AI 角色弹窗 -->
    <el-dialog v-model="showAddAi" title="添加 AI 角色到群" width="420px">
      <div v-if="availableCharacters.length === 0" class="gc-empty">暂无可添加的角色</div>
      <div
        v-for="c in availableCharacters"
        :key="c.id"
        class="gc-add-item"
        @click="addAi(c.id)"
      >
        <span class="gc-member-robot">🤖</span>{{ c.name }}
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { generateAvatarUrl } from '@/utils/localAvatar'
import { parseMentionedCharacters, shouldTriggerAi, stripMentions } from '@/utils/groupChatInput'

const router = useRouter()
const userId = ref(JSON.parse(localStorage.getItem('user') || '{}').id)

const groups = ref([])
const currentGroupId = ref(null)
const currentGroup = ref(null)
const messages = ref([])
const draft = ref('')
const msgList = ref(null)

const humanMembers = ref([])
const aiMembers = ref([])
const availableCharacters = ref([])
const characterNameMap = ref({})
const memberPanelOpen = ref(false)
const showAddAi = ref(false)
const aiThinking = ref(false)
const mentionCandidates = ref([])

const goBack = () => router.push('/friends')

async function loadGroups() {
  try {
    const res = await request.get(`/api/group/my-groups?userId=${userId.value}`)
    if (res.code === 200) groups.value = res.data || []
  } catch (e) { /* 静默 */ }
}

async function loadCharacters() {
  try {
    const res = await request.get(`/api/character/list/user/${userId.value}`)
    if (res.code === 200) {
      availableCharacters.value = res.data || []
      const map = {}
      for (const c of availableCharacters.value) map[c.id] = c.name
      characterNameMap.value = map
    }
  } catch (e) { /* 静默 */ }
}

async function openGroup(g) {
  currentGroupId.value = g.id
  currentGroup.value = g
  memberPanelOpen.value = false
  await Promise.all([loadMessages(), loadMembers(), loadAiMembers()])
}

async function loadMessages() {
  try {
    const res = await request.get(`/api/group/messages/${currentGroupId.value}?limit=50`)
    if (res.code === 200) {
      messages.value = res.data || []
      scrollBottom()
    }
  } catch (e) { /* 静默 */ }
}

async function loadMembers() {
  try {
    const res = await request.get(`/api/group/members/${currentGroupId.value}`)
    if (res.code === 200) humanMembers.value = res.data || []
  } catch (e) { humanMembers.value = [] }
}

async function loadAiMembers() {
  try {
    const res = await request.get(`/api/group/${currentGroupId.value}/ai-members`)
    if (res.code === 200) {
      aiMembers.value = (res.data || []).map((m) => ({
        characterId: m.characterId,
        characterName: characterNameMap.value[m.characterId] || `角色${m.characterId}`
      }))
    }
  } catch (e) { aiMembers.value = [] }
}

function isAiMessage(m) {
  // AI 消息的 senderId 落在群 AI 角色 characterId 集合内
  return aiMembers.value.some((a) => String(a.characterId) === String(m.userId))
}

function avatarStyle(m) {
  return { background: isAiMessage(m) ? '#6366f1' : '#0ea5e9' }
}
function avatarText(m) {
  const n = m.userName || '?'
  return n.slice(0, 1)
}
function characterNameOf(cid) {
  return characterNameMap.value[cid] || `角色${cid}`
}

async function send() {
  const text = draft.value.trim()
  if (!text || !currentGroupId.value) return
  draft.value = ''
  mentionCandidates.value = []

  try {
    const res = await request.post('/api/group/message', {
      groupId: currentGroupId.value,
      userId: userId.value,
      userName: '我',
      content: text
    })
    if (res.code !== 200) {
      ElMessage.error(res.message || '发送失败')
      return
    }
    await loadMessages()

    const mentioned = parseMentionedCharacters(text, aiMembers.value)
    if (shouldTriggerAi(text, mentioned)) {
      aiThinking.value = true
      try {
        const aiRes = await request.post(`/api/group/${currentGroupId.value}/ai-round`, {
          triggerMessage: stripMentions(text)
        })
        if (aiRes.code === 200) await loadMessages()
      } catch (e) { /* AI 回复失败不影响用户消息 */ } finally {
        aiThinking.value = false
      }
    }
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

function onInput() {
  // 输入 @ 后弹出 AI 角色候选
  const m = draft.value.match(/@([^\s@]*)$/)
  if (m) {
    const q = m[1]
    mentionCandidates.value = aiMembers.value.filter(
      (a) => !q || a.characterName.includes(q)
    )
  } else {
    mentionCandidates.value = []
  }
}

function applyMention(c) {
  draft.value = draft.value.replace(/@([^\s@]*)$/, '@' + c.characterName + ' ')
  mentionCandidates.value = []
}

function openMemberPanel() {
  memberPanelOpen.value = true
}

async function addAi(characterId) {
  try {
    const res = await request.post(`/api/group/${currentGroupId.value}/ai-members`, { characterId })
    if (res.code === 200) {
      ElMessage.success('已添加 AI 角色')
      showAddAi.value = false
      await loadAiMembers()
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch (e) { ElMessage.error('添加失败') }
}

async function removeAi(characterId) {
  try {
    const res = await request.delete(`/api/group/${currentGroupId.value}/ai-members/${characterId}`)
    if (res.code === 200) {
      ElMessage.success('已移除')
      await loadAiMembers()
    }
  } catch (e) { ElMessage.error('移除失败') }
}

function scrollBottom() {
  nextTick(() => {
    if (msgList.value) msgList.value.scrollTop = msgList.value.scrollHeight
  })
}

onMounted(() => {
  loadGroups()
  loadCharacters()
})
</script>

<style scoped>
.group-chat-page { display: flex; height: 100vh; background: var(--bg-primary, #f5f7fa); }
.gc-sidebar { width: 240px; background: var(--card-bg, #fff); border-right: 1px solid var(--border-primary, #e5e7eb); display: flex; flex-direction: column; }
.gc-sidebar-header { display: flex; align-items: center; justify-content: space-between; padding: 16px; }
.gc-sidebar-header h3 { margin: 0; font-size: 16px; }
.gc-group-list { flex: 1; overflow-y: auto; padding: 8px; }
.gc-group-item { padding: 10px 12px; border-radius: 8px; cursor: pointer; margin-bottom: 4px; }
.gc-group-item:hover { background: var(--bg-hover, #f1f5f9); }
.gc-group-item.active { background: #4f46e5; color: #fff; }
.gc-group-name { font-size: 14px; font-weight: 500; }
.gc-group-sub { font-size: 11px; opacity: 0.7; }
.gc-main { flex: 1; display: flex; flex-direction: column; }
.gc-empty-page { align-items: center; justify-content: center; color: #94a3b8; }
.gc-header { display: flex; align-items: center; gap: 10px; padding: 14px 20px; background: var(--card-bg, #fff); border-bottom: 1px solid var(--border-primary, #e5e7eb); }
.gc-header-title { flex: 1; font-size: 16px; font-weight: 600; }
.gc-btn { padding: 6px 12px; border: none; border-radius: 6px; background: var(--bg-hover, #f1f5f9); cursor: pointer; }
.gc-messages { flex: 1; overflow-y: auto; padding: 20px; }
.gc-msg { display: flex; margin-bottom: 16px; }
.gc-msg.mine { flex-direction: row-reverse; }
.gc-avatar { width: 38px; height: 38px; border-radius: 50%; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 15px; flex-shrink: 0; }
.gc-bubble-wrap { margin: 0 10px; max-width: 60%; }
.gc-sender { font-size: 12px; color: #64748b; margin-bottom: 4px; }
.gc-msg.mine .gc-sender { text-align: right; }
.gc-ai-badge { background: #eef2ff; color: #4f46e5; border-radius: 4px; padding: 0 4px; margin-left: 4px; font-size: 11px; }
.gc-bubble { background: var(--card-bg, #fff); padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.5; word-break: break-word; }
.gc-msg.mine .gc-bubble { background: #4f46e5; color: #fff; }
.gc-typing { font-size: 12px; color: #94a3b8; padding: 6px 0; }
.gc-input { display: flex; gap: 10px; padding: 14px 20px; background: var(--card-bg, #fff); border-top: 1px solid var(--border-primary, #e5e7eb); }
.gc-mention-box { position: absolute; bottom: 70px; left: 50%; transform: translateX(-50%); background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.1); z-index: 20; }
.gc-mention-item { padding: 8px 16px; cursor: pointer; }
.gc-mention-item:hover { background: #f1f5f9; }
.gc-members { position: fixed; right: 0; top: 0; bottom: 0; width: 260px; background: rgba(0,0,0,0.3); z-index: 30; }
.gc-members-card { position: absolute; right: 0; top: 0; bottom: 0; width: 260px; background: #fff; padding: 16px; overflow-y: auto; }
.gc-members-header { display: flex; justify-content: space-between; font-weight: 600; margin-bottom: 12px; }
.gc-members-title { font-size: 12px; color: #94a3b8; margin: 10px 0 6px; }
.gc-member-row { display: flex; align-items: center; gap: 6px; padding: 6px 0; font-size: 14px; }
.gc-member-dot { width: 8px; height: 8px; border-radius: 50%; background: #22c55e; }
.gc-remove { margin-left: auto; border: none; background: transparent; color: #ef4444; cursor: pointer; font-size: 12px; }
.gc-members-empty { font-size: 12px; color: #cbd5e1; }
.gc-add-item { display: flex; align-items: center; gap: 8px; padding: 10px; cursor: pointer; border-radius: 6px; }
.gc-add-item:hover { background: #f1f5f9; }
.gc-empty { color: #94a3b8; padding: 20px; text-align: center; }
</style>
