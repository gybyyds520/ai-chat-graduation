package com.example.aichat.common.security;

import com.example.aichat.entity.User;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.AdminService;
import com.example.aichat.utils.TokenUtils;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * M3 ⑨ 账号注销后 Token 立即失效的过滤器测试。
 *
 * 覆盖点：
 *   - 用户账号状态为 deleted 时，即使携带有效 JWT，SecurityContext 也不注入（视为未认证 → 401）；
 *   - 用户记录不存在时同样拒绝；
 *   - 账号状态正常（active）时正常注入 Authentication（回归保护）。
 */
class JwtDeletedAccountFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
    private final FilterChain noopChain = (req, resp) -> { };
    private UserMapper userMapper;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        adminService = mock(AdminService.class);
        when(adminService.isAdmin(anyLong())).thenReturn(false);
        ReflectionTestUtils.setField(filter, "userMapper", userMapper);
        ReflectionTestUtils.setField(filter, "adminService", adminService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User userWithStatus(String status) {
        User u = new User();
        u.setId(42L);
        u.setUsername("alice");
        u.setAccountStatus(status);
        return u;
    }

    @Test
    void deletedAccountToken_应被拒绝且不注入认证() throws Exception {
        when(userMapper.selectById(42L)).thenReturn(userWithStatus("deleted"));

        String token = TokenUtils.generateToken("42", "alice");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(req, new MockHttpServletResponse(), noopChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "已注销账号的 token 不应注入认证");
    }

    @Test
    void missingUserToken_应被拒绝() throws Exception {
        when(userMapper.selectById(42L)).thenReturn(null);

        String token = TokenUtils.generateToken("42", "alice");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(req, new MockHttpServletResponse(), noopChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void activeAccountToken_应正常注入认证() throws Exception {
        when(userMapper.selectById(42L)).thenReturn(userWithStatus("active"));

        String token = TokenUtils.generateToken("42", "alice");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(req, new MockHttpServletResponse(), noopChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "正常账号 token 应注入认证（回归保护）");
    }
}
