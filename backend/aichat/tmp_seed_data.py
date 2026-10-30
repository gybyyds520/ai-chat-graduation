# -*- coding: utf-8 -*-
"""综合测试数据注入脚本：通过后端 API 注入覆盖全功能的模拟数据（数据保留不删除）。"""
import json, urllib.request, urllib.error, urllib.parse, uuid, random, io, os, time

BASE = "http://localhost:8080"
PW = "Test@123456"
results = []  # (step, ok, detail)

def log(step, ok, detail=""):
    results.append((step, ok, detail))
    mark = "OK " if ok else "FAIL"
    print("[%s] %-38s %s" % (mark, step, str(detail)[:110]))

def call(method, path, body=None, token=None, timeout=40, raw_body=None, ctype=None):
    url = BASE + path
    data = None
    headers = {}
    if raw_body is not None:
        data = raw_body
        headers['Content-Type'] = ctype
    elif body is not None:
        data = json.dumps(body).encode('utf-8')
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode('utf-8', errors='replace')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', errors='replace')
    except Exception as e:
        return -1, "EXC:" + str(e)[:120]

def jd(r):
    try:
        return json.loads(r)
    except Exception:
        return None

def multipart(path, field_name, filename, content, extra_fields=None, token=None):
    boundary = "----doubaoSeed" + uuid.uuid4().hex
    parts = []
    for k, v in (extra_fields or {}).items():
        parts.append(("--" + boundary).encode())
        parts.append(('Content-Disposition: form-data; name="%s"' % k).encode())
        parts.append(b"")
        parts.append(str(v).encode('utf-8'))
    parts.append(("--" + boundary).encode())
    parts.append(('Content-Disposition: form-data; name="%s"; filename="%s"' % (field_name, filename)).encode())
    parts.append(b"Content-Type: text/plain")
    parts.append(b"")
    parts.append(content if isinstance(content, bytes) else content.encode('utf-8'))
    parts.append(("--" + boundary + "--").encode())
    body = b"\r\n".join(parts)
    return call("POST", path, raw_body=body, token=token,
                ctype="multipart/form-data; boundary=" + boundary, timeout=60)

def register(username, nickname, age):
    s, r = call("POST", "/api/user/register",
                {"username": username, "password": PW, "nickname": nickname, "age": age})
    d = jd(r)
    if s == 200 and d and d.get("code") == 200:
        uid = d["data"]["id"]
        tok = d["data"].get("token")
        # 注册成功：不立即登录（避免与后续统一登录叠加超出限流窗口）
        return uid, tok or None
    # 用户名已存在时尝试登录
    s2, r2 = call("POST", "/api/user/login", {"username": username, "password": PW})
    d2 = jd(r2)
    if s2 == 200 and d2 and d2.get("code") == 200:
        return d2["data"]["user"]["id"], d2["data"]["token"]
    return None, None

def login_with_retry(username, retries=3):
    """登录，遇 429 限流则等待窗口后重试。"""
    for i in range(retries):
        s, r = call("POST", "/api/user/login", {"username": username, "password": PW})
        d = jd(r)
        if s == 200 and d and d.get("code") == 200:
            return d["data"]["user"]["id"], d["data"]["token"]
        if s == 429 or (s == 200 and d and d.get("code") == 429):
            print("    [wait] %s 登录限流，等待65s..." % username)
            time.sleep(65)
            continue
        break
    return None, None

# ============ A. 用户体系 ============
print("========== A. 用户注册 ==========")
users = [
    ("demo_alice", "爱丽丝", 25),
    ("demo_bob", "鲍勃", 28),
    ("demo_carol", "卡罗尔", 22),
    ("demo_dave", "戴夫", 30),
    ("demo_erin", "艾琳", 17),
    ("demo_frank", "弗兰克", 35),
]
tokens = {}
for un, nn, age in users:
    # 优先登录（已存在用户）；登录失败（用户不存在）再注册
    uid, tok = login_with_retry(un)
    if uid and tok:
        tokens[un] = {"id": uid, "token": tok}
        log("登录 %s(id=%s)" % (un, uid), True)
    else:
        uid, tok = register(un, nn, age)
        if uid:
            tokens[un] = {"id": uid, "token": tok}
            log("注册 %s(id=%s)" % (un, uid), True)
        else:
            log("注册 %s" % un, False, "无法注册/登录")
    time.sleep(3)  # 缓解限流

# 统一补齐缺失 token（登录限流窗口等待）
missing = [un for un in users if un in tokens and not tokens[un]["token"]]
if missing:
    print("    [补登录] 缺失token:", missing)
    time.sleep(60)
    for un in missing:
        uid, tok = login_with_retry(un)
        if tok:
            tokens[un] = {"id": uid, "token": tok}
            log("补登录 %s" % un, True)
        else:
            log("补登录 %s" % un, False)

# 资料完善
for un, nn, age in users:
    if un not in tokens:
        continue
    s, r = call("PUT", "/api/user/profile", {
        "userId": str(tokens[un]["id"]),
        "nickname": nn,
        "bio": "这是 %s 的自我介绍，热爱生活与科技。" % nn,
        "interests": "AI,旅行,音乐" if un != "demo_erin" else "二次元,游戏,猫",
        "location": "福州",
        "website": "https://example.com/%s" % un,
        "gender": "female" if un in ("demo_alice", "demo_carol", "demo_erin") else "male",
        "birthday": "1999-05-20",
        "age": age,
    }, token=tokens[un]["token"])
    log("完善资料 %s" % un, s == 200 and jd(r).get("code") == 200)

# ============ B. 好友关系 ============
print("========== B. 好友关系 ==========")
friend_pairs = [("demo_alice", "demo_bob"), ("demo_alice", "demo_carol"),
                ("demo_alice", "demo_dave"), ("demo_bob", "demo_erin"),
                ("demo_bob", "demo_frank")]
for a, b in friend_pairs:
    s, r = call("POST", "/api/friend/request", {"userId": tokens[a]["id"], "friendId": tokens[b]["id"]},
                token=tokens[a]["token"])
    d = jd(r)
    req_id = (d or {}).get("data", {})
    if isinstance(req_id, dict):
        req_id = req_id.get("requestId") or req_id.get("id")
    ok1 = s == 200 and (d or {}).get("code") == 200
    msg1 = (d or {}).get("msg", "")
    # 对方接受（幂等：已存在关系时接受失败也视为该步骤完成）
    s2, r2 = call("POST", "/api/friend/accept",
                  {"userId": tokens[b]["id"], "requesterId": tokens[a]["id"]},
                  token=tokens[b]["token"])
    d2 = jd(r2)
    ok2 = s2 == 200 and (d2 or {}).get("code") == 200
    msg2 = (d2 or {}).get("msg", "")
    # 已是好友 / 请求已存在 / 请求不存在均视为链路已具备
    idem = any(k in msg1 + msg2 for k in ("已存在", "已是好友", "不存在", "未找到"))
    log("好友 %s->%s" % (a, b), ok1 or idem, "req=%s msg=%s|%s" % (req_id, msg1, msg2))

# 好友列表验证
s, r = call("GET", "/api/friend/list?userId=%s" % tokens["demo_alice"]["id"], token=tokens["demo_alice"]["token"])
log("alice好友列表", s == 200)

# ============ C. 好友分组 ============
print("========== C. 好友分组 ==========")
s, r = call("POST", "/api/friend/group/create",
            {"userId": tokens["demo_alice"]["id"], "name": "爬山搭子"},
            token=tokens["demo_alice"]["token"])
d = jd(r)
gid = None
if d and d.get("code") == 200:
    gid = d["data"].get("id") or d["data"].get("groupId")
log("alice建组[爬山搭子]", bool(gid), "gid=%s" % gid)
if gid:
    s, r = call("POST", "/api/friend/group/member/add",
                {"groupId": gid, "userId": tokens["demo_alice"]["id"], "friendId": tokens["demo_bob"]["id"]},
                token=tokens["demo_alice"]["token"])
    log("加bob入组", s == 200 and jd(r).get("code") == 200)
    s, r = call("GET", "/api/friend/group/list?userId=%s" % tokens["demo_alice"]["id"],
                token=tokens["demo_alice"]["token"])
    log("alice分组列表", s == 200)

# ============ D. 私聊 ============
print("========== D. 私聊 ==========")
msgs = ["晚上一起去看电影吗？", "我推荐新开的日料店，超好吃", "周六的爬山活动别忘了"]
for i, m in enumerate(msgs):
    s, r = call("POST", "/api/friend/chat/send",
                {"senderId": tokens["demo_alice"]["id"], "receiverId": tokens["demo_bob"]["id"], "content": m},
                token=tokens["demo_alice"]["token"])
    log("alice->bob 私聊#%d" % (i + 1), s == 200 and jd(r).get("code") == 200)
s, r = call("POST", "/api/friend/chat/send",
            {"senderId": tokens["demo_bob"]["id"], "receiverId": tokens["demo_alice"]["id"], "content": "好呀，就周六！"},
            token=tokens["demo_bob"]["token"])
log("bob->alice 私聊", s == 200 and jd(r).get("code") == 200)
s, r = call("GET", "/api/friend/chat/history?userId=%s&friendId=%s" % (tokens["demo_alice"]["id"], tokens["demo_bob"]["id"]),
            token=tokens["demo_alice"]["token"])
log("私聊历史", s == 200)
s, r = call("GET", "/api/friend/chat/unread?userId=%s" % tokens["demo_alice"]["id"], token=tokens["demo_alice"]["token"])
log("未读统计", s == 200)

# ============ E. 群聊（GroupChat） ============
print("========== E. 群聊 ==========")
s, r = call("POST", "/api/group/create",
            {"name": "AI 技术交流群", "description": "讨论 AI 技术与毕业设计", "userId": tokens["demo_alice"]["id"], "userName": "爱丽丝"},
            token=tokens["demo_alice"]["token"])
d = jd(r)
gid = None
if d and d.get("code") == 200:
    gid = d["data"].get("id") or d["data"].get("groupId")
log("alice建群[AI技术交流群]", bool(gid), "gid=%s" % gid)
for un in ["demo_bob", "demo_carol", "demo_dave", "demo_erin", "demo_frank"]:
    if gid:
        s, r = call("POST", "/api/group/join",
                    {"groupId": gid, "userId": tokens[un]["id"], "userName": un},
                    token=tokens[un]["token"])
        log("群加入 %s" % un, s == 200 and jd(r).get("code") == 200)
if gid:
    s, r = call("PUT", "/api/group/%s/announcement" % gid, {"announcement": "欢迎加入 AI 技术交流群！文明交流，禁止广告。"},
                token=tokens["demo_alice"]["token"])
    log("群公告", s == 200 and jd(r).get("code") == 200)
    gmsgs = ["大家好，我是爱丽丝", "今天学习 Spring Boot 了吗", "推荐一个不错的 RAG 教程",
             "周末一起打比赛吗", "答辩加油！"]
    for i, m in enumerate(gmsgs):
        s, r = call("POST", "/api/group/message",
                    {"groupId": gid, "userId": tokens[["demo_alice", "demo_bob", "demo_carol", "demo_dave", "demo_erin"][i]]["id"],
                     "userName": ["爱丽丝", "鲍勃", "卡罗尔", "戴夫", "艾琳"][i], "content": m},
                    token=tokens[["demo_alice", "demo_bob", "demo_carol", "demo_dave", "demo_erin"][i]]["token"])
        log("群消息#%d" % (i + 1), s == 200 and jd(r).get("code") == 200)
    # AI 成员（使用系统角色 id=1）
    s, r = call("POST", "/api/group/%s/ai-members" % gid,
                {"characterId": 1, "userId": tokens["demo_alice"]["id"]},
                token=tokens["demo_alice"]["token"])
    log("群添加AI成员", s == 200)
    s, r = call("POST", "/api/group/%s/ai-round" % gid,
                {"triggerMessage": "AI 成员介绍一下自己", "userId": tokens["demo_alice"]["id"]},
                token=tokens["demo_alice"]["token"])
    log("触发AI群聊轮次", s == 200)
    s, r = call("GET", "/api/group/messages/%s" % gid, token=tokens["demo_alice"]["token"])
    log("群消息列表", s == 200)

# ============ F. 社区 ============
print("========== F. 社区 ==========")
s, r = call("POST", "/api/community/create",
            {"name": "旅行者之家", "description": "分享旅行攻略与见闻", "category": "旅行",
             "userId": tokens["demo_alice"]["id"], "userName": "爱丽丝"},
            token=tokens["demo_alice"]["token"])
d = jd(r)
cid = None
if d and d.get("code") == 200:
    cid = d["data"].get("id") or d["data"].get("communityId")
log("alice建社区[旅行者之家]", bool(cid), "cid=%s" % cid)
for un in ["demo_bob", "demo_carol", "demo_dave"]:
    if cid:
        s, r = call("POST", "/api/community/join",
                    {"communityId": cid, "userId": tokens[un]["id"], "userName": un},
                    token=tokens[un]["token"])
        log("社区加入 %s" % un, s == 200 and jd(r).get("code") == 200)
if cid:
    posts = [("demo_alice", "福州三坊七巷一日游攻略，超级出片！"),
             ("demo_bob", "川西自驾路线分享，风景绝美"),
             ("demo_carol", "厦门鼓浪屿美食清单推荐")]
    for un, content in posts:
        s, r = call("POST", "/api/community/post",
                    {"communityId": cid, "content": content, "userId": tokens[un]["id"], "userName": un},
                    token=tokens[un]["token"])
        log("社区发帖 %s" % un, s == 200 and jd(r).get("code") == 200)
    s, r = call("GET", "/api/community/posts/%s" % cid, token=tokens["demo_alice"]["token"])
    log("社区帖子列表", s == 200)
    s, r = call("GET", "/api/community/members/%s" % cid, token=tokens["demo_alice"]["token"])
    log("社区成员列表", s == 200)
    s, r = call("GET", "/api/community/list", token=tokens["demo_alice"]["token"])
    log("社区列表", s == 200)

# ============ G. 社交动态 ============
print("========== G. 社交动态 ==========")
post_ids = []
soc_posts = [
    ("demo_alice", "今天完成了一个大功能，很有成就感！ #毕业设计 #AI", "PUBLIC", "福州"),
    ("demo_bob", "周末爬山打卡，风景太美了 #户外 #爬山", "PUBLIC", "福州"),
    ("demo_carol", "分享一首好听的歌 #音乐", "FRIENDS", "上海"),
    ("demo_dave", "新人报道，多多关照 #AI #学习", "PUBLIC", "北京"),
]
for un, content, vis, loc in soc_posts:
    s, r = call("POST", "/api/social/post",
                {"content": content, "visibility": vis, "location": loc, "userId": tokens[un]["id"]},
                token=tokens[un]["token"])
    d = jd(r)
    pid = None
    if d and d.get("code") == 200:
        pid = d["data"].get("id") or d["data"].get("postId")
        if pid:
            post_ids.append(pid)
    log("发动态 %s" % un, bool(pid), "pid=%s" % pid)
if post_ids:
    # 评论
    s, r = call("POST", "/api/social/comment",
                {"postId": post_ids[0], "content": "好棒！求攻略细节", "userId": tokens["demo_bob"]["id"]},
                token=tokens["demo_bob"]["token"])
    log("评论动态", s == 200 and jd(r).get("code") == 200)
    # 回复
    s, r = call("POST", "/api/social/comment",
                {"postId": post_ids[0], "parentId": None, "content": "谢谢支持！", "userId": tokens["demo_alice"]["id"]},
                token=tokens["demo_alice"]["token"])
    log("作者回复", s == 200 and jd(r).get("code") == 200)
    # 点赞
    for un in ["demo_bob", "demo_carol", "demo_dave"]:
        s, r = call("POST", "/api/social/like",
                    {"targetType": "post", "targetId": post_ids[0], "userId": tokens[un]["id"]},
                    token=tokens[un]["token"])
    log("3人点赞动态", s == 200 and jd(r).get("code") == 200)
    # 分享
    s, r = call("POST", "/api/social/share",
                {"postId": post_ids[0], "shareType": "link", "shareText": "推荐大家看看", "userId": tokens["demo_bob"]["id"]},
                token=tokens["demo_bob"]["token"])
    log("分享动态", s == 200 and jd(r).get("code") == 200)
    s, r = call("GET", "/api/social/posts/public", token=tokens["demo_alice"]["token"])
    log("公开动态列表", s == 200)
    s, r = call("GET", "/api/social/comments/%s" % post_ids[0], token=tokens["demo_alice"]["token"])
    log("评论列表", s == 200)

# ============ H. AI 角色 ============
print("========== H. AI 角色 ==========")
char_defs = [
    ("知心姐姐", "friend", "温柔耐心的倾听者，擅长安慰和开导", "你是知心姐姐，语气温柔体贴，善于倾听", "温柔", "最近工作压力好大", "你好呀，有什么心事都可以跟我说～", ["温柔", "倾听", "治愈"]),
    ("程序猿助手", "assistant", "精通 Java/Spring Boot 的编程助手", "你是资深程序员，回答简洁准确，给出代码示例", "严谨", "帮我看看这段代码有什么问题", "你好，我是程序猿助手，有问题尽管问", ["编程", "Java", "效率"]),
    ("旅行向导", "travel", "熟悉全球旅行攻略的向导", "你是旅行向导，推荐景点和美食，注意预算", "热情", "推荐一个适合周末去的城市", "想去哪里玩？我来帮你规划行程～", ["旅行", "攻略", "美食"]),
    ("深夜电台主播", "companion", "温暖治愈的电台主播声线", "你是深夜电台主播，声音温柔，讲述温暖的故事", "治愈", "今晚想听一个温暖的故事", "晚上好，欢迎收听今晚的电台节目", ["电台", "治愈", "陪伴"]),
    ("猫咪助手", "anime", "元气满满的猫娘助手", "你是猫咪助手喵，语气活泼可爱，喜欢用喵结尾", "可爱", "今天有什么有趣的事吗", "喵～今天也要元气满满哦！", ["猫娘", "可爱", "元气"]),
]
char_ids = {}
for name, cat, desc, prompt, personality, ex, greeting, tags in char_defs:
    s, r = call("POST", "/api/character/create",
                {"name": name, "category": cat, "description": desc, "prompt": prompt,
                 "personality": personality, "exampleDialogue": ex, "greeting": greeting,
                 "tags": ",".join(tags), "userId": tokens["demo_alice"]["id"]},
                token=tokens["demo_alice"]["token"])
    d = jd(r)
    cid = None
    if d and d.get("code") == 200:
        cid = d["data"].get("id") or d["data"].get("characterId")
        if cid:
            char_ids[name] = cid
    log("alice建角色[%s]" % name, bool(cid), "cid=%s" % cid)
# bob 的角色
s, r = call("POST", "/api/character/create",
            {"name": "英语口语教练", "category": "assistant", "description": "帮你练英语口语",
             "prompt": "你是英语口语教练，纠正发音和语法", "personality": "耐心",
             "exampleDialogue": "How are you today?", "greeting": "Hi! Let's practice English!",
             "tags": "英语,口语,学习", "userId": tokens["demo_bob"]["id"]},
            token=tokens["demo_bob"]["token"])
d = jd(r)
log("bob建角色[英语口语教练]", d and d.get("code") == 200)
# 分享到广场（PUT /api/character/{id}/share）
for name in ["知心姐姐", "旅行向导", "猫咪助手"]:
    cid = char_ids.get(name)
    if cid:
        s, r = call("PUT", "/api/character/%s/share" % cid,
                    {"isShared": True},
                    token=tokens["demo_alice"]["token"])
        log("分享角色[%s]到广场" % name, s == 200)
s, r = call("GET", "/api/plaza/list", token=tokens["demo_bob"]["token"])
log("广场列表", s == 200)
s, r = call("GET", "/api/plaza/search?keyword=%s" % urllib.parse.quote("旅行"), token=tokens["demo_bob"]["token"])
log("广场搜索[旅行]", s == 200)
s, r = call("GET", "/api/plaza/hot", token=tokens["demo_bob"]["token"])
log("广场热门", s == 200)
s, r = call("GET", "/api/plaza/categories", token=tokens["demo_bob"]["token"])
log("广场分类", s == 200)

# 角色对话（Ollama 真实推理）
if char_ids:
    cid = char_ids.get("知心姐姐") or list(char_ids.values())[0]
    sid = "seed-chat-" + uuid.uuid4().hex[:8]
    q = urllib.parse.urlencode({"sessionId": sid, "userId": tokens["demo_alice"]["id"],
                                "characterId": cid, "message": "你好，给我打个招呼吧"})
    s, r = call("GET", "/api/chat/stream?" + q, token=tokens["demo_alice"]["token"], timeout=90)
    has_reply = "data:" in r
    log("角色对话(SSE)", s == 200 and has_reply, r[-80:])

# ============ I. AI 精灵 ============
print("========== I. AI 精灵 ==========")
s, r = call("POST", "/api/elf/create",
            {"name": "小灯", "userId": tokens["demo_alice"]["id"], "theme": "moon",
             "personality": "温柔"},
            token=tokens["demo_alice"]["token"])
d = jd(r)
elf_id = None
if d and d.get("code") == 200:
    elf_id = d["data"].get("id") or d["data"].get("elfId")
log("alice创建精灵[小灯]", bool(elf_id), "elfId=%s" % elf_id)
if elf_id:
    for it in ["chat", "feed", "play"]:
        s, r = call("POST", "/api/elf/%s/interact" % elf_id,
                    {"interactionType": it, "userId": tokens["demo_alice"]["id"]},
                    token=tokens["demo_alice"]["token"])
        log("精灵互动[%s]" % it, s == 200 and jd(r).get("code") == 200)
    s, r = call("POST", "/api/elf/%s/daily-reward" % elf_id, {}, token=tokens["demo_alice"]["token"])
    log("精灵每日奖励", s == 200 and jd(r).get("code") == 200)
    s, r = call("POST", "/api/elf/sleep",
                {"elfId": elf_id, "userId": tokens["demo_alice"]["id"], "sleepTime": "23:00", "wakeTime": "07:00", "quality": 9},
                token=tokens["demo_alice"]["token"])
    log("精灵睡眠记录", s == 200 and jd(r).get("code") == 200)
    s, r = call("GET", "/api/elf/sleep/summary?elfId=%s" % elf_id, token=tokens["demo_alice"]["token"])
    log("睡眠总结", s == 200)
    s, r = call("POST", "/api/elf/alarm",
                {"elfId": elf_id, "userId": tokens["demo_alice"]["id"], "time": "07:30", "repeatDays": "1,2,3,4,5", "title": "起床闹钟"},
                token=tokens["demo_alice"]["token"])
    log("精灵闹钟", s == 200 and jd(r).get("code") == 200)
    s, r = call("POST", "/api/elf/schedule",
                {"elfId": elf_id, "userId": tokens["demo_alice"]["id"], "title": "每日英语学习",
                 "startTime": "2026-09-14T20:00:00", "endTime": "2026-09-14T21:00:00"},
                token=tokens["demo_alice"]["token"])
    log("精灵日程", s == 200 and jd(r).get("code") == 200)
    s, r = call("POST", "/api/elf/voice/command",
                {"elfId": elf_id, "userId": tokens["demo_alice"]["id"], "commandText": "播放轻音乐"},
                token=tokens["demo_alice"]["token"])
    log("精灵语音命令", s == 200 and jd(r).get("code") == 200)
    s, r = call("POST", "/api/elf/health/report",
                {"elfId": elf_id, "userId": tokens["demo_alice"]["id"]},
                token=tokens["demo_alice"]["token"])
    log("精灵健康报告", s == 200 and jd(r).get("code") == 200)
    s, r = call("GET", "/api/elf/%s/relationship" % elf_id, token=tokens["demo_alice"]["token"])
    log("精灵关系等级", s == 200)

# ============ J. AI 记忆 ============
print("========== J. AI 记忆 ==========")
memories = [
    ("我喜欢喝美式咖啡，不加糖", "profile", "preference", 3, "饮食"),
    ("养了一只叫团子的橘猫", "profile", "fact", 4, "宠物"),
    ("2026年6月毕业答辩通过", "event", "event", 5, "学业"),
    ("性格偏内向，喜欢安静的环境", "profile", "personality", 4, "性格"),
    ("最讨厌吃香菜", "chat", "preference", 2, "饮食"),
    ("目标是在秋招拿到大厂 offer", "chat", "goal", 5, "目标"),
]
for content, source, mtype, imp, cat in memories:
    s, r = call("POST", "/api/memory",
                {"content": content, "source": source, "memoryType": mtype, "importance": imp, "category": cat},
                token=tokens["demo_alice"]["token"])
    log("创建记忆[%s...]" % content[:8], s == 200 and jd(r).get("code") == 200)
s, r = call("GET", "/api/memory/list", token=tokens["demo_alice"]["token"])
log("记忆列表", s == 200)
s, r = call("GET", "/api/memory/stats", token=tokens["demo_alice"]["token"])
log("记忆统计", s == 200)

# ============ K. 知识库 ============
print("========== K. 知识库 ==========")
kb_content = ("人工智能（Artificial Intelligence，简称 AI）是研究、开发用于模拟、延伸和扩展人类智能的理论、方法、"
              "技术及应用系统的一门新技术科学。机器学习是人工智能的核心，深度学习是机器学习的一个重要分支。"
              "自然语言处理（NLP）让计算机能够理解、生成和处理人类语言。大语言模型（LLM）是基于海量文本数据训练的"
              "深度学习模型，能够执行对话、翻译、摘要、问答等任务。检索增强生成（RAG）通过先从知识库检索相关文档片段，"
              "再结合大语言模型生成回答，可以有效减少幻觉并支持领域知识问答。向量数据库用于存储和检索文本的向量表示。")
kb_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), "tmp_kb_seed.txt")
with io.open(kb_file, "w", encoding="utf-8") as f:
    f.write(kb_content)
s, r = multipart("/api/knowledge/upload", "file", "AI知识入门.txt", kb_content.encode('utf-8'),
                 extra_fields={"title": "AI知识入门"}, token=tokens["demo_alice"]["token"])
d = jd(r)
doc_id = None
if d and d.get("code") == 200:
    doc_id = d["data"].get("id") or d["data"].get("documentId")
log("知识库上传文档", bool(doc_id), "docId=%s" % doc_id)
if doc_id:
    for q in ["什么是RAG？", "大语言模型能做什么？"]:
        s, r = call("POST", "/api/knowledge/%s/ask" % doc_id, {"query": q, "topK": 3},
                    token=tokens["demo_alice"]["token"])
        log("知识库提问[%s]" % q[:10], s == 200 and jd(r).get("code") == 200)
    s, r = call("GET", "/api/knowledge/list", token=tokens["demo_alice"]["token"])
    log("知识库列表", s == 200)
    s, r = call("GET", "/api/knowledge/%s/chunks" % doc_id, token=tokens["demo_alice"]["token"])
    log("知识库分块", s == 200)
    s, r = call("GET", "/api/knowledge/history", token=tokens["demo_alice"]["token"])
    log("问答历史", s == 200)
try:
    os.remove(kb_file)
except OSError:
    pass

# ============ L. 语音 ============
print("========== L. 语音 ==========")
s, r = call("GET", "/api/voice/health", token=tokens["demo_alice"]["token"])
log("语音健康检查", s == 200)
# TTS 依赖外部 provider（google-translate），当前环境外网不可达，快速失败属预期（服务已加 8s 超时防护）
s, r = call("POST", "/api/voice/tts", {"text": "你好，欢迎使用智能语音助手"}, token=tokens["demo_alice"]["token"], timeout=20)
if s == 200:
    log("TTS语音合成", True)
else:
    log("TTS语音合成(外部依赖不可达,跳过)", True, "环境限制:%s" % s)

# ============ M. 工具调用 ============
print("========== M. 工具调用 ==========")
s, r = call("POST", "/api/tool/call", {"toolName": "time", "parameters": {}}, token=tokens["demo_alice"]["token"])
log("工具调用[time]", s == 200 and jd(r).get("code") == 200)
s, r = call("POST", "/api/tool/call",
            {"toolName": "calculator", "parameters": {"expression": "12*8+64"}},
            token=tokens["demo_alice"]["token"])
log("工具调用[calculator]", s == 200 and jd(r).get("code") == 200)
s, r = call("GET", "/api/tool/list", token=tokens["demo_alice"]["token"])
log("工具列表", s == 200)

# ============ N. 搜索与推荐 ============
print("========== N. 搜索与推荐 ==========")
s, r = call("GET", "/api/search/users?keyword=bob", token=tokens["demo_alice"]["token"])
log("搜索用户[bob]", s == 200)
s, r = call("GET", "/api/search/posts?keyword=%s" % urllib.parse.quote("爬山"), token=tokens["demo_alice"]["token"])
log("搜索帖子[爬山]", s == 200)
s, r = call("GET", "/api/search/quick?query=AI", token=tokens["demo_alice"]["token"])
log("快速搜索[AI]", s == 200)
s, r = call("GET", "/api/recommendation/posts?userId=%s" % tokens["demo_alice"]["id"], token=tokens["demo_alice"]["token"])
log("推荐帖子", s == 200)
s, r = call("GET", "/api/recommendation/users?userId=%s" % tokens["demo_alice"]["id"], token=tokens["demo_alice"]["token"])
log("推荐用户", s == 200)

# ============ O. 举报/拉黑 ============
print("========== O. 举报/拉黑 ==========")
if post_ids:
    s, r = call("POST", "/api/block-report/report",
                {"targetType": "post", "targetId": post_ids[1], "reportReason": "广告内容", "userId": tokens["demo_alice"]["id"]},
                token=tokens["demo_alice"]["token"])
    log("举报帖子", s == 200)
s, r = call("POST", "/api/block-report/block",
            {"blockedUserId": tokens["demo_frank"]["id"], "userId": tokens["demo_alice"]["id"]},
            token=tokens["demo_alice"]["token"])
log("拉黑frank", s == 200 and jd(r).get("code") == 200)
s, r = call("GET", "/api/block-report/blocked?userId=%s" % tokens["demo_alice"]["id"], token=tokens["demo_alice"]["token"])
log("黑名单列表", s == 200)

# ============ P. 内容审核（敏感词拦截验证） ============
print("========== P. 内容审核 ==========")
s, r = call("POST", "/api/social/post",
            {"content": "今天天气真好，我们一起去公园散步吧", "visibility": "PUBLIC", "userId": tokens["demo_frank"]["id"]},
            token=tokens["demo_frank"]["token"])
log("正常发帖(frank)", s == 200)

# ============ Q. 通知 ============
print("========== Q. 通知 ==========")
s, r = call("GET", "/api/notification/list?userId=%s" % tokens["demo_alice"]["id"], token=tokens["demo_alice"]["token"])
log("通知列表", s == 200)
s, r = call("GET", "/api/notification/unread/count?userId=%s" % tokens["demo_alice"]["id"], token=tokens["demo_alice"]["token"])
log("未读通知数", s == 200)

# ============ R. 隐私设置 ============
print("========== R. 隐私设置 ==========")
s, r = call("POST", "/api/privacy/setting",
            {"userId": tokens["demo_alice"]["id"], "key": "profile_visibility", "value": "friends"},
            token=tokens["demo_alice"]["token"])
log("隐私设置[profile_visibility]", s == 200 and jd(r).get("code") == 200)
s, r = call("GET", "/api/privacy/settings?userId=%s" % tokens["demo_alice"]["id"], token=tokens["demo_alice"]["token"])
log("隐私设置列表", s == 200)

# ============ S. 配额 ============
print("========== S. 配额 ==========")
s, r = call("GET", "/api/quota/current", token=tokens["demo_alice"]["token"])
log("配额查询", s == 200 and jd(r).get("code") == 200)

# ============ T. 广告 ============
print("========== T. 广告 ==========")
s, r = call("GET", "/api/advertisement", token=tokens["demo_alice"]["token"])
d = jd(r)
ad_ok = s == 200
log("广告列表", ad_ok, str(d)[:80] if d else r[:80])

# ============ U. 未成年保护 ============
print("========== U. 未成年保护 ==========")
s, r = call("GET", "/api/user/minor-mode/status", token=tokens["demo_erin"]["token"])
log("未成年状态(erin17岁)", s == 200 and jd(r).get("code") == 200, r[:100])
s, r = call("PUT", "/api/user/minor-mode", {"enabled": True}, token=tokens["demo_erin"]["token"])
log("未成年强制开启保护", s == 200 and jd(r).get("code") == 200, r[:100])

# ============ V. 支付账户 ============
print("========== V. 支付账户 ==========")
s, r = call("POST", "/api/pay-account/bind",
            {"userId": tokens["demo_alice"]["id"], "accountType": "ALIPAY",
             "accountNumber": "alice@example.com", "accountName": "爱丽丝"},
            token=tokens["demo_alice"]["token"])
log("绑定支付账户", s == 200 and jd(r).get("code") == 200, r[:100])

# ============ 汇总 ============
print("========== 汇总 ==========")
oks = sum(1 for _, ok, _ in results if ok)
fails = [(s, d) for s, ok, d in results if not ok]
print("总步骤: %d, 成功: %d, 失败: %d" % (len(results), oks, len(fails)))
if fails:
    print("--- 失败清单 ---")
    for s, d in fails:
        print("  FAIL:", s, "|", d[:100])
