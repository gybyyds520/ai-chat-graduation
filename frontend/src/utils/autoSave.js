class AutoSaveManager {
  constructor() {
    this.saveInterval = 10 * 60 * 1000
    this.localStorageKey = 'ai_chat_auto_save_data'
    this.uploadQueueKey = 'ai_chat_upload_queue'
    this.saveTimer = null
    this.isSaving = false
    this.saveStatus = 'idle'
    this.pendingData = []
    this.userId = null
    
    this.init()
  }

  init() {
    this.loadPendingData()
    this.startAutoSave()
    this.setupEventListeners()
  }

  setUserId(userId) {
    this.userId = userId
  }

  startAutoSave() {
    if (this.saveTimer) {
      clearInterval(this.saveTimer)
    }
    
    this.saveTimer = setInterval(() => {
      this.autoSave()
    }, this.saveInterval)
  }

  stopAutoSave() {
    if (this.saveTimer) {
      clearInterval(this.saveTimer)
      this.saveTimer = null
    }
  }

  setupEventListeners() {
    window.addEventListener('beforeunload', () => {
      this.manualSave()
    })

    window.addEventListener('offline', () => {
      console.log('网络断开，数据将保存在本地')
    })

    window.addEventListener('online', () => {
      console.log('网络恢复，开始上传本地缓存数据')
      this.uploadPendingData()
    })
  }

  captureData(dataType, content, metadata = {}) {
    const dataItem = {
      id: Date.now() + Math.random().toString(36).substr(2, 9),
      userId: this.userId,
      dataType,
      content,
      metadata,
      captureTime: new Date().toISOString(),
      status: 'pending'
    }
    
    this.pendingData.push(dataItem)
    
    if (this.pendingData.length >= 10) {
      this.manualSave()
    }
    
    return dataItem
  }

  captureFormData(formId, formData) {
    return this.captureData('form', formData, { formId })
  }

  captureChatMessage(messageData) {
    return this.captureData('chat_message', messageData, { 
      type: messageData.role || 'user' 
    })
  }

  captureUserAction(actionType, details = {}) {
    return this.captureData('user_action', details, { actionType })
  }

  captureEditorContent(editorId, content) {
    return this.captureData('editor_content', content, { editorId })
  }

  async autoSave() {
    if (this.isSaving || this.pendingData.length === 0) return
    await this.manualSave()
  }

  async manualSave() {
    if (this.isSaving) return
    
    this.isSaving = true
    this.saveStatus = 'saving'
    
    try {
      const dataToSave = [...this.pendingData]
      this.pendingData = []
      
      this.saveToLocalStorage(dataToSave)
      
      if (navigator.onLine) {
        await this.uploadToServer(dataToSave)
      } else {
        this.addToUploadQueue(dataToSave)
      }
      
      this.saveStatus = 'success'
      console.log('数据自动保存成功')
    } catch (error) {
      this.saveStatus = 'error'
      console.error('数据自动保存失败:', error)
      this.addToUploadQueue(this.pendingData)
    } finally {
      this.isSaving = false
      setTimeout(() => {
        if (this.saveStatus === 'success') {
          this.saveStatus = 'idle'
        }
      }, 3000)
    }
  }

  saveToLocalStorage(data) {
    try {
      const existing = this.getLocalData()
      const updated = [...existing, ...data]
      
      if (updated.length > 1000) {
        updated.splice(0, updated.length - 1000)
      }
      
      localStorage.setItem(this.localStorageKey, JSON.stringify(updated))
    } catch (error) {
      console.error('保存到本地存储失败:', error)
    }
  }

  getLocalData() {
    try {
      const data = localStorage.getItem(this.localStorageKey)
      return data ? JSON.parse(data) : []
    } catch {
      return []
    }
  }

  clearLocalData() {
    localStorage.removeItem(this.localStorageKey)
  }

  addToUploadQueue(data) {
    try {
      const existing = this.getUploadQueue()
      const updated = [...existing, ...data]
      localStorage.setItem(this.uploadQueueKey, JSON.stringify(updated))
    } catch (error) {
      console.error('添加到上传队列失败:', error)
    }
  }

  getUploadQueue() {
    try {
      const data = localStorage.getItem(this.uploadQueueKey)
      return data ? JSON.parse(data) : []
    } catch {
      return []
    }
  }

  clearUploadQueue() {
    localStorage.removeItem(this.uploadQueueKey)
  }

  async uploadToServer(data) {
    if (!data || data.length === 0) return
    
    try {
      const response = await fetch('/api/admin/data-pool/upload', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          userId: this.userId,
          data: data
        })
      })
      
      const result = await response.json()
      
      if (result.code === 200) {
        console.log('数据上传成功')
        return true
      } else {
        throw new Error(result.msg || '上传失败')
      }
    } catch (error) {
      console.error('上传数据到服务器失败:', error)
      this.addToUploadQueue(data)
      return false
    }
  }

  async uploadPendingData() {
    const queue = this.getUploadQueue()
    
    if (queue.length === 0) return
    
    try {
      const success = await this.uploadToServer(queue)
      
      if (success) {
        this.clearUploadQueue()
      }
    } catch (error) {
      console.error('上传队列数据失败:', error)
    }
  }

  loadPendingData() {
    const localData = this.getLocalData()
    const queueData = this.getUploadQueue()
    
    this.pendingData = [...localData, ...queueData]
    
    if (navigator.onLine && this.pendingData.length > 0) {
      setTimeout(() => {
        this.uploadPendingData()
      }, 5000)
    }
  }

  getSaveStatus() {
    return this.saveStatus
  }

  getPendingCount() {
    return this.pendingData.length + this.getUploadQueue().length
  }

  static getInstance() {
    if (!AutoSaveManager.instance) {
      AutoSaveManager.instance = new AutoSaveManager()
    }
    return AutoSaveManager.instance
  }
}

export default AutoSaveManager