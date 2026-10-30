<template>
  <div class="page chat-page" :class="{ 'dark': isDark }">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-input-wrap">
        <span class="search-icon">🔍</span>
        <input 
          type="text" 
          class="search-input" 
          placeholder="搜索聊天"
          v-model="searchText"
        />
      </div>
    </div>

    <!-- 聊天列表 -->
    <div class="chat-list">
      <div 
        v-for="(chat, index) in chatList" 
        :key="index" 
        class="chat-item"
        @click="handleChatClick(chat)"
        @longpress="handleLongPress(chat)"
      >
        <div class="chat-avatar">{{ chat.avatar }}</div>
        <div class="chat-info">
          <div class="chat-header">
            <span class="chat-name">{{ chat.name }}</span>
            <span class="chat-time">{{ chat.time }}</span>
          </div>
          <div class="chat-preview">
            <span class="chat-message">{{ chat.message }}</span>
            <span v-if="chat.unread" class="unread-badge">{{ chat.unread }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="chatList.length === 0" class="empty-state">
      <span class="empty-icon">💬</span>
      <p>暂无聊天记录</p>
      <button class="empty-btn">开始新对话</button>
    </div>

    <!-- 底部空间 -->
    <div class="footer-space"></div>
  </div>
</template>

<script setup>
import { ref, computed, inject, onMounted, onUnmounted, watch } from 'vue';
import mpLogger from '@/utils/miniprogramLogger.js';

const isDark = inject('isDark', false);
const searchText = ref('');

// 页面加载完成
onMounted(() => {
  mpLogger.info('PageLifecycle', 'ChatPage mounted', {
    timestamp: new Date().toISOString(),
    pageName: 'ChatPage',
    isDark: isDark,
    initialChatCount: chatList.value.length
  });
  
  mpLogger.info('PageLoad', 'ChatPage loading completed', {
    totalUnread: chatList.value.reduce((sum, chat) => sum + chat.unread, 0)
  });
});

// 页面即将销毁
onUnmounted(() => {
  mpLogger.info('PageLifecycle', 'ChatPage unmounted', {
    pageName: 'ChatPage',
    timestamp: new Date().toISOString()
  });
});

// 监听搜索文本变化
watch(searchText, (newVal, oldVal) => {
  mpLogger.info('Search', 'Search text changed', {
    previousText: oldVal,
    currentText: newVal,
    filteredCount: filteredChatList.value.length,
    timestamp: new Date().toISOString()
  });
});

const chatList = ref([
  { 
    id: 1, 
    name: 'AI助手', 
    avatar: '🤖', 
    message: '有什么我可以帮助你的吗？', 
    time: '刚刚',
    unread: 3 
  },
  { 
    id: 2, 
    name: '张三', 
    avatar: '👨', 
    message: '周末一起去爬山吗？', 
    time: '10分钟前',
    unread: 0 
  },
  { 
    id: 3, 
    name: '设计组群', 
    avatar: '👥', 
    message: '[图片] 新版设计稿已更新', 
    time: '1小时前',
    unread: 12 
  },
  { 
    id: 4, 
    name: '李四', 
    avatar: '👩', 
    message: '好的，明天下午见', 
    time: '昨天',
    unread: 0 
  },
  { 
    id: 5, 
    name: '王五', 
    avatar: '👨‍💼', 
    message: '项目进度汇报一下', 
    time: '2天前',
    unread: 0 
  }
]);

const filteredChatList = computed(() => {
  if (!searchText.value) return chatList.value;
  return chatList.value.filter(item => 
    item.name.toLowerCase().includes(searchText.value.toLowerCase()) ||
    item.message.toLowerCase().includes(searchText.value.toLowerCase())
  );
});

const handleChatClick = (chat) => {
  mpLogger.info('UserInteraction', 'Chat item clicked', {
    chatId: chat.id,
    chatName: chat.name,
    avatar: chat.avatar,
    unreadCount: chat.unread,
    lastMessage: chat.message,
    time: chat.time,
    timestamp: new Date().toISOString()
  });
};

const handleLongPress = (chat) => {
  mpLogger.info('UserInteraction', 'Chat item long pressed', {
    chatId: chat.id,
    chatName: chat.name,
    timestamp: new Date().toISOString()
  });
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-top: calc(var(--status-bar-height, 20px) + 88rpx);
  background: #f5f7fa;
  transition: background 0.3s ease;
}

.page.dark {
  background: #1a1a2e;
}

.search-bar {
  padding: 20rpx 24rpx;
  background: #fff;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.page.dark .search-bar {
  background: #252542;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border-radius: 40rpx;
  padding: 16rpx 24rpx;
}

.page.dark .search-input-wrap {
  background: #3a3a5c;
}

.search-icon {
  font-size: 28rpx;
  margin-right: 16rpx;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 28rpx;
  color: #333;
  outline: none;
}

.page.dark .search-input {
  color: #fff;
}

.search-input::placeholder {
  color: #ccc;
}

.chat-list {
  padding: 16rpx;
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page.dark .chat-item {
  background: #252542;
}

.chat-item:active {
  background: #f5f7fa;
}

.page.dark .chat-item:active {
  background: #3a3a5c;
}

.chat-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.chat-info {
  flex: 1;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.chat-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
}

.page.dark .chat-name {
  color: #fff;
}

.chat-time {
  font-size: 22rpx;
  color: #999;
}

.chat-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-message {
  font-size: 26rpx;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.page.dark .chat-message {
  color: #666;
}

.unread-badge {
  min-width: 36rpx;
  height: 36rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 22rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10rpx;
  margin-left: 12rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
}

.empty-icon {
  font-size: 100rpx;
  margin-bottom: 30rpx;
}

.empty-state p {
  font-size: 28rpx;
  color: #999;
  margin-bottom: 30rpx;
}

.empty-btn {
  padding: 20rpx 60rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 40rpx;
  font-size: 28rpx;
  cursor: pointer;
}

.empty-btn:active {
  opacity: 0.8;
}

.footer-space {
  height: 120rpx;
}
</style>