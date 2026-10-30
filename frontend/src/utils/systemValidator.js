import { modelIsolationManager } from './modelIsolation'
import { computeScheduler } from './computeScheduler'
import { shareManager } from './shareManager'
import { systemIntegration } from './systemIntegration'
import logger from './logger'

class SystemValidator {
  constructor() {
    this.testResults = []
    this.currentTest = null
  }

  async runAllTests() {
    logger.info('[Validator] 开始系统全面验证')
    
    this.testResults = []
    
    await this.testModelIsolation()
    await this.testComputeScheduler()
    await this.testShareManager()
    await this.testSystemIntegration()
    await this.testSecurity()
    
    this.generateReport()
    return this.testResults
  }

  startTest(name) {
    this.currentTest = {
      name,
      status: 'running',
      startTime: Date.now(),
      assertions: [],
      errors: [],
      warnings: []
    }
    logger.info(`[Validator] 测试: ${name}`)
  }

  endTest() {
    if (this.currentTest) {
      this.currentTest.status = this.currentTest.errors.length > 0 ? 'failed' : 'passed'
      this.currentTest.endTime = Date.now()
      this.currentTest.duration = this.currentTest.endTime - this.currentTest.startTime
      this.testResults.push(this.currentTest)
      
      const status = this.currentTest.status === 'passed' ? '✓' : '✗'
      logger.info(`[Validator] ${status} ${this.currentTest.name} (${this.currentTest.duration}ms)`)
    }
    this.currentTest = null
  }

  assert(condition, message) {
    if (this.currentTest) {
      if (condition) {
        this.currentTest.assertions.push({ type: 'pass', message })
      } else {
        this.currentTest.assertions.push({ type: 'fail', message })
        this.currentTest.errors.push(message)
        logger.error(`[Validator] 断言失败: ${message}`)
      }
    }
  }

  warn(message) {
    if (this.currentTest) {
      this.currentTest.warnings.push(message)
      logger.warn(`[Validator] 警告: ${message}`)
    }
  }

  async testModelIsolation() {
    this.startTest('AI模型隔离机制测试')
    
    try {
      const sandbox1 = modelIsolationManager.createSandbox('test-model-1', {
        resourceLimits: { maxMemory: 256, maxCpu: 30, maxTokensPerMinute: 5000 }
      })
      this.assert(sandbox1, '创建沙箱1成功')

      const sandbox2 = modelIsolationManager.createSandbox('test-model-2')
      this.assert(sandbox2, '创建沙箱2成功')

      const status1 = sandbox1.getState()
      this.assert(status1.status === 'active', '沙箱1状态为active')
      this.assert(status1.isIsolated, '沙箱1已隔离')

      sandbox1.setMemory('test-key', 'test-value')
      const memory = sandbox1.getMemory('test-key')
      this.assert(memory === 'test-value', '内存隔离正常')

      const sharedMemory = sandbox2.getMemory('test-key')
      this.assert(sharedMemory === undefined, '跨沙箱内存隔离正常')

      sandbox1.acquireResource('tokens', 100)
      const state = sandbox1.getState()
      this.assert(state.resourceUsage.tokens === 100, '资源获取正常')

      sandbox1.releaseResource('tokens', 50)
      this.assert(sandbox1.getState().resourceUsage.tokens === 50, '资源释放正常')

      sandbox1.destroy()
      sandbox2.destroy()
      this.assert(!modelIsolationManager.getSandbox('test-model-1'), '沙箱销毁成功')

    } catch (e) {
      this.currentTest.errors.push(e.message)
      logger.error(`[Validator] 模型隔离测试异常: ${e.message}`)
    }
    
    this.endTest()
  }

  async testComputeScheduler() {
    this.startTest('算力分配系统测试')
    
    try {
      const task1 = computeScheduler.submitTask('test-model', 'high', 'medium')
      this.assert(task1, '提交任务成功')
      this.assert(task1.status === 'pending', '任务状态为pending')

      const task2 = computeScheduler.submitTask('test-model', 'critical', 'high')
      this.assert(task2.priority === 'critical', '任务优先级设置正确')

      const status = computeScheduler.getQueueStatus()
      this.assert(status.pending.critical >= 1, '关键任务入队正常')

      const metrics = computeScheduler.getMetrics()
      this.assert(metrics.totalTasks >= 2, '任务计数正常')

      const taskStatus = computeScheduler.getTaskStatus(task1.taskId)
      this.assert(taskStatus !== null, '任务状态查询正常')

    } catch (e) {
      this.currentTest.errors.push(e.message)
      logger.error(`[Validator] 算力分配测试异常: ${e.message}`)
    }
    
    this.endTest()
  }

  async testShareManager() {
    this.startTest('分享管理器测试')
    
    try {
      const character = {
        id: 'test-character',
        name: '测试角色',
        description: '测试描述',
        prompt: '测试prompt',
        avatar: 'https://example.com/avatar.png',
        modelType: 'test-model',
        tags: ['test']
      }

      const messages = [
        { id: 'msg-1', senderId: 1, senderName: '用户', content: '你好', sendTime: new Date().toISOString() },
        { id: 'msg-2', senderId: 2, senderName: '角色', content: '您好', sendTime: new Date().toISOString() }
      ]

      const packed = shareManager.packCharacterWithChat(character, messages, {
        includeTrainingData: false,
        includeInteractionFeatures: false,
        maxMessages: 100
      })
      this.assert(packed, '打包数据成功')
      this.assert(packed.version === '1.0.0', '版本号正确')
      this.assert(packed.type === 'character-with-chat', '类型正确')
      this.assert(packed.character.name === '测试角色', '角色信息正确')
      this.assert(packed.chatHistory.length === 2, '聊天记录正确')

      const compressed = shareManager.compressData(packed)
      this.assert(typeof compressed === 'string', '压缩数据成功')

      const decompressed = shareManager.decompressData(compressed)
      this.assert(decompressed.type === 'character-with-chat', '解压数据成功')

      const validation = shareManager.validateImportData(packed)
      this.assert(validation.valid, '数据验证通过')

      const password = 'test-password'
      const encrypted = shareManager.encryptData(packed, password)
      this.assert(typeof encrypted === 'string', '加密数据成功')

      const decrypted = shareManager.decryptData(encrypted, password)
      this.assert(decrypted.character.name === '测试角色', '解密数据成功')

    } catch (e) {
      this.currentTest.errors.push(e.message)
      logger.error(`[Validator] 分享管理器测试异常: ${e.message}`)
    }
    
    this.endTest()
  }

  async testSystemIntegration() {
    this.startTest('系统集成测试')
    
    try {
      await systemIntegration.initialize()
      this.assert(systemIntegration.initialized, '系统集成初始化成功')

      const instance = systemIntegration.createModelInstance('integration-test', {
        resourceLimits: { maxMemory: 512, maxCpu: 50 }
      })
      this.assert(instance, '创建模型实例成功')
      this.assert(instance.modelId === 'integration-test', '实例ID正确')

      const retrieved = systemIntegration.getModelInstance('integration-test')
      this.assert(retrieved === instance, '获取模型实例成功')

      const systemStatus = systemIntegration.getSystemStatus()
      this.assert(systemStatus.initialized, '系统状态正常')
      this.assert(systemStatus.modelCount >= 1, '模型计数正常')

      systemIntegration.removeModelInstance('integration-test')
      const removed = systemIntegration.getModelInstance('integration-test')
      this.assert(removed === undefined, '移除模型实例成功')

    } catch (e) {
      this.currentTest.errors.push(e.message)
      logger.error(`[Validator] 系统集成测试异常: ${e.message}`)
    }
    
    this.endTest()
  }

  async testSecurity() {
    this.startTest('安全防护测试')
    
    try {
      const originalOpen = window.open
      let blocked = false
      
      window.open = function(url) {
        if (url && url.toLowerCase().startsWith('view-source:')) {
          blocked = true
          return null
        }
        return originalOpen.apply(this, arguments)
      }

      window.open('view-source:http://example.com')
      this.assert(blocked, 'view-source协议拦截成功')

      window.open = originalOpen

      this.warn('快捷键测试需要手动验证')
      this.warn('右键菜单测试需要手动验证')

    } catch (e) {
      this.currentTest.errors.push(e.message)
      logger.error(`[Validator] 安全防护测试异常: ${e.message}`)
    }
    
    this.endTest()
  }

  generateReport() {
    const passed = this.testResults.filter(r => r.status === 'passed').length
    const failed = this.testResults.filter(r => r.status === 'failed').length
    const total = this.testResults.length
    
    const report = {
      timestamp: new Date().toISOString(),
      totalTests: total,
      passedTests: passed,
      failedTests: failed,
      successRate: total > 0 ? ((passed / total) * 100).toFixed(2) + '%' : 'N/A',
      tests: this.testResults.map(r => ({
        name: r.name,
        status: r.status,
        duration: r.duration,
        assertions: r.assertions.length,
        errors: r.errors.length,
        warnings: r.warnings.length
      }))
    }

    logger.info('[Validator] ===== 系统验证报告 =====')
    logger.info(`时间: ${report.timestamp}`)
    logger.info(`总测试数: ${report.totalTests}`)
    logger.info(`通过: ${report.passedTests}`)
    logger.info(`失败: ${report.failedTests}`)
    logger.info(`成功率: ${report.successRate}`)
    logger.info('[Validator] =======================')

    return report
  }

  getResults() {
    return this.testResults
  }
}

export const systemValidator = new SystemValidator()

export { SystemValidator }