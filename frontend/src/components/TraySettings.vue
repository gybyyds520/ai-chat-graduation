<template>
  <div class="tray-settings">
    <div class="settings-header">
      <h2>系统托盘设置</h2>
      <p>自定义系统托盘图标的外观和行为</p>
    </div>

    <!-- 图标设置 -->
    <div class="settings-section">
      <h3 class="section-title">
        <span class="section-icon">🎨</span>
        图标设置
      </h3>
      
      <div class="setting-item">
        <label class="setting-label">图标类型</label>
        <div class="radio-group">
          <label class="radio-option">
            <input 
              type="radio" 
              v-model="localConfig.icon.type" 
              value="default"
              @change="updateConfig"
            />
            <span>默认图标</span>
          </label>
          <label class="radio-option">
            <input 
              type="radio" 
              v-model="localConfig.icon.type" 
              value="emoji"
              @change="updateConfig"
            />
            <span>Emoji图标</span>
          </label>
          <label class="radio-option">
            <input 
              type="radio" 
              v-model="localConfig.icon.type" 
              value="custom"
              @change="updateConfig"
            />
            <span>自定义图标</span>
          </label>
        </div>
      </div>

      <div v-if="localConfig.icon.type === 'emoji'" class="setting-item">
        <label class="setting-label">Emoji 选择</label>
        <input 
          type="text" 
          v-model="localConfig.icon.emoji" 
          class="emoji-input"
          placeholder="输入Emoji"
          maxlength="2"
          @input="updateConfig"
        />
        <div class="emoji-preview">
          <span class="preview-emoji">{{ localConfig.icon.emoji }}</span>
        </div>
      </div>

      <div v-if="localConfig.icon.type === 'custom'" class="setting-item">
        <label class="setting-label">自定义图标路径</label>
        <div class="file-input-wrapper">
          <input 
            type="text" 
            v-model="localConfig.icon.path" 
            class="path-input"
            placeholder="选择图标文件..."
            readonly
          />
          <button class="browse-btn" @click="selectCustomIcon">浏览</button>
        </div>
      </div>

      <div class="setting-item">
        <label class="setting-label">
          <input 
            type="checkbox" 
            v-model="localConfig.icon.showBadge"
            @change="updateConfig"
          />
          显示角标
        </label>
        <div v-if="localConfig.icon.showBadge" class="setting-item nested">
          <label class="setting-label">角标颜色</label>
          <input 
            type="color" 
            v-model="localConfig.icon.badgeColor"
            @input="updateConfig"
          />
        </div>
      </div>

      <div class="setting-item">
        <label class="setting-label">图标大小</label>
        <select 
          v-model="localConfig.icon.size" 
          @change="updateConfig"
          class="size-select"
        >
          <option :value="16">16x16</option>
          <option :value="24">24x24</option>
          <option :value="32">32x32</option>
        </select>
      </div>
    </div>

    <!-- 行为设置 -->
    <div class="settings-section">
      <h3 class="section-title">
        <span class="section-icon">⚙️</span>
        行为设置
      </h3>

      <div class="setting-item">
        <label class="setting-label">单击操作</label>
        <select 
          v-model="localConfig.behavior.singleClick" 
          @change="updateConfig"
          class="action-select"
        >
          <option value="show">显示窗口</option>
          <option value="hide">隐藏窗口</option>
          <option value="toggle">切换显示</option>
          <option value="menu">显示菜单</option>
        </select>
      </div>

      <div class="setting-item">
        <label class="setting-label">双击操作</label>
        <select 
          v-model="localConfig.behavior.doubleClick" 
          @change="updateConfig"
          class="action-select"
        >
          <option value="show">显示窗口</option>
          <option value="hide">隐藏窗口</option>
          <option value="toggle">切换显示</option>
        </select>
      </div>

      <div class="setting-item">
        <label class="setting-label">滚轮点击操作</label>
        <select 
          v-model="localConfig.behavior.middleClick" 
          @change="updateConfig"
          class="action-select"
        >
          <option value="newChat">新建对话</option>
          <option value="show">显示窗口</option>
          <option value="hide">隐藏窗口</option>
          <option value="toggle">切换显示</option>
        </select>
      </div>

      <div class="setting-item">
        <label class="setting-label">
          <input 
            type="checkbox" 
            v-model="localConfig.behavior.showNotification"
            @change="updateConfig"
          />
          最小化时显示通知
        </label>
      </div>

      <div class="setting-item">
        <label class="setting-label">
          <input 
            type="checkbox" 
            v-model="localConfig.behavior.flashOnMessage"
            @change="updateConfig"
          />
          收到消息时闪烁图标
        </label>
      </div>
    </div>

    <!-- 菜单设置 -->
    <div class="settings-section">
      <h3 class="section-title">
        <span class="section-icon">📋</span>
        右键菜单
      </h3>

      <div class="menu-options">
        <label class="menu-option">
          <input 
            type="checkbox" 
            v-model="localConfig.menu.showMainWindow"
            @change="updateConfig"
          />
          显示主窗口
        </label>
        <label class="menu-option">
          <input 
            type="checkbox" 
            v-model="localConfig.menu.showNewChat"
            @change="updateConfig"
          />
          新建对话
        </label>
        <label class="menu-option">
          <input 
            type="checkbox" 
            v-model="localConfig.menu.showDesktopElf"
            @change="updateConfig"
          />
          桌面小精灵
        </label>
        <label class="menu-option">
          <input 
            type="checkbox" 
            v-model="localConfig.menu.showSettings"
            @change="updateConfig"
          />
          设置
        </label>
        <label class="menu-option">
          <input 
            type="checkbox" 
            v-model="localConfig.menu.showExit"
            @change="updateConfig"
          />
          退出
        </label>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="settings-footer">
      <button class="btn btn-secondary" @click="resetConfig">
        恢复默认
      </button>
      <button class="btn btn-primary" @click="applyConfig">
        应用设置
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'

const localConfig = reactive({
  icon: {
    type: 'default',
    path: '',
    emoji: '🤖',
    size: 16,
    color: '#4f46e5',
    showBadge: true,
    badgeColor: '#ff4d4f'
  },
  behavior: {
    singleClick: 'show',
    doubleClick: 'show',
    middleClick: 'newChat',
    showNotification: true,
    flashOnMessage: true
  },
  menu: {
    showMainWindow: true,
    showNewChat: true,
    showDesktopElf: true,
    showSettings: true,
    showExit: true,
    customItems: []
  }
})

const loading = ref(false)

onMounted(() => {
  loadConfig()
})

const loadConfig = async () => {
  try {
    const config = window.api.invoke('get-tray-config')
    if (config) {
      Object.assign(localConfig, config)
    }
  } catch (e) {
    console.error('加载托盘配置失败:', e)
  }
}

const updateConfig = () => {
  console.log('托盘配置已更新:', localConfig)
}

const selectCustomIcon = async () => {
  try {
    const result = await window.api.invoke('show-open-dialog', {
      title: '选择图标文件',
      filters: [
        { name: '图像文件', extensions: ['png', 'ico', 'jpg', 'jpeg', 'svg'] },
        { name: '所有文件', extensions: ['*'] }
      ]
    })
    
    if (!result.canceled && result.filePaths.length > 0) {
      localConfig.icon.path = result.filePaths[0]
      localConfig.icon.type = 'custom'
    }
  } catch (e) {
    console.error('选择图标文件失败:', e)
  }
}

const applyConfig = async () => {
  loading.value = true
  try {
    const success = window.api.invoke('update-tray-config', JSON.parse(JSON.stringify(localConfig)))
    if (success) {
      alert('托盘设置已应用')
    } else {
      alert('应用设置失败')
    }
  } catch (e) {
    console.error('应用托盘配置失败:', e)
    alert('应用设置失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const resetConfig = () => {
  if (confirm('确定要恢复默认设置吗？')) {
    Object.assign(localConfig, {
      icon: {
        type: 'default',
        path: '',
        emoji: '🤖',
        size: 16,
        color: '#4f46e5',
        showBadge: true,
        badgeColor: '#ff4d4f'
      },
      behavior: {
        singleClick: 'show',
        doubleClick: 'show',
        middleClick: 'newChat',
        showNotification: true,
        flashOnMessage: true
      },
      menu: {
        showMainWindow: true,
        showNewChat: true,
        showDesktopElf: true,
        showSettings: true,
        showExit: true,
        customItems: []
      }
    })
    applyConfig()
  }
}
</script>

<style scoped>
.tray-settings {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px;
}

.settings-header {
  margin-bottom: 32px;
}

.settings-header h2 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #1e293b;
}

.settings-header p {
  margin: 0;
  color: #64748b;
}

.settings-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 20px 0;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 20px;
}

.setting-item {
  margin-bottom: 20px;
}

.setting-item:last-child {
  margin-bottom: 0;
}

.setting-item.nested {
  margin-left: 24px;
}

.setting-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  margin-bottom: 8px;
}

.setting-label input[type="checkbox"] {
  margin-right: 8px;
  cursor: pointer;
}

.radio-group {
  display: flex;
  gap: 20px;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #64748b;
}

.radio-option input[type="radio"] {
  cursor: pointer;
}

.emoji-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 16px;
  font-family: inherit;
}

.emoji-preview {
  margin-top: 12px;
  text-align: center;
}

.preview-emoji {
  font-size: 48px;
}

.file-input-wrapper {
  display: flex;
  gap: 10px;
}

.path-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  background: #f8fafc;
}

.browse-btn {
  padding: 10px 20px;
  background: #667eea;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.browse-btn:hover {
  background: #5a6fd6;
}

input[type="color"] {
  width: 60px;
  height: 40px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.size-select,
.action-select {
  width: 200px;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  cursor: pointer;
}

.menu-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.menu-option {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #475569;
  cursor: pointer;
}

.menu-option input[type="checkbox"] {
  cursor: pointer;
}

.settings-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: #667eea;
  color: #fff;
}

.btn-primary:hover {
  background: #5a6fd6;
}

.btn-secondary {
  background: #f1f5f9;
  color: #64748b;
}

.btn-secondary:hover {
  background: #e2e8f0;
}

/* 深色模式适配 */
.dark .settings-header h2 {
  color: #f8fafc;
}

.dark .settings-header p {
  color: #94a3b8;
}

.dark .settings-section {
  background: #1e293b;
}

.dark .section-title {
  color: #f1f5f9;
}

.dark .setting-label {
  color: #cbd5e1;
}

.dark .radio-option {
  color: #94a3b8;
}

.dark .emoji-input {
  background: #334155;
  border-color: #475569;
  color: #f1f5f9;
}

.dark .path-input {
  background: #334155;
  border-color: #475569;
  color: #f1f5f9;
}

.dark .size-select,
.dark .action-select {
  background: #334155;
  border-color: #475569;
  color: #f1f5f9;
}

.dark .menu-option {
  color: #cbd5e1;
}

.dark .btn-secondary {
  background: #334155;
  color: #94a3b8;
}

.dark .btn-secondary:hover {
  background: #475569;
}
</style>