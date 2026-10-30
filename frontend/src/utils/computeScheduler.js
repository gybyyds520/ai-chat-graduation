class ComputeTask {
  constructor(taskId, modelId, priority = 'normal', complexity = 'medium') {
    this.taskId = taskId
    this.modelId = modelId
    this.priority = priority
    this.complexity = complexity
    this.status = 'pending'
    this.createdAt = Date.now()
    this.startedAt = null
    this.completedAt = null
    this.resourceUsage = { cpu: 0, memory: 0, tokens: 0 }
    this.estimatedDuration = this._estimateDuration()
    this.actualDuration = null
  }

  _estimateDuration() {
    const baseDurations = {
      low: 100,
      medium: 500,
      high: 2000
    }
    const priorityMultiplier = {
      low: 0.5,
      normal: 1,
      high: 2,
      critical: 4
    }
    return baseDurations[this.complexity] * priorityMultiplier[this.priority]
  }

  start() {
    this.status = 'running'
    this.startedAt = Date.now()
  }

  complete() {
    this.status = 'completed'
    this.completedAt = Date.now()
    this.actualDuration = this.completedAt - this.startedAt
  }

  fail(error) {
    this.status = 'failed'
    this.completedAt = Date.now()
    this.error = error
  }

  updateResourceUsage(cpu, memory, tokens) {
    this.resourceUsage = { cpu, memory, tokens }
  }

  getMetrics() {
    return {
      taskId: this.taskId,
      modelId: this.modelId,
      priority: this.priority,
      complexity: this.complexity,
      status: this.status,
      created: this.createdAt,
      started: this.startedAt,
      completed: this.completedAt,
      estimatedDuration: this.estimatedDuration,
      actualDuration: this.actualDuration,
      resourceUsage: this.resourceUsage,
      efficiency: this.actualDuration ? (this.estimatedDuration / this.actualDuration) * 100 : null
    }
  }
}

class ComputeScheduler {
  constructor() {
    this.taskQueue = []
    this.runningTasks = new Map()
    this.completedTasks = []
    this.resourcePool = {
      totalCpu: 100,
      totalMemory: 2048,
      availableCpu: 100,
      availableMemory: 2048
    }
    this.priorityQueue = {
      critical: [],
      high: [],
      normal: [],
      low: []
    }
    this.schedulingInterval = null
    this.metrics = {
      totalTasks: 0,
      completedTasks: 0,
      failedTasks: 0,
      averageEfficiency: 0,
      resourceUtilization: { cpu: 0, memory: 0 }
    }
    this._efficiencyHistory = []
    this._maxHistorySize = 100
    this.startScheduling()
  }

  submitTask(modelId, priority = 'normal', complexity = 'medium') {
    const taskId = `task-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    const task = new ComputeTask(taskId, modelId, priority, complexity)
    
    this.taskQueue.push(task)
    this.priorityQueue[priority].push(task)
    this.metrics.totalTasks++
    
    this._emit('task-submitted', { taskId, modelId, priority })
    return task
  }

  startScheduling() {
    this.schedulingInterval = setInterval(() => {
      this._scheduleTasks()
      this._updateMetrics()
    }, 100)
  }

  stopScheduling() {
    if (this.schedulingInterval) {
      clearInterval(this.schedulingInterval)
      this.schedulingInterval = null
    }
  }

  _scheduleTasks() {
    const priorities = ['critical', 'high', 'normal', 'low']
    
    for (const priority of priorities) {
      while (this.priorityQueue[priority].length > 0) {
        const task = this.priorityQueue[priority][0]
        
        if (this._canStartTask(task)) {
          this._startTask(task)
          this.priorityQueue[priority].shift()
        } else {
          break
        }
      }
    }
  }

  _canStartTask(task) {
    const estimatedResources = this._estimateTaskResources(task)
    
    return this.resourcePool.availableCpu >= estimatedResources.cpu &&
           this.resourcePool.availableMemory >= estimatedResources.memory
  }

  _estimateTaskResources(task) {
    const complexityMultiplier = {
      low: 0.3,
      medium: 0.6,
      high: 1.0
    }
    
    return {
      cpu: 10 * complexityMultiplier[task.complexity],
      memory: 100 * complexityMultiplier[task.complexity]
    }
  }

  _startTask(task) {
    const resources = this._estimateTaskResources(task)
    
    this.resourcePool.availableCpu -= resources.cpu
    this.resourcePool.availableMemory -= resources.memory
    
    task.start()
    task.updateResourceUsage(resources.cpu, resources.memory, 0)
    this.runningTasks.set(task.taskId, task)
    
    this._emit('task-started', { taskId: task.taskId, modelId: task.modelId })
    
    this._simulateTaskExecution(task)
  }

  _simulateTaskExecution(task) {
    setTimeout(() => {
      this._completeTask(task)
    }, task.estimatedDuration)
  }

  _completeTask(task) {
    const resources = this._estimateTaskResources(task)
    
    this.resourcePool.availableCpu += resources.cpu
    this.resourcePool.availableMemory += resources.memory
    
    task.complete()
    this.runningTasks.delete(task.taskId)
    this.completedTasks.push(task)
    
    if (this.completedTasks.length > 500) {
      this.completedTasks = this.completedTasks.slice(-200)
    }
    
    this.metrics.completedTasks++
    this._recordEfficiency(task.getMetrics().efficiency)
    
    this._emit('task-completed', {
      taskId: task.taskId,
      modelId: task.modelId,
      efficiency: task.getMetrics().efficiency
    })
  }

  _recordEfficiency(efficiency) {
    if (efficiency !== null && !isNaN(efficiency)) {
      this._efficiencyHistory.push(efficiency)
      if (this._efficiencyHistory.length > this._maxHistorySize) {
        this._efficiencyHistory.shift()
      }
    }
  }

  _updateMetrics() {
    const total = this._efficiencyHistory.reduce((sum, val) => sum + val, 0)
    this.metrics.averageEfficiency = this._efficiencyHistory.length > 0 
      ? (total / this._efficiencyHistory.length).toFixed(2) 
      : 0
    
    const totalCpu = this.resourcePool.totalCpu
    const totalMemory = this.resourcePool.totalMemory
    this.metrics.resourceUtilization = {
      cpu: (((totalCpu - this.resourcePool.availableCpu) / totalCpu) * 100).toFixed(2),
      memory: (((totalMemory - this.resourcePool.availableMemory) / totalMemory) * 100).toFixed(2)
    }
  }

  cancelTask(taskId) {
    for (const priority of Object.keys(this.priorityQueue)) {
      const index = this.priorityQueue[priority].findIndex(t => t.taskId === taskId)
      if (index !== -1) {
        this.priorityQueue[priority].splice(index, 1)
        this._emit('task-canceled', { taskId })
        return true
      }
    }
    
    const runningTask = this.runningTasks.get(taskId)
    if (runningTask) {
      runningTask.fail('canceled')
      this.runningTasks.delete(taskId)
      this.metrics.failedTasks++
      this._emit('task-canceled', { taskId })
      return true
    }
    
    return false
  }

  getTaskStatus(taskId) {
    for (const priority of Object.keys(this.priorityQueue)) {
      const task = this.priorityQueue[priority].find(t => t.taskId === taskId)
      if (task) {
        return task.getMetrics()
      }
    }
    
    const runningTask = this.runningTasks.get(taskId)
    if (runningTask) {
      return runningTask.getMetrics()
    }
    
    const completedTask = this.completedTasks.find(t => t.taskId === taskId)
    if (completedTask) {
      return completedTask.getMetrics()
    }
    
    return null
  }

  getQueueStatus() {
    return {
      pending: {
        critical: this.priorityQueue.critical.length,
        high: this.priorityQueue.high.length,
        normal: this.priorityQueue.normal.length,
        low: this.priorityQueue.low.length
      },
      running: this.runningTasks.size,
      resources: {
        cpu: {
          total: this.resourcePool.totalCpu,
          available: this.resourcePool.availableCpu,
          used: this.resourcePool.totalCpu - this.resourcePool.availableCpu
        },
        memory: {
          total: this.resourcePool.totalMemory,
          available: this.resourcePool.availableMemory,
          used: this.resourcePool.totalMemory - this.resourcePool.availableMemory
        }
      }
    }
  }

  getMetrics() {
    return {
      ...this.metrics,
      taskDistribution: this.getQueueStatus().pending,
      resourceUtilization: this.metrics.resourceUtilization
    }
  }

  subscribe(eventName, callback) {
    this._eventBus = this._eventBus || new EventTarget()
    this._eventBus.addEventListener(eventName, callback)
  }

  unsubscribe(eventName, callback) {
    if (this._eventBus) {
      this._eventBus.removeEventListener(eventName, callback)
    }
  }

  _emit(eventName, detail) {
    if (this._eventBus) {
      this._eventBus.dispatchEvent(new CustomEvent(eventName, { detail }))
    }
  }

  destroy() {
    this.stopScheduling()
    for (const task of this.runningTasks.values()) {
      task.fail('system shutdown')
    }
    this.runningTasks.clear()
    this.taskQueue = []
    for (const priority of Object.keys(this.priorityQueue)) {
      this.priorityQueue[priority] = []
    }
  }
}

export const computeScheduler = new ComputeScheduler()

export { ComputeTask, ComputeScheduler }