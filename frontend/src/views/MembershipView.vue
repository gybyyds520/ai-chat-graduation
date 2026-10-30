<template>
  <div class="membership-container">
    <h2 class="membership-title">会员等级与用量</h2>

    <!-- 三档等级卡片 -->
    <div class="tier-cards">
      <div
        v-for="tier in tierList"
        :key="tier.code"
        class="tier-card"
        :class="{ recommended: tier.code === 'PREMIUM' }"
      >
        <h3>{{ tier.label }}</h3>
        <div class="price">¥{{ tier.price }}/月</div>
        <ul>
          <li v-for="(limit, feature) in tier.quotas" :key="feature">
            {{ featureLabel(feature) }}：{{ limit === 0 ? '不可用' : '每日 ' + limit + ' 次' }}
          </li>
        </ul>
        <button
          class="upgrade-btn"
          :disabled="!canUpgrade(currentTier, tier.code)"
          @click="onUpgrade(tier.code)"
        >
          {{ currentTier === tier.code ? '当前等级' : (canUpgrade(currentTier, tier.code) ? '升级到此等级' : '已最高') }}
        </button>
      </div>
    </div>

    <!-- 当日用量配额 -->
    <div class="quota-panel">
      <h3>今日用量（{{ resetDate }} 重置）</h3>
      <div v-for="feature in featureList" :key="feature" class="quota-row">
        <span class="quota-label">{{ featureLabel(feature) }}</span>
        <div class="bar">
          <div class="bar-inner" :style="{ width: usagePercent(feature) + '%' }"></div>
        </div>
        <span class="quota-num">{{ usageUsed(feature) }} / {{ usageLimit(feature) }}</span>
      </div>
      <p v-if="hasExceeded" class="exceed-tip">部分功能今日已达上限，可升级会员或明日再试。</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { MEMBERSHIP_TIERS, FEATURE_LABELS, canUpgradeFrom, getTier } from '../data/membership'

const props = defineProps({
  currentTier: { type: String, default: 'FREE' },
  used: { type: Object, default: () => ({}) },
  resetDate: { type: String, default: '' }
})

const tierList = computed(() => [MEMBERSHIP_TIERS.FREE, MEMBERSHIP_TIERS.BASIC, MEMBERSHIP_TIERS.PREMIUM])
const featureList = computed(() => Object.keys(FEATURE_LABELS))

function featureLabel(f) {
  return FEATURE_LABELS[f] || f
}
function usageLimit(f) {
  return getTier(props.currentTier).quotas[f] ?? 0
}
function usageUsed(f) {
  return props.used[f] ?? 0
}
function usagePercent(f) {
  const limit = usageLimit(f)
  if (limit <= 0) return 100
  return Math.min(100, Math.round((usageUsed(f) / limit) * 100))
}
function hasExceeded() {
  return featureList.value.some(
    (f) => usageLimit(f) > 0 && usageUsed(f) >= usageLimit(f)
  )
}
function onUpgrade(code) {
  if (typeof props.onUpgrade === 'function') props.onUpgrade(code)
}
</script>

<style scoped>
.membership-container { padding: 16px; }
.tier-cards { display: flex; gap: 12px; flex-wrap: wrap; }
.tier-card { flex: 1; min-width: 200px; border: 1px solid #ddd; border-radius: 10px; padding: 16px; }
.tier-card.recommended { border-color: #f5a623; }
.price { font-size: 22px; font-weight: 700; margin: 8px 0; }
.upgrade-btn { margin-top: 10px; width: 100%; padding: 8px; border-radius: 6px; border: none; background: #4a90e2; color: #fff; }
.upgrade-btn:disabled { background: #bbb; }
.quota-panel { margin-top: 20px; }
.quota-row { display: flex; align-items: center; gap: 10px; margin: 8px 0; }
.quota-label { width: 80px; }
.bar { flex: 1; height: 8px; background: #eee; border-radius: 4px; overflow: hidden; }
.bar-inner { height: 100%; background: #4a90e2; }
.quota-num { width: 70px; text-align: right; }
.exceed-tip { color: #d33; }
</style>
