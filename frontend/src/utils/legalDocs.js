/**
 * 法律文档（用户协议 / 隐私政策）解析工具。
 *
 * 本模块只做纯函数解析，不依赖 Vue / DOM / 网络，便于单元测试。
 * 页面层（UserAgreement.vue）直接导入下方的原始文本常量，
 * 调用 parseAgreementSection 渲染为结构化条款列表。
 */

// ---------- 中文数字归一化（第N条 的 N 可能是阿拉伯数字或中文数字） ----------
const CN_DIGITS = {
  零: 0,
  一: 1,
  二: 2,
  两: 2,
  三: 3,
  四: 4,
  五: 5,
  六: 6,
  七: 7,
  八: 8,
  九: 9,
  十: 10,
  百: 100
}

/**
 * 将中文数字 / 阿拉伯数字字符串归一为整数。
 * 仅覆盖协议条款序号所需的 1~999 范围；超出或无法解析时回退为 NaN。
 */
export function normalizeChineseNumber(token) {
  if (token === null || token === undefined) return NaN
  const str = String(token).trim()
  if (/^\d+$/.test(str)) return parseInt(str, 10)

  let result = 0
  let current = 0
  let hasDigit = false
  for (const ch of str) {
    const v = CN_DIGITS[ch]
    if (v === undefined) continue
    hasDigit = true
    if (v === 10 || v === 100) {
      result += (current === 0 ? 1 : current) * v
      current = 0
    } else {
      current = v
    }
  }
  result += current
  return hasDigit ? result : NaN
}

// ---------- 协议文本常量 ----------
// 约定：每条以 "第N条 标题" 开头，其后到下一条之前的非空行作为该条正文。

export const USER_AGREEMENT_TEXT = `用户协议

更新日期：2026年9月17日
生效日期：2026年9月17日

欢迎您使用 AI Chat（以下简称"本产品"）。请您在注册或使用本产品之前，认真阅读并充分理解本协议全部内容。您勾选"已阅读并同意"或实际使用本产品，即视为您已充分阅读、理解并接受本协议的全部内容。

第一条 协议的确认与接受
您通过互联网自行登录或注册本产品，即表示您自愿接受本协议的全部约定。若您不同意本协议的任意内容，请立即停止注册或使用本产品。

第二条 账号注册与使用
您承诺以真实、准确、合法的信息注册账号，并在账号信息发生变更时及时更新。您应妥善保管账号与密码，因您主动泄露或保管不善导致的损失由您自行承担。

第三条 用户行为规范
您承诺不利用本产品从事违法违规活动，不发布违法、有害、侵权、骚扰、色情、暴力等内容，不干扰本产品的正常运行，不恶意请求或滥用系统资源。

第四条 内容归属与使用
您在本产品中输入或生成的内容，归您本人所有。为提供对话、记忆、推荐等服务所必需，您授予本产品在全球范围内、免费地存储、处理、展示上述内容的非独占许可。

第五条 AI 生成内容免责声明
本产品由人工智能模型生成的内容仅供参考，不构成任何专业建议（包括但不限于法律、医疗、投资建议）。您应自行判断并承担使用相关内容的风险。

第六条 付费服务与会员权益
本产品提供会员、点数等付费服务。付费成功后，相应权益将在订单支付完成并经过对账确认后发放。付费服务一经购买，除法律另有规定或本协议另有约定外，不予退款。

第七条 退款政策
因本产品原因导致服务不可用或权益未按时发放的，您可申请退款。具体退款流程、时效与规则以《支付流程说明》文档及实际活动规则为准。

第八条 协议的变更
本产品有权根据业务调整适时修改本协议，并通过站内公告、页面提示等方式通知您。若您在协议变更后继续使用本产品，即视为接受变更后的协议。

第九条 违约处理
若您违反本协议约定，本产品有权视情节采取警告、限制功能、暂停或终止服务、封禁账号等措施，并保留追究法律责任的权利。

第十条 服务的中断与终止
因系统维护、升级、不可抗力、网络故障等原因导致的服务中断，本产品不承担赔偿责任，但将尽可能提前公告并尽快恢复。

第十一条 法律适用与争议解决
本协议的订立、执行、解释及争议解决均适用中华人民共和国法律。因本协议产生的争议，双方应友好协商解决；协商不成的，提交本产品运营方所在地有管辖权的人民法院诉讼解决。

第十二条 其他
本协议条款无论因何种原因部分无效，其余条款仍保持有效。本产品未行使或迟延行使本协议项下的权利，不构成对该权利的放弃。
`

export const PRIVACY_POLICY_TEXT = `隐私政策

更新日期：2026年9月17日
生效日期：2026年9月17日

本产品非常重视用户的个人信息与隐私保护。本政策旨在向您说明我们如何收集、使用、存储、共享和保护您的个人信息，以及您如何管理您的个人信息。

第一条 我们收集的信息
我们会收集您主动提供的注册信息（如用户名、邮箱）、您在使用过程中产生的对话内容、角色设定、动态、设备与日志信息，以及为完成支付所必需的订单信息。

第二条 信息的使用
我们使用收集的信息为您提供账号登录、智能对话、角色生成、好友与社区、会员权益发放、问题排查与安全风控等基础业务功能。

第三条 信息的存储与保护
您的个人信息存储于中华人民共和国境内的服务器。我们采用加密传输、访问控制、审计日志等技术与管理措施保护您的信息安全。

第四条 信息的共享与披露
除非获得您的明确同意或法律法规要求，我们不会向第三方共享、转让您的个人信息。在与微信支付、支付宝等支付渠道交互时，我们仅传递完成支付所必需的最小信息。

第五条 Cookie 与同类技术
本产品可能使用本地存储、会话缓存等技术记录您的登录状态、主题偏好等，以提升使用体验。您可以通过浏览器或系统设置清理这些数据。

第六条 账号权限
您有权访问、更正、导出您的个人信息，也有权申请注销账号。账号注销后，我们将删除或匿名化您的个人信息，法律法规另有规定的除外。

第七条 未成年人保护
若您为未满 14 周岁的未成年人，请在监护人的陪同、指导下使用本产品，并在监护人同意后提供个人信息。

第八条 第三方服务
本产品可能接入微信支付、支付宝、对象存储、短信等第三方服务。第三方服务对其自身收集的信息独立负责，其隐私政策可在对应服务商官网查阅。

第九条 安全事件处置
如发生个人信息泄露等安全事件，我们将按照法律法规要求及时告知您事件概况、可能影响、已采取或拟采取的处置措施，并向监管部门报告。

第十条 政策的更新
本政策可能适时更新。更新后我们将在页面显著位置公示。对于重大变更，我们可能通过站内信、邮件等方式进一步通知您。

第十一条 联系我们
如您对本政策或个人信息保护有任何疑问、意见或投诉，可通过本产品内的反馈渠道与我们联系。我们将在 15 个工作日内回复。
`

// ---------- 解析 ----------
const CLAUSE_HEADER_RE = /^第\s*([0-9一二三四五六七八九十百两]+)\s*条\s*[：:、.．]?\s*(.*)$/

/**
 * 解析一段协议/政策文本为结构化条款。
 *
 * 输入格式约定：
 *   - 顶部第一行非空行作为文档标题；
 *   - 标题后、第一条之前的非空行作为 preamble（前言）；
 *   - 以 "第N条 标题" 开头的行作为条款起始，N 支持阿拉伯数字或中文数字；
 *   - 两条之间的非空行拼接为当前条目的 body（以换行连接）。
 *
 * @param {string} content 原始协议文本
 * @returns {{
 *   title: string,
 *   preamble: string,
 *   clauses: Array<{ index: number, title: string, body: string }>,
 *   total: number
 * }}
 */
export function parseAgreementSection(content) {
  const empty = { title: '', preamble: '', clauses: [], total: 0 }
  if (typeof content !== 'string' || content.trim() === '') {
    return empty
  }

  const lines = content.split(/\r?\n/).map((l) => l.trim()).filter((l) => l !== '')

  let title = ''
  const preambleLines = []
  const clauses = []
  let current = null
  let seenHeader = false
  let titleAssigned = false

  for (const line of lines) {
    const m = line.match(CLAUSE_HEADER_RE)
    if (m) {
      seenHeader = true
      if (current) clauses.push(current)
      current = {
        index: normalizeChineseNumber(m[1]),
        title: (m[2] || '').trim(),
        body: ''
      }
      continue
    }

    if (!seenHeader) {
      if (!titleAssigned) {
        title = line
        titleAssigned = true
      } else {
        preambleLines.push(line)
      }
    } else if (current) {
      current.body = current.body ? current.body + '\n' + line : line
    }
  }
  if (current) clauses.push(current)

  return {
    title,
    preamble: preambleLines.join('\n'),
    clauses,
    total: clauses.length
  }
}

/**
 * 便捷函数：统计文本中解析出的条款数。
 */
export function countAgreementClauses(content) {
  return parseAgreementSection(content).total
}

/**
 * 便捷函数：判断两段协议是否已经被用户有效同意。
 * 仅做布尔校验，不记录持久化状态（持久化由调用方处理）。
 */
export function isAgreementAccepted(agreed) {
  return agreed === true || agreed === 'true'
}

export default {
  parseAgreementSection,
  countAgreementClauses,
  isAgreementAccepted,
  normalizeChineseNumber,
  USER_AGREEMENT_TEXT,
  PRIVACY_POLICY_TEXT
}
