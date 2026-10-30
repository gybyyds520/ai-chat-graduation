package com.example.aichat.controller;

import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.dto.R;
import com.example.aichat.service.QuotaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * QuotaController 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class QuotaControllerTest {

    @Mock
    private QuotaService quotaService;

    @InjectMocks
    private QuotaController controller;

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
    void current_应返回当前用户配额() {
        when(quotaService.getCurrentQuota(42L)).thenReturn(Map.of("membershipLevel", "FREE"));
        R<Map<String, Object>> resp = controller.current();
        assertEquals(200, resp.getCode());
        assertEquals("FREE", resp.getData().get("membershipLevel"));
    }

    @Test
    void reset_应重置当前用户配额() {
        R resp = controller.reset();
        assertEquals(200, resp.getCode());
        verify(quotaService).resetDailyQuotas(42L);
    }
}
