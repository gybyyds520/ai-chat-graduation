<template>
  <div class="chat-page">
    <div :class="['chat-sidebar', { collapsed: sidebarCollapsed }]" :style="{ transform: sidebarCollapsed ? 'translateX(-100%)' : 'translateX(0)' }">
      <div class="sidebar-header">
        <div class="logo-section">
          <svg width="32" height="32" viewBox="0 0 48 48" fill="none">
            <rect width="48" height="48" rx="12" fill="url(#sidebarLogoGrad)"/>
            <path d="M24 14C18.48 14 14 18.48 14 24s4.48 10 10 10 10-4.48 10-10S29.52 14 24 14zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z" fill="white"/>
            <defs>
              <linearGradient id="sidebarLogoGrad" x1="0" y1="0" x2="48" y2="48">
                <stop stop-color="#4f46e5"/>
                <stop offset="1" stop-color="#7c3aed"/>
              </linearGradient>
            </defs>
          </svg>
          <h3>AI Chat</h3>
        </div>
        <div class="user-info">
          <LocalAvatar :seed="String(userId || '')" :size="32" />
          <span class="user-name">{{ userNickname || '用户' }}</span>
        </div>
      </div>

      <div class="section-title">
        <span>AI 角色</span>
      </div>

      <div class="character-list">
        <div
          v-for="character in characters"
          :key="character.id"
          :class="['character-item', { active: selectedCharacter?.id === character.id }]"
          @click="selectCharacter(character)"
        >
          <div class="character-avatar">
            <LocalAvatar :seed="character.name || String(character.id)" :size="44" :src="character.avatar" :variant="character.avatarStyle || 'avataaars'" />
          </div>
          <div class="character-info">
            <div class="character-name">
              {{ character.name }}
              <span v-if="character.isShared" class="shared-badge">已分享</span>
            </div>
            <div class="character-preview">{{ character.prompt?.substring(0, 20) }}...</div>
          </div>
          <el-button
            v-if="!character.isSystem"
            :class="['share-plaza-btn', { shared: character.isShared }]"
            size="small"
            @click.stop="shareToPlaza(character)"
          >{{ character.isShared ? '取消分享' : '分享到广场' }}</el-button>
          <div v-if="selectedCharacter?.id === character.id" class="active-indicator"></div>
        </div>
      </div>

      <div class="quota-info" v-if="quotaText" :title="'额度每日零点重置'">{{ quotaText }}</div>
      <div class="sidebar-actions">
        <el-button type="primary" @click="goToFriendChat" class="action-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          好友聊天
        </el-button>
        <el-button @click="goToFriends" class="action-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          好友管理
        </el-button>
        <el-button type="success" @click="goToCharacterImport" class="action-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="17 8 12 3 7 8"/>
            <line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          导入角色
        </el-button>
        <el-button type="warning" @click="goToCreateCharacter" class="action-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="16"/>
            <line x1="8" y1="12" x2="16" y2="12"/>
          </svg>
          创建角色
        </el-button>
        <el-button type="info" @click="goToFeed" class="action-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
          AI投喂
        </el-button>
        <el-button type="info" @click="goToDesktopElf" class="action-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          桌面小精灵
        </el-button>
        <el-button type="warning" @click="goToMemory" class="action-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2a5 5 0 0 0-5 5v1a5 5 0 0 0 0 10v1a5 5 0 0 0 10 0v-1a5 5 0 0 0 0-10V7a5 5 0 0 0-5-5z"/>
          </svg>
          AI记忆
        </el-button>
        <el-button type="info" @click="goToCommunity" class="action-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          兴趣社群
        </el-button>
        <el-button type="info" @click="goToPlaza" class="action-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          发现角色
        </el-button>
        <el-button type="info" @click="goToProfile" class="action-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
          个人中心
        </el-button>
        <el-button type="warning" @click="goToUpgrade" class="action-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          版本升级
        </el-button>
        <el-button type="danger" @click="handleLogout" class="action-btn logout">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          退出登录
        </el-button>
      </div>
    </div>

    <div v-if="!sidebarCollapsed" class="sidebar-overlay" @click="sidebarCollapsed = true"></div>

    <div class="chat-main">
      <div class="mobile-header" v-if="sidebarCollapsed">
        <button class="menu-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="12" x2="21" y2="12"/>
            <line x1="3" y1="6" x2="21" y2="6"/>
            <line x1="3" y1="18" x2="21" y2="18"/>
          </svg>
        </button>
      </div>

      <div class="chat-header" v-if="selectedCharacter">
        <div class="header-info">
          <LocalAvatar :seed="selectedCharacter.name || String(selectedCharacter.id)" :size="40" :src="selectedCharacter.avatar" :variant="selectedCharacter.avatarStyle || 'avataaars'" />
          <div class="header-text">
            <span class="character-name">{{ selectedCharacter.name }}</span>
            <span class="character-status">在线</span>
          </div>
        </div>
        <div class="header-actions">
          <el-button 
            circle 
            :class="['ollama-status-btn', { 'status-running': ollamaStatus === 'running', 'status-stopped': ollamaStatus === 'stopped' }]"
            @click="checkOllamaStatus"
            :title="ollamaStatus === 'running' ? 'Ollama服务运行中' : 'Ollama服务已停止'"
          >
            <svg v-if="ollamaStatus === 'running'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <rect x="9" y="9" width="6" height="6"/>
            </svg>
          </el-button>
          <el-button 
            circle 
            @click="restartOllama"
            :loading="isRestarting"
            title="重启Ollama服务"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
              <path d="M3 3v5h5"/>
              <path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/>
              <path d="M16 21h5v-5"/>
            </svg>
          </el-button>
          <el-button 
            circle 
            :class="['voice-toggle-btn', { 'voice-enabled': speechEnabled }]"
            @click="toggleVoiceOutput"
            :title="speechEnabled ? '关闭语音输出' : '开启语音输出'"
          >
            <svg v-if="speechEnabled" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
              <path d="M15.54 8.46a5 5 0 010 7.07"/>
              <path d="M19.07 4.93a10 10 0 010 14.14"/>
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/>
              <line x1="23" y1="9" x2="17" y2="15"/>
              <line x1="17" y1="9" x2="23" y2="15"/>
            </svg>
          </el-button>
          <el-button circle>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="1"/>
              <circle cx="19" cy="12" r="1"/>
              <circle cx="5" cy="12" r="1"/>
            </svg>
          </el-button>
        </div>
      </div>

      <!-- 灵魂引擎：此刻的自我（对话前注入 + 展示演化状态） -->
      <SoulPanel
        v-if="selectedCharacter"
        :character-id="selectedCharacter.id"
        :character-name="selectedCharacter.name"
        class="soul-panel-host"
      />

      <div class="message-list" ref="messageList">
        <div v-if="!selectedCharacter" class="empty-state">
          <div class="empty-icon">💬</div>
          <h3>选择一个 AI 角色开始对话</h3>
          <p>在左侧列表中选择一个角色，开启智能对话之旅</p>
        </div>

        <div v-else-if="messages.length === 0" class="welcome-state">
          <LocalAvatar :seed="selectedCharacter.name || String(selectedCharacter.id)" :size="64" :src="selectedCharacter.avatar" :variant="selectedCharacter.avatarStyle || 'avataaars'" />
          <h3>与 {{ selectedCharacter.name }} 对话</h3>
          <p>{{ selectedCharacter.prompt || '开始你的智能对话吧' }}</p>
        </div>

        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message-item', msg.senderType, { 'is-system-bubble': isSystemBubble(msg), 'is-deleted-bubble': msg.isDeleted, 'is-greeting-bubble': msg.isGreeting }]"
        >
          <div class="message-avatar" v-if="!msg.isDeleted">
            <LocalAvatar
              :seed="msg.senderType === 'user' ? 'current_user' : (selectedCharacter?.name || String(selectedCharacter?.id))"
              :size="36"
              :src="msg.senderType === 'user' ? '' : selectedCharacter?.avatar"
              :variant="selectedCharacter?.avatarStyle || 'avataaars'"
            />
          </div>
          <div class="message-bubble">
            <div v-if="msg.isDeleted" class="deleted-placeholder">
              <span class="deleted-icon">🗘</span> {{ deletedMessagePlaceholder() }}
            </div>
            <template v-else>
              <img v-if="msg.imageUrl" :src="msg.imageUrl" class="chat-image-preview" alt="用户发送的图片" />
              <div class="message-content">{{ msg.content }}<span v-if="msg.streaming" class="type-cursor">▋</span></div>
              <!-- M4 ⑬：AI 回复语音气泡（播放按钮 + 时长 + 波形柱） -->
              <div
                v-if="msg.senderType === 'ai' && !msg.streaming && msg.content"
                class="voice-bubble"
                :class="{ playing: voicePlayingIndex === index }"
                @click="playAiVoice(index)"
              >
                <span class="voice-play-icon">{{ voicePlayingIndex === index ? '❚❚' : '▶' }}</span>
                <span class="voice-wave">
                  <i
                    v-for="(h, wi) in voiceBars(msg.content)"
                    :key="wi"
                    :style="{ height: h + 'px' }"
                  ></i>
                </span>
                <span class="voice-duration">{{ formatVoiceDuration(estimateVoiceSeconds(msg.content)) }}</span>
              </div>
              <div class="message-time">{{ formatTime(msg.createTime) }}</div>
              <div class="message-actions">
                <el-button
                  v-if="!isSending && msg.senderType !== 'user' && index === lastAiMessageIndex"
                  link
                  size="small"
                  class="regen-msg-btn"
                  @click="regenerateReply(index)"
                >重新生成</el-button>
                <el-button
                  v-if="!isSending && msg.senderType === 'user'"
                  link
                  size="small"
                  class="edit-msg-btn"
                  @click="editUserMessage(index)"
                >编辑</el-button>
                <el-button
                  v-if="!isSending && index === lastAiMessageIndex"
                  link
                  size="small"
                  class="delete-msg-btn"
                  @click="deleteMessage(index)"
                >删除</el-button>
                <el-button
                  v-if="!isSending && !msg.isDeleted"
                  link
                  size="small"
                  class="share-msg-btn"
                  @click="shareConversationAsCard(index)"
                >分享</el-button>
              </div>
            </template>
          </div>
        </div>

        <!-- 快捷回复建议（基于角色人设） -->
        <div v-if="quickReplies.length && !isSending && selectedCharacter" class="quick-replies">
          <button
            v-for="(q, i) in quickReplies"
            :key="i"
            class="quick-reply-chip"
            @click="useQuickReply(q)"
          >{{ q }}</button>
        </div>

        <div v-if="isSending" class="message-item ai">
          <div class="message-avatar">
            <LocalAvatar :seed="selectedCharacter?.name || String(selectedCharacter?.id || '')" :size="36" :src="selectedCharacter?.avatar" :variant="selectedCharacter?.avatarStyle || 'avataaars'" />
          </div>
          <div class="message-bubble">
            <div class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-input-area">
        <div class="input-container">
          <el-input
            v-model="inputMessage"
            :disabled="isSending || !selectedCharacter"
            :placeholder="recognitionText || '输入消息...'"
            @keyup.enter="sendMessage"
          />
          <input
            ref="imageInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            style="display:none"
            @change="onImageSelected"
          />
          <el-button
            @click="triggerImageUpload"
            :loading="visionUploading"
            :disabled="!selectedCharacter"
            title="发送图片（Ollama llava 视觉理解）"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
              <circle cx="8.5" cy="8.5" r="1.5"/>
              <polyline points="21 15 16 10 5 21"/>
            </svg>
          </el-button>
          <el-button
            @click="toggleVoiceInput"
            :class="['voice-btn', { listening: isListening }]"
            :disabled="!selectedCharacter"
            :title="isListening ? '停止录音' : '开始语音输入'"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
              <line x1="12" y1="19" x2="12" y2="23"/>
              <line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
          </el-button>
          <el-button
            @click="sendMessage"
            :disabled="isSending || !selectedCharacter || !inputMessage.trim()"
            class="send-btn"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"/>
              <polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          </el-button>
          <el-button
            @click="toggleVoiceCallPanel"
            :type="voiceCallOpen ? 'primary' : 'default'"
            :disabled="!selectedCharacter"
            title="语音通话"
          >🎤 语音通话</el-button>
        </div>

        <!-- 实时语音通话面板 -->
        <div v-if="voiceCallOpen" class="voice-call-panel">
          <div class="voice-call-header">
            <span class="voice-call-title">实时语音通话</span>
            <span class="voice-call-state" :class="voiceCallState">{{ voiceCallStateText }}</span>
            <span v-if="voiceCallAdapterMock" class="voice-call-badge">模拟模式</span>
          </div>
          <div class="voice-call-controls">
            <el-button
              size="small"
              :type="voiceCallState === 'recording' ? 'danger' : 'primary'"
              @click="onVoiceRecordClick"
            >{{ voiceCallState === 'recording' ? '停止并发送' : '开始说话' }}</el-button>
            <el-button size="small" @click="onVoiceEndCall">结束通话</el-button>
            <el-select v-model="voiceCallSelected" size="small" style="width: 130px" title="选择音色">
              <el-option v-for="v in voiceProfiles" :key="v.voiceId" :label="v.name" :value="v.voiceId" />
            </el-select>
          </div>
          <div class="voice-call-text" v-if="voiceCallUserText">
            <div class="vc-row user">你说：{{ voiceCallUserText }}</div>
            <div class="vc-row ai">AI：{{ voiceCallAiText }}</div>
          </div>
        </div>
        <div class="input-hint" v-if="!selectedCharacter">
          请先选择一个 AI 角色
        </div>
        <div v-if="isListening" class="voice-indicator">
          <span class="pulse"></span>
          正在聆听...
        </div>
      </div>
    </div>

    <!-- 主页入场动画：登录进入主页面时播放（每次浏览器会话首次进入播放一次，可跳过；全局背景面板可随时重播） -->
    <EntranceVideoOverlay
      ref="entranceOverlayRef"
      :video-src="entranceVideoSrc"
      storage-key="entrance_video_played_chat"
      @finish="onEntranceFinished"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { pinMessageToMemory } from '@/api/memory'
import { getCurrentQuota } from '@/api/quota'
import logger from '@/utils/logger'
import LocalAvatar from '@/components/LocalAvatar.vue'
import SoulPanel from '@/components/SoulPanel.vue'
import EntranceVideoOverlay from '@/components/EntranceVideoOverlay.vue'
import ChatWebSocketClient from '@/utils/websocket.js'
import speechService from '@/utils/speech.js'
import speechRecognitionService from '@/utils/speechRecognition.js'
import voiceCallClient, { VOICE_STATE } from '@/utils/voiceCall.js'
import { VOICE_PROFILES, loadVoicePreference } from '@/utils/voiceProfiles.js'
import { drawShareCard, exportCardPng } from '@/utils/shareCard.js'
import {
  buildGreetingMessage,
  isSystemBubble,
  generateQuickReplies,
  deletedMessagePlaceholder
} from '@/utils/chatExperience'
import { formatVoiceDuration, waveformHeights, seedFromString } from '@/utils/voiceBubble'

const router = useRouter()

// 主页入场动画视频（本地静态资源，古风美人 · 蝴蝶光阵 · 欢迎回来）
const entranceVideoSrc = '/videos/entrance.mp4'
const entranceOverlayRef = ref(null)
const onEntranceFinished = () => { /* 动画播放完成/跳过，主页面已可正常使用 */ }

// 全局背景面板（右下角）触发“重播开场动画”时，重新展示入场动画
const handleReplayEntrance = () => {
  try { sessionStorage.removeItem('entrance_video_played_chat') } catch (e) { /* ignore */ }
  entranceOverlayRef.value?.show()
}

// 修复：原代码误用 ref(() => {...})，导致 userId.value 是函数而非值，
// 传给 LocalAvatar 的 seed 属性时触发 "props.seed.replace is not a function" 错误，
// 进而使整个 ChatView 渲染崩溃白屏。改为 computed 以正确求值。
const userId = computed(() => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.id || user.userId || localStorage.getItem('token')
})
const userNickname = computed(() => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.nickname || user.username || ''
})

const goToFriends = () => router.push('/friends')
const goToFriendChat = () => router.push('/friend-chat')
const goToCharacterImport = () => router.push('/character-import')
const goToCreateCharacter = () => router.push('/create-character')
const goToFeed = () => router.push('/feed')
const goToDesktopElf = () => router.push('/desktop-elf')
const goToProfile = () => router.push('/profile')
const goToUpgrade = () => router.push('/upgrade')
const goToCommunity = () => router.push('/community')
const goToMemory = () => router.push('/memory')

const pinToMemory = async (msg) => {
  try {
    await pinMessageToMemory({ content: msg.content, source: 'chat' })
    ElMessage.success('已钉选到长期记忆')
  } catch (e) {
    ElMessage.error('钉选失败，请稍后重试')
  }
}
const goToPlaza = () => router.push('/plaza')

const characters = ref([])
const selectedCharacter = ref(null)
const messages = ref([])
const inputMessage = ref('')
const isSending = ref(false)
const messageList = ref(null)
const sidebarCollapsed = ref(false)
const wsClient = ref(null)
const currentAiMessageIndex = ref(-1)
const isListening = ref(false)
const recognitionText = ref('')
const ollamaStatus = ref('unknown')
// 今日剩余额度显示（对接 /api/quota/current）
const quotaText = ref('')
const loadQuota = async () => {
  try {
    const res = await getCurrentQuota()
    if (res && res.code === 200 && res.data && res.data.features) {
      const chat = res.data.features['ai_chat'] || {}
      if (chat.available === false) {
        quotaText.value = ''
      } else if (!chat.limit || chat.limit <= 0) {
        quotaText.value = 'AI 对话：不限次'
      } else {
        quotaText.value = `今日剩余 AI 对话 ${chat.remaining} / ${chat.limit} 次`
      }
    }
  } catch (e) {
    // 额度查询失败不阻塞聊天主流程
  }
}
const isRestarting = ref(false)
const speechEnabled = ref(speechService.enabled)

// ===== 聊天体验升级：开场白 / 快捷回复 / 删除占位 / 打字机 =====
const quickReplies = ref([])
const lastAiMessageIndex = ref(-1)
// 记录每个角色是否已展示过开场白，避免切换角色重复弹出
const greetedCharacterIds = new Set()

// 生成快捷回复建议（基于角色人设）
const refreshQuickReplies = () => {
  if (!selectedCharacter.value) {
    quickReplies.value = []
    return
  }
  const persona = selectedCharacter.value.personality || selectedCharacter.value.prompt || ''
  quickReplies.value = generateQuickReplies(persona)
}

const useQuickReply = (text) => {
  if (!text) return
  inputMessage.value = text
  sendMessage()
}

// 删除消息：不直接移除，而是显示"消息已删除"占位
const deleteMessage = (index) => {
  const msg = messages.value[index]
  if (!msg || msg.isDeleted) return
  msg.isDeleted = true
  msg.content = ''
  quickReplies.value = []
}

// M2 ⑧：把从开头到 index 的对话绘制成分享卡片并导出 PNG（纯 Canvas，无外部依赖）
const shareConversationAsCard = (index) => {
  try {
    const slice = messages.value
      .slice(0, index + 1)
      .filter((m) => !m.isDeleted && m.content)
      .slice(-10)
      .map((m) => ({
        role: m.senderType === 'user' ? 'user' : 'assistant',
        content: m.content,
        time: m.createTime ? new Date(m.createTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : ''
      }))
    if (slice.length === 0) {
      ElMessage.warning('暂无可分享的内容')
      return
    }
    const canvas = document.createElement('canvas')
    drawShareCard(canvas, slice, {
      characterName: selectedCharacter.value?.name || 'AI 角色',
      appName: 'AI 对话'
    })
    const url = exportCardPng(canvas)
    const a = document.createElement('a')
    a.href = url
    a.download = `share-${selectedCharacter.value?.name || 'chat'}-${Date.now()}.png`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    ElMessage.success('已导出分享图片')
  } catch (e) {
    ElMessage.error('生成分享图片失败')
  }
}

// M1 ①：重新生成 AI 回复（有后端消息 id 走 /regenerate，否则以最后一条用户消息重发）
const regenerateReply = async (index) => {
  const msg = messages.value[index]
  if (!msg || isSending.value) return
  if (msg.id) {
    try {
      const res = await request.post(`/api/chat/regenerate/${msg.id}`)
      if (res.code === 200 && res.data?.content) {
        msg.content = res.data.content
        return
      }
    } catch (e) { /* fallthrough */ }
  }
  // 兜底：找到上一条用户消息重发，触发新的 AI 回复
  for (let i = index - 1; i >= 0; i--) {
    if (messages.value[i].senderType === 'user' && !messages.value[i].isDeleted) {
      inputMessage.value = messages.value[i].content
      sendMessage()
      return
    }
  }
  ElMessage.warning('暂无可重新生成的对话')
}

// M1 ①：编辑本人用户消息，编辑后清除下游回复
const editUserMessage = async (index) => {
  const msg = messages.value[index]
  if (!msg || isSending.value) return
  const newText = window.prompt('编辑消息：', msg.content || '')
  if (newText == null) return
  const trimmed = newText.trim()
  if (!trimmed) return
  if (msg.id) {
    try {
      await request.put(`/api/chat/message/${msg.id}`, { content: trimmed })
    } catch (e) { /* 本地仍更新 */ }
  }
  msg.content = trimmed
  // 清除该消息之后的下游 AI 回复
  messages.value = messages.value.slice(0, index + 1)
}

// ===== 实时语音通话（半双工轮次管道） =====
const voiceCallOpen = ref(false)
const voiceCallState = ref('idle')
const voiceCallUserText = ref('')
const voiceCallAiText = ref('')
const voiceCallAdapterMock = ref(false)
const voiceProfiles = VOICE_PROFILES
const voiceCallSelected = ref(loadVoicePreference())
let voiceStopRecord = null
let voiceStopAsr = null

const voiceCallStateText = computed(() => ({
  idle: '待命中', recording: '录音中…', processing: '识别回复中…', playing: '播放回复中…'
}[voiceCallState.value] || '待命中'))

voiceCallClient.onStateChange = (s) => { voiceCallState.value = s }

const toggleVoiceCallPanel = async () => {
  voiceCallOpen.value = !voiceCallOpen.value
  if (voiceCallOpen.value) {
    try {
      await voiceCallClient.startVoiceCall(selectedCharacter.value?.id)
      voiceCallAdapterMock.value = !!voiceCallClient.adapterInfo?.mockMode
    } catch (e) {
      ElMessage.error('开启语音通话失败，请重试')
      voiceCallOpen.value = false
    }
  } else {
    await onVoiceEndCall()
  }
}

const onVoiceRecordClick = async () => {
  try {
    if (voiceCallState.value === 'recording') {
      // ─── 停止并发送 ───
      if (voiceStopAsr) {
        // 浏览器语音识别模式：等待最终转写文本合并后走 LLM -> TTS
        const stopFn = voiceStopAsr
        voiceStopAsr = null
        const text = String((await stopFn()) || '').trim()
        if (text) {
          const data = await voiceCallClient.sendText(text)
          voiceCallUserText.value = text
          voiceCallAiText.value = data.aiText || ''
          playVoiceResponse(data)
        } else {
          ElMessage.warning('没听清你说的话，请靠近麦克风再说一次')
          voiceCallState.value = 'recording'
        }
      } else if (voiceStopRecord) {
        // 录音上传模式（浏览器不支持语音识别时）：上传音频走后端 ASR
        const blob = await voiceStopRecord()
        voiceStopRecord = null
        const data = await voiceCallClient.sendAudio(blob)
        voiceCallUserText.value = data.userText || ''
        voiceCallAiText.value = data.aiText || ''
        playVoiceResponse(data)
      }
    } else {
      // ─── 开始说话 ───
      // 优先浏览器 Web Speech API 语音识别（真实转写、无需后端 ASR）
      if (voiceCallClient.isBrowserAsrSupported()) {
        voiceCallUserText.value = ''
        voiceStopAsr = voiceCallClient.startBrowserAsr((text) => {
          voiceCallUserText.value = text
        }, {
          onError: (msg) => {
            ElMessage.error(msg)
            // 权限/设备错误时自动回到可重试状态
            if (voiceStopAsr) { voiceStopAsr = null }
            voiceCallState.value = 'recording'
          }
        })
        voiceCallState.value = 'recording'
      } else {
        voiceStopRecord = await voiceCallClient.recordAudio()
        if (!voiceStopRecord) {
          ElMessage.error('录音启动失败，请检查麦克风权限')
        }
      }
    }
  } catch (e) {
    ElMessage.error('语音处理失败：' + (e.message || e))
    voiceCallState.value = 'recording'
  }
}

/**
 * 播放 AI 语音回复：优先后端合成的音频；
 * 后端音频为空（如 TTS 失败/静音占位）时降级用浏览器语音合成朗读（真实中文语音）。
 */
const playVoiceResponse = (data) => {
  if (data.audioBase64) {
    voiceCallClient.playAudio(data.audioBase64, data.audioFormat)
  } else if (data.aiText) {
    speakWithBrowser(data.aiText)
  } else {
    voiceCallState.value = 'recording'
  }
}

/** 浏览器 SpeechSynthesis 朗读（无外部依赖的真实中文语音）。 */
const speakWithBrowser = (text) => {
  try {
    if (!window.speechSynthesis) {
      voiceCallState.value = 'recording'
      return
    }
    window.speechSynthesis.cancel()
    const u = new SpeechSynthesisUtterance(text)
    u.lang = 'zh-CN'
    u.rate = 1
    const voices = window.speechSynthesis.getVoices()
    const zh = voices.find(v => v.lang && v.lang.toLowerCase().startsWith('zh'))
    if (zh) u.voice = zh
    u.onend = () => { voiceCallState.value = 'recording' }
    u.onerror = () => { voiceCallState.value = 'recording' }
    window.speechSynthesis.speak(u)
  } catch (e) {
    voiceCallState.value = 'recording'
  }
}

const onVoiceEndCall = async () => {
  if (voiceStopAsr) { voiceStopAsr(); voiceStopAsr = null }
  await voiceCallClient.endVoiceCall()
  voiceCallOpen.value = false
  voiceCallState.value = 'idle'
  voiceCallUserText.value = ''
  voiceCallAiText.value = ''
}


const checkOllamaStatus = async () => {
  try {
    const response = await request.get('/api/aisystem/ollama/status')
    if (response.code === 200) {
      ollamaStatus.value = response.data.available ? 'running' : 'stopped'
      ElMessage.info(`Ollama服务状态: ${ollamaStatus.value === 'running' ? '运行中' : '已停止'}`)
    }
  } catch (error) {
    logger.error('检查Ollama状态失败:', error)
    ollamaStatus.value = 'error'
  }
}

const restartOllama = async () => {
  isRestarting.value = true
  try {
    const response = await request.post('/api/aisystem/ollama/restart')
    if (response.code === 200) {
      ElMessage.success('Ollama服务重启请求已发送')
      ollamaStatus.value = 'restarting'
      
      setTimeout(() => {
        checkOllamaStatus()
      }, 5000)
    } else {
      ElMessage.error(response.message || '重启失败')
    }
  } catch (error) {
    logger.error('重启Ollama失败:', error)
    ElMessage.error('重启失败')
  } finally {
    isRestarting.value = false
  }
}

const fetchCharacters = async () => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    const currentUserId = user.id || user.userId
    
    if (currentUserId) {
      const response = await request.get(`/api/character/list/user/${currentUserId}`)
      if (response.code === 200) {
        characters.value = response.data
        if (characters.value.length > 0) {
          selectCharacter(characters.value[0])
        }
      }
    } else {
      const response = await request.get('/api/character/list')
      if (response.code === 200) {
        characters.value = response.data
        if (characters.value.length > 0) {
          selectCharacter(characters.value[0])
        }
      }
    }
  } catch (error) {
    logger.error('获取角色列表失败:', error)
  }
}

const selectCharacter = (character) => {
  selectedCharacter.value = character
  messages.value = []
  lastAiMessageIndex.value = -1
  quickReplies.value = []
  refreshQuickReplies()
}

// 分享 / 取消分享自建角色到广场
const shareToPlaza = async (character) => {
  if (!character || !character.id) return
  const sharing = !character.isShared
  try {
    const url = sharing ? `/api/plaza/share/${character.id}` : `/api/plaza/unshare/${character.id}`
    const res = await request.post(url)
    if (res.code === 200) {
      character.isShared = sharing
      ElMessage.success(sharing ? '已分享到广场' : '已取消分享')
    } else {
      ElMessage.error(res.message || (sharing ? '分享失败' : '取消分享失败'))
    }
  } catch (error) {
    ElMessage.error((sharing ? '分享失败：' : '取消分享失败：') + (error.message || '请先登录'))
  }
}

const initWebSocket = () => {
  wsClient.value = new ChatWebSocketClient({
    url: `ws://${window.location.hostname}:8080/ws/chat`,
    userId: userId.value,
    reconnectInterval: 3000,
    maxReconnectAttempts: 3,
    onConnected: () => {
      logger.info('[ChatView] WebSocket连接成功')
    },
    onStart: (data) => {
      logger.info('[ChatView] AI开始回复')
    },
    onData: (data) => {
      logger.debug('[ChatView] 收到AI数据:', data.content)
      if (currentAiMessageIndex.value >= 0 && messages.value[currentAiMessageIndex.value]) {
        messages.value[currentAiMessageIndex.value].content += data.content
        scrollToBottom()
      }
    },
    onDone: (data) => {
      logger.info('[ChatView] AI回复完成')
      isSending.value = false
      currentAiMessageIndex.value = -1
      // AI回复完成后朗读回复内容
      if (data.content) {
        speechService.speak(data.content)
      }
    },
    onError: (error) => {
      logger.error('[ChatView] WebSocket错误:', error)
      if (currentAiMessageIndex.value >= 0 && messages.value[currentAiMessageIndex.value]) {
        if (messages.value[currentAiMessageIndex.value].content.trim() === '') {
          messages.value[currentAiMessageIndex.value].content = '[连接错误，请重试]'
        }
      }
      isSending.value = false
      currentAiMessageIndex.value = -1
    }
  })
  
  wsClient.value.connect()
}

// 初始化语音识别
const initSpeechRecognition = () => {
  speechRecognitionService.init()
  
  speechRecognitionService.onResult = (transcript, isFinal) => {
    if (isFinal) {
      inputMessage.value = transcript
      recognitionText.value = ''
    } else {
      recognitionText.value = transcript
    }
  }
  
  speechRecognitionService.onError = (error) => {
    logger.error('[ChatView] 语音识别错误:', error)
    isListening.value = false
    if (error !== 'no-speech') {
      ElMessage.error('语音识别失败，请重试')
    }
  }
  
  speechRecognitionService.onEnd = () => {
    isListening.value = false
    if (recognitionText.value) {
      inputMessage.value = recognitionText.value
      recognitionText.value = ''
    }
  }
}

// 切换语音输入
const toggleVoiceInput = () => {
  if (!speechRecognitionService.enabled) {
    ElMessage.warning('当前浏览器不支持语音识别')
    return
  }
  
  if (isListening.value) {
    speechRecognitionService.stop()
    isListening.value = false
  } else {
    // 停止当前朗读
    speechService.stop()
    speechRecognitionService.start()
    isListening.value = true
  }
}

// 切换语音输出
const toggleVoiceOutput = () => {
  speechService.setEnabled(!speechService.enabled)
  speechEnabled.value = speechService.enabled
  ElMessage.success(speechService.enabled ? '语音输出已开启' : '语音输出已关闭')
}

// ===== M4 ⑫：图片上传 + Ollama llava 视觉理解 =====
const imageInput = ref(null)
const visionUploading = ref(false)

const triggerImageUpload = () => {
  if (!selectedCharacter.value) return
  imageInput.value && imageInput.value.click()
}

const onImageSelected = async (e) => {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  e.target.value = '' // 允许重复选择同一文件
  visionUploading.value = true
  // 本地预览 URL（聊天气泡中直接展示）
  const imageUrl = URL.createObjectURL(file)
  messages.value.push({
    senderType: 'user',
    content: '[图片] ' + (file.name || ''),
    imageUrl,
    createTime: new Date().toISOString()
  })
  scrollToBottom()
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('userId', String(userId.value || ''))
    const res = await request.post('/api/vision/analyze', formData)
    if (res && res.code === 200 && res.data) {
      const desc = res.data.description || ''
      const reply = res.data.enabled === false
        ? '（视觉理解未启用）' + (res.data.message || '')
        : (res.data.success ? ('图片内容分析：' + desc) : ('图片分析失败：' + (res.data.message || '')))
      messages.value.push({
        senderType: 'ai',
        content: reply,
        createTime: new Date().toISOString()
      })
      lastAiMessageIndex.value = messages.value.length - 1
    } else {
      messages.value.push({
        senderType: 'ai',
        content: '图片分析失败：' + ((res && res.msg) || '未知错误'),
        createTime: new Date().toISOString()
      })
    }
  } catch (err) {
    logger.error('[视觉理解] 上传失败:', err)
    messages.value.push({
      senderType: 'ai',
      content: '图片上传失败，请确认后端视觉服务可用',
      createTime: new Date().toISOString()
    })
  } finally {
    visionUploading.value = false
    scrollToBottom()
  }
}

// ===== M4 ⑬：AI 回复语音气泡播放 =====
const voicePlayingIndex = ref(-1)
let voicePlayTimer = null

// 估算语音时长（中文约 4 字/秒）
const estimateVoiceSeconds = (content) => Math.max(1, Math.ceil(((content || '').length) / 4))

// 生成该消息的波形柱高（按内容字符串稳定种子）
const voiceBars = (content) => waveformHeights(18, seedFromString(content || ''), 4, 22)

const playAiVoice = (index) => {
  const msg = messages.value[index]
  if (!msg || !msg.content) return
  if (voicePlayingIndex.value === index) {
    speechService.stop()
    voicePlayingIndex.value = -1
    if (voicePlayTimer) { clearTimeout(voicePlayTimer); voicePlayTimer = null }
    return
  }
  voicePlayingIndex.value = index
  speechService.stop()
  speechService.speak(msg.content)
  if (voicePlayTimer) clearTimeout(voicePlayTimer)
  voicePlayTimer = setTimeout(() => { voicePlayingIndex.value = -1 }, estimateVoiceSeconds(msg.content) * 1000)
}

const sendMessage = async () => {
  // 用户开始输入时停止当前朗读
  speechService.stop()

  if (!inputMessage.value.trim() || !selectedCharacter.value || isSending.value) return

  const message = inputMessage.value.trim()
  inputMessage.value = ''
  isSending.value = true

  messages.value.push({
    senderType: 'user',
    content: message,
    createTime: new Date().toISOString()
  })
  scrollToBottom()

  currentAiMessageIndex.value = messages.value.push({
    senderType: 'ai',
    content: '',
    createTime: new Date().toISOString(),
    streaming: true
  }) - 1

  const sessionId = userId.value + '_' + selectedCharacter.value.id

  // 优先使用 SSE 流式接口（更可靠，不依赖 WebSocket 连接状态）
  // WebSocket 在本应用中连接不稳定（多次出现 readyState 判断后未发送的情况），
  // 改用 EventSource 直接消费 /api/chat/stream 流式响应。
  try {
    const params = new URLSearchParams({
      sessionId: sessionId,
      userId: String(userId.value),
      characterId: String(selectedCharacter.value.id),
      message: message
    })
    const token = localStorage.getItem('token')
    // SSE 不支持自定义 header，通过 query 参数传递 token（后端 SSE 端点已放行认证）
    if (token) {
      params.set('token', token)
    }
    const eventSource = new EventSource(
      `${window.location.protocol === 'https:' ? 'https' : 'http'}://${window.location.hostname}:8080/api/chat/stream?${params.toString()}`
    )

    eventSource.addEventListener('message', (event) => {
      if (messages.value[currentAiMessageIndex.value]) {
        messages.value[currentAiMessageIndex.value].content += event.data
        scrollToBottom()
      }
    })

    eventSource.addEventListener('error', (event) => {
      // 后端 emitter.complete() 会关闭连接，EventSource 触发 error。
      // 若已有内容则视为正常结束，否则才是真正的错误。
      const hasContent = messages.value[currentAiMessageIndex.value]?.content?.trim()
      eventSource.close()
      if (!hasContent) {
        logger.error('[ChatView] SSE错误:', event)
        if (messages.value[currentAiMessageIndex.value]) {
          messages.value[currentAiMessageIndex.value].content = '[获取回复失败，请重试]'
        }
      } else {
        // 正常结束，朗读回复
        speechService.speak(messages.value[currentAiMessageIndex.value].content)
      }
      finalizeAiMessage()
    })

    eventSource.addEventListener('done', () => {
      eventSource.close()
      // 朗读回复
      if (messages.value[currentAiMessageIndex.value]?.content) {
        speechService.speak(messages.value[currentAiMessageIndex.value].content)
      }
      finalizeAiMessage()
    })
  } catch (error) {
    logger.error('[ChatView] 发送消息失败:', error)
    if (messages.value[currentAiMessageIndex.value]) {
      messages.value[currentAiMessageIndex.value].content = '[发送失败，请重试]'
    }
    finalizeAiMessage()
  }
}

// AI 回复结束：关闭打字机光标、记录最后一条 AI 消息、刷新快捷回复建议
const finalizeAiMessage = () => {
  if (messages.value[currentAiMessageIndex.value]) {
    messages.value[currentAiMessageIndex.value].streaming = false
    lastAiMessageIndex.value = currentAiMessageIndex.value
  }
  refreshQuickReplies()
  isSending.value = false
  currentAiMessageIndex.value = -1
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageList.value) {
      messageList.value.scrollTop = messageList.value.scrollHeight
    }
  })
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('isAdmin')
  router.push('/login')
}

onMounted(() => {
  fetchCharacters()
  loadQuota()
  initWebSocket()
  speechService.init()
  initSpeechRecognition()
  window.addEventListener('replay-entrance', handleReplayEntrance)
})

onUnmounted(() => {
  if (wsClient.value) {
    wsClient.value.close()
  }
  speechService.stop()
  speechRecognitionService.stop()
  window.removeEventListener('replay-entrance', handleReplayEntrance)
})

watch(selectedCharacter, (newChar) => {
  messages.value = []
  lastAiMessageIndex.value = -1
  quickReplies.value = []
  // 新角色首次对话：展示角色 greeting 作为开场白
  if (newChar && !greetedCharacterIds.has(newChar.id)) {
    greetedCharacterIds.add(newChar.id)
    const greeting = buildGreetingMessage(newChar)
    if (greeting) {
      messages.value.push(greeting)
    }
  }
  refreshQuickReplies()
})
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  background: var(--bg-secondary);
  position: relative;
  overflow: hidden;
}

/* 用户端背景由全局 UserBgManager 提供（山云背景 + 切换 + 重播入口），此处不再维护局部背景层 */

.chat-sidebar {
  width: 300px;
  background: var(--sidebar-bg);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-md);
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.quota-info {
  margin: 12px 16px 0;
  padding: 8px 12px;
  font-size: 12px;
  border-radius: 8px;
  background: rgba(79, 70, 229, 0.12);
  color: var(--color-primary, #a5b4fc);
  text-align: center;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.logo-section h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-inverse);
  margin: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

.user-name {
  font-size: 14px;
  color: var(--text-secondary);
}

.section-title {
  padding: 16px 20px 8px;
}

.section-title span {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.character-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px;
}

.character-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  margin-bottom: 4px;
}

.character-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.character-item.active {
  background: rgba(79, 70, 229, 0.2);
}

.active-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: var(--color-primary);
  border-radius: 0 3px 3px 0;
}

.character-info {
  flex: 1;
  min-width: 0;
}

.shared-badge {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 400;
  color: #67c23a;
  border: 1px solid #67c23a;
  border-radius: 8px;
  vertical-align: middle;
}

.share-plaza-btn {
  flex-shrink: 0;
  font-size: 12px;
  padding: 2px 8px;
}
.share-plaza-btn.shared {
  color: #e6a23c;
}

.character-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.character-preview {
  font-size: 12px;
  color: var(--text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-actions {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
  /* 按钮区过长时自身可滚动，保证小屏/移动端“退出登录”等底部按钮可达 */
  max-height: 46vh;
  overflow-y: auto;
  scrollbar-width: thin;
}

.action-btn {
  justify-content: flex-start;
  padding-left: 12px;
  gap: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: none;
  color: var(--sidebar-text);
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.action-btn.logout {
  margin-top: 8px;
  background: rgba(239, 68, 68, 0.1);
  color: #fca5a5;
}

.action-btn.logout:hover {
  background: rgba(239, 68, 68, 0.2);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--card-bg);
}

.chat-header {
  padding: 16px 24px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--border-primary);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 灵魂引擎面板：与聊天区同宽对齐 */
.soul-panel-host {
  margin: 12px 24px 0;
  max-height: 260px;
  overflow: auto;
  flex-shrink: 0;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-text {
  display: flex;
  flex-direction: column;
}

.header-text .character-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-text .character-status {
  font-size: 12px;
  color: var(--color-success);
}

.header-actions .el-button {
  background: var(--bg-tertiary);
  border: none;
  color: var(--text-secondary);
}

.header-actions .el-button:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.ollama-status-btn.status-running {
  color: #22c55e;
}

.ollama-status-btn.status-running:hover {
  background: rgba(34, 197, 94, 0.1);
}

.ollama-status-btn.status-stopped {
  color: #ef4444;
}

.ollama-status-btn.status-stopped:hover {
  background: rgba(239, 68, 68, 0.1);
}

.voice-toggle-btn {
  color: var(--text-secondary);
}

.voice-toggle-btn.voice-enabled {
  color: #3b82f6;
}

.voice-toggle-btn.voice-enabled:hover {
  background: rgba(59, 130, 246, 0.1);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--bg-secondary);
}

.empty-state,
.welcome-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: 40px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.empty-state h3,
.welcome-state h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-state p,
.welcome-state p {
  font-size: 14px;
  color: var(--text-secondary);
  max-width: 300px;
}

.welcome-state h3 {
  margin-top: 16px;
  margin-bottom: 8px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-bubble {
  max-width: 70%;
  padding: 14px 18px;
  border-radius: 16px;
  position: relative;
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-item.ai .message-bubble {
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-bottom-left-radius: 4px;
  box-shadow: var(--shadow-sm);
}

.message-content {
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-time {
  font-size: 11px;
  opacity: 0.6;
  margin-top: 6px;
  text-align: right;
}

.message-item.user .message-time {
  color: rgba(255, 255, 255, 0.8);
}

.message-item.ai .message-time {
  color: var(--text-muted);
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 6px;
}
.pin-memory-btn {
  border: none;
  background: transparent;
  color: #909399;
  font-size: 11px;
  cursor: pointer;
  padding: 0 4px;
  border-radius: 4px;
}
.pin-memory-btn:hover {
  background: #ecf5ff;
  color: #409eff;
}

/* ===== 聊天体验升级样式 ===== */
.is-system-bubble .message-bubble {
  background: transparent;
  border: none;
  box-shadow: none;
  color: var(--text-muted);
  font-size: 12px;
  text-align: center;
}
.is-greeting-bubble .message-bubble {
  border-left: 3px solid var(--color-primary);
}
.deleted-placeholder {
  font-size: 12px;
  color: var(--text-muted);
  opacity: 0.7;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
}
.deleted-icon { font-size: 14px; }
.message-actions { margin-top: 4px; }
.delete-msg-btn { font-size: 11px; color: var(--text-muted); padding: 0; height: auto; }
.delete-msg-btn:hover { color: #f56c6c; }
.type-cursor {
  display: inline-block;
  margin-left: 1px;
  animation: blink-caret 1s steps(1) infinite;
  color: var(--color-primary);
}
@keyframes blink-caret { 50% { opacity: 0; } }
.quick-replies {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 24px 12px;
}
.quick-reply-chip {
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid var(--color-primary);
  background: var(--card-bg);
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.quick-reply-chip:hover {
  background: var(--color-primary);
  color: #fff;
}
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: var(--text-muted);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.chat-input-area {
  padding: 16px 24px 24px;
  background: var(--card-bg);
  border-top: 1px solid var(--border-primary);
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: center;
}

.input-container .el-input {
  flex: 1;
}

.input-container .el-input__wrapper {
  height: 48px;
  padding: 0 16px;
}

.input-container .el-input__inner {
  height: 100%;
  line-height: 48px;
}

.send-btn {
  width: 48px;
  height: 48px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  border: none;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
  transition: all 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.4);
}

.send-btn:disabled {
  background: var(--border-secondary);
  box-shadow: none;
}

.voice-btn {
  width: 48px;
  height: 48px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-secondary);
  transition: all 0.3s ease;
}

.voice-btn:hover:not(:disabled) {
  background: var(--bg-hover);
  border-color: var(--accent-primary);
  box-shadow: 0 0 12px rgba(79, 70, 229, 0.3);
}

.voice-btn.listening {
  background: linear-gradient(135deg, #ef4444, #f97316);
  border-color: #ef4444;
  animation: pulse-voice 1.5s ease-in-out infinite;
}

.voice-btn.listening svg {
  stroke: white;
}

@keyframes pulse-voice {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4);
  }
  50% {
    box-shadow: 0 0 0 12px rgba(239, 68, 68, 0);
  }
}

.voice-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.voice-indicator .pulse {
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
  animation: pulse-dot 1s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.8);
  }
}

.sidebar-overlay {
  display: none;
}

.mobile-header {
  display: none;
}

@media (max-width: 768px) {
  .chat-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    width: 280px;
    z-index: 1000;
    transition: transform 0.3s ease;
  }

  .chat-sidebar.collapsed {
    transform: translateX(-100%);
  }

  .sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 999;
  }

  .mobile-header {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    background: var(--card-bg);
    border-bottom: 1px solid var(--border-primary);
  }

  .menu-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    background: var(--bg-tertiary);
    border: none;
    border-radius: 8px;
    cursor: pointer;
    color: var(--text-primary);
  }

  .menu-toggle:hover {
    background: var(--bg-hover);
  }

  .chat-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  .chat-header {
    padding: 12px 16px;
  }

  .header-info {
    gap: 10px;
  }

  .header-text .character-name {
    font-size: 14px;
  }

  .message-list {
    padding: 16px;
  }

  .message-bubble {
    max-width: 85%;
    padding: 12px 14px;
  }

  .message-content {
    font-size: 14px;
  }

  .chat-input-area {
    padding: 12px 16px 16px;
  }

  .input-container {
    gap: 8px;
  }

  .send-btn {
    width: 44px;
    height: 44px;
  }

  .empty-state,
  .welcome-state {
    padding: 24px;
  }

  .empty-icon {
    font-size: 48px;
  }

  .empty-state h3,
  .welcome-state h3 {
    font-size: 18px;
  }
}

@media (max-width: 480px) {
  .sidebar-actions {
    padding: 12px;
  }

  .action-btn {
    padding: 10px 12px;
    font-size: 13px;
  }

  .character-list {
    padding: 0 8px;
  }

  .character-item {
    padding: 10px;
    gap: 10px;
  }

  .character-avatar {
    width: 40px;
    height: 40px;
  }

  .sidebar-header {
    padding: 16px;
  }

  .message-list {
    padding: 12px;
  }

  .message-item {
    margin-bottom: 16px;
  }

  .message-bubble {
    max-width: 90%;
    padding: 10px 12px;
  }
}

.input-hint {
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}
.chat-image-preview {
  max-width: 220px;
  max-height: 220px;
  border-radius: 10px;
  margin-bottom: 6px;
  display: block;
  object-fit: cover;
}

.voice-bubble {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  margin: 4px 0;
  border-radius: 18px;
  background: rgba(79, 70, 229, 0.12);
  cursor: pointer;
  user-select: none;
  max-width: 220px;
}

.voice-bubble:hover {
  background: rgba(79, 70, 229, 0.2);
}

.voice-bubble.playing {
  background: rgba(79, 70, 229, 0.28);
}

.voice-play-icon {
  font-size: 12px;
  color: #4f46e5;
}

.voice-wave {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 22px;
}

.voice-wave i {
  display: inline-block;
  width: 2px;
  border-radius: 2px;
  background: #6366f1;
}

.voice-bubble.playing .voice-wave i {
  background: #4f46e5;
}

.voice-duration {
  font-size: 12px;
  color: #6366f1;
}
</style>