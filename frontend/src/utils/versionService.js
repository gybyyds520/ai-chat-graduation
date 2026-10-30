import { createLogger } from './logger'

const logger = createLogger('VersionService')

const STORAGE_KEY = 'app-version-info'

const DEFAULT_VERSION = {
  currentVersion: '1.0.0',
  versionType: 'free',
  upgradeTargetVersion: '2.0.0',
  upgradeDeadline: null,
  lastCheckTime: null
}

class VersionService {
  constructor() {
    this.versionInfo = this.loadVersionInfo()
    this.countdownInterval = null
    this.listeners = new Set()
  }

  loadVersionInfo() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved) {
        return { ...DEFAULT_VERSION, ...JSON.parse(saved) }
      }
    } catch (e) {
      logger.error('VERSION_LOAD_FAILED', 'Failed to load version info', { error: e.message })
    }
    return { ...DEFAULT_VERSION }
  }

  saveVersionInfo() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.versionInfo))
    } catch (e) {
      logger.error('VERSION_SAVE_FAILED', 'Failed to save version info', { error: e.message })
    }
  }

  getVersionInfo() {
    return { ...this.versionInfo }
  }

  getVersionType() {
    return this.versionInfo.versionType
  }

  getVersionTypeLabel() {
    const labels = {
      free: '免费版',
      pro: '专业版',
      premium: '高级版',
      enterprise: '企业版'
    }
    return labels[this.versionInfo.versionType] || this.versionInfo.versionType
  }

  getVersionTypeColor() {
    const colors = {
      free: '#6b7280',
      pro: '#3b82f6',
      premium: '#f59e0b',
      enterprise: '#8b5cf6'
    }
    return colors[this.versionInfo.versionType] || '#6b7280'
  }

  setVersionType(type) {
    this.versionInfo.versionType = type
    this.saveVersionInfo()
    this.notifyListeners('versionTypeChanged', type)
    logger.info('VERSION_TYPE_CHANGED', `Version type changed to: ${type}`)
  }

  setUpgradeDeadline(timestamp) {
    this.versionInfo.upgradeDeadline = timestamp
    this.saveVersionInfo()
    this.notifyListeners('upgradeDeadlineChanged', timestamp)
    logger.info('UPGRADE_DEADLINE_SET', `Upgrade deadline set to: ${new Date(timestamp)}`)
  }

  clearUpgradeDeadline() {
    this.versionInfo.upgradeDeadline = null
    this.saveVersionInfo()
    this.notifyListeners('upgradeDeadlineCleared')
    logger.info('UPGRADE_DEADLINE_CLEARED', 'Upgrade deadline cleared')
  }

  hasUpgradeDeadline() {
    return this.versionInfo.upgradeDeadline && this.versionInfo.upgradeDeadline > Date.now()
  }

  getCountdown() {
    if (!this.versionInfo.upgradeDeadline) {
      return null
    }

    const now = Date.now()
    const diff = this.versionInfo.upgradeDeadline - now

    if (diff <= 0) {
      return { expired: true, days: 0, hours: 0, minutes: 0, seconds: 0 }
    }

    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((diff % (1000 * 60)) / 1000)

    return {
      expired: false,
      days,
      hours,
      minutes,
      seconds,
      total: diff
    }
  }

  startCountdown() {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval)
    }

    this.countdownInterval = setInterval(() => {
      const countdown = this.getCountdown()
      if (countdown) {
        this.notifyListeners('countdownUpdate', countdown)
        if (countdown.expired) {
          this.stopCountdown()
        }
      }
    }, 1000)

    logger.info('COUNTDOWN_STARTED', 'Upgrade countdown started')
  }

  stopCountdown() {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval)
      this.countdownInterval = null
    }
    logger.info('COUNTDOWN_STOPPED', 'Upgrade countdown stopped')
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

  destroy() {
    this.stopCountdown()
    this.listeners.clear()
  }
}

const versionService = new VersionService()

export { versionService }
export default versionService