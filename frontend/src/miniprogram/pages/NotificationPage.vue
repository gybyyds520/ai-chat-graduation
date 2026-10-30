<template>
  <div class="page notification-page" :class="{ 'dark': isDark }">
    <!-- 通知列表 -->
    <div class="notification-list">
      <!-- 系统通知 -->
      <div class="notification-group">
        <div class="group-title">系统通知</div>
        <div 
          v-for="(notif, index) in systemNotifications" 
          :key="index" 
          class="notification-item"
          :class="{ 'unread': !notif.read }"
          @click="markAsRead(notif)"
        >
          <div class="notif-icon" :class="notif.type">
            {{ getIcon(notif.type) }}
          </div>
          <div class="notif-content">
            <h3 class="notif-title">{{ notif.title }}</h3>
            <p class="notif-desc">{{ notif.desc }}</p>
            <span class="notif-time">{{ notif.time }}</span>
          </div>
          <div v-if="!notif.read" class="unread-dot"></div>
        </div>
      </div>

      <!-- 互动通知 -->
      <div class="notification-group">
        <div class="group-title">互动通知</div>
        <div 
          v-for="(notif, index) in interactionNotifications" 
          :key="index" 
          class="notification-item"
          :class="{ 'unread': !notif.read }"
          @click="markAsRead(notif)"
        >
          <div class="notif-icon" :class="notif.type">
            {{ getIcon(notif.type) }}
          </div>
          <div class="notif-content">
            <h3 class="notif-title">{{ notif.title }}</h3>
            <p class="notif-desc">{{ notif.desc }}</p>
            <span class="notif-time">{{ notif.time }}</span>
          </div>
          <div v-if="!notif.read" class="unread-dot"></div>
        </div>
      </div>

      <!-- 服务通知 -->
      <div class="notification-group">
        <div class="group-title">服务通知</div>
        <div 
          v-for="(notif, index) in serviceNotifications" 
          :key="index" 
          class="notification-item"
          :class="{ 'unread': !notif.read }"
          @click="markAsRead(notif)"
        >
          <div class="notif-icon" :class="notif.type">
            {{ getIcon(notif.type) }}
          </div>
          <div class="notif-content">
            <h3 class="notif-title">{{ notif.title }}</h3>
            <p class="notif-desc">{{ notif.desc }}</p>
            <span class="notif-time">{{ notif.time }}</span>
          </div>
          <div v-if="!notif.read" class="unread-dot"></div>
        </div>
      </div>
    </div>

    <!-- 全部已读按钮 -->
    <div class="mark-all-read">
      <button class="read-btn" @click="markAllRead">全部已读</button>
    </div>

    <!-- 底部空间 -->
    <div class="footer-space"></div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted, onUnmounted } from 'vue';
import mpLogger from '@/utils/miniprogramLogger.js';

const isDark = inject('isDark', false);

const systemNotifications = ref([
  { id: 1, type: 'system', title: '系统更新', desc: 'AI Chat v1.0.0版本已发布，新增多项功能', time: '5分钟前', read: false },
  { id: 2, type: 'system', title: '维护通知', desc: '今晚22:00-00:00将进行系统维护', time: '1小时前', read: false },
  { id: 3, type: 'system', title: '安全提醒', desc: '您的账户在新设备上登录', time: '3小时前', read: true },
]);

const interactionNotifications = ref([
  { id: 4, type: 'like', title: '点赞通知', desc: '用户"AI爱好者"点赞了您的文章', time: '10分钟前', read: false },
  { id: 5, type: 'comment', title: '评论通知', desc: '用户"创意达人"评论了您的作品', time: '20分钟前', read: false },
  { id: 6, type: 'follow', title: '关注通知', desc: '用户"技术宅"关注了您', time: '1小时前', read: true },
]);

const serviceNotifications = ref([
  { id: 7, type: 'service', title: '会员到期提醒', desc: '您的VIP会员还有7天到期', time: '1天前', read: true },
  { id: 8, type: 'service', title: '存储空间', desc: '您的存储空间已使用80%', time: '2天前', read: true },
]);

const getIcon = (type) => {
  const icons = {
    system: '⚙️',
    like: '❤️',
    comment: '💬',
    follow: '👤',
    service: '🔔'
  };
  return icons[type] || '📧';
};

const markAsRead = (notif) => {
  if (!notif.read) {
    notif.read = true;
    mpLogger.info('Notification', 'Notification marked as read', {
      id: notif.id,
      title: notif.title
    });
  }
};

const markAllRead = () => {
  systemNotifications.value.forEach(n => n.read = true);
  interactionNotifications.value.forEach(n => n.read = true);
  serviceNotifications.value.forEach(n => n.read = true);
  
  mpLogger.info('Notification', 'All notifications marked as read', {
    timestamp: new Date().toISOString()
  });
};

onMounted(() => {
  mpLogger.info('PageLifecycle', 'NotificationPage mounted', {
    timestamp: new Date().toISOString(),
    unreadCount: getUnreadCount()
  });
});

onUnmounted(() => {
  mpLogger.info('PageLifecycle', 'NotificationPage unmounted', {
    timestamp: new Date().toISOString()
  });
});

const getUnreadCount = () => {
  return [
    ...systemNotifications.value,
    ...interactionNotifications.value,
    ...serviceNotifications.value
  ].filter(n => !n.read).length;
};
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-top: calc(var(--status-bar-height, 20px) + 88rpx);
  background: #f5f7fa;
}

.page.dark {
  background: #1a1a2e;
}

.notification-list {
  padding: 20rpx;
}

.notification-group {
  background: #fff;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.page.dark .notification-group {
  background: #252542;
}

.group-title {
  padding: 20rpx 24rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #999;
  border-bottom: 1rpx solid #f0f0f0;
}

.page.dark .group-title {
  color: #666;
  border-bottom-color: #3a3a5c;
}

.notification-item {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s ease;
}

.page.dark .notification-item {
  border-bottom-color: #3a3a5c;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:active {
  background: #f5f7fa;
}

.page.dark .notification-item:active {
  background: #3a3a5c;
}

.notification-item.unread {
  background: #fff9f0;
}

.page.dark .notification-item.unread {
  background: #2d2d4a;
}

.notif-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.notif-icon.system {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.notif-icon.like {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
}

.notif-icon.comment {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.notif-icon.follow {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.notif-icon.service {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.notif-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.notif-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin: 0 0 8rpx 0;
}

.page.dark .notif-title {
  color: #fff;
}

.notif-desc {
  font-size: 24rpx;
  color: #999;
  margin: 0 0 8rpx 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.page.dark .notif-desc {
  color: #666;
}

.notif-time {
  font-size: 22rpx;
  color: #ccc;
}

.page.dark .notif-time {
  color: #555;
}

.unread-dot {
  width: 16rpx;
  height: 16rpx;
  background: #ff6b6b;
  border-radius: 50%;
  margin-left: 16rpx;
}

.mark-all-read {
  padding: 20rpx;
}

.read-btn {
  width: 100%;
  height: 80rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 40rpx;
  font-size: 30rpx;
  cursor: pointer;
}

.read-btn:active {
  opacity: 0.8;
}

.footer-space {
  height: 120rpx;
}
</style>