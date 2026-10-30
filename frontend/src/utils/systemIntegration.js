import { modelIsolationManager } from './modelIsolation'
import { computeScheduler } from './computeScheduler'
import { shareManager } from './shareManager'
import logger from './logger'

class SystemIntegration {
  constructor() {
    this.initialized = false
    this.modelInstances = new Map()
  }

  async initialize() {
    if (this.initialized) return

    logger.info('[System] 初始化系统集成')

    this._setupEventListeners()
    this._setupModelIntegration()
    this._setupComputeIntegration()

    this.initialized = true
    logger.info('[System] 系统集成初始化完成')
  }

  _setupEventListeners() {
    modelIsolationManager.subscribe('isolation-alert', (e) => {
      const detail = e.detail
      logger.warn(`[System] 隔离告警: ${detail.type} - 模型: ${detail.modelId}`, detail.detail)
    })

    modelIsolationManager.subscribe('isolation-status-update', (e) => {
      const detail = e.detail
      logger.info(`[System] 隔离状态更新: 总数=${detail.total}, 问题数=${detail.problematic}`)
    })

    computeScheduler.subscribe('task-submitted', (e) => {
      logger.debug(`[System] 任务提交: ${e.detail.taskId}`)
    })

    computeScheduler.subscribe('task-completed', (e) => {
      logger.debug(`[System] 任务完成: ${e.detail.taskId}, 效率: ${e.detail.efficiency}%`)
    })

    computeScheduler.subscribe('task-failed', (e) => {
      logger.error(`[System] 任务失败: ${e.detail.taskId}`)
    })
  }

  _setupModelIntegration() {
    if (!window.__modelInstances) {
      window.__modelInstances = new Map()
    }
    this.modelInstances = window.__modelInstances
  }

  _setupComputeIntegration() {
    if (!window.__computeScheduler) {
      window.__computeScheduler = computeScheduler
    }
  }

  createModelInstance(modelId, config = {}) {
    const sandbox = modelIsolationManager.createSandbox(modelId, config)
    
    const instance = {
      modelId,
      config,
      sandbox,
      taskQueue: [],
      isRunning: false,
      lastTaskTime: 0
    }

    this.modelInstances.set(modelId, instance)
    logger.info(`[System] 创建模型实例: ${modelId}`)

    return instance
  }

  getModelInstance(modelId) {
    return this.modelInstances.get(modelId)
  }

  removeModelInstance(modelId) {
    modelIsolationManager.destroySandbox(modelId)
    this.modelInstances.delete(modelId)
    logger.info(`[System] 移除模型实例: ${modelId}`)
  }

  submitModelTask(modelId, taskData, priority = 'normal', complexity = 'medium') {
    const instance = this.modelInstances.get(modelId)
    
    if (!instance) {
      logger.error(`[System] 模型实例不存在: ${modelId}`)
      throw new Error(`Model instance not found: ${modelId}`)
    }

    const task = computeScheduler.submitTask(modelId, priority, complexity)
    
    task.onComplete = (result) => {
      instance.lastTaskTime = Date.now()
      logger.debug(`[System] 模型任务完成: ${modelId}, 任务: ${task.taskId}`)
    }

    instance.taskQueue.push(task)
    return task
  }

  getSystemStatus() {
    return {
      initialized: this.initialized,
      modelCount: this.modelInstances.size,
      isolationStatus: modelIsolationManager.getAllIsolationStatus(),
      computeStatus: computeScheduler.getQueueStatus(),
      computeMetrics: computeScheduler.getMetrics()
    }
  }

  shutdown() {
    logger.info('[System] 系统关闭中...')
    
    for (const modelId of this.modelInstances.keys()) {
      this.removeModelInstance(modelId)
    }
    
    computeScheduler.destroy()
    modelIsolationManager.destroy()
    
    this.initialized = false
    logger.info('[System] 系统已关闭')
  }
}

export const systemIntegration = new SystemIntegration()

export { SystemIntegration }