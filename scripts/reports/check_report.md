# AI Chat 自动化健康检测报告

> 生成时间：2026-09-15 16:48（Asia/Shanghai）
> 执行方式：`python scripts/run_all_checks.py`（端口 + 接口冒烟 + 三端代码级）
> 总体结论：**✅ 全部通过** —— 端口 4/4、接口 91/91、三端代码级 0 缺陷、构建与单测基线绿。

---

## 一、执行摘要

| 检测项 | 结果 | 明细 |
|---|---|---|
| 端口健康 | ✅ 4/4 | 5187 / 8080 / 11434 / 3306 全部 LISTENING |
| 接口冒烟 | ✅ 91/91 | 0 失败，0 慢请求（>3000ms） |
| 模块/三端代码级 | ✅ 通过 | 路由 26/26、移动端断点齐全、Electron 8/8、API 层齐全 |
| 后端构建 | ✅ 通过 | `mvnw -o compile` exit 0 |
| 前端构建 | ✅ 通过 | `npm run build` ✓ built in 2m18s |
| 后端单测基线 | ✅ 0 失败 | 88 类 / 794 用例 / 0 失败 / 0 错误 / 35 跳过 |
| 前端单测基线 | ✅ 全通过 | 41 测试文件 / 586 用例 |

---

## 二、端口检测结果（check_ports.py）

| 端口 | 用途 | 监听 | 健康探测 | PID | 耗时 |
|---|---|---|---|---|---|
| 5187 | Vite 前端 | YES | HTTP 200 | 21260 | 48.6ms |
| 8080 | 后端 API | YES | 200 `{status:UP, db:UP}` | 22936 | 15.8ms |
| 11434 | Ollama 推理 | YES | 200（llama3.2:3b 已加载） | 7272 | 2.7ms |
| 3306 | MySQL | YES | TCP LISTENING | 6276 | 0.3ms |

原始数据：`scripts/reports/ports_report.json`

---

## 三、接口冒烟结果（smoke_api.py）

- 登录：`demo_alice` 用户 token ✅、`admin` 管理 token ✅
- 遍历 **15 个模块组共 91 个端点**，全部返回预期状态码。
- 耗时分布：绝大多数 3–20ms；最慢 `/api/search/quick?query=a` ≈ 1949ms（含外部搜索调用，<3000ms 阈值，不告警）；`/api/recommendation/users` ≈ 69ms。
- 无 404 / 500 / 超时。

按模块通过情况：

| 模块组 | 端点数 | 结果 |
|---|---|---|
| 健康/认证/用户 | 11 | 全 200 |
| 好友/私聊/群聊 | 10 | 全 200 |
| 社群/广场/记忆/通知 | 11 | 全 200 |
| 投喂/动态/推荐 | 6 | 全 200 |
| AI 角色/精灵/知识库 | 7 | 全 200 |
| 搜索/模型/工具 | 6 | 全 200 |
| 配额/风险/隐私/资料/举报 | 6 | 全 200 |
| 语音/图片/外部/爬虫/聊天 | 10 | 全 200（外部/爬虫用 admin token，鉴权合规） |
| 管理/数据池/AI系统/审核/备份/支付 | 16 | 全 200 |
| 公开公告/广告 | 2 | 200 |

原始数据：`scripts/reports/smoke_report.json`

**鉴权合规性核验**（安全矩阵）：
- `/api/external/**`、`/api/crawler/**` 类级 `@PreAuthorize("hasRole('ADMIN')")`：普通用户 token → 403（首轮回放发现），admin token → 200 ✅，符合 SSRF/成本防护设计。
- `/api/admin/**`（除 login 与 public 公告）：普通用户访问应 403，admin → 200 ✅。

---

## 四、模块遍历 & 三端代码级检测（check_modules.py）

### 4.1 Web 前端路由完整性
- 路由表 26 条，对应 `.vue` 组件 **26/26 全部存在**，无悬空路由。
- API 层：`frontend/src/utils/request.js` 封装 + baseURL 配置 ✅。

### 4.2 移动端视口
- 全仓 `@media` 块 43 处；断点命中：`768px`×12、`480px`×11、`320px`×7。
- 项目采用 Bootstrap 风格断点（1200/1024/992/768/480/320px）：375px 手机视口由 `max-width:480px` 覆盖，491px 平板由 `max-width:768px` 覆盖——**响应式适配完备，非缺陷**。

### 4.3 Electron 桌面端
| 检查项 | 结果 |
|---|---|
| main.cjs / preload / tray 存在 | ✅ |
| BrowserWindow | ✅ |
| frame:false 无边框 | ✅ |
| Tray 托盘 | ✅ |
| 精灵（DesktopElf） | ✅ |
| loadURL/loadFile | ✅ |

原始数据：`scripts/reports/modules_report.json`

---

## 五、修复清单

> 本轮检测**未发现后端/前端业务代码的 P0/P1 缺陷**（接口 91/91、三端代码级、构建、单测基线全部绿）。
> 发现并修复的问题均集中在**检测脚本自身**，修复后复测全部通过。

| # | 严重度 | 问题描述 | 根因 | 修复方式 | 验证结果 |
|---|---|---|---|---|---|
| F1 | P1（脚本） | 首轮冒烟 29 个端点 400，含用户/好友/私聊/通知/精灵/隐私等模块 | 冒烟脚本未传 Controller 声明的必填 query 参数（`userId`/`userIds`/`username`/`elfId`/`query`/`friendId`） | 为各端点补齐正确参数：好友/通知/推荐等加 `userId=82`；私聊历史加 `userId=82&friendId=83`；精灵子端点加 `elfId=2`（先调 `/api/elf/get` 取得）；搜索 quick 改用 `query=`；好友搜索改用 `username=` | 复测 91/91 全 200，0 失败 |
| F2 | P1（脚本） | `/api/external/*`、`/api/crawler/*` 返回 403 | 这两个 Controller 类级 `@PreAuthorize("hasRole('ADMIN')")`，脚本误用普通用户 token | 鉴权角色由 `user` 改为 `admin`（安全设计本身正确，非业务 bug） | admin token 访问全 200，鉴权矩阵验证通过 |
| F3 | P2（脚本） | `check_modules.py` 运行即 SyntaxError | f-string 括号笔误：`...存在问题}` 多一个 `}` | 拆为变量拼接：`msg = (...) + RESET` | 脚本正常运行，退出码 0 |
| F4 | P2（脚本） | 移动端断点初检报 375px/491px "未命中" | 检测脚本硬编码了建议断点，与项目实际 Bootstrap 断点（768/480/320）不符，造成误报 | 断点清单对齐项目实际，并加注释说明 375/491 由相邻断点覆盖 | 三档断点全部命中，无误报 |

**业务代码回归**：本轮未修改任何 `backend/` 或 `frontend/` 业务源码，故无需补业务单测；现有后端 794 用例 / 前端 586 用例基线保持全绿（安全约束零回退、数据保留未删）。

---

## 六、最终测试统计

| 指标 | 数值 |
|---|---|
| 后端用例数 | **794**（88 测试类；0 失败 / 0 错误 / 35 跳过） |
| 前端用例数 | **586**（41 测试文件；全通过） |
| 接口冒烟 | 91/91 通过，0 失败，0 慢请求 |
| 端口健康 | 4/4 |
| 三端代码级 | 路由 26/26、Electron 8/8、移动端断点齐全 |
| 后端构建 | `mvnw -o compile` ✅ |
| 前端构建 | `npm run build` ✅（2m18s） |

---

## 七、产物清单

| 产物 | 路径 |
|---|---|
| 测试方案（v2.0） | `测试方案.md` |
| 一键检测入口 | `scripts/run_all_checks.py` |
| 端口检测 | `scripts/check_ports.py` |
| 接口冒烟 | `scripts/smoke_api.py` |
| 模块/三端检测 | `scripts/check_modules.py` |
| 本报告 | `scripts/reports/check_report.md` |
| 机器可读报告 | `scripts/reports/ports_report.json`、`smoke_report.json`、`modules_report.json`、`overview.json` |
