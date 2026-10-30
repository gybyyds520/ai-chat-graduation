package com.example.aichat.config;

import com.example.aichat.service.AdminService;
import com.example.aichat.utils.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC 权限矩阵集成测试 — 遍历核心管理端接口，验证普通用户一律 403、管理员放行。
 *
 * 覆盖接口组：
 *   - /api/admin/users          （URL 匹配 hasRole ADMIN）
 *   - /api/admin/stats          （URL 匹配 hasRole ADMIN）
 *   - /api/admin/characters     （URL 匹配 hasRole ADMIN）
 *   - /api/admin/dashboard/stats（URL 匹配 hasRole ADMIN）
 *   - /api/audit/logs           （类级 @PreAuthorize hasRole ADMIN，不走 URL 匹配）
 *   - /api/audit/stats          （类级 @PreAuthorize hasRole ADMIN）
 *
 * 测试矩阵（每个接口 × 三种身份）：
 *   - 无 token          → 401
 *   - 普通用户 token    → 403
 *   - 管理员 token      → 200
 *
 * 架构同 SecurityConfigAuthorizationTest：仅加载 SecurityConfig + StubController，
 * 跳过 DataSource，CI 无 MySQL 可运行。
 */
@org.junit.jupiter.api.extension.ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SecurityRbacMatrixIntegrationTest.TestConfig.class})
@WebAppConfiguration
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class
})
class SecurityRbacMatrixIntegrationTest {

    /** 管理端 GET 接口清单（URL 匹配 hasRole ADMIN）。 */
    private static final String[] ADMIN_GET_ENDPOINTS = {
            "/api/admin/users",
            "/api/admin/stats",
            "/api/admin/characters",
            "/api/admin/dashboard/stats"
    };

    /** /api/audit/** 管理端 GET 接口（类级 @PreAuthorize，不走 URL 匹配）。 */
    private static final String[] AUDIT_GET_ENDPOINTS = {
            "/api/audit/logs",
            "/api/audit/stats"
    };

    @Configuration
    @Import(SecurityConfig.class)
    static class TestConfig {
        @Bean
        public AdminService adminService() {
            AdminService mock = org.mockito.Mockito.mock(AdminService.class);
            org.mockito.Mockito.when(mock.isAdmin(org.mockito.ArgumentMatchers.anyLong())).thenReturn(false);
            org.mockito.Mockito.when(mock.isAdmin(0L)).thenReturn(true);
            return mock;
        }

        /**
         * Stub 控制器：模拟管理端接口。
         * - /api/admin/** 由 SecurityConfig URL 规则保护（hasRole ADMIN）；
         * - /api/audit/** 由类级 @PreAuthorize 保护。
         */
        @RestController
        static class StubAdminController {
            @GetMapping("/api/admin/users")
            public String users() { return "users-ok"; }

            @GetMapping("/api/admin/stats")
            public String stats() { return "stats-ok"; }

            @GetMapping("/api/admin/characters")
            public String characters() { return "characters-ok"; }

            @GetMapping("/api/admin/dashboard/stats")
            public String dashboard() { return "dashboard-ok"; }

            @PostMapping("/api/admin/login")
            public String adminLogin() { return "admin-login-ok"; }
        }

        @RestController
        @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
        static class StubAuditController {
            @GetMapping("/api/audit/logs")
            public String logs() { return "logs-ok"; }

            @GetMapping("/api/audit/stats")
            public String auditStats() { return "audit-stats-ok"; }
        }
    }

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
        userToken = TokenUtils.generateToken("42", "alice");
        adminToken = TokenUtils.generateToken("0", "admin");
    }

    // === /api/admin/** URL 匹配保护 ===

    @Test
    void admin接口_无token_返回401() throws Exception {
        for (String endpoint : ADMIN_GET_ENDPOINTS) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(401));
        }
    }

    @Test
    void admin接口_普通用户token_返回403() throws Exception {
        for (String endpoint : ADMIN_GET_ENDPOINTS) {
            mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
        }
    }

    @Test
    void admin接口_管理员token_返回200() throws Exception {
        for (String endpoint : ADMIN_GET_ENDPOINTS) {
            mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }

    // === /api/audit/** 类级 @PreAuthorize 保护 ===

    @Test
    void audit接口_无token_返回401() throws Exception {
        for (String endpoint : AUDIT_GET_ENDPOINTS) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void audit接口_普通用户token_返回403() throws Exception {
        for (String endpoint : AUDIT_GET_ENDPOINTS) {
            mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
        }
    }

    @Test
    void audit接口_管理员token_返回200() throws Exception {
        for (String endpoint : AUDIT_GET_ENDPOINTS) {
            mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }

    // === 写操作也受保护 ===

    @Test
    void admin写操作_普通用户_403() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin登录端点_公开可访问() throws Exception {
        // /api/admin/login 必须 permitAll，否则无法获取 admin token
        mockMvc.perform(post("/api/admin/login")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isOk());
    }
}
