import { createLogger } from './logger'

const logger = createLogger('AlarmManager')

const STORAGE_KEY = 'desktop-elf-alarms'
const DEFAULT_RINGTONES = [
  { id: 'classic', name: '经典闹钟', url: null, type: 'system' },
  { id: 'digital', name: '数字音效', url: null, type: 'system' },
  { id: 'nature', name: '自然之声', url: null, type: 'system' },
  { id: 'birds', name: '鸟鸣', url: null, type: 'system' },
  { id: 'guitar', name: '吉他', url: null, type: 'system' }
]

class AlarmManager {
  constructor() {
    this.alarms = []
    this.timers = new Map()
    this.listeners = new Set()
    this.audioContext = null
    this.currentOscillator = null
    this.currentGainNode = null
    this.ringtoneInterval = null
    this.init()
  }

  init() {
    this.loadAlarms()
    this.scheduleAllAlarms()
    this.setupSystemWakeup()
  }

  loadAlarms() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved) {
        this.alarms = JSON.parse(saved)
        logger.info('ALARMS_LOADED', `Loaded ${this.alarms.length} alarms`)
      }
    } catch (e) {
      logger.error('ALARMS_LOAD_FAILED', 'Failed to load alarms', { error: e.message })
      this.alarms = []
    }
  }

  saveAlarms() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.alarms))
      logger.info('ALARMS_SAVED', `Saved ${this.alarms.length} alarms`)
    } catch (e) {
      logger.error('ALARMS_SAVE_FAILED', 'Failed to save alarms', { error: e.message })
    }
  }

  getAlarms() {
    return [...this.alarms]
  }

  getAlarmById(id) {
    return this.alarms.find(a => a.id === id)
  }

  createAlarm(options) {
    const alarm = {
      id: Date.now().toString(),
      enabled: true,
      time: options.time || '08:00',
      label: options.label || '',
      repeat: options.repeat || 'once',
      repeatDays: options.repeatDays || [],
      ringtone: options.ringtone || 'classic',
      volume: options.volume !== undefined ? options.volume : 80,
      snoozeEnabled: options.snoozeEnabled !== undefined ? options.snoozeEnabled : true,
      snoozeInterval: options.snoozeInterval || 5,
      createdAt: Date.now(),
      lastTriggered: null
    }

    this.alarms.push(alarm)
    this.saveAlarms()
    this.scheduleAlarm(alarm)
    this.notifyListeners('alarmCreated', alarm)
    logger.info('ALARM_CREATED', `Alarm created: ${alarm.id} at ${alarm.time}`)

    return alarm
  }

  updateAlarm(id, options) {
    const index = this.alarms.findIndex(a => a.id === id)
    if (index === -1) {
      logger.warn('ALARM_UPDATE_FAILED', `Alarm not found: ${id}`)
      return null
    }

    const oldAlarm = { ...this.alarms[index] }
    this.alarms[index] = { ...this.alarms[index], ...options }
    this.saveAlarms()
    
    this.cancelAlarm(id)
    if (this.alarms[index].enabled) {
      this.scheduleAlarm(this.alarms[index])
    }
    
    this.notifyListeners('alarmUpdated', { old: oldAlarm, new: this.alarms[index] })
    logger.info('ALARM_UPDATED', `Alarm updated: ${id}`)

    return this.alarms[index]
  }

  deleteAlarm(id) {
    const index = this.alarms.findIndex(a => a.id === id)
    if (index === -1) {
      logger.warn('ALARM_DELETE_FAILED', `Alarm not found: ${id}`)
      return false
    }

    const alarm = this.alarms[index]
    this.cancelAlarm(id)
    this.alarms.splice(index, 1)
    this.saveAlarms()
    this.notifyListeners('alarmDeleted', alarm)
    logger.info('ALARM_DELETED', `Alarm deleted: ${id}`)

    return true
  }

  toggleAlarm(id) {
    const alarm = this.alarms.find(a => a.id === id)
    if (!alarm) {
      logger.warn('ALARM_TOGGLE_FAILED', `Alarm not found: ${id}`)
      return null
    }

    alarm.enabled = !alarm.enabled
    this.saveAlarms()

    if (alarm.enabled) {
      this.scheduleAlarm(alarm)
      logger.info('ALARM_ENABLED', `Alarm enabled: ${id}`)
    } else {
      this.cancelAlarm(id)
      logger.info('ALARM_DISABLED', `Alarm disabled: ${id}`)
    }

    this.notifyListeners('alarmToggled', alarm)
    return alarm
  }

  scheduleAlarm(alarm) {
    this.cancelAlarm(alarm.id)

    if (!alarm.enabled) return

    const nextTime = this.getNextAlarmTime(alarm)
    if (!nextTime) {
      logger.warn('SCHEDULE_FAILED', `No valid next time for alarm: ${alarm.id}`)
      return
    }

    const delay = nextTime.getTime() - Date.now()
    if (delay <= 0) {
      logger.warn('SCHEDULE_FAILED', `Alarm time already passed: ${alarm.id}`)
      return
    }

    const timer = setTimeout(() => {
      this.triggerAlarm(alarm)
    }, delay)

    this.timers.set(alarm.id, timer)
    logger.info('ALARM_SCHEDULED', `Alarm scheduled: ${alarm.id} at ${nextTime.toLocaleString()}`)
  }

  scheduleAllAlarms() {
    this.cancelAllAlarms()
    this.alarms.forEach(alarm => {
      if (alarm.enabled) {
        this.scheduleAlarm(alarm)
      }
    })
  }

  cancelAlarm(id) {
    const timer = this.timers.get(id)
    if (timer) {
      clearTimeout(timer)
      this.timers.delete(id)
      logger.debug('ALARM_CANCELLED', `Alarm cancelled: ${id}`)
    }
  }

  cancelAllAlarms() {
    this.timers.forEach(timer => clearTimeout(timer))
    this.timers.clear()
    logger.info('ALL_ALARMS_CANCELLED', 'All alarms cancelled')
  }

  getNextAlarmTime(alarm) {
    const [hours, minutes] = alarm.time.split(':').map(Number)
    const now = new Date()
    let nextTime = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hours, minutes, 0, 0)

    if (alarm.repeat === 'once') {
      if (nextTime <= now) {
        return null
      }
      return nextTime
    }

    if (nextTime <= now) {
      nextTime.setDate(nextTime.getDate() + 1)
    }

    if (alarm.repeat === 'weekdays') {
      const day = nextTime.getDay()
      if (day === 0 || day === 6) {
        const daysToAdd = day === 0 ? 1 : 2
        nextTime.setDate(nextTime.getDate() + daysToAdd)
      }
    } else if (alarm.repeat === 'weekend') {
      const day = nextTime.getDay()
      if (day >= 1 && day <= 5) {
        const daysToAdd = 6 - day
        nextTime.setDate(nextTime.getDate() + daysToAdd)
      }
    } else if (alarm.repeat === 'custom' && alarm.repeatDays.length > 0) {
      const currentDay = nextTime.getDay()
      const dayNumbers = alarm.repeatDays.map(d => {
        const map = { sun: 0, mon: 1, tue: 2, wed: 3, thu: 4, fri: 5, sat: 6 }
        return map[d]
      }).filter(n => n !== undefined)

      if (dayNumbers.length === 0) return null

      let found = false
      for (let i = 0; i < 7; i++) {
        const checkDay = (currentDay + i) % 7
        if (dayNumbers.includes(checkDay)) {
          nextTime.setDate(nextTime.getDate() + i)
          found = true
          break
        }
      }

      if (!found) return null
    }

    return nextTime
  }

  triggerAlarm(alarm) {
    logger.info('ALARM_TRIGGERED', `Alarm triggered: ${alarm.id}`)
    
    alarm.lastTriggered = Date.now()
    this.saveAlarms()

    this.playRingtone(alarm)
    this.showNotification(alarm)
    this.notifyListeners('alarmTriggered', alarm)

    if (alarm.repeat !== 'once') {
      this.scheduleAlarm(alarm)
    } else {
      alarm.enabled = false
      this.saveAlarms()
    }
  }

  playRingtone(alarm) {
    try {
      this.stopRingtone()

      if (!this.audioContext) {
        this.audioContext = new (window.AudioContext || window.webkitAudioContext)()
      }

      const frequencies = {
        classic: [523.25, 659.25, 783.99, 1046.50],
        digital: [880, 1100, 1320, 1760],
        nature: [440, 554.37, 659.25, 880],
        birds: [1000, 1200, 1500, 2000],
        guitar: [329.63, 392, 440, 523.25]
      }

      const freqList = frequencies[alarm.ringtone] || frequencies.classic
      const volume = alarm.volume / 100

      const playSequence = () => {
        if (!this.audioContext) return

        const oscillator = this.audioContext.createOscillator()
        const gainNode = this.audioContext.createGain()

        oscillator.connect(gainNode)
        gainNode.connect(this.audioContext.destination)

        this.currentOscillator = oscillator
        this.currentGainNode = gainNode

        gainNode.gain.value = 0
        oscillator.type = 'sine'

        let time = this.audioContext.currentTime
        freqList.forEach((freq, index) => {
          oscillator.frequency.setValueAtTime(freq, time)
          gainNode.gain.setValueAtTime(volume, time)
          time += 0.3
          gainNode.gain.setValueAtTime(0, time)
          time += 0.1
        })

        oscillator.start(this.audioContext.currentTime)
        oscillator.stop(time + 0.1)
      }

      playSequence()

      this.ringtoneInterval = setInterval(() => {
        playSequence()
      }, 1600)

      logger.debug('RINGTONE_PLAYED', `Ringtone playing: ${alarm.ringtone}, volume: ${volume}`)
    } catch (e) {
      logger.error('RINGTONE_PLAY_FAILED', 'Failed to play ringtone', { error: e.message })
    }
  }

  stopRingtone() {
    if (this.ringtoneInterval) {
      clearInterval(this.ringtoneInterval)
      this.ringtoneInterval = null
    }

    if (this.currentOscillator) {
      try {
        this.currentOscillator.stop()
      } catch (e) {}
      this.currentOscillator = null
    }

    if (this.audioContext) {
      try {
        this.audioContext.close()
      } catch (e) {}
      this.audioContext = null
    }

    logger.debug('RINGTONE_STOPPED', 'Ringtone stopped')
  }

  snoozeAlarm(alarm) {
    if (!alarm.snoozeEnabled) return null

    const snoozeTime = new Date(Date.now() + alarm.snoozeInterval * 60 * 1000)
    const timer = setTimeout(() => {
      this.triggerAlarm(alarm)
    }, alarm.snoozeInterval * 60 * 1000)

    this.timers.set(`snooze-${alarm.id}`, timer)
    this.notifyListeners('alarmSnoozed', { alarm, snoozeUntil: snoozeTime })
    logger.info('ALARM_SNOOZED', `Alarm snoozed: ${alarm.id} for ${alarm.snoozeInterval} minutes`)

    return snoozeTime
  }

  showNotification(alarm) {
    if (!('Notification' in window)) {
      logger.warn('NOTIFICATION_NOT_SUPPORTED', 'Browser does not support notifications')
      return
    }

    if (Notification.permission !== 'granted') {
      Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          this.showNotification(alarm)
        }
      })
      return
    }

    new Notification('闹钟提醒', {
      body: alarm.label || `闹钟时间到！${alarm.time}`,
      icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="%234f46e5" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
      sound: null,
      vibrate: [200, 100, 200]
    })
  }

  setupSystemWakeup() {
    if ('wakeLock' in navigator) {
      document.addEventListener('visibilitychange', async () => {
        if (document.visibilityState === 'visible') {
          try {
            await navigator.wakeLock.request('screen')
            logger.debug('WAKE_LOCK_ACQUIRED', 'Screen wake lock acquired')
          } catch (e) {
            logger.warn('WAKE_LOCK_FAILED', 'Failed to acquire wake lock', { error: e.message })
          }
        }
      })
    }

    setInterval(() => {
      this.scheduleAllAlarms()
    }, 60000)
  }

  subscribe(listener) {
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  notifyListeners(event, data) {
    this.listeners.forEach(listener => {
      try {
        listener(event, data)
      } catch (e) {
        logger.error('LISTENER_ERROR', 'Error in listener', { error: e.message })
      }
    })
  }

  getAvailableRingtones() {
    return [...DEFAULT_RINGTONES]
  }

  destroy() {
    this.cancelAllAlarms()
    this.stopRingtone()
    this.listeners.clear()
    logger.info('ALARM_MANAGER_DESTROYED', 'Alarm manager destroyed')
  }
}

const alarmManager = new AlarmManager()

export { alarmManager, DEFAULT_RINGTONES }
export default alarmManager