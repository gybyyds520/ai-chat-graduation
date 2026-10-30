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
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4 (P4-h) SecurityConfig 路由鉴权集成测试。
 *
 * 覆盖点：
 * 1. 公开路径在无 token 时应放行（认证端点、健康检查、匿名浏览 GET）；
 * 2. 受保护写操作（POST/PUT/DELETE）在无 token 时应返回 401，且响应为标准 JSON；
 * 3. 管理员端点 /api/admin/** 在普通用户 token 时返回 403，在管理员 token 时放行；
 * 4. 401/403 响应体格式与 R DTO 一致（code/msg/data），便于前端统一处理。
 *
 * 测试架构：用 @ContextConfiguration 显式指定加载 SecurityConfig + StubController，
 * 用 @ImportAutoConfiguration 仅加载 Web MVC + Security 自动配置，
 * 完全跳过 AichatApplication（避免 @MapperScan 触发 DataSource 依赖），CI 无 MySQL 可运行。
 */
@org.junit.jupiter.api.extension.ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SecurityConfigAuthorizationTest.TestConfig.class})
@WebAppConfiguration
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class
})
class SecurityConfigAuthorizationTest {

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

        @RestController
        static class StubController {
            @PostMapping("/api/user/login")
            public String login() { return "ok"; }

            @PostMapping("/api/user/register")
            public String register() { return "ok"; }

            @GetMapping("/api/health")
            public String health() { return "ok"; }

            @GetMapping("/api/character/list")
            public String characterList() { return "ok"; }

            @GetMapping("/api/character/list/user/{userId}")
            public String characterListByUser(@PathVariable Long userId) { return "ok"; }

            @GetMapping("/api/character/{id}")
            public String characterDetail(@PathVariable Long id) { return "ok"; }

            @PostMapping("/api/character/create")
            public String createCharacter() { return "ok"; }

            @PutMapping("/api/character/{id}")
            public String updateCharacter(@PathVariable Long id) { return "ok"; }

            @DeleteMapping("/api/character/{id}")
            public String deleteCharacter(@PathVariable Long id) { return "ok"; }

            @PostMapping("/api/payment/create")
            public String createPayment() { return "ok"; }

            @GetMapping("/api/community/posts")
            public String communityPosts() { return "ok"; }

            @GetMapping("/api/admin/users")
            public String adminUsers() { return "ok"; }

            @PostMapping("/api/admin/users/{id}/lock")
            public String adminLockUser(@PathVariable Long id) { return "ok"; }
        }
    }

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        // 应用 Spring Security 集成，使 MockMvc 经过完整 FilterChain 鉴权
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // === 1. 公开路径：无 token 应放行 ===

    @Test
    void loginEndpoint_allowsAnonymous() throws Exception {
        mockMvc.perform(post("/api/user/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registerEndpoint_allowsAnonymous() throws Exception {
        mockMvc.perform(post("/api/user/register"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpoint_allowsAnonymous() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void characterListGet_allowsAnonymousBrowsing() throws Exception {
        mockMvc.perform(get("/api/character/list"))
                .andExpect(status().isOk());
    }

    @Test
    void characterDetailGet_allowsAnonymousBrowsing() throws Exception {
        mockMvc.perform(get("/api/character/123"))
                .andExpect(status().isOk());
    }

    @Test
    void communityPostsGet_allowsAnonymousBrowsing() throws Exception {
        mockMvc.perform(get("/api/community/posts"))
                .andExpect(status().isOk());
    }

    // === 2. 受保护写操作：无 token 应返回 401 ===

    @Test
    void createCharacterPost_rejectsAnonymous() throws Exception {
        mockMvc.perform(post("/api/character/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    void updateCharacterPut_rejectsAnonymous() throws Exception {
        mockMvc.perform(put("/api/character/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCharacterDelete_rejectsAnonymous() throws Exception {
        mockMvc.perform(delete("/api/character/123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPaymentPost_rejectsAnonymous() throws Exception {
        mockMvc.perform(post("/api/payment/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // === 3. 管理员端点：角色分级控制 ===

    @Test
    void adminEndpoint_rejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_rejectsNormalUser() throws Exception {
        String userToken = TokenUtils.generateToken("42", "alice");
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    void adminEndpoint_allowsAdmin() throws Exception {
        String adminToken = TokenUtils.generateToken("0", "admin");
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminWriteEndpoint_rejectsNormalUser() throws Exception {
        String userToken = TokenUtils.generateToken("42", "alice");
        mockMvc.perform(post("/api/admin/users/123/lock")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWriteEndpoint_allowsAdmin() throws Exception {
        String adminToken = TokenUtils.generateToken("0", "admin");
        mockMvc.perform(post("/api/admin/users/123/lock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    // === 4. 普通 token 应能通过受保护写操作（验证放行路径） ===

    @Test
    void createCharacterPost_allowsAuthenticatedUser() throws Exception {
        String userToken = TokenUtils.generateToken("42", "alice");
        mockMvc.perform(post("/api/character/create")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
