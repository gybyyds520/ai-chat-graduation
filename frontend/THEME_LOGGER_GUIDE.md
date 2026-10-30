# 主题切换日志记录功能使用说明

## 概述

主题切换日志记录功能用于追踪和验证主题切换过程中 CSS 变量的变化，确保所有页面在模式切换时能够实现整体、一致的视觉变化。

## 功能特性

### 1. 统一的日志工具 (`themeLogger.js`)

提供完整的日志记录功能，包括：

- **日志级别**：DEBUG、INFO、WARN、ERROR
- **日志格式**：统一的时间戳、组件名、操作类型
- **变量追踪**：自动记录所有主题相关 CSS 变量的值
- **变化对比**：对比切换前后的变量变化
- **性能监控**：记录主题切换耗时

### 2. 关键日志记录点

#### 组件挂载时
```javascript
logThemeOnMounted(componentName)
```
记录组件初始化时的主题状态和所有 CSS 变量值。

#### 主题切换触发时
```javascript
logThemeSwitchTrigger(componentName, triggerSource)
```
记录主题切换的触发来源（如 localStorage_change）。

#### 主题切换前
```javascript
const beforeVariables = logThemeBefore(componentName)
```
记录切换前的主题状态和所有 CSS 变量值。

#### 主题切换后
```javascript
logThemeAfter(componentName, beforeVariables)
```
记录切换后的主题状态，并与切换前进行对比，显示变化的变量。

#### 主题切换完成
```javascript
logThemeSwitchComplete(componentName, duration)
```
记录主题切换完成，包括总耗时。

### 3. 主题日志查看器 (`ThemeLogViewer.vue`)

提供可视化的日志查看界面：

- **日志筛选**：按级别、组件、操作类型筛选
- **日志统计**：显示各级别日志数量
- **日志详情**：查看每条日志的完整信息
- **变量对比**：直观显示 CSS 变量的变化
- **导出功能**：将日志导出为 JSON 文件
- **清除功能**：清空历史日志记录

## 集成方式

### 1. 导入日志工具

```javascript
import {
  logThemeOnMounted,
  logThemeSwitchTrigger,
  logThemeBefore,
  logThemeAfter,
  logThemeSwitchComplete
} from '@/utils/themeLogger'
```

### 2. 添加主题切换监听

```javascript
// 定义组件名称
const componentName = 'YourComponentName'

// 主题切换相关变量
let themeSwitchStartTime = null
let beforeThemeVariables = null

// 监听主题切换开始
const handleThemeSwitchStart = () => {
  themeSwitchStartTime = Date.now()
  beforeThemeVariables = logThemeBefore(componentName)
  logThemeSwitchTrigger(componentName, 'localStorage_change')
}

// 监听主题切换结束
const handleThemeSwitchEnd = () => {
  if (themeSwitchStartTime && beforeThemeVariables) {
    const duration = Date.now() - themeSwitchStartTime
    logThemeAfter(componentName, beforeThemeVariables)
    logThemeSwitchComplete(componentName, duration)
    themeSwitchStartTime = null
    beforeThemeVariables = null
  }
}

// 监听 localStorage 变化
const storageListener = (event) => {
  if (event.key === 'theme') {
    handleThemeSwitchStart()
    // 延迟记录切换后的状态，确保 DOM 已更新
    setTimeout(handleThemeSwitchEnd, 350)
  }
}
```

### 3. 在生命周期钩子中集成

```javascript
import { onMounted, onUnmounted } from 'vue'

onMounted(() => {
  // 记录组件挂载时的主题状态
  logThemeOnMounted(componentName)
  
  // 添加 localStorage 监听器
  window.addEventListener('storage', storageListener)
  
  // 其他初始化代码...
})

onUnmounted(() => {
  // 移除 localStorage 监听器
  window.removeEventListener('storage', storageListener)
})
```

### 4. 添加日志查看器

```vue
<template>
  <div>
    <!-- 你的组件内容 -->
    
    <!-- 主题日志查看器 -->
    <ThemeLogViewer v-model="logViewerVisible" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ThemeLogViewer from '@/components/ThemeLogViewer.vue'

const logViewerVisible = ref(false)
</script>
```

## 日志输出示例

### 控制台输出

```
[ThemeLogger] [DEBUG] [FeedView] THEME_SWITCH_BEFORE {
  theme: 'light',
  variables: {
    'bg-primary': '#ffffff',
    'text-primary': '#0f172a',
    'color-primary': '#3b82f6',
    // ... 更多变量
  },
  timestamp: 1704067200000
}

[ThemeLogger] [INFO] [FeedView] THEME_SWITCH_AFTER {
  theme: 'dark',
  variables: {
    'bg-primary': '#0f172a',
    'text-primary': '#f8fafc',
    'color-primary': '#60a5fa',
    // ... 更多变量
  },
  changes: {
    'bg-primary': { before: '#ffffff', after: '#0f172a' },
    'text-primary': { before: '#0f172a', after: '#f8fafc' },
    // ... 更多变化
  },
  changedCount: 18,
  unchangedCount: 2,
  timestamp: 1704067200350
}

[ThemeLogger] [INFO] [FeedView] THEME_SWITCH_COMPLETE {
  theme: 'dark',
  variables: { /* ... */ },
  duration: '350ms',
  timestamp: 1704067200350
}
```

### 导出的 JSON 文件

```json
{
  "exportTime": "2024-01-01T00:00:00.000Z",
  "totalLogs": 15,
  "logs": [
    {
      "timestamp": "2024-01-01T00:00:00.000Z",
      "level": "INFO",
      "component": "FeedView",
      "action": "COMPONENT_MOUNTED",
      "theme": "light",
      "variables": { /* ... */ }
    },
    {
      "timestamp": "2024-01-01T00:01:00.000Z",
      "level": "DEBUG",
      "component": "FeedView",
      "action": "THEME_SWITCH_TRIGGER",
      "currentTheme": "light",
      "triggerSource": "localStorage_change"
    },
    // ... 更多日志
  ]
}
```

## 已集成的组件

### 1. FeedView.vue
- 位置：`frontend/src/views/FeedView.vue`
- 日志按钮：页面头部右侧的"主题日志"按钮
- 监听范围：整个 Feed 投喂中心

### 2. AdminView.vue
- 位置：`frontend/src/views/AdminView.vue`
- 日志按钮：侧边栏头部的"主题日志"按钮
- 监听范围：整个管理员后台

## 性能考虑

1. **日志历史限制**：默认保留最近 100 条日志
2. **异步记录**：日志记录不会阻塞主线程
3. **按需导出**：仅在用户主动点击时导出日志
4. **内存管理**：自动清理旧日志记录

## 故障排查

### 日志未显示

1. 检查是否正确导入 `themeLogger.js`
2. 确认组件名称设置正确
3. 查看浏览器控制台是否有错误信息

### 变量值未更新

1. 检查 CSS 变量是否正确定义在 `theme.css` 中
2. 确认主题类正确应用到 `html` 根元素
3. 验证 `getComputedStyle()` 能正确获取变量值

### 性能问题

1. 减少日志历史记录数量（修改 `maxHistory` 配置）
2. 在生产环境中禁用详细日志（使用 `setLoggingEnabled(false)`）
3. 调整日志级别（使用 `setLogLevel('WARN')`）

## API 参考

### 主要函数

| 函数名 | 参数 | 返回值 | 说明 |
|--------|------|--------|------|
| `logThemeOnMounted` | `componentName` | `Object` | 记录组件挂载时的主题状态 |
| `logThemeSwitchTrigger` | `componentName, triggerSource` | `void` | 记录主题切换触发事件 |
| `logThemeBefore` | `componentName` | `Object` | 记录主题切换前的状态 |
| `logThemeAfter` | `componentName, beforeVariables` | `Object` | 记录主题切换后的状态 |
| `logThemeSwitchComplete` | `componentName, duration` | `void` | 记录主题切换完成 |
| `getThemeVariables` | 无 | `Object` | 获取所有主题 CSS 变量值 |
| `compareThemeVariables` | `before, after` | `Object` | 比较两组变量值的差异 |
| `getLogHistory` | 无 | `Array` | 获取日志历史记录 |
| `clearLogHistory` | 无 | `void` | 清除日志历史记录 |
| `exportLogsAsJSON` | 无 | `void` | 导出日志为 JSON 文件 |
| `setLoggingEnabled` | `enabled` | `void` | 启用或禁用日志记录 |
| `setLogLevel` | `level` | `void` | 设置日志级别 |

## 最佳实践

1. **组件命名**：使用清晰、唯一的组件名称
2. **触发源标识**：准确记录主题切换的触发来源
3. **性能监控**：关注主题切换耗时，优化用户体验
4. **定期清理**：定期导出并清理日志历史
5. **生产环境**：在生产环境中使用更严格的日志级别

## 总结

主题切换日志记录功能提供了完整的主题切换追踪能力，帮助开发者：

- 验证主题切换的正确性
- 定位主题相关的问题
- 优化主题切换性能
- 分析用户主题使用习惯

通过统一的日志格式和可视化的查看工具，可以轻松追踪主题切换的全过程，确保应用的主题切换功能稳定可靠。