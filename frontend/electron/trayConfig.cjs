const path = require('path')
const fs = require('fs')

class TrayConfig {
  constructor(app) {
    this.configPath = path.join(app.getPath('appData'), 'ai-chat-frontend', 'tray-config.json')
    this.defaultConfig = {
      icon: {
        type: 'default',
        path: '',
        emoji: '🤖',
        size: 16,
        color: '#4f46e5',
        showBadge: true,
        badgeColor: '#ff4d4f'
      },
      position: {
        area: 'tray',
        x: null,
        y: null
      },
      behavior: {
        singleClick: 'show',
        doubleClick: 'show',
        middleClick: 'newChat',
        showNotification: true,
        flashOnMessage: true
      },
      menu: {
        showMainWindow: true,
        showNewChat: true,
        showDesktopElf: true,
        showSettings: true,
        showExit: true,
        customItems: []
      }
    }
    
    this.config = this.loadConfig()
  }

  loadConfig() {
    try {
      const dir = path.dirname(this.configPath)
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true })
      }
      const data = fs.readFileSync(this.configPath, 'utf-8')
      return { ...this.defaultConfig, ...JSON.parse(data) }
    } catch (e) {
      return { ...this.defaultConfig }
    }
  }

  saveConfig() {
    try {
      const dir = path.dirname(this.configPath)
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true })
      }
      fs.writeFileSync(this.configPath, JSON.stringify(this.config, null, 2), { encoding: 'utf-8' })
    } catch (e) {
      console.warn('保存托盘配置失败:', e.message)
    }
  }

  getConfig() {
    return this.config
  }

  updateConfig(newConfig) {
    this.config = { ...this.config, ...newConfig }
    this.saveConfig()
  }

  updateIconConfig(iconConfig) {
    this.config.icon = { ...this.config.icon, ...iconConfig }
    this.saveConfig()
  }

  updatePositionConfig(positionConfig) {
    this.config.position = { ...this.config.position, ...positionConfig }
    this.saveConfig()
  }

  updateBehaviorConfig(behaviorConfig) {
    this.config.behavior = { ...this.config.behavior, ...behaviorConfig }
    this.saveConfig()
  }

  updateMenuConfig(menuConfig) {
    this.config.menu = { ...this.config.menu, ...menuConfig }
    this.saveConfig()
  }

  resetToDefault() {
    this.config = { ...this.defaultConfig }
    this.saveConfig()
  }
}

module.exports = TrayConfig