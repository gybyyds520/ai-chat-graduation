# -*- coding: utf-8 -*-
"""
run_all_checks.py — 一键运行全部健康检测

顺序：
  1. check_ports   端口健康
  2. smoke_api     接口冒烟（依赖端口 8080 可达）
  3. check_modules 模块遍历 & 三端代码级

汇总三份 JSON，生成总览，并在任一关键步骤失败时以非 0 退出。

用法：
  python scripts/run_all_checks.py
环境变量：
  API_BASE  覆盖后端地址（默认 http://127.0.0.1:8080）
"""
import io
import json
import os
import subprocess
import sys
import time

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

GREEN = "\033[92m"; RED = "\033[91m"; YELLOW = "\033[93m"
CYAN = "\033[96m"; BOLD = "\033[1m"; RESET = "\033[0m"

HERE = os.path.dirname(os.path.abspath(__file__))
REPORT_DIR = os.path.join(HERE, "reports")


def run_step(name, script):
    path = os.path.join(HERE, script)
    print(f"\n{BOLD}{CYAN}########## {name} ##########{RESET}")
    proc = subprocess.run([sys.executable, path], cwd=os.path.dirname(HERE))
    return proc.returncode


def load_json(name):
    p = os.path.join(REPORT_DIR, name)
    if not os.path.isfile(p):
        return None
    with io.open(p, "r", encoding="utf-8") as f:
        return json.load(f)


def main():
    os.makedirs(REPORT_DIR, exist_ok=True)
    print(f"{BOLD}{'='*60}\n  一键健康检测 {time.strftime('%Y-%m-%d %H:%M:%S')}\n{'='*60}{RESET}")

    rc_ports = run_step("1/3 端口检测", "check_ports.py")
    rc_smoke = run_step("2/3 接口冒烟", "smoke_api.py")
    rc_mods = run_step("3/3 模块遍历/三端", "check_modules.py")

    ports = load_json("ports_report.json")
    smoke = load_json("smoke_report.json")
    mods = load_json("modules_report.json")

    overview = {
        "timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
        "steps": {
            "check_ports": rc_ports,
            "smoke_api": rc_smoke,
            "check_modules": rc_mods,
        },
        "ports": ports,
        "smoke": {k: v for k, v in (smoke or {}).items() if k != "items"},
        "modules": {k: v for k, v in (mods or {}).items() if k != "routes"},
    }
    ov_path = os.path.join(REPORT_DIR, "overview.json")
    with io.open(ov_path, "w", encoding="utf-8") as f:
        json.dump(overview, f, ensure_ascii=False, indent=2)

    print(f"\n{BOLD}{'='*60}\n  汇总\n{'='*60}{RESET}")
    if ports:
        print(f"端口：{ports['healthy']}/{ports['total']} 健康  (rc={rc_ports})")
    if smoke:
        print(f"接口：{smoke['pass']}/{smoke['total']} 通过，{smoke['fail']} 失败，{smoke['slow']} 慢  (rc={rc_smoke})")
    if mods:
        miss = mods.get("routes", {}).get("missing", [])
        print(f"模块：路由缺失 {len(miss)}，Electron {'OK' if mods['electron']['ok'] else '异常'}  (rc={rc_mods})")

    bad = [n for n, rc in [("ports", rc_ports), ("smoke", rc_smoke), ("modules", rc_mods)] if rc != 0]
    if bad:
        print(f"\n{RED}{BOLD}✗ 存在失败步骤：{', '.join(bad)}{RESET}")
        print(f"总览报告：{ov_path}")
        return 1
    print(f"\n{GREEN}{BOLD}✓ 全部检测通过{RESET}")
    print(f"总览报告：{ov_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
