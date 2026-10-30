# HTTPS 部署指南

本文档说明 AI-Chat 项目如何开启 HTTPS，覆盖**本地自签名证书**、**后端 Spring Boot HTTPS**、**前端 Vite HTTPS 开发服务器**，以及**生产环境 Let's Encrypt + Nginx 反向代理**方案。

> 现状：默认配置为 **HTTP（后端 8080 / 前端 5187）**，`server.ssl.enabled=false`，不影响现有开发与测试环境。HTTPS 通过 prod profile 或单独注释块按需开启。

---

## 一、生成自签名证书

后端默认使用 PKCS12 格式的 `keystore.p12`。二选一即可。

### 方式 A：JDK 自带 keytool（推荐，直接生成 Spring Boot 可用的 PKCS12）

```powershell
# 在 backend/aichat/src/main/resources/ 目录下执行
keytool -genkeypair `
  -alias ai-chat `
  -keyalg RSA -keysize 2048 `
  -storetype PKCS12 `
  -keystore keystore.p12 `
  -storepass changeit `
  -validity 3650 `
  -dname "CN=localhost, OU=dev, O=ai-chat, L=Fuzhou, ST=Fujian, C=CN" `
  -ext "SAN=dns:localhost,ip:127.0.0.1"
```

- `-alias ai-chat` 与 `application.yml` 中 `key-alias: ai-chat` 对应。
- `-storepass changeit` 与 `key-store-password: changeit` 对应（生产务必改成强密码并外置到环境变量）。
- `-ext SAN=...` 用于把 localhost / 127.0.0.1 写进证书主题备用名，否则浏览器会报域名不匹配。
- 生成后会在当前目录得到 `keystore.p12`。

### 方式 B：OpenSSL 生成 PEM，再转 PKCS12

```bash
# 1. 生成私钥与自签证书（PEM）
openssl req -x509 -newkey rsa:2048 -nodes `
  -keyout server.key -out server.crt -days 3650 `
  -subj "/CN=localhost" `
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

# 2. 转成 PKCS12，供 Spring Boot 使用
openssl pkcs12 -export -in server.crt -inkey server.key `
  -name ai-chat -out keystore.p12 -password pass:changeit
```

> 安全提示：`keystore.p12` 含私钥，**不要提交到 Git**。应在 `.gitignore` 中忽略，或在生产环境通过环境变量 `SERVER_SSL_KEY_STORE` 指向外部绝对路径。

---

## 二、后端 HTTPS 配置

### 1. 基础配置（已内置，默认关闭）

`backend/aichat/src/main/resources/application.yml` 已包含：

```yaml
server:
  port: 8080
  ssl:
    enabled: false              # 默认关闭；prod 覆盖为 true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: ai-chat
```

### 2. 通过 prod profile 开启（推荐）

新建 `backend/aichat/src/main/resources/application-prod.yml`：

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SERVER_SSL_KEYSTORE_PASSWORD}   # 从环境变量注入
    key-store-type: PKCS12
    key-alias: ai-chat
```

启动生产后端：

```powershell
cd backend\aichat
$env:SPRING_PROFILES_ACTIVE="prod"
$env:SERVER_SSL_KEYSTORE_PASSWORD="你的强密码"
.\mvnw.cmd spring-boot:run
```

启动后访问 `https://localhost:8443`。

### 3. 证书放在外部目录（不打进 jar）

生产更推荐把证书放在 jar 外部，避免私钥随制品分发：

```yaml
server:
  ssl:
    key-store: file:/etc/ai-chat/keystore.p12   # Linux 示例
    # key-store: file:C:/ai-chat/cert/keystore.p12  # Windows 示例
```

---

## 三、前端 HTTPS 开发配置

`frontend/vite.config.js` 的 `server` 段已预留注释块。开启步骤：

1. 生成证书（二选一）：
   - 安装 [mkcert](https://github.com/FiloSottile/mkcert) 后执行 `mkcert localhost 127.0.0.1`，得到受本机信任的证书。
   - 或用任意自签证书脚本生成 `server.key` / `server.crt`，放到 `frontend/cert/`。
2. 在 `vite.config.js` 顶部引入 `fs`：
   ```js
   import fs from 'fs'
   ```
3. 取消 `server.https` 注释：
   ```js
   https: {
     key: fs.readFileSync(resolve(__dirname, 'cert/server.key')),
     cert: fs.readFileSync(resolve(__dirname, 'cert/server.crt'))
   },
   ```
4. 启动后用 `https://localhost:5187` 访问。

> 注意：前端走 HTTPS 时，`server.proxy` 里指向后端的 target 仍为 `http://localhost:8080`（后端 HTTP）即可；若后端也开了 HTTPS，需把 target 改为 `https://localhost:8443` 并视情况配置 `secure: false`（自签证书场景）。

---

## 四、生产环境 HTTPS（Let's Encrypt + Nginx 反向代理）

生产推荐由 Nginx 统一终止 TLS，后端继续跑内网 HTTP，证书自动续期。

### 1. 申请 Let's Encrypt 证书（certbot）

```bash
# Ubuntu/Debian
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d chat.example.com
# 证书位于 /etc/letsencrypt/live/chat.example.com/
# 自动续期：sudo certbot renew --dry-run
```

### 2. Nginx 配置示例

```nginx
server {
    listen 80;
    server_name chat.example.com;
    return 301 https://$host$request_uri;   # HTTP 跳转 HTTPS
}

server {
    listen 443 ssl;
    server_name chat.example.com;

    ssl_certificate     /etc/letsencrypt/live/chat.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/chat.example.com/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # 前端静态资源（Vue build 产物）
    root /var/www/ai-chat/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反代到内网 8080
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket
    location /ws/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 3. 后端配合项

- 后端保持 HTTP(8080) 即可，无需再开 `server.ssl`。
- 确认 `application.yml` 的 `cors.allowed-origins` 已包含 `https://chat.example.com`。
- 后端读取 `X-Forwarded-Proto` 识别原始协议；如框架未自动识别，可在 Nginx 已注入该头的前提下，按需在后端配置 `server.forward-headers-strategy: framework`。

---

## 五、常见问题

| 现象 | 原因 | 处理 |
|------|------|------|
| 浏览器提示"您的连接不是私密连接" | 使用了自签名证书 | 本地开发点击"高级 → 继续访问"，或用 mkcert 让本机信任 |
| 启动报 `keystore password was incorrect` | `key-store-password` 与生成证书时的 storepass 不一致 | 两者保持一致，或通过环境变量注入 |
| 启动报 `alias name not found` | `key-alias` 与 keytool 生成时的 `-alias` 不一致 | 统一为 `ai-chat` |
| HTTPS 开启后接口调不通 / 混合内容报错 | 前端 HTTPS 页面里仍写死了 `http://` 的接口地址 | 接口走相对路径，或在 axios 基址使用 `https://` |
| 域名不匹配 NET::ERR_CERT_COMMON_NAME_INVALID | 证书 SAN 未包含访问域名 | 生成证书时通过 `-ext SAN=dns:xxx` 补全域名/IP |
| 前后端都开 HTTPS 时代理报错 | 自签证书不被 Node 信任 | Vite proxy 加 `secure: false`，或使用 mkcert 信任链 |
| 证书快过期 | 自签证书 validity 到期 / Let's Encrypt 90 天到期 | 自签重新生成；Let's Encrypt 用 `certbot renew` 自动续期 |

---

## 六、本地自签名 HTTPS 验证（毕设演示用，无需真实域名/公网）

> 本项目仅用于毕设演示、不上线公网，因此**不做 Nginx 生产模板、不申请真实域名/Let's Encrypt**。
> 下面用免费开源的 OpenSSL 生成本地自签名证书，让后端在 `https://localhost:8443` 跑通即可。

### 1. 用 OpenSSL 生成自签名证书并转 PKCS12

```bash
# 在 backend/aichat/src/main/resources/ 目录下执行（免费，OpenSSL 开源）
# 1) 生成自签证书 + 私钥（CN 必须是 localhost，SAN 包含 127.0.0.1）
openssl req -x509 -newkey rsa:2048 -nodes ^
  -keyout server.key -out server.crt -days 3650 ^
  -subj "/CN=localhost" ^
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

# 2) 转成 Spring Boot 可用的 PKCS12（别名 ai-chat，口令 changeit）
openssl pkcs12 -export -in server.crt -inkey server.key ^
  -name ai-chat -out keystore.p12 -password pass:changeit
```

> 也可直接用 JDK 自带 `keytool`（见本文第一节"方式 A"），效果相同。

### 2. 以 https profile 启动后端（8443）

主 `application.yml` 保持 `ssl.enabled=false`（保护现有 877 测试基线不回退）。
本地验证时启用单独的 `https` profile：

```powershell
cd backend\aichat
$env:SPRING_PROFILES_ACTIVE="https"
.\mvnw.cmd spring-boot:run
```

`application-https.yml` 已就绪：端口 8443、开启 SSL、加载上面生成的 `keystore.p12`。

### 3. 浏览器验证

- 访问 `https://localhost:8443/docs/swagger-ui`（或任意接口）。
- 浏览器提示"您的连接不是私密连接"属**正常现象**（自签名证书），点"高级 → 继续前往 localhost"即可。
- 控制台日志出现 `Tomcat started on port(s): 8443 (https)` 即验证通过。

> 说明：自签名证书只用于本地演示。若将来真要上线公网，才需要真实域名 + ICP 备案 + 可信 CA 证书；毕设阶段不需要。
