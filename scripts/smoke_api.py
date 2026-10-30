# -*- coding: utf-8 -*-
"""
smoke_api.py — 后端接口全量冒烟

流程：
  1. 健康探针 /api/health
  2. 普通用户登录 demo_alice/Test@123456 → user_token
  3. 管理员登录 admin/123456            → admin_token
  4. 按模块遍历关键端点（method/url/鉴权角色/预期状态码）
  5. 记录 status_code + 响应时间(ms) + 是否通过
  6. 慢请求 > SLOW_MS 标记 WARN；失败端点红色高亮

输出：
  - scripts/reports/smoke_report.json
  - 控制台彩色汇总
退出码：存在 P0 失败(非预期状态码) → 1；否则 0
"""
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

GREEN = "\033[92m"; RED = "\033[91m"; YELLOW = "\033[93m"
CYAN = "\033[96m"; BOLD = "\033[1m"; GREY = "\033[90m"; RESET = "\033[0m"

BASE = os.environ.get("API_BASE", "http://127.0.0.1:8080")
REPORT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "reports")
TIMEOUT = 15
SLOW_MS = 3000

USER_CREDS = {"username": "demo_alice", "password": "Test@123456"}
ADMIN_CREDS = {"username": "admin", "password": "123456"}

# demo_alice 固定用户/实体 ID（来自测试账号清单）
UID = "82"      # demo_alice
FRIEND = "83"   # demo_bob
ELF = "2"       # alice 的精灵「小灯」

# 端点矩阵：(模块, method, path, auth角色, 预期状态码集合)
# auth: user=带alice token, admin=带admin token, none=不带token
ENDPOINTS = [
    # ---- 健康 ----
    ("健康",       "GET",    "/api/health",                        "none",  {200}),
    # ---- 认证/用户 ----
    ("认证",       "POST",   "/api/user/login",                    "none",  {200}),
    ("用户",       "GET",    "/api/user/me",                       "user",  {200}),
    ("用户",       "GET",    f"/api/user/profile?userId={UID}",    "user",  {200}),
    ("用户",       "GET",    "/api/user/membership",               "user",  {200}),
    ("用户",       "GET",    f"/api/user/stats?userId={UID}",      "user",  {200}),
    ("用户",       "GET",    f"/api/user/status?userIds={UID}",    "user",  {200}),
    ("用户",       "GET",    "/api/user/minor-mode/status",        "user",  {200}),
    ("用户",       "GET",    "/api/user/voice-preference",         "user",  {200}),
    ("认证设备",   "GET",    "/api/auth/devices",                  "user",  {200}),
    ("认证告警",   "GET",    "/api/auth/alerts",                   "user",  {200}),
    ("认证徽章",   "GET",    "/api/auth/badges",                   "user",  {200}),
    ("认证标签",   "GET",    "/api/auth/tags",                     "user",  {200}),
    # ---- 好友 ----
    ("好友",       "GET",    f"/api/friend/list?userId={UID}",     "user",  {200}),
    ("好友",       "GET",    f"/api/friend/requests?userId={UID}", "user",  {200}),
    ("好友",       "GET",    "/api/friend/search?username=demo_bob", "user", {200}),
    ("好友分组",   "GET",    f"/api/friend/group/list?userId={UID}", "user", {200}),
    # ---- 私聊 ----
    ("私聊",       "GET",    f"/api/friend/chat/list?userId={UID}", "user", {200}),
    ("私聊",       "GET",    f"/api/friend/chat/unread?userId={UID}", "user", {200}),
    ("私聊",       "GET",    f"/api/friend/chat/history?userId={UID}&friendId={FRIEND}", "user", {200}),
    # ---- 群聊 ----
    ("群聊",       "GET",    f"/api/group/my-groups?userId={UID}", "user",  {200}),
    ("群聊",       "GET",    "/api/group/public",                  "user",  {200}),
    ("群聊",       "GET",    "/api/group/activity-ranking",       "user",  {200}),
    # ---- 社群 ----
    ("社群",       "GET",    "/api/community/list",                "user",  {200}),
    ("社群",       "GET",    "/api/community/search?keyword=a",    "user",  {200}),
    # ---- 广场 ----
    ("广场",       "GET",    "/api/plaza/list",                    "none",  {200}),
    ("广场",       "GET",    "/api/plaza/hot",                     "none",  {200}),
    ("广场",       "GET",    "/api/plaza/categories",              "none",  {200}),
    ("广场",       "GET",    "/api/plaza/search?keyword=旅行",      "none",  {200}),
    # ---- 记忆 ----
    ("记忆",       "GET",    "/api/memory/list",                   "user",  {200}),
    ("记忆",       "GET",    "/api/memory/stats",                  "user",  {200}),
    # ---- 通知 ----
    ("通知",       "GET",    f"/api/notification/list?userId={UID}&page=1&size=5", "user", {200}),
    ("通知",       "GET",    f"/api/notification/unread/count?userId={UID}", "user", {200}),
    ("通知",       "GET",    f"/api/notification/settings?userId={UID}", "user", {200}),
    # ---- 投喂/动态 ----
    ("投喂",       "GET",    "/api/feed/history",                  "user",  {200}),
    ("投喂",       "GET",    "/api/feed/stats",                    "user",  {200}),
    ("社交",       "GET",    "/api/social/posts/public",           "user",  {200}),
    ("社交",       "GET",    f"/api/social/posts/user/{UID}",      "user",  {200}),
    ("推荐",       "GET",    f"/api/recommendation/posts?userId={UID}", "user", {200}),
    ("推荐",       "GET",    f"/api/recommendation/users?userId={UID}", "user", {200}),
    # ---- AI 角色/精灵/知识库 ----
    ("AI角色",     "GET",    "/api/character/list",                "user",  {200}),
    ("AI角色",     "GET",    f"/api/character/shared-with-me?userId={UID}", "user", {200}),
    ("AI精灵",     "GET",    f"/api/elf/get?userId={UID}",         "user",  {200}),
    ("AI精灵",     "GET",    f"/api/elf/alarms?elfId={ELF}",       "user",  {200}),
    ("AI精灵",     "GET",    f"/api/elf/schedules?elfId={ELF}",    "user",  {200}),
    ("知识库",     "GET",    "/api/knowledge/list",                "user",  {200}),
    ("知识库",     "GET",    "/api/knowledge/history",             "user",  {200}),
    # ---- 搜索/模型/工具 ----
    ("搜索",       "GET",    "/api/search/users?keyword=a",        "user",  {200}),
    ("搜索",       "GET",    "/api/search/posts?keyword=a",       "user",  {200}),
    ("搜索",       "GET",    "/api/search/quick?query=a",        "user",  {200}),
    ("模型",       "GET",    "/api/model/list",                    "user",  {200}),
    ("工具",       "GET",    "/api/tool/list",                     "user",  {200}),
    ("工具",       "GET",    "/api/tool/history",                  "user",  {200}),
    # ---- 配额/风险/隐私/资料 ----
    ("配额",       "GET",    "/api/quota/current",                 "user",  {200}),
    ("风险",       "GET",    f"/api/risk/history?userId={UID}",   "user",  {200}),
    ("隐私",       "GET",    f"/api/privacy/settings?userId={UID}", "user",  {200}),
    ("资料",       "GET",    f"/api/profile/{UID}",                 "user",  {200}),
    ("资料",       "GET",    f"/api/profile/statistics/{UID}",    "user",  {200}),
    ("举报拉黑",   "GET",    f"/api/block-report/blocked?userId={UID}", "user", {200}),
    # ---- 语音/图片/外部/爬虫/研究 ----
    ("语音",       "GET",    "/api/voice/health",                  "user",  {200, 503}),
    ("图片",       "GET",    "/api/image/health",                  "user",  {200, 503}),
    ("外部API",    "GET",    "/api/external/health",               "admin", {200}),
    ("外部API",    "GET",    "/api/external/providers",            "admin", {200}),
    ("外部API",    "GET",    "/api/external/status",               "admin", {200}),
    ("爬虫",       "GET",    "/api/crawler/stats",                 "admin", {200}),
    ("爬虫",       "GET",    "/api/crawler/health",                "admin", {200}),
    ("深度研究",   "GET",    "/api/research/list",                 "user",  {200}),
    ("聊天",       "GET",    "/api/chat/health",                   "user",  {200}),
    ("聊天",       "GET",    "/api/chat/models",                   "user",  {200}),
    # ---- 广告/公告（公开） ----
    ("广告",       "GET",    "/api/advertisement/active/home",    "none",  {200, 404}),
    ("公告",       "GET",    "/api/admin/announcements/public/active", "none", {200}),
    # ---- 管理端（admin token） ----
    ("管理",       "POST",   "/api/admin/login",                   "none",  {200}),
    ("管理",       "GET",    "/api/admin/users",                   "admin", {200}),
    ("管理",       "GET",    "/api/admin/stats",                   "admin", {200}),
    ("管理",       "GET",    "/api/admin/users/count",             "admin", {200}),
    ("管理",       "GET",    "/api/admin/characters",              "admin", {200}),
    ("管理公告",   "GET",    "/api/admin/announcements/active",    "admin", {200}),
    ("数据池",     "GET",    "/api/admin/data-pool/records",       "admin", {200}),
    ("数据池",     "GET",    "/api/admin/data-pool/config",        "admin", {200}),
    ("数据池",     "GET",    "/api/admin/data-pool/stats",         "admin", {200}),
    ("数据池",     "GET",    "/api/admin/data-pool/types",         "admin", {200}),
    ("数据池",     "GET",    "/api/admin/data-pool/auto-save/status", "admin", {200}),
    ("AI系统",     "GET",    "/api/aisystem/list",                 "admin", {200}),
    ("AI系统",     "GET",    "/api/aisystem/ollama/status",       "admin", {200}),
    ("AI系统",     "GET",    "/api/aisystem/config",               "admin", {200}),
    ("AI系统",     "GET",    "/api/aisystem/stats",                "admin", {200}),
    ("内容审核",   "GET",    "/api/audit/logs",                    "admin", {200}),
    ("内容审核",   "GET",    "/api/audit/stats",                   "admin", {200}),
    ("备份",       "GET",    "/api/admin/backup/list",             "admin", {200}),
    ("备份",       "GET",    "/api/admin/backup/stats",            "admin", {200}),
    ("支付",       "GET",    "/api/payment/pending",              "admin", {200}),
]


def login(kind, creds):
    """登录并返回 token；失败返回 None。"""
    url = f"{BASE}/api/admin/login" if kind == "admin" else f"{BASE}/api/user/login"
    try:
        r = requests.post(url, json=creds, timeout=TIMEOUT)
        d = r.json()
        tok = (d.get("data") or {}).get("token")
        return tok, r.status_code, d.get("code")
    except Exception as e:
        return None, None, str(e)


def auth_header(role, user_tok, admin_tok):
    if role == "user" and user_tok:
        return {"Authorization": f"Bearer {user_tok}"}
    if role == "admin" and admin_tok:
        return {"Authorization": f"Bearer {admin_tok}"}
    return {}


def main():
    os.makedirs(REPORT_DIR, exist_ok=True)
    session = requests.Session()

    print(f"{BOLD}{CYAN}=== 接口冒烟检测 {time.strftime('%Y-%m-%d %H:%M:%S')} ==={RESET}")
    print(f"目标：{BASE}\n")

    # 0. 健康探针
    try:
        h = session.get(f"{BASE}/api/health", timeout=TIMEOUT)
        print(f"健康探针 /api/health → {h.status_code} {h.text[:80]}")
        if h.status_code != 200:
            print(f"{RED}后端不健康，终止冒烟{RESET}")
            return 2
    except Exception as e:
        print(f"{RED}后端不可达：{e}{RESET}")
        return 2

    # 1. 登录
    user_tok, u_code, u_biz = login("user", USER_CREDS)
    admin_tok, a_code, a_biz = login("admin", ADMIN_CREDS)
    print(f"用户登录 demo_alice → http={u_code} biz={u_biz} token={'OK' if user_tok else 'MISS'}")
    print(f"管理登录 admin     → http={a_code} biz={a_biz} token={'OK' if admin_tok else 'MISS'}\n")
    if not user_tok:
        print(f"{RED}用户登录失败，无法继续鉴权冒烟{RESET}")
        return 2

    # 2. 遍历端点
    rows = []
    fail = 0
    warn_slow = 0
    print(f"{'模块':<10}{'方法':<7}{'端点':<42}{'状态':<8}{'耗时':<10}结果")
    print("-" * 92)

    for module, method, path, role, expected in ENDPOINTS:
        # 登录类端点单独处理（已在上面登录阶段验证过，这里直接跳过 POST login 重复）
        url = f"{BASE}{path}"
        headers = auth_header(role, user_tok, admin_tok)
        t0 = time.time()
        try:
            if method == "GET":
                r = session.get(url, headers=headers, timeout=TIMEOUT)
            elif method == "POST":
                # 仅 login 走 POST
                if path.endswith("/api/user/login"):
                    r = session.post(url, json=USER_CREDS, timeout=TIMEOUT)
                elif path.endswith("/api/admin/login"):
                    r = session.post(url, json=ADMIN_CREDS, timeout=TIMEOUT)
                else:
                    r = session.post(url, headers=headers, timeout=TIMEOUT)
            else:
                r = session.request(method, url, headers=headers, timeout=TIMEOUT)
            elapsed = (time.time() - t0) * 1000
            code = r.status_code
        except Exception as e:
            elapsed = (time.time() - t0) * 1000
            code = None
            err = str(e)[:60]
            row = {"module": module, "method": method, "path": path, "role": role,
                   "status": None, "elapsed_ms": round(elapsed, 1), "ok": False, "error": err,
                   "expected": sorted(expected)}
            rows.append(row)
            fail += 1
            print(f"{module:<10}{method:<7}{path:<42}{RED}ERR{RESET:<8}{round(elapsed,1):<10}{RED}FAIL {err}{RESET}")
            continue

        ok = code in expected
        slow = elapsed > SLOW_MS
        if not ok:
            fail += 1
        if slow:
            warn_slow += 1

        status_s = str(code)
        if ok and slow:
            mark = f"{YELLOW}SLOW{RESET}"
        elif ok:
            mark = f"{GREEN}OK{RESET}"
        else:
            mark = f"{RED}FAIL(期望{sorted(expected)}){RESET}"
            status_s = f"{RED}{code}{RESET}"
        spend = f"{round(elapsed,1)}ms"
        print(f"{module:<10}{method:<7}{path:<42}{status_s:<16}{spend:<10}{mark}")

        rows.append({"module": module, "method": method, "path": path, "role": role,
                     "status": code, "elapsed_ms": round(elapsed, 1), "ok": ok,
                     "slow": slow, "expected": sorted(expected)})

    summary = {
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "base": BASE,
        "total": len(rows),
        "pass": sum(1 for r in rows if r["ok"]),
        "fail": fail,
        "slow": warn_slow,
        "user_login_ok": bool(user_tok),
        "admin_login_ok": bool(admin_tok),
        "items": rows,
    }
    out_path = os.path.join(REPORT_DIR, "smoke_report.json")
    with io.open(out_path, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)

    print("-" * 92)
    tag = f"{GREEN}全部通过" if fail == 0 else f"{RED}{fail} 个失败"
    print(f"结果：{tag}{RESET}  通过 {summary['pass']}/{summary['total']}，慢请求 {warn_slow}")
    print(f"报告：{out_path}")
    return 1 if fail > 0 else 0


if __name__ == "__main__":
    sys.exit(main())
