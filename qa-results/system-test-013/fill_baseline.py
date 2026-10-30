# -*- coding: utf-8 -*-
import json, io, sys

p = r"C:\Users\郭耀彬\Desktop\gybct\ai-chat-graduation\qa-results\system-test-013\qa-run.json"
with io.open(p, "r", encoding="utf-8") as f:
    d = json.load(f)

# ---- input / sources ----
d["input"]["summary"] = (
    "ai-chat-graduation 本地开发环境 QA 系统测试。后端 Spring Boot 8080（PID 11200，00:46 由兄弟代理重启），"
    "前端 Vite+Vue 5187，MySQL 8.0 3306 库 ai_chat。Redis 6379 / Ollama 11434 / Prometheus 9090 未监听。"
    "核心被测面：注册 POST /api/user/register、登录 POST /api/user/login（返回 JWT）、鉴权保护与参数校验。"
)
d["input"]["sources"] = [
    {"id": "SRC-001", "type": "source_code", "locator": r"backend\aichat\src\main\java\com\example\aichat\controller\UserController.java",
     "access_status": "read", "completeness_checked": True, "item_count": 1, "reviewed_item_count": 1},
    {"id": "SRC-002", "type": "source_code", "locator": r"backend\aichat\src\main\java\com\example\aichat\dto\user\RegisterRequest.java",
     "access_status": "read", "completeness_checked": True, "item_count": 1, "reviewed_item_count": 1},
    {"id": "SRC-003", "type": "source_code", "locator": r"backend\aichat\src\main\java\com\example\aichat\service\impl\UserServiceImpl.java",
     "access_status": "read", "completeness_checked": True, "item_count": 1, "reviewed_item_count": 1},
    {"id": "SRC-004", "type": "source_code", "locator": r"backend\aichat\src\main\java\com\example\aichat\service\CacheService.java",
     "access_status": "read", "completeness_checked": True, "item_count": 1, "reviewed_item_count": 1},
    {"id": "SRC-005", "type": "live_api", "locator": "http://localhost:8080/api/health",
     "access_status": "read", "completeness_checked": True, "item_count": 1, "reviewed_item_count": 1},
    {"id": "SRC-006", "type": "live_web", "locator": "http://localhost:5187/register",
     "access_status": "read", "completeness_checked": True, "item_count": 1, "reviewed_item_count": 1},
    {"id": "SRC-007", "type": "live_db", "locator": "mysql 127.0.0.1:3306/ai_chat (information_schema.PROCESSLIST)",
     "access_status": "read", "completeness_checked": True, "item_count": 1, "reviewed_item_count": 1},
    {"id": "SRC-008", "type": "runtime_log", "locator": r"backend\aichat\logs\app.log",
     "access_status": "read", "completeness_checked": False, "item_count": 1, "reviewed_item_count": 1},
    {"id": "SRC-009", "type": "doc", "locator": r"测试方案.md / 测试账号与数据清单.md",
     "access_status": "not_applicable", "completeness_checked": False, "item_count": 2, "reviewed_item_count": 0},
]
d["input"]["assumptions"] = [
    "Redis 6379 未监听；CacheService 对 Redis 操作 try/catch 降级，登录主流程设计上不应硬依赖 Redis 可用性。",
    "测试账号 admin/123456、demo_alice..frank/Test@123456 由任务书提供，直接复用。",
    "兄弟代理正在并发修复配额/群消息并可能重启服务；同一用例跨重启以实际观察为准，首败保留。",
]
d["input"]["conflicts"] = []

d["environment"] = {
    "os": "Windows",
    "backend_url": "http://localhost:8080",
    "frontend_url": "http://localhost:5187",
    "db": "mysql 127.0.0.1:3306/ai_chat UP",
    "redis": "6379 NOT listening",
    "ollama": "11434 NOT listening",
    "prometheus": "9090 NOT listening",
    "backend_pid": 11200,
    "test_accounts": ["admin/123456", "demo_alice/Test@123456"],
}

# ---- requirements ----
d["requirements"] = [
    {"id": "REQ-001", "summary": "注册：合法用户名(3-20字母数字下划线)+密码(6-20)+有效邮箱可注册成功，返回用户且不回显密码；用户名唯一。",
     "source": "SRC-001,SRC-002", "risk": "P0",
     "behavior": {"given": "未注册访客", "when": "POST /api/user/register 合法 body",
                  "then": "200 返回 user(密码字段为空)", "side_effect": "user 表新增一行"}},
    {"id": "REQ-002", "summary": "登录：正确用户名密码返回 JWT token 与 user；错误凭据拒绝；disabled/locked 账号拒绝。",
     "source": "SRC-001,SRC-003", "risk": "P0",
     "behavior": {"given": "已注册用户", "when": "POST /api/user/login", "then": "200 含 token(Bearer)",
                   "failure": "凭据错误返回 401"}},
    {"id": "REQ-003", "summary": "参数校验：非法用户名/密码过短/邮箱格式错误/年龄越界由 Bean Validation 返回 400 与明确中文提示。",
     "source": "SRC-002", "risk": "P1"},
    {"id": "REQ-004", "summary": "鉴权保护：受保护接口无 token 或无效 token 返回 401；普通用户不能访问管理员专属接口。",
     "source": "SRC-001,SecurityConfig", "risk": "P1"},
    {"id": "REQ-005", "summary": "防爆破/限流：登录注册接口限流；连续失败达到阈值锁定。",
     "source": "SRC-001,SRC-003", "risk": "P2"},
]

# ---- risk mechanisms ----
d["risk_mechanisms"] = [
    {"id": "RM-AUTH-001", "title": "注册→登录→获取JWT 核心旅程不可用",
     "failure_mode": "登录接口在兄弟代理并发/重启窗口不响应，用户无法进入系统",
     "business_impact": "所有受保护功能不可达，阻断发布",
     "oracle": "POST /api/user/login 正确凭据应在数秒内返回 200+token；不应无限挂起",
     "requirement_ids": ["REQ-002"], "case_ids": ["TC-003"], "priority": "P0", "status": "identified"},
    {"id": "RM-AUTH-002", "title": "注册接口参数校验/用户名唯一约束失效",
     "failure_mode": "非法输入或重复用户名未被服务端拒绝",
     "business_impact": "脏数据/账户可枚举", "oracle": "非法输入 400；重复用户名返回业务错误",
     "requirement_ids": ["REQ-001", "REQ-003"], "case_ids": ["TC-002", "TC-004", "TC-005", "TC-006", "TC-012"],
     "priority": "P0", "status": "identified"},
    {"id": "RM-AUTH-003", "title": "未鉴权/越权访问受保护资源",
     "failure_mode": "无 token 或普通用户可访问需鉴权/管理员接口",
     "business_impact": "水平/垂直越权，数据泄露", "oracle": "无 token→401；普通用户访问管理员接口→403",
     "requirement_ids": ["REQ-004"], "case_ids": ["TC-009", "TC-010"], "priority": "P1", "status": "identified"},
    {"id": "RM-AUTH-004", "title": "登录失败错误码与锁定",
     "failure_mode": "错误凭据返回 200/500 而非 401；限流被业务异常吞掉",
     "business_impact": "防爆破失效/误判", "oracle": "错误密码→401；高频→429/锁定",
     "requirement_ids": ["REQ-002", "REQ-005"], "case_ids": ["TC-007", "TC-008", "TC-011"], "priority": "P1", "status": "identified"},
]

# ---- cases ----
d["cases"] = [
    {"id": "TC-001", "module": "health", "title": "服务健康检查", "priority": "P0", "type": "smoke",
     "steps": "GET /api/health", "test_data": "无", "expected_result": "200，body status=UP 且 db=UP",
     "requirement_ids": [], "risk_mechanism_ids": [], "execution_mode": "automated",
     "state_oracle": {"terminal": "HTTP 200 JSON"}, "evidence_expected": ["curl 输出"]},
    {"id": "TC-002", "module": "auth", "title": "注册合法一次性账号(含013)", "priority": "P0", "type": "core_flow",
     "steps": "POST /api/user/register {username: qa013_<rand>, password: Test@123456, email: qa013_<rand>@example.com, age:25}",
     "test_data": "用户名含013", "expected_result": "200，返回 user 对象且 password 字段为空；user 表新增记录",
     "requirement_ids": ["REQ-001"], "risk_mechanism_ids": ["RM-AUTH-002"], "execution_mode": "automated",
     "release_blocking_reason": "注册是新用户入口，失败则系统不可用",
     "state_oracle": {"terminal": "200 R.success(user)", "side_effect": "user 表 +1"},
     "evidence_expected": ["HTTP 状态码与响应体"]},
    {"id": "TC-003", "module": "auth", "title": "正确凭据登录返回 JWT", "priority": "P0", "type": "core_flow",
     "steps": "POST /api/user/login {username:admin,password:123456}",
     "test_data": "admin/123456", "expected_result": "数秒内 200，含 token(Bearer) 与 user",
     "requirement_ids": ["REQ-002"], "risk_mechanism_ids": ["RM-AUTH-001"], "execution_mode": "automated",
     "release_blocking_reason": "无法登录=系统不可用",
     "state_oracle": {"terminal": "200 R.success{user,token,tokenType=Bearer}"},
     "evidence_expected": ["HTTP 状态码与 token"]},
    {"id": "TC-004", "module": "auth", "title": "注册用户名含非法字符(中文)被拒", "priority": "P1", "type": "exception",
     "steps": "POST /api/user/register username=测试用户013", "test_data": "username=测试用户013",
     "expected_result": "400，提示用户名只能包含字母数字下划线",
     "requirement_ids": ["REQ-003"], "risk_mechanism_ids": ["RM-AUTH-002"], "execution_mode": "automated",
     "state_oracle": {"terminal": "400 校验错误"}},
    {"id": "TC-005", "module": "auth", "title": "注册密码过短(<6)被拒", "priority": "P1", "type": "exception",
     "steps": "POST /api/user/register password=123", "test_data": "password=123",
     "expected_result": "400，提示密码长度6-20",
     "requirement_ids": ["REQ-003"], "risk_mechanism_ids": ["RM-AUTH-002"], "execution_mode": "automated",
     "state_oracle": {"terminal": "400 校验错误"}},
    {"id": "TC-006", "module": "auth", "title": "注册重复用户名被拒", "priority": "P1", "type": "exception",
     "steps": "用已存在用户名(admin)再次注册", "test_data": "username=admin",
     "expected_result": "业务错误：用户名已存在（非429/500）",
     "requirement_ids": ["REQ-001"], "risk_mechanism_ids": ["RM-AUTH-002"], "execution_mode": "automated",
     "state_oracle": {"terminal": "业务错误 USERNAME_EXISTS"}},
    {"id": "TC-007", "module": "auth", "title": "错误密码登录被拒(401)", "priority": "P1", "type": "exception",
     "steps": "POST /api/user/login admin/wrongpass", "test_data": "wrongpass",
     "expected_result": "401 登录失败，不返回 token",
     "requirement_ids": ["REQ-002"], "risk_mechanism_ids": ["RM-AUTH-004"], "execution_mode": "automated",
     "state_oracle": {"terminal": "401"}},
    {"id": "TC-008", "module": "auth", "title": "不存在用户登录被拒", "priority": "P1", "type": "exception",
     "steps": "POST /api/user/login nosuchuser013/x", "test_data": "nosuchuser013",
     "expected_result": "401 登录失败",
     "requirement_ids": ["REQ-002"], "risk_mechanism_ids": ["RM-AUTH-004"], "execution_mode": "automated",
     "state_oracle": {"terminal": "401"}},
    {"id": "TC-009", "module": "auth", "title": "无 token 访问受保护接口被拒", "priority": "P1", "type": "permission_security",
     "steps": "GET /api/user/membership 不带 Authorization", "test_data": "无",
     "expected_result": "401 未认证",
     "requirement_ids": ["REQ-004"], "risk_mechanism_ids": ["RM-AUTH-003"], "execution_mode": "automated",
     "state_oracle": {"terminal": "401"}},
    {"id": "TC-010", "module": "auth", "title": "普通用户访问管理员接口被拒(403)", "priority": "P1", "type": "permission_security",
     "steps": "用 demo_alice token 调用管理员接口(如 /api/auth/badge/award 或 /api/admin/*)",
     "test_data": "demo_alice token", "expected_result": "403 禁止访问",
     "requirement_ids": ["REQ-004"], "risk_mechanism_ids": ["RM-AUTH-003"], "execution_mode": "automated",
     "state_oracle": {"terminal": "403"}},
    {"id": "TC-011", "module": "auth", "title": "高频登录限流/防爆破", "priority": "P2", "type": "exception",
     "steps": "短时间内连续错误登录 >=10 次", "test_data": "错误密码",
     "expected_result": "触发 429 或账户锁定（条件观察）",
     "requirement_ids": ["REQ-005"], "risk_mechanism_ids": ["RM-AUTH-004"], "execution_mode": "automated",
     "state_oracle": {"terminal": "429/锁定（环境依赖 Redis，按实际记录）"}},
    {"id": "TC-012", "module": "auth", "title": "注册邮箱格式错误被拒", "priority": "P1", "type": "exception",
     "steps": "POST /api/user/register email=notanemail", "test_data": "email=notanemail",
     "expected_result": "400 邮箱格式不正确",
     "requirement_ids": ["REQ-003"], "risk_mechanism_ids": ["RM-AUTH-002"], "execution_mode": "automated",
     "state_oracle": {"terminal": "400 校验错误"}},
]

# ---- acceptance checks ----
d["acceptance_checks"] = [
    {"id": "AC-001", "title": "注册-登录核心旅程", "type": "core_flow", "case_ids": ["TC-002", "TC-003"],
     "blocking": True, "notes": "P0：新用户注册并登录取得 token 是系统入口"},
    {"id": "AC-002", "title": "鉴权与越权防护", "type": "permission_security", "case_ids": ["TC-009", "TC-010"],
     "blocking": True, "notes": "未授权访问必须被拒绝"},
    {"id": "AC-003", "title": "输入校验与错误码", "type": "exception", "case_ids": ["TC-004", "TC-005", "TC-006", "TC-007", "TC-008", "TC-012"],
     "blocking": False, "notes": "参数校验与登录失败码"},
]

with io.open(p, "w", encoding="utf-8") as f:
    json.dump(d, f, ensure_ascii=False, indent=2)
print("baseline filled OK")
