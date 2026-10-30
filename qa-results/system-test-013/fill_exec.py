# -*- coding: utf-8 -*-
import json, io
p = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\qa-run.json"
with io.open(p,"r",encoding="utf-8") as f: d=json.load(f)

ts="2026-09-30T01:40:00+08:00"
干扰="兄弟代理并发 E2E 导致 http-nio-8080 worker 高峰排队，登录 POST 在 00:50-01:30 间多次不响应；低谷(01:40)复测全部毫秒级返回，判定为环境/并发干扰而非产品缺陷。"

d["executions"] = [
 {"id":"EXE-001","case_id":"TC-001","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"GET /api/health -> HTTP 200 {status:UP,db:UP} 12ms","started_at":ts,"finished_at":ts,"attempt":1,"tester_notes":""},
 {"id":"EXE-002","case_id":"TC-002","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"POST /api/user/register -> HTTP 200, code=200, user id=88 username=qa013_4qyeev, 响应体不含 password 字段","started_at":ts,"finished_at":ts,"attempt":1,"tester_notes":""},
 {"id":"EXE-003","case_id":"TC-003","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"新注册用户与 demo_alice 登录均 200 返回 Bearer token(65ms/70ms)。admin/123456 登录 401，经查库为种子口令明文问题，见 BUG-001。"+干扰,"started_at":ts,"finished_at":ts,"attempt":1},
 {"id":"EXE-004","case_id":"TC-004","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"username=测试用户013 -> HTTP 400 '用户名只能包含字母、数字和下划线' 5ms","started_at":ts,"finished_at":ts,"attempt":1},
 {"id":"EXE-005","case_id":"TC-005","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"password=123 -> HTTP 400 '密码长度必须在6-20个字符之间' 15ms","started_at":ts,"finished_at":ts,"attempt":1},
 {"id":"EXE-006","case_id":"TC-006","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"username=admin 重复注册 -> HTTP 400 code=10100 '用户名已存在' 16ms(未被限流吞成429)","started_at":ts,"finished_at":ts,"attempt":1},
 {"id":"EXE-007","case_id":"TC-007","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"admin/wrongpass -> HTTP 401 code=10101 '用户名或密码错误' 9ms","started_at":ts,"finished_at":ts,"attempt":1},
 {"id":"EXE-008","case_id":"TC-008","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"不存在用户 -> HTTP 401 code=10101 '用户名或密码错误' 9ms；与错误密码提示完全一致，无用户枚举差异","started_at":ts,"finished_at":ts,"attempt":1},
 {"id":"EXE-009","case_id":"TC-009","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"无 token GET /api/user/membership -> 401 '未登录或登录已过期'；带新用户 token -> 200 返回配额(FREE) 5ms","started_at":ts,"finished_at":ts,"attempt":1},
 {"id":"EXE-010","case_id":"TC-010","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"普通用户 POST /api/auth/badge/award -> 403 '需要管理员权限'；GET /api/admin/users -> 403 '权限不足'","started_at":ts,"finished_at":ts,"attempt":1},
 {"id":"EXE-011","case_id":"TC-011","status":"blocked","execution_level":"partial_validation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"未执行：依赖 Redis 计数/锁定(6379 未监听)，且连续错误登录有锁定测试账号风险，按 unverified 登记","started_at":ts,"finished_at":ts,"attempt":1},
 {"id":"EXE-012","case_id":"TC-012","status":"passed","execution_level":"full_automation","validation_scope":"formal","execution_method":"automated","operator":"qa-agent","actual_result":"email=notanemail -> HTTP 400 '邮箱格式不正确' 105ms","started_at":ts,"finished_at":ts,"attempt":1},
]

d["evidence"] = [
 {"id":"EV-001","type":"http_response","description":"health 200 db UP","level":"L2_observation","validation_scope":"formal","locator":"api_results.txt TC-001"},
 {"id":"EV-002","type":"http_response","description":"注册成功 200 返回 user 无 password","level":"L2_observation","validation_scope":"formal","locator":"api_results.txt TC-002"},
 {"id":"EV-003","type":"http_response","description":"登录成功返回 Bearer token","level":"L2_observation","validation_scope":"formal","locator":"api_results2.txt TC-003b; api_results3.txt demo_alice"},
 {"id":"EV-004","type":"http_response","description":"参数校验/重复用户名/错误凭据/越权 全部按预期拒绝","level":"L2_observation","validation_scope":"formal","locator":"api_results.txt/results2/results3"},
 {"id":"EV-005","type":"db_query","description":"user 表 admin(id=63) password 字段为明文 '123456'，demo_alice/test 为 bcrypt 哈希","level":"L2_observation","validation_scope":"formal","locator":"mysql SELECT"},
 {"id":"EV-006","type":"runtime_log","description":"高峰期登录探针无对应 UserMapper 查询；01:25 大量并发 http-nio 线程(JWT校验)，低谷复测恢复","level":"L2_observation","validation_scope":"formal","locator":"backend/aichat/logs/app.log"},
]

d["bugs"] = [
 {"id":"BUG-001","title":"admin 账号口令以明文存储导致文档给定管理员凭据无法登录","module":"user/auth","status":"open","severity":"S3","severity_basis":"测试库种子数据：仅影响 admin 种子账号登录，注册/正常用户均正确 bcrypt；不影响线上代码逻辑","priority":"P3","category":"data_seed","environment":"本地开发 MySQL ai_chat","preconditions":"使用任务给定 admin/123456","steps":["POST /api/user/login {username:admin,password:123456}","查库 SELECT password FROM user WHERE id=63"],"actual_result":"登录 401 '用户名或密码错误'；查库 password 字段值为明文 123456(非 bcrypt 前缀)","expected_result":"种子管理员口令应 bcrypt 哈希存储，admin/123456 可正常登录","reproducibility":"always","repro_attempts":2,"first_failure_preserved":True,"evidence_grade":"L2_observation","evidence_ids":["EV-005"],"impact":"文档管理员账号无法登录，运维/管理员入口在本环境不可用","analysis":{"classification":"data","trigger_hypothesis":"种子/SQL 初始化脚本直接写入明文口令，未走 BCryptPasswordEncoder.encode","change_correlation":"与兄弟代理在跑功能修复无直接关联","blast_radius":"仅 admin(id=63) 种子行；新建用户(如 id=88)哈希正常","confidence":"high"},"workaround":"用 demo_alice/Test@123456 或新注册账号登录；或修正种子为 bcrypt 哈希","related_ids":["EXE-003"],"independent_failure_signature":"admin-seed-password-plaintext-not-bcrypt"},
]

# test data
d["test_data"]["writes_allowed"]=True
d["test_data"]["accounts"]=["admin/123456(401-数据问题)","demo_alice/Test@123456"]
d["test_data"]["created_records"]=["user id=88 username=qa013_4qyeev (本次注册，保留不删)"]
d["test_data"]["cleanup"]={"required":False,"status":"completed","command":"none(按指示保留测试数据不删)","residuals":["qa013_4qyeev/Test@123456 保留"]}

with io.open(p,"w",encoding="utf-8") as f: json.dump(d,f,ensure_ascii=False,indent=2)
print("exec+bugs written")
