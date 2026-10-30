# -*- coding: utf-8 -*-
"""第四轮残留失败项定向诊断。"""
import json, urllib.request, urllib.error

BASE = "http://localhost:8080"
PW = "Test@123456"

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
    except Exception as e:
        return -1, "EXC:" + str(e)[:100]

def login(username):
    s, r = call("POST", "/api/user/login", {"username": username, "password": PW})
    d = json.loads(r)
    return d["data"]["user"]["id"], d["data"]["token"]

a_id, a_tok = login("demo_alice")
b_id, b_tok = login("demo_bob")
f_id, f_tok = login("demo_frank")
print("alice=%s bob=%s frank=%s" % (a_id, b_id, f_id))

def probe(title, m, p, body=None, tok=None):
    s, r = call(m, p, body, tok)
    print("\n### %s\n    %s %s -> %s %s" % (title, m, p, s, r[:300]))

# 1 私聊 send 真实字段
probe("私聊 #1", "POST", "/api/friend/chat/send", {"userId": a_id, "friendId": b_id, "content": "你好"}, a_tok)
probe("私聊 #2 用 senderId", "POST", "/api/friend/chat/send", {"senderId": a_id, "receiverId": b_id, "content": "你好"}, a_tok)
# 2 建群（带 userId+userName）
probe("建群", "POST", "/api/group/create", {"name": "诊断群X", "description": "d", "userId": a_id, "userName": "爱丽丝"}, a_tok)
# 3 精灵日程
probe("精灵日程", "POST", "/api/elf/schedule", {"elfId": 2, "userId": a_id, "title": "喝水", "time": "10:00", "reminderMinutes": 5}, a_tok)
# 4 搜索帖子
probe("搜索帖子", "GET", "/api/search/posts?keyword=%s&userId=%s" % (urllib.parse.quote("爬山"), a_id), None, a_tok)
probe("搜索帖子 无userId", "GET", "/api/search/posts?keyword=%s" % urllib.parse.quote("爬山"), None, a_tok)
# 5 举报
probe("举报", "POST", "/api/block-report/report", {"targetType": "post", "targetId": 27, "reportReason": "广告内容", "userId": a_id}, a_tok)
probe("举报 无userId", "POST", "/api/block-report/report", {"targetType": "post", "targetId": 27, "reportReason": "广告内容"}, a_tok)
# 6 拉黑
probe("拉黑", "POST", "/api/block-report/block", {"blockedUserId": f_id, "userId": a_id}, a_tok)
# 7 隐私设置
probe("隐私设置", "POST", "/api/privacy/setting", {"userId": a_id, "key": "profile_visibility", "value": "friends"}, a_tok)
