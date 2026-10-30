# -*- coding: utf-8 -*-
"""验证自动采集：admin 查数据池 -> alice 发动态 -> 再查数据池是否出现 social_post 记录。"""
import json, urllib.request, urllib.error, time

BASE = "http://localhost:8080"

def call(method, path, body=None, token=None, timeout=15):
    req = urllib.request.Request(BASE + path, data=json.dumps(body).encode() if body is not None else None, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read().decode('utf-8', errors='replace')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', errors='replace')

# 1. admin 登录
s, r = call("POST", "/api/admin/login", {"username": "admin", "password": "123456"})
d = json.loads(r)
admin_tok = d["data"]["token"]
print("admin 登录:", s, "token:", bool(admin_tok))

# 2. 查数据池现有记录（统计）
s, r = call("GET", "/api/admin/data-pool/stats", token=admin_tok)
print("采集前 stats:", r[:300])

# 3. alice 登录并发动态
s, r = call("POST", "/api/user/login", {"username": "demo_alice", "password": "Test@123456"})
d = json.loads(r)
a_tok = d["data"]["token"]
a_id = d["data"]["user"]["id"]
print("alice 登录 id:", a_id)

body = {"userId": a_id, "content": "【自动采集验证】数据池同步测试专用动态 2026-09-14",
        "visibility": "PUBLIC"}
s, r = call("POST", "/api/social/post", body, a_tok)
print("发动态:", s, r[:200])

# 4. 等 2 秒（异步采集）再查数据池记录
time.sleep(3)
s, r = call("GET", "/api/admin/data-pool/records?size=20&keyword=%s" % urllib.parse.quote("自动采集验证"), token=admin_tok)
d = json.loads(r)
recs = d.get("data", {}).get("records", [])
print("采集后命中记录数:", len(recs))
for rec in recs[:5]:
    print("  id=%s userId=%s dataType=%s status=%s score=%s content=%s" % (
        rec.get("id"), rec.get("userId"), rec.get("dataType"),
        rec.get("status"), rec.get("integrityScore"), (rec.get("content") or "")[:40]))
