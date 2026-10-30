const isProduction = process.env.NODE_ENV === 'production'
const LOG_LEVEL = isProduction ? 'warn' : 'debug'

const LOG_LEVELS = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3
}

function shouldLog(level) {
  return LOG_LEVELS[level] >= LOG_LEVELS[LOG_LEVEL]
}

const baseLogger = {
  debug: (...args) => {
    if (shouldLog('debug')) {
      console.debug('[DEBUG]', ...args)
    }
  },
  info: (...args) => {
    if (shouldLog('info')) {
      console.info('[INFO]', ...args)
    }
  },
  warn: (...args) => {
    if (shouldLog('warn')) {
      console.warn('[WARN]', ...args)
    }
  },
  error: (...args) => {
    if (shouldLog('error')) {
      console.error('[ERROR]', ...args)
    }
  },
  always: (...args) => {
    console.log(...args)
  },
  alwaysError: (...args) => {
    console.error(...args)
  },
  logToServer: (level, ...args) => {
    if (isProduction) {
      try {
        const payload = {
          level,
          message: args.map(arg => 
            typeof arg === 'object' ? JSON.stringify(arg) : String(arg)
          ).join(' '),
          timestamp: Date.now(),
          environment: isProduction ? 'production' : 'development'
        }
        
        if (navigator.sendBeacon) {
          navigator.sendBeacon('/api/log', JSON.stringify(payload))
        } else {
          fetch('/api/log', {
            method: 'POST',
            body: JSON.stringify(payload),
            headers: { 'Content-Type': 'application/json' },
            keepalive: true
          }).catch(() => {})
        }
      } catch (e) {
        console.error('Failed to send log to server:', e)
      }
    }
  }
}

export function createLogger(moduleName) {
  return {
    debug: (...args) => {
      if (shouldLog('debug')) {
        console.debug(`[DEBUG] [${moduleName}]`, ...args)
      }
    },
    info: (...args) => {
      if (shouldLog('info')) {
        console.info(`[INFO] [${moduleName}]`, ...args)
      }
    },
    warn: (...args) => {
      if (shouldLog('warn')) {
        console.warn(`[WARN] [${moduleName}]`, ...args)
      }
      baseLogger.logToServer('warn', `[${moduleName}]`, ...args)
    },
    error: (...args) => {
      if (shouldLog('error')) {
        console.error(`[ERROR] [${moduleName}]`, ...args)
      }
      baseLogger.logToServer('error', `[${moduleName}]`, ...args)
    },
    always: (...args) => {
      console.log(`[${moduleName}]`, ...args)
    },
    alwaysError: (...args) => {
      console.error(`[${moduleName}]`, ...args)
      baseLogger.logToServer('error', `[${moduleName}]`, ...args)
    }
  }
}

export default baseLogger