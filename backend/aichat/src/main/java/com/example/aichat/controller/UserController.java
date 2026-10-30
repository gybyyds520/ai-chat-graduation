package com.example.aichat.controller;

import com.example.aichat.common.audit.Audited;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.dto.user.LoginRequest;
import com.example.aichat.dto.user.RegisterRequest;
import com.example.aichat.entity.User;
import com.example.aichat.service.UserOnlineStatusService;
import com.example.aichat.service.UserService;
import com.example.aichat.utils.TokenUtils;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户相关接口。
 *
 * 重构要点（Phase 1 缺陷修复）：
 * 1. 引入 DTO + @Valid + Bean Validation，移除手写 if/else 校验；
 * 2. 移除对 UserMapper 的直接注入，所有数据访问走 UserService（解除分层违规）；
 * 3. 移除对 PasswordEncoder 的注入，密码加密下沉到 Service（单一职责）；
 * 4. register/login 改为让 Service 抛 BusinessException，由 GlobalExceptionHandler 统一响应。
 */
@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserOnlineStatusService userOnlineStatusService;

    @Autowired
    private com.example.aichat.service.MinorProtectionService minorProtectionService;

    @Autowired
    private com.example.aichat.service.QuotaService quotaService;

    @PostMapping("/register")
    @Audited(action = "REGISTER", resourceType = "USER")
    @RateLimiter(name = "authApi", fallbackMethod = "registerFallback")
    public R<User> register(@Valid @RequestBody RegisterRequest request) {
        // 参数校验由 @Valid 自动完成，校验失败由 GlobalExceptionHandler 处理 MethodArgumentNotValidException
        User user = userService.register(
                request.getUsername().trim(),
                request.getPassword(),
                request.getNickname(),
                request.getAge()
        );
        user.setPassword(null);
        return R.success(user);
    }

    /**
     * 注册接口限流降级方法 - 当 1 分钟内请求超过 10 次时调用。
     * 签名须与原方法一致（参数列表相同），并追加一个 Throwable 参数。
     *
     * Bug 修复（E2E 发现）：原实现捕获所有 Throwable 返回 429，
     * 导致 BusinessException（如 USERNAME_EXISTS）被吞掉，客户端收到 429 而非真实错误码。
     * 修复：BusinessException 向上抛出由 GlobalExceptionHandler 统一处理，
     * fallback 仅处理真正的限流异常（RequestNotPermitted）。
     */
    @SuppressWarnings("unused")
    private R<User> registerFallback(RegisterRequest request, Throwable t) {
        // 仅限流异常（RequestNotPermitted）返回 429；
        // 其余异常（业务/DB/运行时）原样上抛，由 GlobalExceptionHandler 统一处理，
        // 避免 DB 错误等真实故障被误报为"请求过于频繁"。
        if (!(t instanceof RequestNotPermitted)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
        logger.warn("[RATE_LIMITED] action=register username={} reason={}",
                request.getUsername(), t.getMessage());
        return R.error(429, "请求过于频繁，请稍后再试");
    }

    @PostMapping("/login")
    @Audited(action = "LOGIN", resourceType = "USER")
    @RateLimiter(name = "authApi", fallbackMethod = "loginFallback")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        // 校验失败：MethodArgumentNotValidException → 400
        // 业务失败（用户名/密码错误）：Service 抛 BusinessException(LOGIN_FAILED) → 401
        // 访问日志由 @Audited 切面统一记录，无需手写 logger.info
        User user = userService.login(request.getUsername().trim(), request.getPassword());
        user.setPassword(null);
        // 生成 JWT token（与 AuthServiceImpl.loginByPhone 保持一致），
        // JwtAuthenticationFilter 据此注入 ROLE_USER，使受保护接口可通过认证。
        String token = TokenUtils.generateToken(user.getId().toString(), user.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("token", token);
        result.put("tokenType", "Bearer");
        return R.success(result);
    }

    /**
     * 登录接口限流降级方法 - 防爆破，1 分钟 10 次阈值。
     *
     * Bug 修复（E2E 发现）：原实现捕获所有 Throwable 返回 429，
     * 导致 BusinessException(LOGIN_FAILED/ACCOUNT_LOCKED) 被吞掉，
     * 客户端收到 429 而非真实的 401/403，暴力破解锁定逻辑无法生效。
     * 修复：BusinessException 向上抛出，仅处理 RequestNotPermitted 限流异常。
     */
    @SuppressWarnings("unused")
    private R<User> loginFallback(LoginRequest request, Throwable t) {
        // 仅限流异常返回 429，其余异常原样上抛（同 registerFallback 逻辑）
        if (!(t instanceof RequestNotPermitted)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
        logger.warn("[RATE_LIMITED] action=login username={} reason={}",
                request.getUsername(), t.getMessage());
        return R.error(429, "登录尝试过于频繁，请稍后再试");
    }

    /**
     * 当前登录用户的会员信息（等级 / 到期时间 / 配额配置）。
     */
    @GetMapping("/membership")
    public R<Map<String, Object>> membership() {
        Long userId = AuthContext.requireUserId();
        return R.success(quotaService.getCurrentQuota(userId));
    }

    @GetMapping("/{id}")
    public R<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user != null) {
            // P1-8：他人查询只返回公开基础字段，剥离手机号/邮箱/登录IP等 PII；本人信息走 /api/user/me
            Long current = AuthContext.requireUserId();
            if (!current.equals(id)) {
                sanitizeForPublic(user);
            }
            return R.success(user);
        }
        throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
    }

    @PostMapping("/forgot-password")
    public R<String> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return R.badRequest("邮箱不能为空");
        }

        // 发送重置密码邮件（模拟）
        boolean result = userService.sendResetPasswordEmail(email);
        if (result) {
            return R.success("重置密码链接已发送至您的邮箱");
        }
        return R.error("该邮箱未注册");
    }

    @PostMapping("/reset-password")
    public R<String> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || token.trim().isEmpty()) {
            return R.badRequest("令牌不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return R.badRequest("新密码不能为空");
        }
        if (newPassword.length() < 8 || newPassword.length() > 20) {
            return R.badRequest("密码长度必须在8-20个字符之间");
        }

        boolean result = userService.resetPassword(token, newPassword);
        if (result) {
            return R.success("密码重置成功");
        }
        return R.error("链接已失效，请重新申请");
    }

    @GetMapping("/profile")
    public R<User> getUserProfile(@RequestParam("userId") Long userId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            // P1-8：他人查询只返回公开基础字段，剥离手机号/邮箱/登录IP等 PII
            Long current = AuthContext.requireUserId();
            if (!current.equals(userId)) {
                sanitizeForPublic(user);
            }
            return R.success(user);
        }
        throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
    }

    /**
     * P1-8：对外公开资料脱敏。清空手机号、邮箱、最近登录 IP/时间、账号状态、登录安全计数等
     * 非公开字段，仅保留昵称/头像/性别/简介等社交公开信息，防止任意登录用户遍历 ID 爬取 PII。
     */
    private void sanitizeForPublic(User user) {
        user.setPhone(null);
        user.setEmail(null);
        user.setLastLoginIp(null);
        user.setLastLoginTime(null);
        user.setAccountStatus(null);
        user.setFailedLoginCount(null);
        user.setLockTime(null);
        user.setSecurityQuestion(null);
        user.setMfaEnabled(null);
        user.setPhoneVerified(null);
        user.setEmailVerified(null);
    }

    /**
     * 获取当前登录用户信息 - Phase 3 (P3-d) 示范 AuthContext + @PreAuthorize。
     *
     * 改造对比：
     *   原模式（手写 token 解析）：
     *     @GetMapping("/me")
     *     public R<User> me(@RequestHeader("Authorization") String authHeader) {
     *         String token = authHeader.substring(7);
     *         if (!TokenUtils.validateToken(token)) return R.unauthorized("未登录");
     *         String userId = TokenUtils.getUserIdFromToken(token);
     *         User user = userService.getUserById(Long.parseLong(userId));
     *         return R.success(user);
     *     }
     *
     *   新模式（声明式权限 + AuthContext）：
     *     - @PreAuthorize("isAuthenticated()") 替代手写 token 校验
     *     - AuthContext.getCurrentUserId() 替代手写 token 解析
     *     - 由 GlobalExceptionHandler 统一处理 AccessDeniedException（403）/ AuthenticationException（401）
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "GET_CURRENT_USER", resourceType = "USER")
    public R<User> getCurrentUser() {
        // 从 SecurityContext 取当前用户 ID，无需 Controller 接收 token 参数
        Long userId = AuthContext.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(UserErrorCode.TOKEN_INVALID));
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        return R.success(user);
    }

    @PutMapping("/profile")
    public R<User> updateUserProfile(@RequestBody Map<String, Object> request) {
        String userIdStr = (String) request.get("userId");
        String nickname = (String) request.get("nickname");
        String email = (String) request.get("email");
        String avatar = (String) request.get("avatar");
        String bio = (String) request.get("bio");
        Object ageObj = request.get("age");

        // 安全修复（P0）：只能更新当前登录用户自己的资料，忽略/拒绝他人 userId
        Long clientUserId = null;
        if (userIdStr != null && !userIdStr.trim().isEmpty()) {
            try {
                clientUserId = Long.parseLong(userIdStr.trim());
            } catch (NumberFormatException ignored) {
                // 忽略非法格式，以登录用户为准
            }
        }
        Long userId = AuthContext.requireSelf(clientUserId);

        User user = userService.getUserById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }

        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname.trim());
        }
        if (email != null && !email.trim().isEmpty()) {
            // 安全修复：校验邮箱格式，防止非法数据写入
            if (!email.trim().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return R.badRequest("邮箱格式不正确");
            }
            user.setEmail(email.trim());
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        if (bio != null) {
            user.setBio(bio);
        }

        // 未成年保护：更新自声明年龄时，按是否跨 18 岁门槛自动同步未成年模式开关
        if (ageObj != null) {
            Integer newAge;
            try {
                newAge = ((Number) ageObj).intValue();
            } catch (ClassCastException e) {
                return R.badRequest("年龄格式不正确");
            }
            if (newAge < 0 || newAge > 120) {
                throw new BusinessException(UserErrorCode.MINOR_AGE_INVALID);
            }
            Integer oldAge = user.getAge();
            boolean wasMinor = oldAge != null && oldAge > 0 && oldAge < 18;
            boolean nowMinor = newAge > 0 && newAge < 18;
            user.setAge(newAge > 0 ? newAge : null);
            if (!wasMinor && nowMinor) {
                // 年龄从成年改为未成年 → 自动开启未成年保护
                user.setMinorModeEnabled(true);
                user.setMinorModeUpdatedAt(java.time.LocalDateTime.now());
            } else if (wasMinor && !nowMinor) {
                // 年龄从未成年改为成年 → 自动关闭未成年保护
                user.setMinorModeEnabled(false);
                user.setMinorModeUpdatedAt(java.time.LocalDateTime.now());
            }
        }

        User updatedUser = userService.updateUser(user);
        if (updatedUser != null) {
            return R.success(updatedUser);
        }
        return R.error("更新失败");
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> getUserStats(@RequestParam("userId") Long userId) {
        // 安全修复（P0）：统计信息（含最近活跃时间）仅本人可查看
        Long current = AuthContext.requireSelf(userId);
        User user = userService.getUserById(current);
        if (user == null) {
            return R.error("用户不存在");
        }

        Map<String, Object> stats = Map.of(
            "totalChats", 128,
            "totalMessages", 3520,
            "totalCharacters", 5,
            "usageHours", 42.5,
            "lastActive", user.getUpdatedAt()
        );
        return R.success(stats);
    }

    @PutMapping("/change-password")
    public R<String> changePassword(@RequestBody Map<String, String> request) {
        String userIdStr = request.get("userId");
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        // 安全修复（P0）：只能修改当前登录用户自己的密码
        Long clientUserId = null;
        if (userIdStr != null && !userIdStr.trim().isEmpty()) {
            try {
                clientUserId = Long.parseLong(userIdStr.trim());
            } catch (NumberFormatException ignored) {
                // 忽略非法格式，以登录用户为准
            }
        }
        Long userId = AuthContext.requireSelf(clientUserId);
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return R.error("旧密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return R.error("新密码不能为空");
        }
        if (newPassword.length() < 8 || newPassword.length() > 20) {
            return R.error("密码长度必须在8-20个字符之间");
        }

        boolean result = userService.changePassword(userId, oldPassword, newPassword);
        if (result) {
            return R.success("密码修改成功");
        }
        return R.error("旧密码不正确");
    }

    @PostMapping("/upload-avatar")
    public R<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.badRequest("请选择要上传的图片");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            return R.badRequest("请上传有效的图片文件（支持JPG、PNG、GIF、WebP格式）");
        }

        try {
            File saved = com.example.aichat.util.FileUploadUtil.save(file, "user");

            Map<String, Object> result = new HashMap<>();
            result.put("id", saved.getName());
            result.put("fileName", fileName);
            result.put("filePath", com.example.aichat.util.FileUploadUtil.toUrl("user", saved));
            result.put("size", file.getSize());

            return R.success(result);
        } catch (IOException e) {
            return R.error("图片上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/send-verification-code")
    public R<String> sendVerificationCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String phone = request.get("phone");

        if ((email == null || email.trim().isEmpty()) && (phone == null || phone.trim().isEmpty())) {
            return R.error("请提供邮箱或手机号");
        }

        if (email != null && !email.trim().isEmpty()) {
            boolean result = userService.sendVerificationEmail(email.trim());
            if (result) {
                return R.success("验证码已发送至您的邮箱");
            }
            return R.error("该邮箱未注册");
        }

        if (phone != null && !phone.trim().isEmpty()) {
            boolean result = userService.sendVerificationSms(phone.trim());
            if (result) {
                return R.success("验证码已发送至您的手机");
            }
            return R.error("该手机号未注册");
        }

        return R.error("参数错误");
    }

    @PostMapping("/verify-code")
    public R<String> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String phone = request.get("phone");
        String code = request.get("code");

        if (code == null || code.trim().isEmpty()) {
            return R.error("验证码不能为空");
        }

        boolean valid = false;
        if (email != null && !email.trim().isEmpty()) {
            valid = userService.verifyEmailCode(email.trim(), code.trim());
        } else if (phone != null && !phone.trim().isEmpty()) {
            valid = userService.verifySmsCode(phone.trim(), code.trim());
        }

        if (valid) {
            return R.success("验证码验证成功");
        }
        return R.error("验证码错误或已过期");
    }

    @PostMapping("/reset-password-by-code")
    public R<String> resetPasswordByCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String phone = request.get("phone");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        if ((email == null || email.trim().isEmpty()) && (phone == null || phone.trim().isEmpty())) {
            return R.error("请提供邮箱或手机号");
        }
        if (code == null || code.trim().isEmpty()) {
            return R.error("验证码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return R.error("新密码不能为空");
        }
        if (newPassword.length() < 8 || newPassword.length() > 20) {
            return R.error("密码长度必须在8-20个字符之间");
        }

        // 重构目的：将"校验验证码 + 修改密码"的事务下沉到 Service，
        // Controller 不再持有 UserMapper/PasswordEncoder。
        String contact = (email != null && !email.trim().isEmpty()) ? email.trim() : phone.trim();
        boolean result = userService.resetPasswordByCode(contact, code.trim(), newPassword);
        if (result) {
            return R.success("密码修改成功");
        }
        return R.error("验证码错误或已过期");
    }

    @GetMapping("/status")
    public R<Map<Long, String>> getUserStatus(@RequestParam String userIds) {
        // 安全修复：解析用户ID时捕获NumberFormatException，防止非法输入导致500错误
        List<Long> userIdList;
        try {
            userIdList = Arrays.stream(userIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return R.badRequest("用户ID格式不正确");
        }

        Map<Long, String> statusMap = userOnlineStatusService.getFriendStatusMap(userIdList);
        return R.success(statusMap);
    }

    @PostMapping("/status/update")
    public R<String> updateUserStatus(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String status = (String) request.get("status");

        if (userIdObj == null || status == null) {
            return R.error("参数错误");
        }

        Long clientUserId;
        try {
            clientUserId = ((Number) userIdObj).longValue();
        } catch (ClassCastException e) {
            return R.error("参数错误");
        }

        // P1 安全修复：强制以当前登录用户为准，禁止客户端通过传入他人 userId 改他人在线状态（IDOR）
        Long userId = AuthContext.requireSelf(clientUserId);

        userOnlineStatusService.updateStatus(userId, status);
        return R.success("状态更新成功");
    }

    // ==================== 未成年保护模式 ====================

    /**
     * 获取当前登录用户的未成年保护模式状态。
     * 返回 isMinor / minorModeActive / nightRestricted / currentTime / nightStartTime / nightEndTime。
     */
    @GetMapping("/minor-mode/status")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "GET_MINOR_MODE_STATUS", resourceType = "USER")
    public R<Map<String, Object>> getMinorModeStatus() {
        Long userId = AuthContext.requireUserId();
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        return R.success(minorProtectionService.getMinorModeStatus(user));
    }

    /**
     * 开启/关闭未成年保护模式（仅本人）。
     * - 成年人可自由开启/关闭；
     * - 未成年人不可手动关闭（Service 层拒绝）。
     */
    @PutMapping("/minor-mode")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "TOGGLE_MINOR_MODE", resourceType = "USER")
    public R<User> updateMinorMode(@RequestBody Map<String, Object> body) {
        Long userId = AuthContext.requireUserId();
        Object enabledObj = body.get("enabled");
        if (enabledObj == null) {
            return R.badRequest("enabled 不能为空");
        }
        boolean enabled;
        try {
            enabled = Boolean.parseBoolean(String.valueOf(enabledObj));
        } catch (Exception e) {
            return R.badRequest("enabled 必须为布尔值");
        }
        User updated = minorProtectionService.updateMinorMode(userId, enabled);
        updated.setPassword(null);
        return R.success(updated);
    }

    // ==================== 语音音色偏好（多音色 TTS） ====================

    /**
     * 获取当前登录用户的语音音色偏好。
     * GET /api/user/voice-preference
     */
    @GetMapping("/voice-preference")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> getVoicePreference() {
        Long userId = AuthContext.requireUserId();
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        String pref = com.example.aichat.service.voice.VoiceProfile.isValid(user.getVoicePreference())
                ? user.getVoicePreference()
                : com.example.aichat.service.voice.VoiceProfile.DEFAULT.getVoiceId();
        Map<String, Object> data = new HashMap<>();
        data.put("voicePreference", pref);
        data.put("voices", com.example.aichat.service.voice.VoiceProfile.values().length);
        return R.success(data);
    }

    /**
     * 更新当前登录用户的语音音色偏好。
     * PUT /api/user/voice-preference  body: { "voicePreference": "warm" }
     */
    @PutMapping("/voice-preference")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> updateVoicePreference(@RequestBody Map<String, Object> body) {
        Long userId = AuthContext.requireUserId();
        Object raw = body == null ? null : body.get("voicePreference");
        String voicePreference = raw == null ? null : String.valueOf(raw).trim();
        if (!com.example.aichat.service.voice.VoiceProfile.isValid(voicePreference)) {
            return R.badRequest("音色标识不合法，可选值：default/warm/cold/young/old/female/male");
        }
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
        user.setVoicePreference(voicePreference);
        User updated = userService.updateUser(user);
        if (updated == null) {
            return R.error("更新音色偏好失败");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("voicePreference", updated.getVoicePreference());
        return R.success(data);
    }

    // ==================== 账号自助注销（M3 ⑨） ====================

    /**
     * 账号自助删除（软删除）。
     *
     * 安全要求：
     *   - requireUserId 校验当前登录用户（仅本人可注销自己的账号）；
     *   - 二次确认：请求体 confirmText 必须为 "DELETE" 才执行；
     *   - 软删除：标记 accountStatus=deleted，不可恢复；
     *   - 删除后该 token 立即失效（JwtAuthenticationFilter 按 accountStatus 拒绝）。
     */
    @DeleteMapping("/account")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = "DELETE_ACCOUNT", resourceType = "USER")
    public R<String> deleteAccount(@RequestBody Map<String, String> body) {
        Long userId = AuthContext.requireUserId();
        String confirmText = body == null ? null : body.get("confirmText");
        userService.deleteAccount(userId, confirmText);
        return R.success("账号已注销");
    }
}
