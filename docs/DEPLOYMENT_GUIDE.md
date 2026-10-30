# AI Chat 部署指南

## 概述

本指南提供 AI Chat 应用的完整部署方案，支持 Web 服务器部署和桌面应用打包两种方式。

---

## 目录

1. [快速开始](#快速开始)
2. [Web 服务器部署](#web-服务器部署)
3. [桌面应用打包](#桌面应用打包)
4. [Docker 部署](#docker-部署)
5. [环境变量配置](#环境变量配置)
6. [性能优化](#性能优化)
7. [故障排除](#故障排除)

---

## 快速开始

### 方式 1：本地开发服务器

```bash
cd frontend
npm run dev
# 访问: http://localhost:5173
```

### 方式 2：生产构建

```bash
cd frontend
npm run build
npm run preview
# 访问: http://localhost:4173
```

### 方式 3：一键部署脚本

```bash
# Windows
frontend/scripts/deploy-web.bat

# Linux/Mac
sh frontend/scripts/deploy-web.sh
```

---

## Web 服务器部署

### 环境要求

| 组件 | 版本 |
|------|------|
| Node.js | ≥ 18.x |
| npm | ≥ 9.x |
| Web Server | Nginx / Apache / IIS |

### 部署步骤

#### 步骤 1：构建生产版本

```bash
cd frontend
npm run build
```

构建产物位于 `frontend/dist/` 目录。

#### 步骤 2：部署到 Nginx

**配置文件**: `/etc/nginx/sites-available/aichat`

```nginx
server {
    listen 80;
    server_name aichat.example.com;
    
    root /var/www/aichat/dist;
    index index.html;
    
    # Gzip 压缩
    gzip on;
    gzip_types text/css application/javascript image/svg+xml;
    
    # 缓存静态资源
    location ~* \.(js|css|png|jpg|jpeg|svg|ico)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # SPA 路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 安全头
    add_header X-Frame-Options "SAMEORIGIN";
    add_header X-XSS-Protection "1; mode=block";
    add_header X-Content-Type-Options "nosniff";
}
```

**启用配置**:

```bash
sudo ln -s /etc/nginx/sites-available/aichat /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

#### 步骤 3：部署到 Apache

**配置文件**: `/etc/apache2/sites-available/aichat.conf`

```apache
<VirtualHost *:80>
    ServerName aichat.example.com
    DocumentRoot /var/www/aichat/dist
    
    <Directory /var/www/aichat/dist>
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>
    
    # SPA 路由支持
    RewriteEngine On
    RewriteBase /
    RewriteRule ^index\.html$ - [L]
    RewriteCond %{REQUEST_FILENAME} !-f
    RewriteCond %{REQUEST_FILENAME} !-d
    RewriteRule . /index.html [L]
    
    # Gzip 压缩
    SetOutputFilter DEFLATE
    AddOutputFilterByType DEFLATE text/css application/javascript
</VirtualHost>
```

**启用配置**:

```bash
sudo a2enmod rewrite
sudo a2ensite aichat.conf
sudo systemctl reload apache2
```

#### 步骤 4：部署到 IIS

**Web.config**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <system.webServer>
        <rewrite>
            <rules>
                <rule name="SPA Routes" stopProcessing="true">
                    <match url=".*" />
                    <conditions logicalGrouping="MatchAll">
                        <add input="{REQUEST_FILENAME}" matchType="IsFile" negate="true" />
                        <add input="{REQUEST_FILENAME}" matchType="IsDirectory" negate="true" />
                    </conditions>
                    <action type="Rewrite" url="/index.html" />
                </rule>
            </rules>
        </rewrite>
        <staticContent>
            <clientCache cacheControlMode="UseMaxAge" cacheControlMaxAge="365.00:00:00" />
        </staticContent>
        <httpCompression>
            <dynamicTypes>
                <add mimeType="text/css" enabled="true" />
                <add mimeType="application/javascript" enabled="true" />
            </dynamicTypes>
        </httpCompression>
    </system.webServer>
</configuration>
```

---

## 桌面应用打包

### 环境要求

| 组件 | 版本 |
|------|------|
| Node.js | ≥ 18.x |
| npm | ≥ 9.x |
| Electron | ≥ 28.x |

### 打包步骤

#### 步骤 1：安装依赖

```bash
cd frontend
npm install
npm install electron --save-dev
```

#### 步骤 2：构建前端

```bash
npm run build
```

#### 步骤 3：打包桌面应用

```bash
# Windows
npm run electron:build -- --win

# macOS
npm run electron:build -- --mac

# Linux
npm run electron:build -- --linux

# 全部平台
npm run electron:build
```

#### 步骤 4：创建桌面快捷方式

```bash
# 创建所有快捷方式
node scripts/create-shortcut.js -all

# 仅创建桌面快捷方式
node scripts/create-shortcut.js -desktop
```

### 打包产物

```
frontend/dist/electron/
├── win-unpacked/           # Windows 解压版
│   └── AI Chat.exe
├── AI Chat Setup 1.0.0.exe  # Windows 安装包
├── mac/                     # macOS 应用
│   └── AI Chat.app
└── linux-unpacked/          # Linux 解压版
    └── ai-chat
```

---

## Docker 部署

### 方式 1：使用 Nginx 镜像

**Dockerfile**:

```dockerfile
FROM nginx:alpine

COPY frontend/dist/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

**nginx.conf**:

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;
    
    gzip on;
    gzip_types text/css application/javascript;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

**构建和运行**:

```bash
# 构建镜像
docker build -t aichat:latest .

# 运行容器
docker run -d -p 80:80 --name aichat aichat:latest
```

### 方式 2：使用 Node.js 开发服务器

```bash
# 启动开发容器
docker run -it --rm -v $(pwd)/frontend:/app -p 5173:5173 node:20-alpine sh -c "
    cd /app && npm install && npm run dev -- --host 0.0.0.0
"
```

---

## 环境变量配置

### 开发环境

创建 `.env.development.local`:

```env
# API 配置
VITE_API_BASE_URL=http://localhost:3000/api
VITE_WS_BASE_URL=ws://localhost:3000/ws

# 特性开关
VITE_ENABLE_VOICE=true
VITE_ENABLE_DESKTOP_ELF=true

# 日志级别
VITE_LOG_LEVEL=debug
```

### 生产环境

创建 `.env.production.local`:

```env
# API 配置
VITE_API_BASE_URL=https://api.aichat.example.com
VITE_WS_BASE_URL=wss://api.aichat.example.com/ws

# 特性开关
VITE_ENABLE_VOICE=true
VITE_ENABLE_DESKTOP_ELF=true

# 日志级别
VITE_LOG_LEVEL=info

# Sentry 监控
VITE_SENTRY_DSN=your-sentry-dsn
```

---

## 性能优化

### 代码分割

修改 `vite.config.js`:

```javascript
export default {
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendors': ['vue', 'vue-router'],
          'element-plus': ['element-plus'],
          'charts': ['echarts', 'vue-echarts'],
          'three': ['three'],
          'hands': ['@mediapipe/hands']
        }
      }
    }
  }
}
```

### 懒加载组件

```javascript
const DesktopElf = defineAsyncComponent(() => 
  import('./components/DesktopElf.vue')
)
```

### 图片优化

```bash
# 安装图片压缩工具
npm install imagemin --save-dev

# 压缩命令
npx imagemin src/assets/images/* --out-dir=dist/assets/images
```

---

## 故障排除

### 问题 1：前端无法连接后端

**症状**: 控制台显示 CORS 错误或连接超时

**解决方案**:

```nginx
# 在后端配置 CORS
add_header Access-Control-Allow-Origin "*";
add_header Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS";
add_header Access-Control-Allow-Headers "Content-Type, Authorization";
```

### 问题 2：Electron 打包失败

**症状**: `npm run electron:build` 报错

**解决方案**:

```bash
# 清除缓存
npm cache clean --force

# 删除旧构建
rm -rf dist/electron

# 设置镜像
npm config set ELECTRON_MIRROR https://npmmirror.com/mirrors/electron/

# 重新安装
npm install electron --save-dev
```

### 问题 3：页面空白

**症状**: 浏览器显示空白页面，控制台无错误

**解决方案**:

1. 检查 `dist/index.html` 是否存在
2. 确认所有资源路径正确
3. 检查 JavaScript 控制台是否有静默错误
4. 验证 Vue 路由配置

### 问题 4：内存占用过高

**症状**: 应用运行一段时间后内存占用持续增长

**解决方案**:

1. 启用 Vue DevTools 进行内存分析
2. 检查是否有未清理的事件监听器
3. 优化图片和资源加载
4. 实现组件懒加载

---

## 部署检查清单

### 生产环境检查

- [ ] ✅ 构建成功 (`npm run build`)
- [ ] ✅ 所有测试通过 (`npm test`)
- [ ] ✅ HTTPS 配置完成
- [ ] ✅ CORS 配置正确
- [ ] ✅ 安全头已设置
- [ ] ✅ 日志记录已配置
- [ ] ✅ 监控告警已设置
- [ ] ✅ 备份策略已制定

### 桌面应用检查

- [ ] ✅ Electron 版本正确
- [ ] ✅ 图标资源完整
- [ ] ✅ 安装包生成成功
- [ ] ✅ 快捷方式创建成功
- [ ] ✅ 启动测试通过
- [ ] ✅ 卸载测试通过

---

## 技术支持

如有问题，请提供以下信息：

1. 操作系统版本
2. Node.js/npm 版本
3. 完整的错误日志
4. 部署方式（Web/桌面/Docker）

---

*文档版本: 1.0.0*
*最后更新: 2026-06-22*