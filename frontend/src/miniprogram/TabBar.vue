<template>
  <nav class="tabbar" :class="{ 'dark': isDark }">
    <div 
      v-for="(tab, index) in tabs" 
      :key="tab.id"
      class="tab-item"
      :class="{ 'active': activeIndex === index }"
      @click="handleClick(index)"
    >
      <span class="tab-icon">{{ tab.icon }}</span>
      <span class="tab-name">{{ tab.name }}</span>
      <span v-if="tab.badge" class="tab-badge">{{ tab.badge }}</span>
    </div>
  </nav>
</template>

<script setup>
import { inject } from 'vue'
import mpLogger from '@/utils/miniprogramLogger.js'

const props = defineProps({
  activeIndex: {
    type: Number,
    default: 0
  },
  tabs: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['change'])

const isDark = inject('isDark', false)

const handleClick = (index) => {
  // 记录点击事件
  mpLogger.info('TabBar', 'Tab item clicked', {
    clickedIndex: index,
    clickedTab: props.tabs[index]?.name || 'Unknown',
    currentIndex: props.activeIndex,
    isSameTab: index === props.activeIndex,
    timestamp: new Date().toISOString()
  })
  
  // 只有切换到不同的 Tab 才触发事件
  if (index !== props.activeIndex) {
    mpLogger.debug('TabBar', 'Emitting change event', {
      from: props.activeIndex,
      to: index
    })
    emit('change', index)
  } else {
    mpLogger.debug('TabBar', 'Same tab clicked, no change event')
  }
}
</script>

<style scoped>
.tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100rpx;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-around;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  z-index: 100;
  transition: background 0.3s ease;
}

.tabbar.dark {
  background: #1a1a2e;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  position: relative;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-item:active {
  transform: scale(0.95);
}

.tab-icon {
  font-size: 40rpx;
  margin-bottom: 8rpx;
  transition: transform 0.2s ease;
}

.tab-item.active .tab-icon {
  transform: scale(1.1);
}

.tab-name {
  font-size: 22rpx;
  color: #999;
  transition: color 0.2s ease;
}

.tab-item.active .tab-name {
  color: #667eea;
  font-weight: 500;
}

.tabbar.dark .tab-name {
  color: #666;
}

.tabbar.dark .tab-item.active .tab-name {
  color: #a78bfa;
}

.tab-badge {
  position: absolute;
  top: 8rpx;
  right: 50%;
  transform: translateX(20rpx);
  min-width: 32rpx;
  height: 32rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 20rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8rpx;
}
</style>