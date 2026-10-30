<template>
  <div id="app" :class="{ 'custom-titlebar': isElectron, 'window-maximized': isMaximized }">
    <!-- 自定义标题栏（仅Electron环境） -->
    <div v-if="isElectron && showTitleBar" class="custom-titlebar-wrapper">
      <div class="titlebar-left">
        <div class="titlebar-icon">
          <svg width="20" height="20" viewBox="0 0 48 48" fill="none">
            <rect width="48" height="48" rx="12" fill="url(#titlebarLogoGrad)"/>
            <path d="M24 14C18.48 14 14 18.48 14 24s4.48 10 10 10 10-4.48 10-10S29.52 14 24 14zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z" fill="white"/>
            <defs>
              <linearGradient id="titlebarLogoGrad" x1="0" y1="0" x2="48" y2="48">
                <stop stop-color="#4f46e5"/>
                <stop offset="1" stop-color="#7c3aed"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <span class="titlebar-title">AI Chat</span>
      </div>
      <div class="titlebar-center">
        <span class="titlebar-subtitle">{{ currentPageTitle }}</span>
      </div>
      <div class="titlebar-right">
        <VersionInfo />
        <div class="titlebar-chandelier">
          <ChandelierToggle size="sm" />
        </div>
        <button class="titlebar-btn" @click.stop="minimizeWindow" title="最小化">
          <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
            <path d="M1 5h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>
        <button class="titlebar-btn" @click.stop="maximizeWindow" :title="isMaximized ? '还原' : '最大化'">
          <svg v-if="!isMaximized" width="10" height="10" viewBox="0 0 10 10" fill="none">
            <rect x="1" y="1" width="8" height="8" stroke="currentColor" stroke-width="1.5" rx="1"/>
          </svg>
          <svg v-else width="12" height="12" viewBox="0 0 12 12" fill="none">
            <rect x="0" y="0" width="12" height="12" stroke="currentColor" stroke-width="1.5" rx="1"/>
            <rect x="3" y="3" width="6" height="6" stroke="currentColor" stroke-width="1.5" rx="1"/>
          </svg>
        </button>
        <button class="titlebar-btn close-btn" @click.stop="closeWindow" title="关闭">
          <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
            <path d="M1 1l8 8M9 1l-8 8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
    </div>
    
    <!-- 全局粒子背景（科技感动态氛围层） -->
    <ParticleEffect />
    
    <!-- 全局用户端背景层：山云背景 + 随时更换背景 + 重播入场动画 -->
    <UserBgManager />
    
    <!-- 鼠标霓虹光晕（桌面端专属） -->
    <MouseGlow />
    
    <!-- 页面路由（带切换过渡动画） -->
    <Transition name="page-fade" mode="out-in">
      <router-view />
    </Transition>
    
    <ChandelierToggle v-if="!isElectron" draggable />
    
    <DesktopElfCharacter v-if="elfVisible" />
    
    <!-- 通知提示 -->
    <Transition name="fade">
      <div v-if="notification.visible" class="notification-toast" :class="notification.type">
        <svg v-if="notification.type === 'success'" class="notification-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 11.08V12a10 10 0 11-5.93-9.14"/>
          <polyline points="22 4 12 14.01 9 11.01"/>
        </svg>
        <svg v-else-if="notification.type === 'error'" class="notification-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="15" y1="9" x2="9" y2="15"/>
          <line x1="9" y1="9" x2="15" y2="15"/>
        </svg>
        <svg v-else-if="notification.type === 'warning'" class="notification-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <path d="M12 6v6l4 2"/>
        </svg>
        <svg v-else class="notification-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <path d="M12 16v-4M12 8h.01"/>
        </svg>
        <span class="notification-message">{{ notification.message }}</span>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useTheme } from '@/composables/useTheme';
import DesktopElfCharacter from '@/components/DesktopElfCharacter.vue';
import ChandelierToggle from '@/components/ChandelierToggle.vue';
import VersionInfo from '@/components/VersionInfo.vue';
import ParticleEffect from '@/components/ParticleEffect.vue';
import MouseGlow from '@/components/MouseGlow.vue';
import UserBgManager from '@/components/UserBgManager.vue';
import versionService from '@/utils/versionService';
import { useElfConfig } from '@/utils/elfBridge';
import logger from '@/utils/logger.js';

const { currentTheme, initTheme } = useTheme();
const route = useRoute();
const router = useRouter();

// 桌面精灵全局配置（设计器关闭后全局不渲染）
const { config: elfConfig } = useElfConfig();
const elfVisible = computed(() => elfConfig.value.visible !== false);

const isElectron = ref(false);
const showTitleBar = ref(true);
const isMaximized = ref(false);

const notification = ref({
  visible: false,
  type: 'info',
  message: ''
});

const currentPageTitle = ref('');

const pageTitles = {
  'landing': '欢迎',
  'login': '登录',
  'register': '注册',
  'chat': 'AI 对话',
  'friends': '好友管理',
  'friendChat': '好友聊天',
  'characterImport': '导入角色',
  'createCharacter': '创建角色',
  'admin': '管理后台',
  'datapool': '数据池',
  'feed': 'AI投喂',
  'desktopElf': '桌面小精灵',
  'socialFeed': '社交动态',
  'notifications': '通知中心',
  'privacy': '隐私设置',
  'elf': 'AI小精灵',
  'community': '兴趣社群'
};

function showNotification(type, message) {
  notification.value = { visible: true, type, message };
  setTimeout(() => {
    notification.value.visible = false;
  }, 3000);
}

function minimizeWindow() {
  try {
    if (window.electronAPI) {
      window.electronAPI.minimizeWindow();
    }
  } catch (e) {
    logger.error('Failed to minimize window:', e);
  }
}

function maximizeWindow() {
  try {
    if (window.electronAPI) {
      isMaximized.value = !isMaximized.value;
      window.electronAPI.maximizeWindow();
    }
  } catch (e) {
    logger.error('Failed to maximize window:', e);
    isMaximized.value = !isMaximized.value;
  }
}

function closeWindow() {
  try {
    if (window.electronAPI) {
      window.electronAPI.closeWindow();
    }
  } catch (e) {
    logger.error('Failed to close window:', e);
  }
}

function checkElectronEnvironment() {
  try {
    isElectron.value = typeof window !== 'undefined' && window.electronAPI !== undefined;
    if (isElectron.value) {
      const version = window.electronAPI.getAppVersion();
      logger.info(`AI Chat v${version} - Electron模式`);
    }
  } catch (e) {
    logger.error('Failed to check Electron environment:', e);
    isElectron.value = false;
  }
}

function updatePageTitle() {
  currentPageTitle.value = pageTitles[route.name] || '';
}

watch(() => route.name, updatePageTitle);

onMounted(() => {
  try {
    initTheme();
    checkElectronEnvironment();
    updatePageTitle();
  } catch (e) {
    logger.error('Failed during mount:', e);
  }
});
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-primary);
  background: var(--bg-secondary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  transition: background-color 0.3s ease, color 0.3s ease;
  /* 移动端：禁止双击缩放/长按菜单抢占点击，点击立即响应 */
  touch-action: manipulation;
}

body {
  min-height: 100vh;
}

#app {
  min-height: 100vh;
  background: var(--bg-secondary);
  transition: background-color 0.3s ease;
}



/* 自定义标题栏 */
.custom-titlebar {
  padding-top: 40px;
}

.custom-titlebar-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: #1e293b;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0;
  -webkit-app-region: drag;
  z-index: 99999;
  border-bottom: 1px solid #334155;
  box-sizing: border-box;
  overflow: hidden;
  transform: translateZ(0);
  backface-visibility: hidden;
}

.window-maximized .custom-titlebar-wrapper {
  border-top-left-radius: 0;
  border-top-right-radius: 0;
  padding: 0;
  height: 40px;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  margin: 0 !important;
}

.window-maximized .titlebar-btn {
  height: 40px;
}

.window-maximized .custom-titlebar {
  padding-top: 40px;
}

.titlebar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 12px;
  -webkit-app-region: drag;
  flex-shrink: 0;
}

.titlebar-icon {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  -webkit-app-region: drag;
}

.titlebar-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  -webkit-app-region: drag;
}

.titlebar-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  -webkit-app-region: drag;
}

.titlebar-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  -webkit-app-region: drag;
}

.titlebar-right {
  display: flex;
  gap: 0;
  -webkit-app-region: no-drag;
  flex-shrink: 0;
  position: relative;
  z-index: 100000;
  transform: translateZ(0);
}

.titlebar-chandelier {
  width: 46px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  -webkit-app-region: no-drag;
  cursor: pointer;
  transition: background-color 0.15s ease;
  overflow: visible;
}

.titlebar-btn {
  width: 46px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  color: #d1d5db;
  cursor: default;
  transition: background-color 0.15s ease, color 0.15s ease;
  font-size: 14px;
  padding: 0;
  border-radius: 0;
  overflow: hidden;
  position: relative;
  z-index: 100000;
  -webkit-app-region: no-drag;
  box-sizing: border-box;
  line-height: 1;
  text-align: center;
  user-select: none;
  -webkit-user-select: none;
  transform: translateZ(0);
  backface-visibility: hidden;
}

.titlebar-btn:focus {
  outline: none;
}

.titlebar-btn svg {
  pointer-events: none;
  flex-shrink: 0;
}

.titlebar-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #e5e7eb;
}

.titlebar-btn:active {
  background: rgba(255, 255, 255, 0.18);
}

.titlebar-btn.close-btn:hover {
  background: #ef4444;
  color: white;
}

.titlebar-btn.close-btn:active {
  background: #dc2626;
}

a {
  color: var(--color-primary);
  text-decoration: none;
  transition: color 0.2s ease;
}

a:hover {
  color: var(--color-primary-hover);
}

button {
  font-family: inherit;
  cursor: pointer;
}

input, textarea, select {
  font-family: inherit;
  font-size: inherit;
}

/* Element Plus 主题适配 */
.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-hover);
  --el-button-hover-border-color: var(--color-primary-hover);
  --el-button-text-color: var(--text-inverse);
}

.el-message-box {
  border-radius: 12px !important;
  background: var(--card-bg) !important;
  border: 1px solid var(--border-primary) !important;
}

.el-dialog {
  border-radius: 16px !important;
  background: var(--card-bg) !important;
}

.el-input__wrapper {
  border-radius: 8px !important;
  background: var(--input-bg) !important;
  box-shadow: 0 0 0 1px var(--input-border) !important;
}

.el-input__wrapper:hover {
  box-shadow: 0 0 0 1px var(--input-focus-border) !important;
}

.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px var(--input-focus-border) !important;
}

.el-button {
  border-radius: 8px !important;
  font-weight: 500;
}

.el-tag {
  border-radius: 6px !important;
}

/* 暗色模式下的Element Plus适配 */
.dark .el-dialog__header {
  border-bottom: 1px solid var(--border-primary);
}

.dark .el-dialog__footer {
  border-top: 1px solid var(--border-primary);
}

.dark .el-table {
  --el-table-bg-color: var(--card-bg);
  --el-table-tr-bg-color: var(--card-bg);
  --el-table-header-bg-color: var(--bg-tertiary);
  --el-table-row-hover-bg-color: var(--bg-hover);
  --el-table-border-color: var(--border-primary);
  --el-table-text-color: var(--text-primary);
}

/* 入场动画播放期间隐藏昼夜切换吊灯（overlay 出现时不可见、不可点） */
body:has(.entrance-overlay) .chandelier-toggle-container {
  opacity: 0;
  pointer-events: none;
}

/* ===== 全局用户端山云背景：登录后页面根容器透明，露出 UserBgManager 背景层 ===== */
.chat-page,
.friend-container,
.friend-chat-container,
.group-chat-page,
.community-container,
.plaza-container,
.memory-view,
.feed-module,
.notification-container,
.profile-container,
.create-character-container,
.character-import-container,
.elf-designer,
.social-feed-container,
.privacy-container,
.announcement-container,
.kb-container,
.research-view,
.elf-container,
.membership-container,
.upgrade-container,
.admin-container,
.datapool-container,
.admin-profile-container {
  background: transparent !important;
}

/* 通知提示 */
.notification-toast {
  position: fixed;
  bottom: 24px;
  left: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  background: var(--card-bg);
  border-radius: 12px;
  box-shadow: var(--shadow-lg);
  z-index: 10000;
  min-width: 280px;
  border: 1px solid var(--border-primary);
}

.notification-toast.success {
  border-color: var(--color-success);
}

.notification-toast.error {
  border-color: var(--color-danger);
}

.notification-toast.warning {
  border-color: var(--color-warning);
}

.notification-icon {
  flex-shrink: 0;
}

.notification-toast.success .notification-icon {
  color: var(--color-success);
}

.notification-toast.error .notification-icon {
  color: var(--color-danger);
}

.notification-toast.warning .notification-icon {
  color: var(--color-warning);
}

.notification-message {
  font-size: 14px;
  color: var(--text-primary);
}

/* 通知动画 */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* 页面切换过渡动画 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.28s ease, transform 0.28s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* 全局滚动条美化（科幻感） */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: var(--border-primary, #334155);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--color-primary, #6366f1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  html, body {
    font-size: 13px;
  }

  .custom-titlebar-wrapper {
    height: 44px;
  }

  .custom-titlebar {
    padding-top: 44px;
  }

  .titlebar-subtitle {
    display: none;
  }

  .notification-toast {
    left: 16px;
    right: 16px;
    min-width: auto;
    bottom: 16px;
  }
}

@media (max-width: 480px) {
  html, body {
    font-size: 12px;
  }

  .el-dialog {
    margin: 16px !important;
    width: calc(100% - 32px) !important;
    max-width: 100%;
  }

  .el-table {
    font-size: 12px;
  }

  .el-button {
    padding: 8px 12px;
    font-size: 13px;
  }

  .el-input__wrapper {
    padding: 6px 12px;
  }

  .notification-toast {
    padding: 10px 16px;
    font-size: 13px;
  }
}

@media (max-width: 320px) {
  html, body {
    font-size: 11px;
  }
}
</style>