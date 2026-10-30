/**
 * 会员权益对比纯数据与计算（便于单元测试）。
 *
 * 免费版 vs 会员版：消息数 / 语音时长 / 图片生成 / 知识库文件数。
 * 提供对比表数据结构、超额判定、升级引导亮点文案。
 */

/**
 * 各档位配额（每日 / 总量口径在 value 中以文本说明，limit 为数值阈值）。
 * limit=null 表示不限。
 */
export const PLANS = {
  free: {
    key: 'free',
    name: '免费版',
    price: '¥0',
    highlight: false,
    quotas: {
      messages: { label: '每日消息数', value: '50 条/天', limit: 50 },
      voiceMinutes: { label: '语音时长', value: '10 分钟/天', limit: 10 },
      imageGeneration: { label: '图片生成', value: '3 张/天', limit: 3 },
      kbFiles: { label: '知识库文件数', value: '5 个', limit: 5 }
    }
  },
  member: {
    key: 'member',
    name: '会员版',
    price: '¥19.9/月',
    highlight: true,
    quotas: {
      messages: { label: '每日消息数', value: '不限', limit: null },
      voiceMinutes: { label: '语音时长', value: '300 分钟/天', limit: 300 },
      imageGeneration: { label: '图片生成', value: '50 张/天', limit: 50 },
      kbFiles: { label: '知识库文件数', value: '100 个', limit: 100 }
    }
  }
}

/**
 * 生成两档对比表行：每行 { key, label, free, member, better }。
 * better='member' | 'free' | 'tie'。
 */
export function buildComparisonRows() {
  const free = PLANS.free.quotas
  const member = PLANS.member.quotas
  const rows = []
  for (const key of Object.keys(free)) {
    const fLimit = free[key].limit
    const mLimit = member[key].limit
    let better = 'tie'
    if (mLimit === null) {
      better = 'member'
    } else if (fLimit === null) {
      better = 'free'
    } else if (mLimit > fLimit) {
      better = 'member'
    } else if (mLimit < fLimit) {
      better = 'free'
    }
    rows.push({
      key,
      label: free[key].label,
      free: free[key].value,
      member: member[key].value,
      better
    })
  }
  return rows
}

/**
 * 判断当前用量是否在某档下超额。
 * @param {string} quotaKey messages/voiceMinutes/imageGeneration/kbFiles
 * @param {number} used 已用量
 * @param {object} plan 档位（PLANS.free / PLANS.member）
 * @returns {boolean}
 */
export function isOverQuota(quotaKey, used, plan = PLANS.free) {
  const q = plan && plan.quotas ? plan.quotas[quotaKey] : null
  if (!q) return false
  if (q.limit === null || q.limit === undefined) return false // 不限
  const usedNum = Number(used)
  if (isNaN(usedNum)) return false
  return usedNum >= q.limit
}

/**
 * 给定用量，返回是否需要升级引导，以及触发的额度项。
 * @param {object} usage { messages, voiceMinutes, imageGeneration, kbFiles }
 * @param {object} plan 当前档位
 * @returns {{needUpgrade:boolean, exceeded:string[]}}
 */
export function checkUpgradeNeeded(usage = {}, plan = PLANS.free) {
  const exceeded = []
  for (const key of Object.keys(plan.quotas)) {
    if (isOverQuota(key, usage[key], plan)) {
      exceeded.push(key)
    }
  }
  return { needUpgrade: exceeded.length > 0, exceeded }
}

/**
 * 会员权益亮点文案（升级引导弹窗用）。
 */
export const MEMBER_HIGHLIGHTS = [
  '消息数不限，畅聊无上限',
  '语音时长提升至 300 分钟/天',
  '图片生成 50 张/天，创意不设限',
  '知识库 100 个文件，资料随你喂',
  '去广告 + 优先客服支持'
]

export default {
  PLANS,
  MEMBER_HIGHLIGHTS,
  buildComparisonRows,
  isOverQuota,
  checkUpgradeNeeded
}
