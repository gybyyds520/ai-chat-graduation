<template>
  <div class="privacy-container">
    <div class="privacy-header">
      <h1>隐私设置</h1>
      <p>管理您的隐私和安全选项</p>
    </div>
    
    <div class="privacy-sections">
      <div class="section">
        <h2>动态隐私</h2>
        <div class="setting-item">
          <div class="setting-label">
            <span>动态可见范围</span>
            <span class="setting-desc">控制谁可以看到您的动态</span>
          </div>
          <select v-model="settings.postVisibility" @change="saveSetting('postVisibility', settings.postVisibility)">
            <option value="public">公开</option>
            <option value="friends">仅好友</option>
            <option value="private">仅自己</option>
          </select>
        </div>
        
        <div class="setting-item">
          <div class="setting-label">
            <span>允许评论</span>
            <span class="setting-desc">允许他人评论您的动态</span>
          </div>
          <div class="toggle" :class="{ active: settings.allowComments }" @click="toggleSetting('allowComments')">
            <span class="toggle-circle"></span>
          </div>
        </div>
        
        <div class="setting-item">
          <div class="setting-label">
            <span>允许分享</span>
            <span class="setting-desc">允许他人分享您的动态</span>
          </div>
          <div class="toggle" :class="{ active: settings.allowShare }" @click="toggleSetting('allowShare')">
            <span class="toggle-circle"></span>
          </div>
        </div>
      </div>
      
      <div class="section">
        <h2>好友隐私</h2>
        <div class="setting-item">
          <div class="setting-label">
            <span>好友请求</span>
            <span class="setting-desc">谁可以向您发送好友请求</span>
          </div>
          <select v-model="settings.friendRequests" @change="saveSetting('friendRequests', settings.friendRequests)">
            <option value="everyone">所有人</option>
            <option value="friendsOfFriends">好友的好友</option>
            <option value="nobody">不允许</option>
          </select>
        </div>
        
        <div class="setting-item">
          <div class="setting-label">
            <span>在线状态</span>
            <span class="setting-desc">是否显示您的在线状态</span>
          </div>
          <div class="toggle" :class="{ active: settings.showOnlineStatus }" @click="toggleSetting('showOnlineStatus')">
            <span class="toggle-circle"></span>
          </div>
        </div>
        
        <div class="setting-item">
          <div class="setting-label">
            <span>互看好友列表</span>
            <span class="setting-desc">允许好友查看您的好友列表</span>
          </div>
          <div class="toggle" :class="{ active: settings.showFriendList }" @click="toggleSetting('showFriendList')">
            <span class="toggle-circle"></span>
          </div>
        </div>
      </div>
      
      <div class="section">
        <h2>消息隐私</h2>
        <div class="setting-item">
          <div class="setting-label">
            <span>陌生人消息</span>
            <span class="setting-desc">是否接收陌生人的消息</span>
          </div>
          <div class="toggle" :class="{ active: settings.allowStrangerMessages }" @click="toggleSetting('allowStrangerMessages')">
            <span class="toggle-circle"></span>
          </div>
        </div>
        
        <div class="setting-item">
          <div class="setting-label">
            <span>已读回执</span>
            <span class="setting-desc">发送已读回执给消息发送者</span>
          </div>
          <div class="toggle" :class="{ active: settings.sendReadReceipts }" @click="toggleSetting('sendReadReceipts')">
            <span class="toggle-circle"></span>
          </div>
        </div>
      </div>
      
      <div class="section">
        <h2>数据与安全</h2>
        <div class="setting-item">
          <div class="setting-label">
            <span>广告个性化</span>
            <span class="setting-desc">基于您的使用习惯推荐广告</span>
          </div>
          <div class="toggle" :class="{ active: settings.allowPersonalizedAds }" @click="toggleSetting('allowPersonalizedAds')">
            <span class="toggle-circle"></span>
          </div>
        </div>
        
        <div class="setting-item">
          <div class="setting-label">
            <span>位置信息</span>
            <span class="setting-desc">允许应用获取您的位置信息</span>
          </div>
          <div class="toggle" :class="{ active: settings.allowLocation }" @click="toggleSetting('allowLocation')">
            <span class="toggle-circle"></span>
          </div>
        </div>
        
        <div v-if="minorStatusText" class="setting-item minor-note">
          <div class="setting-label">
            <span>未成年保护模式</span>
            <span class="setting-desc">{{ minorStatusText }}</span>
          </div>
        </div>

        <button @click="exportData" class="btn-action">导出个人数据</button>
        <button @click="deleteAccount" class="btn-danger">删除账号</button>
      </div>
    </div>
    
    <div v-if="showExportModal" class="modal-overlay" @click="showExportModal = false">
      <div class="modal-content" @click.stop>
        <h2>导出个人数据</h2>
        <p>您的个人数据将以JSON格式导出，包含您的个人信息、动态、评论和消息记录。</p>
        <div class="modal-actions">
          <button @click="showExportModal = false" class="btn-cancel">取消</button>
          <button @click="confirmExport" class="btn-primary">确认导出</button>
        </div>
      </div>
    </div>
    
    <div v-if="showDeleteModal" class="modal-overlay" @click="showDeleteModal = false">
      <div class="modal-content danger" @click.stop>
        <h2>⚠️ 删除账号</h2>
        <p>此操作将永久删除您的账号及所有数据，包括动态、评论、消息和好友关系。此操作不可撤销！</p>
        <input v-model="deleteConfirm" placeholder="请输入 DELETE 确认删除" />
        <div class="modal-actions">
          <button @click="showDeleteModal = false" class="btn-cancel">取消</button>
          <button 
            :disabled="deleteConfirm !== 'DELETE'" 
            @click="confirmDelete" 
            class="btn-danger"
          >
            确认删除
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getCurrentUserId } from '@/utils/auth'
import { describeMinorModeStatus } from '@/utils/minorGuard'

const settings = reactive({
  postVisibility: 'public',
  allowComments: true,
  allowShare: true,
  friendRequests: 'everyone',
  showOnlineStatus: true,
  showFriendList: false,
  allowStrangerMessages: true,
  sendReadReceipts: true,
  allowPersonalizedAds: true,
  allowLocation: true
})

const showExportModal = ref(false)
const showDeleteModal = ref(false)
const deleteConfirm = ref('')

// 未成年保护模式状态（后端 GET /api/user/minor-mode/status）
const minorStatus = ref(null)
const minorStatusText = ref('')

const loadMinorStatus = async () => {
  try {
    const res = await request.get('/api/user/minor-mode/status')
    if (res.code === 200 && res.data) {
      minorStatus.value = res.data
      minorStatusText.value = describeMinorModeStatus(res.data).text
    }
  } catch (e) {
    // 未登录或后端未就绪时静默降级，不影响其它隐私设置
    minorStatusText.value = ''
  }
}

const loadSettings = async () => {
  try {
    const userId = getCurrentUserId()
    if (!userId) return
    const res = await request.get(`/api/privacy/settings?userId=${userId}`)
    if (res.code === 200 && res.data) {
      Object.assign(settings, res.data)
    }
  } catch (error) {
    console.error('加载隐私设置失败:', error)
  }
}

const saveSetting = async (key, value) => {
  try {
    const userId = getCurrentUserId()
    if (!userId) return
    await request.post('/api/privacy/setting', {
      userId,
      key,
      value: typeof value === 'boolean' ? value.toString() : value
    })
  } catch (error) {
    console.error('保存设置失败:', error)
  }
}

const toggleSetting = (key) => {
  settings[key] = !settings[key]
  saveSetting(key, settings[key])
}

const exportData = () => {
  showExportModal.value = true
}

const exporting = ref(false)

const confirmExport = async () => {
  if (exporting.value) return
  exporting.value = true
  try {
    // 后端 GET /api/user/data/export 返回当前登录用户的资料+对话+动态 JSON
    const res = await request.get('/api/user/data/export')
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.msg || '导出失败，请稍后重试')
      return
    }
    const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const stamp = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '')
    a.href = url
    a.download = my_data_.json
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('数据已导出为 JSON 文件')
    showExportModal.value = false
  } catch (e) {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

const deleteAccount = () => {
  showDeleteModal.value = true
}

const confirmDelete = () => {
  alert('账号删除功能正在开发中...')
  showDeleteModal.value = false
  deleteConfirm.value = ''
}

onMounted(() => {
  loadSettings()
  loadMinorStatus()
})
</script>

<style scoped>
.privacy-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.privacy-header {
  margin-bottom: 30px;
}

.privacy-header h1 {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 8px;
}

.privacy-header p {
  color: #999;
}

.privacy-sections {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}

.section h2 {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f8f8f8;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-label {
  flex: 1;
}

.setting-label span:first-child {
  display: block;
  font-weight: 500;
  margin-bottom: 4px;
}

.setting-desc {
  font-size: 12px;
  color: #999;
}

.setting-item select {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  font-size: 14px;
  min-width: 150px;
}

.toggle {
  width: 48px;
  height: 28px;
  background: #ddd;
  border-radius: 14px;
  cursor: pointer;
  position: relative;
  transition: background 0.3s;
}

.toggle.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.toggle-circle {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 24px;
  height: 24px;
  background: white;
  border-radius: 50%;
  transition: left 0.3s;
}

.toggle.active .toggle-circle {
  left: 22px;
}

.btn-action {
  width: 100%;
  padding: 12px;
  border: 1px solid #667eea;
  background: white;
  color: #667eea;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  margin-bottom: 12px;
}

.btn-action:hover {
  background: #f5f0ff;
}

.btn-danger {
  width: 100%;
  padding: 12px;
  border: none;
  background: #ff4d4f;
  color: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.btn-danger:hover {
  background: #d9363e;
}

.btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 400px;
}

.modal-content.danger {
  border: 2px solid #ff4d4f;
}

.modal-content h2 {
  margin-bottom: 12px;
}

.modal-content p {
  color: #666;
  margin-bottom: 20px;
  line-height: 1.6;
}

.modal-content input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-cancel {
  padding: 10px 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  cursor: pointer;
}

.btn-primary {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  background: #667eea;
  color: white;
  cursor: pointer;
}
</style>