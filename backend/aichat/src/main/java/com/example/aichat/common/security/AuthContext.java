package com.example.aichat.common.security;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 认证上下文工具类 - 提供 Service/Controller 中获取当前用户的便捷方法。
 *
 * Phase 2 (P2-2) 引入：替代手写 token 解析 + 业务方法接收 userId 参数的模式。
 *
 * 使用示例：
 *   Long userId = AuthContext.getCurrentUserId().orElseThrow(...);
 *   String username = AuthContext.getCurrentUsername().orElse("anonymous");
 *
 * 兼容性：当请求未携带 token（permitAll 路径），返回 Optional.empty()，
 *        不抛异常，业务可自行决定是否要求登录。
 */
public final class AuthContext {

    private AuthContext() {
        // 工具类禁止实例化
    }

    /**
     * 获取当前认证主体（可选）。
     */
    public static Optional<UserPrincipal> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
            return Optional.empty();
        }
        return Optional.of((UserPrincipal) auth.getPrincipal());
    }

    /**
     * 获取当前用户 ID（可选）。
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(UserPrincipal::getUserId);
    }

    /**
     * 安全加固：获取当前登录用户 ID，未登录时抛出 401 业务异常。
     * 控制器应优先使用本方法代替信任客户端传入的 userId，杜绝横向越权（IDOR）。
     */
    public static Long requireUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new BusinessException(UserErrorCode.TOKEN_INVALID));
    }

    /**
     * 安全加固：校验客户端传入的 userId 与当前登录用户一致，不一致则拒绝（403）。
     * 用于向后兼容前端仍在请求体/参数中携带 userId 的接口。
     *
     * @param clientUserId 客户端传入的 userId；为 null 时仅要求已登录
     * @return 当前登录用户 ID
     */
    public static Long requireSelf(Long clientUserId) {
        Long current = requireUserId();
        if (clientUserId != null && !current.equals(clientUserId)) {
            throw new BusinessException(UserErrorCode.TOKEN_INVALID);
        }
        return current;
    }

    /**
     * 获取当前用户名（可选）。
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentUser().map(UserPrincipal::getUsername);
    }

    /**
     * 判断当前是否已认证（携带有效 token）。
     */
    public static boolean isAuthenticated() {
        return getCurrentUser().isPresent();
    }

    /**
     * 判断当前用户是否拥有指定角色。
     */
    public static boolean hasRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(roleWithPrefix));
    }
}
