# Nginx 反向代理配置（SSE / WebSocket / HTTPS）

> 本文档说明前端静态托管 + 后端 API / SSE / WebSocket 反代的关键配置点，并给出完整 `nginx.conf` 示例。
> 对应后端：Spring Boot 监听 `8080`（HTTP）/ `8443`（HTTPS）；SSE 流式端点 `/api/chat/stream/**`；WebSocket 端点 `/ws/**`；API 文档 `/docs/**`。

---

## 1. 为什么需要专门配置

本项目有三类长连接 / 实时流量，普通 Nginx 默认配置会出问题：

| 流量类型 | 端点 | 默认配置的问题 |
| --- | --- | --- |
| SSE 流式对话 | `/api/chat/stream/**` | Nginx 默认开启响应缓冲，AI 逐字输出会被攒住，前端表现为"半天不出字再一次性蹦出"；且默认 60s 读超时会掐断长对话 |
| WebSocket 好友/群聊 | `/ws/**` | 默认不转发 `Upgrade`/`Connection` 头，握手失败；长连接 60s 空闲被断 |
| 静态资源 | `/assets/**` | 不带缓存头，重复拉取浪费带宽 |

---

## 2. SSE 流式对话配置要点

对 `/api/chat/stream/**` 单独 location：

```nginx
location /api/chat/stream/ {
    proxy_pass http://backend:8080/api/chat/stream/;
    proxy_http_version 1.1;

    # 关键：关闭缓冲，让逐字流实时下发
    proxy_buffering off;
    proxy_cache off;
    chunked_transfer_encoding on;

    # 长连接超时：AI 回复可能持续很久
    proxy_read_timeout 300s;     # 读超时 5 分钟
    proxy_send_timeout 300s;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # SSE 必须关闭 gzip，否则流被压缩后无法实时到达
    gzip off;
}
```

要点：
- `proxy_buffering off` 是 SSE 能否逐字显示的关键。
- `proxy_read_timeout 300s` 保证长对话不被中途掐断（任务要求值）。
- 该 location 优先级高于通用 `/api/`，必须写在 `/api/` 之前或更精确匹配。

---

## 3. WebSocket 粘性会话配置

```nginx
location /ws/ {
    proxy_pass http://backend:8080/ws/;
    proxy_http_version 1.1;

    # 关键：转发 WebSocket 握手头
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # WebSocket 长连接：读超时拉长到 1 小时，避免空闲被断
    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
}
```

**多实例部署时的粘性会话**：若后端起多个实例，WebSocket 握手后必须始终打到同一台实例，否则订阅上下文丢失。在 `upstream` 上用 `ip_hash`：

```nginx
upstream backend {
    ip_hash;                       # 按客户端 IP 粘滞到同一后端
    server 10.0.0.11:8080;
    server 10.0.0.12:8080;
    keepalive 32;
}
```

> 单实例部署可不写 `upstream`，直接 `proxy_pass http://127.0.0.1:8080`。

---

## 4. 静态文件缓存

前端构建产物文件名带内容哈希（`assets/index-abc123.js`），可安全长缓存：

```nginx
# 带哈希的构建产物：强缓存 1 年
location ~* \.(js|css|png|jpg|jpeg|gif|ico|webp|woff2?)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}

# index.html 必须每次校验，否则发版后用户拿不到新包
location = /index.html {
    add_header Cache-Control "no-cache";
}
```

---

## 5. HTTPS 证书配置

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate     /etc/nginx/ssl/your-domain.com.crt;
    ssl_certificate_key /etc/nginx/ssl/your-domain.com.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 10m;
}

# HTTP 80 统一跳转 HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

证书生成（自签/正式证书）详见仓库 `docs/HTTPS部署指南.md`。后端也可直接监听 8443，由 Nginx 终结 SSL 后明文反代到 8080（更常见，推荐）。

---

## 6. 完整 nginx.conf 示例

```nginx
events {
    worker_connections 1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    sendfile        on;
    keepalive_timeout 65;
    server_tokens   off;
    client_max_body_size 60m;          # 与后端 multipart 100MB 协调，留余量

    # 多实例后端；单实例可删掉并把 proxy_pass 改为 http://127.0.0.1:8080
    upstream backend {
        ip_hash;                       # WebSocket 粘性会话
        server 127.0.0.1:8080;
        keepalive 32;
    }

    # 安全响应头
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # HTTP -> HTTPS
    server {
        listen 80;
        server_name your-domain.com;
        return 301 https://$host$request_uri;
    }

    server {
        listen 443 ssl;
        server_name your-domain.com;

        ssl_certificate     /etc/nginx/ssl/your-domain.com.crt;
        ssl_certificate_key /etc/nginx/ssl/your-domain.com.key;
        ssl_protocols       TLSv1.2 TLSv1.3;

        root  /usr/share/nginx/html;     # 前端 dist 产物目录
        index index.html;

        # ① SSE 流式对话（必须放在通用 /api/ 之前，更精确匹配）
        location /api/chat/stream/ {
            proxy_pass http://backend/api/chat/stream/;
            proxy_http_version 1.1;
            proxy_buffering off;
            proxy_cache off;
            chunked_transfer_encoding on;
            gzip off;
            proxy_read_timeout 300s;      # SSE 超时 300s
            proxy_send_timeout 300s;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # ② WebSocket（好友/群聊）
        location /ws/ {
            proxy_pass http://backend/ws/;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_read_timeout 3600s;
            proxy_send_timeout 3600s;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # ③ 通用 API
        location /api/ {
            proxy_pass http://backend/api/;
            proxy_read_timeout 300s;
            proxy_send_timeout 300s;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # ④ API 文档（springdoc，阶段二 P1）
        location /docs/ {
            proxy_pass http://backend/docs/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # ⑤ 上传文件
        location /uploads/ {
            proxy_pass http://backend/uploads/;
            proxy_set_header Host $host;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }

        # ⑥ 静态构建产物长缓存
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|webp|woff2?)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }

        # ⑦ SPA 路由回退
        location / {
            try_files $uri $uri/ /index.html;
        }
    }
}
```

---

## 7. 验证清单

- [ ] `nginx -t` 配置语法通过后再 reload
- [ ] 浏览器访问 `https://your-domain.com/docs/swagger-ui.html` 能打开 API 文档
- [ ] AI 对话能逐字流式输出（不再一次性蹦出）
- [ ] 长时间对话（>60s）不被 Nginx 掐断
- [ ] 好友消息实时接收，WebSocket 不断连
- [ ] 刷新任意前端路由不 404（SPA 回退生效）
- [ ] HTTP 80 自动跳 HTTPS
