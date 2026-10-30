<template>
  <div class="page content-detail-page" :class="{ 'dark': isDark }">
    <!-- 内容头部 -->
    <div class="content-header">
      <div class="content-image">
        <span class="content-icon">🎨</span>
      </div>
      <div class="content-meta">
        <div class="author-info">
          <span class="author-avatar">👤</span>
          <span class="author-name">AI创作者</span>
        </div>
        <div class="content-stats">
          <span class="stat">❤️ 2.3k</span>
          <span class="stat">💬 328</span>
          <span class="stat">🔗 156</span>
        </div>
      </div>
    </div>

    <!-- 内容正文 -->
    <div class="content-body">
      <h1 class="content-title">AI生成艺术的无限可能</h1>
      <div class="content-tags">
        <span class="tag">AI绘画</span>
        <span class="tag">Midjourney</span>
        <span class="tag">创意设计</span>
      </div>
      <div class="content-text">
        <p>
          随着人工智能技术的飞速发展，AI生成艺术已经成为创意领域的新宠。
          从Stable Diffusion到Midjourney，各种强大的AI工具让每个人都能成为艺术家。
        </p>
        <p>
          AI艺术不仅仅是简单的图像生成，它代表了人机协作的新范式。
          艺术家们可以利用AI作为创意伙伴，探索前所未有的艺术风格和表现形式。
        </p>
        <h2>什么是AI生成艺术？</h2>
        <p>
          AI生成艺术是指使用机器学习算法，特别是深度学习模型，来创作艺术作品的过程。
          这些模型通过学习海量的艺术作品数据，可以生成全新的、独特的艺术作品。
        </p>
        <h2>主要技术手段</h2>
        <ul>
          <li>**文本到图像生成**：通过文字描述生成图像</li>
          <li>**风格迁移**：将一种艺术风格应用到另一个图像上</li>
          <li>**超分辨率**：提升图像分辨率</li>
          <li>**图像修复**：修复损坏或缺失的图像部分</li>
        </ul>
        <h2>未来展望</h2>
        <p>
          AI生成艺术正在改变我们对创造力的理解。未来，我们可能会看到更多AI与人类艺术家的合作项目，
          创造出超越人类想象的艺术作品。
        </p>
      </div>
    </div>

    <!-- 评论区 -->
    <div class="comment-section">
      <div class="section-title">评论 (328)</div>
      <div class="comment-list">
        <div 
          v-for="(comment, index) in comments" 
          :key="index" 
          class="comment-item"
        >
          <span class="comment-avatar">{{ comment.avatar }}</span>
          <div class="comment-content">
            <div class="comment-header">
              <span class="comment-name">{{ comment.name }}</span>
              <span class="comment-time">{{ comment.time }}</span>
            </div>
            <p class="comment-text">{{ comment.text }}</p>
            <div class="comment-actions">
              <span class="action" @click="handleLike(comment)">❤️ {{ comment.likes }}</span>
              <span class="action">💬 回复</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部空间 -->
    <div class="footer-space"></div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted, onUnmounted } from 'vue';
import mpLogger from '@/utils/miniprogramLogger.js';

const isDark = inject('isDark', false);

const comments = ref([
  { id: 1, avatar: '🎨', name: '艺术爱好者', time: '2小时前', text: '非常精彩的文章！AI艺术确实改变了创作方式。', likes: 128 },
  { id: 2, avatar: '🤖', name: 'AI研究员', time: '3小时前', text: '期待更多关于AI生成技术的深度解析。', likes: 89 },
  { id: 3, avatar: '✨', name: '创意设计师', time: '5小时前', text: '已经开始尝试用AI辅助设计了，效果很棒！', likes: 156 },
]);

const handleLike = (comment) => {
  comment.likes++;
  mpLogger.info('Interaction', 'Comment liked', {
    commentId: comment.id,
    newLikes: comment.likes
  });
};

onMounted(() => {
  mpLogger.info('PageLifecycle', 'ContentDetailPage mounted', {
    timestamp: new Date().toISOString()
  });
});

onUnmounted(() => {
  mpLogger.info('PageLifecycle', 'ContentDetailPage unmounted', {
    timestamp: new Date().toISOString()
  });
});
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

.content-header {
  background: #fff;
  overflow: hidden;
}

.page.dark .content-header {
  background: #252542;
}

.content-image {
  width: 100%;
  height: 400rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.content-icon {
  font-size: 120rpx;
}

.content-meta {
  padding: 24rpx;
}

.author-info {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}

.author-avatar {
  width: 64rpx;
  height: 64rpx;
  background: #f5f7fa;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  margin-right: 16rpx;
}

.page.dark .author-avatar {
  background: #3a3a5c;
}

.author-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.page.dark .author-name {
  color: #fff;
}

.content-stats {
  display: flex;
  gap: 32rpx;
}

.stat {
  font-size: 24rpx;
  color: #999;
}

.page.dark .stat {
  color: #666;
}

.content-body {
  margin: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}

.page.dark .content-body {
  background: #252542;
}

.content-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
  margin: 0 0 20rpx 0;
  line-height: 1.4;
}

.page.dark .content-title {
  color: #fff;
}

.content-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.tag {
  padding: 8rpx 20rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 24rpx;
  border-radius: 20rpx;
}

.content-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.8;
}

.page.dark .content-text {
  color: #aaa;
}

.content-text h2 {
  font-size: 32rpx;
  color: #333;
  margin: 24rpx 0 16rpx 0;
}

.page.dark .content-text h2 {
  color: #fff;
}

.content-text p {
  margin: 0 0 16rpx 0;
}

.content-text ul {
  margin: 0;
  padding-left: 24rpx;
}

.content-text li {
  margin-bottom: 12rpx;
}

.comment-section {
  margin: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.page.dark .comment-section {
  background: #252542;
}

.section-title {
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  border-bottom: 1rpx solid #f0f0f0;
}

.page.dark .section-title {
  color: #fff;
  border-bottom-color: #3a3a5c;
}

.comment-list {
  padding: 16rpx 24rpx;
}

.comment-item {
  display: flex;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.page.dark .comment-item {
  border-bottom-color: #3a3a5c;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  width: 64rpx;
  height: 64rpx;
  background: #f5f7fa;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  margin-right: 16rpx;
  flex-shrink: 0;
}

.page.dark .comment-avatar {
  background: #3a3a5c;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.comment-name {
  font-size: 26rpx;
  font-weight: 600;
  color: #333;
}

.page.dark .comment-name {
  color: #fff;
}

.comment-time {
  font-size: 22rpx;
  color: #999;
}

.page.dark .comment-time {
  color: #666;
}

.comment-text {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  margin: 0 0 12rpx 0;
}

.page.dark .comment-text {
  color: #aaa;
}

.comment-actions {
  display: flex;
  gap: 24rpx;
}

.action {
  font-size: 22rpx;
  color: #999;
  cursor: pointer;
}

.page.dark .action {
  color: #666;
}

.action:active {
  color: #667eea;
}

.footer-space {
  height: 120rpx;
}
</style>