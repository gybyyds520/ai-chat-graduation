package com.example.aichat.common.security;

import com.example.aichat.service.AdminService;
import com.example.aichat.utils.TokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Phase 3 (P3-e) JwtAuthenticationFilter 角色注入验证测试。
 *
 * 覆盖点：
 * 1. 无 token 请求：SecurityContext 保持空，不抛异常；
 * 2. 普通用户 token（userId != 0）：注入 ROLE_USER；
 * 3. 管理员 token（userId == 0）：注入 ROLE_ADMIN；
 * 4. 无效 token：SecurityContext 保持空，请求继续。
 *
 * 使用 Spring 的 MockHttpServletRequest/Response，纯单元测试，无需 ApplicationContext。
 */
class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
    private final FilterChain noopChain = (req, resp) -> { /* no-op */ };
    private AdminService mockAdminService;

    @BeforeEach
    void setUp() {
        // 注入 mock AdminService：userId=0 视为管理员，其他为普通用户
        mockAdminService = mock(AdminService.class);
        when(mockAdminService.isAdmin(anyLong())).thenReturn(false);
        when(mockAdminService.isAdmin(0L)).thenReturn(true);
        ReflectionTestUtils.setField(filter, "adminService", mockAdminService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noTokenRequest_securityContextRemainsEmpty() throws Exception {
        HttpServletRequest req = new MockHttpServletRequest();
        HttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, noopChain);
        // 无 token 时不应注入认证信息
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void normalUserToken_injectsRoleUser() throws Exception {
        // 生成 userId=42 的普通用户 token
        String token = TokenUtils.generateToken("42", "alice");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        HttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, noopChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "有效 token 应注入 Authentication");
        assertTrue(auth.isAuthenticated());
        assertTrue(auth.getPrincipal() instanceof UserPrincipal);
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(42L, principal.getUserId());
        // 普通用户应为 ROLE_USER，不是 ROLE_ADMIN
        assertTrue(AuthContext.hasRole("USER"));
        assertFalse(AuthContext.hasRole("ADMIN"));
    }

    @Test
    void adminToken_injectsRoleAdmin() throws Exception {
        // 生成 userId=0 的管理员 token（与 AdminController.adminLogin 颁发的格式一致）
        String adminToken = TokenUtils.generateToken("0", "admin");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + adminToken);
        HttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, noopChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "管理员 token 应注入 Authentication");
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals(0L, principal.getUserId());
        // 管理员应为 ROLE_ADMIN
        assertTrue(AuthContext.hasRole("ADMIN"));
        assertFalse(AuthContext.hasRole("USER"), "管理员 token 不应同时拥有 USER 角色");
    }

    @Test
    void invalidToken_securityContextRemainsEmpty() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer invalid.token.value");
        HttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, noopChain);
        // 无效 token 不应注入认证，但请求应继续（不抛异常）
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void legacyTokenHeader_supported() throws Exception {
        // 兼容历史前端可能用的 token: xxx 头格式
        String token = TokenUtils.generateToken("42", "alice");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("token", token);  // 非标准 Authorization 头
        HttpServletResponse resp = new MockHttpServletResponse();

        filter.doFilter(req, resp, noopChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "应兼容历史 token 头格式");
    }
}
