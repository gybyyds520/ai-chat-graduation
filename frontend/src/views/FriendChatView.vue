<template>
  <div class="friend-chat-container">
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>好友聊天</h3>
      </div>
      
      <div class="friend-list">
        <div v-if="friends.length === 0" class="empty-state">
          暂无好友
        </div>
        <div 
          v-for="friend in friends" 
          :key="getFriendId(friend)"
          :class="['friend-item', { active: selectedFriendId === getFriendId(friend) }]"
          @click="selectFriend(friend)"
        >
          <div class="friend-avatar">
            <img :src="getFriendAvatar(friend)" alt="">
            <div :class="['online-status', getFriendStatus(getFriendId(friend))]"></div>
          </div>
          <div class="friend-info">
            <div class="friend-name">{{ getFriendName(friend) }}</div>
            <div class="friend-last-message" v-if="getLastMessage(friend)">
              {{ formatMessagePreview(getLastMessage(friend)) }}
            </div>
          </div>
          <div class="friend-status-text">{{ getFriendStatusText(getFriendId(friend)) }}</div>
        </div>
      </div>

      <div class="nav-buttons">
        <el-button @click="goToFriends" class="nav-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          好友管理
        </el-button>
        <el-button @click="goToAiChat" class="nav-btn">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          AI聊天
        </el-button>
        <el-button @click="handleLogout" class="nav-btn logout">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
            <polyline points="16 17 21 12 16 7"/>
            <line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          退出登录
        </el-button>
      </div>
    </div>

    <div class="chat-area" v-if="selectedFriend">
      <div class="chat-header">
        <div class="header-avatar">
          <img :src="getFriendAvatar(selectedFriend)" alt="">
          <div :class="['online-status', getFriendStatus(selectedFriendId)]"></div>
        </div>
        <div class="header-info">
          <div class="header-name">{{ getFriendName(selectedFriend) }}</div>
          <div class="header-status">{{ getFriendStatusText(selectedFriendId) }}</div>
        </div>
        <div class="header-actions">
          <button 
            class="action-btn share-btn" 
            @click.stop="handleShareClick"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12l7-7 7 7"/>
            </svg>
            <span>分享</span>
          </button>
          <button 
            class="action-btn import-btn" 
            @click.stop="handleImportClick"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
            <span>导入</span>
          </button>
        </div>
      </div>

      <div class="message-list" ref="messageList" @scroll="handleScroll">
        <div 
          v-for="(message, index) in messages" 
          :key="message.id || index" 
          :class="['message-item', message.senderId === userId ? 'user' : 'friend']"
        >
          <div class="message-avatar">
            <img :src="message.senderId === userId ? getUserAvatar(userId) : getFriendAvatar(selectedFriend)" alt="">
          </div>
          <div class="message-content">
            <div v-if="message.messageType === 'recall' || message.type === 'recall'" class="recalled-placeholder">
              消息已撤回
            </div>
            <template v-else>
            <div class="message-text" v-if="message.messageType === 'text' || !message.messageType">
              <span v-html="renderEmoji(message.content)"></span>
            </div>
            <div class="message-emoji" v-else-if="message.messageType === 'emoji'">
              <span class="emoji-large">{{ message.content }}</span>
            </div>
            <div class="message-file" v-else-if="message.messageType === 'file' || message.type === 'file'">
              <div class="file-icon">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                  <polyline points="10 9 9 9 8 9"/>
                </svg>
              </div>
              <div class="file-info">
                <div class="file-name">{{ message.fileName }}</div>
                <div class="file-size">{{ formatFileSize(message.fileSize) }}</div>
              </div>
              <a :href="message.filePath" download class="download-btn">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
              </a>
            </div>
            <div class="message-time">
              {{ formatTime(message.createTime) }}
              <button
                v-if="message.senderId === userId && canRecall(message)"
                class="recall-btn"
                @click.stop="recallMessage(message)"
              >撤回</button>
              <span v-if="message.senderId !== userId && message.isRead" class="read-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="#4f46e5" stroke="none">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
              </span>
              <span v-if="message.senderId !== userId && !message.isRead" class="unread-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" stroke-width="2">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
                </svg>
              </span>
            </div>
            </template>
          </div>
        </div>
        
        <div v-if="isTyping" class="typing-indicator">
          <div class="typing-dots">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
          <span class="typing-text">{{ getFriendName(selectedFriend) }} 正在输入...</span>
        </div>
      </div>

      <div class="chat-input">
        <button class="input-action-btn" @click="triggerFileUpload">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
        </button>
        <button class="input-action-btn" @click="toggleEmojiPicker">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M8 14s1.5-2 4-2 4 2 4 2"/>
            <line x1="9" y1="9" x2="9.01" y2="9"/>
            <line x1="15" y1="9" x2="15.01" y2="9"/>
          </svg>
        </button>
        <el-input
          v-model="inputMessage"
          placeholder="输入消息..."
          @keyup.enter="sendMessage"
          @input="handleInput"
        />
        <el-button type="primary" @click="sendMessage">发送</el-button>
      </div>

      <div v-if="showEmojiPicker" class="emoji-picker">
        <div class="emoji-category">
          <span class="category-title">常用表情</span>
          <div class="emoji-grid">
            <span 
              v-for="emoji in commonEmojis" 
              :key="emoji" 
              class="emoji-item"
              @click="selectEmoji(emoji)"
            >{{ emoji }}</span>
          </div>
        </div>
        <div class="emoji-category">
          <span class="category-title">表情符号</span>
          <div class="emoji-grid">
            <span 
              v-for="emoji in emojiEmoticons" 
              :key="emoji" 
              class="emoji-item"
              @click="selectEmoji(emoji)"
            >{{ emoji }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-area-empty" v-else>
      <div class="empty-content">
        <p>选择一个好友开始聊天</p>
      </div>
    </div>

    <el-dialog v-model="showShareModal" title="分享AI角色与聊天记录" width="600px">
      <div class="share-options">
        <div class="share-option">
          <span class="option-label">选择要分享的AI角色</span>
          <el-select v-model="shareOptions.characterId" placeholder="请选择AI角色" style="width: 100%">
            <el-option v-for="char in availableCharacters" :key="char.id" :label="char.name" :value="char.id" />
          </el-select>
        </div>
        <div class="share-option">
          <span class="option-label">分享方式</span>
          <el-radio-group v-model="shareOptions.mode" class="radio-group">
            <el-radio label="direct">直接发送给好友</el-radio>
            <el-radio label="export">导出文件</el-radio>
          </el-radio-group>
        </div>
        <div class="share-option">
          <el-checkbox v-model="shareOptions.includeChatHistory">包含聊天记录</el-checkbox>
        </div>
        <div class="share-option" v-if="shareOptions.includeChatHistory">
          <el-input-number v-model="shareOptions.maxMessages" :min="10" :max="1000" label="最大消息数" />
        </div>
        <div class="share-option">
          <el-checkbox v-model="shareOptions.anonymize">匿名化发送者信息</el-checkbox>
        </div>
      </div>
      <template #footer>
        <el-button @click="showShareModal = false">取消</el-button>
        <el-button type="primary" @click="handleShare" :disabled="!shareOptions.characterId">
          {{ shareOptions.mode === 'direct' ? '直接发送' : '导出文件' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showImportModal" title="导入AI角色与聊天记录" width="500px">
      <div class="import-options">
        <div class="import-option">
          <el-upload
            :show-file-list="false"
            :on-change="handleFileSelect"
            :before-upload="handleImportFile"
            accept=".json,.json.gz,.aichat"
            class="upload-btn"
          >
            <el-button type="primary">选择文件</el-button>
          </el-upload>
          <span v-if="importFileName" class="file-name">{{ importFileName }}</span>
        </div>
        <div class="import-option" v-if="importOptions.needPassword">
          <el-input v-model="importOptions.password" type="password" placeholder="输入加密密码" />
        </div>
        <div class="import-option">
          <el-checkbox v-model="importOptions.mergeChat">合并到当前聊天</el-checkbox>
        </div>
      </div>
      <template #footer>
        <el-button @click="showImportModal = false">取消</el-button>
        <el-button type="primary" @click="confirmImport" :disabled="!importFile">确认导入</el-button>
      </template>
    </el-dialog>

    <input 
      ref="fileInputRef"
      type="file" 
      multiple 
      class="hidden-file-input" 
      @change="handleFileSelectForSend"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateAvatarUrl } from '@/utils/localAvatar'
import { shareManager } from '@/utils/shareManager'
import ChatWebSocketClient from '@/utils/websocket'

const router = useRouter()

// 修复：token 现为 JWT 字符串，parseInt 会得到 NaN。从 user 对象取 id。
const userId = ref(JSON.parse(localStorage.getItem('user') || '{}').id)
const friends = ref([])
const selectedFriendId = ref(null)
const selectedFriend = ref(null)
const messages = ref([])
const inputMessage = ref('')
const messageList = ref(null)
const lastMessages = ref({})
const route = useRoute()
const fileInputRef = ref(null)

const showShareModal = ref(false)
const showImportModal = ref(false)
const showEmojiPicker = ref(false)

const availableCharacters = ref([])
const friendStatuses = ref({})
const isTyping = ref(false)
const typingTimeout = ref(null)
const unreadDot = ref(false)

const wsClient = ref(null)

const commonEmojis = ['😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗', '😚', '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '🤐', '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '😮‍💨', '🤥', '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕', '🤢', '🤮', '🤧', '🥵', '🥶', '🥴', '😵', '🤯', '🤠', '🥳', '🥸', '😎', '🤓', '🧐', '😕', '😟', '🙁', '☹️', '😮', '😯', '😲', '😳', '🥺', '😦', '😧', '😨', '😰', '😥', '😢', '😭', '😱', '😖', '😣', '😞', '😓', '😩', '😫', '🥱', '😤', '😡', '😠', '🤬', '😈', '👿', '💀', '☠️']
const emojiEmoticons = ['👍', '👎', '❤️', '🔥', '🎉', '💯', '✨', '🌟', '⭐', '💪', '👏', '🙌', '🙏', '👋', '🤝', '✌️', '🤞', '🤟', '🤘', '👌', '👈', '👉', '👆', '👇', '☝️', '✋', '🤚', '🖐️', '🖖', '👋', '🤙', '💪', '🦾', '🦵', '🦿', '🦶', '👄', '👅', '👂', '👃', '👁️', '👀', '👤', '👥', '👦', '👧', '👨', '👩', '👴', '👵', '👶', '👼', '🤰', '🤱', '👸', '🤴', '🧟', '🧛', '🧞', '🧝', '🧙', '🧚', '🧜', '🧞']

const shareOptions = ref({
  characterId: null,
  mode: 'direct',
  includeChatHistory: true,
  maxMessages: 100,
  anonymize: false
})

const importOptions = ref({
  needPassword: false,
  password: '',
  mergeChat: false
})

const importFile = ref(null)
const importFileName = ref('')

const initWebSocket = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  const currentUserId = user.id || user.userId
  
  wsClient.value = new ChatWebSocketClient({
    url: `ws://${window.location.hostname}:8080/ws/chat`,
    userId: currentUserId,
    reconnectInterval: 3000,
    maxReconnectAttempts: 3,
    onMessage: (data) => {
      handleWebSocketMessage(data)
    },
    onError: (error) => {
      console.error('[FriendChatView] WebSocket错误:', error)
    }
  })
  
  wsClient.value.connect()
}

const handleWebSocketMessage = (data) => {
  if (data.type === 'friend_message') {
    if (data.senderId === selectedFriendId.value) {
      messages.value.push(data)
      lastMessages.value[data.senderId] = data
      scrollToBottom()
    }
  } else if (data.type === 'typing') {
    if (data.userId === selectedFriendId.value) {
      isTyping.value = data.isTyping
      if (data.isTyping) {
        if (typingTimeout.value) clearTimeout(typingTimeout.value)
        typingTimeout.value = setTimeout(() => {
          isTyping.value = false
        }, 3000)
      }
    }
  } else if (data.type === 'read_receipt') {
    const msgIndex = messages.value.findIndex(m => m.id === data.messageId)
    if (msgIndex !== -1) {
      messages.value[msgIndex].isRead = true
    }
  } else if (data.type === 'message_recalled') {
    // M1 ④：对方撤回了某条消息，本地更新为占位
    const idx = messages.value.findIndex(m => m.id === data.messageId)
    if (idx !== -1) {
      messages.value[idx].content = '消息已撤回'
      messages.value[idx].messageType = 'recall'
      messages.value[idx].type = 'recall'
    }
  } else if (data.type === 'elf_reminder' || data.type === 'proactive_message') {
    // M1 ②：收到精灵主动消息/定时提醒 → 桌面通知 + 红点
    notifyUser(data.title || '精灵提醒', data.content || data.message || '')
    unreadDot.value = true
  } else if (data.type === 'user_status') {
    friendStatuses.value[data.userId] = data.status
  }
}

const loadFriends = async () => {
  try {
    const response = await request.get(`/api/friend/list?userId=${userId.value}`)
    if (response.code === 200) {
      friends.value = response.data
      
      for (const friend of friends.value) {
        const friendId = getFriendId(friend)
        const historyResponse = await request.get(`/api/friend/chat/history?userId=${userId.value}&friendId=${friendId}&limit=1`)
        if (historyResponse.code === 200 && historyResponse.data.length > 0) {
          lastMessages.value[friendId] = historyResponse.data[0]
        }
      }
      
      if (route.query.friendId && friends.value.length > 0) {
        const friendId = parseInt(route.query.friendId)
        const friend = friends.value.find(f => getFriendId(f) === friendId)
        if (friend) {
          await selectFriend(friend)
        }
      }
    }
  } catch (error) {
    console.error('加载好友列表失败:', error)
  }
}

const loadFriendStatuses = async () => {
  try {
    const friendIds = friends.value.map(f => getFriendId(f))
    const response = await request.get(`/api/user/status?userIds=${friendIds.join(',')}`)
    if (response.code === 200) {
      friendStatuses.value = response.data
    }
  } catch (error) {
    console.error('加载好友状态失败:', error)
  }
}

const loadCharacters = async () => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    const userId = user.id || user.userId
    
    if (userId) {
      const response = await request.get(`/api/character/list/user/${userId}`)
      if (response.code === 200) {
        availableCharacters.value = response.data
      }
    } else {
      const response = await request.get('/api/character/list')
      if (response.code === 200) {
        availableCharacters.value = response.data.filter(c => c.isSystem)
      }
    }
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
}

const selectFriend = async (friend) => {
  selectedFriend.value = friend
  selectedFriendId.value = getFriendId(friend)
  await loadMessages()
  markAsRead()
}

const loadMessages = async () => {
  if (!selectedFriendId.value) return
  
  try {
    const response = await request.get(`/api/friend/chat/history?userId=${userId.value}&friendId=${selectedFriendId.value}&limit=50`)
    if (response.code === 200) {
      messages.value = response.data
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载聊天记录失败:', error)
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !selectedFriendId.value) return
  
  const content = inputMessage.value.trim()
  inputMessage.value = ''
  
  if (wsClient.value && wsClient.value.isConnected) {
    wsClient.value.send({
      type: 'friend_message',
      receiverId: selectedFriendId.value,
      content: content,
      messageType: 'text'
    })
    
    messages.value.push({
      id: Date.now(),
      senderId: userId.value,
      senderName: '我',
      receiverId: selectedFriendId.value,
      content: content,
      messageType: 'text',
      createTime: new Date().toISOString(),
      isRead: false
    })
    lastMessages.value[selectedFriendId.value] = messages.value[messages.value.length - 1]
    scrollToBottom()
  } else {
    try {
      const response = await request.post('/api/friend/chat/send', {
        senderId: userId.value,
        receiverId: selectedFriendId.value,
        content: content,
        type: 'text'
      })
      
      if (response.code === 200) {
        messages.value.push(response.data)
        lastMessages.value[selectedFriendId.value] = response.data
        scrollToBottom()
      } else {
        ElMessage.error(response.message || '发送失败')
      }
    } catch (error) {
      ElMessage.error('发送失败')
    }
  }
}

const handleInput = () => {
  if (inputMessage.value.length > 0 && selectedFriendId.value) {
    if (wsClient.value && wsClient.value.isConnected) {
      wsClient.value.send({
        type: 'typing',
        targetId: selectedFriendId.value,
        isTyping: true
      })
    }
    
    if (typingTimeout.value) clearTimeout(typingTimeout.value)
    typingTimeout.value = setTimeout(() => {
      if (wsClient.value && wsClient.value.isConnected) {
        wsClient.value.send({
          type: 'typing',
          targetId: selectedFriendId.value,
          isTyping: false
        })
      }
    }, 1000)
  }
}

const markAsRead = () => {
  if (selectedFriendId.value && wsClient.value && wsClient.value.isConnected) {
    wsClient.value.send({
      type: 'read',
      senderId: selectedFriendId.value
    })
  }
}

// M1 ④：撤回时间窗 2 分钟
const canRecall = (message) => {
  if (!message.createTime) return false
  const created = new Date(message.createTime).getTime()
  return Date.now() - created < 2 * 60 * 1000
}

const recallMessage = async (message) => {
  if (!message.id) {
    ElMessage.warning('该消息暂不支持撤回')
    return
  }
  try {
    const res = await request.put(`/api/friend/chat/message/${message.id}/recall`)
    if (res.code === 200) {
      message.content = '消息已撤回'
      message.messageType = 'recall'
      message.type = 'recall'
      ElMessage.success('已撤回')
    } else {
      ElMessage.error(res.message || '撤回失败')
    }
  } catch (e) {
    ElMessage.error('撤回失败')
  }
}

// M1 ②：桌面通知（浏览器 Notification），失败时退化为 ElMessage
const notifyUser = (title, body) => {
  try {
    if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
      new Notification(title, { body })
    } else if (typeof Notification !== 'undefined' && Notification.permission !== 'denied') {
      Notification.requestPermission().then(p => {
        if (p === 'granted') new Notification(title, { body })
      })
    } else {
      ElMessage.info(`${title}：${body}`)
    }
  } catch (e) {
    ElMessage.info(`${title}：${body}`)
  }
}

const sendFile = async (file) => {
  if (!selectedFriendId.value) return
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('senderId', userId.value)
    formData.append('receiverId', selectedFriendId.value)
    
    const response = await request.post('/api/friend/chat/send/file', formData)
    
    if (response.code === 200) {
      messages.value.push(response.data)
      lastMessages.value[selectedFriendId.value] = response.data
      scrollToBottom()
      ElMessage.success('文件发送成功')
    } else {
      ElMessage.error(response.message || '文件发送失败')
    }
  } catch (error) {
    ElMessage.error('文件发送失败')
  }
}

const triggerFileUpload = () => {
  fileInputRef.value?.click()
}

const handleFileSelectForSend = async (event) => {
  const files = Array.from(event.target.files)
  
  for (const file of files) {
    await sendFile(file)
  }
  
  event.target.value = ''
}

const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

const selectEmoji = async (emoji) => {
  inputMessage.value += emoji
  showEmojiPicker.value = false
  
  if (selectedFriendId.value) {
    if (wsClient.value && wsClient.value.isConnected) {
      wsClient.value.send({
        type: 'friend_message',
        receiverId: selectedFriendId.value,
        content: emoji,
        messageType: 'emoji'
      })
      
      messages.value.push({
        id: Date.now(),
        senderId: userId.value,
        senderName: '我',
        receiverId: selectedFriendId.value,
        content: emoji,
        messageType: 'emoji',
        createTime: new Date().toISOString(),
        isRead: false
      })
      lastMessages.value[selectedFriendId.value] = messages.value[messages.value.length - 1]
      scrollToBottom()
    } else {
      try {
        const response = await request.post('/api/friend/chat/send', {
          senderId: userId.value,
          receiverId: selectedFriendId.value,
          content: emoji,
          type: 'emoji'
        })
        
        if (response.code === 200) {
          messages.value.push(response.data)
          lastMessages.value[selectedFriendId.value] = response.data
          scrollToBottom()
        }
      } catch (error) {
        console.error('发送表情失败:', error)
      }
    }
  }
}

const escapeHtml = (text) => {
  return text
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

const renderEmoji = (text) => {
  if (!text) return ''
  // 安全修复（P0）：先 HTML 转义再替换表情符号，杜绝存储型 XSS
  const safe = escapeHtml(text)
  return safe.replace(/:(\w+):/g, (match, name) => {
    const emojiMap = {
      smile: '😊', happy: '😄', sad: '😢', angry: '😠', love: '❤️',
      ok: '👍', no: '👎', fire: '🔥', star: '⭐', party: '🎉',
      cool: '😎', confused: '😕', cry: '😭', laugh: '😂', wink: '😉'
    }
    return emojiMap[name] || match
  })
}

const getFriendId = (friend) => {
  return friend.userId === userId.value ? friend.friendId : friend.userId
}

const getFriendName = (friend) => {
  return friend.username || '好友'
}

const getFriendAvatar = (friend) => {
  const friendId = getFriendId(friend)
  return generateAvatarUrl(friendId.toString())
}

const getUserAvatar = (uid) => {
  return generateAvatarUrl(uid.toString())
}

const getFriendStatus = (friendId) => {
  return friendStatuses.value[friendId] || 'offline'
}

const getFriendStatusText = (friendId) => {
  const status = getFriendStatus(friendId)
  const statusMap = {
    online: '在线',
    offline: '离线',
    away: '离开',
    busy: '忙碌',
    invisible: '隐身'
  }
  return statusMap[status] || '离线'
}

const getLastMessage = (friend) => {
  const friendId = getFriendId(friend)
  return lastMessages.value[friendId]
}

const formatMessagePreview = (message) => {
  if (!message) return ''
  if (message.type === 'file' || message.messageType === 'file') {
    return '[文件] ' + message.fileName
  }
  if (message.messageType === 'emoji') {
    return '[表情]'
  }
  return message.content?.substring(0, 30) + (message.content?.length > 30 ? '...' : '')
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatFileSize = (size) => {
  if (!size) return ''
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageList.value) {
      messageList.value.scrollTop = messageList.value.scrollHeight
    }
  })
}

const handleScroll = () => {
  if (messageList.value) {
    const { scrollTop, scrollHeight, clientHeight } = messageList.value
    if (scrollHeight - scrollTop - clientHeight < 100) {
      markAsRead()
    }
  }
}

const goToFriends = () => {
  router.push('/friends')
}

const goToAiChat = () => {
  router.push('/chat')
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  if (wsClient.value) {
    wsClient.value.destroy()
  }
  router.push('/login')
}

const handleShareClick = () => {
  showShareModal.value = true
}

const handleImportClick = () => {
  showImportModal.value = true
}

const handleShare = async () => {
  try {
    const options = shareOptions.value

    const character = availableCharacters.value.find(c => c.id === options.characterId)
    
    let shareData = {
      type: 'character',
      character: character,
      messages: options.includeChatHistory ? messages.value.slice(-options.maxMessages) : [],
      anonymize: options.anonymize
    }

    if (options.mode === 'direct') {
      const response = await request.post('/api/friend/chat/share', {
        senderId: userId.value,
        receiverId: selectedFriendId.value,
        data: shareData
      })
      
      if (response.code === 200) {
        ElMessage.success('分享内容已发送给好友')
      } else {
        ElMessage.error(response.message || '分享失败')
      }
    } else {
      const filename = `aichat-share-${character?.name || 'character'}-${new Date().toISOString().split('T')[0]}`
      shareManager.exportToFile(shareData, filename, 'json', '')
      ElMessage.success('文件已导出')
    }
    
    showShareModal.value = false
  } catch (error) {
    ElMessage.error('分享失败')
  }
}

const handleFileSelect = (file) => {
  importFile.value = file
  importFileName.value = file.name
}

const handleImportFile = () => {
  return false
}

const confirmImport = async () => {
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    formData.append('password', importOptions.value.password)
    formData.append('mergeChat', importOptions.value.mergeChat ? 'true' : 'false')
    formData.append('userId', userId.value)
    formData.append('friendId', selectedFriendId.value)
    
    const response = await request.post('/api/friend/chat/import', formData)
    
    if (response.code === 200) {
      ElMessage.success('导入成功')
      showImportModal.value = false
      await loadMessages()
    } else {
      ElMessage.error(response.message || '导入失败')
    }
  } catch (error) {
    ElMessage.error('导入失败')
  }
}

watch(selectedFriendId, () => {
  isTyping.value = false
  showEmojiPicker.value = false
})

onMounted(() => {
  initWebSocket()
  loadFriends()
  loadFriendStatuses()
  loadCharacters()
})

onUnmounted(() => {
  if (wsClient.value) {
    wsClient.value.destroy()
  }
  if (typingTimeout.value) {
    clearTimeout(typingTimeout.value)
  }
})
</script>

<style scoped>
.friend-chat-container {
  display: flex;
  height: 100vh;
  background: var(--bg-primary);
}

.sidebar {
  width: 300px;
  background: var(--card-bg);
  border-right: 1px solid var(--border-primary);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-primary);
}

.sidebar-header h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
}

.friend-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.empty-state {
  text-align: center;
  color: var(--text-secondary);
  padding: 40px;
}

.friend-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;
}

.friend-item:hover {
  background: var(--bg-hover);
}

.friend-item.active {
  background: var(--color-primary);
}

.friend-item.active .friend-name,
.friend-item.active .friend-last-message,
.friend-item.active .friend-status-text {
  color: white;
}

.friend-avatar {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
}

.friend-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.online-status {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--card-bg);
}

.online-status.online {
  background: #22c55e;
}

.online-status.offline {
  background: #6b7280;
}

.online-status.away {
  background: #f59e0b;
}

.online-status.busy {
  background: #ef4444;
}

.online-status.invisible {
  background: #6b7280;
}

.friend-info {
  flex: 1;
  overflow: hidden;
}

.friend-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.friend-last-message {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.friend-status-text {
  font-size: 11px;
  color: var(--text-secondary);
}

.nav-buttons {
  padding: 10px;
  border-top: 1px solid var(--border-primary);
}

.nav-btn {
  width: 100%;
  margin-bottom: 8px;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--border-primary);
}

.header-avatar {
  position: relative;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
}

.header-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.header-info {
  flex: 1;
}

.header-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

.header-status {
  font-size: 12px;
  color: var(--text-secondary);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: var(--bg-hover);
  border: none;
  border-radius: 6px;
  color: var(--text-primary);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: var(--border-primary);
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content {
  max-width: 60%;
  margin: 0 12px;
}

.message-text {
  background: var(--card-bg);
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-primary);
  word-break: break-word;
}

.message-item.user .message-text {
  background: var(--color-primary);
  color: white;
}

.message-emoji {
  background: transparent;
}

.emoji-large {
  font-size: 48px;
}

.message-file {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--card-bg);
  padding: 12px 16px;
  border-radius: 12px;
}

.message-item.user .message-file {
  background: var(--color-primary);
}

.file-icon {
  color: var(--color-primary);
}

.message-item.user .file-icon {
  color: white;
}

.file-info {
  flex: 1;
}

.file-name {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.message-item.user .file-name {
  color: white;
}

.file-size {
  font-size: 12px;
  color: var(--text-secondary);
}

.message-item.user .file-size {
  color: rgba(255, 255, 255, 0.7);
}

.download-btn {
  color: var(--color-primary);
}

.message-item.user .download-btn {
  color: white;
}

.message-time {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.message-item.user .message-time {
  justify-content: flex-end;
}

.read-icon, .unread-icon {
  margin-left: 4px;
}

.recall-btn {
  margin-left: 8px;
  font-size: 11px;
  color: #ef4444;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0 4px;
}

.recall-btn:hover {
  text-decoration: underline;
}

.recalled-placeholder {
  font-size: 12px;
  color: var(--text-secondary);
  text-align: center;
  padding: 8px 12px;
  font-style: italic;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--card-bg);
  border-radius: 12px;
  margin-bottom: 12px;
  width: fit-content;
}

.typing-dots {
  display: flex;
  gap: 4px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-secondary);
  animation: typing 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.typing-text {
  font-size: 13px;
  color: var(--text-secondary);
}

.chat-input {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--card-bg);
  border-top: 1px solid var(--border-primary);
}

.input-action-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-hover);
  border: none;
  border-radius: 8px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s;
}

.input-action-btn:hover {
  background: var(--border-primary);
}

.chat-input .el-input {
  flex: 1;
}

.emoji-picker {
  position: absolute;
  bottom: 80px;
  left: 300px;
  right: 0;
  background: var(--card-bg);
  border-top: 1px solid var(--border-primary);
  padding: 20px;
  max-height: 300px;
  overflow-y: auto;
}

.emoji-category {
  margin-bottom: 20px;
}

.category-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 12px;
  display: block;
}

.emoji-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.emoji-item {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.emoji-item:hover {
  background: var(--bg-hover);
}

.chat-area-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-content p {
  color: var(--text-secondary);
  font-size: 16px;
}

.hidden-file-input {
  display: none;
}

.share-options, .import-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.share-option, .import-option {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.radio-group {
  display: flex;
  gap: 16px;
}
</style>