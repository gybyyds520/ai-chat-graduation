# -*- coding: utf-8 -*-
import json, io
p = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\qa-run.json"
with io.open(p, "r", encoding="utf-8") as f:
    d = json.load(f)

# input.artifacts = SRC list (reuse sources)
d["input"]["artifacts"] = d["input"]["sources"]

# unverified: known out-of-scope / environment-blocked
d["unverified"] = [
    "Redis 6379 未监听：依赖 Redis 的在线状态/计数/限流到账语义按降级路径观察，不验证 Redis 命中分支",
    "Ollama 11434 未监听：AI 对话/角色生成等大模型推理链路不验证真实模型返回",
    "Prometheus 9090 未监听：监控指标采集不验证",
    "支付真实回调/到账：不发起真实支付，仅验证接口存在性与鉴权",
    "移动端/其他浏览器/多视口兼容：本轮仅桌面 Chrome 内嵌浏览器与 API",
    "兄弟代理正在改的配额原子更新(P1-3)与群消息成员校验(P1-4)：避免重复劳动，本轮不专门回归其改动",
    "登录 POST 在兄弟代理并发 E2E 高峰排队超时：以 UI 旅程 + 低谷重试交叉验证，高峰不无限重试",
]
with io.open(p, "w", encoding="utf-8") as f:
    json.dump(d, f, ensure_ascii=False, indent=2)
print("block fixes applied")
