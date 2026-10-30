<template>
  <div class="profile-container">
    <button class="back-btn" @click="goBack">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M19 12H5M12 19l-7-7 7-7"/>
      </svg>
      返回
    </button>
    <div class="profile-header">
      <div class="profile-card">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <div class="avatar" :style="{ background: userProfile.avatar ? 'none' : 'var(--gradient-primary)' }">
              <img v-if="userProfile.avatar" :src="userProfile.avatar" alt="" />
              <span v-else>{{ userProfile.nickname?.charAt(0) || 'U' }}</span>
            </div>
            <button class="change-avatar-btn" :disabled="avatarUploading" title="更换头像" @click="triggerAvatarUpload">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 15v-6M3 21h18M16 3.13a4 4 0 0 1 0 7.75M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              </svg>
            </button>
          </div>
          <h2>{{ userProfile.nickname || userProfile.username }}</h2>
          <p class="user-id">ID: {{ userProfile.id }}</p>
        </div>
        <div class="profile-stats">
          <div class="stat-item">
            <span class="stat-value">{{ userStats.chatCount || 0 }}</span>
            <span class="stat-label">对话次数</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userStats.characterCount || 0 }}</span>
            <span class="stat-label">AI角色</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userStats.friendCount || 0 }}</span>
            <span class="stat-label">好友数量</span>
          </div>
        </div>
      </div>
    </div>

    <!-- M2 ⑦ 每日签到打卡 -->
    <div class="checkin-card">
      <div class="checkin-left">
        <div class="checkin-title">每日签到</div>
        <div class="checkin-sub">已连续签到 <b>{{ checkinStatus.consecutiveDays || 0 }}</b> 天 · 今日奖励 +{{ checkinStatus.todayReward || 5 }}</div>
      </div>
      <el-button type="primary" :disabled="checkinStatus.checkedToday" @click="doCheckin">
        {{ checkinStatus.checkedToday ? '今日已签到' : '签到领取' }}
      </el-button>
    </div>

    <div class="profile-content">
      <div class="profile-tabs">
        <div 
          :class="['tab-item', { active: activeTab === 'info' }]"
          @click="activeTab = 'info'"
        >
          个人资料
        </div>
        <div 
          :class="['tab-item', { active: activeTab === 'password' }]"
          @click="activeTab = 'password'"
        >
          修改密码
        </div>
        <div 
          :class="['tab-item', { active: activeTab === 'payment' }]"
          @click="activeTab = 'payment'"
        >
          支付账户
        </div>
        <div
          :class="['tab-item', { active: activeTab === 'voice' }]"
          @click="activeTab = 'voice'"
        >
                    语音设置
        </div>
        <div
          :class="['tab-item', { active: activeTab === 'account' }]"
          @click="activeTab = 'account'"
        >
          账号安全
        </div>
      </div>

      <div class="profile-actions">
        <button class="action-btn" @click="goToAnnouncements">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
          系统公告
        </button>
      </div>

      <div class="tab-content">
        <div v-if="activeTab === 'info'" class="info-form">
          <h3>编辑个人资料</h3>
          <el-form :model="editForm" label-width="100px">
            <el-form-item label="用户名">
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
            <el-form-item label="个性签名">
              <el-input v-model="editForm.signature" type="textarea" :rows="3" placeholder="请输入个性签名" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile" :loading="saving">保存修改</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="activeTab === 'password'" class="password-form">
          <h3>修改密码</h3>
          <el-form :model="passwordForm" label-width="120px">
            <el-form-item label="当前密码">
              <el-input v-model="passwordForm.currentPassword" type="password" placeholder="请输入当前密码" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（至少6位）" />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword" :loading="changingPassword">确认修改</el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="activeTab === 'payment'" class="payment-form">
          <h3>支付账户绑定</h3>
          <div v-if="!hasPaymentAccount" class="no-account-tip">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="color: var(--color-warning);">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <p>您还未绑定任何支付账户</p>
            <p class="tip-text">绑定支付账户后才能购买付费版本</p>
          </div>
          
          <div class="payment-list">
            <div v-for="account in paymentAccounts" :key="account.id" class="payment-item">
              <div class="payment-icon" :class="account.accountType">
                <svg v-if="account.accountType === 'alipay'" width="24" height="24" viewBox="0 0 24 24" fill="#1677FF">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
                </svg>
                <svg v-else-if="account.accountType === 'wechat'" width="24" height="24" viewBox="0 0 24 24" fill="#07C160">
                  <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.322-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.89c-.135-.01-.27-.03-.407-.032zm-2.53 3.274c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z"/>
                </svg>
                <svg v-else-if="account.accountType === 'bank'" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="color: var(--color-primary);">
                  <rect x="2" y="5" width="20" height="14" rx="2"/>
                  <path d="M2 10h20M2 14h20M6 18h12"/>
                </svg>
              </div>
              <div class="payment-info">
                <span class="payment-type">{{ getAccountTypeName(account.accountType) }}</span>
                <span class="payment-number">{{ account.accountNumber }}</span>
              </div>
              <div class="payment-status" :class="account.status.toLowerCase()">
                {{ getStatusText(account.status) }}
              </div>
              <button v-if="account.status === 'VERIFIED'" class="unbind-btn" @click="unbindAccount(account.id)">
                解绑
              </button>
              <button v-else class="verify-btn" @click="showVerifyModal(account)">
                验证
              </button>
            </div>
          </div>

          <div class="add-payment-section">
            <h4>添加支付账户</h4>
            <el-form :model="paymentForm" label-width="100px">
              <el-form-item label="账户类型">
                <el-select v-model="paymentForm.accountType" placeholder="请选择账户类型">
                  <el-option label="支付宝" value="alipay" />
                  <el-option label="微信支付" value="wechat" />
                  <el-option label="银行卡" value="bank" />
                </el-select>
              </el-form-item>
              <el-form-item label="账户号码">
                <el-input v-model="paymentForm.accountNumber" placeholder="请输入账户号码" />
              </el-form-item>
              <el-form-item label="开户姓名">
                <el-input v-model="paymentForm.accountName" placeholder="请输入开户姓名" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="bindAccount" :loading="binding">绑定账户</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <div v-if="activeTab === 'voice'" class="voice-form">
          <h3>语音音色偏好</h3>
          <p class="voice-tip">选择实时语音通话与朗读使用的音色，设置会同步到服务端并在下次通话时生效。</p>
          <el-form label-width="100px">
            <el-form-item label="音色">
              <el-select v-model="voicePreference" placeholder="请选择音色">
                <el-option v-for="v in voiceProfiles" :key="v.voiceId" :label="v.name" :value="v.voiceId">
                  <span style="float: left">{{ v.name }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">{{ v.scene }}</span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveVoicePreference" :loading="savingVoice">保存音色</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="activeTab === 'account'" class="account-form">
          <h3>账号安全</h3>
          <div class="danger-zone">
            <h4>危险操作</h4>
            <p class="danger-tip">注销账号将永久删除你的资料、对话历史、好友关系与记忆，且<span>不可恢复</span>。删除后当前登录态立即失效。</p>
            <el-button type="danger" @click="openDeleteAccountDialog">删除账号</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showVerifyDialog" title="账户验证" width="400px">
      <el-form :model="verifyForm" label-width="100px">
        <el-form-item label="验证码">
          <el-input v-model="verifyForm.verificationCode" placeholder="请输入验证码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showVerifyDialog = false">取消</el-button>
        <el-button type="primary" @click="submitVerification" :loading="verifying">确认验证</el-button>
      </template>
    </el-dialog>    <el-dialog v-model="showDeleteAccountDialog" title="确认删除账号" width="440px" :close-on-click-modal="false">
      <div class="delete-account-warning">
        <p>此操作不可撤销。请仔细阅读以下内容：</p>
        <ul>
          <li>你的个人资料、头像将被匿名化；</li>
          <li>所有 AI 对话与好友私聊记录将被删除；</li>
          <li>好友关系与 AI 记忆将被清除；</li>
          <li>当前登录凭证立即失效，需重新登录其他账号。</li>
        </ul>
        <p>请输入 <b>DELETE</b> 以确认注销账号：</p>
        <el-input v-model="deleteConfirmText" placeholder="请输入 DELETE" />
      </div>
      <template #footer>
        <el-button @click="showDeleteAccountDialog = false">取消</el-button>
        <el-button type="danger" :disabled="deleteConfirmText !== 'DELETE'" :loading="deletingAccount" @click="confirmDeleteAccount">确认删除</el-button>
      </template>
    </el-dialog>

    <input 

    <input 
      ref="fileInputRef"
      type="file" 
      accept="image/jpeg,image/png,image/gif" 
      class="hidden-file-input" 
      @change="handleAvatarUpload"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import logger from '@/utils/logger'
import { VOICE_PROFILES, loadVoicePreference as loadLocalVoice, saveVoicePreference as persistLocalVoice } from '@/utils/voiceProfiles'
import { validateImageFile } from '@/utils/avatar'

const router = useRouter()

const activeTab = ref('info')
const saving = ref(false)
const changingPassword = ref(false)
const fileInputRef = ref(null)
const avatarUploading = ref(false)

// ===== 语音音色偏好 =====
const voiceProfiles = VOICE_PROFILES
const voicePreference = ref(loadLocalVoice())
const savingVoice = ref(false)

const loadVoicePreferenceFromServer = async () => {
  try {
    const resp = await request.get('/api/user/voice-preference')
    if (resp && resp.code === 200 && resp.data && resp.data.voicePreference) {
      voicePreference.value = resp.data.voicePreference
    }
  } catch (e) {
    logger.warn('读取音色偏好失败，使用本地缓存', e)
  }
}

const saveVoicePreference = async () => {
  savingVoice.value = true
  try {
    await request.put('/api/user/voice-preference', { voicePreference: voicePreference.value })
    persistLocalVoice(voicePreference.value)
    ElMessage.success('音色偏好已保存')
  } catch (e) {
    ElMessage.error('保存音色偏好失败')
  } finally {
    savingVoice.value = false
  }
}


const userProfile = reactive({
  id: '',
  username: '',
  nickname: '',
  email: '',
  phone: '',
  signature: ''
})

const userStats = reactive({
  chatCount: 0,
  characterCount: 0,
  friendCount: 0
})

const editForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  signature: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const paymentForm = reactive({
  accountType: '',
  accountNumber: '',
  accountName: ''
})

const verifyForm = reactive({
  verificationCode: ''
})

const paymentAccounts = ref([])
const hasPaymentAccount = ref(false)
const binding = ref(false)
const verifying = ref(false)
const showVerifyDialog = ref(false)
const currentVerifyAccount = ref(null)

const loadPaymentAccounts = async () => {
  try {
    const response = await request.get(`/api/pay-account/user/${userProfile.id}`)
    if (response.code === 200) {
      paymentAccounts.value = response.data || []
      hasPaymentAccount.value = paymentAccounts.value.length > 0
    }
  } catch (error) {
    logger.error('加载支付账户失败:', error)
  }
}

const bindAccount = async () => {
  if (!paymentForm.accountType) {
    ElMessage.warning('请选择账户类型')
    return
  }
  if (!paymentForm.accountNumber) {
    ElMessage.warning('请输入账户号码')
    return
  }
  if (!paymentForm.accountName) {
    ElMessage.warning('请输入开户姓名')
    return
  }

  binding.value = true
  try {
    const response = await request.post('/api/pay-account/bind', {
      userId: userProfile.id,
      accountType: paymentForm.accountType,
      accountNumber: paymentForm.accountNumber,
      accountName: paymentForm.accountName
    })

    if (response.code === 200) {
      ElMessage.success(response.data.message || '账户绑定成功')
      paymentForm.accountType = ''
      paymentForm.accountNumber = ''
      paymentForm.accountName = ''
      loadPaymentAccounts()
    } else {
      ElMessage.error(response.msg || '绑定失败')
    }
  } catch (error) {
    logger.error('绑定支付账户失败:', error)
    ElMessage.error('绑定失败，请稍后重试')
  } finally {
    binding.value = false
  }
}

const showVerifyModal = (account) => {
  currentVerifyAccount.value = account
  verifyForm.verificationCode = ''
  showVerifyDialog.value = true
}

const submitVerification = async () => {
  if (!verifyForm.verificationCode || verifyForm.verificationCode.length !== 6) {
    ElMessage.warning('请输入6位验证码')
    return
  }

  verifying.value = true
  try {
    const response = await request.post('/api/pay-account/verify', {
      id: currentVerifyAccount.value.id,
      verificationCode: verifyForm.verificationCode
    })

    if (response.code === 200) {
      ElMessage.success(response.data.message || '验证成功')
      showVerifyDialog.value = false
      loadPaymentAccounts()
    } else {
      ElMessage.error(response.msg || '验证失败')
    }
  } catch (error) {
    logger.error('账户验证失败:', error)
    ElMessage.error('验证失败，请检查验证码是否正确')
  } finally {
    verifying.value = false
  }
}

const unbindAccount = async (accountId) => {
  try {
    const response = await request.delete(`/api/pay-account/${accountId}`)
    if (response.code === 200) {
      ElMessage.success('账户解绑成功')
      loadPaymentAccounts()
    } else {
      ElMessage.error(response.msg || '解绑失败')
    }
  } catch (error) {
    logger.error('解绑支付账户失败:', error)
    ElMessage.error('解绑失败，请稍后重试')
  }
}

const getAccountTypeName = (type) => {
  const types = {
    alipay: '支付宝',
    wechat: '微信支付',
    bank: '银行卡'
  }
  return types[type] || type
}

const getStatusText = (status) => {
  const texts = {
    VERIFIED: '已验证',
    PENDING_VERIFICATION: '待验证'
  }
  return texts[status] || status
}

const loadUserProfile = async () => {
  try {
    const savedUser = localStorage.getItem('user')
    if (savedUser) {
      const parsed = JSON.parse(savedUser)
      userProfile.id = parsed.id || parsed.userId || ''
      userProfile.username = parsed.username || ''
      userProfile.nickname = parsed.nickname || parsed.username || ''
      userProfile.email = parsed.email || ''
      userProfile.phone = parsed.phone || ''
      userProfile.signature = parsed.signature || ''

      editForm.username = userProfile.username
      editForm.nickname = userProfile.nickname
      editForm.email = userProfile.email
      editForm.phone = userProfile.phone
      editForm.signature = userProfile.signature
    }

    const response = await request.get(`/api/user/profile?userId=${userProfile.id}`)
    if (response.code === 200 && response.data) {
      Object.assign(userProfile, response.data)
      Object.assign(editForm, response.data)
    }
  } catch (error) {
    logger.error('加载用户资料失败:', error)
  }
}

const loadUserStats = async () => {
  try {
    const response = await request.get(`/api/user/stats?userId=${userProfile.id}`)
    if (response.code === 200 && response.data) {
      Object.assign(userStats, response.data)
    }
  } catch (error) {
    logger.error('加载用户统计失败:', error)
  }
}

const saveProfile = async () => {
  if (!editForm.nickname) {
    ElMessage.warning('请输入昵称')
    return
  }

  saving.value = true
  try {
    const response = await request.put('/api/user/profile', {
      userId: userProfile.id,
      nickname: editForm.nickname,
      email: editForm.email,
      phone: editForm.phone,
      bio: editForm.signature
    })

    if (response.code === 200) {
      ElMessage.success('个人资料更新成功')
      Object.assign(userProfile, editForm)

      const savedUser = localStorage.getItem('user')
      if (savedUser) {
        const parsed = JSON.parse(savedUser)
        parsed.nickname = editForm.nickname
        parsed.email = editForm.email
        parsed.phone = editForm.phone
        parsed.signature = editForm.signature
        localStorage.setItem('user', JSON.stringify(parsed))
      }
    } else {
      ElMessage.error(response.msg || '更新失败')
    }
  } catch (error) {
    logger.error('保存个人资料失败:', error)
    ElMessage.error('更新失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const resetForm = () => {
  editForm.nickname = userProfile.nickname
  editForm.email = userProfile.email
  editForm.phone = userProfile.phone
  editForm.signature = userProfile.signature
}

const changePassword = async () => {
  if (!passwordForm.currentPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少需要6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  changingPassword.value = true
  try {
    const response = await request.put('/api/user/change-password', {
      userId: userProfile.id,
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
    logger.error('修改密码失败:', error)
    ElMessage.error('密码修改失败，请检查当前密码是否正确')
  } finally {
    changingPassword.value = false
  }
}

const resetPasswordForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

// ===== M3 ⑨ 账号自助注销 =====
const showDeleteAccountDialog = ref(false)
const deleteConfirmText = ref('')
const deletingAccount = ref(false)

const openDeleteAccountDialog = () => {
  deleteConfirmText.value = ''
  showDeleteAccountDialog.value = true
}

const confirmDeleteAccount = async () => {
  if (deleteConfirmText.value !== 'DELETE') return
  deletingAccount.value = true
  try {
    const response = await request.delete('/api/user/account', {
      data: { confirmText: 'DELETE' }
    })
    if (response.code === 200) {
      ElMessage.success('账号已注销，正在退出登录...')
      // 清理本地登录态
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('isAdmin')
      setTimeout(() => router.push('/login'), 800)
    } else {
      ElMessage.error(response.msg || response.message || '账号注销失败')
    }
  } catch (error) {
    logger.error('账号注销失败:', error)
    ElMessage.error('账号注销失败，请稍后重试')
  } finally {
    deletingAccount.value = false
    showDeleteAccountDialog.value = false
  }
}

const goBack = () => {
  router.push('/chat')
}

const goToAnnouncements = () => {
  router.push('/announcements')
}

const triggerAvatarUpload = () => {
  fileInputRef.value?.click()
}

const handleAvatarUpload = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  const check = validateImageFile(file, { label: '头像图片' })
  if (!check.valid) {
    ElMessage.error(check.message)
    return
  }
  if (avatarUploading.value) return
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await request.post('/api/user/upload-avatar', formData)
    
    if (response.code === 200) {
      userProfile.avatar = response.data.filePath
      editForm.avatar = response.data.filePath
      ElMessage.success('头像更新成功')
      
      const savedUser = localStorage.getItem('user')
      if (savedUser) {
        const parsed = JSON.parse(savedUser)
        parsed.avatar = response.data.filePath
        localStorage.setItem('user', JSON.stringify(parsed))
      }
    } else {
      ElMessage.error(response.message || '头像更新失败')
    }
  } catch (error) {
    ElMessage.error('头像更新失败')
  } finally {
    avatarUploading.value = false
    event.target.value = ''
  }
}

const checkinStatus = ref({ checkedToday: false, consecutiveDays: 0, todayReward: 5 })

const loadCheckinStatus = async () => {
  try {
    const res = await request.get('/api/checkin/status')
    if (res.code === 200 && res.data) checkinStatus.value = res.data
  } catch (e) { /* 静默 */ }
}

const doCheckin = async () => {
  try {
    const res = await request.post('/api/checkin')
    if (res.code === 200 && res.data) {
      checkinStatus.value = {
        checkedToday: true,
        consecutiveDays: res.data.consecutiveDays,
        todayReward: res.data.totalReward
      }
      ElMessage.success(`签到成功！连续 ${res.data.consecutiveDays} 天，额度 +${res.data.totalReward}`)
    } else {
      ElMessage.error(res.message || '签到失败')
    }
  } catch (e) {
    ElMessage.error('签到失败')
  }
}

onMounted(() => {
  loadCheckinStatus()
  loadUserProfile()
  loadUserStats()
  loadPaymentAccounts()
  loadVoicePreferenceFromServer()
})
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: var(--bg-secondary);
  padding: var(--spacing-lg);
}

.profile-header {
  margin-bottom: var(--spacing-xl);
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

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 700;
  color: white;
  margin-bottom: var(--spacing-md);
}

.avatar-section h2 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.user-id {
  font-size: 14px;
  color: var(--text-muted);
}

.profile-stats {
  display: flex;
  gap: var(--spacing-2xl);
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.profile-content {
  max-width: 600px;
  margin: 0 auto;
}

.profile-actions {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(59, 130, 246, 0.05);
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

.back-btn {
  position: absolute;
  top: var(--spacing-lg);
  left: var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--card-bg);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  z-index: 100;
}

.back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
}

.avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.change-avatar-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: var(--image-btn-width);
  height: var(--image-btn-height);
  border-radius: var(--image-btn-border-radius);
  background: var(--image-btn-bg);
  border: var(--image-btn-border-width) solid var(--image-btn-border-color);
  color: var(--image-btn-color);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  box-shadow: var(--shadow-md);
}

.change-avatar-btn:hover {
  background: var(--image-btn-bg-hover);
  transform: scale(1.1);
  box-shadow: var(--shadow-neon);
}

.hidden-file-input {
  display: none;
}

.payment-form h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-lg);
}

.no-account-tip {
  text-align: center;
  padding: var(--spacing-2xl);
  background: rgba(251, 191, 36, 0.1);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.no-account-tip p {
  margin: 0;
  color: var(--text-secondary);
}

.no-account-tip .tip-text {
  font-size: 14px;
  color: var(--color-warning);
}

.payment-list {
  margin-bottom: var(--spacing-xl);
}

.payment-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-lg);
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-md);
  border: 1px solid var(--border-primary);
}

.payment-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
}

.payment-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.payment-type {
  font-weight: 600;
  color: var(--text-primary);
}

.payment-number {
  font-size: 14px;
  color: var(--text-muted);
}

.payment-status {
  font-size: 13px;
  padding: 4px 12px;
  border-radius: var(--radius-md);
}

.payment-status.verified {
  background: rgba(34, 197, 94, 0.1);
  color: var(--color-success);
}

.payment-status.pending_verification {
  background: rgba(251, 191, 36, 0.1);
  color: var(--color-warning);
}

.unbind-btn {
  padding: 6px 16px;
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  transition: all var(--transition-fast);
}

.unbind-btn:hover {
  background: rgba(239, 68, 68, 0.2);
}

.verify-btn {
  padding: 6px 16px;
  background: rgba(59, 130, 246, 0.1);
  color: var(--color-primary);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  transition: all var(--transition-fast);
}

.verify-btn:hover {
  background: rgba(59, 130, 246, 0.2);
}

.add-payment-section {
  border-top: 1px solid var(--border-primary);
  padding-top: var(--spacing-xl);
}

.add-payment-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-lg);
}

.checkin-card { display: flex; align-items: center; justify-content: space-between; margin: 0 24px 16px; padding: 16px 20px; background: linear-gradient(135deg, #eef2ff, #f0f9ff); border-radius: 14px; }
.checkin-title { font-size: 16px; font-weight: 600; color: #1e293b; }
.checkin-sub { font-size: 13px; color: #64748b; margin-top: 4px; }
.checkin-sub b { color: #4f46e5; }
.account-form .danger-zone {
  margin-top: 16px;
  padding: 20px;
  border: 1px solid #f56c6c;
  border-radius: 8px;
  background: #fef0f0;
}
.account-form .danger-zone h4 {
  margin: 0 0 10px;
  color: #f56c6c;
}
.account-form .danger-tip {
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 14px;
}
.account-form .danger-tip span {
  color: #f56c6c;
  font-weight: 600;
}
.delete-account-warning ul {
  padding-left: 20px;
  line-height: 1.9;
  color: #606266;
}
.delete-account-warning p {
  color: #606266;
  line-height: 1.7;
}
</style>