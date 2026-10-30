package com.example.aichat.common.security;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthContext 单元测试。
 *
 * 覆盖点：
 *   1. requireUserId：已认证返回 ID、未认证抛 TOKEN_INVALID；
 *   2. requireSelf：客户端 ID 与当前用户一致返回、不一致抛 TOKEN_INVALID、
 *      客户端传 null 仅要求已认证；
 *   3. getCurrentUser/getCurrentUsername/isAuthenticated/hasRole。
 */
class AuthContextTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String username, String... roles) {
        UserPrincipal principal = new UserPrincipal(userId, username, List.of(roles));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void requireUserId_已认证返回ID() {
        authenticateAs(42L, "alice", "ROLE_USER");
        assertEquals(42L, AuthContext.requireUserId());
    }

    @Test
    void requireUserId_未认证抛TOKEN_INVALID() {
        BusinessException ex = assertThrows(BusinessException.class, AuthContext::requireUserId);
        assertEquals(UserErrorCode.TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    void requireSelf_客户端ID一致返回当前ID() {
        authenticateAs(42L, "alice", "ROLE_USER");
        assertEquals(42L, AuthContext.requireSelf(42L));
    }

    @Test
    void requireSelf_客户端ID不一致抛TOKEN_INVALID() {
        authenticateAs(42L, "alice", "ROLE_USER");
        BusinessException ex = assertThrows(BusinessException.class, () -> AuthContext.requireSelf(999L));
        assertEquals(UserErrorCode.TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    void requireSelf_客户端为null仅要求已认证() {
        authenticateAs(42L, "alice", "ROLE_USER");
        assertEquals(42L, AuthContext.requireSelf(null));
    }

    @Test
    void requireSelf_未认证抛异常() {
        assertThrows(BusinessException.class, () -> AuthContext.requireSelf(1L));
    }

    @Test
    void getCurrentUser_已认证返回Optional() {
        authenticateAs(42L, "alice", "ROLE_USER");
        Optional<UserPrincipal> user = AuthContext.getCurrentUser();
        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
    }

    @Test
    void getCurrentUserId_未认证返回empty() {
        assertTrue(AuthContext.getCurrentUserId().isEmpty());
    }

    @Test
    void getCurrentUsername_已认证返回用户名() {
        authenticateAs(42L, "bob", "ROLE_USER");
        assertEquals("bob", AuthContext.getCurrentUsername().orElseThrow());
    }

    @Test
    void isAuthenticated_已认证true未认证false() {
        assertFalse(AuthContext.isAuthenticated());
        authenticateAs(1L, "x", "ROLE_USER");
        assertTrue(AuthContext.isAuthenticated());
    }

    @Test
    void hasRole_具备角色true不具备false() {
        authenticateAs(1L, "admin", "ROLE_ADMIN");
        assertTrue(AuthContext.hasRole("ADMIN"));
        assertFalse(AuthContext.hasRole("USER"));
    }

    @Test
    void hasRole_未认证false() {
        assertFalse(AuthContext.hasRole("ADMIN"));
    }
}
