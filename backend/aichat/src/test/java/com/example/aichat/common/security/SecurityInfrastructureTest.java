package com.example.aichat.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2 (P2-2) 安全基础设施验证测试。
 *
 * 覆盖点：
 * 1. UserPrincipal 构造与 UserDetails 接口实现正确；
 * 2. AuthContext 在有/无 SecurityContext 情况下均行为正确；
 * 3. 角色判断 hasRole 支持 ROLE_ 前缀自动补全。
 *
 * 不依赖 Spring 上下文启动，纯单元测试可在 CI 无 MySQL 环境运行。
 */
class SecurityInfrastructureTest {

    @AfterEach
    void clearContext() {
        // 每个测试后清理 SecurityContext，避免互相干扰
        SecurityContextHolder.clearContext();
    }

    @Test
    void userPrincipal_defaultUserRoleApplied() {
        UserPrincipal principal = new UserPrincipal(42L, "alice");
        // 默认构造应赋予 ROLE_USER
        assertNotNull(principal.getAuthorities());
        assertTrue(principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertEquals(42L, principal.getUserId());
        assertEquals("alice", principal.getUsername());
        // UserDetails 默认状态：账户可用、未过期、未锁定
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
        // JWT 无状态认证：getPassword 返回 null
        assertNull(principal.getPassword());
    }

    @Test
    void userPrincipal_customRolesWithAndWithoutPrefix() {
        // 显式带 ROLE_ 前缀
        UserPrincipal p1 = new UserPrincipal(1L, "admin",
                java.util.List.of("ROLE_ADMIN", "MANAGE_USERS"));
        assertTrue(p1.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(p1.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGE_USERS")));

        // 不带前缀的角色自动补全
        UserPrincipal p2 = new UserPrincipal(2L, "user", Collections.singleton("USER"));
        assertTrue(p2.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void authContext_emptyWhenNoAuthentication() {
        // 无认证时返回 Optional.empty，不抛异常
        assertTrue(AuthContext.getCurrentUserId().isEmpty());
        assertTrue(AuthContext.getCurrentUsername().isEmpty());
        assertFalse(AuthContext.isAuthenticated());
        assertFalse(AuthContext.hasRole("ADMIN"));
    }

    @Test
    void authContext_returnsUserWhenAuthenticated() {
        UserPrincipal principal = new UserPrincipal(99L, "bob");
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(Optional.of(99L), AuthContext.getCurrentUserId());
        assertEquals(Optional.of("bob"), AuthContext.getCurrentUsername());
        assertTrue(AuthContext.isAuthenticated());
        assertTrue(AuthContext.hasRole("USER"));
        assertFalse(AuthContext.hasRole("ADMIN"));
    }

    @Test
    void authContext_hasRoleSupportsPrefixAndNoPrefix() {
        UserPrincipal principal = new UserPrincipal(1L, "admin",
                java.util.List.of("ROLE_ADMIN"));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 传 "ADMIN" 或 "ROLE_ADMIN" 都应识别
        assertTrue(AuthContext.hasRole("ADMIN"));
        assertTrue(AuthContext.hasRole("ROLE_ADMIN"));
    }

    @Test
    void authContext_anonymousAuthenticationTreatedAsUnauthenticated() {
        // Spring Security 的 anonymousAuthenticationToken 不应被识别为已登录用户
        // （principal 不是 UserPrincipal 实例）
        Authentication anonymous = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        // 我们的 AuthContext 只识别 UserPrincipal 类型，匿名应视为未登录
        assertFalse(AuthContext.isAuthenticated());
        assertTrue(AuthContext.getCurrentUserId().isEmpty());
    }
}
