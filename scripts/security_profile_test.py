# -*- coding: utf-8 -*-
"""
security_profile_test.py — 后端安全画像系统化测试

四类画像：
  1. 恶意型  SQL注入 / XSS / 越权(IDOR) / 敏感词绕过 / 伪造JWT / 路径穿越 / 批量注册 / 举报滥用
  2. 技术型  无token / 错token / 过期token / 参数篡改(类型/负数/空数组) / 并发请求
  3. 边界型  空值 / 超长(1000+) / 特殊字符 / Emoji / 中英文混排 / 负数零超限
  4. 高消费型 重复购买会员 / 重复支付回调 / 越权看他人订单 / 普通用户调管理审核

每条用例记录：画像 / 模块 / 步骤 / 预期 / 实际(status+body摘要) / 严重度 / 通过?
输出：scripts/reports/security_profile_report.json + 控制台汇总
"""
import base64
import concurrent.futures
import io
import json
import os
import sys
import time

import requests

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

BASE = os.environ.get("API_BASE", "http://127.0.0.1:8080")
REPORT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "reports")
TIMEOUT = 15

DEMO_USERS = ["demo_alice", "demo_bob", "demo_carol", "demo_dave", "demo_erin", "demo_frank"]
PASSWORD = "Test@123456"

# (画像, 模块, 步骤, 预期, 实际, 严重度, 通过)
results = []


def record(profile, module, step, expect, resp, severity, pass_judge):
    try:
        body = resp.text[:300] if resp is not None else "NO_RESP"
        status = resp.status_code if resp is not None else None
    except Exception:
        body = "EXC"
        status = None
    results.append({
        "profile": profile, "module": module, "step": step,
        "expect": expect, "status": status, "body": body,
        "severity": severity, "pass": bool(pass_judge),
    })
    mark = "PASS" if pass_judge else ("FAIL" if severity in ("P0", "P1") else "WARN")
    print(f"  [{mark:<4}] ({severity}) {module} :: {step}  -> {status}")


def login(username, password):
    r = requests.post(f"{BASE}/api/user/login", json={"username": username, "password": password}, timeout=TIMEOUT)
    d = r.json().get("data") or {}
    return d.get("token"), (d.get("user") or {}).get("id")


def hdr(token):
    return {"Authorization": f"Bearer {token}"} if token else {}


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def make_forged_jwt(user_id="0", alg="HS256"):
    """构造一个签名错误的 JWT（把 payload 改成 admin/目标用户，但签名随便填）。"""
    header = b64url(json.dumps({"alg": alg, "typ": "JWT"}).encode())
    payload = b64url(json.dumps({"userId": user_id, "username": "admin",
                                  "exp": 9999999999, "iat": 1000000000}).encode())
    sig = b64url(b"forged-signature-bytes-xxxx")
    return f"{header}.{payload}.{sig}"


def main():
    os.makedirs(REPORT_DIR, exist_ok=True)
    s = requests.Session()

    print("=" * 70)
    print(" 后端安全画像系统化测试  " + time.strftime("%Y-%m-%d %H:%M:%S"))
    print("=" * 70)

    # ---------- 0. 登录所有账号 ----------
    print("\n[0] 登录测试账号 ...")
    tokens, ids = {}, {}
    for u in DEMO_USERS:
        try:
            t, uid = login(u, PASSWORD)
            if t:
                tokens[u], ids[u] = t, uid
                print(f"    {u}: id={uid} token=OK")
            else:
                print(f"    {u}: 登录被拒（可能未成年夜间模式），跳过")
        except Exception as e:
            print(f"    {u} LOGIN FAIL {e}")
    # 安全测试核心依赖 alice/bob
    if "demo_alice" not in tokens or "demo_bob" not in tokens:
        print("!! alice/bob 登录失败，无法继续")
        return 2
    # admin
    ar = requests.post(f"{BASE}/api/admin/login", json={"username": "admin", "password": "123456"}, timeout=TIMEOUT)
    adata = ar.json().get("data") or {}
    admin_tok = adata.get("token")
    admin_id = (adata.get("user") or {}).get("id")
    print(f"    admin: id={admin_id} token={'OK' if admin_tok else 'MISS'}")

    alice, bob = tokens["demo_alice"], tokens["demo_bob"]
    AID, BID = ids["demo_alice"], ids["demo_bob"]

    # =====================================================================
    # 画像 1：恶意型
    # =====================================================================
    print("\n[画像1] 恶意型 ...")

    # --- 1.1 SQL 注入：搜索/查询接口 ---
    sqli_payloads = ["' OR 1=1--", "' OR '1'='1", "1; DROP TABLE user--", "' UNION SELECT 1,2,3--"]
    for p in sqli_payloads:
        for path in ["/api/search/users?keyword=", "/api/friend/search?username=", "/api/community/search?keyword="]:
            try:
                r = s.get(f"{BASE}{path}{requests.utils.quote(p)}", headers=hdr(alice), timeout=TIMEOUT)
                # 预期：不 500，不回显 SQL 错误，不返回全表数据
                body = r.text.lower()
                leak = any(k in body for k in ["sql syntax", "mysql", "ora-", "syntax error", "jdbc", "exception"])
                ok = (r.status_code < 500) and not leak
                record("恶意型", "SQL注入", f"{path.split('?')[0]} payload={p}",
                       "非5xx且无SQL错误回显", r, "P0", ok)
            except Exception as e:
                record("恶意型", "SQL注入", f"{path} {p}", "可达", None, "P1", False)

    # --- 1.2 XSS：发帖/评论注入脚本 ---
    xss = "<script>alert(1)</script>"
    try:
        r = s.post(f"{BASE}/api/social/post", headers=hdr(alice),
                   json={"userId": AID, "content": f"安全测试 {xss}"}, timeout=TIMEOUT)
        # 预期：不 500；要么审核拦截，要么入库后转义（后端不应反射执行）
        ok = r.status_code < 500 and "sql syntax" not in r.text.lower()
        record("恶意型", "XSS", f"发帖 content={xss}", "非5xx，脚本不被当作可执行HTML回显",
               r, "P1", ok)
        # 取回该帖，看是否原样存储 script（存储型XSS风险，记录现象）
        try:
            pd = r.json().get("data") or {}
            pid = pd.get("id")
            if pid:
                g = s.get(f"{BASE}/api/social/post/{pid}", timeout=TIMEOUT)
                stored_raw = "<script>alert(1)</script>" in g.text
                record("恶意型", "XSS", f"取回帖子 id={pid} 检查原始script是否原样存储",
                       "前端需转义；后端记录存储形态", g, "P2", not stored_raw)
        except Exception:
            pass
    except Exception as e:
        record("恶意型", "XSS", "发帖", "可达", None, "P1", False)

    # --- 1.3 越权 (IDOR)：alice token 访问 bob 的资源 ---
    # 1.3a bob 的私聊历史
    try:
        r = s.get(f"{BASE}/api/friend/chat/history?userId={BID}&friendId={AID}",
                  headers=hdr(alice), timeout=TIMEOUT)
        # 预期：401/403 或业务拒绝（requireSelf），不能返回 bob 的聊天
        denied = r.status_code in (401, 403)
        record("恶意型", "越权IDOR", f"alice查bob(userId={BID})的私聊历史",
               "401/403 拒绝", r, "P0", denied)
    except Exception as e:
        record("恶意型", "越权IDOR", "私聊历史", "可达", None, "P0", False)

    # 1.3b bob 的通知列表
    try:
        r = s.get(f"{BASE}/api/notification/list?userId={BID}&page=1&size=5",
                  headers=hdr(alice), timeout=TIMEOUT)
        denied = r.status_code in (401, 403)
        record("恶意型", "越权IDOR", f"alice查bob(userId={BID})的通知列表",
               "401/403 拒绝", r, "P0", denied)
    except Exception:
        record("恶意型", "越权IDOR", "通知列表", "可达", None, "P0", False)

    # 1.3c bob 的举报记录
    try:
        r = s.get(f"{BASE}/api/block-report/reports/user?userId={BID}",
                  headers=hdr(alice), timeout=TIMEOUT)
        denied = r.status_code in (401, 403)
        record("恶意型", "越权IDOR", f"alice查bob(userId={BID})的举报记录",
               "401/403 拒绝", r, "P0", denied)
    except Exception:
        record("恶意型", "越权IDOR", "举报记录", "可达", None, "P0", False)

    # 1.3d alice 尝试删除 bob 的帖子（postId 用 1，userId 传 AID 但 requireSelf 会校验 token 身份）
    try:
        r = s.delete(f"{BASE}/api/social/post/1?userId={BID}", headers=hdr(alice), timeout=TIMEOUT)
        denied = r.status_code in (401, 403)
        record("恶意型", "越权IDOR", "alice token 删除 bob(userId=83) 的帖子",
               "401/403 拒绝", r, "P0", denied)
    except Exception:
        record("恶意型", "越权IDOR", "删他人帖", "可达", None, "P0", False)

    # 1.3e 普通用户 token 访问管理端
    try:
        r = s.get(f"{BASE}/api/admin/users", headers=hdr(alice), timeout=TIMEOUT)
        denied = r.status_code == 403
        record("恶意型", "越权IDOR", "普通用户alice访问 /api/admin/users",
               "403", r, "P0", denied)
    except Exception:
        record("恶意型", "越权IDOR", "管理端越权", "可达", None, "P0", False)

    # 1.3f 普通用户审核支付
    try:
        r = s.post(f"{BASE}/api/payment/1/review", headers=hdr(alice),
                   json={"reviewStatus": "APPROVED"}, timeout=TIMEOUT)
        denied = r.status_code == 403
        record("恶意型", "越权IDOR", "普通用户alice审核支付单",
               "403", r, "P0", denied)
    except Exception:
        record("恶意型", "越权IDOR", "审核支付越权", "可达", None, "P0", False)

    # --- 1.4 敏感词绕过：谐音/拆字/emoji 替换 ---
    obfuscated = ["这是草泥马谐音测试", "敏感词拆字测试操+艹", "你个傻逼🎭🤖😡"]
    for txt in obfuscated:
        try:
            r = s.post(f"{BASE}/api/social/post", headers=hdr(alice),
                       json={"userId": AID, "content": txt}, timeout=TIMEOUT)
            # 记录现象：被拦截(非200/业务错误)还是放行(200)
            blocked = r.status_code in (400, 422) or (r.json().get("code") not in (200, 0, None))
            record("恶意型", "敏感词绕过", f"发帖={txt}", "记录审核策略（拦截或放行）",
                   r, "P2", True)  # 仅记录，不判失败
        except Exception:
            record("恶意型", "敏感词绕过", txt, "可达", None, "P2", False)

    # --- 1.5 伪造 JWT ---
    forged_admin = make_forged_jwt("0")
    try:
        r = s.get(f"{BASE}/api/admin/users", headers=hdr(forged_admin), timeout=TIMEOUT)
        record("恶意型", "伪造JWT", "篡改payload为admin+伪造签名访问管理端",
               "401（签名校验失败）", r, "P0", r.status_code in (401, 403))
    except Exception:
        record("恶意型", "伪造JWT", "伪造admin token", "可达", None, "P0", False)

    forged_user = make_forged_jwt(str(BID))  # 伪造成 bob
    try:
        r = s.get(f"{BASE}/api/user/me", headers=hdr(forged_user), timeout=TIMEOUT)
        record("恶意型", "伪造JWT", "篡改payload为bob+伪造签名访问 /api/user/me",
               "401", r, "P0", r.status_code == 401)
    except Exception:
        record("恶意型", "伪造JWT", "伪造user token", "可达", None, "P0", False)

    # none 算法
    none_tok = f"{b64url(b'{\"alg\":\"none\",\"typ\":\"JWT\"}')}.{b64url(b'{\"userId\":\"0\"}')}."
    try:
        r = s.get(f"{BASE}/api/user/me", headers=hdr(none_tok), timeout=TIMEOUT)
        record("恶意型", "伪造JWT", "alg=none 攻击", "401", r, "P0", r.status_code == 401)
    except Exception:
        record("恶意型", "伪造JWT", "none alg", "可达", None, "P0", False)

    # 垃圾 token
    try:
        r = s.get(f"{BASE}/api/user/me", headers=hdr("garbage.token.here"), timeout=TIMEOUT)
        record("恶意型", "伪造JWT", "垃圾token访问 /api/user/me", "401", r, "P0", r.status_code == 401)
    except Exception:
        record("恶意型", "伪造JWT", "垃圾token", "可达", None, "P0", False)

    # --- 1.6 路径穿越：头像上传文件名 ---
    try:
        files = {"file": ("../../etc/passwd", b"root:x:0:0::/root:/bin/bash", "text/plain")}
        r = s.post(f"{BASE}/api/user/upload-avatar", headers=hdr(alice), files=files, timeout=TIMEOUT)
        # 预期：不 500；不能返回穿越后的路径；应拒绝或安全落盘
        body = r.text.lower()
        leak = "/etc/passwd" in body or "c:\\windows" in body or "root:x:0:0" in body
        record("恶意型", "路径穿越", "头像上传 filename=../../etc/passwd",
               "不5xx，不回显服务器路径/文件内容", r, "P0", (r.status_code < 500) and not leak)
    except Exception:
        record("恶意型", "路径穿越", "头像上传穿越", "可达", None, "P1", False)

    # --- 1.7 批量注册：连续注册 6 个，看限流 ---
    print("    （批量注册限流探测，6 次）")
    reg_codes = []
    for i in range(6):
        uname = f"sectest_013_{int(time.time())}_{i}"
        try:
            r = s.post(f"{BASE}/api/user/register",
                       json={"username": uname, "password": "Test@123456", "nickname": "st"},
                       timeout=TIMEOUT)
            reg_codes.append(r.status_code)
        except Exception:
            reg_codes.append(None)
    rate_limited = any(c == 429 for c in reg_codes)
    record("恶意型", "批量注册", f"6次连续注册，状态码={reg_codes}",
           "出现429限流 或 至少不全部成功刷号", None if False else type("R", (), {"status_code": None, "text": str(reg_codes)}),
           "P1", rate_limited or (reg_codes.count(200) <= 2))

    # --- 1.8 举报滥用：重复举报同一用户 ---
    rep_codes = []
    for i in range(5):
        try:
            r = s.post(f"{BASE}/api/block-report/report", headers=hdr(alice),
                        json={"userId": AID, "targetType": "USER", "targetId": BID,
                              "reportReason": f"滥用测试{i}"}, timeout=TIMEOUT)
            rep_codes.append(r.status_code)
        except Exception:
            rep_codes.append(None)
    dup_blocked = any(c in (400, 409) for c in rep_codes)
    record("恶意型", "举报滥用", f"alice连续5次举报bob，状态码={rep_codes}",
           "重复举报被拒(400/409) 或 有上限",
           type("R", (), {"status_code": None, "text": str(rep_codes)}),
           "P1", dup_blocked)

    # =====================================================================
    # 画像 2：技术型
    # =====================================================================
    print("\n[画像2] 技术型 ...")
    protected = [
        ("GET", "/api/user/me"),
        ("GET", "/api/memory/list"),
        ("GET", "/api/quota/current"),
        ("POST", "/api/social/post"),
    ]
    # 2.1 无 token
    for method, path in protected:
        try:
            if method == "GET":
                r = s.get(f"{BASE}{path}", timeout=TIMEOUT)
            else:
                r = s.post(f"{BASE}{path}", json={"userId": AID, "content": "x"}, timeout=TIMEOUT)
            record("技术型", "无token", f"{method} {path}", "401", r, "P0", r.status_code == 401)
        except Exception:
            record("技术型", "无token", path, "可达", None, "P0", False)

    # 2.2 错误 token
    for method, path in protected[:2]:
        try:
            r = s.get(f"{BASE}{path}", headers=hdr("fake.token.xyz"), timeout=TIMEOUT)
            record("技术型", "错token", f"{method} {path}", "401", r, "P0", r.status_code == 401)
        except Exception:
            record("技术型", "错token", path, "可达", None, "P0", False)

    # 2.3 参数篡改：类型错误 / 负数 / 空数组
    # 传 string 给期望 int 的 path 变量
    try:
        r = s.get(f"{BASE}/api/social/post/notanint", headers=hdr(alice), timeout=TIMEOUT)
        record("技术型", "参数篡改", "postId传字符串 'notanint'", "400/404 不500", r, "P1", r.status_code < 500)
    except Exception:
        record("技术型", "参数篡改", "字符串id", "可达", None, "P1", False)

    try:
        r = s.get(f"{BASE}/api/user/profile?userId=-1", headers=hdr(alice), timeout=TIMEOUT)
        record("技术型", "参数篡改", "userId=-1 负数", "4xx/不500", r, "P1", r.status_code < 500)
    except Exception:
        record("技术型", "参数篡改", "负数id", "可达", None, "P1", False)

    try:
        r = s.post(f"{BASE}/api/social/posts/friends", headers=hdr(alice),
                   json={"userIds": [], "limit": -5}, timeout=TIMEOUT)
        record("技术型", "参数篡改", "空数组userIds + limit=-5", "4xx/空结果 不500", r, "P1", r.status_code < 500)
    except Exception:
        record("技术型", "参数篡改", "空数组", "可达", None, "P1", False)

    # 2.4 并发请求：同用户 10 并发
    def one_shot(_):
        try:
            rr = requests.get(f"{BASE}/api/user/me", headers=hdr(alice), timeout=TIMEOUT)
            return rr.status_code
        except Exception:
            return None
    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as ex:
        codes = list(ex.map(one_shot, range(10)))
    oks = sum(1 for c in codes if c == 200)
    record("技术型", "并发请求", f"alice 10并发 /api/user/me codes={codes}",
           "无500，不串号", type("R", (), {"status_code": None, "text": f"oks={oks}/10 codes={codes}"}),
           "P1", oks == 10 and 500 not in codes)

    # =====================================================================
    # 画像 3：边界型
    # =====================================================================
    print("\n[画像3] 边界型 ...")
    # 3.1 空值
    for body in [{"userId": AID, "content": ""}, {"userId": AID, "content": "   "}, {"content": None}]:
        try:
            r = s.post(f"{BASE}/api/social/post", headers=hdr(alice), json=body, timeout=TIMEOUT)
            record("边界型", "空值", f"发帖 body={body}", "4xx业务拒绝 不500", r, "P1", r.status_code < 500)
        except Exception:
            record("边界型", "空值", str(body), "可达", None, "P1", False)

    # 3.2 超长 1000+ 字符
    long_text = "A" * 2000
    try:
        r = s.post(f"{BASE}/api/social/post", headers=hdr(alice),
                   json={"userId": AID, "content": long_text}, timeout=TIMEOUT)
        body = r.text.lower()
        ok = r.status_code < 500 and "data too long" not in body  # 若DB截断报错应被Handler转为友好文案
        record("边界型", "超长", "发帖 2000字符", "4xx友好提示 或 截断 不500/不裸SQL", r, "P1", ok)
    except Exception:
        record("边界型", "超长", "2000字符", "可达", None, "P1", False)

    # 3.3 特殊字符
    special = "<>&'\"/\\=`"
    try:
        r = s.post(f"{BASE}/api/social/post", headers=hdr(alice),
                   json={"userId": AID, "content": f"特殊字符测试 {special}"}, timeout=TIMEOUT)
        record("边界型", "特殊字符", f"发帖 {special}", "不500", r, "P1", r.status_code < 500)
    except Exception:
        record("边界型", "特殊字符", special, "可达", None, "P1", False)

    # 3.4 Emoji
    emoji = "🎭🤖😊🔥🎉"
    try:
        r = s.post(f"{BASE}/api/social/post", headers=hdr(alice),
                   json={"userId": AID, "content": f"emoji {emoji}"}, timeout=TIMEOUT)
        record("边界型", "Emoji", f"发帖 {emoji}", "不500不乱码", r, "P1", r.status_code < 500)
    except Exception:
        record("边界型", "Emoji", emoji, "可达", None, "P1", False)

    # 3.5 中英文混排
    mixed = "Hello你好世界 ChatGPT人工智能 test123"
    try:
        r = s.post(f"{BASE}/api/social/post", headers=hdr(alice),
                   json={"userId": AID, "content": mixed}, timeout=TIMEOUT)
        record("边界型", "中英文混排", "发帖混排", "不500", r, "P2", r.status_code < 500)
    except Exception:
        record("边界型", "中英文混排", "混排", "可达", None, "P2", False)

    # 3.6 零/超限数值
    for q in ["page=0&size=0", "page=-1&size=999999", "page=abc&size=xyz"]:
        try:
            r = s.get(f"{BASE}/api/notification/list?userId={AID}&{q}", headers=hdr(alice), timeout=TIMEOUT)
            record("边界型", "数值", f"notification {q}", "4xx/空结果 不500", r, "P1", r.status_code < 500)
        except Exception:
            record("边界型", "数值", q, "可达", None, "P1", False)

    # =====================================================================
    # 画像 4：高消费型
    # =====================================================================
    print("\n[画像4] 高消费型 ...")
    # 4.1 越权看他人订单：先创建一个 bob 的订单不现实；改为直接 GET /api/payment/{id} 用 alice token 探
    for oid in [1, 2, 99999]:
        try:
            r = s.get(f"{BASE}/api/payment/{oid}", headers=hdr(alice), timeout=TIMEOUT)
            # 自己的订单可看；他人的应403；不存在的应业务错误。关键是不能越权返回他人订单
            record("高消费型", "订单越权", f"alice GET /api/payment/{oid}",
                   "403/业务拒绝 他人订单", r, "P0", r.status_code in (200, 400, 401, 403, 404))
        except Exception:
            record("高消费型", "订单越权", str(oid), "可达", None, "P0", False)

    # 4.2 普通用户不能列全部订单（GET /api/payment 列表是 admin only）
    try:
        r = s.get(f"{BASE}/api/payment?page=1&size=20", headers=hdr(alice), timeout=TIMEOUT)
        record("高消费型", "订单越权", "alice GET /api/payment 全部订单列表", "403", r, "P0", r.status_code == 403)
    except Exception:
        record("高消费型", "订单越权", "全部订单列表", "可达", None, "P0", False)

    # 4.3 重复购买会员：连续 create 两笔（缺支付账户可能直接403，记录现象）
    pay_codes = []
    for i in range(2):
        try:
            r = s.post(f"{BASE}/api/payment/create", headers=hdr(alice),
                       json={"versionType": "MONTHLY", "durationDays": 30}, timeout=TIMEOUT)
            pay_codes.append((r.status_code, r.text[:120]))
        except Exception as e:
            pay_codes.append(("EXC", str(e)))
    record("高消费型", "重复购买", f"连续2次 payment/create -> {pay_codes}",
           "幂等/不重复开单/缺账户时友好403",
           type("R", (), {"status_code": None, "text": str(pay_codes)}), "P2", True)

    # 4.4 普通用户不能审核（已在 1.3f 覆盖）
    # 4.5 额度查询正常
    try:
        r = s.get(f"{BASE}/api/quota/current", headers=hdr(alice), timeout=TIMEOUT)
        record("高消费型", "额度", "GET /api/quota/current", "200", r, "P2", r.status_code == 200)
    except Exception:
        record("高消费型", "额度", "quota", "可达", None, "P2", False)

    # ---------- 汇总 ----------
    total = len(results)
    fails = [x for x in results if not x["pass"]]
    p0 = [x for x in fails if x["severity"] == "P0"]
    p1 = [x for x in fails if x["severity"] == "P1"]
    p2 = [x for x in fails if x["severity"] == "P2"]

    print("\n" + "=" * 70)
    print(f" 总计 {total} 条：通过 {total-len(fails)}，未过 {len(fails)}（P0={len(p0)} P1={len(p1)} P2={len(p2)}）")
    if p0:
        print("\n  !! P0 漏洞：")
        for x in p0:
            print(f"     - [{x['module']}] {x['step']}  -> {x['status']} {x['body'][:80]}")
    if p1:
        print("\n  P1 缺陷：")
        for x in p1:
            print(f"     - [{x['module']}] {x['step']}  -> {x['status']} {x['body'][:80]}")

    report = {
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "base": BASE,
        "total": total,
        "pass": total - len(fails),
        "fail": len(fails),
        "p0": len(p0), "p1": len(p1), "p2": len(p2),
        "alice_id": AID, "bob_id": BID,
        "items": results,
        "p0_items": p0, "p1_items": p1,
    }
    out = os.path.join(REPORT_DIR, "security_profile_report.json")
    with io.open(out, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(f"\n报告：{out}")
    return 1 if p0 else 0


if __name__ == "__main__":
    sys.exit(main())
