<template>
  <div class="register-page">
    <div class="register-bg">
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
    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
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
          <h1>创建账号</h1>
          <p>加入 AI Chat 开始智能对话之旅</p>
        </div>

        <form @submit.prevent="handleRegister" class="register-form">
          <div class="form-item">
            <label>用户名</label>
            <div class="input-wrapper">
              <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 10a4 4 0 100-8 4 4 0 000 8zm0 2c-4.42 0-8 1.79-8 4v2h16v-2c0-2.21-3.58-4-8-4z"/>
              </svg>
              <input
                v-model="registerForm.username"
                type="text"
                placeholder="请输入用户名（3-20个字符）"
                autocomplete="username"
              />
            </div>
          </div>

          <div class="form-item">
            <label>邮箱</label>
            <div class="input-wrapper">
              <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                <path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/>
              </svg>
              <input
                v-model="registerForm.email"
                type="email"
                placeholder="请输入邮箱地址"
                autocomplete="email"
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
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码（8-20个字符，需包含字母和数字）"
                autocomplete="new-password"
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
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                autocomplete="new-password"
              />
            </div>
          </div>

          <div class="form-item">
            <label>昵称 <span class="optional">（可选）</span></label>
            <div class="input-wrapper">
              <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9c0-3.87 3.13-7 7-7s7 3.13 7 7H3z"/>
              </svg>
              <input
                v-model="registerForm.nickname"
                type="text"
                placeholder="请输入昵称（选填）"
              />
            </div>
          </div>

          <div class="form-item">
            <label>年龄 <span class="optional">（选填，用于未成年保护）</span></label>
            <div class="input-wrapper">
              <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 2a6 6 0 00-6 6v3.586l-.707.707A1 1 0 004 14h12a1 1 0 00.707-1.707L16 11.586V8a6 6 0 00-6-6zM10 18a2 2 0 002-2h-4a2 2 0 002 2z"/>
              </svg>
              <input
                v-model="registerForm.age"
                type="number"
                min="0"
                max="120"
                placeholder="请输入年龄（选填；未满18岁将开启未成年保护）"
                @input="onAgeInput"
              />
            </div>
            <p v-if="ageHint" class="age-hint">{{ ageHint }}</p>
          </div>

          <div class="agree-row">
            <label class="agree-checkbox">
              <input v-model="agreed" type="checkbox" />
              <span class="agree-text">
                我已阅读并同意
                <a href="/agreement" @click.prevent="openAgreement('agreement')">《用户协议》</a>
                和
                <a href="/agreement" @click.prevent="openAgreement('privacy')">《隐私政策》</a>
              </span>
            </label>
          </div>

          <button type="submit" class="register-btn" :disabled="loading">
            <span v-if="!loading">注册</span>
            <span v-else class="loading-spinner"></span>
          </button>

          <div class="form-footer">
            <span>已有账号？</span>
            <a href="#" @click.prevent="goToLogin">立即登录</a>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import logger from '@/utils/logger'
import { classifyDeclaredAge } from '@/utils/minorGuard'
import { validateRegisterInput } from '@/utils/accountForm'

const router = useRouter()
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  age: ''
})
const loading = ref(false)
const agreed = ref(false)

// 年龄自声明：实时给出校验/未成年保护提示（纯逻辑来自 minorGuard，便于单测）
const ageCheck = computed(() => classifyDeclaredAge(registerForm.age))
const ageHint = ref('')
const onAgeInput = () => {
  ageHint.value = ageCheck.value.message || ''
}

const openAgreement = (tab) => {
  // 跳转到法律文档页；通过 query 指定默认打开的 tab
  router.push({ path: '/agreement', query: { tab } })
}

const handleRegister = async () => {
  // 防重复提交：请求进行中直接忽略后续点击（按钮 disabled 依赖 Vue 重渲染，
  // 快速连点/回车连发时此处兜底）。
  if (loading.value) return

  // 表单校验（纯逻辑来自 accountForm，便于单测）
  const check = validateRegisterInput(registerForm, ageCheck.value)
  if (!check.valid) {
    ElMessage.warning(check.message)
    return
  }

  loading.value = true

  try {
    const payload = {
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      nickname: registerForm.nickname
    }
    // 仅在用户声明了有效年龄时上送（0/空 = 不声明，按成年处理）
    if (ageCheck.value.age !== null) {
      payload.age = ageCheck.value.age
    }
    const response = await request.post('/api/user/register', payload)

    if (response.code === 200) {
      ElMessage.success('注册成功！')
      router.push('/login')
    } else {
      ElMessage.error(response.msg || '注册失败')
    }
  } catch (error) {
    logger.error('注册失败:', error)
    ElMessage.error('注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--bg-primary);
  font-family: var(--font-family-body);
}

.register-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(79, 70, 229, 0.3), transparent),
    radial-gradient(ellipse 60% 40% at 80% 50%, rgba(124, 58, 237, 0.2), transparent),
    radial-gradient(ellipse 40% 30% at 20% 80%, rgba(59, 130, 246, 0.15), transparent);
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.05) 1px, transparent 1px);
  background-size: 60px 60px;
  animation: gridMove 24s linear infinite;
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
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.9), rgba(59, 130, 246, 0.4));
}
.beam-2 {
  bottom: -16%;
  right: -10%;
  background: linear-gradient(315deg, rgba(124, 58, 237, 0.85), rgba(236, 72, 153, 0.35));
  animation-delay: -5s;
  animation-duration: 20s;
}
.beam-3 {
  top: 30%;
  left: 45%;
  width: 40%;
  height: 35%;
  background: linear-gradient(180deg, rgba(34, 211, 238, 0.55), rgba(79, 70, 229, 0.35));
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

.register-container {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  width: 100%;
  max-width: 440px;
}

.register-card {
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

.register-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--gradient-primary);
}

.register-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo {
  display: inline-flex;
  margin-bottom: 16px;
}

.register-header h1 {
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

.register-header p {
  font-size: 14px;
  color: var(--text-muted);
}

.register-form {
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

  .form-item label .optional {
    font-weight: 400;
    color: var(--text-muted);
    text-transform: none;
    letter-spacing: 0;
  }

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  color: var(--text-muted);
  pointer-events: none;
}

.form-item input {
  width: 100%;
  padding: 14px 14px 14px 44px;
  background: var(--input-bg);
  border: 1px solid var(--input-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  color: var(--text-primary);
  transition: all 0.2s ease;
}

.form-item input::placeholder {
  color: var(--input-placeholder);
}

.age-hint {
  font-size: 12px;
  color: var(--color-warning);
  line-height: 1.5;
  margin-top: 4px;
}

.form-item input:focus {
  outline: none;
  border-color: var(--color-primary);
  background: var(--bg-tertiary);
  box-shadow: var(--shadow-glow);
}

.register-btn {
  width: 100%;
  padding: 14px;
  background: var(--gradient-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: var(--shadow-glow);
  position: relative;
  overflow: hidden;
}

.agree-row {
  margin-top: 4px;
}

.agree-checkbox {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.agree-checkbox input[type='checkbox'] {
  margin-top: 3px;
  width: 15px;
  height: 15px;
  accent-color: var(--color-primary);
  cursor: pointer;
}

.agree-text {
  flex: 1;
}

.agree-text a {
  color: var(--color-primary);
  text-decoration: none;
}

.agree-text a:hover {
  text-decoration: underline;
  color: var(--color-primary-hover);
}

.register-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-neon);
}

.register-btn:active:not(:disabled) {
  transform: translateY(0);
}

.register-btn:disabled {
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
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
  font-weight: 500;
  margin-left: 6px;
}

.form-footer a:hover {
  color: var(--color-primary-hover);
  text-decoration: underline;
}

@media (max-width: 768px) {
  .register-card {
    padding: 32px 24px;
    border-radius: 20px;
  }

  .register-header h1 {
    font-size: 24px;
  }

  .register-form {
    gap: 16px;
  }

  .form-item input {
    padding: 12px 12px 12px 40px;
    font-size: 14px;
  }

  .register-btn {
    padding: 12px;
    font-size: 15px;
  }
}

@media (max-width: 480px) {
  .register-container {
    padding: 16px;
    gap: 24px;
  }

  .register-card {
    padding: 24px 20px;
    border-radius: 16px;
  }

  .register-header {
    margin-bottom: 24px;
  }

  .logo {
    margin-bottom: 12px;
  }

  .logo svg {
    width: 40px;
    height: 40px;
  }

  .register-header h1 {
    font-size: 22px;
  }

  .register-header p {
    font-size: 13px;
  }

  .form-item {
    gap: 6px;
  }

  .form-item label {
    font-size: 12px;
  }

  .form-item input {
    padding: 11px 11px 11px 38px;
    font-size: 14px;
    border-radius: 10px;
  }

  .input-icon {
    left: 12px;
    width: 18px;
    height: 18px;
  }

  .form-options {
    margin-top: 4px;
  }

  .checkbox-wrapper {
    gap: 8px;
  }

  .checkmark {
    width: 18px;
    height: 18px;
  }

  .checkbox-label {
    font-size: 13px;
  }

  .register-btn {
    padding: 11px;
    font-size: 14px;
    border-radius: 10px;
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