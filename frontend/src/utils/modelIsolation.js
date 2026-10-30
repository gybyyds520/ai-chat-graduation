class ModelSandbox {
  constructor(modelId, modelConfig) {
    this.modelId = modelId
    this.modelConfig = modelConfig
    this.isolationState = {
      status: 'initialized',
      resourceUsage: { memory: 0, cpu: 0, tokens: 0 },
      lastAccessTime: Date.now(),
      errorCount: 0,
      isIsolated: true
    }
    this._memoryStore = new Map()
    this._eventBus = new EventTarget()
    this._accessControl = new Set()
    this._isolationBoundary = {
      allowedPaths: [],
      blockedPaths: [],
      resourceLimits: modelConfig.resourceLimits || {
        maxMemory: 512,
        maxCpu: 50,
        maxTokensPerMinute: 10000
      }
    }
    this._tokenBucket = {
      tokens: this._isolationBoundary.resourceLimits.maxTokensPerMinute,
      lastRefill: Date.now(),
      rate: this._isolationBoundary.resourceLimits.maxTokensPerMinute / 60
    }
    this._monitorInterval = null
    this.startMonitoring()
  }

  getState() {
    return { ...this.isolationState }
  }

  hasAccess(resourcePath) {
    if (this._isolationBoundary.allowedPaths.includes(resourcePath)) {
      return true
    }
    if (this._isolationBoundary.blockedPaths.includes(resourcePath)) {
      return false
    }
    return true
  }

  acquireResource(resourcePath, amount = 1) {
    if (!this.hasAccess(resourcePath)) {
      throw new Error(`Access denied to resource: ${resourcePath}`)
    }

    if (resourcePath === 'tokens') {
      if (!this._tryAcquireTokens(amount)) {
        throw new Error('Token limit exceeded')
      }
    }

    this.isolationState.resourceUsage[resourcePath] = 
      (this.isolationState.resourceUsage[resourcePath] || 0) + amount
    
    this.isolationState.lastAccessTime = Date.now()
    this._emit('resource-acquired', { resourcePath, amount })
    return true
  }

  releaseResource(resourcePath, amount = 1) {
    if (this.isolationState.resourceUsage[resourcePath] >= amount) {
      this.isolationState.resourceUsage[resourcePath] -= amount
      this._emit('resource-released', { resourcePath, amount })
    }
  }

  _tryAcquireTokens(amount) {
    const now = Date.now()
    const elapsed = (now - this._tokenBucket.lastRefill) / 1000
    this._tokenBucket.tokens = Math.min(
      this._isolationBoundary.resourceLimits.maxTokensPerMinute,
      this._tokenBucket.tokens + elapsed * this._tokenBucket.rate
    )
    this._tokenBucket.lastRefill = now

    if (this._tokenBucket.tokens >= amount) {
      this._tokenBucket.tokens -= amount
      return true
    }
    return false
  }

  setMemory(key, value) {
    const sanitizedKey = `model:${this.modelId}:${key}`
    this._memoryStore.set(sanitizedKey, value)
    this._emit('memory-set', { key, value })
  }

  getMemory(key) {
    const sanitizedKey = `model:${this.modelId}:${key}`
    return this._memoryStore.get(sanitizedKey)
  }

  clearMemory() {
    const prefix = `model:${this.modelId}:`
    for (const key of this._memoryStore.keys()) {
      if (key.startsWith(prefix)) {
        this._memoryStore.delete(key)
      }
    }
    this._emit('memory-cleared')
  }

  subscribe(eventName, callback) {
    this._eventBus.addEventListener(eventName, callback)
  }

  unsubscribe(eventName, callback) {
    this._eventBus.removeEventListener(eventName, callback)
  }

  _emit(eventName, detail) {
    this._eventBus.dispatchEvent(new CustomEvent(eventName, { detail }))
  }

  startMonitoring() {
    this._monitorInterval = setInterval(() => {
      this._checkResourceLimits()
      this._checkHealthStatus()
    }, 1000)
  }

  stopMonitoring() {
    if (this._monitorInterval) {
      clearInterval(this._monitorInterval)
      this._monitorInterval = null
    }
  }

  _checkResourceLimits() {
    const limits = this._isolationBoundary.resourceLimits
    
    if (this.isolationState.resourceUsage.memory > limits.maxMemory) {
      this._emit('resource-limit-exceeded', { resource: 'memory', limit: limits.maxMemory })
      this.isolationState.status = 'warning'
    }
    
    if (this.isolationState.resourceUsage.cpu > limits.maxCpu) {
      this._emit('resource-limit-exceeded', { resource: 'cpu', limit: limits.maxCpu })
      this.isolationState.status = 'warning'
    }

    if (this.isolationState.status === 'warning') {
      this.isolationState.errorCount++
    }
  }

  _checkHealthStatus() {
    const now = Date.now()
    const idleThreshold = 5 * 60 * 1000

    if (now - this.isolationState.lastAccessTime > idleThreshold) {
      this.isolationState.status = 'idle'
    } else if (this.isolationState.errorCount > 5) {
      this.isolationState.status = 'error'
    } else {
      this.isolationState.status = 'active'
    }
  }

  destroy() {
    this.stopMonitoring()
    this.clearMemory()
    this._eventBus = null
  }
}

class ModelIsolationManager {
  constructor() {
    this.sandboxes = new Map()
    this.globalMonitor = new EventTarget()
    this._monitorInterval = null
    this._isolationRules = {
      defaultResourceLimits: {
        maxMemory: 512,
        maxCpu: 50,
        maxTokensPerMinute: 10000
      }
    }
    this.startGlobalMonitoring()
  }

  createSandbox(modelId, modelConfig = {}) {
    if (this.sandboxes.has(modelId)) {
      return this.sandboxes.get(modelId)
    }

    const config = {
      resourceLimits: { ...this._isolationRules.defaultResourceLimits, ...modelConfig.resourceLimits },
      ...modelConfig
    }

    const sandbox = new ModelSandbox(modelId, config)
    
    sandbox.subscribe('resource-limit-exceeded', (e) => {
      this._emit('isolation-alert', {
        modelId,
        type: 'resource-limit-exceeded',
        detail: e.detail
      })
    })

    sandbox.subscribe('memory-cleared', () => {
      this._emit('isolation-event', {
        modelId,
        type: 'memory-cleared'
      })
    })

    this.sandboxes.set(modelId, sandbox)
    this._emit('sandbox-created', { modelId })
    return sandbox
  }

  getSandbox(modelId) {
    return this.sandboxes.get(modelId)
  }

  destroySandbox(modelId) {
    const sandbox = this.sandboxes.get(modelId)
    if (sandbox) {
      sandbox.destroy()
      this.sandboxes.delete(modelId)
      this._emit('sandbox-destroyed', { modelId })
    }
  }

  getIsolationStatus(modelId) {
    const sandbox = this.sandboxes.get(modelId)
    if (!sandbox) {
      return { exists: false }
    }
    return {
      exists: true,
      ...sandbox.getState()
    }
  }

  getAllIsolationStatus() {
    const statuses = []
    for (const [modelId, sandbox] of this.sandboxes.entries()) {
      statuses.push({
        modelId,
        ...sandbox.getState()
      })
    }
    return statuses
  }

  enforceIsolation(modelId) {
    const sandbox = this.sandboxes.get(modelId)
    if (sandbox) {
      sandbox.isolationState.isIsolated = true
      sandbox.clearMemory()
      this._emit('isolation-enforced', { modelId })
    }
  }

  relaxIsolation(modelId) {
    const sandbox = this.sandboxes.get(modelId)
    if (sandbox) {
      sandbox.isolationState.isIsolated = false
      this._emit('isolation-relaxed', { modelId })
    }
  }

  subscribe(eventName, callback) {
    this.globalMonitor.addEventListener(eventName, callback)
  }

  unsubscribe(eventName, callback) {
    this.globalMonitor.removeEventListener(eventName, callback)
  }

  _emit(eventName, detail) {
    this.globalMonitor.dispatchEvent(new CustomEvent(eventName, { detail }))
  }

  startGlobalMonitoring() {
    this._monitorInterval = setInterval(() => {
      this._checkAllSandboxes()
    }, 5000)
  }

  stopGlobalMonitoring() {
    if (this._monitorInterval) {
      clearInterval(this._monitorInterval)
      this._monitorInterval = null
    }
  }

  _checkAllSandboxes() {
    const statuses = this.getAllIsolationStatus()
    const problematic = statuses.filter(s => s.status === 'error' || s.status === 'warning')
    
    if (problematic.length > 0) {
      this._emit('isolation-status-update', {
        total: statuses.length,
        problematic: problematic.length,
        details: problematic
      })
    }
  }

  destroy() {
    this.stopGlobalMonitoring()
    for (const sandbox of this.sandboxes.values()) {
      sandbox.destroy()
    }
    this.sandboxes.clear()
  }
}

export const modelIsolationManager = new ModelIsolationManager()

export { ModelSandbox, ModelIsolationManager }