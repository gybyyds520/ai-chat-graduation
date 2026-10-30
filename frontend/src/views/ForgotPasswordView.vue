<template>
  <div class="forgot-password-page">
    <div class="login-bg">
      <div class="bg-gradient"></div>
      <div class="bg-grid"></div>
    </div>
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <div class="logo">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <rect width="48" height="48" rx="12" fill="url(#logoGrad)"/>
              <path d="M24 14C18.48 14 14 18.48 14 24s4.48 10 10 10 10-4.48 10-10S29.52 14 24 14zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z" fill="white"/>
              <defs>
                <linearGradient id="logoGrad" x1="0" y1="0" x2="48" y2="48">
                  <stop stop-color="#4f46e5"/>
                  <stop offset="1" stop-color="#7c3aed"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h1>忘记密码</h1>
          <p>{{ stepTitle }}</p>
        </div>

        <form @submit.prevent="handleSubmit" class="login-form">
          <template v-if="currentStep === 1">
            <div class="form-item">
              <label>选择验证方式</label>
              <div class="verification-methods">
                <button type="button" @click="selectMethod('email')" :class="['method-btn', { active: verificationMethod === 'email' }]">
                  <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z"/>
                    <path d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z"/>
                  </svg>
                  <span>邮箱验证</span>
                </button>
                <button type="button" @click="selectMethod('phone')" :class="['method-btn', { active: verificationMethod === 'phone' }]">
                  <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 5V3z"/>
                  </svg>
                  <span>短信验证</span>
                </button>
              </div>
            </div>

            <div class="form-item">
              <label>{{ verificationMethod === 'email' ? '邮箱' : '手机号' }}</label>
              <div class="input-wrapper">
                <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                  <path v-if="verificationMethod === 'email'" d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z"/>
                  <path v-if="verificationMethod === 'email'" d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z"/>
                  <path v-else d="M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 5V3z"/>
                </svg>
                <input
                  v-model="forgotForm.account"
                  :type="verificationMethod === 'email' ? 'email' : 'tel'"
                  :placeholder="verificationMethod === 'email' ? '请输入注册邮箱' : '请输入注册手机号'"
                />
              </div>
            </div>
          </template>

          <template v-else-if="currentStep === 2">
            <div class="form-item">
              <label>{{ verificationMethod === 'email' ? '邮箱验证码' : '短信验证码' }}</label>
              <div class="input-wrapper">
                <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                <input
                  v-model="forgotForm.code"
                  type="text"
                  placeholder="请输入验证码"
                  maxlength="6"
                />
                <button
                  type="button"
                  @click="sendCode"
                  :disabled="countdown > 0"
                  class="send-code-btn"
                >
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </button>
              </div>
            </div>
          </template>

          <template v-else-if="currentStep === 3">
            <div class="form-item">
              <label>新密码</label>
              <div class="input-wrapper">
                <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M10 2a4 4 0 00-4 4v2H4a2 2 0 00-2 2v8a2 2 0 002 2h12a2 2 0 002-2v-8a2 2 0 00-2-2h-2V6a4 4 0 00-4-4zm2 6h-4v2h4V8zm-4 6v-2h4v2H8z"/>
                </svg>
                <input
                  v-model="forgotForm.newPassword"
                  type="password"
                  placeholder="请输入新密码（8-20位）"
                />
              </div>
            </div>
            <div class="form-item">
              <label>确认密码</label>
              <div class="input-wrapper">
                <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                  <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                <input
                  v-model="forgotForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入新密码"
                />
              </div>
            </div>
          </template>

          <div class="form-footer">
            <button type="button" @click="goToLogin" class="back-btn">返回登录</button>
          </div>

          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="!loading">{{ stepButtonText }}</span>
            <span v-else class="loading-spinner"></span>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import logger from '@/utils/logger'
import { useTheme } from '@/composables/useTheme'

const router = useRouter()
const { initTheme } = useTheme()

const currentStep = ref(1)
const verificationMethod = ref('email')
const loading = ref(false)
const countdown = ref(0)

const forgotForm = reactive({
  account: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const stepTitle = computed(() => {
  const titles = {
    1: '选择验证方式找回密码',
    2: '输入验证码',
    3: '设置新密码'
  }
  return titles[currentStep.value]
})

const stepButtonText = computed(() => {
  const texts = {
    1: '下一步',
    2: '验证并继续',
    3: '确认修改'
  }
  return texts[currentStep.value]
})

onMounted(() => {
  initTheme()
})

const selectMethod = (method) => {
  verificationMethod.value = method
}

const sendCode = async () => {
  if (!forgotForm.account) {
    ElMessage.warning(`请输入${verificationMethod.value === 'email' ? '邮箱' : '手机号'}`)
    return
  }

  loading.value = true
  try {
    const params = verificationMethod.value === 'email' 
      ? { email: forgotForm.account }
      : { phone: forgotForm.account }
    
    const response = await request.post('/api/user/send-verification-code', params)
    
    if (response.code === 200) {
      ElMessage.success(response.msg || '验证码发送成功')
      startCountdown()
    } else {
      ElMessage.error(response.msg || '发送失败')
    }
  } catch (error) {
    logger.error('发送验证码失败:', error)
    ElMessage.error('发送失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const startCountdown = () => {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

const handleSubmit = async () => {
  loading.value = true

  try {
    if (currentStep.value === 1) {
      if (!forgotForm.account) {
        ElMessage.warning(`请输入${verificationMethod.value === 'email' ? '邮箱' : '手机号'}`)
        loading.value = false
        return
      }

      await sendCode()
      if (countdown.value > 0) {
        currentStep.value = 2
      }
    } else if (currentStep.value === 2) {
      if (!forgotForm.code) {
        ElMessage.warning('请输入验证码')
        loading.value = false
        return
      }

      const params = verificationMethod.value === 'email'
        ? { email: forgotForm.account, code: forgotForm.code }
        : { phone: forgotForm.account, code: forgotForm.code }

      const response = await request.post('/api/user/verify-code', params)
      
      if (response.code === 200) {
        ElMessage.success('验证码验证成功')
        currentStep.value = 3
      } else {
        ElMessage.error(response.msg || '验证码错误')
      }
    } else if (currentStep.value === 3) {
      if (!forgotForm.newPassword || !forgotForm.confirmPassword) {
        ElMessage.warning('请填写密码')
        loading.value = false
        return
      }

      if (forgotForm.newPassword.length < 8 || forgotForm.newPassword.length > 20) {
        ElMessage.warning('密码长度必须在8-20个字符之间')
        loading.value = false
        return
      }

      if (forgotForm.newPassword !== forgotForm.confirmPassword) {
        ElMessage.warning('两次输入的密码不一致')
        loading.value = false
        return
      }

      const params = {
        email: verificationMethod.value === 'email' ? forgotForm.account : null,
        phone: verificationMethod.value === 'phone' ? forgotForm.account : null,
        code: forgotForm.code,
        newPassword: forgotForm.newPassword
      }

      const response = await request.post('/api/user/reset-password-by-code', params)
      
      if (response.code === 200) {
        ElMessage.success('密码修改成功，请重新登录')
        router.push('/login')
      } else {
        ElMessage.error(response.msg || '密码修改失败')
      }
    }
  } catch (error) {
    logger.error('操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.forgot-password-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--bg-primary);
  font-family: var(--font-family-body);
}

.login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(99, 102, 241, 0.3), transparent),
    radial-gradient(ellipse 60% 40% at 80% 50%, rgba(168, 85, 247, 0.2), transparent),
    radial-gradient(ellipse 40% 30% at 20% 80%, rgba(6, 182, 212, 0.15), transparent);
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.05) 1px, transparent 1px);
  background-size: 60px 60px;
  animation: gridMove 20s linear infinite;
}

@keyframes gridMove {
  0% { background-position: 0 0; }
  100% { background-position: 60px 60px; }
}

.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xl);
  padding: var(--spacing-md);
  width: 100%;
  max-width: var(--width-card-sm);
}

.login-card {
  width: 100%;
  background: var(--gradient-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl) var(--spacing-xl);
  box-shadow: var(--shadow-neon);
  position: relative;
  overflow: hidden;
  animation: fadeIn 0.6s ease-out;
}

.login-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--gradient-primary);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo {
  display: inline-flex;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}

.login-header h1 {
  font-family: var(--font-family-display);
  font-size: 32px;
  font-weight: 700;
  background: var(--gradient-text-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.login-header p {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.verification-methods {
  display: flex;
  gap: var(--spacing-md);
}

.method-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background: var(--input-bg);
  border: 2px solid var(--input-border);
  border-radius: var(--radius-md);
  font-size: var(--font-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.method-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.method-btn.active {
  background: rgba(99, 102, 241, 0.1);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: var(--spacing-md);
  color: var(--text-muted);
  pointer-events: none;
  transition: color var(--transition-fast);
}

.form-item input {
  width: 100%;
  padding: var(--spacing-md) var(--spacing-md) var(--spacing-md) calc(var(--spacing-md) * 3);
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: var(--radius-md);
  font-size: var(--font-md);
  color: var(--text-primary);
  font-family: var(--font-family-body);
  transition: all var(--transition-fast);
}

.form-item input::placeholder {
  color: var(--input-placeholder);
}

.form-item input:focus {
  outline: none;
  border-color: var(--color-primary);
  background: var(--bg-tertiary);
  box-shadow: var(--shadow-glow);
}

.send-code-btn {
  position: absolute;
  right: var(--spacing-sm);
  padding: var(--spacing-xs) var(--spacing-md);
  background: var(--gradient-primary);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--font-xs);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.send-code-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-glow);
}

.send-code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.back-btn {
  padding: var(--spacing-sm) var(--spacing-md);
  background: transparent;
  border: 1px solid var(--input-border);
  border-radius: var(--radius-sm);
  font-size: var(--font-sm);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.form-footer {
  text-align: center;
}

.login-btn {
  width: 100%;
  padding: var(--spacing-md);
  background: var(--gradient-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-md);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-normal);
  box-shadow: var(--shadow-glow);
  position: relative;
  overflow: hidden;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.login-btn:hover:not(:disabled)::before {
  left: 100%;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-neon);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@media (max-width: 480px) {
  .verification-methods {
    flex-direction: column;
  }

  .send-code-btn {
    right: var(--spacing-xs);
    padding: var(--spacing-xs) var(--spacing-sm);
    font-size: var(--font-xs);
  }
}
</style>