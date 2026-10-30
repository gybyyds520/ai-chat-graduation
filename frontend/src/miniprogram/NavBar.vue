<template>
  <nav class="navbar" :class="{ 'dark': isDark }">
    <div class="navbar-content">
      <!-- 左侧区域 -->
      <div class="navbar-left" @click="handleBack" v-if="showBack">
        <span class="back-icon">‹</span>
      </div>
      <div class="navbar-left" v-else></div>

      <!-- 中间标题 -->
      <div class="navbar-title">
        <slot name="title">{{ title }}</slot>
      </div>

      <!-- 右侧区域 -->
      <div class="navbar-right">
        <slot name="right">
          <span v-if="rightText" class="right-text" @click="$emit('rightClick')">
            {{ rightText }}
          </span>
          <slot name="right-icon"></slot>
        </slot>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { inject } from 'vue';

defineProps({
  title: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: false
  },
  rightText: {
    type: String,
    default: ''
  }
});

defineEmits(['back', 'rightClick']);

const isDark = inject('isDark', false);

const handleBack = () => {
  window.history.back();
};
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 88rpx;
  padding-top: var(--status-bar-height, 20px);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.1);
  z-index: 100;
  transition: background 0.3s ease;
}

.navbar.dark {
  background: linear-gradient(135deg, #2d3748 0%, #1a202c 100%);
}

.navbar-content {
  width: 100%;
  max-width: 750rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
}

.navbar-left {
  width: 80rpx;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.back-icon {
  font-size: 48rpx;
  color: #fff;
  font-weight: bold;
  cursor: pointer;
  padding: 8rpx;
}

.back-icon:active {
  opacity: 0.7;
}

.navbar-title {
  flex: 1;
  text-align: center;
  color: #fff;
  font-size: 34rpx;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.navbar-right {
  width: 80rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.right-text {
  color: #fff;
  font-size: 28rpx;
  cursor: pointer;
  padding: 8rpx 16rpx;
}

.right-text:active {
  opacity: 0.7;
}
</style>