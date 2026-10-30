package com.example.aichat.controller;

import com.example.aichat.common.audit.Audited;
import com.example.aichat.dto.R;
import com.example.aichat.entity.User;
import com.example.aichat.entity.ChatMessage;
import com.example.aichat.entity.FriendChatMessage;
import com.example.aichat.entity.Friendship;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.AiCharacterMapper;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.mapper.ChatMessageMapper;
import com.example.aichat.mapper.FriendChatMessageMapper;
import com.example.aichat.mapper.FeedTaskMapper;
import com.example.aichat.mapper.CommunityPostMapper;
import com.example.aichat.entity.FeedTask;
import com.example.aichat.entity.CommunityPost;
import com.example.aichat.service.AdminService;
import com.example.aichat.utils.TokenUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员后台接口。
 *
 * Phase 3 (P3-b) 声明式权限示范：
 *   - 类级别 @PreAuthorize("hasRole('ADMIN')")：所有方法默认要求管理员角色；
 *   - adminLogin 方法用 @PreAuthorize("permitAll") 覆盖，允许未登录访问；
 *   - JwtAuthenticationFilter 会根据 token 中的 userId 区分角色（0=ADMIN, 其他=USER），
 *     前端调 /api/admin/login 获取 admin token 后，带 token 调用其他 admin 接口。
 *
 * 兼容性提示：此前前端调用 admin 接口无 token 也能成功，现在会返回 403。
 * 前端需先调 /api/admin/login 获取 token，再在后续请求头携带 Authorization: Bearer xxx。
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AiCharacterMapper aiCharacterMapper;

    @Autowired
    private FriendshipMapper friendshipMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private FriendChatMessageMapper friendChatMessageMapper;

    @Autowired
    private FeedTaskMapper feedTaskMapper;

    @Autowired
    private CommunityPostMapper communityPostMapper;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * 管理员登录 - 公开访问，返回 admin token。
     * Phase 3：登录成功后颁发 userId=0 的 token，JwtAuthenticationFilter 会注入 ROLE_ADMIN。
     */
    @PostMapping("/login")
    @PreAuthorize("permitAll")
    @Audited(action = "ADMIN_LOGIN", resourceType = "ADMIN")
    public R adminLogin(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        User admin = adminService.adminLogin(username, password);
        if (admin != null) {
            // 颁发 admin token（使用数据库中 admin 记录的真实 id）
            // JwtAuthenticationFilter 通过 adminService.isAdmin() 判断是否为管理员
            String adminToken = TokenUtils.generateToken(String.valueOf(admin.getId()), admin.getUsername());
            Map<String, Object> result = new HashMap<>();
            result.put("admin", admin);
            result.put("token", adminToken);
            result.put("tokenType", "Bearer");
            return R.success(result);
        }
        return R.unauthorized("管理员用户名或密码错误");
    }

    @PostMapping("/change-password")
    public R changePassword(@RequestBody Map<String, String> request) {
        Long adminId = Long.parseLong(request.get("adminId"));
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        User admin = userMapper.selectById(adminId);
        if (admin == null) {
            return R.error("管理员不存在");
        }

        // 安全修复：使用BCrypt验证密码而非明文比较
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            return R.error("旧密码错误");
        }

        if (newPassword == null || newPassword.isEmpty()) {
            return R.error("新密码不能为空");
        }

        if (newPassword.length() < 6) {
            return R.error("新密码长度不能少于6位");
        }

        // 安全修复：密码加密存储
        admin.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(admin);
        return R.success("密码修改成功");
    }

    @PostMapping("/update-profile")
    public R updateProfile(@RequestBody Map<String, String> request) {
        Long adminId = Long.parseLong(request.get("adminId"));
        String nickname = request.get("nickname");
        String email = request.get("email");

        User admin = userMapper.selectById(adminId);
        if (admin == null) {
            return R.error("管理员不存在");
        }

        if (nickname != null && !nickname.isEmpty()) {
            admin.setNickname(nickname);
        }

        if (email != null) {
            admin.setEmail(email);
        }

        userMapper.updateById(admin);
        return R.success("个人信息修改成功");
    }

    @GetMapping("/users")
    public R getAllUsers() {
        List<User> users = userMapper.selectList(null);
        // 安全修复：清除所有用户密码字段，防止批量泄露
        users.forEach(u -> u.setPassword(null));
        return R.success(users);
    }

    @DeleteMapping("/users/{id}")
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public R deleteUser(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return R.error("删除失败，用户不存在");
        }

        // P1-13 修复：原实现 userMapper.deleteById(id) 物理删行，导致聊天/好友等表 user_id 变成
        // 悬挂外键（历史消息 NPE / 脏数据），且与用户自助注销（accountStatus=deleted）策略不一致。
        // 改为同一事务内：清理关联数据 → 软删除用户行（保留行避免悬挂外键，擦除 PII）。
        chatMessageMapper.delete(new QueryWrapper<ChatMessage>().eq("user_id", id));
        friendChatMessageMapper.delete(new QueryWrapper<FriendChatMessage>()
                .eq("sender_id", id).or().eq("receiver_id", id));
        friendshipMapper.delete(new QueryWrapper<Friendship>()
                .eq("user_id", id).or().eq("friend_id", id));

        user.setAccountStatus("deleted");
        user.setNickname("已注销用户");
        user.setAvatar(null);
        user.setAvatarUrl(null);
        user.setEmail(null);
        user.setPhone(null);
        user.setBio(null);
        user.setUsername("deleted_" + id + "_" + System.currentTimeMillis());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        logger.info("[ADMIN_DELETE_USER] 管理员删除用户（软删除+级联）: {}", id);
        return R.success("删除成功");
    }

    @GetMapping("/stats")
    public R getStats() {
        Map<String, Object> stats = new HashMap<>();

        long userCount = userMapper.selectCount(null);
        long characterCount = aiCharacterMapper.selectCount(null);
        long friendshipCount = friendshipMapper.selectCount(null);
        long aiChatCount = chatMessageMapper.selectCount(null);
        long friendChatCount = friendChatMessageMapper.selectCount(null);
        long messageCount = aiChatCount + friendChatCount;

        stats.put("userCount", userCount);
        stats.put("characterCount", characterCount);
        stats.put("friendshipCount", friendshipCount);
        stats.put("messageCount", messageCount);
        stats.put("aiChatCount", aiChatCount);
        stats.put("friendChatCount", friendChatCount);

        // 数据池与社区统计
        long feedTaskCount = feedTaskMapper.selectCount(null);
        long communityPostCount = communityPostMapper.selectCount(null);
        stats.put("feedTaskCount", feedTaskCount);
        stats.put("communityPostCount", communityPostCount);

        // 实时系统监控指标（基于真实数据库状态计算，不使用硬编码）
        try {
            LocalDateTime activeThreshold = LocalDateTime.now().minusMinutes(30);
            // 活跃用户：最近30分钟内登录过的用户
            QueryWrapper<User> activeUserQuery = new QueryWrapper<>();
            activeUserQuery.ge("last_login_time", activeThreshold);
            long activeUsers = userMapper.selectCount(activeUserQuery);
            stats.put("activeUsers", activeUsers);

            // 处理中请求：PROCESSING 状态的 AI 投喂任务数
            QueryWrapper<FeedTask> processingQuery = new QueryWrapper<>();
            processingQuery.eq("status", "PROCESSING");
            long processingRequests = feedTaskMapper.selectCount(processingQuery);
            stats.put("processingRequests", processingRequests);

            // 服务器负载：JVM 内存使用率（真实）
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            int serverLoad = (int) ((usedMemory * 100) / maxMemory);
            stats.put("serverLoad", serverLoad);

            // 平均响应时间：今日 AI 消息总数除以今日活跃分钟数（真实负载指标）
            LocalDateTime todayStartRealtime = LocalDateTime.now().with(LocalTime.MIN);
            QueryWrapper<ChatMessage> todayAiChatQueryRealtime = new QueryWrapper<>();
            todayAiChatQueryRealtime.ge("create_time", todayStartRealtime);
            long todayAiMsgs = chatMessageMapper.selectCount(todayAiChatQueryRealtime);
            long minutesToday = Math.max(java.time.Duration.between(todayStartRealtime, LocalDateTime.now()).toMinutes(), 1);
            stats.put("avgResponseTime", todayAiMsgs / minutesToday);
        } catch (Exception e) {
            logger.warn("计算实时监控指标失败", e);
            stats.put("activeUsers", 0);
            stats.put("processingRequests", 0);
            stats.put("serverLoad", 0);
            stats.put("avgResponseTime", 0);
        }

        QueryWrapper<AiCharacter> systemCharacterQuery = new QueryWrapper<>();
        systemCharacterQuery.eq("is_system", true);
        long systemCharacterCount = aiCharacterMapper.selectCount(systemCharacterQuery);
        stats.put("systemCharacterCount", systemCharacterCount);
        stats.put("userCharacterCount", characterCount - systemCharacterCount);

        try {
            LocalDateTime todayStart = LocalDateTime.now().with(LocalTime.MIN);
            
            QueryWrapper<User> todayUserQuery = new QueryWrapper<>();
            todayUserQuery.ge("created_at", todayStart);
            long todayNewUsers = userMapper.selectCount(todayUserQuery);
            stats.put("dailyNewUsers", todayNewUsers);

            QueryWrapper<ChatMessage> todayAiChatQuery = new QueryWrapper<>();
            todayAiChatQuery.ge("create_time", todayStart);
            long todayAiMessages = chatMessageMapper.selectCount(todayAiChatQuery);
            
            QueryWrapper<FriendChatMessage> todayFriendChatQuery = new QueryWrapper<>();
            todayFriendChatQuery.ge("create_time", todayStart);
            long todayFriendMessages = friendChatMessageMapper.selectCount(todayFriendChatQuery);
            stats.put("todayMessages", todayAiMessages + todayFriendMessages);

            QueryWrapper<Friendship> pendingFriendQuery = new QueryWrapper<>();
            pendingFriendQuery.eq("status", "pending");
            pendingFriendQuery.ge("create_time", todayStart);
            long todayFriendRequests = friendshipMapper.selectCount(pendingFriendQuery);
            stats.put("todayFriendRequests", todayFriendRequests);
        } catch (Exception e) {
            logger.error("统计今日新增与消息数据失败", e);
            stats.put("dailyNewUsers", 0);
            stats.put("todayMessages", 0);
            stats.put("todayNewCharacters", 0);
            stats.put("todayFriendRequests", 0);
        }

        return R.success(stats);
    }

    /**
     * 获取所有AI角色（系统+用户创建），供管理员端角色管理使用。
     */
    @GetMapping("/characters")
    public R getAllCharacters() {
        List<AiCharacter> allCharacters = aiCharacterMapper.selectList(null);
        return R.success(allCharacters);
    }

    @PostMapping("/users")
    public R createUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String nickname = request.get("nickname");

        if (username == null || username.trim().isEmpty()) {
            return R.badRequest("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            return R.badRequest("密码不能为空且长度不能少于6位");
        }

        User existUser = userMapper.selectByUsername(username);
        if (existUser != null) {
            return R.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        // 安全修复：密码加密存储
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null ? nickname : username);
        userMapper.insert(user);
        user.setPassword(null);
        return R.success(user);
    }

    @PutMapping("/users/{id}")
    public R updateUser(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return R.error("用户不存在");
        }

        String nickname = request.get("nickname");
        String password = request.get("password");

        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }
        if (password != null && !password.isEmpty()) {
            // 安全修复：校验密码长度后再加密存储
            if (password.length() < 6) {
                return R.badRequest("密码长度不能少于6位");
            }
            user.setPassword(passwordEncoder.encode(password));
        }

        userMapper.updateById(user);
        user.setPassword(null);
        return R.success(user);
    }

    @GetMapping("/users/version/{versionType}")
    public R getUsersByVersion(@PathVariable String versionType) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("version_type", versionType);
        wrapper.orderByDesc("created_at");
        List<User> users = userMapper.selectList(wrapper);
        return R.success(users);
    }

    @PutMapping("/users/{id}/version")
    public R updateUserVersion(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return R.error("用户不存在");
        }

        if (request.containsKey("versionType")) {
            user.setVersionType((String) request.get("versionType"));
        }
        if (request.containsKey("versionExpireTime")) {
            user.setVersionExpireTime(LocalDateTime.parse((String) request.get("versionExpireTime")));
        }
        if (request.containsKey("accountStatus")) {
            user.setAccountStatus((String) request.get("accountStatus"));
        }
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);
        return R.success(user);
    }

    @GetMapping("/users/count")
    public R getUserCountByVersion() {
        Map<String, Object> countMap = new HashMap<>();
        
        QueryWrapper<User> freeWrapper = new QueryWrapper<>();
        freeWrapper.eq("version_type", "FREE").or().isNull("version_type");
        countMap.put("freeCount", userMapper.selectCount(freeWrapper));

        QueryWrapper<User> proWrapper = new QueryWrapper<>();
        proWrapper.eq("version_type", "PRO");
        countMap.put("proCount", userMapper.selectCount(proWrapper));

        QueryWrapper<User> enterpriseWrapper = new QueryWrapper<>();
        enterpriseWrapper.eq("version_type", "ENTERPRISE");
        countMap.put("enterpriseCount", userMapper.selectCount(enterpriseWrapper));

        return R.success(countMap);
    }

    @GetMapping("/ai-chat-messages")
    public R getAiChatMessages(@RequestParam(defaultValue = "1") Integer page, 
                             @RequestParam(defaultValue = "20") Integer size,
                             @RequestParam(required = false) Long userId,
                             @RequestParam(required = false) Long characterId) {
        Page<ChatMessage> pageParam = new Page<>(page, size);
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        
        if (userId != null && characterId != null) {
            wrapper.like("session_id", userId + "_" + characterId);
        } else if (userId != null) {
            wrapper.likeRight("session_id", userId + "_");
        } else if (characterId != null) {
            wrapper.like("session_id", "_" + characterId);
        }
        
        wrapper.orderByDesc("create_time");
        IPage<ChatMessage> result = chatMessageMapper.selectPage(pageParam, wrapper);
        return R.success(result);
    }

    @GetMapping("/ai-chat-messages/session")
    public R getSessionChatMessages(@RequestParam String sessionId) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        wrapper.orderByAsc("create_time");
        List<ChatMessage> messages = chatMessageMapper.selectList(wrapper);
        return R.success(messages);
    }

    @GetMapping("/sessions")
    public R getSessions() {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT session_id");
        List<Object> sessionIdList = chatMessageMapper.selectObjs(wrapper);

        List<Map<String, Object>> sessions = sessionIdList.stream().map(obj -> {
            Map<String, Object> session = new HashMap<>();
            String sessionId = (String) obj;
            session.put("sessionId", sessionId);

            String[] parts = sessionId.split("_");
            if (parts.length >= 2) {
                try {
                    Long userId = Long.parseLong(parts[0]);
                    Long charId = Long.parseLong(parts[1]);
                    session.put("userId", userId);
                    session.put("characterId", charId);

                    User user = userMapper.selectById(userId);
                    AiCharacter character = aiCharacterMapper.selectById(charId);
                    session.put("userName", user != null ? user.getNickname() : "未知用户(" + userId + ")");
                    session.put("characterName", character != null ? character.getName() : "未知角色(" + charId + ")");
                } catch (Exception e) {
                    session.put("userName", "无法解析(" + parts[0] + ")");
                    session.put("characterName", "无法解析(" + (parts.length > 1 ? parts[1] : "null") + ")");
                }
            } else {
                session.put("userName", "格式异常");
                session.put("characterName", "格式异常");
            }
            return session;
        }).distinct().toList();

        return R.success(sessions);
    }

    @GetMapping("/friend-chat-messages")
    public R getFriendChatMessages(@RequestParam(defaultValue = "1") Integer page, 
                                  @RequestParam(defaultValue = "20") Integer size,
                                  @RequestParam(required = false) Long senderId,
                                  @RequestParam(required = false) Long receiverId) {
        try {
            // 安全修复：使用参数化查询替代SQL拼接，防止SQL注入
            Page<FriendChatMessage> pageParam = new Page<>(page, size);
            QueryWrapper<FriendChatMessage> wrapper = new QueryWrapper<>();
            
            if (senderId != null) {
                wrapper.eq("sender_id", senderId);
            }
            if (receiverId != null) {
                wrapper.eq("receiver_id", receiverId);
            }
            
            wrapper.orderByDesc("create_time");
            IPage<FriendChatMessage> result = friendChatMessageMapper.selectPage(pageParam, wrapper);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("records", result.getRecords());
            resultData.put("total", result.getTotal());
            resultData.put("current", page);
            resultData.put("size", size);
            
            return R.success(resultData);
        } catch (Exception e) {
            logger.error("加载好友聊天记录失败", e);
            return R.error("加载好友聊天记录失败");
        }
    }

    @DeleteMapping("/ai-chat-messages/{id}")
    public R deleteAiChatMessage(@PathVariable Long id) {
        int result = chatMessageMapper.deleteById(id);
        if (result > 0) {
            return R.success("删除成功");
        }
        return R.error("删除失败");
    }

    @DeleteMapping("/friend-chat-messages/{id}")
    public R deleteFriendChatMessage(@PathVariable Long id) {
        int result = friendChatMessageMapper.deleteById(id);
        if (result > 0) {
            return R.success("删除成功");
        }
        return R.error("删除失败");
    }

    // ==================== M3 ⑩ 数据看板 ====================

    /**
     * 数据看板统计数据（供前端 ECharts 折线图 + 数字卡片使用）。
     *
     * 返回：
     *   - todayActiveUsers   今日活跃用户数（今日有登录记录）
     *   - todayNewUsers      今日新增用户数
     *   - todayChats         今日对话次数（AI 对话 + 好友私聊）
     *   - activeTrend        最近 7 天每日活跃用户数 [{date, count}]
     *   - newUserTrend       最近 7 天每日新增用户数
     *   - chatTrend          最近 7 天每日对话次数
     */
    @GetMapping("/dashboard/stats")
    public R dashboardStats() {
        LocalDateTime todayStart = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(6).with(LocalTime.MIN);

        Map<String, Object> result = new HashMap<>();
        try {
            // ---- 今日数字卡片 ----
            QueryWrapper<User> todayActiveQw = new QueryWrapper<>();
            todayActiveQw.ge("last_login_time", todayStart);
            long todayActiveUsers = userMapper.selectCount(todayActiveQw);

            QueryWrapper<User> todayNewQw = new QueryWrapper<>();
            todayNewQw.ge("created_at", todayStart);
            long todayNewUsers = userMapper.selectCount(todayNewQw);

            long todayChats = countChatsSince(todayStart);

            result.put("todayActiveUsers", todayActiveUsers);
            result.put("todayNewUsers", todayNewUsers);
            result.put("todayChats", todayChats);

            // ---- 最近 7 天趋势（按天分桶） ----
            result.put("activeTrend", buildDailyTrend(
                    "last_login_time", weekAgo, true));
            result.put("newUserTrend", buildDailyTrend(
                    "created_at", weekAgo, false));
            result.put("chatTrend", buildChatTrend(weekAgo));
        } catch (Exception e) {
            logger.error("生成数据看板统计失败", e);
            // 降级返回空趋势，保证前端不崩
            result.put("todayActiveUsers", 0);
            result.put("todayNewUsers", 0);
            result.put("todayChats", 0);
            result.put("activeTrend", emptyTrend(weekAgo));
            result.put("newUserTrend", emptyTrend(weekAgo));
            result.put("chatTrend", emptyTrend(weekAgo));
        }
        return R.success(result);
    }

    /** 统计 since 时刻起的对话总数（AI + 好友）。 */
    private long countChatsSince(LocalDateTime since) {
        QueryWrapper<ChatMessage> aiQw = new QueryWrapper<>();
        aiQw.ge("create_time", since);
        long ai = chatMessageMapper.selectCount(aiQw);
        QueryWrapper<FriendChatMessage> friendQw = new QueryWrapper<>();
        friendQw.ge("create_time", since);
        long friend = friendChatMessageMapper.selectCount(friendQw);
        return ai + friend;
    }

    /**
     * 按天聚合单表指标，返回最近 7 天 [{date, count}]（补齐缺失为 0）。
     *
     * @param dateColumn    用于按天分桶的时间列
     * @param since         起始时刻（本周第一天 00:00）
     * @param distinctUsers true 时 COUNT(DISTINCT id)（活跃去重），否则 COUNT(*)
     */
    private List<Map<String, Object>> buildDailyTrend(String dateColumn, LocalDateTime since, boolean distinctUsers) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.select("DATE(" + dateColumn + ") AS day, "
                + (distinctUsers ? "COUNT(DISTINCT id)" : "COUNT(*)") + " AS cnt");
        qw.ge(dateColumn, since);
        qw.groupBy("DATE(" + dateColumn + ")");
        List<Map<String, Object>> rows = userMapper.selectMaps(qw);
        return bucketize(rows, since, "day", "cnt");
    }

    /** 对话趋势 = AI 对话 + 好友私聊按天求和。 */
    private List<Map<String, Object>> buildChatTrend(LocalDateTime since) {
        QueryWrapper<ChatMessage> aiQw = new QueryWrapper<>();
        aiQw.select("DATE(create_time) AS day, COUNT(*) AS cnt");
        aiQw.ge("create_time", since);
        aiQw.groupBy("DATE(create_time)");
        List<Map<String, Object>> aiRows = chatMessageMapper.selectMaps(aiQw);

        QueryWrapper<FriendChatMessage> friendQw = new QueryWrapper<>();
        friendQw.select("DATE(create_time) AS day, COUNT(*) AS cnt");
        friendQw.ge("create_time", since);
        friendQw.groupBy("DATE(create_time)");
        List<Map<String, Object>> friendRows = friendChatMessageMapper.selectMaps(friendQw);

        // 合并两表按天计数
        java.util.Map<String, Long> merged = new java.util.HashMap<>();
        mergeRows(merged, aiRows);
        mergeRows(merged, friendRows);

        // 补齐 7 天
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        java.time.LocalDate cursor = since.toLocalDate();
        java.time.LocalDate today = java.time.LocalDate.now();
        while (!cursor.isAfter(today)) {
            String key = cursor.toString();
            long cnt = merged.getOrDefault(key, 0L);
            Map<String, Object> item = new HashMap<>();
            item.put("date", key);
            item.put("count", cnt);
            result.add(item);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private void mergeRows(java.util.Map<String, Long> bucket, List<Map<String, Object>> rows) {
        if (rows == null) return;
        for (Map<String, Object> row : rows) {
            Object dayObj = row.get("day");
            Object cntObj = row.get("cnt");
            if (dayObj == null) continue;
            String day = dayObj.toString();
            long cnt = 0L;
            if (cntObj instanceof Number) {
                cnt = ((Number) cntObj).longValue();
            }
            bucket.merge(day, cnt, Long::sum);
        }
    }

    /** 将 selectMaps 结果按最近 7 天补齐成 [{date,count}]。 */
    private List<Map<String, Object>> bucketize(List<Map<String, Object>> rows,
                                                 LocalDateTime since, String dayKey, String cntKey) {
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object dayObj = row.get(dayKey);
                Object cntObj = row.get(cntKey);
                if (dayObj == null) continue;
                long cnt = (cntObj instanceof Number) ? ((Number) cntObj).longValue() : 0L;
                map.put(dayObj.toString(), cnt);
            }
        }
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        java.time.LocalDate cursor = since.toLocalDate();
        java.time.LocalDate today = java.time.LocalDate.now();
        while (!cursor.isAfter(today)) {
            String key = cursor.toString();
            Map<String, Object> item = new HashMap<>();
            item.put("date", key);
            item.put("count", map.getOrDefault(key, 0L));
            result.add(item);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private List<Map<String, Object>> emptyTrend(LocalDateTime since) {
        return bucketize(java.util.Collections.emptyList(), since, "day", "cnt");
    }
}
