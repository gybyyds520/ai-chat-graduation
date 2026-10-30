import {
  formatModelSize,
  ollamaStatusTag,
  normalizeAiConfig,
  maskApiKey,
  formatThousands,
  validateOllamaModelName
} from '../../src/utils/aiSystemManage'

describe('AI 系统管理纯逻辑工具', () => {
  describe('formatModelSize', () => {
    it('应将字节数格式化为 B/KB/MB/GB 字符串', () => {
      expect(formatModelSize(0)).toBe('0 B')
      expect(formatModelSize(1024)).toBe('1 KB')
      expect(formatModelSize(1536)).toBe('1.5 KB')
      expect(formatModelSize(1024 * 1024)).toBe('1 MB')
      expect(formatModelSize(1024 * 1024 * 1024)).toBe('1 GB')
      expect(formatModelSize(1.5 * 1024 * 1024 * 1024)).toBe('1.5 GB')
    })

    it('非法输入应回退为 0 B', () => {
      expect(formatModelSize(null)).toBe('0 B')
      expect(formatModelSize(undefined)).toBe('0 B')
      expect(formatModelSize('abc')).toBe('0 B')
    })
  })

  describe('ollamaStatusTag', () => {
    it('available=true 应映射为 success/在线', () => {
      expect(ollamaStatusTag(true)).toEqual({ type: 'success', text: '在线' })
      expect(ollamaStatusTag(1)).toEqual({ type: 'success', text: '在线' })
      expect(ollamaStatusTag('true')).toEqual({ type: 'success', text: '在线' })
    })

    it('available=false 应映射为 info/离线', () => {
      expect(ollamaStatusTag(false)).toEqual({ type: 'info', text: '离线' })
      expect(ollamaStatusTag(0)).toEqual({ type: 'info', text: '离线' })
      expect(ollamaStatusTag(undefined)).toEqual({ type: 'info', text: '离线' })
    })
  })

  describe('normalizeAiConfig', () => {
    it('应把后端字符串值归一化为数字/布尔类型', () => {
      const raw = {
        defaultModel: 'llama3.2:3b',
        temperature: '0.7',
        maxTokens: '2048',
        requestTimeoutSec: '60',
        aiChatEnabled: 'true',
        maintenanceNotice: '维护中'
      }
      const cfg = normalizeAiConfig(raw)
      expect(cfg.temperature).toBe(0.7)
      expect(cfg.maxTokens).toBe(2048)
      expect(cfg.requestTimeoutSec).toBe(60)
      expect(cfg.aiChatEnabled).toBe(true)
      expect(cfg.defaultModel).toBe('llama3.2:3b')
      expect(cfg.maintenanceNotice).toBe('维护中')
    })

    it('缺省字段应使用安全默认值', () => {
      const cfg = normalizeAiConfig(null)
      expect(cfg.temperature).toBe(0.7)
      expect(cfg.maxTokens).toBe(2048)
      expect(cfg.requestTimeoutSec).toBe(60)
      expect(cfg.aiChatEnabled).toBe(false)
      expect(cfg.defaultModel).toBe('')
      expect(cfg.maintenanceNotice).toBe('')
    })
  })

  describe('maskApiKey', () => {
    it('长密钥应保留前 8 位与后 4 位并以 **** 连接', () => {
      const masked = maskApiKey('sk-abcdefghijklmnopqrstuvwxyz1234')
      expect(masked.startsWith('sk-abcde')).toBe(true)
      expect(masked).toContain('****')
      expect(masked.endsWith('234')).toBe(true)
    })

    it('短密钥应整体脱敏，空值应显示 -', () => {
      expect(maskApiKey('short')).toBe('****')
      expect(maskApiKey('')).toBe('-')
      expect(maskApiKey(null)).toBe('-')
      expect(maskApiKey(undefined)).toBe('-')
    })
  })

  describe('formatThousands', () => {
    it('应为大数字添加千分位分隔符', () => {
      expect(formatThousands(0)).toBe('0')
      expect(formatThousands(999)).toBe('999')
      expect(formatThousands(1234)).toBe('1,234')
      expect(formatThousands(12345678)).toBe('12,345,678')
    })

    it('字符串数字与非法输入应安全处理', () => {
      expect(formatThousands('5000')).toBe('5,000')
      expect(formatThousands(null)).toBe('0')
      expect(formatThousands('abc')).toBe('0')
    })
  })

  describe('validateOllamaModelName', () => {
    it('合法模型名应通过', () => {
      expect(validateOllamaModelName('llama3.2:3b')).toBe('')
      expect(validateOllamaModelName('ollama/llama3:latest')).toBe('')
    })

    it('空名、含空格、非法字符应报错', () => {
      expect(validateOllamaModelName('')).toContain('不能为空')
      expect(validateOllamaModelName(null)).toContain('不能为空')
      expect(validateOllamaModelName('llama 3.2')).toContain('空格')
      expect(validateOllamaModelName('bad$name!')).toContain('非法字符')
    })
  })
})
