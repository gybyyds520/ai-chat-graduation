<template>
  <div class="page chat-detail-page" :class="{ 'dark': isDark }">
    <!-- 消息列表 -->
    <div class="message-list" ref="messageList">
      <div 
        v-for="(msg, index) in messages" 
        :key="index" 
        class="message-item"
        :class="{ 'is-self': msg.isSelf }"
      >
        <div class="avatar-wrap">
          <span class="avatar">{{ msg.isSelf ? '👤' : '🤖' }}</span>
        </div>
        <div class="message-content">
          <div class="message-bubble">
            <span>{{ msg.content }}</span>
          </div>
          <span class="message-time">{{ msg.time }}</span>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <div class="input-wrap">
        <input 
          type="text" 
          class="message-input" 
          placeholder="输入消息..." 
          v-model="inputText"
          @keyup.enter="sendMessage"
        />
        <button class="send-btn" @click="sendMessage">发送</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, nextTick, onMounted, onUnmounted } from 'vue';
import mpLogger from '@/utils/miniprogramLogger.js';

const isDark = inject('isDark', false);
const inputText = ref('');
const messageList = ref(null);

const messages = ref([
  { id: 1, content: '你好！我是AI助手，有什么可以帮助你的？', isSelf: false, time: '10:00' },
  { id: 2, content: '你好！我想了解一下AI技术。', isSelf: true, time: '10:01' },
  { id: 3, content: '当然可以！AI是人工智能的缩写，它涵盖了机器学习、深度学习、自然语言处理等多个领域...', isSelf: false, time: '10:02' },
  { id: 4, content: '能具体说说深度学习吗？', isSelf: true, time: '10:03' },
  { id: 5, content: '深度学习是机器学习的一个分支，它使用多层神经网络来模拟人脑的学习过程...', isSelf: false, time: '10:05' },
]);

const sendMessage = () => {
  if (!inputText.value.trim()) return;
  
  const newMessage = {
    id: Date.now(),
    content: inputText.value,
    isSelf: true,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  };
  
  messages.value.push(newMessage);
  inputText.value = '';
  
  mpLogger.info('Message', 'Message sent', {
    content: newMessage.content,
    time: newMessage.time,
    messageCount: messages.value.length
  });
  
  scrollToBottom();
  
  // 模拟AI回复
  setTimeout(() => {
    const reply = {
      id: Date.now() + 1,
      content: '收到你的消息！我正在处理...这是一个模拟回复。',
      isSelf: false,
      time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    };
    
    messages.value.push(reply);
    
    mpLogger.info('Message', 'AI reply received', {
      content: reply.content,
      messageCount: messages.value.length
    });
    
    scrollToBottom();
  }, 1500);
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messageList.value) {
      messageList.value.scrollTop = messageList.value.scrollHeight;
    }
  });
};

onMounted(() => {
  mpLogger.info('PageLifecycle', 'ChatDetailPage mounted', {
    timestamp: new Date().toISOString(),
    initialMessageCount: messages.value.length
  });
  
  scrollToBottom();
});

onUnmounted(() => {
  mpLogger.info('PageLifecycle', 'ChatDetailPage unmounted', {
    timestamp: new Date().toISOString()
  });
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-top: calc(var(--status-bar-height, 20px) + 88rpx);
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.page.dark {
  background: #1a1a2e;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20rpx;
  -webkit-overflow-scrolling: touch;
}

.message-item {
  display: flex;
  margin-bottom: 30rpx;
  animation: fadeIn 0.3s ease;
}

.message-item.is-self {
  flex-direction: row-reverse;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.avatar-wrap {
  flex-shrink: 0;
  width: 80rpx;
  height: 80rpx;
}

.avatar {
  width: 100%;
  height: 100%;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.1);
}

.page.dark .avatar {
  background: #252542;
}

.message-content {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.message-item.is-self .message-content {
  align-items: flex-end;
}

.message-bubble {
  background: #fff;
  border-radius: 24rpx;
  padding: 20rpx 28rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.08);
}

.page.dark .message-bubble {
  background: #252542;
}

.message-item.is-self .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.message-bubble span {
  font-size: 28rpx;
  color: #333;
  line-height: 1.5;
}

.page.dark .message-bubble span {
  color: #fff;
}

.message-item.is-self .message-bubble span {
  color: #fff;
}

.message-time {
  font-size: 20rpx;
  color: #999;
  margin-top: 8rpx;
}

.page.dark .message-time {
  color: #666;
}

.input-area {
  padding: 20rpx;
  background: #fff;
  border-top: 1rpx solid #f0f0f0;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}

.page.dark .input-area {
  background: #252542;
  border-top-color: #3a3a5c;
}

.input-wrap {
  display: flex;
  gap: 16rpx;
}

.message-input {
  flex: 1;
  height: 80rpx;
  background: #f5f7fa;
  border-radius: 40rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
  border: none;
  outline: none;
}

.page.dark .message-input {
  background: #3a3a5c;
  color: #fff;
}

.message-input::placeholder {
  color: #ccc;
}

.send-btn {
  padding: 0 32rpx;
  height: 80rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 40rpx;
  font-size: 28rpx;
  cursor: pointer;
}

.send-btn:active {
  opacity: 0.8;
}
</style>