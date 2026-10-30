# -*- coding: utf-8 -*-
"""验证 AI 系统管理：安全校验 / Ollama 模型管理 / 配置读写 / 统计。"""
import json, urllib.request, urllib.error

BASE = "http://localhost:8080"

def call(method, path, body=None, token=None, timeout=20):
    req = urllib.request.Request(BASE + path, data=json.dumps(body).encode() if body is not None else None, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read().decode('utf-8', errors='replace')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', errors='replace')

# 1. 安全：无 token 访问管理端点应 401/403
for p in ['/api/aisystem/list', '/api/aisystem/ollama/status', '/api/aisystem/stats', '/api/aisystem/config']:
    s, _ = call("GET", p)
    print("未登录 GET %-30s -> %s (期望401/403)" % (p, s))

# 2. admin 登录
s, r = call("POST", "/api/admin/login", {"username": "admin", "password": "123456"})
tok = json.loads(r)["data"]["token"]
print("admin 登录:", s)

# 3. Ollama 状态与模型
s, r = call("GET", "/api/aisystem/ollama/status", token=tok)
d = json.loads(r)
print("Ollama 状态:", s, json.dumps(d.get("data"), ensure_ascii=False)[:220])
s, r = call("GET", "/api/aisystem/ollama/models", token=tok)
d = json.loads(r)
data = d.get("data")
mods = data if isinstance(data, list) else data.get("models") if isinstance(data, dict) else data
print("模型列表:", s, json.dumps(mods, ensure_ascii=False)[:300])

# 4. 配置读写
s, r = call("GET", "/api/aisystem/config", token=tok)
cfg = json.loads(r).get("data")
print("配置读取:", s, json.dumps(cfg, ensure_ascii=False)[:200])
s, r = call("PUT", "/api/aisystem/config", {"defaultModel": "llama3.2:3b", "temperature": 0.8, "maxTokens": 1024, "requestTimeoutSec": 60, "aiChatEnabled": True, "maintenanceNotice": ""}, tok)
print("配置更新:", s, r[:100])
s, r = call("GET", "/api/aisystem/config", token=tok)
print("配置回读:", s, json.dumps(json.loads(r).get("data"), ensure_ascii=False)[:200])

# 5. 统计
s, r = call("GET", "/api/aisystem/stats", token=tok)
print("统计:", s, json.dumps(json.loads(r).get("data"), ensure_ascii=False)[:220])

# 6. authenticate 公开（API Key 认证用途，无 token 可访问）
s, r = call("POST", "/api/aisystem/authenticate", None)  # 无 header，验证不返回 403
print("authenticate 未带密钥(应200但认证失败):", s, r[:80])
