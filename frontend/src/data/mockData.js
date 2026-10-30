// 桌面小精灵模拟数据

// 用户配置数据
export const mockUserConfig = {
  id: 'user-001',
  username: '张三',
  theme: 'dark',
  accentColor: '#6366f1',
  autoSave: true,
  saveInterval: 30000,
  language: 'zh-CN'
}

// 小部件模板数据
export const mockWidgetTemplates = [
  {
    type: 'clock',
    name: '时钟',
    description: '显示当前时间和日期',
    icon: 'clock',
    defaultSize: { width: 200, height: 120 },
    defaultPosition: { x: 50, y: 50 },
    configurable: true
  },
  {
    type: 'weather',
    name: '天气',
    description: '显示当前天气信息',
    icon: 'cloud',
    defaultSize: { width: 240, height: 150 },
    defaultPosition: { x: 300, y: 50 },
    configurable: true
  },
  {
    type: 'calendar',
    name: '日历',
    description: '显示月历和日程',
    icon: 'calendar',
    defaultSize: { width: 280, height: 240 },
    defaultPosition: { x: 580, y: 50 },
    configurable: true
  },
  {
    type: 'notes',
    name: '便签',
    description: '记录和管理便签',
    icon: 'file-text',
    defaultSize: { width: 260, height: 200 },
    defaultPosition: { x: 50, y: 200 },
    configurable: true
  },
  {
    type: 'systemMonitor',
    name: '系统监控',
    description: '监控系统资源使用',
    icon: 'activity',
    defaultSize: { width: 300, height: 180 },
    defaultPosition: { x: 350, y: 200 },
    configurable: true
  }
]

// 预设小部件配置
export const mockWidgets = [
  {
    id: 'widget-clock-001',
    type: 'clock',
    name: '数字时钟',
    position: { x: 100, y: 100 },
    size: { width: 220, height: 140 },
    zIndex: 1,
    visible: true,
    transparency: 0.95,
    config: {
      displayMode: 'digital',
      showDate: true,
      showSeconds: true,
      timeFormat: '24h',
      fontSize: 24,
      fontFamily: 'Arial',
      textColor: '#ffffff',
      backgroundColor: 'rgba(30, 41, 59, 0.8)'
    }
  },
  {
    id: 'widget-weather-001',
    type: 'weather',
    name: '天气预报',
    position: { x: 350, y: 100 },
    size: { width: 260, height: 160 },
    zIndex: 2,
    visible: true,
    transparency: 0.9,
    config: {
      city: '北京',
      temperature: 26,
      humidity: 65,
      windSpeed: 12,
      weatherType: 'sunny',
      forecast: [
        { day: '周一', temp: '24-32°C', icon: 'sunny' },
        { day: '周二', temp: '23-30°C', icon: 'cloudy' },
        { day: '周三', temp: '22-28°C', icon: 'rainy' },
        { day: '周四', temp: '25-33°C', icon: 'sunny' },
        { day: '周五', temp: '24-31°C', icon: 'cloudy' }
      ]
    }
  },
  {
    id: 'widget-calendar-001',
    type: 'calendar',
    name: '日程日历',
    position: { x: 650, y: 100 },
    size: { width: 300, height: 260 },
    zIndex: 3,
    visible: true,
    transparency: 0.95,
    config: {
      currentDate: new Date().toISOString().split('T')[0],
      showWeekends: true,
      highlightToday: true,
      events: [
        { date: new Date().toISOString().split('T')[0], title: '团队会议', time: '14:00', color: '#ef4444' },
        { date: new Date(Date.now() + 86400000).toISOString().split('T')[0], title: '产品发布', time: '10:00', color: '#22c55e' },
        { date: new Date(Date.now() + 172800000).toISOString().split('T')[0], title: '客户拜访', time: '09:00', color: '#3b82f6' }
      ]
    }
  },
  {
    id: 'widget-notes-001',
    type: 'notes',
    name: '待办事项',
    position: { x: 100, y: 280 },
    size: { width: 280, height: 220 },
    zIndex: 4,
    visible: true,
    transparency: 0.9,
    config: {
      notes: [
        { id: 'note-1', content: '完成项目文档', completed: false, color: '#fef08a' },
        { id: 'note-2', content: '代码审查会议', completed: false, color: '#bbf7d0' },
        { id: 'note-3', content: '更新依赖版本', completed: true, color: '#bfdbfe' },
        { id: 'note-4', content: '准备演示材料', completed: false, color: '#fecaca' }
      ],
      defaultColor: '#fef08a'
    }
  },
  {
    id: 'widget-system-001',
    type: 'systemMonitor',
    name: '系统状态',
    position: { x: 420, y: 280 },
    size: { width: 320, height: 200 },
    zIndex: 5,
    visible: true,
    transparency: 0.95,
    config: {
      updateInterval: 2000,
      showCpu: true,
      showMemory: true,
      showNetwork: true,
      showDisk: true
    }
  }
]

// 系统资源模拟数据（模拟系统监控小部件使用）
export const mockSystemResources = {
  cpu: {
    usage: 42,
    cores: 8,
    model: 'Intel(R) Core(TM) i7-10700K',
    frequency: '3.80 GHz'
  },
  memory: {
    total: 16384,
    used: 8542,
    available: 7842,
    usage: 52
  },
  disk: {
    total: 1000,
    used: 456,
    available: 544,
    usage: 46
  },
  network: {
    upload: 1245,
    download: 8934,
    uploadSpeed: '1.2 MB/s',
    downloadSpeed: '8.7 MB/s'
  },
  battery: {
    level: 78,
    status: 'charging',
    estimatedTime: '2小时30分'
  },
  uptime: '4小时23分'
}

// 用户交互事件模拟数据
export const mockUserEvents = [
  { id: 'evt-001', type: 'widget-create', widgetId: 'widget-clock-001', timestamp: Date.now() - 3600000, user: '张三' },
  { id: 'evt-002', type: 'widget-update', widgetId: 'widget-clock-001', timestamp: Date.now() - 3500000, user: '张三', changes: { fontSize: 24 } },
  { id: 'evt-003', type: 'widget-create', widgetId: 'widget-weather-001', timestamp: Date.now() - 3400000, user: '张三' },
  { id: 'evt-004', type: 'widget-move', widgetId: 'widget-clock-001', timestamp: Date.now() - 3300000, user: '张三', position: { x: 100, y: 100 } },
  { id: 'evt-005', type: 'widget-resize', widgetId: 'widget-weather-001', timestamp: Date.now() - 3200000, user: '张三', size: { width: 260, height: 160 } },
  { id: 'evt-006', type: 'widget-delete', widgetId: 'widget-old-001', timestamp: Date.now() - 3100000, user: '张三' },
  { id: 'evt-007', type: 'config-save', timestamp: Date.now() - 3000000, user: '张三' },
  { id: 'evt-008', type: 'config-export', timestamp: Date.now() - 2900000, user: '张三', filename: 'widget-config-20240101.json' },
  { id: 'evt-009', type: 'widget-create', widgetId: 'widget-calendar-001', timestamp: Date.now() - 2800000, user: '张三' },
  { id: 'evt-010', type: 'widget-create', widgetId: 'widget-notes-001', timestamp: Date.now() - 2700000, user: '张三' },
  { id: 'evt-011', type: 'note-add', widgetId: 'widget-notes-001', timestamp: Date.now() - 2600000, user: '张三', content: '完成项目文档' },
  { id: 'evt-012', type: 'note-toggle', widgetId: 'widget-notes-001', timestamp: Date.now() - 2500000, user: '张三', noteId: 'note-3' },
  { id: 'evt-013', type: 'widget-create', widgetId: 'widget-system-001', timestamp: Date.now() - 2400000, user: '张三' },
  { id: 'evt-014', type: 'theme-change', timestamp: Date.now() - 2300000, user: '张三', theme: 'dark' },
  { id: 'evt-015', type: 'widget-create', timestamp: Date.now() - 2200000, user: '张三' }
]

// 天气模拟数据
export const mockWeatherData = {
  cities: [
    { id: '1', name: '北京', country: '中国', lat: 39.9042, lng: 116.4074 },
    { id: '2', name: '上海', country: '中国', lat: 31.2304, lng: 121.4737 },
    { id: '3', name: '广州', country: '中国', lat: 23.1291, lng: 113.2644 },
    { id: '4', name: '深圳', country: '中国', lat: 22.5431, lng: 114.0579 },
    { id: '5', name: '杭州', country: '中国', lat: 30.2741, lng: 120.1552 },
    { id: '6', name: '成都', country: '中国', lat: 30.5728, lng: 104.0668 }
  ],
  weatherTypes: ['sunny', 'cloudy', 'rainy', 'stormy', 'snowy', 'foggy'],
  getWeatherByCity: (cityName) => {
    const weatherTypes = ['sunny', 'cloudy', 'rainy', 'stormy', 'snowy', 'foggy']
    const randomType = weatherTypes[Math.floor(Math.random() * weatherTypes.length)]
    const temperatures = {
      '北京': { min: 15, max: 32 },
      '上海': { min: 20, max: 35 },
      '广州': { min: 25, max: 38 },
      '深圳': { min: 25, max: 37 },
      '杭州': { min: 18, max: 33 },
      '成都': { min: 16, max: 30 }
    }
    const tempRange = temperatures[cityName] || { min: 15, max: 35 }
    const temperature = Math.floor(Math.random() * (tempRange.max - tempRange.min)) + tempRange.min
    
    return {
      city: cityName,
      temperature,
      humidity: Math.floor(Math.random() * 50) + 40,
      windSpeed: Math.floor(Math.random() * 20) + 5,
      weatherType: randomType,
      feelsLike: temperature + (Math.random() > 0.5 ? 2 : -2),
      forecast: Array.from({ length: 5 }, (_, i) => ({
        day: ['周一', '周二', '周三', '周四', '周五'][i],
        temp: `${Math.floor(Math.random() * 10) + 20}-${Math.floor(Math.random() * 10) + 30}°C`,
        icon: weatherTypes[Math.floor(Math.random() * weatherTypes.length)]
      }))
    }
  }
}

// 日历事件模拟数据
export const mockCalendarEvents = [
  {
    id: 'event-001',
    title: '团队周会',
    description: '每周项目进度汇报',
    date: new Date().toISOString().split('T')[0],
    time: '10:00',
    duration: '1小时',
    color: '#ef4444',
    location: '会议室A',
    participants: ['张三', '李四', '王五']
  },
  {
    id: 'event-002',
    title: '产品发布准备',
    description: '新版本发布前检查',
    date: new Date(Date.now() + 86400000).toISOString().split('T')[0],
    time: '14:00',
    duration: '2小时',
    color: '#22c55e',
    location: '大会议室',
    participants: ['张三', '赵六']
  },
  {
    id: 'event-003',
    title: '客户演示',
    description: '向客户展示新产品功能',
    date: new Date(Date.now() + 172800000).toISOString().split('T')[0],
    time: '09:30',
    duration: '3小时',
    color: '#3b82f6',
    location: '客户公司',
    participants: ['张三', '钱七']
  },
  {
    id: 'event-004',
    title: '代码审查',
    description: '审查新功能代码',
    date: new Date(Date.now() + 259200000).toISOString().split('T')[0],
    time: '15:00',
    duration: '1.5小时',
    color: '#f59e0b',
    location: '远程会议',
    participants: ['张三', '孙八']
  },
  {
    id: 'event-005',
    title: '团建活动',
    description: '团队聚餐',
    date: new Date(Date.now() + 345600000).toISOString().split('T')[0],
    time: '18:00',
    duration: '3小时',
    color: '#ec4899',
    location: '餐厅',
    participants: ['张三', '李四', '王五', '赵六', '钱七', '孙八']
  }
]

// 便签模拟数据
export const mockNotes = [
  { id: 'note-001', content: '完成项目文档编写', completed: false, color: '#fef08a', createdAt: Date.now() - 86400000 },
  { id: 'note-002', content: '准备下周演示材料', completed: false, color: '#bbf7d0', createdAt: Date.now() - 72000000 },
  { id: 'note-003', content: '更新依赖版本', completed: true, color: '#bfdbfe', createdAt: Date.now() - 57600000 },
  { id: 'note-004', content: '修复登录页面bug', completed: true, color: '#fecaca', createdAt: Date.now() - 43200000 },
  { id: 'note-005', content: '设计数据库表结构', completed: false, color: '#e9d5ff', createdAt: Date.now() - 28800000 },
  { id: 'note-006', content: '编写单元测试', completed: false, color: '#fed7aa', createdAt: Date.now() - 14400000 }
]

// 配置导入导出模板
export const mockExportConfig = {
  version: '1.0.0',
  exportTime: new Date().toISOString(),
  user: mockUserConfig,
  widgets: mockWidgets,
  settings: {
    autoSave: true,
    saveInterval: 30000,
    theme: 'dark',
    accentColor: '#6366f1'
  }
}

// 组件状态变化模拟
export const mockStateChanges = [
  { timestamp: Date.now(), component: 'ClockWidget', state: 'initialized', details: { mode: 'digital' } },
  { timestamp: Date.now() + 100, component: 'ClockWidget', state: 'rendered', details: { width: 220, height: 140 } },
  { timestamp: Date.now() + 200, component: 'WeatherWidget', state: 'initialized', details: { city: '北京' } },
  { timestamp: Date.now() + 300, component: 'WeatherWidget', state: 'dataLoaded', details: { temperature: 26 } },
  { timestamp: Date.now() + 400, component: 'CalendarWidget', state: 'initialized', details: { month: 6 } },
  { timestamp: Date.now() + 500, component: 'CalendarWidget', state: 'eventsLoaded', details: { count: 3 } },
  { timestamp: Date.now() + 600, component: 'NotesWidget', state: 'initialized', details: { noteCount: 4 } },
  { timestamp: Date.now() + 700, component: 'NotesWidget', state: 'rendered', details: { activeNote: null } },
  { timestamp: Date.now() + 800, component: 'SystemMonitorWidget', state: 'initialized', details: { interval: 2000 } },
  { timestamp: Date.now() + 900, component: 'SystemMonitorWidget', state: 'monitoring', details: { cpu: 42, memory: 52 } }
]

export default {
  mockUserConfig,
  mockWidgetTemplates,
  mockWidgets,
  mockSystemResources,
  mockUserEvents,
  mockWeatherData,
  mockCalendarEvents,
  mockNotes,
  mockExportConfig,
  mockStateChanges
}
