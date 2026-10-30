# -*- coding: utf-8 -*-
import json, io
p = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\qa-run.json"
d = json.load(io.open(p, encoding="utf-8"))

ev_ids = {e["id"] for e in d.get("evidence", [])}
exec_ids = {e["id"] for e in d.get("executions", [])}
print("evidence:", sorted(ev_ids))
print("executions:", sorted(exec_ids))

for b in d["bugs"]:
    if b["id"] == "BUG-USERAUTH-001":
        b["status"] = "closed"
        b["resolution"] = ("已修复并复验关闭：根因=admin(id=63)口令明文 + AdminServiceImpl 原手写明文 equals 兼容分支。"
            "修复3处：①AdminServiceImpl 统一 passwordEncoder.matches（兼容{bcrypt}前缀与裸$2a$，明文必然不匹配）；"
            "②AdminServiceImplTest 移除明文兼容契约用例，新增「{bcrypt}前缀哈希正确匹配」「明文口令不兼容返回null」两用例，Admin相关21用例全过；"
            "③库中 UPDATE user SET password=bcrypt哈希 WHERE id=63。"
            "MainAgent 验证：/api/user/login admin/123456→200、/api/admin/login→200、错误密码→401、demo_alice({bcrypt})→200、gyb789(裸$2a$)→200。"
            "我方独立复验(verify_fix.txt,L2)：user-login 200、admin-login 200、错误密码 401、demo_alice 200。")
        # add evidence ids present in canonical
        ids = set(b.get("evidence_ids", []))
        for ev in ["EV-005", "EV-007"]:
            if ev in ev_ids: ids.add(ev)
        b["evidence_ids"] = sorted(ids)
        # link regression executions (match by suffix)
        rel = set(b.get("related_ids", []))
        for eid in exec_ids:
            if eid.endswith(("-013","-014","-015")):
                rel.add(eid)
        b["related_ids"] = sorted(rel)
        print("bug fixed:", b["status"], b["evidence_ids"], b["related_ids"])

# ensure release decision consistency
d["release_decision"]["decision"] = "conditional_go"
d["release_decision"]["blocking_bug_ids"] = []
io.open(p, "w", encoding="utf-8").write(json.dumps(d, ensure_ascii=False, indent=2))
print("decision:", d["release_decision"]["decision"], d["release_decision"]["blocking_bug_ids"])
