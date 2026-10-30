<template>
  <div class="page profile-page" :class="{ 'dark': isDark }">
    <!-- 用户信息区域 -->
    <div class="profile-header">
      <div class="profile-bg"></div>
      <div class="profile-info">
        <div class="avatar-wrap">
          <span class="avatar">👤</span>
          <span class="vip-badge">VIP</span>
        </div>
        <div class="user-info">
          <h2 class="username">{{ user.name }}</h2>
          <p class="user-desc">{{ user.desc }}</p>
        </div>
        <button class="edit-btn">编辑资料</button>
      </div>

      <!-- 数据统计 -->
      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-value">{{ user.stats.posts }}</span>
          <span class="stat-label">作品</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ user.stats.followers }}</span>
          <span class="stat-label">粉丝</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <span class="stat-value">{{ user.stats.following }}</span>
          <span class="stat-label">关注</span>
        </div>
      </div>
    </div>

    <!-- 功能菜单 -->
    <div class="menu-section">
      <div class="menu-group">
        <div 
          v-for="(item, index) in menuItems" 
          :key="index" 
          class="menu-item"
          @click="handleMenuClick(item)"
        >
          <span class="menu-icon">{{ item.icon }}</span>
          <span class="menu-text">{{ item.text }}</span>
          <span class="menu-arrow">›</span>
        </div>
      </div>
    </div>

    <!-- 设置区域 -->
    <div class="menu-section">
      <div class="section-title">设置</div>
      <div class="menu-group">
        <div 
          v-for="(item, index) in settingsItems" 
          :key="index" 
          class="menu-item"
          @click="handleMenuClick(item)"
        >
          <span class="menu-icon">{{ item.icon }}</span>
          <span class="menu-text">{{ item.text }}</span>
          <span v-if="item.toggle" class="toggle-wrap">
            <span class="toggle" :class="{ 'on': item.value }"></span>
          </span>
          <span v-else class="menu-arrow">›</span>
        </div>
      </div>
    </div>

    <!-- 版本信息 -->
    <div class="version-info">
      <span>AI Chat v1.0.0</span>
    </div>

    <!-- 底部空间 -->
    <div class="footer-space"></div>
  </div>
</template>

<script setup>
import { ref, inject } from 'vue';

const isDark = inject('isDark', false);

const user = ref({
  name: '用户昵称',
  desc: '热爱AI技术，分享精彩内容',
  stats: {
    posts: 128,
    followers: 3421,
    following: 156
  }
});

const menuItems = ref([
  { icon: '📁', text: '我的收藏' },
  { icon: '💾', text: '离线缓存' },
  { icon: '🎁', text: '我的礼包' },
  { icon: '🎫', text: '优惠券' },
  { icon: '🏆', text: '成就中心' },
  { icon: '💬', text: '意见反馈' }
]);

const settingsItems = ref([
  { icon: '🔔', text: '消息通知', toggle: true, value: true },
  { icon: '🌙', text: '深色模式', toggle: true, value: false },
  { icon: '🔒', text: '隐私设置' },
  { icon: '📊', text: '数据管理' },
  { icon: '📱', text: '设备管理' },
  { icon: '❓', text: '帮助与反馈' }
]);

const handleMenuClick = (item) => {
  console.log('点击菜单:', item.text);
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

.profile-header {
  position: relative;
  padding-bottom: 30rpx;
}

.profile-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 300rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.profile-info {
  position: relative;
  display: flex;
  align-items: flex-start;
  padding: 200rpx 24rpx 20rpx;
}

.avatar-wrap {
  position: relative;
  margin-right: 20rpx;
}

.avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  border: 6rpx solid #fff;
}

.vip-badge {
  position: absolute;
  bottom: -5rpx;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #ffd700 0%, #ff8c00 100%);
  color: #fff;
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
}

.user-info {
  flex: 1;
}

.username {
  font-size: 36rpx;
  font-weight: 600;
  color: #fff;
  margin: 0 0 8rpx 0;
}

.user-desc {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
}

.edit-btn {
  padding: 12rpx 24rpx;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  border: 2rpx solid rgba(255, 255, 255, 0.3);
  border-radius: 40rpx;
  font-size: 24rpx;
  cursor: pointer;
}

.edit-btn:active {
  background: rgba(255, 255, 255, 0.3);
}

.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin-top: 20rpx;
  padding: 0 24rpx;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 36rpx;
  font-weight: 600;
  color: #333;
}

.page.dark .stat-value {
  color: #fff;
}

.stat-label {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.stat-divider {
  width: 1rpx;
  height: 60rpx;
  background: #e0e0e0;
}

.page.dark .stat-divider {
  background: #3a3a5c;
}

.menu-section {
  margin: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.page.dark .menu-section {
  background: #252542;
}

.section-title {
  padding: 20rpx 24rpx;
  font-size: 24rpx;
  color: #999;
  border-bottom: 1rpx solid #f0f0f0;
}

.page.dark .section-title {
  color: #666;
  border-bottom-color: #3a3a5c;
}

.menu-group {
  display: flex;
  flex-direction: column;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s ease;
}

.page.dark .menu-item {
  border-bottom-color: #3a3a5c;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:active {
  background: #f5f7fa;
}

.page.dark .menu-item:active {
  background: #3a3a5c;
}

.menu-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
}

.menu-text {
  flex: 1;
  font-size: 30rpx;
  color: #333;
}

.page.dark .menu-text {
  color: #fff;
}

.menu-arrow {
  font-size: 36rpx;
  color: #ccc;
}

.toggle-wrap {
  width: 80rpx;
  height: 44rpx;
  background: #ccc;
  border-radius: 22rpx;
  position: relative;
  margin-right: 8rpx;
}

.toggle {
  position: absolute;
  top: 4rpx;
  left: 4rpx;
  width: 36rpx;
  height: 36rpx;
  background: #fff;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.toggle.on {
  left: 40rpx;
}

.toggle-wrap.on {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.version-info {
  text-align: center;
  padding: 40rpx;
}

.version-info span {
  font-size: 24rpx;
  color: #999;
}

.footer-space {
  height: 120rpx;
}
</style>