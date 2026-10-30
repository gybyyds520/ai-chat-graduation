<template>
  <div class="agreement-page">
    <div class="agreement-container">
      <header class="agreement-header">
        <button class="back-btn" @click="goBack" type="button">← 返回</button>
        <h1>法律文档</h1>
        <p>请您仔细阅读以下条款，使用本产品即视为同意全部内容</p>
      </header>

      <div class="tab-bar" role="tablist">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          role="tab"
          :aria-selected="activeTab === tab.key"
          class="tab"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <section class="doc-panel">
        <h2 class="doc-title">{{ activeDoc.title }}</h2>
        <p v-if="activeDoc.preamble" class="doc-preamble">{{ activeDoc.preamble }}</p>

        <article
          v-for="clause in activeDoc.clauses"
          :key="clause.index"
          class="clause"
        >
          <h3 class="clause-title">第{{ formatIndex(clause.index) }}条 {{ clause.title }}</h3>
          <p class="clause-body">{{ clause.body }}</p>
        </article>

        <p class="doc-footer">— 文档结束 —</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  parseAgreementSection,
  USER_AGREEMENT_TEXT,
  PRIVACY_POLICY_TEXT
} from '@/utils/legalDocs'

const router = useRouter()
const route = useRoute()

const tabs = [
  { key: 'agreement', label: '用户协议' },
  { key: 'privacy', label: '隐私政策' }
]

const agreementDoc = parseAgreementSection(USER_AGREEMENT_TEXT)
const privacyDoc = parseAgreementSection(PRIVACY_POLICY_TEXT)

const initialTab = route.query.tab === 'privacy' ? 'privacy' : 'agreement'
const activeTab = ref(initialTab)
const activeDoc = computed(() =>
  activeTab.value === 'privacy' ? privacyDoc : agreementDoc
)

// 将条款序号渲染为"一/二/…/十一"等中文形式（与协议正文一致）
const INDEX_CN = ['', '一', '二', '三', '四', '五', '六', '七', '八', '九', '十',
  '十一', '十二', '十三', '十四', '十五']
function formatIndex(n) {
  if (typeof n === 'number' && n >= 1 && n <= INDEX_CN.length - 1) {
    return INDEX_CN[n]
  }
  return String(n)
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/login')
  }
}
</script>

<style scoped>
.agreement-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 24px 16px;
  box-sizing: border-box;
}

.agreement-container {
  max-width: 860px;
  margin: 0 auto;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.06);
  padding: 32px 36px;
}

.agreement-header {
  margin-bottom: 20px;
}

.back-btn {
  border: none;
  background: transparent;
  color: #4f46e5;
  font-size: 14px;
  cursor: pointer;
  padding: 0;
  margin-bottom: 12px;
}

.back-btn:hover {
  text-decoration: underline;
}

.agreement-header h1 {
  font-size: 26px;
  margin: 0 0 6px;
  color: #0f172a;
}

.agreement-header p {
  color: #64748b;
  font-size: 14px;
  margin: 0;
}

.tab-bar {
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 24px;
}

.tab {
  border: none;
  background: transparent;
  padding: 10px 18px;
  font-size: 15px;
  color: #475569;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}

.tab.active {
  color: #4f46e5;
  border-bottom-color: #4f46e5;
  font-weight: 600;
}

.doc-title {
  font-size: 22px;
  color: #0f172a;
  margin: 0 0 12px;
}

.doc-preamble {
  color: #475569;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-line;
  background: #f8fafc;
  border-left: 3px solid #c7d2fe;
  padding: 12px 16px;
  border-radius: 6px;
  margin: 0 0 24px;
}

.clause {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px dashed #e2e8f0;
}

.clause:last-of-type {
  border-bottom: none;
}

.clause-title {
  font-size: 16px;
  color: #1e293b;
  margin: 0 0 8px;
  font-weight: 600;
}

.clause-body {
  color: #334155;
  font-size: 14px;
  line-height: 1.8;
  margin: 0;
  white-space: pre-line;
}

.doc-footer {
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .agreement-container {
    padding: 20px 18px;
  }

  .agreement-header h1 {
    font-size: 22px;
  }

  .tab {
    padding: 8px 12px;
    font-size: 14px;
  }
}
</style>
