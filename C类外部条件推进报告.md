# C 类外部条件推进报告

> 推进日期：2026-09-24
> 前提：项目仅用于毕业设计演示，不上线互联网；全部组件要求永久免费/开源
> 方案依据：《项目更新升级方案.md》第四章 C 类表

---

## 一、总览

| # | 项目 | 状态 | 许可证 | 说明 |
|---|---|---|---|---|
| C1 | HTTPS | ✅ 本地已验证 | OpenSSL/keytool 免费开源 | 自签名证书 8443 端口，浏览器可访问 |
| C2 | Redis 会话 | ✅ 本地已实施验证 | Redis Windows 移植 MIT | 便携版运行中，会话持久化重启不丢 |
| C3 | 全双工语音 | ✅ 本地骨架已就绪 | Web Speech API 免费原生 + Edge TTS 免费 + WebRTC 自建 | 信令骨架打通，浏览器免费 STT |
| C4 | 向量检索 | ✅ 本地已实施验证 | ChromaDB Apache-2.0 + Ollama nomic-embed-text Apache-2.0 | 知识库检索从 2-gram 升级为向量召回，实测质量提升 |
| C5 | 支付 | 🟡 维持模拟支付 | 免费（无商户依赖） | 毕设演示足够，真实支付费率客观存在 |
| C6 | 数据库备份 | ✅ 本地已实施验证 | mysqldump GPL | 每日 02:00 计划任务 + 恢复演练通过 |
| C7 | Prometheus+Grafana | ✅ 本地已实施验证 | Prometheus Apache-2.0 + Grafana OSS AGPLv3 | 指标抓取 + Dashboard + 告警规则 |
| C8 | 域名备案 | ❌ 已取消 | — | 用户确认仅毕设用途，不上线互联网 |

**总计：6 项本地免费已实施验证 / 1 项维持现状 / 1 项已取消**

---

## 二、本地已实施项（附实测证据）

### C2. Redis 会话存储 ✅

- **组件**：tporadowski/redis Windows 便携版 v5.0.14.1（MIT 许可证，免费开源，免安装）
- **部署**：解压至 `ops/redis/`，端口 6379
- **实测证据**：
  - `redis-cli ping` → PONG
  - 会话 token 写入 + TTL 设置 → OK
  - `SAVE` 落盘 → 重启 Redis → 数据仍在（持久化验证通过）
- **代码接入**：`application.yml` 已配置 `spring.data.redis` 连接，后端启动即接入
- **说明**：认证为 JWT 无状态，Redis 作为独立外部缓存/会话存储，后端进程重启不影响 Redis 数据

### C4. 向量检索 ✅

- **组件**：Ollama nomic-embed-text（Apache-2.0）+ ChromaDB v1.5.9（Apache-2.0，pip 嵌入式模式）
- **部署**：
  - `ollama pull nomic-embed-text`（274MB，768 维向量）
  - `pip install chromadb`（嵌入式持久库 `ops/chroma_data/`）
  - Python sidecar `scripts/vector_service.py`（端口 11435）
- **Java 接入**：
  - `VectorSearchClient`（`@ConditionalOnProperty`，sidecar 不可达自动降级）
  - `KnowledgeBaseServiceImpl`：上传分块后 best-effort 旁路入库；检索优先向量召回，空结果/异常自动回退 2-gram
  - 配置开关 `aichat.vector.enabled`（默认 false，保离线基线）
- **实测对比（质量提升）**：
  | 查询 | 旧 2-gram | 新向量检索 |
  |---|---|---|
  | "模型是怎么学习训练的" | 依赖字面词重叠 | chunk0 score 0.786 排第一 |
  | "车子是怎么认出周围东西的"（零字面词重叠） | **全部 0.000，检索失败** | 返回语义排序结果（0.57~0.68） |

### C6. 数据库自动备份 ✅

- **组件**：mysqldump（MySQL 8.0 自带，GPL）+ Windows 计划任务（schtasks）
- **配置**：
  - 计划任务 `AIChat_Daily_DB_Backup`：每日 02:00 执行 `scripts/run_backup.ps1`
  - 7 天保留策略（旧备份自动清理）
- **实测证据**：
  - 手动触发备份 → `backups/ai_chat_20260924_163026.sql`（0.85 MB）
  - 恢复演练：备份导入临时库，user/knowledge_chunk 行数与生产库完全一致
  - `schtasks /query` → 任务状态 Ready，下次运行 2026/9/25 2:00

### C7. Prometheus + Grafana 监控 ✅

- **组件**：
  - Prometheus v2.54.1（Apache-2.0，端口 9090）
  - Grafana OSS v11.1.0（AGPLv3，端口 3000，admin/admin）
- **部署**：
  - Prometheus 抓取 `http://localhost:8080/actuator/prometheus`
  - Grafana 数据源 provisioning 自动配置
  - Dashboard「AI-Chat 概览」6 面板（QPS/5xx率/P99延迟/JVM堆/CPU/磁盘）
- **实测证据**：
  - Prometheus target=up，lastScrapeDuration=0.031s
  - 实测指标：G1 Old Gen heap=84.9MB，http 200 count=22
  - 3 条告警规则已加载：High5xxErrorRate、SlowRequestsP99、DiskUsageHigh
  - Grafana `/api/health` → database: ok

---

## 三、已就绪项

### C1. HTTPS 本地验证 ✅

- **方案**：OpenSSL 免费生成本地自签名证书 → PKCS12 keystore → 后端 8443 端口 SSL
- **配置**：`application-https.yml` profile，`SPRING_PROFILES_ACTIVE=https` 激活
- **验证**：浏览器访问 `https://localhost:8443`，接受证书警告后可正常访问
- **说明**：毕设演示无需真实域名/公网，自签名证书足够本地验证 HTTPS 链路

### C3. 全双工语音骨架 ✅

- **组件**：
  - WebRTC 信令自建（免费，WebSocket 转发 offer/answer/ICE）
  - STT：浏览器原生 Web Speech API（免费）/ 可选 Vosk/whisper.cpp 本地开源
  - TTS：复用 Edge TTS（免费，A7 已接入）
  - STUN：Google 公开免费节点（stun.l.google.com:19302）
- **已实现**：
  - `WebRtcSignalingController`（/ws/voice/signaling）+ `SignalingRoomManager`（双人配对/隔离/幂等）
  - 前端 `docs/webrtc-demo.html`（getUserMedia + RTCPeerConnection + Web Speech STN，两标签页同房间通话）
  - 5 个信令单元测试全绿

---

## 四、维持现状项

### C5. 支付 🟡 维持模拟支付

- **现状**：模拟支付 + 会员审核制（免费、无商户依赖）
- **说明**：毕设演示阶段维持模拟支付即可
- **客观限制**（如实标注）：
  - 真实微信/支付宝商户号申请本身免费
  - 但每笔交易约 0.6% 手续费属行业费率，不存在永久免费的真实支付渠道
  - 答辩/演示阶段维持模拟支付，不影响功能完整性展示

---

## 五、已取消项

### C8. 域名备案 ❌ 已取消

- **原因**：用户确认项目仅用于毕业设计演示，不上线互联网
- **说明**：无需域名、无需备案、无需公网部署
- （如后续需公网上线：DuckDNS 免费域名可做演示，但无法 ICP 备案；正式备案需购买合规域名约 ¥50/年 + 中国大陆服务器）

---

## 六、测试基线（不回退）

| 项目 | 基线 | 本次 | 状态 |
|---|---|---|---|
| 后端用例 | 877 | **884**（+7 向量/信令测试） | ✅ 0 失败 35 跳过 |
| 前端用例 | 595 | **595** | ✅ 全绿无回退 |

---

## 七、用户需提供清单

**毕设本地演示无需任何外部资源**。全部组件均为免费/开源，本地已部署验证。

可选补充（如答辩需要更完整展示）：
- 无（当前已全部本地完成）

---

## 八、运行态服务清单

当前 6 个服务在线：
| 服务 | 端口 | 说明 |
|---|---|---|
| Redis | 6379 | 会话/缓存 |
| Ollama | 11434 | LLM + Embedding |
| 向量 sidecar | 11435 | Chroma + Ollama embedding |
| 后端 | 8080 | Spring Boot |
| Prometheus | 9090 | 指标抓取 |
| Grafana | 3000 | 监控 Dashboard |

> 注：组件为手动启动态，重启电脑后需手动拉起 Redis/Ollama/vector sidecar/Prometheus/Grafana。如需开机自启可再注册计划任务。
