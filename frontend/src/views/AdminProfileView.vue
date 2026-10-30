<template>
  <div class="admin-profile-container">
    <div class="profile-header-bar">
      <button class="back-btn" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        返回
      </button>
      <h1>个人中心</h1>
      <div class="header-spacer"></div>
    </div>

    <div class="admin-profile-header">
      <div class="profile-card">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <div class="avatar admin-avatar" :style="{ background: adminProfile.avatar ? 'none' : 'linear-gradient(135deg, #f59e0b 0%, #ef4444 100%)' }">
              <img v-if="adminProfile.avatar" :src="adminProfile.avatar" alt="头像" />
              <span v-else>{{ adminProfile.username?.charAt(0) || 'A' }}</span>
            </div>
            <button class="change-avatar-btn" @click="triggerAvatarUpload">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 15v-6M3 21h18M16 3.13a4 4 0 0 1 0 7.75M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              </svg>
            </button>
            <input
              ref="avatarInput"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              class="avatar-input"
              @change="handleAvatarChange"
            />
          </div>
          <h2>{{ adminProfile.nickname || adminProfile.username }}</h2>
          <p class="admin-badge">管理员</p>
        </div>
      </div>
    </div>

    <div class="admin-profile-content">
      <div class="profile-tabs">
        <div 
          :class="['tab-item', { active: activeTab === 'info' }]"
          @click="activeTab = 'info'"
        >
          管理员资料
        </div>
        <div 
          :class="['tab-item', { active: activeTab === 'password' }]"
          @click="activeTab = 'password'"
        >
          修改密码
        </div>
      </div>

      <div class="tab-content">
        <div v-if="activeTab === 'info'" class="info-form">
          <h3>编辑管理员资料</h3>
          <el-form :model="editForm" label-width="120px">
            <el-form-item label="管理员账号">
              <el-input v-model="editForm.username" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" type="email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="editForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile" :loading="saving">保存修改</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="activeTab === 'password'" class="password-form">
          <h3>修改管理员密码</h3>
          <el-form :model="passwordForm" label-width="140px">
            <el-form-item label="当前密码">
              <el-input v-model="passwordForm.currentPassword" type="password" placeholder="请输入当前密码" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input 
                v-model="passwordForm.newPassword" 
                type="password" 
                placeholder="请输入新密码（至少8位）"
                @input="checkPasswordStrength"
              />
              <div v-if="passwordForm.newPassword" class="password-strength">
                <div class="strength-label">密码强度：</div>
                <div class="strength-bars">
                  <div 
                    v-for="(bar, index) in 4" 
                    :key="index"
                    :class="['strength-bar', getStrengthClass(index)]"
                  ></div>
                </div>
                <div class="strength-text" :class="getStrengthTextClass()">
                  {{ passwordStrengthText }}
                </div>
              </div>
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
              <div v-if="passwordForm.confirmPassword && passwordForm.newPassword" class="confirm-hint">
                <span v-if="passwordForm.newPassword === passwordForm.confirmPassword" class="hint-success">✓ 两次输入一致</span>
                <span v-else class="hint-error">✗ 两次输入不一致</span>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword" :loading="changingPassword">确认修改</el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import logger from '@/utils/logger'

const router = useRouter()

const activeTab = ref('info')
const saving = ref(false)
const changingPassword = ref(false)
const adminId = ref(null)
const avatarInput = ref(null)
const passwordStrength = ref(0)
const passwordStrengthText = ref('')

const adminProfile = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})

const editForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const goBack = () => {
  router.back()
}

const triggerAvatarUpload = () => {
  avatarInput.value?.click()
}

const handleAvatarChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('请上传图片文件')
    return
  }

  const maxSize = 5 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }

  try {
    const formData = new FormData()
    formData.append('file', file)

    // 复用已存在的通用头像上传接口（管理员同样是登录用户，token 指向管理员 User 记录）
    const response = await request.post('/api/user/upload-avatar', formData)

    if (response.code === 200 && response.data) {
      const avatarUrl = response.data.filePath || response.data.avatarUrl
      adminProfile.avatar = avatarUrl
      ElMessage.success('头像上传成功')

      const savedUser = localStorage.getItem('user')
      if (savedUser) {
        const parsed = JSON.parse(savedUser)
        parsed.avatar = avatarUrl
        localStorage.setItem('user', JSON.stringify(parsed))
      }
    } else {
      ElMessage.error(response.msg || '头像上传失败')
    }
  } catch (error) {
    logger.error('上传头像失败:', error)
    ElMessage.error('头像上传失败，请稍后重试')
  }

  event.target.value = ''
}

const checkPasswordStrength = (value) => {
  let strength = 0
  
  if (value.length >= 8) strength++
  if (value.length >= 12) strength++
  if (/[A-Z]/.test(value)) strength++
  if (/[a-z]/.test(value)) strength++
  if (/[0-9]/.test(value)) strength++
  if (/[^A-Za-z0-9]/.test(value)) strength++

  passwordStrength.value = Math.min(strength, 4)

  const texts = ['', '弱', '中等', '强', '非常强']
  passwordStrengthText.value = texts[passwordStrength.value] || ''
}

const getStrengthClass = (index) => {
  if (index < passwordStrength.value) {
    if (passwordStrength.value <= 1) return 'weak'
    if (passwordStrength.value === 2) return 'medium'
    return 'strong'
  }
  return ''
}

const getStrengthTextClass = () => {
  if (passwordStrength.value <= 1) return 'weak'
  if (passwordStrength.value === 2) return 'medium'
  return 'strong'
}

const loadAdminProfile = async () => {
  try {
    const savedUser = localStorage.getItem('user')
    if (savedUser) {
      const parsed = JSON.parse(savedUser)
      adminId.value = parsed.id
      adminProfile.username = parsed.username || ''
      adminProfile.nickname = parsed.nickname || parsed.username || ''
      adminProfile.email = parsed.email || ''
      adminProfile.phone = parsed.phone || ''
      adminProfile.avatar = parsed.avatar || ''

      editForm.username = adminProfile.username
      editForm.nickname = adminProfile.nickname
      editForm.email = adminProfile.email
      editForm.phone = adminProfile.phone
    }

    // 复用当前登录用户接口（管理员 token 指向自身 User 记录），服务端无 /api/admin/profile
    const response = await request.get('/api/user/me')
    if (response.code === 200 && response.data) {
      Object.assign(adminProfile, response.data)
      Object.assign(editForm, response.data)
    }
  } catch (error) {
    logger.error('加载管理员资料失败:', error)
  }
}

const saveProfile = async () => {
  if (!editForm.nickname) {
    ElMessage.warning('请输入昵称')
    return
  }

  saving.value = true
  try {
    const response = await request.post('/api/admin/update-profile', {
      adminId: adminId.value,
      nickname: editForm.nickname,
      email: editForm.email
    })

    if (response.code === 200) {
      ElMessage.success('管理员资料更新成功')
      Object.assign(adminProfile, editForm)

      const savedUser = localStorage.getItem('user')
      if (savedUser) {
        const parsed = JSON.parse(savedUser)
        parsed.nickname = editForm.nickname
        parsed.email = editForm.email
        parsed.phone = editForm.phone
        localStorage.setItem('user', JSON.stringify(parsed))
      }
    } else {
      ElMessage.error(response.msg || '更新失败')
    }
  } catch (error) {
    logger.error('保存管理员资料失败:', error)
    ElMessage.error('更新失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const resetForm = () => {
  editForm.nickname = adminProfile.nickname
  editForm.email = adminProfile.email
  editForm.phone = adminProfile.phone
}

const changePassword = async () => {
  if (!passwordForm.currentPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 8) {
    ElMessage.warning('新密码至少需要8位')
    return
  }
  if (passwordStrength.value <= 1) {
    ElMessage.warning('密码强度太弱，请使用更复杂的密码')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  changingPassword.value = true
  try {
    const response = await request.post('/api/admin/change-password', {
      adminId: adminId.value,
      oldPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    })

    if (response.code === 200) {
      ElMessage.success('密码修改成功')
      resetPasswordForm()
    } else {
      ElMessage.error(response.msg || '密码修改失败')
    }
  } catch (error) {
    logger.error('修改管理员密码失败:', error)
    ElMessage.error('密码修改失败，请检查当前密码是否正确')
  } finally {
    changingPassword.value = false
  }
}

const resetPasswordForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordStrength.value = 0
  passwordStrengthText.value = ''
}

onMounted(() => {
  loadAdminProfile()
})
</script>

<style scoped>
.admin-profile-container {
  min-height: 100vh;
  background: var(--bg-secondary);
}

.profile-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--border-primary);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.back-btn:hover {
  background: var(--bg-hover);
}

.profile-header-bar h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-spacer {
  width: 80px;
}

.admin-profile-header {
  padding: var(--spacing-xl) var(--spacing-lg);
}

.profile-card {
  background: var(--gradient-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xl);
}

.avatar-section {
  text-align: center;
}

.avatar-wrapper {
  position: relative;
  margin-bottom: var(--spacing-md);
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 700;
  color: white;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.admin-avatar {
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
}

.change-avatar-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-primary);
  border: 3px solid white;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.change-avatar-btn:hover {
  background: var(--color-primary-hover);
  transform: scale(1.1);
}

.avatar-input {
  display: none;
}

.avatar-section h2 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.admin-badge {
  display: inline-block;
  padding: 4px 12px;
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.admin-profile-content {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 var(--spacing-lg) var(--spacing-lg);
}

.profile-tabs {
  display: flex;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: 4px;
  margin-bottom: var(--spacing-lg);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tab-item.active {
  background: var(--gradient-primary);
  color: white;
}

.tab-content {
  background: var(--gradient-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
}

.info-form h3,
.password-form h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-lg);
}

.el-form-item {
  margin-bottom: var(--spacing-lg);
}

.el-input,
.el-textarea {
  width: 100%;
}

.el-button {
  margin-right: var(--spacing-md);
}

.password-strength {
  margin-top: 8px;
}

.strength-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.strength-bars {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
}

.strength-bar {
  flex: 1;
  height: 6px;
  background: var(--border-secondary);
  border-radius: 3px;
  transition: all 0.3s ease;
}

.strength-bar.weak {
  background: #ef4444;
}

.strength-bar.medium {
  background: #f59e0b;
}

.strength-bar.strong {
  background: #22c55e;
}

.strength-text {
  font-size: 12px;
}

.strength-text.weak {
  color: #ef4444;
}

.strength-text.medium {
  color: #f59e0b;
}

.strength-text.strong {
  color: #22c55e;
}

.confirm-hint {
  margin-top: 8px;
  font-size: 12px;
}

.hint-success {
  color: #22c55e;
}

.hint-error {
  color: #ef4444;
}
</style>