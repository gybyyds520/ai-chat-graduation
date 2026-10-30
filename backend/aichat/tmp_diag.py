# -*- coding: utf-8 -*-
"""诊断脚本：重放注入失败的端点，打印完整响应体以区分参数错误与真实故障。"""
import json, urllib.request, urllib.error, urllib.parse, uuid, random

BASE = "http://localhost:8080"
PW = "Test@123456"

def call(method, path, body=None, token=None, timeout=40):
    url = BASE + path
    data = json.dumps(body).encode('utf-8') if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode('utf-8', errors='replace')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', errors='replace')

def login(username):
    s, r = call("POST", "/api/user/login", {"username": username, "password": PW})
    d = json.loads(r)
    return d["data"]["user"]["id"], d["data"]["token"]

a_id, a_tok = login("demo_alice")
b_id, b_tok = login("demo_bob")
c_id, c_tok = login("demo_carol")
f_id, f_tok = login("demo_frank")
print("alice=%s bob=%s carol=%s frank=%s" % (a_id, b_id, c_id, f_id))

def probe(title, m, p, body=None, tok=None):
    s, r = call(m, p, body, tok)
    print("\n### %s -> %s %s" % (title, s, r[:220]))

# 1 私聊
probe("私聊 send alice->bob", "POST", "/api/friend/chat/send",
      {"userId": a_id, "friendId": b_id, "content": "测试消息"}, a_tok)
# 2 建群
probe("建群", "POST", "/api/group/create",
      {"name": "诊断群%d" % random.randint(100, 999), "description": "d", "userId": a_id, "userName": "爱丽丝"}, a_tok)
# 3 社区帖子列表
probe("社区帖子列表", "GET", "/api/community/posts/2", None, a_tok)
# 4 分享动态
probe("分享动态", "POST", "/api/social/share",
      {"postId": 22, "shareType": "link", "shareText": "推荐", "userId": b_id}, b_tok)
# 5 角色分享（找分享端点）
probe("角色分享 try /api/character/share", "POST", "/api/character/share",
      {"characterId": 44, "userId": a_id, "isShared": True}, a_tok)
probe("角色分享 try /api/plaza/share", "POST", "/api/plaza/share",
      {"characterId": 44, "userId": a_id}, a_tok)
# 6 广场搜索
probe("广场搜索", "GET", "/api/plaza/search?keyword=%s" % urllib.parse.quote("旅行"), None, b_tok)
# 7 精灵互动
probe("精灵互动 chat", "POST", "/api/elf/2/interact",
      {"interactionType": "chat", "userId": a_id}, a_tok)
probe("精灵睡眠", "POST", "/api/elf/sleep",
      {"elfId": 2, "userId": a_id, "sleepTime": "23:00", "wakeTime": "07:00", "quality": 9}, a_tok)
probe("精灵闹钟", "POST", "/api/elf/alarm",
      {"elfId": 2, "userId": a_id, "time": "07:30", "repeatDays": "1,2,3,4,5", "title": "起床"}, a_tok)
# 8 TTS
probe("TTS", "POST", "/api/voice/tts", {"text": "你好"}, a_tok)
# 9 搜索帖子
probe("搜索帖子", "GET", "/api/search/posts?keyword=%s" % urllib.parse.quote("爬山"), None, a_tok)
# 10 推荐
probe("推荐帖子", "GET", "/api/recommendation/posts?userId=%s" % a_id, None, a_tok)
# 11 举报
probe("举报帖子", "POST", "/api/block-report/report",
      {"targetType": "post", "targetId": 23, "reportReason": "广告内容", "userId": a_id}, a_tok)
probe("拉黑", "POST", "/api/block-report/block",
      {"blockedUserId": f_id, "userId": a_id}, a_tok)
# 12 隐私设置
probe("隐私设置 POST", "POST", "/api/privacy/setting",
      {"userId": a_id, "key": "profile_visibility", "value": "friends"}, a_tok)
# 13 支付账户
probe("绑定支付账户", "POST", "/api/pay-account/bind",
      {"userId": a_id, "accountType": "ALIPAY", "accountNo": "alice@example.com"}, a_tok)
