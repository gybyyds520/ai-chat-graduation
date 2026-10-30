# 后端只读代码诊断报告（建立故障基线阶段）

- 项目：`backend/aichat`（Spring Boot 3.2.5 / Java 17）
- 基线：1035 用例 0 失败 35 跳过；clean compile 通过
- 方法：只读静态代码审查，未修改任何文件，未运行 mvn/服务
- 分类：**明确缺陷** / **契约冲突** / **环境相关** / **证据不足**

严重度定义：P0=可被利用直接造成越权/资金/数据泄露；P1=特定条件下数据不一致或绕过防护；P2=体验/性能/可维护性问题。

---

## 一、P0 明确缺陷

### P0-1 WebSocket 连接在无 token 时回退到 URL `userId` 参数 → 任意用户身份冒充
- 文件：`websocket/ChatWebSocketHandler.java`
- 行号：162-168（`getUserIdFromToken` 的"兼容旧客户端"分支）
- 触发条件：客户端建立 WS 连接时不带 token，仅在 query 中传 `?userId=<目标ID>`。
- 实际机制：第 151 行 token 分支不命中时，代码直接 `Long.parseLong(queryUserId)` 并返回；随后 `afterConnectionEstablished` 把该 userId 写入 `userIdToSession`/`sessionIdToUserId`。后续 `handleFriendMessage`/`handleGroupMessage`/`handleRead` 全部以这个 userId 作为发件人身份。
- 原始症状：攻击者以 `?userId=1` 连接即可冒充 userId=1 的用户收发私聊/群聊、标记消息已读、触发通知已读回调。
- 证据：代码注释自称"不构成身份凭证"，但返回值被信任为发件人；与第 104-115 行的 token 校验逻辑自相矛盾。
- 是否值得修复：**必须修**。删除该回退分支，无 token 直接拒绝建立有身份连接。

---

## 二、P1 明确缺陷

### P1-1 短信验证码登录接口无尝试次数限制 → 6 位验证码可暴力破解
- 文件：`service/impl/AuthServiceImpl.java`（`verifySmsCode` 第 75-82 行）；`controller/AuthController.java`（`/sms/login` 第 55-71 行）
- 触发条件：攻击者截获/猜得手机号后，对 `/api/auth/sms/login` 反复提交 6 位数字验证码。
- 实际机制：`/sms/send` 有 `@RateLimiter("smsApi")`，但 `/sms/login` 没有任何限流/失败计数；`verifySmsCode` 不记录失败次数、不锁定手机号。6 位空间 100 万次，按 100 QPS 约 2.7 小时可穷举。
- 原始症状：任意手机号可被接管登录。
- 证据：`UserServiceImpl.login` 对用户名密码登录有 5 次锁定，但短信登录路径完全没有对应防护；`SmsCodeMapper.markAsUsed` 仅在成功后调用。
- 建议：按手机号做失败计数（Redis 5 次/10 分钟），并限制单码有效期内尝试次数；新码下发时应使旧码失效。

### P1-2 支付审核通过后 `paymentStatus` 永远停留在 PENDING；并发审核可重复升级会员
- 文件：`service/impl/PaymentServiceImpl.java`
- 行号：102-129（`reviewPayment`），131-152（`upgradeUserVersion`）
- 触发条件：(a) 任意一笔 APPROVED 订单落库后；(b) 两个管理员同时审核同一 PENDING 订单。
- 实际机制：(a) 第 114 行只 set `reviewStatus`，从未 set `paymentStatus="SUCCESS"`，导致支付流水表永远显示待支付；(b) 第 109 行 check-then-act，无乐观锁/`SELECT ... FOR UPDATE`，两个并发事务都读到 `reviewStatus=PENDING`，都通过检查，都调用 `upgradeUserVersion`，会员到期时间被叠加两次 `durationDays`。
- 原始症状：财务对账时支付状态与实际不符；并发审核导致会员时长被重复赠送。
- 建议：APPROVED 分支同步 `paymentStatus="SUCCESS"`；对 `reviewStatus` 加乐观锁（`UPDATE ... WHERE id=? AND review_status='PENDING'`），按影响行数判断是否抢到审核权。

### P1-3 配额扣减 check-then-act 存在并发竞态 → 可超额使用
- 文件：`service/impl/QuotaServiceImpl.java`
- 行号：118-152（`checkAndConsume`）
- 触发条件：同一用户并发发起多个 AI 聊天请求。
- 实际机制：第 119 行 selectOne 读 used_count，第 146 行判断 `used >= ceiling`，第 149 行 `used+1` 写回。两个并发事务都读到 `used=limit-1`，都通过判断，都写回 `limit`，实际消耗 `limit+1`。
- 原始症状：会员用户可突破每日额度上限。
- 建议：改用原子 SQL `UPDATE user_quota SET used_count=used_count+1 WHERE user_id=? AND feature_type=? AND reset_date=? AND used_count < limit_count + bonus_limit`，按影响行数=0 抛 QUOTA_EXCEEDED。

### P1-4 群机器人 CRUD 无群主/成员校验；群消息历史无成员校验
- 文件：`service/impl/GroupChatServiceImpl.java`
- 行号：345-356 `createGroupBot`；369-384 `updateGroupBot`；387-389 `deleteGroupBot`；214-216 `getGroupMessages`
- 触发条件：任意已登录用户调用对应接口，传入他人 groupId/botId。
- 实际机制：三个 bot 写方法直接 `groupBotMapper.insert/update/deleteById`，不查群、不查成员、不查群主；`getGroupMessages` 直接按 groupId 返回消息列表，不调用 `groupMemberMapper.findMember`。
- 原始症状：任意用户可向任意群插入/篡改/删除机器人；可读取任意群的历史消息（含非公开群）。
- 证据：同文件 `sendMessage`(180)、`deleteGroup`(225)、`updateAnnouncement`(282) 都做了权限校验，bot 相关方法明显遗漏。
- 建议：统一补 `findMember`/`ownerId` 校验；`getGroupMessages` 加成员校验后返回。

### P1-5 群 `leaveGroup` 未加事务 → 成员已删但群人数未回滚
- 文件：`service/impl/GroupChatServiceImpl.java`
- 行号：152-163
- 触发条件：删除成员记录成功后、更新 `memberCount` 之前 DB 异常。
- 实际机制：方法上无 `@Transactional`；两步写非原子。同问题也存在于 `CommunityServiceImpl.leaveCommunity`(139-150)、`CommunityServiceImpl.createPost`(191 postCount+1)、`toggleLike`(262/272 likeCount)、`createComment`(349 commentCount)。
- 原始症状：群/社群 memberCount 与实际成员数漂移；点赞数/评论数与真实记录不一致。
- 建议：统一加 `@Transactional(rollbackFor=Exception.class)`；计数器改用原子 UPDATE 而非读改写。

### P1-6 SSRF 防护存在 DNS rebinding TOCTOU 绕过
- 文件：`util/UrlSecurityUtil.java`；`service/impl/ExternalApiServiceImpl.java`
- 行号：`UrlSecurityUtil.validateUrl` 第 53-61 行解析 IP；`proxyRequest`/`fetchUrl`/`downloadFile` 第 249/530/592 行 `URI.create(url)` 后由 HttpClient 再次解析。
- 触发条件：攻击者控制一个域名，首次 DNS 解析返回公网 IP（通过校验），实际 HTTP 请求时 DNS 切换为 `127.0.0.1` 或内网 IP。
- 实际机制：校验与请求使用两次独立 DNS 解析，中间无缓存绑定；HttpClient 不强制连接到校验时拿到的 IP。
- 原始症状：服务端可被诱导访问内网元数据接口（如 `169.254.169.254`）、内网管理端口。
- 建议：校验时解析到 IP 后，直接用 IP 构造请求并保留 Host 头；或使用自定义 `DnsResolver` 绑定单次连接。

### P1-7 微信 OAuth 为 mock 实现，openId 由 code 哈希得出 → 任意 code 可登录任意账号
- 文件：`service/impl/OAuthServiceImpl.java`
- 行号：229-257 `mockWechatApiCall`；246 行 `openId = "wx_" + Math.abs(code.hashCode())`
- 触发条件：(a) 生产部署仍使用该 mock；(b) 任意客户端提交任意 code。
- 实际机制：没有真实微信 code2session 调用；openId 由 code 的 Java hashCode 确定性导出。攻击者知道目标用户 openId 后可反推/碰撞 code；更严重的是 mock 不校验 code 有效性，任何非 `"invalid_code"` 的字符串都能换出一个 openId 并登录/建号。
- 原始症状：OAuth 登录形同虚设；可批量建号或绑定他人微信。
- 建议：生产必须替换为真实微信 API；保留 mock 仅限 `@Profile("dev")`，并在启动时检测。

### P1-8 任意登录用户可通过 `/api/user/{id}`、`/api/user/profile?userId=` 读取他人完整资料（含 email/phone/lastLoginIp）
- 文件：`controller/UserController.java`
- 行号：152-159 `getUserById`；199-206 `getUserProfile`；`entity/User.java` 字段未对 phone/email/lastLoginIp 加 `@JsonIgnore`
- 触发条件：任意已登录用户遍历 userId。
- 实际机制：直接返回完整 `User` 实体；`password`/`mfaSecret`/`securityAnswer` 有 `@JsonIgnore`，但 `phone`、`email`、`lastLoginIp`、`lastLoginTime`、`accountStatus`、`membershipLevel` 全部序列化。
- 原始症状：用户 PII（手机号、邮箱、最近登录 IP）可被批量遍历爬取。
- 证据：`SecurityConfig` 中 GET `/api/profile/**` 是 permitAll，但 `/api/user/{id}` 需登录；即便需登录，登录后任意 IDOR 读取 PII 仍属越权。
- 建议：对外查询接口改用脱敏 DTO（仅返回 nickname/avatar/age 等公开字段）；phone/email 仅本人 `/api/user/me` 可见。

### P1-9 SSE 聊天在角色权限校验前就已扣减配额
- 文件：`controller/ChatController.java`
- 行号：195 `quotaService.checkAndConsume(userId, "ai_chat")` 在 218 `aiCharacterService.canAccess` 之前
- 触发条件：用户传入无权访问的 characterId。
- 实际机制：配额先扣，再校验角色访问权；无权访问时直接 `emitter.complete()` 返回错误，但配额不回滚。
- 原始症状：用户每次试探非法 characterId 都被白扣一次配额。
- 建议：先 `canAccess` + 角色存在性校验通过后再扣配额；或失败时回滚配额。

### P1-10 JWT 无 exp 字段时被视为永久有效
- 文件：`utils/TokenUtils.java`
- 行号：157-173 `extractExpTime`；147 行 `if (expTime > 0 && now > exp)`
- 触发条件：任何能构造出的合法签名 token（目前仅服务端能签发，但若密钥泄露或签名校验被绕过）。
- 实际机制：解析不到 `exp` 时 `extractExpTime` 返回 0，第 147 行条件不成立，token 永不过期。
- 原始症状：一旦签发/泄露无 exp 的 token，永久有效。
- 建议：`expTime <= 0` 直接返回 false（强制要求 exp）。

### P1-11 外部 AI Provider API Key 被硬编码为空字符串
- 文件：`service/impl/ExternalApiServiceImpl.java`
- 行号：348、411、490 `requestBuilder.header(apiKeyHeader, "Bearer ")`
- 触发条件：调用 `aiChat`/`aiCompletions`/`aiEmbeddings`。
- 实际机制：Authorization 头只有 `"Bearer "`（无 key），所有外部 AI 调用必然 401。
- 原始症状：外部模型路由功能完全不可用；若误以为已配置则排障困难。
- 建议：从配置/环境变量注入真实 key，缺失时 fail-fast 而非带空 key 发请求。

### P1-12 好友文件消息 filePath 由客户端控制，未做路径校验
- 文件：`service/impl/FriendChatServiceImpl.java`
- 行号：118-156 `sendFileMessage`
- 触发条件：客户端传 `filePath="../../../etc/passwd"` 或任意已存在路径。
- 实际机制：`filePath` 直接落库；接收端下载/预览时若按该路径读盘，存在路径穿越读任意文件风险。
- 证据：`FileUploadUtil.save` 对上传路径有净化，但此处的 filePath 是客户端直接传入，未走上传工具。
- 建议：filePath 必须由服务端上传后生成（与 upload-avatar 一致），拒绝客户端传入的绝对路径。

### P1-13 管理员删除用户为物理删除且不清理关联数据
- 文件：`controller/AdminController.java`
- 行号：167-174 `deleteUser`
- 触发条件：管理员在后台删除任意用户。
- 实际机制：`userMapper.deleteById(id)` 直接物理删 user 行；聊天、好友、帖子、记忆等表的 user_id 变为悬挂外键。
- 原始症状：查询历史消息/好友列表时 NPE 或脏数据；与用户自助注销的软删除（`accountStatus=deleted`）策略不一致。
- 建议：统一为软删除，或在同一事务内清理/归档关联数据。

### P1-14 WebSocket 通知已读为假操作，未写库
- 文件：`websocket/ChatWebSocketHandler.java`
- 行号：539-550 `handleNotificationRead`
- 触发条件：客户端通过 WS 发 `notification_read` 类型消息。
- 实际机制：仅回写一个 `status:success` 消息，不调用 `notificationMapper.markAsRead`。
- 原始症状：前端误以为已读，但刷新后通知仍为未读。
- 建议：调用 `NotificationService.markAsRead`。

### P1-15 `broadcastToFriends` 实际向所有在线用户广播，而非仅好友
- 文件：`websocket/ChatWebSocketHandler.java`
- 行号：624-637
- 触发条件：任何调用 `broadcastToFriends(userId, msg)` 的位置（如 `UserOnlineStatusServiceImpl.broadcastStatus` 第 170 行）。
- 实际机制：遍历所有 activeSessions，仅排除 `targetUserId == userId`，不查好友关系。
- 原始症状：用户上下线状态对全站所有在线用户可见（隐私泄露 + 名不副实）。
- 建议：真正按好友列表过滤，或重命名方法避免误用。

### P1-16 用户名/手机号枚举
- 文件：`controller/UserController.java`
- 行号：161-175 `forgotPassword`（"该邮箱未注册" vs "已发送"）；394-420 `sendVerificationCode`（"该邮箱/手机号未注册"）
- 触发条件：攻击者批量提交邮箱/手机号。
- 实际机制：不同响应明确区分"未注册"与"已发送"。
- 原始症状：可枚举注册过的手机号/邮箱。
- 建议：统一返回"若该账号存在，验证码已发送"。

---

## 三、P2 明确缺陷（体验/性能/一致性）

### P2-1 `getRecentChats` N+1 查询
- 文件：`service/impl/FriendChatServiceImpl.java:198-249`
- 机制：对每个好友单独查一次最后一条消息。好友数多时慢查询。
- 建议：一次 group 查每个好友最后一条消息。

### P2-2 大批量 `selectList(null)` 全表加载
- 文件：`service/impl/FriendshipServiceImpl.java`（recommendFriends 第 252、438 行 `selectList(null)`）；`service/impl/GroupChatServiceImpl.java:452`；`service/DataBackupService.java:78,101`；`controller/AdminController.java:161`
- 机制：用户量/数据量增长后 OOM 风险。
- 建议：分页/条件查询。

### P2-3 内存态重置令牌/验证码在多实例部署下失效
- 文件：`service/impl/UserServiceImpl.java:41,44`（静态 ConcurrentHashMap）
- 机制：已同步写 Redis，但 Redis 不可用时降级到内存；多实例下令牌发到 A 实例、校验走 B 实例会失败。
- 建议：以 Redis 为唯一存储，删除内存降级。

### P2-4 头像上传仅校验扩展名，不校验文件魔数
- 文件：`controller/UserController.java:375`；`controller/ProfileController.java:52`
- 机制：扩展名白名单，但内容可为任意字节。配合 `/uploads/**` 静态托管，若浏览器对某些类型做 MIME sniffing 仍有 XSS 风险（FileUploadUtil 已禁 svg/html，风险较低）。
- 建议：读取魔数校验图片真实类型。

### P2-5 重新生成回复先删旧 AI 消息再调 LLM，失败即数据丢失
- 文件：`controller/ChatController.java:593-634`
- 机制：`deleteById(messageId)` 后 LLM 失败，旧回复已消失，仅写入"（重新生成失败）"占位。
- 建议：先调 LLM 成功后再删旧消息。

### P2-6 内容审核对每条消息遍历全量敏感词正则
- 文件：`service/impl/ContentAuditServiceImpl.java:96-109`
- 机制：526+ 词 × 每消息 `Pattern.find()`，群聊/私聊高 QPS 下 CPU 开销线性放大。
- 建议：Aho-Corasick 自动机。

### P2-7 备份文件明文落盘，增量备份不清理
- 文件：`service/DataBackupService.java`
- 机制：(a) 备份 JSON 含完整用户数据，无加密；(b) `cleanupOldBackups(7)` 仅在全量备份后调用，增量备份永不清理。
- 建议：备份目录加密/权限收紧；增量也纳入清理策略。

### P2-8 `restoreFromBackup` 未检查 records 为 null，unchecked cast
- 文件：`service/DataBackupService.java:211,216`
- 机制：备份文件无 records 字段时 NPE。
- 建议：防御性判空。

### P2-9 SSE 线程池 CallerRunsPolicy 会阻塞 Tomcat 请求线程
- 文件：`controller/ChatController.java:91-98`
- 机制：队列满后由 HTTP 线程执行任务，5 分钟流处理期间占用请求线程；并发高时拖垮 web 容器。
- 建议：独立异步 + 限流。

### P2-10 手机号注册用户名取后 4 位，碰撞时 insert 失败
- 文件：`service/impl/AuthServiceImpl.java:107`
- 机制：`"user_" + phone.substring(phone.length()-4)`，不同尾号用户撞名时 DB 唯一约束异常 → 500。
- 建议：追加随机后缀或 DB 重试。

### P2-11 好友/社群计数器 null NPE
- 文件：`service/impl/CommunityServiceImpl.java:131,191,349`；`GroupChatServiceImpl.java:145`
- 机制：历史数据 `member_count`/`post_count` 为 null 时 `+1` NPE。
- 建议：`Optional.ofNullable(...).orElse(0)`。

### P2-12 修改密码/注销后旧 JWT 不失效
- 文件：`utils/TokenUtils.java`（无吊销列表）；`service/impl/UserServiceImpl.java:214 changePassword`
- 机制：JWT 24h 内有效，改密/注销后旧 token 仍可用（注销靠 accountStatus 检查兜底，但改密不兜底）。
- 建议：改密后把该用户已签发 token 加入 Redis 黑名单，或缩短有效期 + refresh token。

### P2-13 同一用户多设备 WS 连接互相覆盖会话映射
- 文件：`websocket/ChatWebSocketHandler.java:109`
- 机制：`userIdToSession.put(userId, session)` 后者覆盖前者；先开的设备断开时 `afterConnectionClosed` 会把后者的映射 remove 掉。
- 建议：按 userId 维护 session 集合。

### P2-14 JWT 通过 URL query 传递（SSE 场景）会进入访问日志/浏览器历史
- 文件：`common/security/JwtAuthenticationFilter.java:89-94`
- 机制：GET 允许 `?token=`，会被 Nginx/AccessLog 记录。
- 建议：日志脱敏或改用一次性 ticket 换 SSE token。

### P2-15 `resetDailyQuotas()` 无参版本清空全表
- 文件：`service/impl/QuotaServiceImpl.java:156-158`
- 机制：`userQuotaMapper.delete(new QueryWrapper<>())` 删除所有用户配额。若被误调用或定时触发，全站当日配额清零。
- 建议：改为按 reset_date < today 删除；加调用点审计。

---

## 四、契约冲突 / 证据不足（不判缺陷，需业务确认）

1. **聊天历史归属探测对空会话放行**：`ChatController.getChatHistory:418-424` 用 `getRecentMessages(sessionId,1)` 探测归属；空会话直接放行。因空会话无数据，不构成泄露，但 sessionId 可被猜测后"占位"写入。是否视为漏洞取决于业务对 sessionId 熵的假设。
2. **`/api/chat/stream/**` 在 SecurityConfig 中 permitAll**：虽然方法内自行 `requireUserId()`，但 Spring Security 层不拦截；若未来新增端点忘记加 requireUserId 则完全裸奔。建议移到 authenticated 组。
3. **`/uploads/**` permitAll**：文件按文件名 UUID 命名，无法枚举；但任何拿到 URL 的人（含未登录）可访问。是否符合产品预期需确认。
4. **`/actuator/**` permitAll**：注释称 include 仅 health/metrics/prometheus，需对照 `application.yml` 实际配置确认未暴露 env/beans。
5. **`ExternalApiServiceImpl.proxyRequest` 把 `e.getMessage()` 直接拼进 JSON 返回**：可能泄漏内部 URL/堆栈细节给调用方，GlobalExceptionHandler 不覆盖此类手动返回。
6. **OAuthServiceImpl 风险评估含 `Random` 随机因子**：可能随机拦截合法登录，是否为期望行为需产品确认。

---

## 五、优先修复建议（按投入产出）

1. **本周必改**：P0-1（WS userId 回退）、P1-1（短信登录限流）、P1-2（paymentStatus + 审核乐观锁）、P1-4（群机器人越权）、P1-8（用户 PII 脱敏）。
2. **下个迭代**：P1-3（配额原子扣减）、P1-5（计数器事务/原子 UPDATE）、P1-6（DNS rebinding）、P1-9（配额扣减顺序）、P1-14/15（WS 通知已读 + 广播范围）。
3. **排期优化**：P2 系列（N+1、全表加载、备份加密、线程池、改密 token 失效）。
