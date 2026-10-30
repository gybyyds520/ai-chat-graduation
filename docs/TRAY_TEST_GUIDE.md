# 系统托盘功能测试指南

## 测试环境

| 项目 | 说明 |
|------|------|
| 前端服务 | ✅ 运行中 (http://localhost:5173) |
| Electron | ⏳ 需要安装 |
| 测试类型 | 配置验证 + 功能说明 |

## 测试步骤

### 1. 验证托盘配置文件

**配置文件路径**:
```
%APPDATA%\AI Chat\tray-config.json
```

**默认配置结构**:
```json
{
  "icon": {
    "type": "default",
    "path": "",
    "emoji": "🤖",
    "size": 16,
    "color": "#4f46e5",
    "showBadge": true,
    "badgeColor": "#ff4d4f"
  },
  "behavior": {
    "singleClick": "show",
    "doubleClick": "show",
    "middleClick": "newChat",
    "showNotification": true,
    "flashOnMessage": true
  },
  "menu": {
    "showMainWindow": true,
    "showNewChat": true,
    "showDesktopElf": true,
    "showSettings": true,
    "showExit": true,
    "customItems": []
  }
}
```

### 2. 测试用例

#### 测试用例 1: 图标显示
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 启动应用 | 系统托盘显示默认图标 |
| 2 | 切换图标类型为 Emoji | 托盘图标变为设置的 Emoji |
| 3 | 设置自定义图标 | 托盘显示自定义图片 |
| 4 | 调整图标大小 | 图标尺寸相应变化 |

#### 测试用例 2: 右键菜单
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 右键点击托盘图标 | 显示上下文菜单 |
| 2 | 点击"显示主窗口" | 主窗口显示并聚焦 |
| 3 | 点击"新建对话" | 主窗口显示并新建对话 |
| 4 | 点击"桌面小精灵" | 打开桌面小精灵功能 |
| 5 | 点击"设置" | 打开设置界面 |
| 6 | 点击"退出" | 应用完全退出 |

#### 测试用例 3: 单击操作
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 设置单击为"显示" | 单击托盘显示主窗口 |
| 2 | 设置单击为"隐藏" | 单击托盘隐藏主窗口 |
| 3 | 设置单击为"切换" | 单击托盘切换窗口显示状态 |
| 4 | 设置单击为"菜单" | 单击托盘显示右键菜单 |

#### 测试用例 4: 双击操作
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 设置双击为"显示" | 双击托盘显示主窗口 |
| 2 | 设置双击为"隐藏" | 双击托盘隐藏主窗口 |
| 3 | 设置双击为"切换" | 双击托盘切换窗口显示状态 |

#### 测试用例 5: 消息通知
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 最小化窗口 | 显示托盘通知"应用已最小化到系统托盘" |
| 2 | 收到新消息 | 图标闪烁提醒 |
| 3 | 设置未读角标 | 托盘图标显示未读数量 |

#### 测试用例 6: 菜单自定义
| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 取消勾选"显示主窗口" | 菜单中移除该选项 |
| 2 | 取消勾选"退出" | 菜单中移除退出选项 |
| 3 | 恢复所有选项 | 菜单恢复完整 |

### 3. API 测试

**在浏览器控制台执行以下测试**:

```javascript
// 获取托盘配置
const config = window.electronAPI.getTrayConfig()
console.log('当前托盘配置:', config)

// 更新图标为 Emoji
window.electronAPI.updateTrayConfig({
  icon: { type: 'emoji', emoji: '💬' }
})

// 更新单击行为为切换
window.electronAPI.updateTrayConfig({
  behavior: { singleClick: 'toggle' }
})

// 更新未读角标
window.electronAPI.updateTrayBadge(3)

// 测试闪烁功能
window.electronAPI.startTrayFlash()
setTimeout(() => {
  window.electronAPI.stopTrayFlash()
}, 5000)

// 显示通知
window.electronAPI.showTrayNotification('测试通知', '这是一条测试消息')
```

### 4. 配置验证

**验证增强版托盘管理器逻辑**:

```javascript
// 测试配置加载
const EnhancedTray = require('./electron/enhancedTray')
const TrayConfig = require('./electron/trayConfig')

// 创建配置实例
const config = new TrayConfig()
console.log('默认配置:', config.getConfig())

// 更新配置
config.updateIconConfig({ type: 'emoji', emoji: '🚀' })
console.log('更新后配置:', config.getConfig())

// 验证配置保存
config.saveConfig()
console.log('配置已保存')
```

## 测试结果预期

### 功能验证表

| 功能模块 | 状态 | 说明 |
|----------|------|------|
| 图标显示 | ✅ | 支持默认/Emoji/自定义三种类型 |
| 右键菜单 | ✅ | 支持5个标准菜单项 + 自定义项 |
| 单击操作 | ✅ | 支持4种操作模式 |
| 双击操作 | ✅ | 支持3种操作模式 |
| 滚轮点击 | ✅ | 支持新建对话等操作 |
| 消息通知 | ✅ | 支持气泡通知和图标闪烁 |
| 角标显示 | ✅ | 支持未读数量显示 |
| 配置持久化 | ✅ | 自动保存到用户数据目录 |

### 性能指标

| 指标 | 目标值 |
|------|--------|
| 托盘创建时间 | < 100ms |
| 菜单响应时间 | < 50ms |
| 内存占用 | < 10MB |
| CPU占用 | < 1% |

## 安装 Electron（当网络可用时）

```powershell
# 设置镜像
npm config set ELECTRON_MIRROR https://npmmirror.com/mirrors/electron/

# 安装 Electron
npm install electron --save-dev

# 启动开发环境
npm run electron:dev
```

## 故障排除

### 问题 1: Electron 安装失败

**症状**:
```
Error: Electron failed to install correctly
```

**解决方案**:
```powershell
# 清除缓存
npm cache clean --force

# 设置镜像
npm config set registry https://registry.npmmirror.com
npm config set ELECTRON_MIRROR https://npmmirror.com/mirrors/electron/

# 重新安装
rm -rf node_modules/electron
npm install electron
```

### 问题 2: 托盘图标不显示

**症状**: 托盘区域没有显示应用图标

**解决方案**:
```powershell
# 检查图标文件是否存在
Test-Path "electron/icon.ico"

# 确保图标格式正确
# 推荐尺寸: 64x64, 格式: ICO 或 PNG
```

### 问题 3: 菜单不响应

**症状**: 右键点击托盘图标没有反应

**解决方案**:
```javascript
// 检查主进程日志
console.log('托盘菜单是否创建:', tray !== null)

// 验证菜单模板
const menu = Menu.buildFromTemplate(menuTemplate)
console.log('菜单项数量:', menu.items.length)
```

---

*文档版本: 1.0.0*
*最后更新: 2026-06-22*