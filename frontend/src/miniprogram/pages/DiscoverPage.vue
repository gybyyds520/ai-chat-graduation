<template>
  <div class="page discover-page" :class="{ 'dark': isDark }">
    <!-- 搜索区域 -->
    <div class="discover-header">
      <div class="search-box">
        <input type="text" class="discover-search" placeholder="发现精彩内容" v-model="searchText" />
        <span class="search-btn">🔍</span>
      </div>
    </div>

    <!-- 分类标签 -->
    <scroll-view class="category-scroll" scroll-x>
      <div 
        v-for="(cat, index) in categories" 
        :key="index" 
        class="category-item"
        :class="{ 'active': activeCategory === index }"
        @click="activeCategory = index"
      >
        {{ cat.name }}
      </div>
    </scroll-view>

    <!-- 瀑布流内容 -->
    <div class="waterfall">
      <div class="waterfall-column">
        <div 
          v-for="(item, index) in leftColumn" 
          :key="index" 
          class="waterfall-item"
          @click="handleItemClick(item)"
        >
          <div class="item-image" :style="{ height: item.height + 'rpx' }">
            <span class="item-icon">{{ item.icon }}</span>
          </div>
          <div class="item-info">
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
            <div class="item-stats">
              <span class="stat">{{ item.likes }} ❤️</span>
              <span class="stat">{{ item.comments }} 💬</span>
            </div>
          </div>
        </div>
      </div>
      <div class="waterfall-column">
        <div 
          v-for="(item, index) in rightColumn" 
          :key="index" 
          class="waterfall-item"
          @click="handleItemClick(item)"
        >
          <div class="item-image" :style="{ height: item.height + 'rpx' }">
            <span class="item-icon">{{ item.icon }}</span>
          </div>
          <div class="item-info">
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
            <div class="item-stats">
              <span class="stat">{{ item.likes }} ❤️</span>
              <span class="stat">{{ item.comments }} 💬</span>
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
import { ref, computed, inject } from 'vue';

const isDark = inject('isDark', false);
const searchText = ref('');
const activeCategory = ref(0);

const categories = ref([
  { name: '推荐' },
  { name: 'AI绘画' },
  { name: 'AI写作' },
  { name: '编程助手' },
  { name: '生活技巧' },
  { name: '科技资讯' }
]);

const allItems = ref([
  { title: 'AI生成的绝美风景', desc: '使用Stable Diffusion生成的超现实风景', icon: '🌄', height: 360, likes: 234, comments: 45 },
  { title: 'Midjourney教程', desc: '从零开始学习Midjourney', icon: '🎨', height: 280, likes: 567, comments: 89 },
  { title: 'ChatGPT使用技巧', desc: '10个让你效率翻倍的技巧', icon: '💡', height: 320, likes: 892, comments: 156 },
  { title: 'AI音乐创作', desc: '用AI创作属于自己的音乐', icon: '🎵', height: 240, likes: 345, comments: 67 },
  { title: 'Python自动化', desc: '用AI辅助编写自动化脚本', icon: '🐍', height: 300, likes: 456, comments: 78 },
  { title: 'AI视频生成', desc: 'Text-to-Video技术入门', icon: '🎬', height: 340, likes: 678, comments: 123 },
  { title: '机器学习入门', desc: '零基础入门机器学习', icon: '🤖', height: 260, likes: 789, comments: 145 },
  { title: '数据可视化', desc: '用AI生成精美图表', icon: '📊', height: 290, likes: 234, comments: 56 }
]);

const leftColumn = computed(() => allItems.value.filter((_, i) => i % 2 === 0));
const rightColumn = computed(() => allItems.value.filter((_, i) => i % 2 === 1));

const handleItemClick = (item) => {
  console.log('点击内容:', item.title);
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

.discover-header {
  padding: 20rpx 24rpx;
  background: #fff;
}

.page.dark .discover-header {
  background: #252542;
}

.search-box {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border-radius: 40rpx;
  padding: 4rpx;
}

.page.dark .search-box {
  background: #3a3a5c;
}

.discover-search {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 28rpx;
  color: #333;
  padding: 16rpx 24rpx;
  outline: none;
}

.page.dark .discover-search {
  color: #fff;
}

.discover-search::placeholder {
  color: #ccc;
}

.search-btn {
  width: 72rpx;
  height: 72rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  cursor: pointer;
}

.category-scroll {
  white-space: nowrap;
  padding: 20rpx 0;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.page.dark .category-scroll {
  background: #252542;
  border-bottom-color: #3a3a5c;
}

.category-item {
  display: inline-block;
  padding: 16rpx 32rpx;
  margin: 0 12rpx;
  background: #f5f7fa;
  border-radius: 40rpx;
  font-size: 28rpx;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page.dark .category-item {
  background: #3a3a5c;
  color: #999;
}

.category-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.waterfall {
  display: flex;
  padding: 20rpx;
  gap: 16rpx;
}

.waterfall-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.waterfall-item {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page.dark .waterfall-item {
  background: #252542;
}

.waterfall-item:active {
  transform: scale(0.98);
}

.item-image {
  width: 100%;
  background: linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.page.dark .item-image {
  background: #3a3a5c;
}

.item-icon {
  font-size: 80rpx;
}

.item-info {
  padding: 16rpx;
}

.item-info h3 {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin: 0 0 8rpx 0;
}

.page.dark .item-info h3 {
  color: #fff;
}

.item-info p {
  font-size: 24rpx;
  color: #999;
  margin: 0 0 12rpx 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.page.dark .item-info p {
  color: #666;
}

.item-stats {
  display: flex;
  gap: 20rpx;
}

.stat {
  font-size: 22rpx;
  color: #999;
}

.footer-space {
  height: 120rpx;
}
</style>