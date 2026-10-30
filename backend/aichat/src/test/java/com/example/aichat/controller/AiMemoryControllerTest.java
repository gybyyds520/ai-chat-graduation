package com.example.aichat.controller;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.entity.AiMemory;
import com.example.aichat.service.AiMemoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AI 记忆接口安全测试：requireUserId（未登录拒绝）+ 归属校验（IDOR 传播）。
 */
@ExtendWith(MockitoExtension.class)
class AiMemoryControllerTest {

    private static final Long CURRENT_USER = 1L;

    @Mock
    private AiMemoryService aiMemoryService;

    private AiMemoryController controller;

    @BeforeEach
    void setUp() {
        controller = new AiMemoryController();
        ReflectionTestUtils.setField(controller, "aiMemoryService", aiMemoryService);
        UserPrincipal principal = new UserPrincipal(CURRENT_USER, "u", List.of("ROLE_USER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void stats_未登录_抛TOKEN_INVALID() {
        SecurityContextHolder.clearContext();
        BusinessException ex = assertThrows(BusinessException.class, () -> controller.stats());
        assertEquals(UserErrorCode.TOKEN_INVALID, ex.getErrorCode());
        verify(aiMemoryService, never()).getMemoryStats(any());
    }

    @Test
    void list_已登录_以当前用户调用服务() {
        when(aiMemoryService.listMemories(eq(CURRENT_USER), anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        assertDoesNotThrow(() -> controller.list(1, 10, null, null, null, null, null));
        verify(aiMemoryService).listMemories(eq(CURRENT_USER), eq(1), eq(10), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void list_搜索参数透传() {
        when(aiMemoryService.listMemories(eq(CURRENT_USER), anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        assertDoesNotThrow(() -> controller.list(1, 10, null, null, null, "咖啡", "chat"));
        verify(aiMemoryService).listMemories(eq(CURRENT_USER), eq(1), eq(10), isNull(), isNull(), isNull(), eq("咖啡"), eq("chat"));
    }

    @Test
    void detail_越权访问_服务层拒绝异常向上传播() {
        // 模拟：记忆属于他人，Service 层 requireOwnership 抛 FORBIDDEN
        when(aiMemoryService.getMemory(eq(CURRENT_USER), eq(10L)))
                .thenThrow(new BusinessException(
                        com.example.aichat.common.exception.AiErrorCode.MEMORY_FORBIDDEN));
        BusinessException ex = assertThrows(BusinessException.class, () -> controller.detail(10L));
        assertEquals("无权操作该记忆", ex.getMessage());
    }

    @Test
    void create_内容为空_抛业务异常() {
        assertThrows(BusinessException.class,
                () -> controller.create(Map.of("content", "  ")));
        verify(aiMemoryService, never()).createMemory(any(), any(), any(), any(), any(), any());
    }

    @Test
    void AuthContext_当前用户可用() {
        assertEquals(CURRENT_USER, AuthContext.requireUserId());
    }
}
