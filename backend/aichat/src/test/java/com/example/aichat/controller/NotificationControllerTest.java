package com.example.aichat.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.dto.R;
import com.example.aichat.entity.Notification;
import com.example.aichat.entity.NotificationSetting;
import com.example.aichat.service.NotificationService;
import com.example.aichat.service.NotificationSettingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationController 单元测试（Mockito 隔离）。
 *
 * 覆盖点：
 *   1. GET /list 分页+类型筛选参数正确传递，hasMore 计算正确；
 *   2. POST /clear-all 正确调用 service 并返回 deletedCount；
 *   3. GET /settings 返回用户通知设置；
 *   4. PUT /settings 调用 service 更新设置。
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationSettingService notificationSettingService;

    @InjectMocks
    private NotificationController controller;

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

    private Notification sampleNotification(Long id) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(42L);
        n.setType("like");
        n.setMessage("点赞了你的动态");
        n.setIsRead(false);
        n.setIsDeleted(false);
        return n;
    }

    @Test
    @SuppressWarnings("unchecked")
    void list_应传递分页和类型参数并计算hasMore() {
        Page<Notification> page = new Page<>(1, 20);
        page.setRecords(List.of(sampleNotification(1L), sampleNotification(2L)));
        page.setTotal(25); // 25/20 = 2 页
        when(notificationService.listNotifications(eq(42L), eq(1), eq(20), eq("like"))).thenReturn(page);

        R<Map<String, Object>> resp = controller.getNotifications(42L, 1, 20, "like");

        assertEquals(200, resp.getCode());
        Map<String, Object> data = resp.getData();
        List<Notification> records = (List<Notification>) data.get("records");
        assertEquals(2, records.size());
        assertEquals(25L, data.get("total"));
        assertEquals(1L, data.get("page"));
        assertEquals(20L, data.get("size"));
        assertTrue((Boolean) data.get("hasMore"), "第1页（共2页）应 hasMore=true");
        verify(notificationService).listNotifications(42L, 1, 20, "like");
    }

    @Test
    void clearAll_应调用service并返回deletedCount() {
        when(notificationService.clearAllNotifications(42L)).thenReturn(10);

        R<Map<String, Object>> resp = controller.clearAll(42L);

        assertEquals(200, resp.getCode());
        Map<String, Object> data = resp.getData();
        assertEquals(true, data.get("success"));
        assertEquals(10, data.get("deletedCount"));
        verify(notificationService).clearAllNotifications(42L);
    }

    @Test
    void getSettings_应返回用户通知设置() {
        NotificationSetting setting = new NotificationSetting();
        setting.setUserId(42L);
        setting.setLikeEnabled(true);
        setting.setCommentEnabled(false);
        setting.setShareEnabled(true);
        setting.setSystemEnabled(false);
        when(notificationSettingService.getSettings(42L)).thenReturn(setting);

        R<Map<String, Object>> resp = controller.getSettings(42L);

        assertEquals(200, resp.getCode());
        Map<String, Object> data = resp.getData();
        assertEquals(true, data.get("likeEnabled"));
        assertEquals(false, data.get("commentEnabled"));
        assertEquals(true, data.get("shareEnabled"));
        assertEquals(false, data.get("systemEnabled"));
    }

    @Test
    void updateSettings_应调用service更新设置() {
        NotificationSetting updated = new NotificationSetting();
        updated.setUserId(42L);
        updated.setLikeEnabled(false);
        updated.setCommentEnabled(true);
        updated.setShareEnabled(true);
        updated.setSystemEnabled(true);
        when(notificationSettingService.updateSettings(eq(42L), eq(false), any(), any(), any()))
                .thenReturn(updated);

        Map<String, Object> body = new HashMap<>();
        body.put("likeEnabled", false);
        R<Map<String, Object>> resp = controller.updateSettings(42L, body);

        assertEquals(200, resp.getCode());
        assertEquals(false, resp.getData().get("likeEnabled"));
        verify(notificationSettingService).updateSettings(eq(42L), eq(false), any(), any(), any());
    }
}
