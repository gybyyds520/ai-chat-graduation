/**
 * 会员分级与配额纯逻辑配置（与后端 MembershipLevel 对应）。
 *
 * 纯函数、无副作用，便于单元测试。
 */

export const MEMBERSHIP_TIERS = {
  FREE: {
    code: 'FREE',
    label: '免费版',
    price: 0,
    advanced: false,
    quotas: {
      ai_chat: 20,
      voice_call: 5,
      image_gen: 0,
      doc_upload: 0
    }
  },
  BASIC: {
    code: 'BASIC',
    label: '基础版',
    price: 29,
    advanced: true,
    quotas: {
      ai_chat: 100,
      voice_call: 20,
      image_gen: 5,
      doc_upload: 3
    }
  },
  PREMIUM: {
    code: 'PREMIUM',
    label: '高级版',
    price: 99,
    advanced: true,
    quotas: {
      ai_chat: 500,
      voice_call: 100,
      image_gen: 20,
      doc_upload: 10
    }
  }
}

export const FEATURE_LABELS = {
  ai_chat: 'AI 聊天',
  voice_call: '语音通话',
  image_gen: '图片生成',
  doc_upload: '文档上传'
}

/** 根据等级 code 获取等级配置，非法值回退 FREE。 */
export function getTier(code) {
  return MEMBERSHIP_TIERS[code] || MEMBERSHIP_TIERS.FREE
}

/** 判断是否可升级到目标等级（严格递增）。 */
export function canUpgradeFrom(fromCode, targetCode) {
  const order = ['FREE', 'BASIC', 'PREMIUM']
  const from = order.indexOf(fromCode)
  const target = order.indexOf(targetCode)
  return from !== -1 && target !== -1 && target > from
}

/**
 * 计算某功能的用量状态。
 * @param {string} tierCode 等级
 * @param {string} feature 功能
 * @param {number} used 已用次数
 * @returns {{used:number, limit:number, remaining:number, available:boolean, percent:number}}
 */
export function usageStatus(tierCode, feature, used = 0) {
  const tier = getTier(tierCode)
  const limit = tier.quotas[feature] ?? 0
  const safeUsed = Math.max(0, used)
  const remaining = Math.max(0, limit - safeUsed)
  const available = limit > 0 && safeUsed < limit
  const percent = limit > 0 ? Math.min(100, Math.round((safeUsed / limit) * 100)) : 100
  return { used: safeUsed, limit, remaining, available, percent }
}
