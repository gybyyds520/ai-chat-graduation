# -*- coding: utf-8 -*-
import json, io
p = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\qa-run.json"
d = json.load(io.open(p, encoding="utf-8"))
d["release_decision"] = {
  "decision": "undetermined",
  "rationale": "已实测的注册/登录/JWT鉴权/输入校验/越权防护核心链路通过(12用例中11通过、1阻塞)，无S1/S2缺陷。但受Redis 6379、Ollama 11434、Prometheus 9090未启动及兄弟代理并发E2E干扰，AI对话/社交/配额/限流等核心业务未覆盖，且admin种子口令为明文致文档管理员账号无法登录(BUG-001,S3/P3)。证据不足以判定整体发布；已验证范围(认证注册)可放行。",
  "blocking_bug_ids": [],
  "conditions": [
    "启动Redis/Ollama后回归AI对话与限流/账户锁定",
    "修正admin种子口令为bcrypt哈希",
    "兄弟代理完成配额原子更新/群消息校验改动后回归其影响面"
  ]
}
io.open(p, "w", encoding="utf-8").write(json.dumps(d, ensure_ascii=False, indent=2))
print("release decision set")
