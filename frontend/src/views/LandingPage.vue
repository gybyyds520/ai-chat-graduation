<template>
  <div class="landing-page">
    <!-- 导航栏（Electron环境下隐藏，使用App.vue的自定义标题栏） -->
    <nav class="navbar" v-if="!isElectron">
      <div class="nav-container">
        <div class="nav-logo">
          <svg width="40" height="40" viewBox="0 0 48 48" fill="none">
            <rect width="48" height="48" rx="12" fill="url(#logoGrad)"/>
            <path d="M24 14C18.48 14 14 18.48 14 24s4.48 10 10 10 10-4.48 10-10S29.52 14 24 14zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z" fill="white"/>
            <defs>
              <linearGradient id="logoGrad" x1="0" y1="0" x2="48" y2="48">
                <stop stop-color="#4f46e5"/>
                <stop offset="1" stop-color="#7c3aed"/>
              </linearGradient>
            </defs>
          </svg>
          <span>AI Chat</span>
        </div>
        <div class="nav-links">
          <a href="#features" class="nav-link">功能特性</a>
          <a href="#about" class="nav-link">关于我们</a>
          <a href="#pricing" class="nav-link">价格方案</a>
        </div>
        <div class="nav-actions">
          <button class="btn-outline" @click="goToLogin">登录</button>
          <button class="btn-primary" @click="goToRegister">免费注册</button>
        </div>
        <button class="mobile-menu-btn" @click="mobileMenuOpen = !mobileMenuOpen">
          <span></span>
          <span></span>
          <span></span>
        </button>
      </div>
    </nav>

    <!-- 移动端菜单 -->
    <div v-if="mobileMenuOpen" class="mobile-menu">
      <a href="#features" class="mobile-link" @click="mobileMenuOpen = false">功能特性</a>
      <a href="#about" class="mobile-link" @click="mobileMenuOpen = false">关于我们</a>
      <a href="#pricing" class="mobile-link" @click="mobileMenuOpen = false">价格方案</a>
      <div class="mobile-actions">
        <button class="btn-outline" @click="goToLogin">登录</button>
        <button class="btn-primary" @click="goToRegister">免费注册</button>
      </div>
    </div>

    <!-- Hero区域 -->
    <section class="hero">
      <div class="hero-bg">
        <div class="gradient-1"></div>
        <div class="gradient-2"></div>
        <div class="gradient-3"></div>
        <div class="grid-bg"></div>
        <div class="floating-elements">
          <div class="float-element element-1"></div>
          <div class="float-element element-2"></div>
          <div class="float-element element-3"></div>
        </div>
      </div>
      <div class="hero-content">
        <div class="hero-text">
          <div class="badge">全新升级</div>
          <h1>智能对话<br/>连接未来</h1>
          <p>基于先进大语言模型，为您提供智能、流畅的AI对话体验。创建专属角色，开启无限可能。</p>
          <div class="hero-actions">
            <button class="btn-primary btn-large" @click="goToRegister">
              立即开始
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </button>
            <button class="btn-secondary" @click="scrollToFeatures">
              了解更多
            </button>
          </div>
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-value">100K+</span>
              <span class="stat-label">活跃用户</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">99.9%</span>
              <span class="stat-label">服务可用性</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">24/7</span>
              <span class="stat-label">全天候服务</span>
            </div>
          </div>
        </div>
        <div class="hero-preview">
          <div class="preview-card">
            <div class="preview-header">
              <div class="preview-avatar">🤖</div>
              <div class="preview-info">
                <span class="preview-name">AI助手</span>
                <span class="preview-status online">在线</span>
              </div>
            </div>
            <div class="preview-messages">
              <div class="message user">你好！</div>
              <div class="message ai">您好！我是您的AI助手，有什么可以帮您的？</div>
              <div class="message user">介绍一下你自己</div>
              <div class="message ai typing">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 功能特性 -->
    <section id="features" class="features">
      <div class="section-header">
        <h2>强大功能</h2>
        <p>为您打造全方位的智能对话体验</p>
      </div>
      <div class="features-grid">
        <div class="feature-card">
          <div class="feature-icon">🤖</div>
          <h3>智能对话</h3>
          <p>基于最新大语言模型，提供精准、自然的对话体验</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon">🎭</div>
          <h3>角色定制</h3>
          <p>创建专属AI角色，定义独特性格和技能</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon">🔒</div>
          <h3>隐私安全</h3>
          <p>端到端加密，保护您的每一次对话</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon">💬</div>
          <h3>多轮对话</h3>
          <p>支持上下文理解，连贯的多轮对话体验</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon">🔊</div>
          <h3>语音交互</h3>
          <p>语音识别与合成，解放双手，自然交流</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon">🌐</div>
          <h3>多语言支持</h3>
          <p>支持多种语言，跨越语言障碍</p>
        </div>
      </div>
    </section>

    <!-- 关于我们 -->
    <section id="about" class="about">
      <div class="about-content">
        <div class="about-text">
          <h2>为什么选择我们</h2>
          <p>我们致力于打造最优质的AI对话平台，让人工智能触手可及。</p>
          <ul class="about-list">
            <li>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
              <span>领先的技术架构</span>
            </li>
            <li>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
              <span>持续的产品迭代</span>
            </li>
            <li>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
              <span>专业的技术支持</span>
            </li>
          </ul>
        </div>
        <div class="about-image">
          <div class="image-container">
            <div class="image-glow"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- 价格方案 -->
    <section id="pricing" class="pricing">
      <div class="section-header">
        <h2>价格方案</h2>
        <p>选择适合您的方案</p>
      </div>
      <div class="pricing-grid">
        <div class="pricing-card">
          <h3>免费版</h3>
          <div class="price">
            <span class="currency">¥</span>
            <span class="amount">0</span>
            <span class="period">/月</span>
          </div>
          <ul class="pricing-features">
            <li>基础AI对话</li>
            <li>每日10次对话</li>
            <li>3个自定义角色</li>
            <li>基础语音功能</li>
          </ul>
          <button class="btn-outline" @click="goToRegister">开始使用</button>
        </div>
        <div class="pricing-card popular">
          <div class="popular-badge">推荐</div>
          <h3>专业版</h3>
          <div class="price">
            <span class="currency">¥</span>
            <span class="amount">99</span>
            <span class="period">/月</span>
          </div>
          <ul class="pricing-features">
            <li>高级AI对话</li>
            <li>无限对话次数</li>
            <li>无限自定义角色</li>
            <li>高级语音功能</li>
            <li>优先技术支持</li>
            <li>数据导出</li>
          </ul>
          <button class="btn-primary" @click="goToRegister">立即升级</button>
        </div>
        <div class="pricing-card">
          <h3>企业版</h3>
          <div class="price">
            <span class="currency">¥</span>
            <span class="amount">299</span>
            <span class="period">/月</span>
          </div>
          <ul class="pricing-features">
            <li>全部专业版功能</li>
            <li>私有化部署</li>
            <li>专属客户经理</li>
            <li>定制开发支持</li>
            <li>SLA保障</li>
          </ul>
          <button class="btn-outline" @click="goToRegister">联系我们</button>
        </div>
      </div>
    </section>

    <!-- CTA区域 -->
    <section class="cta">
      <div class="cta-content">
        <h2>准备好开启智能对话之旅了吗？</h2>
        <p>加入10万+用户，体验未来对话方式</p>
        <div class="cta-actions">
          <button class="btn-primary btn-large" @click="goToRegister">
            免费注册
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"/>
            </svg>
          </button>
          <button class="btn-secondary" @click="goToLogin">已有账号？登录</button>
        </div>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-section">
          <div class="footer-logo">
            <svg width="32" height="32" viewBox="0 0 48 48" fill="none">
              <rect width="48" height="48" rx="12" fill="url(#footerLogoGrad)"/>
              <path d="M24 14C18.48 14 14 18.48 14 24s4.48 10 10 10 10-4.48 10-10S29.52 14 24 14zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z" fill="white"/>
              <defs>
                <linearGradient id="footerLogoGrad" x1="0" y1="0" x2="48" y2="48">
                  <stop stop-color="#4f46e5"/>
                  <stop offset="1" stop-color="#7c3aed"/>
                </linearGradient>
              </defs>
            </svg>
            <span>AI Chat</span>
          </div>
          <p>智能对话，连接未来</p>
        </div>
        <div class="footer-section">
          <h4>产品</h4>
          <ul>
            <li><a href="#features">功能特性</a></li>
            <li><a href="#pricing">价格方案</a></li>
            <li><a href="#">API文档</a></li>
            <li><a href="#">更新日志</a></li>
          </ul>
        </div>
        <div class="footer-section">
          <h4>公司</h4>
          <ul>
            <li><a href="#about">关于我们</a></li>
            <li><a href="#">联系我们</a></li>
            <li><a href="#">加入我们</a></li>
            <li><a href="#">合作伙伴</a></li>
          </ul>
        </div>
        <div class="footer-section">
          <h4>法律</h4>
          <ul>
            <li><a href="#">隐私政策</a></li>
            <li><a href="#">服务条款</a></li>
            <li><a href="#">Cookie政策</a></li>
          </ul>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2024 AI Chat. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const mobileMenuOpen = ref(false)
const isElectron = ref(false)

if (typeof window !== 'undefined') {
  isElectron.value = window.electronAPI !== undefined
}

const goToLogin = () => {
  router.push('/login')
}

const goToRegister = () => {
  router.push('/register')
}

const scrollToFeatures = () => {
  document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' })
}
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  background: #0f172a;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 导航栏 */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: #f8fafc;
}

.nav-links {
  display: flex;
  gap: 32px;
}

.nav-link {
  color: #cbd5e1;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s ease;
}

.nav-link:hover {
  color: #f8fafc;
}

.nav-actions {
  display: flex;
  gap: 12px;
}

.btn-outline {
  padding: 10px 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: transparent;
  color: #f8fafc;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-outline:hover {
  border-color: #4f46e5;
  color: #4f46e5;
}

.btn-primary {
  padding: 10px 20px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 15px rgba(79, 70, 229, 0.4);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(79, 70, 229, 0.5);
}

.btn-large {
  padding: 14px 28px;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-secondary {
  padding: 14px 28px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: #f8fafc;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.15);
}

.mobile-menu-btn {
  display: none;
  flex-direction: column;
  gap: 5px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
}

.mobile-menu-btn span {
  width: 24px;
  height: 2px;
  background: #f8fafc;
  border-radius: 2px;
}

.mobile-menu {
  display: none;
  position: fixed;
  top: 72px;
  left: 0;
  right: 0;
  background: rgba(15, 23, 42, 0.95);
  backdrop-filter: blur(20px);
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.mobile-link {
  display: block;
  padding: 12px 0;
  color: #cbd5e1;
  text-decoration: none;
  font-size: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.mobile-link:last-of-type {
  border-bottom: none;
}

.mobile-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.mobile-actions .btn-outline,
.mobile-actions .btn-primary {
  flex: 1;
}

/* Hero区域 */
.hero {
  min-height: 100vh;
  display: flex;
  align-items: center;
  position: relative;
  overflow: hidden;
  padding-top: 72px;
}

.hero-bg {
  position: absolute;
  inset: 0;
}

.gradient-1 {
  position: absolute;
  top: -20%;
  left: -10%;
  width: 600px;
  height: 600px;
  background: radial-gradient(ellipse, rgba(79, 70, 229, 0.3), transparent);
  animation: float 20s ease-in-out infinite;
}

.gradient-2 {
  position: absolute;
  bottom: -20%;
  right: -10%;
  width: 500px;
  height: 500px;
  background: radial-gradient(ellipse, rgba(124, 58, 237, 0.2), transparent);
  animation: float 15s ease-in-out infinite reverse;
}

.gradient-3 {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 800px;
  height: 800px;
  background: radial-gradient(ellipse, rgba(6, 182, 212, 0.1), transparent);
}

.grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
}

.floating-elements {
  position: absolute;
  inset: 0;
}

.float-element {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.4), transparent);
}

.element-1 {
  width: 100px;
  height: 100px;
  top: 20%;
  right: 15%;
  animation: floatUpDown 6s ease-in-out infinite;
}

.element-2 {
  width: 60px;
  height: 60px;
  top: 60%;
  left: 10%;
  animation: floatUpDown 8s ease-in-out infinite 2s;
}

.element-3 {
  width: 80px;
  height: 80px;
  bottom: 25%;
  right: 30%;
  animation: floatUpDown 7s ease-in-out infinite 1s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, 30px); }
}

@keyframes floatUpDown {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: center;
}

.hero-text {
  color: #f8fafc;
}

.badge {
  display: inline-block;
  padding: 6px 16px;
  background: rgba(79, 70, 229, 0.2);
  border: 1px solid rgba(79, 70, 229, 0.3);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  color: #a5b4fc;
  margin-bottom: 24px;
}

.hero-text h1 {
  font-size: 56px;
  font-weight: 800;
  line-height: 1.1;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #ffffff, #c7d2fe, #e0e7ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 30px rgba(199, 210, 254, 0.3);
}

.hero-text p {
  font-size: 18px;
  color: #cbd5e1;
  line-height: 1.6;
  margin-bottom: 32px;
  max-width: 450px;
}

.hero-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 40px;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #f8fafc;
}

.stat-label {
  font-size: 14px;
  color: #94a3b8;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.1);
}

.hero-preview {
  display: flex;
  justify-content: center;
}

.preview-card {
  width: 360px;
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(79, 70, 229, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.preview-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.preview-name {
  font-weight: 600;
  color: #f8fafc;
}

.preview-status {
  font-size: 12px;
  color: #64748b;
}

.preview-status.online {
  color: #10b981;
}

.preview-messages {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 14px;
}

.message.user {
  align-self: flex-end;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.ai {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.05);
  color: #f1f5f9;
  border-bottom-left-radius: 4px;
}

.message.typing {
  display: flex;
  gap: 6px;
  background: rgba(255, 255, 255, 0.05);
  padding: 12px 14px;
}

.typing-dot {
  width: 8px;
  height: 8px;
  background: #94a3b8;
  border-radius: 50%;
  animation: typing 1.4s ease-in-out infinite;
}

.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

/* 功能特性 */
.features {
  padding: 100px 24px;
}

.section-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-header h2 {
  font-size: 36px;
  font-weight: 700;
  color: #f8fafc;
  margin-bottom: 12px;
}

.section-header p {
  font-size: 16px;
  color: #cbd5e1;
}

.features-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  padding: 32px;
  transition: all 0.3s ease;
}

.feature-card:hover {
  border-color: #4f46e5;
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(79, 70, 229, 0.2);
}

.feature-icon {
  font-size: 40px;
  margin-bottom: 16px;
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 600;
  color: #f8fafc;
  margin-bottom: 8px;
}

.feature-card p {
  font-size: 14px;
  color: #cbd5e1;
  line-height: 1.5;
}

/* 关于我们 */
.about {
  padding: 100px 24px;
  background: rgba(30, 41, 59, 0.3);
}

.about-content {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: center;
}

.about-text h2 {
  font-size: 32px;
  font-weight: 700;
  color: #f8fafc;
  margin-bottom: 16px;
}

.about-text p {
  font-size: 16px;
  color: #cbd5e1;
  line-height: 1.6;
  margin-bottom: 32px;
}

.about-list {
  list-style: none;
  padding: 0;
}

.about-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  color: #f1f5f9;
  font-size: 15px;
}

.about-list li svg {
  color: #10b981;
}

.about-image {
  display: flex;
  justify-content: center;
}

.image-container {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.2), rgba(124, 58, 237, 0.2));
  border-radius: 24px;
  position: relative;
}

.image-glow {
  position: absolute;
  inset: 20px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.3), transparent);
  border-radius: 20px;
}

/* 价格方案 */
.pricing {
  padding: 100px 24px;
}

.pricing-grid {
  max-width: 1000px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.pricing-card {
  background: rgba(30, 41, 59, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 40px 32px;
  position: relative;
  transition: all 0.3s ease;
}

.pricing-card:hover {
  border-color: #4f46e5;
  transform: translateY(-4px);
}

.pricing-card.popular {
  border-color: #4f46e5;
  background: rgba(79, 70, 229, 0.1);
  box-shadow: 0 0 40px rgba(79, 70, 229, 0.2);
}

.popular-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  padding: 4px 16px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: white;
}

.pricing-card h3 {
  font-size: 20px;
  font-weight: 600;
  color: #f8fafc;
  margin-bottom: 16px;
}

.price {
  margin-bottom: 24px;
}

.currency {
  font-size: 24px;
  font-weight: 600;
  color: #94a3b8;
}

.amount {
  font-size: 48px;
  font-weight: 700;
  color: #f8fafc;
}

.period {
  font-size: 16px;
  color: #94a3b8;
}

.pricing-features {
  list-style: none;
  padding: 0;
  margin-bottom: 32px;
}

.pricing-features li {
  padding: 8px 0;
  color: #cbd5e1;
  font-size: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.pricing-features li:last-child {
  border-bottom: none;
}

.pricing-card .btn-primary,
.pricing-card .btn-outline {
  width: 100%;
}

/* CTA区域 */
.cta {
  padding: 100px 24px;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.15), rgba(124, 58, 237, 0.1));
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.cta-content {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
}

.cta-content h2 {
  font-size: 36px;
  font-weight: 700;
  color: #f8fafc;
  margin-bottom: 16px;
}

.cta-content p {
  font-size: 18px;
  color: #cbd5e1;
  margin-bottom: 40px;
}

.cta-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

/* 页脚 */
.footer {
  padding: 60px 24px;
  background: rgba(15, 23, 42, 0.95);
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 40px;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #f8fafc;
  margin-bottom: 12px;
}

.footer-section p {
  color: #94a3b8;
  font-size: 14px;
}

.footer-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #f8fafc;
  margin-bottom: 16px;
}

.footer-section ul {
  list-style: none;
  padding: 0;
}

.footer-section li {
  padding: 6px 0;
}

.footer-section a {
  color: #94a3b8;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s ease;
}

.footer-section a:hover {
  color: #f8fafc;
}

.footer-bottom {
  max-width: 1200px;
  margin: 40px auto 0;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .hero-content {
    grid-template-columns: 1fr;
    gap: 40px;
    text-align: center;
  }

  .hero-text h1 {
    font-size: 44px;
  }

  .hero-stats {
    justify-content: center;
  }

  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .about-content {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .pricing-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .nav-links {
    display: none;
  }

  .nav-actions {
    display: none;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .mobile-menu {
    display: block;
  }

  .hero-text h1 {
    font-size: 36px;
  }

  .hero-text p {
    font-size: 16px;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-actions button {
    width: 100%;
    justify-content: center;
  }

  .preview-card {
    width: 100%;
    max-width: 320px;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .pricing-grid {
    grid-template-columns: 1fr;
  }

  .footer-content {
    grid-template-columns: repeat(2, 1fr);
    gap: 32px;
  }
}

@media (max-width: 480px) {
  .nav-container {
    padding: 12px 16px;
  }

  .hero-text h1 {
    font-size: 28px;
  }

  .hero-text p {
    font-size: 14px;
  }

  .stat-value {
    font-size: 22px;
  }

  .section-header h2 {
    font-size: 28px;
  }

  .cta-content h2 {
    font-size: 28px;
  }

  .footer-content {
    grid-template-columns: 1fr;
  }
}
</style>