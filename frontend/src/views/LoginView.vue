<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-gradient"></div>
      <div class="bg-grid"></div>
      <!-- 科技感动态光效层：极光光束 + 漂浮光点 + 扫描光带 -->
      <div class="bg-aurora" aria-hidden="true">
        <span class="aurora-beam beam-1"></span>
        <span class="aurora-beam beam-2"></span>
        <span class="aurora-beam beam-3"></span>
      </div>
      <div class="bg-orbs" aria-hidden="true">
        <span class="orb orb-1"></span>
        <span class="orb orb-2"></span>
        <span class="orb orb-3"></span>
        <span class="orb orb-4"></span>
        <span class="orb orb-5"></span>
      </div>
      <div class="bg-scanline" aria-hidden="true"></div>
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
          <h1>AI Chat</h1>
          <p>智能对话，连接未来</p>
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-item">
            <label>用户名</label>
            <div class="input-wrapper">
              <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 10a4 4 0 100-8 4 4 0 000 8zm0 2c-4.42 0-8 1.79-8 4v2h16v-2c0-2.21-3.58-4-8-4z"/>
              </svg>
              <input
                v-model="loginForm.username"
                type="text"
                placeholder="请输入用户名"
                autocomplete="username"
              />
            </div>
          </div>

          <div class="form-item">
            <label>密码</label>
            <div class="input-wrapper">
              <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 2a4 4 0 00-4 4v2H4a2 2 0 00-2 2v8a2 2 0 002 2h12a2 2 0 002-2v-8a2 2 0 00-2-2h-2V6a4 4 0 00-4-4zm2 6h-4v2h4V8zm-4 6v-2h4v2H8z"/>
              </svg>
              <input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                autocomplete="current-password"
              />
            </div>
          </div>

          <div class="form-options">
            <label class="checkbox-wrapper">
              <input type="checkbox" v-model="loginForm.isAdmin" />
              <span class="checkmark"></span>
              <span class="checkbox-label">管理员登录</span>
            </label>
            <label class="checkbox-wrapper">
              <input type="checkbox" v-model="loginForm.rememberMe" />
              <span class="checkmark"></span>
              <span class="checkbox-label">记住我</span>
            </label>
          </div>
          <a href="#" @click.prevent="handleForgotPassword" class="forgot-password">忘记密码？</a>

          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="!loading">{{ loginForm.isAdmin ? '管理员登录' : '登录' }}</span>
            <span v-else class="loading-spinner"></span>
          </button>

          <div class="form-footer">
            <span>还没有账号？</span>
            <a href="#" @click.prevent="goToRegister">立即注册</a>
          </div>
        </form>
      </div>

      <div class="login-features">
        <div class="feature-item">
          <div class="feature-icon">🤖</div>
          <div class="feature-text">
            <h4>AI 智能对话</h4>
            <p>基于先进大语言模型</p>
          </div>
        </div>
        <div class="feature-item">
          <div class="feature-icon">💬</div>
          <div class="feature-text">
            <h4>角色定制</h4>
            <p>创建专属 AI 角色</p>
          </div>
        </div>
        <div class="feature-item">
          <div class="feature-icon">🔒</div>
          <div class="feature-text">
            <h4>安全可靠</h4>
            <p>端到端加密保护</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import logger from '@/utils/logger'
import { useTheme } from '@/composables/useTheme'
import { validateLoginCredentials } from '@/utils/accountForm'

const router = useRouter()
const { initTheme, currentTheme } = useTheme()
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false,
  isAdmin: false
})

const loading = ref(false)

onMounted(() => {
  initTheme()
})

const handleLogin = async () => {
  // 防重复提交：请求进行中直接忽略后续点击/回车连发
  if (loading.value) return

  const cred = validateLoginCredentials(loginForm)
  if (!cred.valid) {
    ElMessage.warning(cred.message)
    return
  }

  loading.value = true

  try {
    const endpoint = loginForm.isAdmin ? '/api/admin/login' : '/api/user/login'
    const response = await request.post(endpoint, {
      username: loginForm.username,
      password: loginForm.password,
      rememberMe: loginForm.rememberMe
    })

    if (response.code === 200) {
      // 兼容新/旧返回结构：
      // 新版 /api/user/login 返回 { user, token, tokenType }
      // 新版 /api/admin/login 返回 { admin, token, tokenType }
      // 旧版直接返回 user/admin 对象（无 token，用 id 降级）
      const data = response.data || {}
      const userData = data.user || data.admin || data
      const token = data.token || userData.id
      // 兜底：登录响应既无 token 又无 id 时，绝不能把字符串 "undefined" 写进 localStorage，
      // 否则路由守卫会误以为已登录而进入受保护页。直接抛错走 catch 提示。
      if (!token) {
        throw new Error('登录响应缺少 token')
      }
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(userData))

      if (loginForm.rememberMe) {
        localStorage.setItem('rememberMe', 'true')
        localStorage.setItem('savedUsername', loginForm.username)
      } else {
        localStorage.removeItem('rememberMe')
        localStorage.removeItem('savedUsername')
      }

      if (loginForm.isAdmin) {
        localStorage.setItem('isAdmin', 'true')
        ElMessage.success('管理员登录成功！')
        router.push('/admin')
      } else {
        ElMessage.success('登录成功！')
        router.push('/chat')
      }
    } else {
      ElMessage.error(response.msg || '登录失败')
    }
  } catch (error) {
    logger.error('登录失败:', error)
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

const handleForgotPassword = () => {
  router.push('/forgot-password')
}

const goToRegister = () => {
  router.push('/register')
}

// 加载记住的用户名
const initForm = () => {
  if (localStorage.getItem('rememberMe') === 'true') {
    loginForm.rememberMe = true
    loginForm.username = localStorage.getItem('savedUsername') || ''
  }
}

initForm()
</script>

<style scoped>
.login-page {
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

/* ===== 科技感动态光效层（极光光束 / 漂浮光点 / 扫描光带） ===== */
.bg-aurora,
.bg-orbs,
.bg-scanline {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

/* 极光光束：缓慢漂移的彩色光带 */
.aurora-beam {
  position: absolute;
  width: 60%;
  height: 55%;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.35;
  mix-blend-mode: screen;
  animation: auroraDrift 16s ease-in-out infinite alternate;
}
.beam-1 {
  top: -12%;
  left: -8%;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.9), rgba(6, 182, 212, 0.4));
}
.beam-2 {
  bottom: -16%;
  right: -10%;
  background: linear-gradient(315deg, rgba(168, 85, 247, 0.85), rgba(236, 72, 153, 0.35));
  animation-delay: -5s;
  animation-duration: 20s;
}
.beam-3 {
  top: 30%;
  left: 45%;
  width: 40%;
  height: 35%;
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.55), rgba(99, 102, 241, 0.35));
  animation-delay: -10s;
  animation-duration: 24s;
}
@keyframes auroraDrift {
  0%   { transform: translate(0, 0) scale(1) rotate(0deg); }
  50%  { transform: translate(6%, 4%) scale(1.12) rotate(6deg); }
  100% { transform: translate(-4%, -6%) scale(0.96) rotate(-4deg); }
}

/* 漂浮光点：缓慢上升、闪烁的科技感粒子 */
.orb {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(165, 180, 252, 0.9), rgba(165, 180, 252, 0.05) 70%);
  animation: orbFloat var(--orb-dur, 12s) ease-in-out infinite;
  opacity: 0;
}
.orb-1 { --orb-dur: 11s; width: 5px;  height: 5px;  left: 12%; bottom: -10px; }
.orb-2 { --orb-dur: 14s; width: 3px;  height: 3px;  left: 32%; bottom: -10px; animation-delay: -4s; }
.orb-3 { --orb-dur: 9s;  width: 6px;  height: 6px;  left: 55%; bottom: -10px; animation-delay: -7s; }
.orb-4 { --orb-dur: 16s; width: 4px;  height: 4px;  left: 72%; bottom: -10px; animation-delay: -2s; }
.orb-5 { --orb-dur: 12s; width: 7px;  height: 7px;  left: 88%; bottom: -10px; animation-delay: -9s; }
@keyframes orbFloat {
  0%   { transform: translateY(0) translateX(0); opacity: 0; }
  8%   { opacity: 0.7; }
  50%  { transform: translateY(-45vh) translateX(18px); opacity: 0.5; }
  92%  { opacity: 0.7; }
  100% { transform: translateY(-88vh) translateX(-12px); opacity: 0; }
}

/* 扫描光带：缓慢下移的霓虹线，营造 HUD 扫描感 */
.bg-scanline {
  overflow: hidden;
}
.bg-scanline::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 90px;
  background: linear-gradient(180deg, transparent, rgba(99, 102, 241, 0.12), rgba(6, 182, 212, 0.12), transparent);
  animation: scanlineMove 9s linear infinite;
}
@keyframes scanlineMove {
  0%   { transform: translateY(-120%); }
  100% { transform: translateY(120vh); }
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

.login-card::after {
  content: '';
  position: absolute;
  top: -1px;
  left: -1px;
  width: 30px;
  height: 30px;
  border-top: 2px solid var(--color-primary);
  border-left: 2px solid var(--color-primary);
  border-radius: 8px 0 0 0;
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

.form-item input:focus + .input-icon,
.form-item input:focus ~ .input-icon {
  color: var(--color-primary);
}

.form-options {
  display: flex;
  align-items: center;
  gap: var(--spacing-xl);
}

.forgot-password {
  font-size: var(--font-sm);
  color: var(--color-primary);
  text-decoration: none;
  transition: color 0.2s ease;
  text-align: center;
  margin-top: var(--spacing-sm);
  display: block;
}

.forgot-password:hover {
  color: var(--color-primary-hover);
  text-decoration: underline;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  cursor: pointer;
  user-select: none;
}

.checkbox-wrapper input {
  display: none;
}

.checkmark {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-primary);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  background: var(--bg-tertiary);
}

.checkbox-wrapper input:checked + .checkmark {
  background: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.5);
}

.checkbox-wrapper input:checked + .checkmark::after {
  content: '';
  width: 6px;
  height: 10px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
  margin-bottom: 2px;
}

.checkbox-label {
  font-size: var(--font-sm);
  color: var(--text-secondary);
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

.login-btn:active:not(:disabled) {
  transform: translateY(0);
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

.form-footer {
  text-align: center;
  padding-top: 8px;
}

.form-footer span {
  font-size: 14px;
  color: var(--text-muted);
}

.form-footer a {
  font-size: 14px;
  color: var(--color-primary);
  font-weight: 600;
  margin-left: 6px;
  transition: color 0.2s ease;
}

.form-footer a:hover {
  color: var(--color-primary-hover);
  text-decoration: underline;
}

.login-features {
  display: flex;
  gap: var(--spacing-lg);
  justify-content: center;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: var(--radius-md);
  transition: all var(--transition-normal);
}

.feature-item:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-glow);
  transform: translateY(-2px);
}

.feature-icon {
  font-size: 24px;
}

.feature-text h4 {
  font-size: var(--font-xs);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-xs);
  font-family: var(--font-family-display);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.feature-text p {
  font-size: var(--font-xs);
  color: var(--text-muted);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@media (max-width: 768px) {
  .login-features {
    display: none;
  }

  .login-card {
    padding: var(--spacing-xl) var(--spacing-lg);
    border-radius: var(--radius-lg);
  }

  .login-header h1 {
    font-size: var(--font-xl);
  }

  .login-form {
    gap: var(--spacing-md);
  }

  .form-item input {
    padding: var(--spacing-sm) var(--spacing-sm) var(--spacing-sm) calc(var(--spacing-md) * 2.5);
    font-size: var(--font-sm);
  }

  .login-btn {
    padding: var(--spacing-sm);
    font-size: var(--font-sm);
  }
}

@media (max-width: 480px) {
  .login-container {
    padding: var(--spacing-md);
    gap: var(--spacing-lg);
  }

  .login-card {
    padding: var(--spacing-lg) var(--spacing-md);
    border-radius: var(--radius-lg);
  }

  .login-header {
    margin-bottom: var(--spacing-lg);
  }

  .logo {
    margin-bottom: var(--spacing-sm);
  }

  .logo svg {
    width: 40px;
    height: 40px;
  }

  .login-header h1 {
    font-size: var(--font-xl);
    letter-spacing: 1px;
  }

  .login-header p {
    font-size: var(--font-xs);
  }

  .form-item {
    gap: var(--spacing-xs);
  }

  .form-item label {
    font-size: var(--font-xs);
  }

  .form-item input {
    padding: var(--spacing-sm) var(--spacing-sm) var(--spacing-sm) calc(var(--spacing-md) * 2.4);
    font-size: var(--font-sm);
    border-radius: var(--radius-sm);
  }

  .input-icon {
    left: var(--spacing-sm);
    width: 18px;
    height: 18px;
  }

  .form-options {
    margin-top: var(--spacing-xs);
  }

  .checkbox-wrapper {
    gap: var(--spacing-xs);
  }

  .checkmark {
    width: 18px;
    height: 18px;
  }

  .checkbox-label {
    font-size: var(--font-xs);
  }

  .login-btn {
    padding: var(--spacing-sm);
    font-size: var(--font-sm);
    border-radius: var(--radius-sm);
  }

  .form-footer {
    padding-top: 4px;
  }

  .form-footer span,
  .form-footer a {
    font-size: 13px;
  }
}
</style>