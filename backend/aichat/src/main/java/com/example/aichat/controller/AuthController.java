package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.entity.LoginAlert;
import com.example.aichat.entity.LoginDevice;
import com.example.aichat.entity.UserBadge;
import com.example.aichat.entity.UserTag;
import com.example.aichat.service.AuthService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证相关接口。
 *
 * 安全加固（P0）：原实现中 /api/auth/** 在 SecurityConfig 内为 permitAll，
 * 且所有接口信任客户端传入的 userId，导致任何匿名用户都可以读取/操作任意用户的
 * 登录设备、登录告警、徽章、标签，甚至颁发徽章、开启/校验他人 MFA、导出他人数据。
 *
 * 修复策略：
 *   1. 仅 /sms/send、/sms/login 保持公开（登录流程必需），并叠加限流防短信轰炸；
 *   2. 其余全部接口要求登录，且操作对象强制取当前登录用户（忽略/拒绝客户端传入的 userId）；
 *   3. MFA 校验改为真实 TOTP 校验（见 AuthServiceImpl）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/sms/send")
    @RateLimiter(name = "smsApi", fallbackMethod = "rateLimitedSms")
    public R sendSmsCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        if (phone == null || phone.length() != 11) {
            return R.error("请输入正确的手机号");
        }
        authService.sendSmsCode(phone);
        return R.success("验证码已发送");
    }

    /**
     * 限流兜底（短信）。
     */
    public R rateLimitedSms(Map<String, String> request, Throwable t) {
        return R.error(429, "发送过于频繁，请稍后再试");
    }

    @PostMapping("/sms/login")
    public R loginByPhone(@RequestBody Map<String, Object> request) {
        String phone = (String) request.get("phone");
        String code = (String) request.get("code");
        String deviceId = (String) request.get("deviceId");
        String ipAddress = (String) request.get("ipAddress");
        
        if (phone == null || code == null) {
            return R.error("参数错误");
        }
        
        Map<String, Object> result = authService.loginByPhone(phone, code, deviceId, ipAddress);
        if ((Boolean) result.get("success")) {
            return R.success(result);
        }
        return R.error((String) result.get("message"));
    }

    @PostMapping("/mfa/enable")
    @PreAuthorize("isAuthenticated()")
    public R enableMfa(@RequestBody Map<String, Object> request) {
        String secret = (String) request.get("secret");
        if (secret == null || secret.isBlank()) {
            return R.error("参数错误");
        }
        Long userId = AuthContext.requireSelf(null);
        boolean success = authService.enableMfa(userId, secret);
        return success ? R.success("多因素认证已启用") : R.error("操作失败");
    }

    @PostMapping("/mfa/verify")
    @PreAuthorize("isAuthenticated()")
    public R verifyMfa(@RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        if (code == null || code.isBlank()) {
            return R.error("参数错误");
        }
        Long userId = AuthContext.requireSelf(null);
        boolean success = authService.verifyMfa(userId, code);
        return success ? R.success("验证通过") : R.error("验证失败");
    }

    @GetMapping("/devices")
    @PreAuthorize("isAuthenticated()")
    public R getLoginDevices(@RequestParam(required = false) Long userId) {
        Long current = AuthContext.requireSelf(userId);
        List<LoginDevice> devices = authService.getLoginDevices(current);
        return R.success(devices);
    }

    @PostMapping("/device/trust")
    @PreAuthorize("isAuthenticated()")
    public R trustDevice(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        Object deviceIdObj = request.get("deviceId");
        Boolean trusted = (Boolean) request.get("trusted");
        
        if (deviceIdObj == null) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(userIdObj == null ? null : ((Number) userIdObj).longValue());
        Long deviceId = ((Number) deviceIdObj).longValue();
        authService.trustDevice(userId, deviceId, Boolean.TRUE.equals(trusted));
        return R.success("操作成功");
    }

    @PostMapping("/device/logout")
    @PreAuthorize("isAuthenticated()")
    public R logoutDevice(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        Object deviceIdObj = request.get("deviceId");
        
        if (deviceIdObj == null) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(userIdObj == null ? null : ((Number) userIdObj).longValue());
        Long deviceId = ((Number) deviceIdObj).longValue();
        authService.logoutDevice(userId, deviceId);
        return R.success("已退出该设备");
    }

    @GetMapping("/alerts")
    @PreAuthorize("isAuthenticated()")
    public R getLoginAlerts(@RequestParam(required = false) Long userId) {
        Long current = AuthContext.requireSelf(userId);
        List<LoginAlert> alerts = authService.getLoginAlerts(current);
        return R.success(alerts);
    }

    @PostMapping("/alerts/read")
    @PreAuthorize("isAuthenticated()")
    public R markAlertsAsRead(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        Long userId = AuthContext.requireSelf(userIdObj == null ? null : ((Number) userIdObj).longValue());
        authService.markAlertsAsRead(userId);
        return R.success("已全部标记为已读");
    }

    @GetMapping("/badges")
    @PreAuthorize("isAuthenticated()")
    public R getUserBadges(@RequestParam(required = false) Long userId) {
        Long current = AuthContext.requireSelf(userId);
        List<UserBadge> badges = authService.getUserBadges(current);
        return R.success(badges);
    }

    /**
     * 安全加固：颁发徽章属于管理操作，仅管理员可调用；且只允许给指定用户颁发
     * （管理员代操作不属于越权，仍以入参 userId 为准，但必须 ADMIN 角色）。
     */
    @PostMapping("/badge/award")
    @PreAuthorize("hasRole('ADMIN')")
    public R awardBadge(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String badgeId = (String) request.get("badgeId");
        String badgeName = (String) request.get("badgeName");
        String description = (String) request.get("description");
        
        if (userIdObj == null || badgeId == null || badgeName == null) {
            return R.error("参数错误");
        }
        
        authService.awardBadge(((Number) userIdObj).longValue(), badgeId, badgeName, description);
        return R.success("徽章已颁发");
    }

    @GetMapping("/tags")
    @PreAuthorize("isAuthenticated()")
    public R getUserTags(@RequestParam(required = false) Long userId) {
        Long current = AuthContext.requireSelf(userId);
        List<UserTag> tags = authService.getUserTags(current);
        return R.success(tags);
    }

    @PostMapping("/tag/add")
    @PreAuthorize("isAuthenticated()")
    public R addUserTag(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String tagName = (String) request.get("tagName");
        String category = (String) request.get("category");
        
        if (tagName == null || tagName.isBlank()) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(userIdObj == null ? null : ((Number) userIdObj).longValue());
        authService.addUserTag(userId, tagName, category != null ? category : "custom");
        return R.success("标签已添加");
    }

    @PostMapping("/tag/remove")
    @PreAuthorize("isAuthenticated()")
    public R removeUserTag(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String tagName = (String) request.get("tagName");
        
        if (tagName == null || tagName.isBlank()) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(userIdObj == null ? null : ((Number) userIdObj).longValue());
        authService.removeUserTag(userId, tagName);
        return R.success("标签已移除");
    }

    /**
     * 数据导出：仅允许导出当前登录用户自己的数据。
     */
    @PostMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public R exportUserData(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String exportType = (String) request.get("exportType");
        
        if (exportType == null || exportType.isBlank()) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(userIdObj == null ? null : ((Number) userIdObj).longValue());
        String filePath = authService.exportUserData(userId, exportType);
        return R.success(filePath);
    }
}
