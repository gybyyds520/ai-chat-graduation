# CI/CD 流水线指南

> 阶段三 P2 新增。本文档说明本地与云端 CI 流水线的构成、运行方式与接入步骤。

## 一、流水线概览

| 步骤 | 命令 | 作用 | 失败即退出 |
|------|------|------|-----------|
| 1. 后端编译 | `backend/aichat/mvnw -q compile` | 验证 Spring Boot 3.5.16 / Java 25 主源码可编译 | 是 |
| 2. 后端测试 | `backend/aichat/mvnw -q test` | 跑全部后端单测（基线 809 用例，0 失败 35 跳过） | 是 |
| 3. 前端测试 | `frontend/npm run test` | 跑 Vitest 单测（基线 595 用例全绿） | 是 |
| 4. 前端构建 | `frontend/npm run build` | Vite 生产构建，验证路由懒加载分包成功 | 是 |
| 5. 自动化检测 | `python scripts/run_all_checks.py` | 端口/接口冒烟/模块遍历汇总 | 见下文 |

步骤 5 依赖**已启动**的后端实例（127.0.0.1:8080）。本地 CI 脚本会先探测 8080 端口：未监听则自动跳过并提示，不视为 CI 失败；监听中则强制执行，失败即非 0 退出。

## 二、本地运行方式

前置条件：JDK 25、Node.js 18+（含已安装的 `frontend/node_modules`）、Python 3。

在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/ci_pipeline.ps1
```

脚本行为：
- 任意一步失败立即 `exit 1`，并打印 `[CI][FAIL]`；
- 全部通过打印 `[CI][OK] 全部 CI 步骤通过`；
- 步骤 5 因无运行实例而跳过时打印黄色提示，不阻断。

## 三、接入 GitHub Actions

仓库已提供 `.github/workflows/ci.yml`：

- 触发条件：push 到 `main`/`master`，或向 `main`/`master` 提 Pull Request；
- 两个并行 job：
  - `backend`：Ubuntu 上 JDK 25 + Maven 缓存，执行 compile + test；
  - `frontend`：Ubuntu 上 Node 20 + npm 缓存，执行 `npm ci` + test + build。

接入步骤：
1. 将本仓库推送到 GitHub；
2. 确保 `frontend/package-lock.json` 已提交（`npm ci` 依赖它；若尚未提交，先 `npm install` 后提交）；
3. 推送代码后，在仓库 **Actions** 标签页即可看到 `CI` 工作流运行；
4. 可在分支保护规则中要求 `backend` 与 `frontend` 两个 job 全部通过方可合并。

## 四、接入 GitLab CI（可选）

如需迁移到 GitLab，新增 `.gitlab-ci.yml`，沿用同一套命令即可：

```yaml
stages: [backend, frontend]
backend:
  stage: backend
  image: eclipse-temurin:25-jdk
  before_script: cd backend/aichat
  script: [./mvnw -B -q compile, ./mvnw -B -q test]
frontend:
  stage: frontend
  image: node:20
  before_script: cd frontend && npm ci
  script: [npm run test, npm run build]
```

## 五、与既有脚本的关系

- `scripts/run_all_checks.py`：运行期健康检测（端口/冒烟/模块），需要实例在线，不属于编译期门禁；
- `scripts/ci_pipeline.ps1`：本任务新增的**编译期门禁**，不依赖在线实例即可跑步骤 1–4；
- 二者互补：本地提交前跑 `ci_pipeline.ps1`，部署后跑 `run_all_checks.py`。
