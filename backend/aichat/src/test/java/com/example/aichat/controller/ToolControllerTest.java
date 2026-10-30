package com.example.aichat.controller;

import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.dto.R;
import com.example.aichat.service.ToolCallService;
import com.example.aichat.service.tool.ToolResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ToolController 单元测试（Mockito 隔离 Service，手动构造 SecurityContext）。
 */
@ExtendWith(MockitoExtension.class)
class ToolControllerTest {

    @Mock
    private ToolCallService toolCallService;

    @InjectMocks
    private ToolController controller;

    @BeforeEach
    void login() {
        UserPrincipal principal = new UserPrincipal(42L, "alice");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_应返回可用工具() {
        when(toolCallService.listAvailableTools())
                .thenReturn(List.of(Map.of("name", "calculator", "description", "calc")));
        R resp = controller.list();
        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
    }

    @Test
    void call_应转发toolName与parameters() {
        when(toolCallService.callTool(eq(42L), eq("calculator"), any()))
                .thenReturn(ToolResult.success(Map.of("result", 2)));
        R resp = controller.call(Map.of("toolName", "calculator", "parameters", Map.of("expression", "1+1")));
        assertEquals(200, resp.getCode());
        verify(toolCallService).callTool(eq(42L), eq("calculator"), any());
    }

    @Test
    void history_应按当前用户分页() {
        R resp = controller.history(1, 20);
        assertEquals(200, resp.getCode());
        verify(toolCallService).getToolCallHistory(42L, 1, 20);
    }
}
