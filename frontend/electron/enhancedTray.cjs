const { Tray, Menu, nativeImage, shell, ipcMain } = require('electron')
const path = require('path')
const fs = require('fs')

class EnhancedTray {
  constructor(mainWindow, app) {
    this.mainWindow = mainWindow
    this.app = app
    this.tray = null
    this.configPath = path.join(__dirname, '../config', 'tray-config.json')
    this.flashInterval = null
    
    this.config = this.loadConfig()
    
    this.iconCache = {}
  }

  loadConfig() {
    try {
      const dir = path.dirname(this.configPath)
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true })
      }
      const data = fs.readFileSync(this.configPath, 'utf-8')
      return { ...this.getDefaultConfig(), ...JSON.parse(data) }
    } catch (e) {
      return this.getDefaultConfig()
    }
  }

  getDefaultConfig() {
    return {
      icon: {
        type: 'default',
        path: '',
        emoji: '🤖',
        size: 16,
        color: '#4f46e5',
        showBadge: true,
        badgeColor: '#ff4d4f'
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

  updateConfig(newConfig) {
    this.config = { ...this.config, ...newConfig }
    this.saveConfig()
    this.updateTray()
  }

  createIcon(iconType = 'default', badgeCount = 0) {
    const cacheKey = `${iconType}-${badgeCount}`
    
    if (this.iconCache[cacheKey]) {
      return this.iconCache[cacheKey]
    }

    let image
    
    switch (iconType) {
      case 'custom':
        if (this.config.icon.path && fs.existsSync(this.config.icon.path)) {
          image = nativeImage.createFromPath(this.config.icon.path)
        } else {
          image = this.createDefaultIcon()
        }
        break
        
      case 'emoji':
        image = this.createEmojiIcon()
        break
        
      default:
        image = this.createDefaultIcon()
    }

    if (badgeCount > 0 && this.config.icon.showBadge) {
      image = this.addBadgeToImage(image, badgeCount)
    }

    this.iconCache[cacheKey] = image
    
    return image
  }

  createDefaultIcon() {
    const iconPath = path.join(__dirname, 'icon.ico')
    
    if (fs.existsSync(iconPath)) {
      return nativeImage.createFromPath(iconPath)
    }
    
    return nativeImage.createFromPath(path.join(__dirname, 'icon.ico'))
  }

  createEmojiIcon() {
    const emoji = this.config.icon.emoji || '🤖'
    const size = this.config.icon.size * 4
    
    const canvas = nativeImage.createFromDataURL(
      `data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
        <text x="50%" y="50%" font-size="${size * 0.7}" text-anchor="middle" dominant-baseline="middle">${emoji}</text>
      </svg>`
    )
    
    return canvas
  }

  addBadgeToImage(image, count) {
    try {
      const size = image.getSize()
      const badgeSize = Math.min(size.width, size.height) * 0.4
      const badgeText = count > 99 ? '99+' : count.toString()
      
      return image
    } catch (e) {
      return image
    }
  }

  buildContextMenu() {
    const menuItems = []
    const { menu } = this.config

    if (menu.showMainWindow) {
      menuItems.push({
        label: '显示主窗口',
        click: () => this.handleShowMainWindow()
      })
    }

    if (menu.showNewChat) {
      menuItems.push({
        label: '新建对话',
        click: () => this.handleNewChat()
      })
    }

    if (menu.showDesktopElf) {
      menuItems.push({
        label: '桌面小精灵',
        click: () => this.handleOpenDesktopElf()
      })
    }

    if (menuItems.length > 0) {
      menuItems.push({ type: 'separator' })
    }

    if (menu.showSettings) {
      menuItems.push({
        label: '设置',
        click: () => this.handleOpenSettings()
      })
    }

    if (menu.customItems && menu.customItems.length > 0) {
      menuItems.push({ type: 'separator' })
      menu.customItems.forEach(item => {
        menuItems.push({
          label: item.label,
          click: () => this.handleCustomMenuClick(item)
        })
      })
    }

    if (menu.showExit) {
      menuItems.push({ type: 'separator' })
      menuItems.push({
        label: '退出',
        click: () => this.handleExit()
      })
    }

    return Menu.buildFromTemplate(menuItems)
  }

  createTray() {
    try {
      this.tray = new Tray(this.createIcon(this.config.icon.type))
      this.tray.setToolTip('AI Chat')
      
      this.tray.setContextMenu(this.buildContextMenu())
      
      this.bindClickEvents()
      
      console.log('系统托盘创建成功')
    } catch (e) {
      console.error('创建系统托盘失败:', e)
    }
  }

  bindClickEvents() {
    this.tray.on('click', (event, bounds) => {
      this.handleClick(event, bounds, 'single')
    })
    
    this.tray.on('double-click', (event, bounds) => {
      this.handleClick(event, bounds, 'double')
    })
    
    this.tray.on('right-click', (event, bounds) => {
      this.tray.popUpContextMenu()
    })
  }

  handleClick(event, bounds, clickType) {
    const action = this.config.behavior[`${clickType}Click`]
    
    switch (action) {
      case 'show':
        this.handleShowMainWindow()
        break
        
      case 'hide':
        this.handleHideMainWindow()
        break
        
      case 'toggle':
        this.handleToggleWindow()
        break
        
      case 'menu':
        this.tray.popUpContextMenu()
        break
        
      case 'newChat':
        if (clickType === 'middle') {
          this.handleNewChat()
        }
        break
        
      default:
        this.handleShowMainWindow()
    }
  }

  handleShowMainWindow() {
    if (this.mainWindow) {
      this.mainWindow.show()
      this.mainWindow.focus()
    }
  }

  handleHideMainWindow() {
    if (this.mainWindow) {
      this.mainWindow.hide()
    }
  }

  handleToggleWindow() {
    if (this.mainWindow) {
      if (this.mainWindow.isVisible()) {
        this.mainWindow.hide()
      } else {
        this.mainWindow.show()
        this.mainWindow.focus()
      }
    }
  }

  handleNewChat() {
    this.handleShowMainWindow()
    this.mainWindow.webContents.send('new-chat')
  }

  handleOpenDesktopElf() {
    this.handleShowMainWindow()
    this.mainWindow.webContents.send('open-desktop-elf')
  }

  handleOpenSettings() {
    this.handleShowMainWindow()
    this.mainWindow.webContents.send('open-settings')
  }

  handleCustomMenuClick(item) {
    // 安全加固：仅允许 http/https 协议外链，禁止 file:/javascript: 等危险协议
    if (item.action === 'open-url' && item.url) {
      try {
        const url = new URL(item.url)
        if (url.protocol === 'http:' || url.protocol === 'https:') {
          shell.openExternal(item.url)
        } else {
          console.warn('[Tray] 拒绝打开非 http/https 链接:', item.url)
        }
      } catch (e) {
        console.warn('[Tray] 非法链接，已忽略:', item.url)
      }
    } else if (item.action === 'ipc' && item.channel) {
      this.mainWindow.webContents.send(item.channel, item.data)
    }
  }

  handleExit() {
    this.app.quit()
  }

  updateTray() {
    if (this.tray) {
      this.tray.setImage(this.createIcon(this.config.icon.type))
      this.tray.setContextMenu(this.buildContextMenu())
    }
  }

  updateBadge(count) {
    if (this.tray) {
      this.tray.setImage(this.createIcon(this.config.icon.type, count))
      
      if (count > 0) {
        this.tray.setToolTip(`AI Chat (${count} 未读)`)
      } else {
        this.tray.setToolTip('AI Chat')
      }
    }
  }

  startFlash() {
    if (!this.config.behavior.flashOnMessage || !this.tray) return
    
    let isOriginal = true
    const originalIcon = this.createIcon(this.config.icon.type)
    const flashIcon = this.createFlashIcon()
    
    this.flashInterval = setInterval(() => {
      if (this.tray) {
        this.tray.setImage(isOriginal ? flashIcon : originalIcon)
        isOriginal = !isOriginal
      }
    }, 500)
  }

  stopFlash() {
    if (this.flashInterval) {
      clearInterval(this.flashInterval)
      this.flashInterval = null
      this.updateTray()
    }
  }

  createFlashIcon() {
    return nativeImage.createFromPath(path.join(__dirname, 'icon.ico'))
  }

  showNotification(title, body) {
    if (this.config.behavior.showNotification && this.tray) {
      this.tray.displayBalloon({
        title: title || 'AI Chat',
        content: body || '有新消息',
        icon: this.createIcon(this.config.icon.type)
      })
    }
  }

  destroy() {
    this.stopFlash()
    if (this.tray) {
      this.tray.destroy()
      this.tray = null
    }
  }

  getConfig() {
    return this.config
  }
}

module.exports = EnhancedTray