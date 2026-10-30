package com.example.aichat.controller;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.dto.R;
import com.example.aichat.service.UserOnlineStatusService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserController.updateUserStatus 安全测试（P1 IDOR 回归）。
 *
 * 修复点：原实现直接信任请求体中的 userId，任意登录用户可改他人在线状态。
 * 现强制 AuthContext.requireSelf(clientUserId)，客户端传入的 userId 必须与当前登录用户一致。
 *
 * 覆盖点：
 *   1. 本人更新：请求体 userId == 当前登录用户 → 更新成功并落库；
 *   2. 越权更新：请求体 userId != 当前登录用户 → 抛 TOKEN_INVALID，Service 不被调用。
 */
class UserControllerUpdateStatusTest {

    private UserOnlineStatusService userOnlineStatusService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userOnlineStatusService = mock(UserOnlineStatusService.class);
        userController = new UserController();
        ReflectionTestUtils.setField(userController, "userOnlineStatusService", userOnlineStatusService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        UserPrincipal principal = new UserPrincipal(userId, "user-" + userId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Map<String, Object> body(long userId, String status) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("status", status);
        return body;
    }

    @Test
    void updateUserStatus_本人更新应成功并落库() {
        authenticateAs(42L);
        when(userOnlineStatusService.updateStatus(eq(42L), eq("ONLINE")))
                .thenReturn(new com.example.aichat.entity.UserOnlineStatus());

        R<String> result = userController.updateUserStatus(body(42L, "ONLINE"));

        assertNotNull(result);
        verify(userOnlineStatusService).updateStatus(42L, "ONLINE");
    }

    @Test
    void updateUserStatus_越权更新他人应抛TOKEN_INVALID() {
        authenticateAs(42L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userController.updateUserStatus(body(999L, "ONLINE")));
        assertEquals(UserErrorCode.TOKEN_INVALID, ex.getErrorCode());
        verify(userOnlineStatusService, never()).updateStatus(anyLong(), anyString());
    }
}
