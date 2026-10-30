# 主题切换模块使用文档

## 概述

这是一个完整的白天/黑夜模式切换功能模块，支持主题切换、用户偏好记忆、平滑过渡等功能。

## 功能特性

✅ 直观的切换控件（开关按钮）
✅ 白天（亮色）和黑夜（暗色）两种主题样式
✅ 用户偏好记忆功能（localStorage持久化）
✅ 平滑无闪烁的主题切换
✅ 兼容主流浏览器及响应式设计
✅ 支持系统主题偏好检测
✅ 完整的CSS变量系统
✅ Element Plus组件库适配

## 文件结构

```
frontend/src/
├── composables/
│   └── useTheme.ts          # 主题切换组合式函数
├── components/
│   └── ThemeToggle.vue      # 主题切换组件
├── styles/
│   └── theme.css            # 主题CSS变量定义
├── App.vue                  # 应用入口（已集成主题初始化）
└── main.js                  # 主入口（已导入主题样式）
```

## 快速开始

### 1. 基本使用

在任何Vue组件中使用主题切换功能：

```vue
<template>
  <div>
    <!-- 使用预制的主题切换组件 -->
    <ThemeToggle />
    
    <!-- 或者自定义按钮 -->
    <button @click="toggleTheme">
      {{ isDark() ? '切换到白天' : '切换到黑夜' }}
    </button>
  </div>
</template>

<script setup>
import { useTheme } from '@/composables/useTheme'
import ThemeToggle from '@/components/ThemeToggle.vue'

const { toggleTheme, isDark, currentTheme } = useTheme()
</script>
```

### 2. 使用CSS变量

在样式中使用主题变量：

```vue
<template>
  <div class="my-component">
    <h1>标题</h1>
    <p>内容</p>
    <button class="btn">按钮</button>
  </div>
</template>

<style scoped>
.my-component {
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border-primary);
  padding: 20px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

h1 {
  color: var(--text-primary);
}

p {
  color: var(--text-secondary);
}

.btn {
  background: var(--color-primary);
  color: var(--text-inverse);
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.btn:hover {
  background: var(--color-primary-hover);
}
</style>
```

## API文档

### useTheme() 组合式函数

#### 返回值

| 属性/方法 | 类型 | 说明 |
|----------|------|------|
| `currentTheme` | `Ref<ThemeMode>` | 当前主题（'light' 或 'dark'） |
| `toggleTheme()` | `() => void` | 切换主题 |
| `setTheme(theme)` | `(theme: ThemeMode) => void` | 设置指定主题 |
| `initTheme()` | `() => void` | 初始化主题（通常在onMounted中调用） |
| `isDark()` | `() => boolean` | 检查是否为暗色主题 |
| `isLight()` | `() => boolean` | 检查是否为亮色主题 |
| `getSystemTheme()` | `() => ThemeMode` | 获取系统主题偏好 |

#### 使用示例

```javascript
import { useTheme } from '@/composables/useTheme'

export default {
  setup() {
    const { 
      currentTheme, 
      toggleTheme, 
      setTheme, 
      isDark,
      isLight 
    } = useTheme()
    
    // 切换主题
    const handleToggle = () => {
      toggleTheme()
    }
    
    // 设置指定主题
    const setDarkMode = () => {
      setTheme('dark')
    }
    
    // 检查当前主题
    if (isDark()) {
      console.log('当前是暗色主题')
    }
    
    return {
      currentTheme,
      handleToggle,
      setDarkMode
    }
  }
}
```

### ThemeToggle 组件

#### 属性

无

#### 事件

无

#### 使用示例

```vue
<template>
  <div>
    <ThemeToggle />
  </div>
</template>

<script setup>
import ThemeToggle from '@/components/ThemeToggle.vue'
</script>
```

## CSS变量列表

### 颜色变量

#### 背景色
- `--bg-primary`: 主背景色
- `--bg-secondary`: 次级背景色
- `--bg-tertiary`: 三级背景色
- `--bg-hover`: 悬停背景色
- `--bg-active`: 激活背景色

#### 文字颜色
- `--text-primary`: 主文字颜色
- `--text-secondary`: 次级文字颜色
- `--text-tertiary`: 三级文字颜色
- `--text-muted`: 弱化文字颜色
- `--text-inverse`: 反色文字颜色

#### 边框颜色
- `--border-primary`: 主边框颜色
- `--border-secondary`: 次级边框颜色
- `--border-focus`: 焦点边框颜色

#### 品牌色
- `--color-primary`: 主品牌色
- `--color-primary-hover`: 主品牌色悬停
- `--color-primary-active`: 主品牌色激活
- `--color-primary-light`: 主品牌色浅色

#### 功能色
- `--color-success`: 成功色
- `--color-warning`: 警告色
- `--color-error`: 错误色
- `--color-info`: 信息色

### 组件变量

- `--card-bg`: 卡片背景色
- `--card-border`: 卡片边框色
- `--input-bg`: 输入框背景色
- `--input-border`: 输入框边框色
- `--navbar-bg`: 导航栏背景色
- `--sidebar-bg`: 侧边栏背景色

## 高级用法

### 1. 监听主题变化

```vue
<script setup>
import { watch } from 'vue'
import { useTheme } from '@/composables/useTheme'

const { currentTheme } = useTheme()

watch(currentTheme, (newTheme) => {
  console.log('主题已切换为:', newTheme)
  // 执行其他逻辑...
})
</script>
```

### 2. 条件渲染

```vue
<template>
  <div>
    <img v-if="isDark()" src="/dark-logo.png" alt="暗色Logo" />
    <img v-else src="/light-logo.png" alt="亮色Logo" />
  </div>
</template>

<script setup>
import { useTheme } from '@/composables/useTheme'

const { isDark } = useTheme()
</script>
```

### 3. 动态样式绑定

```vue
<template>
  <div :style="{ 
    backgroundColor: isDark() ? '#1e293b' : '#ffffff',
    color: isDark() ? '#f8fafc' : '#0f172a'
  }">
    动态样式内容
  </div>
</template>

<script setup>
import { useTheme } from '@/composables/useTheme'

const { isDark } = useTheme()
</script>
```

## 浏览器兼容性

- Chrome >= 80
- Firefox >= 75
- Safari >= 13
- Edge >= 80

## 测试指南

### 功能测试清单

#### 1. 主题切换测试
- [ ] 点击切换按钮，主题应立即切换
- [ ] 切换过程平滑无闪烁（300ms过渡动画）
- [ ] 所有页面元素应统一转换为新主题

#### 2. 持久化测试
- [ ] 切换主题后刷新页面，应保持上次选择的主题
- [ ] 关闭浏览器后重新打开，应保持上次选择的主题
- [ ] 检查localStorage中`app-theme-mode`键是否正确存储

#### 3. 路由一致性测试
- [ ] 在聊天页面切换主题
- [ ] 导航到其他页面（好友管理、投喂等）
- [ ] 确认所有页面都应用了相同的主题

#### 4. 组件测试
- [ ] 弹窗/对话框：确认背景和文字颜色正确
- [ ] 表格：确认表头和数据行颜色正确
- [ ] 输入框：确认背景、边框和文字颜色正确
- [ ] 按钮：确认各种状态颜色正确
- [ ] 消息提示：确认颜色正确

#### 5. 动态内容测试
- [ ] 滚动加载的内容应应用当前主题
- [ ] 异步加载的数据应应用当前主题
- [ ] 新创建的DOM元素应应用当前主题

#### 6. 响应式测试
- [ ] 在桌面端切换主题正常
- [ ] 在平板端切换主题正常
- [ ] 在移动端切换主题正常

#### 7. 系统主题检测测试
- [ ] 删除localStorage中的`app-theme-mode`键
- [ ] 刷新页面，应跟随系统主题偏好
- [ ] 改变系统主题设置，页面应自动跟随

### 手动测试步骤

```bash
# 1. 启动前端开发服务器
npm run dev

# 2. 打开浏览器开发者工具
# - 检查Application -> Local Storage中是否有app-theme-mode键
# - 检查Elements -> html标签是否有dark/light类和data-theme属性

# 3. 测试场景
# a) 点击主题切换按钮
# b) 刷新页面验证持久化
# c) 导航到不同页面验证一致性
# d) 打开弹窗验证组件适配
```

### 常见问题排查

#### 问题1：主题切换后某些元素颜色不变
**原因**：这些元素使用了硬编码颜色值，没有使用CSS变量

**解决**：将硬编码颜色替换为对应的CSS变量

```css
/* 错误示例 */
.element {
  color: #1e293b; /* 硬编码颜色 */
}

/* 正确示例 */
.element {
  color: var(--text-primary); /* 使用CSS变量 */
}
```

#### 问题2：路由切换后主题丢失
**原因**：路由切换时没有重新应用主题

**解决**：确保在路由守卫中重新应用主题（已在router/index.js中实现）

#### 问题3：Element Plus组件颜色不正确
**原因**：Element Plus组件有自己的样式变量

**解决**：在theme.css中添加对应的暗色模式样式（已实现）

## 注意事项

1. **初始化时机**: 确保在应用挂载时调用 `initTheme()`
2. **CSS变量**: 使用CSS变量时确保在根元素或父元素上定义了变量
3. **Element Plus**: 已自动适配Element Plus组件库
4. **性能**: 主题切换使用CSS transition实现，性能优异
5. **持久化**: 用户偏好自动保存到localStorage
6. **硬编码颜色**: 避免使用硬编码颜色值，统一使用CSS变量

## 示例项目

查看 `ChatView.vue` 文件获取完整的集成示例。

## 更新日志

### v1.0.0 (2026-06-03)
- 初始版本发布
- 支持白天/黑夜模式切换
- 支持用户偏好记忆
- 支持系统主题检测
- 完整的CSS变量系统
- Element Plus组件库适配
