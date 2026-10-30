import { alarmManager } from '../src/utils/alarmManager'

describe('AlarmManager', () => {
  beforeEach(() => {
    alarmManager.alarms = []
    alarmManager.timers.clear()
    localStorage.removeItem('desktop-elf-alarms')
  })

  describe('基本功能', () => {
    it('应该能够创建闹钟', () => {
      const alarm = alarmManager.createAlarm({
        time: '08:00',
        label: '测试闹钟',
        repeat: 'daily'
      })
      
      expect(alarm).toBeDefined()
      expect(alarm.id).toBeDefined()
      expect(alarm.time).toBe('08:00')
      expect(alarm.label).toBe('测试闹钟')
      expect(alarm.repeat).toBe('daily')
      expect(alarm.enabled).toBe(true)
    })

    it('应该能够获取所有闹钟', () => {
      alarmManager.createAlarm({ time: '08:00' })
      alarmManager.createAlarm({ time: '09:00' })
      
      const alarms = alarmManager.getAlarms()
      expect(alarms.length).toBe(2)
    })

    it('应该能够根据ID获取闹钟', () => {
      const alarm = alarmManager.createAlarm({ time: '08:00' })
      const found = alarmManager.getAlarmById(alarm.id)
      
      expect(found).toBeDefined()
      expect(found.id).toBe(alarm.id)
    })

    it('应该能够更新闹钟', () => {
      const alarm = alarmManager.createAlarm({ time: '08:00', label: '旧标签' })
      const updated = alarmManager.updateAlarm(alarm.id, {
        time: '09:00',
        label: '新标签'
      })
      
      expect(updated.time).toBe('09:00')
      expect(updated.label).toBe('新标签')
    })

    it('应该能够删除闹钟', () => {
      const alarm = alarmManager.createAlarm({ time: '08:00' })
      const result = alarmManager.deleteAlarm(alarm.id)
      
      expect(result).toBe(true)
      expect(alarmManager.getAlarmById(alarm.id)).toBeUndefined()
    })

    it('删除不存在的闹钟应该返回false', () => {
      const result = alarmManager.deleteAlarm('nonexistent-id')
      expect(result).toBe(false)
    })

    it('应该能够切换闹钟启用状态', () => {
      const alarm = alarmManager.createAlarm({ time: '08:00' })
      expect(alarm.enabled).toBe(true)
      
      alarmManager.toggleAlarm(alarm.id)
      expect(alarm.enabled).toBe(false)
      
      alarmManager.toggleAlarm(alarm.id)
      expect(alarm.enabled).toBe(true)
    })
  })

  describe('重复模式', () => {
    it('应该支持once模式', () => {
      const alarm = alarmManager.createAlarm({
        time: '08:00',
        repeat: 'once'
      })
      expect(alarm.repeat).toBe('once')
    })

    it('应该支持daily模式', () => {
      const alarm = alarmManager.createAlarm({
        time: '08:00',
        repeat: 'daily'
      })
      expect(alarm.repeat).toBe('daily')
    })

    it('应该支持weekdays模式', () => {
      const alarm = alarmManager.createAlarm({
        time: '08:00',
        repeat: 'weekdays'
      })
      expect(alarm.repeat).toBe('weekdays')
    })

    it('应该支持weekend模式', () => {
      const alarm = alarmManager.createAlarm({
        time: '08:00',
        repeat: 'weekend'
      })
      expect(alarm.repeat).toBe('weekend')
    })

    it('应该支持custom模式', () => {
      const alarm = alarmManager.createAlarm({
        time: '08:00',
        repeat: 'custom',
        repeatDays: ['mon', 'wed', 'fri']
      })
      expect(alarm.repeat).toBe('custom')
      expect(alarm.repeatDays).toEqual(['mon', 'wed', 'fri'])
    })
  })

  describe('贪睡功能', () => {
    it('应该支持贪睡功能', () => {
      const alarm = alarmManager.createAlarm({
        time: '08:00',
        snoozeEnabled: true,
        snoozeInterval: 5
      })
      
      expect(alarm.snoozeEnabled).toBe(true)
      expect(alarm.snoozeInterval).toBe(5)
    })

    it('应该能够执行贪睡', () => {
      const alarm = alarmManager.createAlarm({
        time: '08:00',
        snoozeEnabled: true,
        snoozeInterval: 5
      })
      
      const snoozeUntil = alarmManager.snoozeAlarm(alarm)
      expect(snoozeUntil).toBeDefined()
      
      const expectedTime = new Date(Date.now() + 5 * 60 * 1000)
      expect(snoozeUntil.getTime()).toBeCloseTo(expectedTime.getTime(), -3)
    })

    it('禁用贪睡时应该返回null', () => {
      const alarm = alarmManager.createAlarm({
        time: '08:00',
        snoozeEnabled: false
      })
      
      const snoozeUntil = alarmManager.snoozeAlarm(alarm)
      expect(snoozeUntil).toBeNull()
    })
  })

  describe('时间计算', () => {
    it('应该能够计算下一次闹钟时间', () => {
      const alarm = alarmManager.createAlarm({
        time: '23:59',
        repeat: 'daily'
      })
      
      const nextTime = alarmManager.getNextAlarmTime(alarm)
      expect(nextTime).toBeDefined()
      expect(nextTime instanceof Date).toBe(true)
    })

    it('once模式下应该在时间已过时返回null', () => {
      // 固定系统时间，避免凌晨跨午夜时 (now-1h) 构造出"未来的今天时刻"导致断言失效
      vi.useFakeTimers()
      vi.setSystemTime(new Date(2026, 0, 15, 12, 0, 0))
      try {
        const alarm = alarmManager.createAlarm({
          time: '11:00',
          repeat: 'once'
        })

        const nextTime = alarmManager.getNextAlarmTime(alarm)
        expect(nextTime).toBeNull()
      } finally {
        vi.useRealTimers()
      }
    })
  })

  describe('铃声管理', () => {
    it('应该能够获取可用铃声列表', () => {
      const ringtones = alarmManager.getAvailableRingtones()
      expect(ringtones.length).toBeGreaterThan(0)
      expect(ringtones[0].id).toBeDefined()
      expect(ringtones[0].name).toBeDefined()
    })

    it('应该支持经典铃声', () => {
      const ringtones = alarmManager.getAvailableRingtones()
      const classic = ringtones.find(r => r.id === 'classic')
      expect(classic).toBeDefined()
      expect(classic.name).toBe('经典闹钟')
    })
  })

  describe('本地存储', () => {
    it('应该能够保存闹钟到本地存储', () => {
      alarmManager.createAlarm({ time: '08:00' })
      
      const saved = localStorage.getItem('desktop-elf-alarms')
      expect(saved).toBeDefined()
      
      const parsed = JSON.parse(saved)
      expect(parsed.length).toBe(1)
    })

    it('应该能够从本地存储加载闹钟', () => {
      const alarms = [{
        id: 'test-id',
        time: '08:00',
        enabled: true,
        repeat: 'daily'
      }]
      localStorage.setItem('desktop-elf-alarms', JSON.stringify(alarms))
      
      alarmManager.loadAlarms()
      const loaded = alarmManager.getAlarms()
      
      expect(loaded.length).toBe(1)
      expect(loaded[0].id).toBe('test-id')
    })
  })

  describe('事件订阅', () => {
    it('应该支持事件订阅', () => {
      const listener = vi.fn()
      const unsubscribe = alarmManager.subscribe(listener)
      
      alarmManager.createAlarm({ time: '08:00' })
      
      expect(listener).toHaveBeenCalled()
      unsubscribe()
    })

    it('取消订阅后不应该再收到事件', () => {
      const listener = vi.fn()
      const unsubscribe = alarmManager.subscribe(listener)
      
      alarmManager.createAlarm({ time: '08:00' })
      unsubscribe()
      alarmManager.createAlarm({ time: '09:00' })
      
      expect(listener).toHaveBeenCalledTimes(1)
    })
  })
})