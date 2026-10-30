<template>
  <div class="upgrade-container">
    <div class="upgrade-header">
      <button class="back-btn" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        返回
      </button>
      <h1 class="upgrade-title">🚀 版本升级</h1>
      <p class="upgrade-desc">解锁更多高级功能，畅享AI对话体验</p>
    </div>

    <div class="pricing-section">
      <div class="pricing-card free-card" :class="{ selected: selectedPlan === 'free' }">
        <div class="card-badge">免费</div>
        <h3>基础版</h3>
        <div class="price">
          <span class="currency">¥</span>
          <span class="amount">0</span>
          <span class="period">/永久</span>
        </div>
        <ul class="features">
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            基础AI对话功能
          </li>
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            3个AI角色
          </li>
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            每日100次对话
          </li>
          <li class="feature-item disabled">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            高级角色定制
          </li>
          <li class="feature-item disabled">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            AI投喂功能
          </li>
        </ul>
        <button class="select-btn" :class="{ selected: selectedPlan === 'free' }" @click="selectPlan('free')">
          {{ selectedPlan === 'free' ? '已选择' : '选择方案' }}
        </button>
      </div>

      <div class="pricing-card pro-card" :class="{ selected: selectedPlan === 'pro' }">
        <div class="card-badge popular">推荐</div>
        <h3>专业版</h3>
        <div class="price">
          <span class="currency">¥</span>
          <span class="amount">99</span>
          <span class="period">/月</span>
        </div>
        <div class="discount">或 ¥999/年（省29%）</div>
        <ul class="features">
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            无限AI对话
          </li>
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            无限AI角色创建
          </li>
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            高级角色定制
          </li>
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            AI投喂功能
          </li>
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            优先技术支持
          </li>
        </ul>
        <button class="select-btn primary" @click="selectPlan('pro')">
          {{ selectedPlan === 'pro' ? '已选择' : '立即升级' }}
        </button>
      </div>

      <div class="pricing-card enterprise-card" :class="{ selected: selectedPlan === 'enterprise' }">
        <div class="card-badge">企业版</div>
        <h3>企业版</h3>
        <div class="price">
          <span class="amount">定制</span>
        </div>
        <div class="discount">联系我们获取报价</div>
        <ul class="features">
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            专业版全部功能
          </li>
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            私有化部署
          </li>
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            专属客服支持
          </li>
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            定制开发
          </li>
          <li class="feature-item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            安全审计报告
          </li>
        </ul>
        <button class="select-btn enterprise" @click="selectPlan('enterprise')">
          {{ selectedPlan === 'enterprise' ? '已选择' : '联系销售' }}
        </button>
      </div>
    </div>

    <div v-if="selectedPlan === 'pro'" class="payment-section">
      <h2>选择支付方式</h2>
      <div class="payment-methods">
        <div 
          v-for="method in paymentMethods" 
          :key="method.id"
          :class="['payment-method', { selected: selectedPayment === method.id }]"
          @click="selectedPayment = method.id"
        >
          <div class="method-icon">{{ method.icon }}</div>
          <div class="method-info">
            <div class="method-name">{{ method.name }}</div>
            <div class="method-desc">{{ method.description }}</div>
          </div>
          <div v-if="selectedPayment === method.id" class="check-mark">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </div>
        </div>
      </div>

      <div class="billing-cycle">
        <label class="cycle-option">
          <input type="radio" v-model="billingCycle" value="monthly" />
          <span>月付 ¥99</span>
        </label>
        <label class="cycle-option">
          <input type="radio" v-model="billingCycle" value="yearly" />
          <span>年付 ¥999 <span class="save-badge">省29%</span></span>
        </label>
      </div>

      <div class="total-price">
        <span class="label">应付金额</span>
        <span class="total">{{ billingCycle === 'yearly' ? '¥999' : '¥99' }}</span>
      </div>

      <button class="pay-btn" @click="handlePayment">
        {{ isPaying ? '支付中...' : '确认支付' }}
      </button>
    </div>

    <div v-if="selectedPlan === 'enterprise'" class="contact-section">
      <h2>联系销售</h2>
      <div class="contact-form">
        <el-input v-model="contactInfo.name" placeholder="您的姓名" class="contact-input" />
        <el-input v-model="contactInfo.company" placeholder="公司名称" class="contact-input" />
        <el-input v-model="contactInfo.phone" placeholder="联系电话" class="contact-input" />
        <el-input v-model="contactInfo.email" placeholder="电子邮箱" class="contact-input" />
        <el-input v-model="contactInfo.message" type="textarea" :rows="4" placeholder="请描述您的需求..." class="contact-input" />
        <button class="submit-btn" @click="submitContact">提交咨询</button>
      </div>
    </div>

    <div class="footer-info">
      <p>购买即表示您同意我们的 <a href="#">服务条款</a> 和 <a href="#">隐私政策</a></p>
    </div>

    <el-dialog v-model="paymentSuccessVisible" title="支付成功" width="400px">
      <div class="success-content">
        <div class="success-icon">🎉</div>
        <h3>支付成功！</h3>
        <p>您已成功升级到专业版</p>
        <p class="upgrade-info">高级功能已解锁，畅享AI对话体验</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="closeSuccessModal">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

const selectedPlan = ref('free')
const selectedPayment = ref('wechat')
const billingCycle = ref('monthly')
const isPaying = ref(false)
const paymentSuccessVisible = ref(false)

const contactInfo = reactive({
  name: '',
  company: '',
  phone: '',
  email: '',
  message: ''
})

const paymentMethods = [
  { id: 'wechat', icon: '💳', name: '微信支付', description: '安全便捷的支付方式' },
  { id: 'alipay', icon: '🔷', name: '支付宝', description: '支持花呗、余额宝' },
  { id: 'card', icon: '💵', name: '银行卡', description: '支持各大银行储蓄卡/信用卡' }
]

const goBack = () => {
  router.push('/chat')
}

const selectPlan = (plan) => {
  selectedPlan.value = plan
}

const handlePayment = async () => {
  isPaying.value = true
  
  await new Promise(resolve => setTimeout(resolve, 1500))
  
  isPaying.value = false
  paymentSuccessVisible.value = true
  ElMessage.success('支付成功！')
}

const closeSuccessModal = () => {
  paymentSuccessVisible.value = false
  router.push('/chat')
}

const submitContact = () => {
  if (!contactInfo.name || !contactInfo.phone) {
    ElMessage.warning('请填写姓名和联系电话')
    return
  }
  
  ElMessage.success('提交成功！我们会尽快与您联系')
  contactInfo.name = ''
  contactInfo.company = ''
  contactInfo.phone = ''
  contactInfo.email = ''
  contactInfo.message = ''
}
</script>

<style scoped>
.upgrade-container {
  min-height: 100vh;
  background: var(--bg-primary);
  padding: var(--spacing-lg);
}

.upgrade-header {
  text-align: center;
  margin-bottom: var(--spacing-2xl);
  position: relative;
}

.back-btn {
  position: absolute;
  left: 0;
  top: 0;
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
}

.back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.upgrade-title {
  font-size: 32px;
  font-weight: 700;
  background: var(--gradient-text-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.upgrade-desc {
  font-size: 16px;
  color: var(--text-muted);
}

.pricing-section {
  display: flex;
  gap: var(--spacing-lg);
  justify-content: center;
  max-width: 1200px;
  margin: 0 auto var(--spacing-2xl);
  flex-wrap: wrap;
}

.pricing-card {
  flex: 1;
  min-width: 280px;
  max-width: 320px;
  background: var(--card-bg);
  border: 2px solid var(--border-primary);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  position: relative;
  transition: all var(--transition-normal);
}

.pricing-card:hover {
  transform: translateY(-4px);
}

.pricing-card.selected {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-neon);
}

.card-badge {
  position: absolute;
  top: -12px;
  right: 20px;
  padding: 4px 12px;
  background: var(--bg-tertiary);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}

.card-badge.popular {
  background: var(--gradient-primary);
  color: white;
}

.pricing-card h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-md);
  text-transform: none;
  background: none;
  -webkit-text-fill-color: inherit;
}

.price {
  margin-bottom: var(--spacing-sm);
}

.currency {
  font-size: 24px;
  color: var(--text-secondary);
}

.amount {
  font-size: 48px;
  font-weight: 700;
  color: var(--text-primary);
}

.period {
  font-size: 14px;
  color: var(--text-muted);
}

.discount {
  font-size: 12px;
  color: var(--color-success);
  margin-bottom: var(--spacing-lg);
}

.features {
  list-style: none;
  padding: 0;
  margin: 0 0 var(--spacing-xl);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-primary);
  font-size: 14px;
  color: var(--text-secondary);
}

.feature-item:last-child {
  border-bottom: none;
}

.feature-item.disabled {
  opacity: 0.5;
}

.select-btn {
  width: 100%;
  padding: 12px;
  border: 2px solid var(--border-primary);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-primary);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.select-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.select-btn.selected {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.select-btn.primary {
  background: var(--gradient-primary);
  border-color: transparent;
  color: white;
}

.select-btn.primary:hover {
  box-shadow: var(--shadow-neon);
  transform: translateY(-2px);
}

.select-btn.enterprise {
  background: linear-gradient(135deg, #1e3a8a, #3b82f6);
  border-color: transparent;
  color: white;
}

.payment-section,
.contact-section {
  max-width: 500px;
  margin: 0 auto var(--spacing-xl);
  background: var(--card-bg);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
}

.payment-section h2,
.contact-section h2 {
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-xl);
  text-transform: none;
  background: none;
  -webkit-text-fill-color: inherit;
}

.payment-methods {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: var(--spacing-xl);
}

.payment-method {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 2px solid var(--border-primary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.payment-method:hover {
  border-color: var(--color-primary);
}

.payment-method.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.method-icon {
  font-size: 28px;
}

.method-info {
  flex: 1;
}

.method-name {
  font-weight: 600;
  color: var(--text-primary);
}

.method-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.check-mark {
  color: var(--color-primary);
}

.billing-cycle {
  display: flex;
  justify-content: center;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.cycle-option {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 10px 20px;
  border: 2px solid var(--border-primary);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.cycle-option:hover {
  border-color: var(--color-primary);
}

.cycle-option input:checked + span {
  color: var(--color-primary);
}

.cycle-option input:checked {
  accent-color: var(--color-primary);
}

.save-badge {
  background: var(--color-success-light);
  color: var(--color-success);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  margin-left: 8px;
}

.total-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md);
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-xl);
}

.total-price .label {
  color: var(--text-secondary);
}

.total-price .total {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
}

.pay-btn,
.submit-btn {
  width: 100%;
  padding: 16px;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.pay-btn {
  background: var(--gradient-primary);
  border: none;
  color: white;
}

.pay-btn:hover:not(:disabled) {
  box-shadow: var(--shadow-neon);
  transform: translateY(-2px);
}

.pay-btn:disabled {
  opacity: 0.7;
}

.submit-btn {
  background: var(--gradient-primary);
  border: none;
  color: white;
}

.contact-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.contact-input {
  width: 100%;
}

.footer-info {
  text-align: center;
  padding: var(--spacing-xl);
  color: var(--text-muted);
  font-size: 13px;
}

.footer-info a {
  color: var(--color-primary);
}

.success-content {
  text-align: center;
  padding: var(--spacing-xl);
}

.success-icon {
  font-size: 64px;
  margin-bottom: var(--spacing-lg);
}

.success-content h3 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-sm);
  text-transform: none;
  background: none;
  -webkit-text-fill-color: inherit;
}

.success-content p {
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.upgrade-info {
  font-size: 14px;
  color: var(--color-success) !important;
}

@media (max-width: 768px) {
  .pricing-section {
    flex-direction: column;
    align-items: center;
  }
  
  .pricing-card {
    width: 100%;
    max-width: 100%;
  }
  
  .upgrade-title {
    font-size: 24px;
  }
}
</style>