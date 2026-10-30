const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('electronAPI', {
  minimizeWindow: () => safeSend('minimize-window'),
  maximizeWindow: () => safeSend('maximize-window'),
  closeWindow: () => safeSend('close-window'),
  showWindow: () => safeSend('show-window'),
  setAlwaysOnTop: (value) => safeSend('set-always-on-top', value),

  getAppVersion: () => safeSendSync('get-app-version'),
  getAppPath: (pathName) => safeSendSync('get-app-path', pathName),

  showMessageBox: (options) => {
    return createAsyncHandler('show-message-box', 'message-box-result', options)
  },

  showSaveDialog: (options) => {
    return createAsyncHandler('show-save-dialog', 'save-dialog-result', options)
  },

  showOpenDialog: (options) => {
    return createAsyncHandler('show-open-dialog', 'open-dialog-result', options)
  },

  onNewChat: (callback) => ipcRenderer.on('new-chat', wrapCallback(callback)),
  onOpenDesktopElf: (callback) => ipcRenderer.on('open-desktop-elf', wrapCallback(callback)),
  onOpenSettings: (callback) => ipcRenderer.on('open-settings', wrapCallback(callback)),
  onOpenVoiceSettings: (callback) => ipcRenderer.on('open-voice-settings', wrapCallback(callback)),
  onOpenThemeSettings: (callback) => ipcRenderer.on('open-theme-settings', wrapCallback(callback)),
  onOpenTraySettings: (callback) => ipcRenderer.on('open-tray-settings', wrapCallback(callback)),
  onToggleSidebar: (callback) => ipcRenderer.on('toggle-sidebar', wrapCallback(callback)),
  onExportChat: (callback) => ipcRenderer.on('export-chat', wrapCallback(callback)),
  onImportChat: (callback) => ipcRenderer.on('import-chat', wrapCallback(callback)),
  onClearChat: (callback) => ipcRenderer.on('clear-chat', wrapCallback(callback)),
  onOpenUrl: (callback) => ipcRenderer.on('open-url', wrapCallback(callback)),
  onWindowMaximized: (callback) => ipcRenderer.on('window-maximized', wrapCallback(callback)),

  removeAllListeners: (channel) => {
    try {
      ipcRenderer.removeAllListeners(channel)
    } catch (e) {
      console.warn('[ElectronAPI] Failed to remove listeners:', e)
    }
  },

  removeListener: (channel, callback) => {
    try {
      ipcRenderer.removeListener(channel, callback)
    } catch (e) {
      console.warn('[ElectronAPI] Failed to remove listener:', e)
    }
  },

  getTrayConfig: () => safeSendSync('get-tray-config'),
  updateTrayConfig: (config) => safeSendSync('update-tray-config', config),
  updateTrayBadge: (count) => safeSend('update-tray-badge', count),
  startTrayFlash: () => safeSend('start-tray-flash'),
  stopTrayFlash: () => safeSend('stop-tray-flash'),
  showTrayNotification: (title, body) => safeSend('show-tray-notification', title, body)

  // 安全加固（P1）：移除通用 IPC 透传（invoke/send/sendSync 任意 channel）。
  // 原实现允许渲染进程携带任意 channel 调用主进程，一旦渲染层出现 XSS，
  // 攻击者可滥用任意主进程 IPC 能力。渲染层仅通过上方白名单方法通信。
})

function safeSend(channel, ...args) {
  try {
    ipcRenderer.send(channel, ...args)
  } catch (e) {
    console.warn('[ElectronAPI] Failed to send:', channel, e)
  }
}

function safeSendSync(channel, ...args) {
  try {
    return ipcRenderer.sendSync(channel, ...args)
  } catch (e) {
    console.warn('[ElectronAPI] Failed to sendSync:', channel, e)
    return null
  }
}

function createAsyncHandler(requestChannel, responseChannel, data) {
  return new Promise((resolve) => {
    const timeout = setTimeout(() => {
      console.warn('[ElectronAPI] Async handler timeout:', requestChannel)
      ipcRenderer.removeAllListeners(responseChannel)
      resolve(null)
    }, 30000)

    ipcRenderer.once(responseChannel, (event, result) => {
      clearTimeout(timeout)
      resolve(result)
    })

    safeSend(requestChannel, data)
  })
}

function wrapCallback(callback) {
  return (...args) => {
    try {
      callback(...args)
    } catch (e) {
      console.error('[ElectronAPI] Callback error:', e)
    }
  }
}