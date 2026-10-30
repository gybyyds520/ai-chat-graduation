# 会话 Redis 化方案（阶段二 P1）

> 状态：**方案文档 + 本地可验证部分已落地；多实例会话迁移标注「待外部条件（需 Redis 服务器）」**。

## 一、现状梳理

| 维度 | 现状 |
|------|------|
| 鉴权方式 | 无状态 JWT（`SessionCreationPolicy.STATELESS`），服务端不保存会话，登录态由前端携带 `Authorization: Bearer <token>` |
| HTTP 会话 | 未启用 HttpSession（无状态），天然无会话粘性问题 |
| 已有 Redis 基建 | `spring-boot-starter-data-redis` + Lettuce 连接池已引入；`RedisConfig` 配置了 `RedisTemplate<String,Object>`（String key + JSON value）与 `CacheManager`（默认 TTL 5 分钟）；`CacheService` 封装了 get/set/incr |
| 现存内存态 | `VoiceCallServiceImpl` 使用 `ConcurrentHashMap<String, VoiceSession>` 保存语音通话会话；登录失败计数、限流等部分临时状态散落内存 |

**结论**：真正需要「Redis 化」的不是 JWT 本身（JWT 本就无状态），而是**服务端需要感知、且多实例部署后必须共享的内存态**——典型如：在线状态、token 黑名单（登出/改密后强制失效）、单点登录互踢、语音会话路由、验证码/登录失败计数。

## 二、目标

1. 将上述内存态统一迁移到 Redis，实现**无状态水平扩展**（多实例任意节点均可读写同一份会话）。
2. 利用 Redis TTL 自动过期，替代手写定时清理。
3. 复用已有 `CacheService`，避免重复造轮子。

## 三、迁移步骤

按风险从低到高分三批推进：

### 第 1 批：只读/计数类（低风险，可先落地）
- 登录失败计数、短信验证码、限流计数 → 已可直接用 `CacheService.incr` / `set(key, ttl)`。
- key 规范：`auth:fail:{userId}`、`sms:code:{phone}`、`ratelimit:{api}:{userId}`。

### 第 2 批：登出/改密强制失效（中风险）
- 登录成功后在 Redis 写入 `session:token:{jid} = userId`，TTL 与 JWT 有效期一致；
- `JwtAuthenticationFilter` 校验 JWT 签名后，**额外查 Redis 该 token 是否仍在会话集合中**；
- 登出 / 修改密码 / 管理员踢人时删除对应 key（即黑名单语义）。

### 第 3 批：语音会话与在线状态（中高风险）
- `VoiceCallServiceImpl` 的 `ConcurrentHashMap` 改为 `RedisTemplate`：`voice:session:{callId}`，含 TTL 心跳续期；
- 在线状态 `presence:user:{userId}` 用 Redis 哈希 + 心跳过期，网关层据此路由 WebSocket。

## 四、配置示例（已在 application.yml 落地）

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DB:0}
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
          max-wait: 3000ms
  cache:
    type: redis
    redis:
      time-to-live: 300000   # 5 分钟默认 TTL
      cache-null-values: false
      use-key-prefix: true
```

会话读写示例（复用 `CacheService`）：

```java
// 写入会话
cacheService.set("session:token:" + jid, userId, Duration.ofMinutes(30));

// 校验会话是否仍有效
Object active = cacheService.get("session:token:" + jid);

// 登出/强制下线
cacheService.delete("session:token:" + jid);
```

## 五、风险与回滚

| 风险 | 影响 | 缓解 / 回滚 |
|------|------|-------------|
| Redis 不可用导致登录全部失败 | 可用性下降 | 第 2 批开启「Redis 降级」开关：查不到会话时仅依赖 JWT 签名校验（与现状一致），仅记告警；出问题即关开关回退 |
| 会话 key 未设置 TTL 造成内存泄漏 | Redis 内存上涨 | 所有会话 key 必须带 TTL；上线前用 `--bigkeys` / `INFO memory` 巡检 |
| 多实例时钟不一致导致 TTL 偏差 | 会话提前/延后过期 | 使用 Redis 服务端时间（TTL 由 Redis 计算），不依赖应用本地时间 |
| 历史内存态未清理干净 | 双写不一致 | 灰度期双写内存+Redis，读优先 Redis；确认稳定后再下线内存 Map |

**回滚策略**：每一批次都通过配置开关（如 `auth.session.redis-enabled`）控制；异常时关闭开关，系统自动退化为当前 JWT 无状态模式，无需回滚代码。

## 六、本地可验证部分 vs 待外部条件

- **已落地（本地可验证）**：
  - `spring-boot-starter-data-redis`、`commons-pool2`、`spring-boot-starter-cache` 依赖；
  - `RedisConfig`（RedisTemplate + CacheManager）、`CacheService` 封装；
  - `application.yml` 的 Redis 连接池与缓存配置；
  - 单元测试已覆盖 `CacheService` / `NotificationSetting` 等不依赖真实 Redis 的逻辑。
- **待外部条件（需 Redis 服务器）**：
  - 第 2、3 批的真实多实例会话写入与 `JwtAuthenticationFilter` 集成校验；
  - 需要可用的 Redis 实例（`REDIS_HOST` / `REDIS_PORT`）与压测/预发环境验证水平扩展。
