<template>
  <div class="miniprogram-container" :class="{ 'dark': isDark }">
    <!-- 顶部导航栏 -->
    <NavBar 
      :title="currentPageTitle" 
      :showBack="showBackButton"
      :rightText="currentRightText"
      @back="handleBack"
      @rightClick="handleRightClick"
    />

    <!-- 主内容区域 -->
    <main class="content-area" ref="contentRef" @touchstart="handleTouchStart" @touchmove="handleTouchMove" @touchend="handleTouchEnd">
      <!-- 下拉刷新指示器 -->
      <div v-if="isRefreshing || isPulling" class="refresh-indicator" :style="{ opacity: Math.min(touchDistance / 60, 1) }">
        <div class="refresh-icon" :class="{ 'spinning': isRefreshing }"></div>
        <span>{{ refreshText }}</span>
      </div>

      <!-- 页面内容 -->
      <transition name="slide" mode="out-in">
        <component :is="currentPage" :key="currentIndex" />
      </transition>
    </main>

    <!-- 底部标签栏 -->
    <TabBar 
      :activeIndex="currentIndex" 
      :tabs="tabs"
      @change="handleTabChange"
    />

    <!-- Toast提示 -->
    <Transition name="toast">
      <div v-if="toast.visible" class="toast">{{ toast.message }}</div>
    </Transition>

    <!-- 加载遮罩 -->
    <div v-if="isLoading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import NavBar from './NavBar.vue'
import TabBar from './TabBar.vue'
import HomePage from './pages/HomePage.vue'
import ChatPage from './pages/ChatPage.vue'
import DiscoverPage from './pages/DiscoverPage.vue'
import ProfilePage from './pages/ProfilePage.vue'
import ChatDetailPage from './pages/ChatDetailPage.vue'
import ContentDetailPage from './pages/ContentDetailPage.vue'
import SearchPage from './pages/SearchPage.vue'
import NotificationPage from './pages/NotificationPage.vue'
import mpLogger from '@/utils/miniprogramLogger.js'

// 页面配置
const tabs = [
  { id: 0, name: '首页', icon: '🏠', component: HomePage },
  { id: 1, name: '聊天', icon: '💬', component: ChatPage },
  { id: 2, name: '发现', icon: '🔍', component: DiscoverPage },
  { id: 3, name: '我的', icon: '👤', component: ProfilePage }
]

// 响应式状态
const currentIndex = ref(0)
const isDark = ref(false)
const isLoading = ref(false)
const isRefreshing = ref(false)
const refreshText = ref('下拉刷新')
const toast = ref({ visible: false, message: '' })
const contentRef = ref(null)

// 页面栈管理
const pageStack = ref([])
const currentSubPage = ref(null)
const subPageTitle = ref('')

// 触摸事件相关
const touchStartY = ref(0)
const touchCurrentY = ref(0)
const touchDistance = ref(0)
const isPulling = ref(false)
const refreshStartTime = ref(null)

// 计算属性
const currentPage = computed(() => {
  if (currentSubPage.value) {
    mpLogger.debug('PageRender', `Rendering subpage: ${subPageTitle.value}`)
    return currentSubPage.value
  }
  mpLogger.debug('PageRender', `Rendering page: ${tabs[currentIndex.value].name}`)
  return tabs[currentIndex.value].component
})

const currentPageTitle = computed(() => {
  if (currentSubPage.value) {
    return subPageTitle.value
  }
  return tabs[currentIndex.value].name
})

const showBackButton = computed(() => {
  if (currentSubPage.value) return true
  return currentIndex.value !== 0
})

const currentRightText = computed(() => {
  if (currentSubPage.value) return ''
  switch (currentIndex.value) {
    case 0: return '搜索'
    case 1: return '+'
    case 2: return '更多'
    case 3: return '设置'
    default: return ''
  }
})

// Tab 切换处理
const handleTabChange = (index) => {
  const previousIndex = currentIndex.value
  const previousTab = tabs[previousIndex].name
  const newTab = tabs[index].name
  
  mpLogger.info('TabChange', 'Tab switch initiated', {
    fromIndex: previousIndex,
    toIndex: index,
    fromTab: previousTab,
    toTab: newTab,
    timestamp: new Date().toISOString()
  })
  
  // 切换Tab时清空子页面栈
  if (currentSubPage.value) {
    pageStack.value = []
    currentSubPage.value = null
    subPageTitle.value = ''
    mpLogger.debug('TabChange', 'Subpage stack cleared on tab switch')
  }
  
  currentIndex.value = index
  
  mpLogger.info('TabChange', 'Tab switch completed', {
    currentIndex: index,
    currentTab: newTab
  })
}

// 返回按钮处理
const handleBack = () => {
  if (currentSubPage.value) {
    // 返回上一页
    const prevPage = pageStack.value.pop()
    if (prevPage) {
      currentSubPage.value = prevPage.component
      subPageTitle.value = prevPage.title
    } else {
      currentSubPage.value = null
      subPageTitle.value = ''
    }
    mpLogger.info('Navigation', 'Back to previous page', {
      currentTitle: subPageTitle.value || tabs[currentIndex.value].name,
      stackSize: pageStack.value.length
    })
  } else {
    mpLogger.info('Navigation', 'Back button clicked at root', {
      currentIndex: currentIndex.value,
      currentTab: tabs[currentIndex.value].name
    })
    showToast('已是首页')
  }
}

// 右侧按钮点击处理
const handleRightClick = () => {
  if (currentSubPage.value) return
  
  const actions = ['search', 'newChat', 'more', 'settings']
  const action = actions[currentIndex.value]
  
  mpLogger.info('RightClick', `Right button clicked: ${action}`, {
    index: currentIndex.value,
    action: action
  })
  
  switch (action) {
    case 'search':
      navigateTo('搜索', SearchPage)
      break
    case 'newChat':
      navigateTo('聊天', ChatDetailPage)
      break
    case 'more':
      showToast('更多功能开发中')
      break
    case 'settings':
      navigateTo('通知', NotificationPage)
      break
  }
}

// 页面导航
const navigateTo = (title, component) => {
  mpLogger.info('Navigation', `Navigate to: ${title}`, {
    previousTitle: currentPageTitle.value,
    nextTitle: title,
    timestamp: new Date().toISOString()
  })
  
  pageStack.value.push({
    component: currentPage.value,
    title: currentPageTitle.value
  })
  
  currentSubPage.value = component
  subPageTitle.value = title
}

// 导航回首页
const navigateBack = () => {
  pageStack.value = []
  currentSubPage.value = null
  subPageTitle.value = ''
  
  mpLogger.info('Navigation', 'Navigate back to root', {
    timestamp: new Date().toISOString()
  })
}

// Toast 提示
const showToast = (message) => {
  mpLogger.debug('Toast', `Showing toast: ${message}`)
  
  toast.value = { visible: true, message }
  
  setTimeout(() => {
    toast.value.visible = false
    mpLogger.debug('Toast', 'Toast hidden')
  }, 2000)
}

// 加载状态
const showLoading = () => {
  mpLogger.info('Loading', 'Loading overlay shown')
  isLoading.value = true
}

const hideLoading = () => {
  mpLogger.info('Loading', 'Loading overlay hidden')
  isLoading.value = false
}

// 触摸开始处理
const handleTouchStart = (e) => {
  touchStartY.value = e.touches[0].clientY
  touchCurrentY.value = touchStartY.value
  touchDistance.value = 0
  isPulling.value = false
  
  mpLogger.logTouchEvent(e, 'start', {
    startY: touchStartY.value,
    isRefreshing: isRefreshing.value
  })
}

// 触摸移动处理
const handleTouchMove = (e) => {
  touchCurrentY.value = e.touches[0].clientY
  touchDistance.value = touchCurrentY.value - touchStartY.value
  
  if (touchDistance.value > 0 && !isRefreshing.value) {
    isPulling.value = true
    
    // 根据下拉距离更新提示文本
    if (touchDistance.value < 60) {
      refreshText.value = '下拉刷新'
    } else if (touchDistance.value < 100) {
      refreshText.value = '松开刷新'
    } else {
      refreshText.value = '松开立即刷新'
    }
    
    // 每20px记录一次日志，避免日志过多
    if (Math.floor(touchDistance.value / 20) !== Math.floor((touchDistance.value - (touchCurrentY.value - e.touches[0].clientY)) / 20)) {
      mpLogger.debug('TouchMove', 'Pull distance updated', {
        distance: Math.round(touchDistance.value),
        status: refreshText.value,
        threshold: 60
      })
    }
  }
}

// 触摸结束处理
const handleTouchEnd = () => {
  mpLogger.logTouchEvent({ type: 'touchend' }, 'end', {
    distance: Math.round(touchDistance.value),
    isPulling: isPulling.value,
    threshold: 60,
    willRefresh: isPulling.value && touchDistance.value >= 60 && !isRefreshing.value
  })
  
  if (isPulling.value && touchDistance.value >= 60 && !isRefreshing.value) {
    mpLogger.info('Refresh', 'Refresh triggered by pull gesture', {
      pullDistance: Math.round(touchDistance.value),
      threshold: 60
    })
    startRefresh()
  } else if (isPulling.value) {
    mpLogger.debug('Refresh', 'Pull cancelled', {
      pullDistance: Math.round(touchDistance.value),
      reason: touchDistance.value < 60 ? 'below threshold' : 'already refreshing'
    })
  }
  
  touchDistance.value = 0
  isPulling.value = false
}

// 开始刷新
const startRefresh = () => {
  refreshStartTime.value = Date.now()
  isRefreshing.value = true
  refreshText.value = '刷新中...'
  
  mpLogger.logRefreshEvent('started', {
    startTime: new Date().toISOString()
  })
  
  // 模拟刷新操作
  setTimeout(() => {
    const duration = Date.now() - refreshStartTime.value
    
    isRefreshing.value = false
    refreshText.value = '下拉刷新'
    refreshStartTime.value = null
    
    mpLogger.logRefreshEvent('completed', {
      duration: duration,
      endTime: new Date().toISOString()
    })
    
    showToast('刷新成功')
  }, 2000)
}

// 切换主题
const toggleTheme = () => {
  const previousTheme = isDark.value ? 'dark' : 'light'
  isDark.value = !isDark.value
  const newTheme = isDark.value ? 'dark' : 'light'
  
  mpLogger.info('Theme', 'Theme toggled', {
    from: previousTheme,
    to: newTheme
  })
  
  showToast(isDark.value ? '已切换深色模式' : '已切换浅色模式')
}

// 组件挂载
onMounted(() => {
  mpLogger.info('Lifecycle', 'MiniProgram component mounted', {
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent
  })
  
  showToast('欢迎使用AI Chat')
})

// 组件卸载
onUnmounted(() => {
  mpLogger.info('Lifecycle', 'MiniProgram component unmounted')
})

// 暴露方法供外部调用
defineExpose({
  toggleTheme,
  showToast,
  showLoading,
  hideLoading,
  navigateTo,
  navigateBack,
  getLogs: () => mpLogger.getLogs(),
  clearLogs: () => mpLogger.clearLogs()
})
</script>

<style scoped>
.miniprogram-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f5f7fa 0%, #e4e8ec 100%);
  transition: background 0.3s ease;
}

.miniprogram-container.dark {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
}

.content-area {
  flex: 1;
  overflow-y: auto;
  position: relative;
  -webkit-overflow-scrolling: touch;
}

.refresh-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 15px 0;
  color: #999;
  font-size: 14px;
  transition: opacity 0.2s ease;
}

.refresh-icon {
  width: 30px;
  height: 30px;
  border: 3px solid #ccc;
  border-top-color: #4f46e5;
  border-radius: 50%;
  margin-bottom: 8px;
  transition: transform 0.3s ease;
}

.refresh-icon.spinning {
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.toast {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  z-index: 1000;
}

.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -50%) scale(0.9);
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.miniprogram-container.dark .loading-overlay {
  background: rgba(26, 26, 46, 0.9);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

.loading-overlay span {
  color: #666;
  font-size: 14px;
}

.miniprogram-container.dark .loading-overlay span {
  color: #aaa;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-enter-from {
  transform: translateX(30px);
  opacity: 0;
}

.slide-leave-to {
  transform: translateX(-30px);
  opacity: 0;
}
</style>