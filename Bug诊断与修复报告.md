# Bug 诊断与修复报告

> 诊断日期：2026-09-30
> 依据：doubao-coding-diagnose-and-fix-bugs Skill（建立故障基线 → 找到原因 → 修复并复验）
> 项目：AI 角色社交平台（Spring Boot 3.2.5 + Vue3 + Electron）

---

## 一、故障基线（建立时实测）

| 验证项 | 基线结果 |
|---|---|
| 前端 `npm run test` | 59 文件 / 783 用例全绿（干净复跑为磁盘全量 61 文件 / 805 用例，见说明） |
| 后端 `mvnw test` | 1035 用例 / 0 失败 / 0 错误 / 35 跳过，BUILD SUCCESS |
| 前端 `npm run build` | 通过（1m41s） |
| 后端 `mvnw clean compile` | 通过（BUILD SUCCESS） |
| 接口冒烟 `run_all_checks.py` | 91/91 通过，0 失败 |
| 端口 | 5187 前端 ✅ / 8080 后端 ✅ / 3306 MySQL ✅ / 11434 Ollama ❌（环境未启动） |

**结论：测试与构建层面无失败**。因此本轮以"代码级诊断 → 定位真实潜在缺陷 → 原因级修复"为主线，对前后端全部功能模块做了只读诊断，共确诊 **明确缺陷 18 项**（后端 9 + 前端 9）并全部修复。

---

## 二、后端诊断与修复（9 项）

### P0（1 项，安全）

| 项 | 根因（触发条件→机制→症状） | 修复 | 回归测试 |
|---|---|---|---|
| WebSocket 冒充任意用户 | 握手无/无效 token 时 `getUserIdFromToken` 回退解析 URL `?userId=`；攻击者可指定任意 userId 收发私聊/群聊 | 删除 userId 参数回退分支，只认 `token=`；无 token 以匿名连接不注册身份 | ChatWebSocketHandlerAuthTest（3 例） |

### P1（8 项）

| 项 | 根因 | 修复 | 回归测试 |
|---|---|---|---|
| 短信验证码可穷举 | verifySmsCode 无失败次数限制，6 位验证码可暴力尝试 | 失败 5 次/10 分钟窗口锁号（CacheService 计数），成功即清零；Redis 不可用静默降级不阻断 | AuthSmsRateLimitTest（3 例） |
| 支付审核状态不流转 + 并发叠加 | APPROVED 后 paymentStatus 仍 PENDING；并发审核各自累加会员时长 | `selectById` + `UpdateWrapper` CAS（eq id + review_status=PENDING），抢不到行锁抛已审核；APPROVED 同步 paymentStatus=SUCCESS | PaymentReviewCasTest（2 例） |
| 配额 check-then-act 竞态 | 先查后改，并发可超额 | `UserQuotaMapper` 新增原子 `@Update tryConsumeQuota`（used_count<ceiling 才 +1），affected=0 抛 QUOTA_EXCEEDED | QuotaServiceImplTest/SwitchTest 更新断言（11 例） |
| 群消息历史无成员校验 | getGroupMessages 未校验登录与群成员身份 | 签名加 userId，impl 先 findMember 校验，非成员抛 NOT_GROUP_MEMBER | GroupChatMessagesAuthTest（2 例） |
| 用户 PII 遍历泄露 | getUserById/getUserProfile 返回 phone/email/lastLoginIp 等 | 非本人请求走 sanitizeForPublic 剥离敏感字段；本人不动 | UserPublicProfileSanitizeTest（2 例） |
| 无 exp token 永久有效 | validateToken 仅当 expTime>0 才校验过期，缺 exp 永不过期 | 改为 expTime<=0 一律拒绝 | TokenUtilsTest +2（共 19 例） |
| 文件路径穿越 | sendFileMessage 的 filePath 客户端可控 | isSafeFilePath 拒绝 Windows 盘符与任何 `..` 分量，非法返回 null 不落库 | FriendFileTraversalTest（3 例） |
| 删用户不清理关联数据 | AdminController.deleteUser 物理删行，遗留对话/好友/记忆 | 改 @Transactional 软删 + 级联：删 chat/friend 消息、friendship，置 accountStatus=deleted、清 PII、用户名加后缀 | AdminDeleteUserSoftTest（1 例） |

**后端修改文件**：ChatWebSocketHandler、TokenUtils、PaymentServiceImpl、UserQuotaMapper、QuotaServiceImpl、GroupChatService(+Impl)、GroupChatController、UserController、FriendChatServiceImpl、AdminController、CacheService、SmsCodeMapper、AuthServiceImpl + 7 个新建回归测试类。

---

## 三、前端诊断与修复（9 项）

本轮重点：上一轮"鲜活科技感"4 文件（LoginView/RegisterView/ParticleEffect/theme.css）引入的真实缺陷。

| 项 | 根因 | 修复 |
|---|---|---|
| P1-5 路由主题闪烁 | router afterEach 未存主题时强制回退 light，与 useTheme 跟随系统冲突；系统暗色用户每次路由切换闪白 | 删除 afterEach 主题重设块（useTheme.initTheme 已统一处理） |
| P1-2 RegisterView 硬编码暗色 | 注册页色值写死 #0f172a 等，亮色主题下整页仍暗色、与 LoginView 不一致 | 全部替换为 var(--bg-primary)/var(--gradient-card)/var(--text-*) 等 CSS 变量，网格线改靛蓝，补 fadeIn keyframes |
| P1-1 ParticleEffect reduced-motion 失效 | CSS 媒体查询管不到 requestAnimationFrame 粒子循环，前庭障碍用户仍看全屏粒子 | onMounted 读 matchMedia('(prefers-reduced-motion: reduce)')，命中不启动 animate；监听 change 动态启停，卸载清理 |
| P1-3 全局 h1 渐变 + 无限 titleGlow | 裸 h1~h6 强制渐变透明文字 + h1 无限 drop-shadow 动画，影响管理后台/弹窗所有标题 | 全局标题收窄为中性排版，删除全局 titleGlow；渐变收窄到 .login-header/.register-header 作用域 |
| P1-4 鼠标粒子无节流无上限 | 每次 mousemove 同步 push 10 粒子，高刷屏无界增长 | requestAnimationFrame 合帧 + 硬上限 MAX_MOUSE_PARTICLES=200，超限丢最老 |
| P2-1 canvas DPR 模糊 | canvas 未乘 devicePixelRatio | 后备存储按 dpr 放大 + setTransform，绘制用 CSS 像素 |
| P2-3 resize 无防抖 | 移动端地址栏显隐频繁重建粒子 | 180ms debounce，挂载仍立即初始化 |
| P2-6 token 写入 "undefined" | 登录响应缺 token 时 localStorage.setItem('token', undefined) 污染登录态 | if (!token) throw，走失败提示不落库 |
| P2-5 全局 button 扫光越界 | 全局 button::before 扫光 + overflow:hidden 影响 el-button/标题栏按钮 | 收窄到 .btn-primary/.login-btn/.register-btn 作用域 |

**前端修改文件**：router/index.js、views/RegisterView.vue、styles/theme.css、components/ParticleEffect.vue、views/LoginView.vue。

---

## 四、修复后验证（原场景重跑 + 回归）

| 验证项 | 修复后结果 |
|---|---|
| 前端 `npm run test` | **61 文件 / 805 用例全绿，0 失败** |
| 前端 `npm run build` | ✅ 通过（2m56s，exit 0） |
| 后端 `mvnw -o clean test` | **Tests run: 1067, Failures: 0, Errors: 0, Skipped: 35, BUILD SUCCESS** |
| 接口冒烟 `run_all_checks.py` | **91/91 通过，0 失败**（Ollama 11434 未启动，环境项） |
| 端口 | 5187 ✅ / 8080 ✅（已重启）/ 3306 ✅ / 11434 ❌ 环境未启动 |

**新增回归用例**：后端 +32（1035→1067），前端无新增（改动为样式/DOM 动画/路由副作用，以 build + 既有 theme 系列测试 + 代码审查为证据，未强行抽取纯函数）。

---

## 五、过程中排除的干扰项

1. **后端测试中途 30 个 error**：根因是运行中的 spring-boot:run 进程锁定 target/classes，导致增量编译内部类 .class 缺失（FileNotFoundException、NoClassDefFound），与业务改动无关。停掉 dev 进程后 `clean test` 一次全绿。修复后已重启后端恢复服务。
2. **前端基线 59→61 文件**：原记录 59/783 为测试收集不全；磁盘实际 61 个测试文件，干净复跑 805 用例全绿（含此前偶发 getRandomWait 随机抖动，磁盘断言与源码一致后稳定）。
3. **接口冒烟端口 3/4**：Ollama 11434 未启动为环境状态（本机未拉起服务），非代码缺陷；91 接口全过说明 AI 相关端点走降级路径正常。

---

## 六、遗留事项（如实披露）

### 未修项（后端 P2，超出本轮 P0+P1 明确缺陷范围，建议后续立项）
N+1 查询、全表 selectList(null) OOM 风险、内存态验证码多实例失效、头像不校验魔数、重新生成先删后调 LLM 丢数据、敏感词正则 O(n·m)、备份明文且增量不清理、SSE 线程池 CallerRunsPolicy 阻塞、手机号尾 4 位用户名碰撞、计数器 null NPE、改密后旧 JWT 不失效、多设备 WS 会话覆盖、token 走 URL 进日志、resetDailyQuotas 误调清空全表。

### 契约冲突 / 证据不足项（诊断已标注，未擅动，需业务确认）
空会话归属探测放行、/api/chat/stream/** SecurityConfig 层 permitAll、/uploads/** 与 /actuator/** 公开面、proxyRequest 异常 message 回显、风险评估含随机因子。

### 环境项
- Ollama（11434）未启动：需 `ollama serve` 或启动应用后 AI 对话/搜索/视觉能力才可用（当前走降级）。
- 后端已由本轮重启恢复（spring-boot:run 后台，DB_PASSWORD=123456）。

### 用户数据
本轮全程未删除任何用户数据；AdminController.deleteUser 由物理删改为软删，数据可追溯。

---

## 七、结论

- **明确缺陷 18 项全部原因级修复**（后端 P0×1 + P1×8；前端 P1×5 + P2×4），每个修复附回归测试或可验证证据。
- **测试基线不回退且上升**：后端 1035 → **1067**（+32，0 失败），前端 783/805 全绿。
- **构建与运行态回归通过**：前后端 build、clean compile、91/91 接口冒烟全部通过。
- 安全基线（requireSelf/requireUserId、@PreAuthorize、GlobalExceptionHandler 不回显、日志脱敏）零回退；未引入任何付费依赖。
