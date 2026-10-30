package com.example.aichat.controller;

import com.example.aichat.config.SecurityConfig;
import com.example.aichat.dto.ai.AiConfigDTO;
import com.example.aichat.mapper.AiSystemMapper;
import com.example.aichat.mapper.DataAccessPermissionMapper;
import com.example.aichat.service.AdminService;
import com.example.aichat.service.AiConfigService;
import com.example.aichat.service.AiSystemService;
import com.example.aichat.service.DataAccessPermissionService;
import com.example.aichat.service.ai.OllamaService;
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
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AiSystemController 管理端点鉴权测试。
 *
 * 覆盖：
 *   1. 写端点：普通用户→403、匿名→401、管理员→200；
 *   2. 读端点（list/{id}/{id}/permissions/ollama/status）现已要求 ADMIN：普通用户→403、匿名→401、管理员→200；
 *   3. POST /authenticate 保持公开（API Key 认证），匿名访问不返回 401；
 *   4. 新增配置/统计端点：管理员可访问。
 */
@org.junit.jupiter.api.extension.ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AiSystemControllerSecurityTest.TestConfig.class)
@WebAppConfiguration
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class
})
class AiSystemControllerSecurityTest {

    @Configuration
    @Import(SecurityConfig.class)
    static class TestConfig {
        @Bean
        public AdminService adminService() {
            AdminService mock = mock(AdminService.class);
            when(mock.isAdmin(anyLong())).thenReturn(false);
            when(mock.isAdmin(0L)).thenReturn(true);
            return mock;
        }

        @Bean
        public AiSystemService aiSystemService() {
            AiSystemService mock = mock(AiSystemService.class);
            when(mock.getAllSystems()).thenReturn(List.of());
            when(mock.deleteSystem(anyLong())).thenReturn(true);
            when(mock.getStats()).thenReturn(new LinkedHashMap<>());
            return mock;
        }

        @Bean
        public DataAccessPermissionService dataAccessPermissionService() {
            DataAccessPermissionService mock = mock(DataAccessPermissionService.class);
            when(mock.getSystemPermissions(anyLong())).thenReturn(List.of());
            return mock;
        }

        @Bean
        public OllamaService ollamaService() {
            OllamaService mock = mock(OllamaService.class);
            when(mock.isOllamaAvailable()).thenReturn(true);
            when(mock.listModels()).thenReturn(List.of());
            com.example.aichat.dto.ai.OllamaHealth health = new com.example.aichat.dto.ai.OllamaHealth();
            health.setAvailable(true);
            when(mock.getOllamaHealth()).thenReturn(health);
            return mock;
        }

        @Bean
        public AiConfigService aiConfigService() {
            AiConfigService mock = mock(AiConfigService.class);
            when(mock.getConfig()).thenReturn(new AiConfigDTO());
            return mock;
        }

        @Bean
        public AiSystemMapper aiSystemMapper() {
            return mock(AiSystemMapper.class);
        }

        @Bean
        public DataAccessPermissionMapper dataAccessPermissionMapper() {
            return mock(DataAccessPermissionMapper.class);
        }

        @Bean
        public AiSystemController aiSystemController() {
            return new AiSystemController();
        }
    }

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private String userToken() {
        return TokenUtils.generateToken("42", "alice");
    }

    private String adminToken() {
        return TokenUtils.generateToken("0", "admin");
    }

    @Test
    void writeEndpoint_rejectsNormalUser_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/aisystem/register")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void writeEndpoint_rejectsAnonymous_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/aisystem/register")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void writeEndpoint_allowsAdmin_shouldReachController() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/aisystem/1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void getList_rejectsNormalUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/aisystem/list")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getList_rejectsAnonymous_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/aisystem/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getList_allowsAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/aisystem/list")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void getOllamaStatus_rejectsAnonymous_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/aisystem/ollama/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPermissions_rejectsNormalUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/aisystem/1/permissions")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticate_withoutToken_shouldNotBeBlockedByAuth() throws Exception {
        // 公开端点：不带 JWT 不应返回 401（API Key 通过请求头认证）
        mockMvc.perform(post("/api/aisystem/authenticate")
                        .header("X-API-Key", "ak_demo")
                        .header("X-Secret-Key", "sk_demo"))
                .andExpect(status().isOk());
    }

    @Test
    void getConfig_allowsAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/aisystem/config")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void putConfig_allowsAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(put("/api/aisystem/config")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"defaultModel\":\"llama3.2:3b\"}"))
                .andExpect(status().isOk());
    }
}
