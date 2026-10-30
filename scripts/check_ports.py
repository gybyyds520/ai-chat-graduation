# -*- coding: utf-8 -*-
"""
check_ports.py — 端口与进程健康检测

检测项：
  1. TCP 端口监听探测（socket.connect_ex）
  2. HTTP 健康端点探活（200 视为健康）
  3. 端口对应进程 PID（Windows: netstat -ano | findstr :PORT）

端口矩阵：
  5187  Vite 前端 dev server
  8080  后端 Spring Boot API
  11434 Ollama 本地推理
  3306  MySQL

输出：
  - 控制台彩色结果
  - scripts/reports/ports_report.json
退出码：全部端口健康=0，否则=1
"""
import io
import json
import os
import re
import socket
import subprocess
import sys
import time

# 解决 Windows 控制台中文/ANSI 编码
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

# 颜色（ANSI）
GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
CYAN = "\033[96m"
BOLD = "\033[1m"
RESET = "\033[0m"

REPORT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "reports")

# 端口定义：端口、用途、健康探测 URL（None=仅 TCP）、期望状态码
PORTS = [
    {"port": 5187,  "name": "Vite 前端",     "host": "127.0.0.1", "health_url": "http://127.0.0.1:5187/", "expect": 200},
    {"port": 8080,  "name": "后端 API",      "host": "127.0.0.1", "health_url": "http://127.0.0.1:8080/api/health", "expect": 200},
    {"port": 11434, "name": "Ollama 推理",   "host": "127.0.0.1", "health_url": "http://127.0.0.1:11434/api/tags", "expect": 200},
    {"port": 3306,  "name": "MySQL",         "host": "127.0.0.1", "health_url": None, "expect": None},
]


def port_listening(host, port, timeout=1.5):
    """返回 (是否监听, 耗时ms)"""
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(timeout)
    start = time.time()
    try:
        s.connect((host, port))
        elapsed = (time.time() - start) * 1000
        s.close()
        return True, round(elapsed, 1)
    except Exception:
        return False, None


def http_probe(url, expect, timeout=5):
    """轻量 HTTP 探测，仅依赖标准库 urllib，避免 requests 缺失时崩溃。"""
    import urllib.request
    import urllib.error
    start = time.time()
    try:
        req = urllib.request.Request(url, method="GET", headers={"User-Agent": "health-check"})
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            elapsed = (time.time() - start) * 1000
            code = resp.getcode()
            ok = (code == expect) if expect else (200 <= code < 500)
            body = ""
            try:
                body = resp.read(200).decode("utf-8", errors="replace")
            except Exception:
                pass
            return {"ok": ok, "status": code, "elapsed_ms": round(elapsed, 1), "body": body[:120]}
    except urllib.error.HTTPError as e:
        elapsed = (time.time() - start) * 1000
        ok = (e.code == expect) if expect else False
        return {"ok": ok, "status": e.code, "elapsed_ms": round(elapsed, 1), "body": ""}
    except Exception as e:
        return {"ok": False, "status": None, "elapsed_ms": None, "body": f"ERR: {e}"}


def pid_for_port(port):
    """Windows: netstat -ano | findstr LISTENING 解析 PID"""
    try:
        out = subprocess.run(
            ["netstat", "-ano", "-p", "TCP"],
            capture_output=True, text=True, timeout=10, encoding="utf-8", errors="replace"
        ).stdout
        for line in out.splitlines():
            if f":{port} " in line and "LISTENING" in line.upper():
                parts = line.split()
                return parts[-1].strip()
    except Exception:
        return None
    return None


def main():
    os.makedirs(REPORT_DIR, exist_ok=True)
    results = []
    all_ok = True

    print(f"{BOLD}{CYAN}=== 端口健康检测 {time.strftime('%Y-%m-%d %H:%M:%S')} ==={RESET}\n")
    print(f"{'端口':<8}{'用途':<14}{'监听':<8}{'健康探测':<10}{'PID':<10}{'耗时'}")
    print("-" * 70)

    for item in PORTS:
        port = item["port"]
        listening, l_ms = port_listening(item["host"], port)
        pid = pid_for_port(port) if listening else None
        http_info = {"ok": False, "status": None, "elapsed_ms": None}
        if item["health_url"]:
            if listening:
                http_info = http_probe(item["health_url"], item["expect"])
            else:
                http_info = {"ok": False, "status": None, "elapsed_ms": None, "body": "端口未监听"}
        else:
            # 仅 TCP 的端口（MySQL），监听即视为健康
            http_info = {"ok": listening, "status": "TCP", "elapsed_ms": l_ms}

        ok = listening and http_info["ok"]
        if not ok:
            all_ok = False

        l_mark = f"{GREEN}YES{RESET}" if listening else f"{RED}NO{RESET}"
        h_mark = f"{GREEN}{http_info['status']}{RESET}" if http_info["ok"] else f"{RED}{http_info['status'] or 'FAIL'}{RESET}"
        spend = http_info.get("elapsed_ms") or l_ms or "-"

        print(f"{port:<8}{item['name']:<14}{l_mark:<14}{h_mark:<16}{str(pid or '-'):<10}{spend}ms")
        results.append({
            "port": port, "name": item["name"], "listening": listening,
            "pid": pid, "health_status": http_info["status"],
            "health_ok": http_info["ok"], "elapsed_ms": http_info.get("elapsed_ms"),
            "body": http_info.get("body", "")[:120], "ok": ok,
        })

    summary = {
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "all_ok": all_ok,
        "total": len(results),
        "healthy": sum(1 for r in results if r["ok"]),
        "items": results,
    }
    out_path = os.path.join(REPORT_DIR, "ports_report.json")
    with io.open(out_path, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)

    print("-" * 70)
    tag = f"{GREEN}全部正常" if all_ok else f"{RED}存在异常"
    print(f"结果：{tag}{RESET}  {summary['healthy']}/{summary['total']} 端口健康")
    print(f"报告：{out_path}")
    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main())
