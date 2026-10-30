import logger from './logger'

class TestDataCleaner {
  constructor() {
    this.cleanupTargets = [
      { name: 'localStorage', type: 'storage', cleanup: this.cleanLocalStorage.bind(this) },
      { name: 'sessionStorage', type: 'storage', cleanup: this.cleanSessionStorage.bind(this) },
      { name: 'IndexedDB', type: 'database', cleanup: this.cleanIndexedDB.bind(this) },
      { name: 'CacheStorage', type: 'cache', cleanup: this.cleanCacheStorage.bind(this) },
      { name: 'Cookies', type: 'cookie', cleanup: this.cleanCookies.bind(this) },
      { name: 'window session', type: 'memory', cleanup: this.cleanWindowSession.bind(this) }
    ]
  }

  async cleanAllTestData() {
    logger.info('[Cleaner] 开始清理测试数据')
    
    const results = []
    
    for (const target of this.cleanupTargets) {
      try {
        await target.cleanup()
        results.push({ name: target.name, status: 'success', error: null })
        logger.info(`[Cleaner] ✓ ${target.name} 清理成功`)
      } catch (e) {
        results.push({ name: target.name, status: 'failed', error: e.message })
        logger.warn(`[Cleaner] ✗ ${target.name} 清理失败: ${e.message}`)
      }
    }
    
    this.generateCleanupReport(results)
    return results
  }

  cleanLocalStorage() {
    const testKeys = []
    
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && this.isTestKey(key)) {
        testKeys.push(key)
      }
    }
    
    testKeys.forEach(key => {
      localStorage.removeItem(key)
    })
    
    logger.debug(`[Cleaner] localStorage: 删除 ${testKeys.length} 个测试键`)
  }

  cleanSessionStorage() {
    const testKeys = []
    
    for (let i = 0; i < sessionStorage.length; i++) {
      const key = sessionStorage.key(i)
      if (key && this.isTestKey(key)) {
        testKeys.push(key)
      }
    }
    
    testKeys.forEach(key => {
      sessionStorage.removeItem(key)
    })
    
    logger.debug(`[Cleaner] sessionStorage: 删除 ${testKeys.length} 个测试键`)
  }

  async cleanIndexedDB() {
    return new Promise((resolve) => {
      const request = indexedDB.databases()
      
      request.onsuccess = async () => {
        const databases = request.result || []
        const testDatabases = databases.filter(db => this.isTestName(db.name))
        
        for (const db of testDatabases) {
          indexedDB.deleteDatabase(db.name)
        }
        
        logger.debug(`[Cleaner] IndexedDB: 删除 ${testDatabases.length} 个测试数据库`)
        resolve()
      }
      
      request.onerror = () => {
        logger.debug('[Cleaner] IndexedDB: 无法访问')
        resolve()
      }
    })
  }

  async cleanCacheStorage() {
    const cacheNames = await caches.keys()
    const testCaches = cacheNames.filter(name => this.isTestName(name))
    
    for (const cacheName of testCaches) {
      await caches.delete(cacheName)
    }
    
    logger.debug(`[Cleaner] CacheStorage: 删除 ${testCaches.length} 个测试缓存`)
  }

  cleanCookies() {
    const cookies = document.cookie.split(';')
    const testCookies = []
    
    cookies.forEach(cookie => {
      const [name] = cookie.split('=').map(s => s.trim())
      if (name && this.isTestName(name)) {
        testCookies.push(name)
        document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`
      }
    })
    
    logger.debug(`[Cleaner] Cookies: 删除 ${testCookies.length} 个测试Cookie`)
  }

  cleanWindowSession() {
    if (window.__modelInstances) {
      window.__modelInstances.clear()
    }
    if (window.__computeScheduler) {
      window.__computeScheduler.destroy()
      window.__computeScheduler = null
    }
    
    logger.debug('[Cleaner] Window session: 清理完成')
  }

  isTestKey(key) {
    const testPatterns = [
      /^test_/,
      /^mock_/,
      /^debug_/,
      /^temp_/,
      /^sandbox_/,
      /^validator_/,
      /^integration_/,
      /^share_test_/,
      /^chat_history_export_/
    ]
    
    return testPatterns.some(pattern => pattern.test(key))
  }

  isTestName(name) {
    const testPatterns = [
      /^test/,
      /^mock/,
      /^debug/,
      /^temp/,
      /^sandbox/
    ]
    
    return testPatterns.some(pattern => pattern.test(name.toLowerCase()))
  }

  generateCleanupReport(results) {
    const success = results.filter(r => r.status === 'success').length
    const failed = results.filter(r => r.status === 'failed').length
    
    logger.info('[Cleaner] ===== 清理报告 =====')
    logger.info(`时间: ${new Date().toISOString()}`)
    logger.info(`成功: ${success}`)
    logger.info(`失败: ${failed}`)
    logger.info('[Cleaner] ===================')
  }

  getCleanupTargets() {
    return this.cleanupTargets.map(t => t.name)
  }
}

export const testDataCleaner = new TestDataCleaner()

export { TestDataCleaner }