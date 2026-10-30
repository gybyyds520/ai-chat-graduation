# AI Chat 小程序界面 - 本地开发环境配置指南

## 目录
1. [环境要求](#环境要求)
2. [开发工具安装](#开发工具安装)
3. [依赖项配置](#依赖项配置)
4. [环境变量设置](#环境变量设置)
5. [启动步骤](#启动步骤)
6. [功能测试指南](#功能测试指南)
7. [常见问题排查](#常见问题排查)

---

## 环境要求

### 必需软件

| 软件 | 版本要求 | 用途 |
|------|---------|------|
| Node.js | >= 18.0.0 | JavaScript运行环境 |
| npm | >= 9.0.0 | 包管理器 |
| Java JDK | >= 17 | 后端运行环境 |
| Maven | >= 3.8.0 | Java项目构建工具 |
| Git | >= 2.30.0 | 版本控制 |

### 可选软件

| 软件 | 用途 |
|------|------|
| VS Code | 代码编辑器 |
| Electron | 桌面应用打包 |

---

## 开发工具安装

### 1. Node.js 安装

**Windows:**
```powershell
# 方式1: 使用官方安装包
# 下载地址: https://nodejs.org/
# 选择 LTS 版本下载安装

# 方式2: 使用 Chocolatey (推荐)
choco install nodejs-lts

# 验证安装
node --version
npm --version
```

**配置 npm 镜像源 (加速下载):**
```powershell
# 设置淘宝镜像
npm config set registry https://registry.npmmirror.com

# 设置 Electron 镜像
npm config set ELECTRON_MIRROR https://npmmirror.com/mirrors/electron/

# 验证配置
npm config list
```

### 2. Java JDK 安装

**Windows:**
```powershell
# 方式1: 使用官方安装包
# 下载地址: https://adoptium.net/
# 选择 JDK 25 LTS 版本

# 方式2: 使用 Chocolatey
choco install temurin25

# 验证安装
java -version
javac -version
```

**配置环境变量:**
```powershell
# 设置 JAVA_HOME (PowerShell 管理员模式)
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-25-hotspot", "Machine")

# 添加到 PATH
[Environment]::SetEnvironmentVariable("Path", "$env:Path;%JAVA_HOME%\bin", "Machine")
```

### 3. Maven 安装

**Windows:**
```powershell
# 方式1: 使用 Chocolatey
choco install maven

# 方式2: 手动安装
# 下载地址: https://maven.apache.org/download.cgi
# 解压到 C:\Program Files\Apache\maven

# 配置环境变量
[Environment]::SetEnvironmentVariable("MAVEN_HOME", "C:\Program Files\Apache\maven", "Machine")
[Environment]::SetEnvironmentVariable("Path", "$env:Path;%MAVEN_HOME%\bin", "Machine")

# 验证安装
mvn -version
```

**配置 Maven 镜像 (加速下载):**
编辑 `%MAVEN_HOME%\conf\settings.xml` 或 `%USERPROFILE%\.m2\settings.xml`:
```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

---

## 依赖项配置

### 1. 前端依赖安装

```powershell
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 如果遇到网络问题，使用以下命令
npm install --registry=https://registry.npmmirror.com
```

### 2. 后端依赖安装

```powershell
# 进入后端目录
cd backend/aichat

# 下载 Maven 依赖
mvn dependency:resolve

# 编译项目
mvn compile
```

### 3. 项目依赖清单

**前端核心依赖:**
```json
{
  "vue": "^3.4.21",
  "vue-router": "^4.3.0",
  "element-plus": "^2.6.1",
  "axios": "^1.6.7",
  "echarts": "^6.1.0",
  "three": "^0.184.0",
  "@mediapipe/hands": "^0.4.1675469240"
}
```

**后端核心依赖:**
```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
  </dependency>
  <dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
  </dependency>
</dependencies>
```

---

## 环境变量设置

### 创建环境配置文件

**前端环境变量 (`.env.development`):**
```env
# API 配置
VITE_API_URL=http://localhost:8080
VITE_APP_TITLE=AI Chat Mini Program

# 开发模式
NODE_ENV=development

# 日志级别
VITE_LOG_LEVEL=debug
```

**后端配置 (`application-dev.yml`):**
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/example_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: rootpassword
    driver-class-name: com.mysql.cj.jdbc.Driver

logging:
  level:
    com.example.aichat: DEBUG
    org.springframework.web: INFO
```

---

## 启动步骤

### 方式1: 使用启动器 (推荐)

```powershell
# 双击运行 launcher.bat
# 或在命令行执行:
.\launcher.bat

# 选择选项 1: 启动完整应用
# 选择选项 7: 打开小程序界面
```

### 方式2: 手动启动

**步骤1: 启动后端服务**
```powershell
cd backend/aichat
mvn spring-boot:run
```

**步骤2: 启动前端服务 (新终端)**
```powershell
cd frontend
npm run dev
```

**步骤3: 访问小程序界面**
```
浏览器访问: http://localhost:5175/#/miniprogram
```

### 方式3: 使用 Electron 桌面应用

```powershell
cd frontend

# 构建前端
npm run build

# 启动 Electron
npm run electron:dev
```

---

## 功能测试指南

### 1. 下拉刷新功能测试

**测试步骤:**
1. 打开小程序界面
2. 在首页顶部向下拖动
3. 观察刷新指示器出现
4. 松开触发刷新
5. 等待刷新完成

**验证点:**
- [ ] 触发机制: 下拉距离 >= 60px 触发刷新
- [ ] 加载状态: 显示"刷新中..."和旋转动画
- [ ] 完成更新: 2秒后显示"刷新成功"提示

**日志验证:**
```javascript
// 打开浏览器开发者工具 (F12) -> Console
// 应看到以下日志:
[MiniProgram] Touch start: { startY: 120 }
[MiniProgram] Touch move: { distance: 85, status: 'pulling' }
[MiniProgram] Refresh triggered: { timestamp: '2026-06-22 12:00:00' }
[MiniProgram] Refresh completed: { duration: 2000 }
```

### 2. Tab 切换功能测试

**测试步骤:**
1. 点击底部 TabBar 的不同标签
2. 观察页面切换动画
3. 验证内容正确加载

**验证点:**
- [ ] 切换动画: 滑动过渡效果流畅
- [ ] 内容加载: 对应页面内容正确显示
- [ ] 交互响应: 点击响应时间 < 100ms

**日志验证:**
```javascript
// Console 日志示例:
[MiniProgram] Tab change: { from: 0, to: 1, timestamp: '2026-06-22 12:01:00' }
[MiniProgram] Page transition: { component: 'ChatPage', duration: 300 }
[MiniProgram] Navigation updated: { title: '聊天', rightText: '+' }
```

### 3. 手势操作测试

**测试步骤:**
1. 在内容区域左右滑动
2. 验证页面跟随手势移动
3. 松手后自动吸附到最近页面

**验证点:**
- [ ] 手势识别: 正确识别滑动方向
- [ ] 跟随动画: 内容跟随手指移动
- [ ] 自动吸附: 松手后平滑过渡到目标页面

---

## 常见问题排查

### 问题1: 端口被占用

**症状:**
```
Error: listen EADDRINUSE: address already in use :::8080
```

**解决方案:**
```powershell
# 查找占用端口的进程
netstat -ano | findstr ":8080"

# 终止进程 (替换 PID)
taskkill /F /PID <PID>

# 或使用启动器选项 6 停止所有服务
```

### 问题2: npm install 失败

**症状:**
```
npm ERR! network timeout
npm ERR! electron failed to install
```

**解决方案:**
```powershell
# 清除 npm 缓存
npm cache clean --force

# 设置镜像源
npm config set registry https://registry.npmmirror.com
npm config set ELECTRON_MIRROR https://npmmirror.com/mirrors/electron/

# 重新安装
npm install
```

### 问题3: 后端启动失败

**症状:**
```
Failed to configure a DataSource: 'url' attribute is not specified
```

**解决方案:**
```powershell
# 检查 MySQL 服务是否运行
net start MySQL80

# 或使用 Docker 启动 MySQL
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=rootpassword -p 3306:3306 mysql:8.0

# 创建数据库
mysql -u root -p
CREATE DATABASE example_db;
```

### 问题4: 小程序界面空白

**症状:**
- 页面加载后显示空白
- 控制台有错误信息

**解决方案:**
```powershell
# 检查路由配置
# 确认 /miniprogram 路由已添加到 router/index.js

# 检查组件导入
# 确认所有页面组件文件存在

# 清除浏览器缓存
# Ctrl + Shift + Delete -> 清除缓存
```

### 问题5: 日志不显示

**症状:**
- 控制台没有日志输出

**解决方案:**
```javascript
// 确认 logger.js 已正确配置
// 在组件中导入并使用:
import { logger } from '@/utils/logger'
logger.info('MiniProgram', 'Test message')

// 检查浏览器控制台过滤器
// 确保没有过滤掉 info 级别日志
```

---

## 开发环境验证清单

完成以下检查确认环境配置成功:

- [ ] Node.js 版本 >= 18.0.0
- [ ] npm 版本 >= 9.0.0
- [ ] Java JDK 版本 >= 17
- [ ] Maven 版本 >= 3.8.0
- [ ] 前端依赖安装成功
- [ ] 后端依赖下载完成
- [ ] 后端服务启动成功 (端口 8080)
- [ ] 前端服务启动成功 (端口 5175)
- [ ] 小程序界面可访问
- [ ] 下拉刷新功能正常
- [ ] Tab 切换功能正常
- [ ] 日志输出正常

---

## 进阶配置

### 1. 配置 VS Code 调试

创建 `.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "chrome",
      "request": "launch",
      "name": "Launch Frontend",
      "url": "http://localhost:5175",
      "webRoot": "${workspaceFolder}/frontend/src"
    },
    {
      "type": "java",
      "name": "Debug Backend",
      "request": "attach",
      "hostName": "localhost",
      "port": 5005
    }
  ]
}
```

### 2. 配置热重载

**前端热重载 (已内置):**
```powershell
npm run dev
# 修改代码后自动刷新
```

**后端热重载:**
```xml
<!-- 添加到 pom.xml -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <optional>true</optional>
</dependency>
```

### 3. 配置日志文件输出

创建 `frontend/src/utils/miniprogramLogger.js`:
```javascript
class MiniProgramLogger {
  constructor() {
    this.logs = []
    this.maxLogs = 1000
  }

  log(level, module, action, data) {
    const entry = {
      timestamp: new Date().toISOString(),
      level,
      module,
      action,
      data
    }
    
    this.logs.push(entry)
    if (this.logs.length > this.maxLogs) {
      this.logs.shift()
    }
    
    // 输出到控制台
    console.log(`[${entry.timestamp}] [${module}] [${level}] ${action}`, data || '')
    
    // 可选: 发送到后端
    // this.sendToServer(entry)
  }

  info(module, action, data) {
    this.log('INFO', module, action, data)
  }

  error(module, action, data) {
    this.log('ERROR', module, action, data)
  }

  getLogs() {
    return this.logs
  }
}

export const mpLogger = new MiniProgramLogger()
```

---

## 联系支持

如遇到其他问题，请查看:
- 项目日志: `launcher.log`
- 前端日志: 浏览器控制台 (F12)
- 后端日志: `backend/aichat/logs/`

---

*文档版本: 1.0.0*
*最后更新: 2026-06-22*