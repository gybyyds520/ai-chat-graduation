import { createLogger } from './logger'

const logger = createLogger('Analytics')

const STORAGE_KEY = 'desktop-elf-analytics'
const BATCH_SIZE = 50
const FLUSH_INTERVAL = 60000
const MAX_STORED_EVENTS = 1000

const EVENT_CATEGORIES = {
  WIDGET: 'widget',
  ALARM: 'alarm',
  MUSIC: 'music',
  INTERACTION: 'interaction',
  PERFORMANCE: 'performance',
  ERROR: 'error'
}

const EVENT_TYPES = {
  WIDGET_ADD: 'widget_add',
  WIDGET_REMOVE: 'widget_remove',
  WIDGET_EDIT: 'widget_edit',
  WIDGET_VIEW: 'widget_view',
  ALARM_CREATE: 'alarm_create',
  ALARM_EDIT: 'alarm_edit',
  ALARM_DELETE: 'alarm_delete',
  ALARM_TRIGGER: 'alarm_trigger',
  ALARM_SNOOZE: 'alarm_snooze',
  ALARM_DISMISS: 'alarm_dismiss',
  MUSIC_PLAY: 'music_play',
  MUSIC_PAUSE: 'music_pause',
  MUSIC_NEXT: 'music_next',
  MUSIC_PREV: 'music_prev',
  MUSIC_IMPORT: 'music_import',
  MUSIC_SEEK: 'music_seek',
  MUSIC_VOLUME_CHANGE: 'music_volume_change',
  MUSIC_MODE_CHANGE: 'music_mode_change',
  ELF_CLICK: 'elf_click',
  ELF_DRAG: 'elf_drag',
  ELF_INTERACTION: 'elf_interaction',
  APP_START: 'app_start',
  APP_SHUTDOWN: 'app_shutdown',
  PERFORMANCE_METRIC: 'performance_metric',
  ERROR_OCCURRED: 'error_occurred'
}

class Analytics {
  constructor() {
    this.events = []
    this.userId = null
    this.isEnabled = true
    this.flushTimer = null
    this.init()
  }

  init() {
    this.loadStoredEvents()
    this.initUserId()
    this.startFlushTimer()
    this.trackAppStart()
  }

  initUserId() {
    let id = localStorage.getItem('analytics-user-id')
    if (!id) {
      id = this.generateUUID()
      localStorage.setItem('analytics-user-id', id)
    }
    this.userId = id
    logger.debug('USER_ID_INIT', `User ID initialized: ${id}`)
  }

  generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8)
      return v.toString(16)
    })
  }

  loadStoredEvents() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved) {
        this.events = JSON.parse(saved)
        logger.info('EVENTS_LOADED', `Loaded ${this.events.length} stored events`)
      }
    } catch (e) {
      logger.error('EVENTS_LOAD_FAILED', 'Failed to load stored events', { error: e.message })
      this.events = []
    }
  }

  saveEvents() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.events))
    } catch (e) {
      logger.error('EVENTS_SAVE_FAILED', 'Failed to save events', { error: e.message })
    }
  }

  startFlushTimer() {
    if (this.flushTimer) {
      clearInterval(this.flushTimer)
    }
    this.flushTimer = setInterval(() => {
      this.flush()
    }, FLUSH_INTERVAL)
    logger.debug('FLUSH_TIMER_STARTED', `Flush interval: ${FLUSH_INTERVAL}ms`)
  }

  stopFlushTimer() {
    if (this.flushTimer) {
      clearInterval(this.flushTimer)
      this.flushTimer = null
    }
  }

  track(eventType, data = {}) {
    if (!this.isEnabled) return

    const event = {
      id: this.generateUUID(),
      userId: this.userId,
      timestamp: Date.now(),
      category: this.getCategory(eventType),
      type: eventType,
      data: this.sanitizeData(data),
      platform: this.getPlatform(),
      appVersion: this.getAppVersion()
    }

    this.events.push(event)
    
    if (this.events.length >= MAX_STORED_EVENTS) {
      this.events = this.events.slice(-MAX_STORED_EVENTS)
    }

    if (this.events.length >= BATCH_SIZE) {
      this.flush()
    }

    this.saveEvents()
    logger.debug('EVENT_TRACKED', `Event: ${eventType}`, { count: this.events.length })
  }

  getCategory(eventType) {
    if (eventType.startsWith('widget_')) return EVENT_CATEGORIES.WIDGET
    if (eventType.startsWith('alarm_')) return EVENT_CATEGORIES.ALARM
    if (eventType.startsWith('music_')) return EVENT_CATEGORIES.MUSIC
    if (eventType.startsWith('elf_')) return EVENT_CATEGORIES.INTERACTION
    if (eventType.startsWith('performance_')) return EVENT_CATEGORIES.PERFORMANCE
    if (eventType.startsWith('error_')) return EVENT_CATEGORIES.ERROR
    return EVENT_CATEGORIES.INTERACTION
  }

  sanitizeData(data) {
    const sanitized = {}
    Object.keys(data).forEach(key => {
      const value = data[key]
      if (typeof value === 'string') {
        sanitized[key] = value.length > 200 ? value.substring(0, 200) + '...' : value
      } else if (typeof value === 'number') {
        sanitized[key] = value
      } else if (typeof value === 'boolean') {
        sanitized[key] = value
      } else if (Array.isArray(value)) {
        sanitized[key] = value.slice(0, 10).map(item => 
          typeof item === 'string' ? item.substring(0, 50) : item
        )
      } else if (typeof value === 'object' && value !== null) {
        sanitized[key] = '[object]'
      }
    })
    return sanitized
  }

  getPlatform() {
    if (typeof window !== 'undefined') {
      if (window.navigator.userAgent.includes('Windows')) return 'windows'
      if (window.navigator.userAgent.includes('Mac')) return 'macos'
      if (window.navigator.userAgent.includes('Linux')) return 'linux'
    }
    return 'unknown'
  }

  getAppVersion() {
    try {
      if (window.electronAPI) {
        return window.electronAPI.getAppVersion()
      }
    } catch (e) {
      logger.warn('APP_VERSION_GET_FAILED', 'Failed to get app version', { error: e.message })
    }
    return '1.0.0'
  }

  async flush() {
    if (this.events.length === 0) return

    const batch = [...this.events]
    this.events = []
    this.saveEvents()

    try {
      await this.sendBatch(batch)
      logger.info('EVENTS_FLUSHED', `Successfully flushed ${batch.length} events`)
    } catch (e) {
      logger.error('EVENTS_FLUSH_FAILED', 'Failed to flush events', { error: e.message })
      this.events.push(...batch)
      this.saveEvents()
    }
  }

  async sendBatch(batch) {
    const payload = {
      batch,
      timestamp: Date.now(),
      userId: this.userId
    }

    if (navigator.sendBeacon) {
      navigator.sendBeacon('/api/analytics', JSON.stringify(payload))
    } else {
      await fetch('/api/analytics', {
        method: 'POST',
        body: JSON.stringify(payload),
        headers: { 'Content-Type': 'application/json' },
        keepalive: true
      })
    }
  }

  trackAppStart() {
    this.track(EVENT_TYPES.APP_START, {
      timestamp: Date.now(),
      screenWidth: window.innerWidth,
      screenHeight: window.innerHeight,
      language: navigator.language
    })
  }

  trackAppShutdown() {
    this.track(EVENT_TYPES.APP_SHUTDOWN)
  }

  trackWidgetAdd(type) {
    this.track(EVENT_TYPES.WIDGET_ADD, { type })
  }

  trackWidgetRemove(type) {
    this.track(EVENT_TYPES.WIDGET_REMOVE, { type })
  }

  trackWidgetEdit(type, changes) {
    this.track(EVENT_TYPES.WIDGET_EDIT, { type, changes: Object.keys(changes) })
  }

  trackAlarmCreate(alarm) {
    this.track(EVENT_TYPES.ALARM_CREATE, {
      time: alarm.time,
      repeat: alarm.repeat,
      ringtone: alarm.ringtone,
      volume: alarm.volume
    })
  }

  trackAlarmEdit(alarm) {
    this.track(EVENT_TYPES.ALARM_EDIT, {
      time: alarm.time,
      repeat: alarm.repeat,
      ringtone: alarm.ringtone
    })
  }

  trackAlarmDelete() {
    this.track(EVENT_TYPES.ALARM_DELETE)
  }

  trackAlarmTrigger(alarm) {
    this.track(EVENT_TYPES.ALARM_TRIGGER, {
      time: alarm.time,
      label: alarm.label
    })
  }

  trackAlarmSnooze(minutes) {
    this.track(EVENT_TYPES.ALARM_SNOOZE, { minutes })
  }

  trackAlarmDismiss() {
    this.track(EVENT_TYPES.ALARM_DISMISS)
  }

  trackMusicPlay(track) {
    this.track(EVENT_TYPES.MUSIC_PLAY, {
      trackName: track?.name,
      trackIndex: track?.index,
      totalTracks: track?.total
    })
  }

  trackMusicPause(track) {
    this.track(EVENT_TYPES.MUSIC_PAUSE, {
      trackName: track?.name,
      trackIndex: track?.index
    })
  }

  trackMusicNext() {
    this.track(EVENT_TYPES.MUSIC_NEXT)
  }

  trackMusicPrev() {
    this.track(EVENT_TYPES.MUSIC_PREV)
  }

  trackMusicImport(count) {
    this.track(EVENT_TYPES.MUSIC_IMPORT, { count })
  }

  trackMusicSeek(progress) {
    this.track(EVENT_TYPES.MUSIC_SEEK, { progress })
  }

  trackMusicVolumeChange(volume) {
    this.track(EVENT_TYPES.MUSIC_VOLUME_CHANGE, { volume })
  }

  trackMusicModeChange(mode) {
    this.track(EVENT_TYPES.MUSIC_MODE_CHANGE, { mode })
  }

  trackElfClick() {
    this.track(EVENT_TYPES.ELF_CLICK)
  }

  trackElfDrag() {
    this.track(EVENT_TYPES.ELF_DRAG)
  }

  trackElfInteraction(action) {
    this.track(EVENT_TYPES.ELF_INTERACTION, { action })
  }

  trackPerformanceMetric(metric, value, context = {}) {
    this.track(EVENT_TYPES.PERFORMANCE_METRIC, {
      metric,
      value,
      ...context
    })
  }

  trackError(error, context = {}) {
    this.track(EVENT_TYPES.ERROR_OCCURRED, {
      message: error.message,
      stack: error.stack?.substring(0, 500),
      ...context
    })
  }

  setEnabled(enabled) {
    this.isEnabled = enabled
    localStorage.setItem('analytics-enabled', String(enabled))
    logger.info('ANALYTICS_TOGGLED', `Analytics ${enabled ? 'enabled' : 'disabled'}`)
  }

  isEnabled() {
    return this.isEnabled
  }

  getStats() {
    const stats = {
      totalEvents: this.events.length,
      byCategory: {},
      recentEvents: this.events.slice(-10)
    }

    Object.values(EVENT_CATEGORIES).forEach(cat => {
      stats.byCategory[cat] = this.events.filter(e => e.category === cat).length
    })

    return stats
  }

  destroy() {
    this.stopFlushTimer()
    this.flush()
    logger.info('ANALYTICS_DESTROYED', 'Analytics service destroyed')
  }
}

const analytics = new Analytics()

export { analytics, EVENT_TYPES, EVENT_CATEGORIES }
export default analytics