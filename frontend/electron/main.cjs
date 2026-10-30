const { app, BrowserWindow, Menu, shell, dialog, ipcMain, globalShortcut, screen } = require('electron')
const path = require('path')
const fs = require('fs')
const EnhancedTray = require('./enhancedTray.cjs')

// 黑屏修复：部分环境（虚拟机/远程桌面/集成显卡驱动）GPU 硬件加速渲染异常导致整窗黑屏，
// 统一禁用硬件加速，改用软件渲染（功能不受影响）。
if (app.disableHardwareAcceleration) {
  app.disableHardwareAcceleration()
}

let mainWindow = null
let enhancedTray = null
let isQuiting = false
let launchTime = Date.now()

const registeredTimers = []
const registeredListeners = []

function registerTimer(timer) {
  registeredTimers.push(timer)
}

function clearAllTimers() {
  registeredTimers.forEach(timer => {
    try {
      clearInterval(timer)
      clearTimeout(timer)
    } catch (e) {
      console.warn('清理定时器失败:', e.message)
    }
  })
  registeredTimers.length = 0
}

function registerListener(channel, listener) {
  registeredListeners.push({ channel, listener })
}

function removeAllListeners() {
  registeredListeners.forEach(({ channel, listener }) => {
    try {
      ipcMain.removeListener(channel, listener)
    } catch (e) {
      console.warn('移除监听器失败:', channel, e.message)
    }
  })
  registeredListeners.length = 0
}

const APP_CONFIG_DIR = path.join(__dirname, '../config')
const WINDOW_STATE_FILE = path.join(APP_CONFIG_DIR, 'window-state.json')

function loadWindowState() {
  try {
    const data = fs.readFileSync(WINDOW_STATE_FILE, 'utf-8')
    const state = JSON.parse(data)

    // 屏幕可见性校验：记忆的窗口位置必须与当前显示器工作区有足够交集，
    // 否则（外接屏拔除/分辨率变化/最大化残留导致 x/y 在屏幕外）重置为居中，避免"窗口找不到"。
    if (
      state &&
      typeof state.width === 'number' &&
      typeof state.height === 'number' &&
      typeof state.x === 'number' &&
      typeof state.y === 'number' &&
      screen.getDisplayMatching({ x: state.x, y: state.y, width: 1, height: 1 })
    ) {
      const wa = screen.getDisplayMatching({ x: state.x, y: state.y, width: state.width, height: state.height }).workArea
      const visibleW = Math.min(state.x + state.width, wa.x + wa.width) - Math.max(state.x, wa.x)
      const visibleH = Math.min(state.y + state.height, wa.y + wa.height) - Math.max(state.y, wa.y)
      if (visibleW >= 200 && visibleH >= 200) {
        return state
      }
      console.warn('[Electron] 窗口状态位置不可见，重置为居中:', JSON.stringify(state))
    }
  } catch (e) {
    console.warn('[Electron] 读取窗口状态失败，使用默认:', e.message)
  }
  return { width: 1280, height: 800, x: undefined, y: undefined, maximized: false }
}

function saveWindowState() {
  if (!mainWindow) return
  const bounds = mainWindow.getBounds()
  const state = {
    width: bounds.width,
    height: bounds.height,
    x: bounds.x,
    y: bounds.y,
    maximized: mainWindow.isMaximized()
  }
  try {
    const dir = path.dirname(WINDOW_STATE_FILE)
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true })
    }
    fs.writeFileSync(WINDOW_STATE_FILE, JSON.stringify(state, null, 2), { encoding: 'utf-8' })
  } catch (e) {
    console.warn('保存窗口状态失败(非关键错误):', e.message)
  }
}

function disableDevToolsShortcuts() {
  const devShortcuts = [
    'CommandOrControl+Shift+I',
    'CommandOrControl+Shift+J',
    'CommandOrControl+Shift+C',
    'CommandOrControl+Shift+K',
    'CommandOrControl+Alt+I',
    'F12',
    'Command+Option+I',
    'CommandOrControl+U'
  ]

  devShortcuts.forEach(shortcut => {
    globalShortcut.register(shortcut, () => {})
  })
}

function createWindow() {
  const windowState = loadWindowState()

  const windowOptions = {
    width: windowState.width,
    height: windowState.height,
    minWidth: 800,
    minHeight: 600,
    title: 'AI Chat',
    icon: path.join(__dirname, 'icon.ico'),
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.cjs'),
      enableRemoteModule: false,
      devTools: false,
      enableWebSecurity: true,
      allowRunningInsecureContent: false,
      experimentalFeatures: false,
      experimentalCanvasFeatures: false
    },
    frame: false,
    show: false,
    backgroundColor: '#1e293b',
    titleBarStyle: 'hidden'
  }

  if (windowState.x !== undefined && windowState.y !== undefined) {
    windowOptions.x = windowState.x
    windowOptions.y = windowState.y
  } else {
    windowOptions.center = true
  }

  if (process.platform === 'darwin') {
    windowOptions.trafficLightPosition = { x: 12, y: 12 }
  }

  mainWindow = new BrowserWindow(windowOptions)

  const isDev = process.env.NODE_ENV === 'development' || process.env.ELECTRON_DEV === '1'
  const devServerUrl = process.env.VITE_DEV_SERVER_URL || 'http://localhost:5187'
  const distIndex = path.join(__dirname, '../dist/index.html')

  if (isDev) {
    mainWindow.loadURL(devServerUrl)
    // 加载失败（dev server 未就绪等）→ 1.5s 后重试一次
    mainWindow.webContents.on('did-fail-load', (event, errorCode, errorDescription) => {
      if (errorCode === -3) return // ERR_ABORTED（用户主动取消）
      console.error('[Electron] 页面加载失败:', errorCode, errorDescription)
      setTimeout(() => {
        if (mainWindow && !mainWindow.isDestroyed()) {
          mainWindow.loadURL(devServerUrl)
        }
      }, 1500)
    })
  } else {
    mainWindow.loadFile(distIndex)
  }

  // 兜底显示：即使 ready-to-show 未触发（慢加载/软渲染），6s 后也显示窗口避免"黑屏"假象
  setTimeout(() => {
    if (mainWindow && !mainWindow.isDestroyed() && !mainWindow.isVisible()) {
      console.warn('[Electron] ready-to-show 超时，强制显示窗口')
      mainWindow.show()
    }
  }, 6000)

  mainWindow.once('ready-to-show', () => {
    if (windowState.maximized) {
      mainWindow.maximize()
    }
    mainWindow.show()
  })

  mainWindow.on('resize', saveWindowState)
  mainWindow.on('move', saveWindowState)
  mainWindow.on('maximize', () => {
    saveWindowState()
    mainWindow.webContents.send('window-maximized', true)
  })
  mainWindow.on('unmaximize', () => {
    saveWindowState()
    mainWindow.webContents.send('window-maximized', false)
  })

  mainWindow.on('close', (event) => {
    if (!isQuiting && app.isPackaged) {
      event.preventDefault()
      mainWindow.hide()
      showMinimizeNotification()
    } else if (!isQuiting && !app.isPackaged) {
      app.quit()
    }
  })

  mainWindow.on('closed', () => {
    mainWindow = null
  })

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })

  mainWindow.webContents.on('before-input-event', (event, input) => {
    if (input.control || input.meta) {
      switch (input.key.toLowerCase()) {
        case 'q':
          if (!isQuiting) {
            isQuiting = true
            app.quit()
          }
          break
        case 'n':
          if (!mainWindow.isVisible()) {
            mainWindow.show()
          }
          break
        case 'f':
          mainWindow.setFullScreen(!mainWindow.isFullScreen())
          break
      }
    }

    if (input.key === 'F12' || 
        (input.shift && (input.control || input.meta) && (input.key.toLowerCase() === 'i' || input.key.toLowerCase() === 'j'))) {
      event.preventDefault()
    }
  })

  mainWindow.webContents.on('devtools-opened', () => {
    mainWindow.webContents.closeDevTools()
  })

  mainWindow.webContents.on('devtools-closed', () => {
    mainWindow.webContents.closeDevTools()
  })

  mainWindow.webContents.on('will-attach-webview', (event, webPreferences, params) => {
    webPreferences.devTools = false
  })
}

function createTray() {
  try {
    enhancedTray = new EnhancedTray(mainWindow, app)
    enhancedTray.createTray()
  } catch (e) {
    console.error('创建增强版系统托盘失败:', e)
  }
}

function createMenu() {
  const menuTemplate = [
    {
      label: 'AI Chat',
      submenu: [
        { 
          label: '关于', 
          click: () => showAboutDialog(),
          accelerator: 'CmdOrCtrl+I'
        },
        { type: 'separator' },
        { 
          label: '检查更新', 
          click: () => checkForUpdates(),
          accelerator: 'CmdOrCtrl+U'
        },
        { type: 'separator' },
        { 
          label: '退出', 
          role: 'quit',
          accelerator: 'CmdOrCtrl+Q'
        }
      ]
    },
    {
      label: '文件',
      submenu: [
        { 
          label: '新建对话', 
          click: () => mainWindow.webContents.send('new-chat'),
          accelerator: 'CmdOrCtrl+N'
        },
        { type: 'separator' },
        { 
          label: '导出聊天记录', 
          click: () => exportChatHistory(),
          accelerator: 'CmdOrCtrl+E'
        },
        { 
          label: '导入聊天记录', 
          click: () => importChatHistory()
        },
        { type: 'separator' },
        { 
          label: '清空聊天记录', 
          click: () => clearChatHistory(),
          accelerator: 'CmdOrCtrl+Shift+D'
        }
      ]
    },
    {
      label: '编辑',
      submenu: [
        { label: '撤销', role: 'undo', accelerator: 'CmdOrCtrl+Z' },
        { label: '重做', role: 'redo', accelerator: 'CmdOrCtrl+Y' },
        { type: 'separator' },
        { label: '剪切', role: 'cut', accelerator: 'CmdOrCtrl+X' },
        { label: '复制', role: 'copy', accelerator: 'CmdOrCtrl+C' },
        { label: '粘贴', role: 'paste', accelerator: 'CmdOrCtrl+V' },
        { type: 'separator' },
        { label: '全选', role: 'selectAll', accelerator: 'CmdOrCtrl+A' }
      ]
    },
    {
      label: '视图',
      submenu: [
        { 
          label: '刷新', 
          role: 'reload',
          accelerator: 'CmdOrCtrl+R'
        },
        { 
          label: '强制刷新', 
          role: 'forceReload',
          accelerator: 'CmdOrCtrl+Shift+R'
        },
        { type: 'separator' },
        { 
          label: '全屏', 
          role: 'togglefullscreen',
          accelerator: 'F11'
        },
        { type: 'separator' },
        {
          label: '显示侧边栏',
          type: 'checkbox',
          checked: true,
          click: (menuItem) => {
            mainWindow.webContents.send('toggle-sidebar', menuItem.checked)
          }
        }
      ]
    },
    {
      label: '窗口',
      submenu: [
        { 
          label: '最小化', 
          role: 'minimize',
          accelerator: 'CmdOrCtrl+M'
        },
        { 
          label: '最大化', 
          role: 'maximize',
          accelerator: 'CmdOrCtrl+Shift+M'
        },
        { type: 'separator' },
        { 
          label: '关闭', 
          role: 'close',
          accelerator: 'CmdOrCtrl+W'
        },
        { type: 'separator' },
        {
          label: '置顶',
          type: 'checkbox',
          checked: false,
          click: (menuItem) => {
            mainWindow.setAlwaysOnTop(menuItem.checked)
          }
        }
      ]
    },
    {
      label: '工具',
      submenu: [
        {
          label: '桌面小精灵',
          click: () => {
            mainWindow.show()
            mainWindow.webContents.send('open-desktop-elf')
          },
          accelerator: 'CmdOrCtrl+Shift+E'
        },
        { type: 'separator' },
        {
          label: '语音设置',
          click: () => {
            mainWindow.webContents.send('open-voice-settings')
          }
        },
        {
          label: '主题设置',
          click: () => {
            mainWindow.webContents.send('open-theme-settings')
          }
        },
        { type: 'separator' },
        {
          label: '托盘设置',
          click: () => {
            mainWindow.show()
            mainWindow.webContents.send('open-tray-settings')
          }
        }
      ]
    },
    {
      label: '帮助',
      submenu: [
        { 
          label: '文档', 
          click: () => shell.openExternal('https://docs.aichat.com')
        },
        { 
          label: '社区', 
          click: () => shell.openExternal('https://community.aichat.com')
        },
        { type: 'separator' },
        { 
          label: '报告问题', 
          click: () => shell.openExternal('https://github.com/aichat/issues')
        },
        { 
          label: '反馈建议', 
          click: () => shell.openExternal('https://github.com/aichat/feedback')
        }
      ]
    }
  ]

  const menu = Menu.buildFromTemplate(menuTemplate)
  Menu.setApplicationMenu(menu)
}

function showAboutDialog() {
  dialog.showMessageBox({
    type: 'info',
    title: '关于 AI Chat',
    message: 'AI Chat v1.0.0',
    detail: '智能对话，连接未来\n\n基于先进大语言模型的智能对话应用\n\n支持多种AI角色、语音交互、桌面小精灵等功能',
    icon: path.join(__dirname, 'icon.ico'),
    buttons: ['确定']
  })
}

function checkForUpdates() {
  dialog.showMessageBox({
    type: 'info',
    title: '检查更新',
    message: '当前已是最新版本',
    detail: '版本: 1.0.0\n\n暂无可用更新',
    icon: path.join(__dirname, 'icon.ico'),
    buttons: ['确定']
  })
}

function showMinimizeNotification() {
  if (app.isPackaged && process.platform === 'win32' && enhancedTray) {
    enhancedTray.showNotification('AI Chat', '应用已最小化到系统托盘')
  }
}

function exportChatHistory() {
  const options = {
    title: '导出聊天记录',
    defaultPath: `chat-history-${Date.now()}.json`,
    filters: [
      { name: 'JSON文件', extensions: ['json'] },
      { name: '所有文件', extensions: ['*'] }
    ]
  }

  dialog.showSaveDialog(mainWindow, options).then((result) => {
    if (!result.canceled && result.filePath) {
      mainWindow.webContents.send('export-chat', result.filePath)
    }
  })
}

function importChatHistory() {
  const options = {
    title: '导入聊天记录',
    filters: [
      { name: 'JSON文件', extensions: ['json'] },
      { name: '所有文件', extensions: ['*'] }
    ]
  }

  dialog.showOpenDialog(mainWindow, options).then((result) => {
    if (!result.canceled && result.filePaths.length > 0) {
      mainWindow.webContents.send('import-chat', result.filePaths[0])
    }
  })
}

function clearChatHistory() {
  dialog.showMessageBox({
    type: 'warning',
    title: '确认清空',
    message: '确定要清空所有聊天记录吗？',
    detail: '此操作不可恢复，请谨慎操作',
    icon: path.join(__dirname, 'icon.ico'),
    buttons: ['取消', '确定'],
    defaultId: 0
  }).then((result) => {
    if (result.response === 1) {
      mainWindow.webContents.send('clear-chat')
      dialog.showMessageBox({
        type: 'info',
        title: '操作成功',
        message: '聊天记录已清空',
        icon: path.join(__dirname, 'icon.ico'),
        buttons: ['确定']
      })
    }
  })
}

ipcMain.on('get-app-version', (event) => {
  event.returnValue = app.getVersion()
})

ipcMain.on('get-app-path', (event, pathName) => {
  event.returnValue = app.getPath(pathName)
})

ipcMain.on('show-message-box', (event, options) => {
  dialog.showMessageBox(options).then((result) => {
    event.sender.send('message-box-result', result)
  })
})

ipcMain.on('show-save-dialog', (event, options) => {
  dialog.showSaveDialog(mainWindow, options).then((result) => {
    event.sender.send('save-dialog-result', result)
  })
})

ipcMain.on('show-open-dialog', (event, options) => {
  dialog.showOpenDialog(mainWindow, options).then((result) => {
    event.sender.send('open-dialog-result', result)
  })
})

ipcMain.on('minimize-window', () => {
  mainWindow.minimize()
})

ipcMain.on('maximize-window', () => {
  if (mainWindow.isMaximized()) {
    mainWindow.unmaximize()
  } else {
    mainWindow.maximize()
  }
})

ipcMain.on('close-window', () => {
  if (!isQuiting && app.isPackaged) {
    mainWindow.hide()
    showMinimizeNotification()
  } else {
    isQuiting = true
    app.quit()
  }
})

ipcMain.on('show-window', () => {
  mainWindow.show()
})

ipcMain.on('set-always-on-top', (event, value) => {
  mainWindow.setAlwaysOnTop(value)
})

ipcMain.on('get-tray-config', (event) => {
  if (enhancedTray) {
    event.returnValue = enhancedTray.getConfig()
  } else {
    event.returnValue = null
  }
})

ipcMain.on('update-tray-config', (event, config) => {
  if (enhancedTray) {
    enhancedTray.updateConfig(config)
    event.returnValue = true
  } else {
    event.returnValue = false
  }
})

ipcMain.on('update-tray-badge', (event, count) => {
  if (enhancedTray) {
    enhancedTray.updateBadge(count)
  }
})

ipcMain.on('start-tray-flash', () => {
  if (enhancedTray) {
    enhancedTray.startFlash()
  }
})

ipcMain.on('stop-tray-flash', () => {
  if (enhancedTray) {
    enhancedTray.stopFlash()
  }
})

ipcMain.on('show-tray-notification', (event, title, body) => {
  if (enhancedTray) {
    enhancedTray.showNotification(title, body)
  }
})

const alarmTimers = {}

ipcMain.on('schedule-alarm', (event, alarm) => {
  if (alarmTimers[alarm.id]) {
    clearTimeout(alarmTimers[alarm.id])
  }

  const [hours, minutes] = alarm.time.split(':').map(Number)
  const now = new Date()
  let alarmTime = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hours, minutes, 0, 0)

  if (alarmTime <= now) {
    alarmTime.setDate(alarmTime.getDate() + 1)
  }

  const delay = alarmTime.getTime() - now.getTime()

  const timer = setTimeout(() => {
    if (mainWindow) {
      mainWindow.show()
      mainWindow.webContents.send('alarm-triggered', alarm)
    }
    
    if (enhancedTray) {
      enhancedTray.showNotification('闹钟提醒', alarm.label || `闹钟时间到！${alarm.time}`)
      enhancedTray.startFlash()
      
      setTimeout(() => {
        enhancedTray.stopFlash()
      }, 10000)
    }
  }, delay)

  alarmTimers[alarm.id] = timer
  registerTimer(timer)
  
  console.log(`[Alarm] Scheduled alarm: ${alarm.id} at ${alarmTime.toLocaleString()}`)
})

ipcMain.on('cancel-alarm', (event, alarmId) => {
  if (alarmTimers[alarmId]) {
    clearTimeout(alarmTimers[alarmId])
    delete alarmTimers[alarmId]
    console.log(`[Alarm] Cancelled alarm: ${alarmId}`)
  }
})

ipcMain.on('cancel-all-alarms', () => {
  Object.keys(alarmTimers).forEach(id => {
    clearTimeout(alarmTimers[id])
  })
  alarmTimers = {}
  console.log('[Alarm] All alarms cancelled')
})

let mediaSessionHandler = null

ipcMain.on('start-media-session', () => {
  if (process.platform === 'darwin') {
    app.setUserActivity('music', {
      type: 'playing',
      title: 'AI Chat Music',
      description: '正在播放音乐'
    })
  }
  
  console.log('[Media] Media session started')
})

ipcMain.on('update-media-info', (event, info) => {
  if (process.platform === 'darwin') {
    app.setUserActivity('music', {
      type: info.isPlaying ? 'playing' : 'paused',
      title: info.title || 'AI Chat Music',
      description: info.artist ? `${info.artist} - ${info.album}` : '正在播放'
    })
  }
  
  if (enhancedTray) {
    if (info.isPlaying && info.title) {
      enhancedTray.setToolTip(`正在播放: ${info.title}`)
    } else {
      enhancedTray.setToolTip('AI Chat')
    }
  }
})

ipcMain.on('stop-media-session', () => {
  if (process.platform === 'darwin') {
    app.clearUserActivity('music')
  }
  
  if (enhancedTray) {
    enhancedTray.setToolTip('AI Chat')
  }
  
  console.log('[Media] Media session stopped')
})

ipcMain.on('request-screen-wake', () => {
  if (mainWindow) {
    mainWindow.setAlwaysOnTop(true)
    mainWindow.show()
    
    setTimeout(() => {
      mainWindow.setAlwaysOnTop(false)
    }, 5000)
  }
  console.log('[System] Screen wake requested')
})

ipcMain.on('get-system-info', (event) => {
  const info = {
    platform: process.platform,
    arch: process.arch,
    version: process.getSystemVersion ? process.getSystemVersion() : 'unknown',
    memory: process.getSystemMemoryInfo ? process.getSystemMemoryInfo() : null
  }
  event.returnValue = info
})

app.whenReady().then(() => {
  if (process.platform === 'win32') {
    app.setAppUserModelId('com.aichat.app')
  }
  
  if (process.env.NODE_ENV !== 'development') {
    disableDevToolsShortcuts()
  }

  createWindow()
  Menu.setApplicationMenu(null)
  createTray()

  app.on('activate', () => {
    const allWindows = BrowserWindow.getAllWindows()
    if (allWindows.length === 0) {
      createWindow()
    } else if (mainWindow) {
      mainWindow.show()
    }
  })
})

app.on('before-quit', () => {
  isQuiting = true
  
  console.log('[App] 开始清理资源...')
  
  globalShortcut.unregisterAll()
  
  if (enhancedTray) {
    enhancedTray.destroy()
    enhancedTray = null
  }
  
  clearAllTimers()
  
  removeAllListeners()
  
  ipcMain.removeAllListeners()
  
  if (mainWindow) {
    mainWindow.webContents.removeAllListeners()
  }
  
  console.log('[App] 资源清理完成，准备退出')
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.setAsDefaultProtocolClient('aichat')

app.on('open-url', (event, url) => {
  event.preventDefault()
  if (!mainWindow) {
    createWindow()
  }
})