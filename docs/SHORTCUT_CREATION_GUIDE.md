# AI Chat 桌面快捷方式创建指南

## 概述

本指南帮助您为 AI Chat 应用创建 Windows 桌面快捷方式，使其可以通过直接点击图标启动，且不显示命令行窗口。

## 快速开始

### 方法 1：一键构建和部署（推荐）

运行以下命令将自动完成所有步骤：

```powershell
cd frontend
node scripts/build-deploy.js
```

这将自动：
1. ✅ 生成应用图标
2. ✅ 构建前端
3. ✅ 构建 Electron 应用
4. ✅ 创建桌面快捷方式

### 方法 2：分步操作

#### 步骤 1：生成图标

```powershell
cd frontend
powershell -ExecutionPolicy Bypass -File build/generate-icon-simple.ps1
```

#### 步骤 2：构建前端

```powershell
npm run build
```

#### 步骤 3：构建 Electron

```powershell
npm run electron:build
```

#### 步骤 4：创建快捷方式

```powershell
# 创建所有快捷方式（桌面、开始菜单、快速启动）
node scripts/create-shortcut.js -all

# 仅创建桌面快捷方式
node scripts/create-shortcut.js -desktop

# 仅创建开始菜单快捷方式
node scripts/create-shortcut.js -startmenu

# 仅创建快速启动栏快捷方式
node scripts/create-shortcut.js -quicklaunch
```

## 手动创建快捷方式

### 使用 PowerShell 脚本

```powershell
powershell -ExecutionPolicy Bypass -File build/create-shortcut.ps1 -All
```

### 手动创建（不推荐）

1. 右键桌面空白区域
2. 选择"新建" → "快捷方式"
3. 输入目标路径：
   ```
   C:\Users\[用户名]\AppData\Local\Programs\AI Chat\AI Chat.exe
   ```
4. 点击"下一步"
5. 输入快捷方式名称："AI Chat"
6. 点击"完成"

## 快捷方式配置

### 属性说明

| 属性 | 值 | 说明 |
|------|-----|------|
| 目标位置 | `AI Chat.exe` 完整路径 | 应用程序入口 |
| 起始位置 | 应用程序目录 | 工作目录 |
| 图标 | `icon.ico` | 应用图标 |
| 运行方式 | 最大化窗口 | 启动时窗口状态 |

### 高级配置

编辑 `create-shortcut.ps1` 脚本可以自定义：

```powershell
param(
    [string]$ExePath = "",        # 可执行文件路径
    [string]$ShortcutName = "AI Chat",  # 快捷方式名称
    [string]$WorkingDir = "",     # 工作目录
    [string]$IconPath = "",       # 图标路径
    [switch]$Desktop,             # 创建桌面快捷方式
    [switch]$StartMenu,           # 创建开始菜单快捷方式
    [switch]$QuickLaunch,         # 创建快速启动栏快捷方式
    [switch]$All                  # 创建所有快捷方式
)
```

## 文件清单

### 生成的文件

```
frontend/
├── build/
│   ├── icon.ico                   # 应用图标 (256x256, 128x128, 64x64, 48x48, 32x32, 16x16)
│   ├── generate-icon.ps1          # 图标生成脚本
│   ├── create-shortcut.ps1        # PowerShell 快捷方式创建脚本
│   └── build-and-deploy.ps1      # 一键构建部署脚本
└── scripts/
    ├── create-shortcut.js         # Node.js 快捷方式创建脚本
    └── build-deploy.js            # Node.js 一键构建部署脚本
```

### 快捷方式位置

| 位置 | 路径 |
|------|------|
| 桌面 | `C:\Users\[用户名]\Desktop\AI Chat.lnk` |
| 开始菜单 | `C:\Users\[用户名]\AppData\Roaming\Microsoft\Windows\Start Menu\Programs\AI Chat.lnk` |
| 快速启动 | `C:\Users\[用户名]\AppData\Roaming\Microsoft\Internet Explorer\Quick Launch\AI Chat.lnk` |

## 故障排除

### 问题 1：快捷方式创建失败

**症状**：脚本执行报错

**解决方案**：
1. 确保以管理员权限运行 PowerShell
2. 检查杀毒软件是否阻止脚本执行
3. 手动创建快捷方式

### 问题 2：图标不显示

**症状**：快捷方式显示默认图标

**解决方案**：
1. 确认 `icon.ico` 文件存在
2. 重新生成图标：`powershell -ExecutionPolicy Bypass -File build/generate-icon-simple.ps1`
3. 删除旧快捷方式并重新创建

### 问题 3：启动时出现命令行窗口

**解决方案**：
1. 确保快捷方式指向 `.exe` 文件，不是批处理文件
2. 检查是否使用了 `electron.cmd` 而非 `electron.exe`

### 问题 4：应用程序无法启动

**症状**：双击快捷方式无反应

**解决方案**：
1. 检查 exe 文件是否存在
2. 确认已安装所有依赖（VC++ Redistributable）
3. 查看 Windows 事件日志

## 性能优化

### 自动启动配置

将快捷方式复制到以下目录可实现开机自启：

```
C:\Users\[用户名]\AppData\Roaming\Microsoft\Windows\Start Menu\Programs\Startup\
```

### 减少内存占用

编辑快捷方式，添加启动参数：

```
"C:\path\to\AI Chat.exe" --disable-gpu --no-sandbox
```

## 卸载

### 删除快捷方式

1. 桌面：右键 "AI Chat" → "删除"
2. 开始菜单：右键 "AI Chat" → "从列表中删除"
3. 快速启动：从快速启动栏拖出删除

### 完全卸载

1. 删除所有快捷方式
2. 删除安装目录
3. 删除用户数据：`%APPDATA%\ai-chat-frontend`

## 技术支持

如遇问题，请检查：
- 系统日志：`eventvwr.msc` → Windows 日志 → 应用程序
- 应用日志：`%APPDATA%\ai-chat-frontend\logs\`

---

*文档版本: 1.0.0*
*最后更新: 2026-06-22*