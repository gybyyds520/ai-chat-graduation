import {
  formatDate,
  parseDate,
  getTimestamp,
  getTodayString,
  getYesterdayString,
  getWeekStart,
  getWeekEnd,
  getMonthStart,
  getMonthEnd,
  isToday,
  isYesterday,
  isThisWeek,
  isThisMonth,
  getTimeAgo,
  getRelativeTime,
  addDays,
  addHours,
  addMinutes,
  diffDays,
  generateTimestamp,
  formatDuration,
  formatDurationMs,
  timeConfig
} from '../../src/utils/timeUtils'

describe('timeUtils 时间工具', () => {
  describe('formatDate', () => {
    it('应按默认格式格式化日期', () => {
      const d = new Date(2026, 0, 5, 9, 8, 7)
      expect(formatDate(d)).toBe('2026-01-05 09:08:07')
    })

    it('应支持自定义格式 YYYY/MM/DD', () => {
      const d = new Date(2026, 5, 14)
      expect(formatDate(d, 'YYYY/MM/DD')).toBe('2026/06/14')
    })

    it('应支持 12 小时制与 AM/PM', () => {
      const d = new Date(2026, 0, 1, 14, 30)
      expect(formatDate(d, 'hh:mm A')).toBe('02:30 PM')
    })

    it('应支持中文星期', () => {
      const d = new Date(2026, 0, 1) // 2026-01-01 周四
      expect(formatDate(d, 'W')).toBe('周四')
    })

    it('应接受字符串日期', () => {
      expect(formatDate('2026-01-02', 'YYYY-MM-DD')).toBe('2026-01-02')
    })

    it('空值应返回空串', () => {
      expect(formatDate(null)).toBe('')
      expect(formatDate(undefined)).toBe('')
    })

    it('非法日期应返回空串', () => {
      expect(formatDate('not-a-date')).toBe('')
    })
  })

  describe('parseDate', () => {
    it('应解析合法字符串', () => {
      const d = parseDate('2026-05-06T10:00:00')
      expect(d instanceof Date).toBe(true)
      expect(d.getFullYear()).toBe(2026)
    })

    it('空值应返回 null', () => {
      expect(parseDate('')).toBeNull()
      expect(parseDate(null)).toBeNull()
    })

    it('非法字符串应返回 null', () => {
      expect(parseDate('xyz')).toBeNull()
    })
  })

  describe('today / yesterday 字符串', () => {
    it('getTodayString 应为今天的 YYYY-MM-DD', () => {
      expect(getTodayString()).toBe(formatDate(new Date(), 'YYYY-MM-DD'))
    })

    it('getYesterdayString 应比今天早一天', () => {
      const today = new Date()
      const y = new Date(getYesterdayString())
      expect(y.getDate()).toBe(today.getDate() - (today.getDate() === 1 ? 0 : 1))
    })
  })

  describe('周/月范围', () => {
    it('getWeekStart 应为周一 00:00:00', () => {
      const start = getWeekStart(new Date(2026, 0, 7)) // 周三
      expect(start.getDay()).toBe(1)
      expect(start.getHours()).toBe(0)
      expect(start.getMinutes()).toBe(0)
    })

    it('getMonthStart 应为当月 1 号', () => {
      const start = getMonthStart(new Date(2026, 5, 15))
      expect(start.getDate()).toBe(1)
    })

    it('getMonthEnd 应为当月最后一天 23:59:59', () => {
      const end = getMonthEnd(new Date(2026, 1, 10)) // 2 月
      expect(end.getMonth()).toBe(1)
      expect(end.getDate()).toBe(28)
    })
  })

  describe('isToday / isYesterday / isThisWeek / isThisMonth', () => {
    it('今天应为 true', () => {
      expect(isToday(new Date())).toBe(true)
    })

    it('昨天应为 true', () => {
      const y = new Date()
      y.setDate(y.getDate() - 1)
      expect(isYesterday(y)).toBe(true)
    })

    it('空值应返回 false', () => {
      expect(isToday(null)).toBe(false)
      expect(isYesterday(null)).toBe(false)
      expect(isThisWeek(null)).toBe(false)
      expect(isThisMonth(null)).toBe(false)
    })

    it('本月应为 true', () => {
      expect(isThisMonth(new Date())).toBe(true)
    })
  })

  describe('getTimeAgo 相对时间', () => {
    it('一分钟内应显示刚刚', () => {
      expect(getTimeAgo(new Date())).toBe('刚刚')
    })

    it('数分钟前', () => {
      const t = new Date(Date.now() - 5 * 60 * 1000)
      expect(getTimeAgo(t)).toBe('5分钟前')
    })

    it('数小时前', () => {
      const t = new Date(Date.now() - 3 * 3600 * 1000)
      expect(getTimeAgo(t)).toBe('3小时前')
    })

    it('数天前', () => {
      const t = new Date(Date.now() - 2 * 86400 * 1000)
      expect(getTimeAgo(t)).toBe('2天前')
    })

    it('空值返回空串', () => {
      expect(getTimeAgo(null)).toBe('')
    })
  })

  describe('getRelativeTime', () => {
    it('今天应只显示 HH:mm', () => {
      const now = new Date()
      const result = getRelativeTime(now)
      expect(result).toMatch(/^\d{2}:\d{2}$/)
    })

    it('空值返回空串', () => {
      expect(getRelativeTime(null)).toBe('')
    })
  })

  describe('addDays / addHours / addMinutes', () => {
    it('addDays 应正确加天', () => {
      const base = new Date(2026, 0, 1)
      expect(addDays(base, 5).getDate()).toBe(6)
    })

    it('addDays 支持负数', () => {
      const base = new Date(2026, 0, 10)
      expect(addDays(base, -3).getDate()).toBe(7)
    })

    it('addHours / addMinutes 应生效', () => {
      expect(addHours(new Date(2026, 0, 1, 10), 3).getHours()).toBe(13)
      expect(addMinutes(new Date(2026, 0, 1, 10, 30), 15).getMinutes()).toBe(45)
    })
  })

  describe('diffDays', () => {
    it('相差两天应返回 2', () => {
      expect(diffDays('2026-01-01', '2026-01-03')).toBe(2)
    })

    it('顺序不影响结果', () => {
      expect(diffDays('2026-01-03', '2026-01-01')).toBe(2)
    })
  })

  describe('formatDuration', () => {
    it('120 秒应格式化为 02:00', () => {
      expect(formatDuration(120)).toBe('02:00')
    })

    it('0 秒应返回 00:00', () => {
      expect(formatDuration(0)).toBe('00:00')
      expect(formatDuration(-5)).toBe('00:00')
    })

    it('超过 1 小时应带小时', () => {
      expect(formatDuration(3661)).toBe('01:01:01')
    })

    it('formatDurationMs 应换算毫秒', () => {
      expect(formatDurationMs(60000)).toBe('01:00')
    })
  })

  describe('其他', () => {
    it('getTimestamp 应返回数字', () => {
      expect(typeof getTimestamp()).toBe('number')
    })

    it('generateTimestamp 应返回字符串数字', () => {
      expect(typeof generateTimestamp()).toBe('string')
      expect(Number(generateTimestamp())).not.toBeNaN()
    })

    it('timeConfig 应有默认时区', () => {
      expect(timeConfig.timezone).toBe('Asia/Shanghai')
    })
  })
})
