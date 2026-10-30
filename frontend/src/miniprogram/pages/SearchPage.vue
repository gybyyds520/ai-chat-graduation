<template>
  <div class="page search-page" :class="{ 'dark': isDark }">
    <!-- 搜索框 -->
    <div class="search-header">
      <div class="search-box">
        <input 
          type="text" 
          class="search-input" 
          placeholder="搜索..." 
          v-model="searchQuery"
          @keyup.enter="handleSearch"
          ref="searchInput"
        />
        <button class="search-btn" @click="handleSearch">🔍</button>
        <button v-if="searchQuery" class="clear-btn" @click="clearSearch">✕</button>
      </div>
    </div>

    <!-- 搜索历史 -->
    <div v-if="!searchQuery" class="history-section">
      <div class="section-header">
        <span class="section-title">搜索历史</span>
        <span class="clear-history" @click="clearHistory">清空</span>
      </div>
      <div class="history-tags">
        <span 
          v-for="(item, index) in searchHistory" 
          :key="index" 
          class="history-tag"
          @click="handleHistoryClick(item)"
        >
          {{ item }}
        </span>
      </div>
    </div>

    <!-- 热门搜索 -->
    <div v-if="!searchQuery" class="hot-section">
      <div class="section-header">
        <span class="section-title">热门搜索</span>
      </div>
      <div class="hot-list">
        <div 
          v-for="(item, index) in hotSearches" 
          :key="index" 
          class="hot-item"
          @click="handleHotClick(item)"
        >
          <span class="hot-rank" :class="{ 'top': index < 3 }">{{ index + 1 }}</span>
          <span class="hot-text">{{ item }}</span>
        </div>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-if="searchQuery" class="results-section">
      <div class="results-header">
        <span class="results-count">找到 {{ searchResults.length }} 个结果</span>
      </div>
      
      <div class="results-tabs">
        <span 
          v-for="(tab, index) in resultTabs" 
          :key="index"
          class="result-tab"
          :class="{ 'active': activeTab === index }"
          @click="activeTab = index"
        >
          {{ tab }}
        </span>
      </div>

      <div class="results-list">
        <div 
          v-for="(result, index) in searchResults" 
          :key="index" 
          class="result-item"
          @click="handleResultClick(result)"
        >
          <div class="result-icon">{{ result.icon }}</div>
          <div class="result-info">
            <h3 class="result-title">{{ result.title }}</h3>
            <p class="result-desc">{{ result.desc }}</p>
            <span class="result-tag">{{ result.tag }}</span>
          </div>
        </div>
      </div>

      <!-- 无结果提示 -->
      <div v-if="searchResults.length === 0" class="no-results">
        <span class="no-results-icon">🔍</span>
        <p>未找到相关结果</p>
        <p class="no-results-hint">尝试其他关键词</p>
      </div>
    </div>

    <!-- 底部空间 -->
    <div class="footer-space"></div>
  </div>
</template>

<script setup>import { ref, inject, onMounted, onUnmounted, nextTick } from 'vue';
import mpLogger from '@/utils/miniprogramLogger.js';
const isDark = inject('isDark', false);
const searchQuery = ref('');
const searchInput = ref(null);
const activeTab = ref(0);
const searchHistory = ref(['AI绘画', 'ChatGPT', '深度学习', 'Python教程']);
const hotSearches = ref(['AI生成艺术', 'Midjourney教程', 'ChatGPT使用技巧', '人工智能入门', '机器学习实战']);
const resultTabs = ['全部', '文章', '用户', '话题'];
const searchResults = ref([]);
const handleSearch = () => {
 if (!searchQuery.value.trim())
 return;
 mpLogger.info('Search', 'Search executed', {
 query: searchQuery.value,
 timestamp: new Date().toISOString()
 });
 // 添加到搜索历史
 if (!searchHistory.value.includes(searchQuery.value)) {
 searchHistory.value.unshift(searchQuery.value);
 if (searchHistory.value.length > 10) {
 searchHistory.value.pop();
 }
 }
 // 模拟搜索结果
 searchResults.value = [
 { icon: '🎨', title: 'AI生成艺术的无限可能', desc: '探索AI艺术的创作边界和无限可能...', tag: 'AI绘画' },
 { icon: '📚', title: 'ChatGPT完全使用指南', desc: '从入门到精通，掌握ChatGPT的所有技巧...', tag: 'AI教程' },
 { icon: '🤖', title: '深度学习入门实战', desc: '零基础入门深度学习，构建神经网络...', tag: '技术教程' },
 { icon: '💡', title: 'AI创作工具推荐', desc: '2024年最值得尝试的AI创作工具...', tag: '工具推荐' },
 ];
};
const handleHistoryClick = (query) => {
 searchQuery.value = query;
 handleSearch();
};
const handleHotClick = (query) => {
 searchQuery.value = query;
 handleSearch();
};
const handleResultClick = (result) => {
 mpLogger.info('Search', 'Result clicked', {
 title: result.title,
 tag: result.tag
 });
};
const clearSearch = () => {
 searchQuery.value = '';
 searchResults.value = [];
 nextTick(() => {
 if (searchInput.value) {
 searchInput.value.focus();
 }
 });
};
const clearHistory = () => {
 searchHistory.value = [];
 mpLogger.info('Search', 'Search history cleared');
};
onMounted(() => {
 mpLogger.info('PageLifecycle', 'SearchPage mounted', {
 timestamp: new Date().toISOString()
 });
 nextTick(() => {
 if (searchInput.value) {
 searchInput.value.focus();
 }
 });
});
onUnmounted(() => {
 mpLogger.info('PageLifecycle', 'SearchPage unmounted', {
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

.search-header {
  padding: 20rpx;
  background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
}

.page.dark .search-header {
  background: #252542;
  border-bottom-color: #3a3a5c;
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

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 28rpx;
  color: #333;
  padding: 16rpx 24rpx;
  outline: none;
}

.page.dark .search-input {
  color: #fff;
}

.search-input::placeholder {
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
  border: none;
  cursor: pointer;
}

.clear-btn {
  width: 48rpx;
  height: 48rpx;
  background: #ccc;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #fff;
  border: none;
  cursor: pointer;
  margin-right: 8rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}

.page.dark .section-title {
  color: #fff;
}

.clear-history {
  font-size: 24rpx;
  color: #999;
  cursor: pointer;
}

.page.dark .clear-history {
  color: #666;
}

.history-section, .hot-section {
  margin: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.page.dark .history-section,
.page.dark .hot-section {
  background: #252542;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  padding: 0 24rpx 24rpx;
  gap: 16rpx;
}

.history-tag {
  padding: 12rpx 24rpx;
  background: #f5f7fa;
  border-radius: 24rpx;
  font-size: 26rpx;
  color: #666;
  cursor: pointer;
}

.page.dark .history-tag {
  background: #3a3a5c;
  color: #aaa;
}

.hot-list {
  padding: 0 24rpx 24rpx;
}

.hot-item {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
  cursor: pointer;
}

.page.dark .hot-item {
  border-bottom-color: #3a3a5c;
}

.hot-item:last-child {
  border-bottom: none;
}

.hot-rank {
  width: 48rpx;
  height: 48rpx;
  background: #f5f7fa;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #999;
  margin-right: 20rpx;
}

.page.dark .hot-rank {
  background: #3a3a5c;
  color: #666;
}

.hot-rank.top {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  color: #fff;
}

.hot-text {
  font-size: 28rpx;
  color: #333;
}

.page.dark .hot-text {
  color: #fff;
}

.results-section {
  padding: 20rpx;
}

.results-header {
  margin-bottom: 20rpx;
}

.results-count {
  font-size: 26rpx;
  color: #999;
}

.page.dark .results-count {
  color: #666;
}

.results-tabs {
  display: flex;
  background: #fff;
  border-radius: 12rpx;
  padding: 6rpx;
  margin-bottom: 20rpx;
}

.page.dark .results-tabs {
  background: #252542;
}

.result-tab {
  flex: 1;
  text-align: center;
  padding: 16rpx;
  font-size: 26rpx;
  color: #666;
  border-radius: 8rpx;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page.dark .result-tab {
  color: #999;
}

.result-tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.results-list {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.page.dark .results-list {
  background: #252542;
}

.result-item {
  display: flex;
  padding: 20rpx;
  border-bottom: 1rpx solid #f0f0f0;
  cursor: pointer;
}

.page.dark .result-item {
  border-bottom-color: #3a3a5c;
}

.result-item:last-child {
  border-bottom: none;
}

.result-icon {
  width: 80rpx;
  height: 80rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  margin-right: 20rpx;
}

.result-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.result-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin: 0 0 8rpx 0;
}

.page.dark .result-title {
  color: #fff;
}

.result-desc {
  font-size: 24rpx;
  color: #999;
  margin: 0 0 12rpx 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.page.dark .result-desc {
  color: #666;
}

.result-tag {
  font-size: 22rpx;
  color: #667eea;
}

.no-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 40rpx;
  background: #fff;
  border-radius: 16rpx;
}

.page.dark .no-results {
  background: #252542;
}

.no-results-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.no-results p {
  font-size: 28rpx;
  color: #999;
  margin: 0 0 8rpx 0;
}

.page.dark .no-results p {
  color: #666;
}

.no-results-hint {
  font-size: 24rpx !important;
}

.footer-space {
  height: 120rpx;
}
</style>