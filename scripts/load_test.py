#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
阶段二 P1：关键端点并发压测脚本（Python + requests）。

压测链路（模拟真实用户行为）：
    登录 -> 角色列表 -> 聊天（模拟）-> 好友列表 -> 社群浏览

输出指标：
    - 总请求数 / 成功数 / 失败数
    - 错误率（%）
    - 实际 QPS（总请求数 / 总耗时）
    - 平均响应时间（ms）
    - P50 / P95 / P99 响应时间（ms）

用法示例：
    # 8 并发，持续 30 秒
    python scripts/load_test.py --concurrency 8 --duration 30

    # 指定被测环境账号与地址
    python scripts/load_test.py --base-url http://localhost:8080 \
        --username testuser013 --password 'Test@123456' --concurrency 16 --duration 60

注意：
    - 依赖第三方库：pip install requests
    - 仅在测试/预发环境运行；写接口（聊天/发消息）默认使用只读或模拟负载，
      避免对生产数据造成污染。聊天端点若不可用会自动降级为只统计可达性。
"""

import argparse
import statistics
import threading
import time
from collections import defaultdict

try:
    import requests
except ImportError:  # pragma: no cover
    raise SystemExit("缺少依赖 requests，请先执行: pip install requests")


# ---------- 被测端点（按本项目实际路由配置） ----------
LOGIN_PATH = "/api/user/login"
ROLE_LIST_PATH = "/api/character/list"          # 匿名可读
CHAT_PATH = "/api/friend/chat/send"             # 模拟发消息（需登录）
FRIEND_LIST_PATH = "/api/friend/list"           # 好友列表（需登录）
COMMUNITY_LIST_PATH = "/api/community/list"      # 社群浏览（匿名可读）


class Stats:
    """线程安全的指标采集器。"""

    def __init__(self):
        self._lock = threading.Lock()
        self.latencies = []          # 全部成功请求响应时间（秒）
        self.errors = defaultdict(int)  # 错误分类计数
        self.success = 0
        self.total = 0

    def record_ok(self, latency):
        with self._lock:
            self.success += 1
            self.total += 1
            self.latencies.append(latency)

    def record_err(self, category):
        with self._lock:
            self.total += 1
            self.errors[category] += 1


def percentile(values, p):
    """计算分位数（values 单位：秒）。p 取 0-100。"""
    if not values:
        return 0.0
    s = sorted(values)
    k = (len(s) - 1) * (p / 100.0)
    f = int(k)
    c = min(f + 1, len(s) - 1)
    if f == c:
        return s[f]
    return s[f] + (s[c] - s[f]) * (k - f)


def try_login(base_url, username, password, timeout):
    """登录并返回 Authorization 头；失败返回 None。"""
    try:
        resp = requests.post(
            base_url + LOGIN_PATH,
            json={"username": username, "password": password},
            timeout=timeout,
        )
    except requests.RequestException:
        return None
    if resp.status_code != 200:
        return None
    try:
        body = resp.json()
    except ValueError:
        return None
    data = body.get("data") if isinstance(body, dict) else None
    token = None
    if isinstance(data, dict):
        token = data.get("token") or data.get("accessToken") or data.get("jwt")
    if token:
        return {"Authorization": "Bearer " + token}
    return None


def worker(base_url, headers, stop_at, stats, timeout):
    """单个压测线程：循环请求关键端点，直到 stop_at 时间点。"""
    # 每个线程内的请求顺序：角色列表 -> 好友列表 -> 社群 -> 聊天
    plan = [
        ("GET", ROLE_LIST_PATH, None),
        ("GET", FRIEND_LIST_PATH, None),
        ("GET", COMMUNITY_LIST_PATH, None),
        ("POST", CHAT_PATH, {"toUserId": 1, "content": "load-test message 013"}),
    ]
    while time.time() < stop_at:
        for method, path, body in plan:
            if time.time() >= stop_at:
                break
            url = base_url + path
            t0 = time.time()
            try:
                if method == "GET":
                    r = requests.get(url, headers=headers, timeout=timeout)
                else:
                    r = requests.post(url, headers=headers, json=body, timeout=timeout)
                dt = time.time() - t0
                if r.status_code < 400:
                    stats.record_ok(dt)
                else:
                    stats.record_err("http_%s" % r.status_code)
            except requests.RequestException as e:
                stats.record_err(type(e).__name__)


def main():
    parser = argparse.ArgumentParser(description="AI Chat 关键端点并发压测")
    parser.add_argument("--base-url", default="http://localhost:8080",
                        help="被测服务地址，默认 http://localhost:8080")
    parser.add_argument("--username", default="loadtest013", help="压测账号")
    parser.add_argument("--password", default="loadtest013", help="压测密码")
    parser.add_argument("--concurrency", type=int, default=8, help="并发线程数，默认 8")
    parser.add_argument("--duration", type=int, default=30, help="压测持续秒数，默认 30")
    parser.add_argument("--timeout", type=float, default=5.0, help="单请求超时秒数，默认 5")
    args = parser.parse_args()

    base = args.base_url.rstrip("/")
    print("== AI Chat 压测开始 ==")
    print("目标: %s | 并发=%d | 时长=%ds" % (base, args.concurrency, args.duration))

    headers = try_login(base, args.username, args.password, args.timeout)
    if headers is None:
        print("[警告] 登录失败，将以匿名方式压测（仅匿名可读端点会成功，需登录端点会计入错误）")
        headers = {}
    else:
        print("[OK] 登录成功，已携带 JWT")

    stats = Stats()
    threads = []
    stop_at = time.time() + args.duration
    start = time.time()
    for _ in range(args.concurrency):
        t = threading.Thread(
            target=worker,
            args=(base, headers, stop_at, stats, args.timeout),
            daemon=True,
        )
        t.start()
        threads.append(t)
    for t in threads:
        t.join()
    elapsed = time.time() - start

    # ---------- 汇总 ----------
    lat = stats.latencies
    qps = (stats.total / elapsed) if elapsed > 0 else 0.0
    err_rate = (sum(stats.errors.values()) / stats.total * 100.0) if stats.total else 0.0
    avg_ms = (statistics.mean(lat) * 1000.0) if lat else 0.0

    print("\n== 压测结果 ==")
    print("总耗时        : %.2f s" % elapsed)
    print("总请求数      : %d" % stats.total)
    print("成功请求      : %d" % stats.success)
    print("失败请求      : %d" % sum(stats.errors.values()))
    print("错误率        : %.2f %%" % err_rate)
    print("实际 QPS      : %.2f" % qps)
    print("平均响应时间  : %.2f ms" % avg_ms)
    print("P50 响应时间  : %.2f ms" % (percentile(lat, 50) * 1000.0))
    print("P95 响应时间  : %.2f ms" % (percentile(lat, 95) * 1000.0))
    print("P99 响应时间  : %.2f ms" % (percentile(lat, 99) * 1000.0))
    if stats.errors:
        print("错误分类      :")
        for k, v in sorted(stats.errors.items(), key=lambda x: -x[1]):
            print("  - %-16s %d" % (k, v))


if __name__ == "__main__":
    main()
