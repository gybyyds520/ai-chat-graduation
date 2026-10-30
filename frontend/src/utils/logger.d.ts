declare module '@/utils/logger' {
  interface Logger {
    debug: (...args: unknown[]) => void
    info: (...args: unknown[]) => void
    warn: (...args: unknown[]) => void
    error: (...args: unknown[]) => void
    always: (...args: unknown[]) => void
    alwaysError: (...args: unknown[]) => void
  }

  export function createLogger(moduleName: string): Logger
  const logger: Logger
  export default logger
}