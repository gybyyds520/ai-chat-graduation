import { createLogger } from './logger'

const logger = createLogger('TimeUtils')

const DEFAULT_FORMAT = 'YYYY-MM-DD HH:mm:ss'
const DEFAULT_TIMEZONE = 'Asia/Shanghai'

const FORMAT_PATTERNS = {
  YYYY: (date) => date.getFullYear(),
  YY: (date) => date.getFullYear().toString().slice(-2),
  MM: (date) => String(date.getMonth() + 1).padStart(2, '0'),
  M: (date) => date.getMonth() + 1,
  DD: (date) => String(date.getDate()).padStart(2, '0'),
  D: (date) => date.getDate(),
  HH: (date) => String(date.getHours()).padStart(2, '0'),
  H: (date) => date.getHours(),
  hh: (date) => {
    const hours = date.getHours() % 12
    return String(hours === 0 ? 12 : hours).padStart(2, '0')
  },
  h: (date) => {
    const hours = date.getHours() % 12
    return hours === 0 ? 12 : hours
  },
  mm: (date) => String(date.getMinutes()).padStart(2, '0'),
  m: (date) => date.getMinutes(),
  ss: (date) => String(date.getSeconds()).padStart(2, '0'),
  s: (date) => date.getSeconds(),
  SSS: (date) => String(date.getMilliseconds()).padStart(3, '0'),
  SS: (date) => String(date.getMilliseconds()).padStart(2, '0'),
  A: (date) => date.getHours() >= 12 ? 'PM' : 'AM',
  a: (date) => date.getHours() >= 12 ? 'pm' : 'am',
  w: (date) => ['日', '一', '二', '三', '四', '五', '六'][date.getDay()],
  W: (date) => ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
}

export function formatDate(date, format = DEFAULT_FORMAT) {
  if (!date) {
    return ''
  }
  
  const d = typeof date === 'string' ? new Date(date) : date
  
  if (isNaN(d.getTime())) {
    logger.warn('FORMAT_DATE_INVALID', 'Invalid date passed to formatDate', { date })
    return ''
  }
  
  return format.replace(/YYYY|YY|MM|M|DD|D|HH|H|hh|h|mm|m|ss|s|SSS|SS|A|a|w|W/g, (match) => {
    return FORMAT_PATTERNS[match]?.(d) ?? match
  })
}

export function parseDate(dateString) {
  if (!dateString) {
    return null
  }
  
  const date = new Date(dateString)
  
  if (isNaN(date.getTime())) {
    logger.warn('PARSE_DATE_INVALID', 'Invalid date string', { dateString })
    return null
  }
  
  return date
}

export function getTimestamp() {
  return Date.now()
}

export function getFormattedTimestamp(format = DEFAULT_FORMAT) {
  return formatDate(new Date(), format)
}

export function getTodayString() {
  return formatDate(new Date(), 'YYYY-MM-DD')
}

export function getYesterdayString() {
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  return formatDate(yesterday, 'YYYY-MM-DD')
}

export function getWeekStart(date = new Date()) {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  d.setDate(diff)
  d.setHours(0, 0, 0, 0)
  return d
}

export function getWeekEnd(date = new Date()) {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() + (7 - day) - (day === 0 ? 7 : 0)
  d.setDate(diff)
  d.setHours(23, 59, 59, 999)
  return d
}

export function getMonthStart(date = new Date()) {
  const d = new Date(date)
  d.setDate(1)
  d.setHours(0, 0, 0, 0)
  return d
}

export function getMonthEnd(date = new Date()) {
  const d = new Date(date)
  d.setMonth(d.getMonth() + 1)
  d.setDate(0)
  d.setHours(23, 59, 59, 999)
  return d
}

export function isToday(date) {
  if (!date) return false
  const d = typeof date === 'string' ? new Date(date) : date
  const today = new Date()
  return d.toDateString() === today.toDateString()
}

export function isYesterday(date) {
  if (!date) return false
  const d = typeof date === 'string' ? new Date(date) : date
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  return d.toDateString() === yesterday.toDateString()
}

export function isThisWeek(date) {
  if (!date) return false
  const d = typeof date === 'string' ? new Date(date) : date
  const weekStart = getWeekStart()
  const weekEnd = getWeekEnd()
  return d >= weekStart && d <= weekEnd
}

export function isThisMonth(date) {
  if (!date) return false
  const d = typeof date === 'string' ? new Date(date) : date
  const today = new Date()
  return d.getMonth() === today.getMonth() && d.getFullYear() === today.getFullYear()
}

export function getTimeAgo(date) {
  if (!date) return ''
  const d = typeof date === 'string' ? new Date(date) : date
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  const week = 7 * day
  const month = 30 * day
  const year = 365 * day
  
  if (diff < minute) {
    return '刚刚'
  } else if (diff < hour) {
    return `${Math.floor(diff / minute)}分钟前`
  } else if (diff < day) {
    return `${Math.floor(diff / hour)}小时前`
  } else if (diff < week) {
    return `${Math.floor(diff / day)}天前`
  } else if (diff < month) {
    return `${Math.floor(diff / week)}周前`
  } else if (diff < year) {
    return `${Math.floor(diff / month)}个月前`
  } else {
    return `${Math.floor(diff / year)}年前`
  }
}

export function getRelativeTime(date) {
  if (!date) return ''
  
  if (isToday(date)) {
    return formatDate(date, 'HH:mm')
  } else if (isYesterday(date)) {
    return '昨天 ' + formatDate(date, 'HH:mm')
  } else if (isThisWeek(date)) {
    const d = typeof date === 'string' ? new Date(date) : date
    return `${['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]} ${formatDate(date, 'HH:mm')}`
  } else {
    return formatDate(date, 'MM-DD HH:mm')
  }
}

export function addDays(date, days) {
  const d = typeof date === 'string' ? new Date(date) : new Date(date)
  d.setDate(d.getDate() + days)
  return d
}

export function addHours(date, hours) {
  const d = typeof date === 'string' ? new Date(date) : new Date(date)
  d.setHours(d.getHours() + hours)
  return d
}

export function addMinutes(date, minutes) {
  const d = typeof date === 'string' ? new Date(date) : new Date(date)
  d.setMinutes(d.getMinutes() + minutes)
  return d
}

export function diffDays(date1, date2) {
  const d1 = typeof date1 === 'string' ? new Date(date1) : date1
  const d2 = typeof date2 === 'string' ? new Date(date2) : date2
  const diffTime = Math.abs(d2.getTime() - d1.getTime())
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
}

export function generateTimestamp() {
  return Date.now().toString()
}

export function formatDuration(seconds) {
  if (!seconds || seconds <= 0) {
    return '00:00'
  }
  
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = Math.floor(seconds % 60)
  
  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
  } else {
    return `${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
  }
}

export function formatDurationMs(ms) {
  return formatDuration(ms / 1000)
}

export const timeConfig = {
  defaultFormat: DEFAULT_FORMAT,
  timezone: DEFAULT_TIMEZONE,
  dateSeparator: '-',
  timeSeparator: ':',
  datetimeSeparator: ' '
}

export const TimeUtils = {
  formatDate,
  parseDate,
  getTimestamp,
  getFormattedTimestamp,
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
  config: timeConfig
}

export default TimeUtils