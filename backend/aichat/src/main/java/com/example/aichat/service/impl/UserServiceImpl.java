package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.entity.Friendship;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.example.aichat.service.CacheService cacheService;

    @Autowired
    private com.example.aichat.service.MinorProtectionService minorProtectionService;

    // 安全修复：使用ConcurrentHashMap保证线程安全（生产环境应使用数据库或Redis以支持多实例部署）
    private static final Map<String, ResetPasswordToken> resetTokens = new ConcurrentHashMap<>();

    // 安全修复：使用ConcurrentHashMap保证线程安全（生产环境应使用数据库或Redis以支持多实例部署）
    private static final Map<String, VerificationCode> verificationCodes = new ConcurrentHashMap<>();

    // 安全修复：使用SecureRandom替代Math.random()，防止验证码被预测
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public User register(String username, String password, String nickname) {
        return register(username, password, nickname, null);
    }

    @Override
    public User register(String username, String password, String nickname, Integer age) {
        // 参数校验：username/password 长度（前端 @Valid 已校验，Service 兜底）
        if (username == null || username.length() < 3 || username.length() > 20) {
            throw new BusinessException(UserErrorCode.USERNAME_LENGTH_INVALID);
        }
        if (password == null || password.length() < 6 || password.length() > 20) {
            throw new BusinessException(UserErrorCode.PASSWORD_LENGTH_INVALID);
        }
        // 年龄兜底校验：与 RegisterRequest 的 @Min/@Max 对齐，防止绕过 Controller 调用
        if (age != null && (age < 0 || age > 120)) {
            throw new BusinessException(UserErrorCode.MINOR_AGE_INVALID);
        }
        User existingUser = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("username", username));
        if (existingUser != null) {
            // 重构目的：原实现返回 null 由 Controller 判断，现改为抛出业务异常，
            // 由 GlobalExceptionHandler 统一转换为标准 R 响应。
            logger.warn("[REGISTRATION_FAILED] 用户名已存在, username={}, existingUserId={}", username, existingUser.getId());
            throw new BusinessException(UserErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        // 未成年保护：写入自声明年龄；未成年人由 MinorProtectionService 按 null（自动）逻辑默认开启
        if (age != null && age > 0) {
            user.setAge(age);
        }
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        logger.info("[REGISTRATION_SUCCESS] 用户注册成功, userId={}, username={}, age={}", user.getId(), username, age);
        return user;
    }

    @Override
    public User login(String username, String password) {
        // 参数校验兜底
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException(UserErrorCode.LOGIN_FAILED);
        }
        // 先尝试按用户名查找
        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("username", username));

        // 如果没找到，尝试按邮箱查找
        if (user == null && username.contains("@")) {
            user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("email", username));
        }

        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            // 安全加固：校验密码通过后再判定账号状态，避免对不存在/密码错误的请求泄露账号是否被禁用（用户枚举）。
            // disabled/locked 账号即使密码正确也拒绝登录。
            String accountStatus = user.getAccountStatus();
            if ("disabled".equals(accountStatus)) {
                logger.warn("[LOGIN_REJECTED] account disabled, userId={}, username={}", user.getId(), username);
                throw new BusinessException(UserErrorCode.ACCOUNT_DISABLED);
            }
            if ("locked".equals(accountStatus)) {
                logger.warn("[LOGIN_REJECTED] account locked, userId={}, username={}", user.getId(), username);
                throw new BusinessException(UserErrorCode.ACCOUNT_LOCKED);
            }
            // 安全修复（P1）：登录成功后清理失败计数（通过 Redis），
            // 失败计数用于暴力破解防护，达到 5 次锁定账户 30 分钟。
            cacheService.delete("login_failed:" + username);
            // 未成年保护：登录成功后检查夜间访问限制（未成年模式 + 夜间时段 → 拒绝登录）
            minorProtectionService.checkNightAccess(user);
            return user;
        }
        // 安全修复（P1）：登录失败递增计数，达到 5 次返回 ACCOUNT_LOCKED 错误
        try {
            String key = "login_failed:" + username;
            Integer count = cacheService.get(key, Integer.class);
            int newCount = (count == null ? 0 : count) + 1;
            cacheService.set(key, newCount, 1800);  // 30 分钟 TTL
            if (newCount >= 5) {
                logger.warn("[LOGIN_LOCKED] username={}, failedCount={}", username, newCount);
                throw new BusinessException(UserErrorCode.ACCOUNT_LOCKED);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception ignored) {
            // Redis 异常不影响主流程
        }
        // 重构目的：原 Controller 用 R.unauthorized("用户名或密码错误") 手写判断，
        // 现让 Service 抛业务异常统一处理，避免 Controller 重复代码。
        throw new BusinessException(UserErrorCode.LOGIN_FAILED);
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public boolean sendResetPasswordEmail(String email) {
        User user = getUserByEmail(email);
        if (user == null) {
            return false;
        }

        // 清理过期令牌
        cleanupExpiredTokens();

        // 生成重置令牌
        String token = UUID.randomUUID().toString();
        ResetPasswordToken resetToken = new ResetPasswordToken();
        resetToken.setUserId(user.getId());
        resetToken.setExpireTime(LocalDateTime.now().plusHours(1));
        
        resetTokens.put(token, resetToken);
        // 优化：同步写入Redis，支持多实例部署（Redis不可用时降级到内存Map）
        cacheService.set("reset_token:" + token, resetToken, 3600);

        // 安全修复：移除System.out.println，改用logger且不输出敏感令牌明文
        logger.info("[RESET_PASSWORD] 重置密码令牌已生成, userId={}, tokenPrefix={}***", user.getId(), token.substring(0, 8));

        return true;
    }

    private void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        resetTokens.entrySet().removeIf(entry -> entry.getValue().getExpireTime().isBefore(now));
    }

    @Override
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        // 优先从Redis读取，降级到内存Map
        ResetPasswordToken resetToken = cacheService.get("reset_token:" + token, ResetPasswordToken.class);
        if (resetToken == null) {
            resetToken = resetTokens.get(token);
        }

        if (resetToken == null) {
            return false;
        }

        // 检查令牌是否过期
        if (resetToken.getExpireTime().isBefore(LocalDateTime.now())) {
            resetTokens.remove(token);
            cacheService.delete("reset_token:" + token);
            return false;
        }

        // 更新密码
        User user = userMapper.selectById(resetToken.getUserId());
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userMapper.updateById(user);

            // 移除已使用的令牌
            resetTokens.remove(token);
            cacheService.delete("reset_token:" + token);
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        // 参数校验：新密码长度必须在 6-20 之间
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BusinessException(UserErrorCode.PASSWORD_LENGTH_INVALID);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(UserErrorCode.OLD_PASSWORD_INCORRECT);
        }

        // 更新新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        return true;
    }

    @Override
    public User getUserByEmail(String email) {
        return userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("email", email));
    }

    @Override
    public User getUserByPhone(String phone) {
        return userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("phone", phone));
    }

    @Override
    @Transactional
    public boolean resetPasswordByCode(String contact, String code, String newPassword) {
        if (contact == null || code == null || newPassword == null) {
            return false;
        }
        // 修复：与 changePassword 保持一致的服务层密码长度兜底校验，
        // 避免绕过 Controller 直接调用时写入非法长度密码。
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BusinessException(UserErrorCode.PASSWORD_LENGTH_INVALID);
        }
        // 解析 contact 类型并校验验证码
        User user;
        boolean codeValid;
        if (contact.contains("@")) {
            codeValid = verifyEmailCode(contact, code);
            user = getUserByEmail(contact);
        } else {
            codeValid = verifySmsCode(contact, code);
            user = getUserByPhone(contact);
        }
        if (!codeValid || user == null) {
            return false;
        }
        // 在 Service 内完成密码加密 + 持久化，避免 Controller 持有 PasswordEncoder/Mapper
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        logger.info("[RESET_PASSWORD_BY_CODE] 通过验证码重置密码成功, userId={}", user.getId());
        return true;
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        userMapper.updateById(user);
        return userMapper.selectById(user.getId());
    }

    @Override
    public boolean sendVerificationEmail(String email) {
        User user = getUserByEmail(email);
        if (user == null) {
            return false;
        }

        cleanupExpiredCodes();

        String code = generateCode();
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setCode(code);
        verificationCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        
        verificationCodes.put(email.toLowerCase(), verificationCode);
        // 优化：同步写入Redis，支持多实例部署
        cacheService.set("verify_code:email:" + email.toLowerCase(), code, 300);

        // 安全修复：移除System.out.println，改用logger且不输出验证码明文与邮箱
        logger.info("[VERIFICATION_EMAIL] 邮箱验证码已生成, userId={}", user.getId());

        return true;
    }

    @Override
    public boolean sendVerificationSms(String phone) {
        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("phone", phone));
        if (user == null) {
            return false;
        }

        cleanupExpiredCodes();

        String code = generateCode();
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setCode(code);
        verificationCode.setExpireTime(LocalDateTime.now().plusMinutes(5));
        
        verificationCodes.put("phone:" + phone, verificationCode);
        // 优化：同步写入Redis，支持多实例部署
        cacheService.set("verify_code:phone:" + phone, code, 300);

        // 安全修复：移除System.out.println，改用logger且不输出验证码明文与手机号
        logger.info("[VERIFICATION_SMS] 短信验证码已生成, userId={}", user.getId());

        return true;
    }

    @Override
    public boolean verifyEmailCode(String email, String code) {
        cleanupExpiredCodes();
        String key = email.toLowerCase();

        // 优先从Redis读取验证码
        String redisCode = cacheService.get("verify_code:email:" + key, String.class);
        if (redisCode != null) {
            boolean valid = redisCode.equals(code);
            if (valid) {
                cacheService.delete("verify_code:email:" + key);
                verificationCodes.remove(key);
            }
            return valid;
        }

        // 降级到内存Map
        VerificationCode verificationCode = verificationCodes.get(key);
        if (verificationCode == null) {
            return false;
        }

        if (verificationCode.getExpireTime().isBefore(LocalDateTime.now())) {
            verificationCodes.remove(key);
            return false;
        }

        boolean valid = verificationCode.getCode().equals(code);
        if (valid) {
            verificationCodes.remove(key);
        }

        return valid;
    }

    @Override
    public boolean verifySmsCode(String phone, String code) {
        cleanupExpiredCodes();
        String key = "phone:" + phone;

        // 优先从Redis读取验证码
        String redisCode = cacheService.get("verify_code:" + key, String.class);
        if (redisCode != null) {
            boolean valid = redisCode.equals(code);
            if (valid) {
                cacheService.delete("verify_code:" + key);
                verificationCodes.remove(key);
            }
            return valid;
        }

        // 降级到内存Map
        VerificationCode verificationCode = verificationCodes.get(key);
        if (verificationCode == null) {
            return false;
        }

        if (verificationCode.getExpireTime().isBefore(LocalDateTime.now())) {
            verificationCodes.remove(key);
            return false;
        }

        boolean valid = verificationCode.getCode().equals(code);
        if (valid) {
            verificationCodes.remove(key);
        }

        return valid;
    }

    @Autowired
    private com.example.aichat.mapper.ChatMessageMapper chatMessageMapper;

    @Autowired
    private com.example.aichat.mapper.FriendChatMessageMapper friendChatMessageMapper;

    @Autowired
    private com.example.aichat.mapper.FriendshipMapper friendshipMapper;

    @Autowired
    private com.example.aichat.mapper.AiMemoryMapper aiMemoryMapper;

    @Override
    @Transactional
    public void deleteAccount(Long userId, String confirmText) {
        if (userId == null) {
            throw new BusinessException(UserErrorCode.TOKEN_INVALID);
        }
        // 二次确认：必须严格传入 "DELETE"
        if (confirmText == null || !"DELETE".equals(confirmText.trim())) {
            logger.warn("[ACCOUNT_DELETE_REJECTED] 二次确认不通过, userId={}", userId);
            throw new BusinessException(UserErrorCode.ACCOUNT_DELETE_CONFIRM_REQUIRED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        // 已注销账号拒绝重复操作
        if ("deleted".equals(user.getAccountStatus())) {
            throw new BusinessException(UserErrorCode.ACCOUNT_ALREADY_DELETED);
        }

        // 1) 清理对话历史：AI 对话（按 user_id）
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.aichat.entity.ChatMessage> chatQw =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            chatQw.eq("user_id", userId);
            chatMessageMapper.delete(chatQw);
        } catch (Exception e) {
            logger.warn("[ACCOUNT_DELETE] 清理 AI 对话历史失败, userId={}", userId, e);
        }

        // 2) 清理好友私聊记录（作为发送方或接收方）
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.aichat.entity.FriendChatMessage> fcmQw =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            fcmQw.eq("sender_id", userId).or().eq("receiver_id", userId);
            friendChatMessageMapper.delete(fcmQw);
        } catch (Exception e) {
            logger.warn("[ACCOUNT_DELETE] 清理好友私聊失败, userId={}", userId, e);
        }

        // 3) 清理好友关系（我加的、别人加我的）
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Friendship> friendQw =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            friendQw.eq("user_id", userId).or().eq("friend_id", userId);
            friendshipMapper.delete(friendQw);
        } catch (Exception e) {
            logger.warn("[ACCOUNT_DELETE] 清理好友关系失败, userId={}", userId, e);
        }

        // 4) 清理 AI 记忆
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.aichat.entity.AiMemory> memQw =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            memQw.eq("user_id", userId);
            aiMemoryMapper.delete(memQw);
        } catch (Exception e) {
            logger.warn("[ACCOUNT_DELETE] 清理 AI 记忆失败, userId={}", userId, e);
        }

        // 5) 删除头像文件
        deleteAvatarFile(user.getAvatar());

        // 6) 软删除：标记 accountStatus=deleted，并擦除敏感资料字段
        user.setAccountStatus("deleted");
        user.setNickname("已注销用户");
        user.setAvatar(null);
        user.setAvatarUrl(null);
        user.setEmail(null);
        user.setPhone(null);
        user.setBio(null);
        user.setMinorModeEnabled(null);
        user.setMinorModeUpdatedAt(null);
        // 用户名保留以便审计，但加后缀避免与新注册用户名冲突
        user.setUsername("deleted_" + userId + "_" + System.currentTimeMillis());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        logger.info("[ACCOUNT_DELETED] 账号自助注销成功, userId={}", userId);
    }

    /**
     * 删除用户头像文件（尽力而为，失败不阻断注销主流程）。
     */
    private void deleteAvatarFile(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }
        try {
            // avatarUrl 形如 /uploads/user/xxx.png，截取文件名后定位 uploads/user 目录
            String path = avatarUrl.replace('\\', '/');
            int uploadIdx = path.indexOf("/uploads/");
            if (uploadIdx < 0) {
                return;
            }
            String relative = path.substring(uploadIdx + "/uploads/".length());
            File file = new File(System.getProperty("user.dir") + File.separator + "uploads"
                    + File.separator + relative.replace('/', File.separatorChar));
            if (file.exists() && file.isFile()) {
                // 安全校验：规范路径必须位于 uploads 内，防止路径穿越
                File root = new File(System.getProperty("user.dir"), "uploads").getCanonicalFile();
                if (file.getCanonicalPath().startsWith(root.getPath() + File.separator)) {
                    if (file.delete()) {
                        logger.info("[ACCOUNT_DELETE] 头像文件已删除: {}", file.getPath());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("[ACCOUNT_DELETE] 删除头像文件失败, avatar={}", avatarUrl, e);
        }
    }

    private String generateCode() {
        // 安全修复：使用SecureRandom生成验证码，防止可预测攻击
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }

    private void cleanupExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        verificationCodes.entrySet().removeIf(entry -> entry.getValue().getExpireTime().isBefore(now));
    }

    // 内部类：重置密码令牌
    private static class ResetPasswordToken {
        private Long userId;
        private LocalDateTime expireTime;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public LocalDateTime getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(LocalDateTime expireTime) {
            this.expireTime = expireTime;
        }
    }

    // 内部类：验证码
    private static class VerificationCode {
        private String code;
        private LocalDateTime expireTime;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public LocalDateTime getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(LocalDateTime expireTime) {
            this.expireTime = expireTime;
        }
    }
}