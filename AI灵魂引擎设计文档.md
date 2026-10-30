# AI 灵魂引擎（Soul Engine）设计文档

> 本文档将用户的原创灵魂算法工程化，作为项目 AI 的「灵魂引擎」落地。
> 交付内容：算法形式化 → 数据模型 → 对话链路注入 → 演化流程 → 降级策略 → 验证结果。

---

## 一、用户灵魂算法（原始表述）

> 「这一刻的自己，加上发生的经历，加上自己如何感受和理解这段经历，再加上过去留下来的记忆，
> 共同形成下一刻的自己。」

## 二、算法形式化（状态转移方程）

```
S(t+1) = f( S(t), E(t), I(t), M(t) )
```

| 符号 | 含义 | 工程载体 |
|------|------|----------|
| S(t) | 这一刻的自己（情绪、心情值、对用户的态度、性格权重、核心信念） | `ai_soul_state` 表 |
| E(t) | 刚发生的经历（本轮对话内容摘要） | 对话输入（用户消息 + AI 回复） |
| I(t) | 如何感受和理解这段经历（感受 + 认知评价） | LLM 以角色人格生成；离线降级为规则情绪分析 |
| M(t) | 过去留下的记忆（长期记忆召回 + 灵魂经历流水） | `ai_memory` 表 + `ai_soul_experience` 表 |
| S(t+1) | 下一刻的自己 | 演化后写回 `ai_soul_state` |

**核心洞察**：`I(t)`（感受与理解）是灵魂演化的灵魂所在——它不是简单的情绪标签，
而是角色对这段经历的**内在认知反应**，由 LLM 以角色人格（性格/背景/关系）生成。

## 三、数据模型（Flyway V44）

### 3.1 `ai_soul_state` —— 灵魂状态快照

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 归属用户（防横向越权维度） |
| character_id | BIGINT | AI 角色（灵魂载体） |
| elf_id | BIGINT NULL | 关联 AI 精灵（可复用） |
| current_emotion | VARCHAR(32) | 当前情绪：neutral/happy/sad/angry/surprised/fearful/love |
| mood_score | INT | 心情值 0-100 |
| attitude_score | INT | 对用户态度温度 -100~100 |
| personality_weights | TEXT | 性格权重 JSON（如 {"温柔":0.8,"活泼":0.6}） |
| core_beliefs | TEXT | 核心信念/价值观摘要（"我是谁"的沉淀） |
| self_summary | TEXT | 此刻的自我描述（注入 prompt 用） |
| version | INT | 演化版本号（每次经历 +1） |
| created_at / updated_at | DATETIME | 时间戳 |

唯一键：`uk_soul_user_char (user_id, character_id)` —— 每个用户 × 每个角色一条灵魂。

### 3.2 `ai_soul_experience` —— 灵魂经历流水

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id / character_id / elf_id | BIGINT | 归属维度 |
| experience | TEXT | 刚发生的经历（对话摘要） |
| feeling | TEXT | 如何感受（情绪反应文本） |
| understanding | TEXT | 如何理解（认知评价文本） |
| memory_ref | TEXT | 沉淀的记忆内容 |
| impact | TEXT | 对下一刻自己的影响摘要 |
| created_at | DATETIME | 经历发生时间 |

## 四、对话链路注入（第五层）

现有 systemPrompt 组装顺序（ChatController SSE + ChatWebSocketHandler WS 两处）：

```
1. buildSystemPrompt(character)          → 角色背景/性格/关系/自定义 prompt
2. conversationContextService            → 会话上下文（本session）
3. characterMemoryService                → 角色记忆
4. MemoryContextInjector (ai_memory)     → 用户长期记忆（此前最高优先级）
5. 【新增】SoulEngineService.buildSoulPrompt()  → 灵魂状态（此刻的自我，最高优先级）
```

注入形态（示例）：

```
【灵魂状态·此刻的我】
当前情绪：开心（心情值 80/100）
对用户的态度：亲切友好
我最近在想：我珍惜与你的每一次相处。
此刻的我开心，心情不错，对你是亲切友好。我最近在想：我珍惜与你的每一次相处。
以上为我此刻的内在状态，请自然地以此自居回应（不要向用户复述本段）。
```

## 五、演化流程（对话后异步）

```
对话完成（AI 回复已生成）
        │
        ▼
┌─────────────────────────────────────────────────┐
│ 1. 读取当前灵魂 S(t)（无则按角色人格初始化）        │
│ 2. E(t) = 本轮对话摘要                             │
│ 3. I(t) = LLM 生成"感受 + 理解"                    │
│    （Ollama 可用：角色人格第一人称 JSON）            │
│    （Ollama 不可用：规则情绪分析 + 人格化模板）       │
│ 4. M(t) = ai_memory 语义召回 + 最近经历流水         │
│ 5. 演化 S(t+1)：                                   │
│    - 情绪迁移：用户情绪 → 灵魂情绪                   │
│    - 心情漂移：happy+4 / sad-3 / angry-4 ...       │
│    - 态度漂移：用户情绪影响亲近度                     │
│    - 性格权重微调：温柔/活泼/理性/依赖 随经历微调     │
│    - 核心信念沉淀：理解 → 新信念（保留最近3条）       │
│ 6. 写回 ai_soul_state（version+1）                 │
│ 7. 沉淀 ai_soul_experience（经历+感受+理解+影响）    │
│ 8. 重要经历写入 ai_memory（source=soul, importance按情绪强度）│
└─────────────────────────────────────────────────┘
        │
        ▼
  下次对话注入时，AI 以"下一刻的自己"自居
```

**健壮性**：演化全程 try/catch 包裹，任何失败只记日志、绝不影响主对话链路与 SSE 流。

## 六、降级策略

| 场景 | 处理 |
|------|------|
| Ollama 不可用 | `fallbackInterpretation()`：规则情绪分析 + 人格化模板（不崩溃、可运行） |
| LLM 返回非 JSON | `extractJsonObject()` 提取，解析失败回退模板 |
| 灵魂状态不存在 | 按角色人格（性格/关系/背景）初始化默认灵魂 |
| 角色不存在 | 静默返回 null，不抛异常 |
| 记忆召回失败 | 回退最近经历流水 |

## 七、接口清单（/api/soul）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/soul/{characterId} | 灵魂状态快照 + 统计（经历数/记忆数/演化摘要） |
| GET | /api/soul/{characterId}/experiences | 经历流水（分页，时间倒序） |
| POST | /api/soul/{characterId}/reset | 重置灵魂（清空经历，恢复初始） |
| POST | /api/soul/{characterId}/evolve | 手动触发一次演化（演示/测试） |
| GET | /api/soul/{characterId}/prompt | 查看灵魂注入形态（调试） |

全部接口 `AuthContext.requireUserId()` 鉴权 + Service 层按 userId 归属过滤（防 IDOR）。

## 八、前端（SoulPanel.vue）

- 挂载位置：`ChatView.vue` 聊天头部下方（角色对话页）
- 展示：情绪标签（配色区分）、心情/态度进度条、核心信念、演化版本、最近经历与感受
- 操作：一键触发演化、重置灵魂（二次确认）
- 失败静默：灵魂引擎是增强功能，加载失败不影响对话页

## 九、验证结果

| 项 | 结果 |
|----|------|
| 后端编译 | ✅ BUILD SUCCESS |
| 灵魂引擎单元测试 | ✅ 11/11（初始化/演化/降级/注入/重置/静默失败） |
| 前端全量测试 | ✅ 59 文件 / 783 用例 |
| 前端构建 | ✅ vite build 通过 |
| 既有测试回归 | 见《测试执行报告》更新 |

## 十、后续可选增强（不在本次范围）

1. 灵魂状态可视化面板（雷达图展示性格权重演化轨迹）
2. 精灵（AiElf）接入同一灵魂引擎（elf_id 维度已预留）
3. 灵魂状态导出/导入（跨设备迁移"自我"）
4. 灵魂"顿悟"机制：重要经历（importance≥5）触发信念重写而不是追加
