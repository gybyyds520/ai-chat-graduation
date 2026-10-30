# -*- coding: utf-8 -*-
import json, io
p = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\qa-run.json"
d = json.load(io.open(p, encoding="utf-8"))
ts = "2026-09-30T09:50:00+08:00"

# close bug
for b in d["bugs"]:
    if b["id"] == "BUG-001":
        b["status"] = "closed"
        b["analysis"]["confidence"] = "high"
        b["actual_result"] = "admin/123456 登录 401；库中 password 为明文。已修复：AdminServiceImpl 统一 passwordEncoder.matches；库中 id=63 password 改为 {bcrypt}$2b$12$...。独立复验：user-login 200、admin-login 200、错误密码 401。"
        b["related_ids"] = ["EXE-003", "EXE-013", "EXE-014"]
        b["resolution"] = "已修复并复验通过（代码统一 DelegatingPasswordEncoder.matches + 数据改为 bcrypt 哈希）"

# new executions (regression after fix)
d["executions"] += [
 {"id":"EXE-013","case_id":"TC-003","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"修复后复验 POST /api/user/login admin/123456 -> HTTP 200 code=200 返回 Bearer token(id=63 管理员)","started_at":ts,"finished_at":ts,"attempt":2,"retest_of":"EXE-003","tester_notes":"BUG-001 修复后回归"},
 {"id":"EXE-014","case_id":"TC-010","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"POST /api/admin/login admin/123456 -> HTTP 200 code=200 返回 admin 信息","started_at":ts,"finished_at":ts,"attempt":1,"tester_notes":"管理员登录入口修复后回归"},
 {"id":"EXE-015","case_id":"TC-007","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"admin/wrongpass -> HTTP 401 code=10101 用户名或密码错误(0.37s)，确认修复未放宽密码校验","started_at":ts,"finished_at":ts,"attempt":1,"tester_notes":""},
 {"id":"EXE-016","case_id":"TC-011","status":"blocked","execution_level":"partial_validation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"本机未安装 redis-server(PATH/常见路径均未命中)，登录失败计数/账户锁定依赖 Redis，无法补测；不安装新软件。","started_at":ts,"finished_at":ts,"attempt":1,"tester_notes":"环境：redis 未安装"},
]

d["evidence"] += [
 {"id":"EV-007","type":"http_response","description":"BUG-001 修复后独立复验：admin user-login 200、admin admin-login 200、错误密码 401、demo_alice 200","level":"L2_observation","validation_scope":"formal","locator":"verify_fix.txt"},
 {"id":"EV-008","type":"service_probe","description":"本地启动 ollama serve，11434/api/tags 200，已拉取 nomic-embed-text(嵌入模型)；redis-server/prometheus 本机未安装","level":"L2_observation","validation_scope":"formal","locator":"ollama /api/tags"},
]

# unverified refresh
d["unverified"] = [
 "Redis 本机未安装(redis-server 未在 PATH/常见路径)：登录失败计数/账户锁定(TC-011)与在线状态无法补测，按环境阻塞记录，不安装新软件",
 "Prometheus 本机未安装：9090 指标采集不验证",
 "Ollama 已启动(11434 UP，含 nomic-embed-text 嵌入模型)，但未确认可用对话模型与后端聊天接口契约，AI 对话业务链路端到端本轮未执行",
 "支付真实回调/到账：不发起真实支付",
 "移动端/其他浏览器/多视口兼容：仅桌面 API 探测",
 "兄弟代理在改的配额原子更新(P1-3)/群消息成员校验(P1-4)：避免重复劳动，未专门回归",
 "前端 5187 收口时未监听；本轮以 API 断言为证据，未重复 UI 旅程",
]

# release decision refresh
d["release_decision"] = {
 "decision": "conditional_go",
 "rationale": "修复 BUG-001 后，注册/登录(含管理员双入口)/JWT 鉴权/输入校验/越权防护核心链路全部通过，无开放 S1/S2，P0 阻断集合为空。可在满足下列条件后放行已验证范围；AI 对话/社交/配额/限流锁定等大面积业务仍因 Redis/Prometheus 未安装、无确认对话模型而未覆盖。",
 "blocking_bug_ids": [],
 "conditions": [
  "安装/启动 Redis 后回归登录失败计数与账户锁定(TC-011)",
  "配置可用对话模型后端到端回归 AI 对话链路",
  "兄弟代理完成配额/群消息改动后按影响面回归",
  "上述条件由对应开发/测试负责人在提测前关闭并留证"
 ]
}
io.open(p,"w",encoding="utf-8").write(json.dumps(d,ensure_ascii=False,indent=2))
print("closure update written")
