package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.dto.R;
import com.example.aichat.entity.ElfReminder;
import com.example.aichat.service.AiElfService;
import com.example.aichat.service.ElfReminderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * M1 ②：POST /api/elf/reminder、GET /api/elf/reminders 控制器单测。
 */
@ExtendWith(MockitoExtension.class)
class AiElfReminderControllerTest {

    private static final Long CURRENT_USER = 100L;

    @Mock
    private AiElfService aiElfService;

    @Mock
    private ElfReminderService elfReminderService;

    private AiElfController controller;

    @BeforeEach
    void setUp() {
        controller = new AiElfController();
        ReflectionTestUtils.setField(controller, "aiElfService", aiElfService);
        ReflectionTestUtils.setField(controller, "elfReminderService", elfReminderService);
        UserPrincipal principal = new UserPrincipal(CURRENT_USER, "u", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReminder_合法时间_成功创建() {
        String future = LocalDateTime.now().plusHours(1).toString();
        ElfReminder saved = new ElfReminder();
        saved.setId(7L);
        when(elfReminderService.createReminder(eq(CURRENT_USER), eq("喝水"), eq("该喝水了"), any()))
                .thenReturn(saved);

        R<?> r = controller.createReminder(Map.of("title", "喝水", "content", "该喝水了", "remindTime", future));
        assertEquals(200, r.getCode());
    }

    @Test
    void createReminder_过去时间_拒绝() {
        String past = LocalDateTime.now().minusHours(1).toString();
        R<?> r = controller.createReminder(Map.of("title", "t", "remindTime", past));
        assertEquals(400, r.getCode());
    }

    @Test
    void createReminder_缺少字段_抛业务异常() {
        assertThrows(Exception.class,
                () -> controller.createReminder(Map.of("title", "t")));
    }

    @Test
    void listReminders_返回本人列表() {
        when(elfReminderService.listReminders(CURRENT_USER)).thenReturn(List.of(new ElfReminder()));
        R<?> r = controller.listReminders();
        assertEquals(200, r.getCode());
    }

    @Test
    void requireUserId_已登录_可用() {
        assertEquals(CURRENT_USER, AuthContext.requireUserId());
    }
}
