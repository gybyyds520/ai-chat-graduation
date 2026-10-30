# -*- coding: utf-8 -*-
"""验证社区发帖采集链路。"""
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

s, r = call("POST", "/api/admin/login", {"username": "admin", "password": "123456"})
admin_tok = json.loads(r)["data"]["token"]
s, r = call("POST", "/api/user/login", {"username": "demo_alice", "password": "Test@123456"})
d = json.loads(r); a_tok, a_id = d["data"]["token"], d["data"]["user"]["id"]

# 发社区帖子（communityId=3 旅行者之家）
s, r = call("POST", "/api/community/post",
            {"communityId": 3, "userId": a_id, "content": "【自动采集验证】社区帖子数据池同步测试 2026-09-14"},
            a_tok)
print("社区发帖:", s, r[:160])

time.sleep(3)
s, r = call("GET", "/api/admin/data-pool/records?size=20&keyword=%s" % urllib.parse.quote("数据池同步测试"), token=admin_tok)
d = json.loads(r)
recs = d.get("data", {}).get("records", [])
print("数据池命中:", len(recs))
for rec in recs[:5]:
    print("  id=%s userId=%s type=%s status=%s score=%s content=%s" % (
        rec.get("id"), rec.get("userId"), rec.get("dataType"), rec.get("status"),
        rec.get("integrityScore"), (rec.get("content") or "")[:36]))
