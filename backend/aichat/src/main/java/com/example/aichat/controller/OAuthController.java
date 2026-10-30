package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.service.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/oauth")
public class OAuthController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    private final OAuthService oAuthService;

    public OAuthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    /**
     * 安全修复（P2）：OAuth state 防 CSRF。
     * 发起登录时生成一次性 state（10 分钟有效），回调时必须回传同一 state，
     * 防止攻击者伪造授权回调。state 仅存内存，重启后失效（可接受，登录重发起即可）。
     */
    private static final ConcurrentHashMap<String, Long> WECHAT_STATES = new ConcurrentHashMap<>();
    private static final long STATE_TTL_MS = 10 * 60 * 1000L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateState() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @PostMapping("/wechat/login")
    public R wechatLogin(@RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        String deviceId = (String) request.get("deviceId");
        String ipAddress = (String) request.get("ipAddress");

        logger.info("[OAuthController] 收到微信登录请求: code={}, deviceId={}, ipAddress={}", 
                   code != null ? code.substring(0, 8) + "..." : null, deviceId, ipAddress);

        if (code == null || code.trim().isEmpty()) {
            logger.warn("[OAuthController] 微信登录失败: code为空");
            return R.badRequest("code不能为空");
        }

        Map<String, Object> result = oAuthService.wechatLogin(code, deviceId, ipAddress);
        
        if ((Boolean) result.get("success")) {
            // 生成一次性 state 供浏览器回调流程校验（OAuth CSRF 防护）
            String state = generateState();
            WECHAT_STATES.put(state, System.currentTimeMillis());
            result.put("state", state);
            return R.success(result);
        }
        
        String message = (String) result.get("message");
        if (result.containsKey("riskLevel")) {
            return R.error(message);
        }
        return R.error(message);
    }

    @GetMapping("/wechat/callback")
    public R wechatCallback(@RequestParam String code, @RequestParam(required = false) String state) {
        logger.info("[OAuthController] 收到微信回调: code={}, state={}", 
                   code != null ? code.substring(0, 8) + "..." : null, state);

        // 安全修复（P2）：state 必须由本服务签发且在有效期内，校验后立即失效（一次性）
        if (state == null || state.isBlank()) {
            logger.warn("[OAuthController] 微信回调缺少 state，已拒绝（CSRF 防护）");
            return R.badRequest("state 缺失，请重新发起授权");
        }
        Long issuedAt = WECHAT_STATES.remove(state);
        if (issuedAt == null) {
            logger.warn("[OAuthController] 微信回调 state 无效或已过期，已拒绝");
            return R.badRequest("state 无效或已过期，请重新发起授权");
        }
        if (System.currentTimeMillis() - issuedAt > STATE_TTL_MS) {
            logger.warn("[OAuthController] 微信回调 state 已过期，已拒绝");
            return R.badRequest("state 已过期，请重新发起授权");
        }

        Map<String, Object> result = oAuthService.wechatCallback(code, state);
        
        if ((Boolean) result.get("success")) {
            return R.success(result);
        }
        return R.error((String) result.get("message"));
    }

    @PostMapping("/wechat/bind")
    public R bindWechat(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String code = (String) request.get("code");

        if (code == null || code.trim().isEmpty()) {
            return R.badRequest("参数错误");
        }

        // 安全修复（P0）：绑定只允许操作当前登录用户自己的账号，防止替他人绑定微信
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        logger.info("[OAuthController] 用户 {} 绑定微信", userId);

        try {
            oAuthService.bindWechat(userId, code);
            return R.success("绑定成功");
        } catch (RuntimeException e) {
            logger.error("[OAuthController] 微信绑定失败: {}", e.getMessage());
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/wechat/unbind")
    public R unbindWechat(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");

        // 安全修复（P0）：解绑只允许操作当前登录用户自己的账号
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        logger.info("[OAuthController] 用户 {} 解绑微信", userId);

        oAuthService.unbindWechat(userId);
        return R.success("解绑成功");
    }

    @GetMapping("/wechat/status")
    public R checkWechatBindStatus(@RequestParam(required = false) Long userId) {
        // 安全修复（P0）：只能查询自己的绑定状态
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        boolean bound = oAuthService.isWechatBound(current);
        return R.success(Map.of("bound", bound));
    }
}