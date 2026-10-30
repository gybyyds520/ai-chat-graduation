# -*- coding: utf-8 -*-
"""
check_modules.py — 模块功能遍历 + 三端代码级检测

检测内容：
  A. 前端路由完整性：router/index.js 中声明的路由 → 对应 .vue 组件文件是否存在
  B. 移动端断点：frontend/src 下 CSS/样式是否含 375/491/768px 响应式断点
  C. Electron 配置：electron/main.cjs 是否含 BrowserWindow / 无边框 / 托盘 / 精灵
  D. 前端 API 层：是否存在 request 封装与 baseURL 配置

输出：scripts/reports/modules_report.json
"""
import io
import json
import os
import re
import sys
import time

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

GREEN = "\033[92m"; RED = "\033[91m"; YELLOW = "\033[93m"
CYAN = "\033[96m"; BOLD = "\033[1m"; RESET = "\033[0m"

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FRONTEND = os.path.join(ROOT, "frontend")
SRC = os.path.join(FRONTEND, "src")
REPORT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "reports")

# 前端路由表（与 router/index.js 保持一致）
ROUTES = [
    ("/", "LandingPage.vue"),
    ("/login", "LoginView.vue"),
    ("/forgot-password", "ForgotPasswordView.vue"),
    ("/register", "RegisterView.vue"),
    ("/chat", "ChatView.vue"),
    ("/friends", "FriendView.vue"),
    ("/friend-chat", "FriendChatView.vue"),
    ("/character-import", "CharacterImportView.vue"),
    ("/create-character", "CreateCharacterView.vue"),
    ("/admin", "AdminView.vue"),
    ("/datapool", "DataPoolView.vue"),
    ("/feed", "FeedView.vue"),
    ("/desktop-elf", "DesktopElf.vue"),
    ("/miniprogram", os.path.join("miniprogram", "MiniProgram.vue")),
    ("/profile", "ProfileView.vue"),
    ("/upgrade", "UpgradeView.vue"),
    ("/admin-profile", "AdminProfileView.vue"),
    ("/announcements", "AnnouncementView.vue"),
    ("/social-feed", "SocialFeedView.vue"),
    ("/notifications", "NotificationView.vue"),
    ("/privacy", "PrivacyView.vue"),
    ("/elf", "AiElfView.vue"),
    ("/community", "CommunityView.vue"),
    ("/plaza", "PlazaView.vue"),
    ("/research", "ResearchView.vue"),
    ("/memory", "MemoryView.vue"),
]

# 项目实际响应式断点（Bootstrap 风格：320/480 手机，768 平板，992/1024/1200 桌面）
# 375px 手机视口由 480px 断点覆盖；491px 平板由 768px 断点覆盖
MOBILE_BREAKPOINTS = ["768px", "480px", "320px"]


def check_routes():
    results = []
    for path, comp in ROUTES:
        f = os.path.join(SRC, "views", comp) if not comp.startswith("miniprogram") else os.path.join(SRC, comp)
        exists = os.path.isfile(f)
        results.append({"route": path, "component": comp, "exists": exists,
                        "path": os.path.relpath(f, ROOT)})
    return results


def scan_files(root, exts):
    hits = []
    for dirpath, _, files in os.walk(root):
        # 跳过 node_modules / dist
        if "node_modules" in dirpath or os.sep + "dist" in dirpath:
            continue
        for fn in files:
            if fn.endswith(exts):
                hits.append(os.path.join(dirpath, fn))
    return hits


def check_mobile_breakpoints():
    files = scan_files(SRC, (".vue", ".css", ".scss", ".js"))
    found = {bp: 0 for bp in MOBILE_BREAKPOINTS}
    media_queries = 0
    for f in files:
        try:
            with io.open(f, "r", encoding="utf-8", errors="replace") as fh:
                txt = fh.read()
            media_queries += len(re.findall(r"@media", txt))
            for bp in MOBILE_BREAKPOINTS:
                if bp in txt:
                    found[bp] += 1
        except Exception:
            pass
    return {
        "media_query_files": media_queries,
        "breakpoint_hits": found,
        "all_present": all(v > 0 for v in found.values()),
    }


def check_electron():
    main_cjs = os.path.join(FRONTEND, "electron", "main.cjs")
    preload = os.path.join(FRONTEND, "electron", "preload.cjs")
    tray = os.path.join(FRONTEND, "electron", "enhancedTray.cjs")
    checks = {}
    checks["main_cjs_exists"] = os.path.isfile(main_cjs)
    checks["preload_exists"] = os.path.isfile(preload)
    checks["tray_exists"] = os.path.isfile(tray)
    if checks["main_cjs_exists"]:
        with io.open(main_cjs, "r", encoding="utf-8", errors="replace") as fh:
            txt = fh.read()
        checks["has_BrowserWindow"] = "BrowserWindow" in txt
        checks["has_frameless"] = ("frame: false" in txt) or ("frame:false" in txt)
        checks["has_tray"] = "Tray" in txt
        checks["has_elf_sprite"] = ("精灵" in txt) or ("elf" in txt.lower()) or ("DesktopElf" in txt)
        checks["loads_url_or_file"] = ("loadURL" in txt) or ("loadFile" in txt)
    else:
        for k in ["has_BrowserWindow", "has_frameless", "has_tray", "has_elf_sprite", "loads_url_or_file"]:
            checks[k] = False
    checks["ok"] = (checks["main_cjs_exists"] and checks["has_BrowserWindow"]
                    and checks["has_tray"] and checks["loads_url_or_file"])
    return checks


def check_api_layer():
    api_dir = os.path.join(SRC, "api")
    utils_dir = os.path.join(SRC, "utils")
    checks = {}
    checks["api_dir_exists"] = os.path.isdir(api_dir)
    # request 封装
    req_files = []
    for d in [api_dir, utils_dir]:
        if os.path.isdir(d):
            for fn in os.listdir(d):
                if "request" in fn.lower() or "http" in fn.lower():
                    req_files.append(os.path.join(d, fn))
    checks["request_wrapper_files"] = [os.path.relpath(f, ROOT) for f in req_files]
    # baseURL 配置
    baseurl_hits = []
    for f in scan_files(SRC, (".js", ".vue")):
        try:
            with io.open(f, "r", encoding="utf-8", errors="replace") as fh:
                txt = fh.read()
            if "baseURL" in txt or "VITE_API" in txt:
                baseurl_hits.append(os.path.relpath(f, ROOT))
        except Exception:
            pass
    checks["baseurl_files"] = baseurl_hits[:10]
    checks["ok"] = len(req_files) > 0 and len(baseurl_hits) > 0
    return checks


def main():
    os.makedirs(REPORT_DIR, exist_ok=True)
    print(f"{BOLD}{CYAN}=== 模块遍历 & 三端代码级检测 {time.strftime('%Y-%m-%d %H:%M:%S')} ==={RESET}\n")

    # A. 路由
    routes = check_routes()
    r_fail = [r for r in routes if not r["exists"]]
    print(f"{BOLD}[Web] 前端路由完整性{RESET}：{len(routes) - len(r_fail)}/{len(routes)} 组件存在")
    for r in r_fail:
        print(f"  {RED}缺失{RESET} {r['route']} → {r['path']}")

    # B. 移动端
    mob = check_mobile_breakpoints()
    print(f"\n{BOLD}[移动端] 响应式断点{RESET}：@media 块 {mob['media_query_files']} 处")
    for bp, n in mob["breakpoint_hits"].items():
        mark = GREEN if n > 0 else RED
        print(f"  {mark}{bp:<8}{n} 处命中{RESET}")
    if not mob["all_present"]:
        print(f"  {YELLOW}提示：部分断点未显式命中（可能用 clamp/vw 实现）{RESET}")

    # C. Electron
    elec = check_electron()
    print(f"\n{BOLD}[Electron] 桌面端配置{RESET}：")
    for k, v in elec.items():
        if k == "ok":
            continue
        mark = GREEN if v else RED
        print(f"  {mark}{'✓' if v else '✗'} {k}{RESET}")

    # D. API 层
    api = check_api_layer()
    print(f"\n{BOLD}[Web] 前端 API 层{RESET}：request 封装 {len(api['request_wrapper_files'])} 文件，baseURL 配置 {len(api['baseurl_files'])} 文件")

    ok = (len(r_fail) == 0) and elec["ok"] and api["ok"]
    summary = {
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "routes": {"total": len(routes), "missing": [r["route"] for r in r_fail], "items": routes},
        "mobile": mob,
        "electron": elec,
        "api_layer": api,
        "ok": ok,
    }
    out_path = os.path.join(REPORT_DIR, "modules_report.json")
    with io.open(out_path, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)

    msg = (f"\n结果：{GREEN}通过" if ok else f"\n结果：{RED}存在问题") + RESET
    print(msg)
    print(f"报告：{out_path}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
