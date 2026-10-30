<template>
  <div class="page home-page" :class="{ 'dark': isDark }">
    <!-- 轮播图区域 -->
    <swiper class="banner-swiper" :autoplay="true" :interval="4000" :circular="true">
      <swiper-item v-for="(banner, index) in banners" :key="index">
        <div class="banner-item" :style="{ background: banner.bgColor }">
          <div class="banner-content">
            <h3>{{ banner.title }}</h3>
            <p>{{ banner.desc }}</p>
          </div>
          <span class="banner-icon">{{ banner.icon }}</span>
        </div>
      </swiper-item>
    </swiper>

    <!-- 功能入口 -->
    <div class="quick-actions">
      <div 
        v-for="(action, index) in quickActions" 
        :key="index" 
        class="action-item"
        @click="handleAction(action)"
      >
        <div class="action-icon" :style="{ background: action.bgColor }">
          {{ action.icon }}
        </div>
        <span class="action-name">{{ action.name }}</span>
      </div>
    </div>

    <!-- 卡片列表 -->
    <div class="section">
      <div class="section-header">
        <h2>推荐内容</h2>
        <span class="more">查看更多 ›</span>
      </div>
      
      <div class="card-list">
        <div 
          v-for="(card, index) in cardList" 
          :key="index" 
          class="card"
          @click="handleCardClick(card)"
        >
          <div class="card-image">
            <span class="card-icon">{{ card.icon }}</span>
          </div>
          <div class="card-content">
            <h3>{{ card.title }}</h3>
            <p>{{ card.desc }}</p>
            <div class="card-footer">
              <span class="card-tag" :style="{ background: card.tagColor }">{{ card.tag }}</span>
              <span class="card-time">{{ card.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 横向滚动列表 -->
    <div class="section">
      <div class="section-header">
        <h2>热门话题</h2>
        <span class="more">热门 ›</span>
      </div>
      
      <div class="horizontal-scroll">
        <div 
          v-for="(topic, index) in topics" 
          :key="index" 
          class="topic-item"
          @click="handleTopicClick(topic)"
        >
          <div class="topic-icon">{{ topic.icon }}</div>
          <div class="topic-info">
            <span class="topic-name">{{ topic.name }}</span>
            <span class="topic-count">{{ topic.count }}人参与</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部信息 -->
    <div class="footer-space"></div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted, onUnmounted } from 'vue';
import mpLogger from '@/utils/miniprogramLogger.js';

const isDark = inject('isDark', false);

// 页面加载完成
onMounted(() => {
  mpLogger.info('PageLifecycle', 'HomePage mounted', {
    timestamp: new Date().toISOString(),
    pageName: 'HomePage',
    isDark: isDark
  });
  
  mpLogger.info('PageLoad', 'HomePage loading completed', {
    bannersCount: banners.value.length,
    actionsCount: quickActions.value.length,
    cardsCount: cardList.value.length,
    topicsCount: topics.value.length
  });
});

// 页面即将销毁
onUnmounted(() => {
  mpLogger.info('PageLifecycle', 'HomePage unmounted', {
    pageName: 'HomePage',
    timestamp: new Date().toISOString()
  });
});

const banners = ref([
  { title: '欢迎使用AI助手', desc: '智能对话，无限可能', icon: '🤖', bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { title: '全新功能上线', desc: '桌面小精灵等你来体验', icon: '✨', bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { title: '语音助手升级', desc: '更自然的语音交互', icon: '🎙️', bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' }
]);

const quickActions = ref([
  { name: '智能问答', icon: '💡', bgColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { name: '图片生成', icon: '🎨', bgColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { name: '语音聊天', icon: '🎤', bgColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { name: '文件处理', icon: '📄', bgColor: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' },
  { name: '天气查询', icon: '🌤️', bgColor: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)' },
  { name: '设置', icon: '⚙️', bgColor: 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)' }
]);

const cardList = ref([
  { title: 'AI绘画入门指南', desc: '从零开始学习AI绘画技巧', icon: '🎨', tag: '教程', tagColor: '#667eea', time: '2小时前' },
  { title: '智能助手使用技巧', desc: '掌握这些技巧，效率提升10倍', icon: '💡', tag: '技巧', tagColor: '#43e97b', time: '5小时前' },
  { title: '最新AI模型推荐', desc: '2024年最值得尝试的AI模型', icon: '🚀', tag: '推荐', tagColor: '#f5576c', time: '1天前' }
]);

const topics = ref([
  { name: 'AI创作', icon: '🎭', count: 1280 },
  { name: '编程助手', icon: '💻', count: 892 },
  { name: '生活助手', icon: '🏠', count: 654 },
  { name: '学习辅导', icon: '📚', count: 432 },
  { name: '创意设计', icon: '🎨', count: 318 }
]);

const handleAction = (action) => {
  mpLogger.info('UserInteraction', 'Quick action clicked', {
    actionName: action.name,
    actionIcon: action.icon,
    timestamp: new Date().toISOString()
  });
};

const handleCardClick = (card) => {
  mpLogger.info('UserInteraction', 'Card clicked', {
    cardTitle: card.title,
    cardTag: card.tag,
    cardTime: card.time,
    timestamp: new Date().toISOString()
  });
};

const handleTopicClick = (topic) => {
  mpLogger.info('UserInteraction', 'Topic clicked', {
    topicName: topic.name,
    topicIcon: topic.icon,
    participantCount: topic.count,
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

.banner-swiper {
  width: 100%;
  height: 300rpx;
  margin-bottom: 30rpx;
}

.banner-item {
  width: 100%;
  height: 100%;
  border-radius: 24rpx;
  margin: 0 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40rpx;
  box-sizing: border-box;
}

.banner-content h3 {
  color: #fff;
  font-size: 36rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.banner-content p {
  color: rgba(255, 255, 255, 0.8);
  font-size: 26rpx;
}

.banner-icon {
  font-size: 80rpx;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  padding: 0 24rpx;
  margin-bottom: 30rpx;
}

.action-item {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24rpx;
  cursor: pointer;
}

.action-item:active {
  transform: scale(0.95);
}

.action-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
  margin-bottom: 12rpx;
}

.action-name {
  font-size: 24rpx;
  color: #333;
}

.page.dark .action-name {
  color: #ccc;
}

.section {
  padding: 0 24rpx;
  margin-bottom: 30rpx;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.section-header h2 {
  font-size: 32rpx;
  font-weight: 600;
  color: #333;
}

.page.dark .section-header h2 {
  color: #fff;
}

.more {
  font-size: 26rpx;
  color: #999;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  display: flex;
  gap: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s ease;
}

.page.dark .card {
  background: #252542;
}

.card:active {
  transform: scale(0.99);
  box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.05);
}

.card-image {
  width: 120rpx;
  height: 120rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.page.dark .card-image {
  background: #3a3a5c;
}

.card-icon {
  font-size: 56rpx;
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.card-content h3 {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.page.dark .card-content h3 {
  color: #fff;
}

.card-content p {
  font-size: 24rpx;
  color: #999;
  margin: 8rpx 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.card-tag {
  font-size: 20rpx;
  color: #fff;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.card-time {
  font-size: 22rpx;
  color: #ccc;
}

.horizontal-scroll {
  display: flex;
  gap: 20rpx;
  overflow-x: auto;
  padding-bottom: 10rpx;
  -webkit-overflow-scrolling: touch;
}

.horizontal-scroll::-webkit-scrollbar {
  display: none;
}

.topic-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx 32rpx;
  background: #fff;
  border-radius: 20rpx;
  min-width: 140rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.page.dark .topic-item {
  background: #252542;
}

.topic-item:active {
  transform: scale(0.95);
}

.topic-icon {
  font-size: 48rpx;
  margin-bottom: 12rpx;
}

.topic-name {
  font-size: 26rpx;
  color: #333;
  margin-bottom: 8rpx;
}

.page.dark .topic-name {
  color: #fff;
}

.topic-count {
  font-size: 22rpx;
  color: #999;
}

.footer-space {
  height: 120rpx;
}
</style>