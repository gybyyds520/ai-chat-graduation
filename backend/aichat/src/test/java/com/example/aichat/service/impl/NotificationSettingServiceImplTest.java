package com.example.aichat.service.impl;

import com.example.aichat.entity.NotificationSetting;
import com.example.aichat.mapper.NotificationSettingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NotificationSettingService 单元测试（Mockito 隔离）。
 *
 * 覆盖点：
 *   1. getSettings 新用户返回默认值（全部 true）；
 *   2. updateSettings 更新后 getSettings 返回新值；
 *   3. getSettings 已有设置返回正确值；
 *   4. updateSettings 部分字段更新（只改 likeEnabled 其他保持不变）。
 */
@ExtendWith(MockitoExtension.class)
class NotificationSettingServiceImplTest {

    @Mock
    private NotificationSettingMapper notificationSettingMapper;

    @InjectMocks
    private NotificationSettingServiceImpl notificationSettingService;

    @Test
    void getSettings_新用户应返回默认值全部开启() {
        when(notificationSettingMapper.selectByUserId(1L)).thenReturn(null);

        NotificationSetting settings = notificationSettingService.getSettings(1L);

        assertNotNull(settings);
        assertEquals(1L, settings.getUserId());
        assertTrue(settings.getLikeEnabled());
        assertTrue(settings.getCommentEnabled());
        assertTrue(settings.getShareEnabled());
        assertTrue(settings.getSystemEnabled());
    }

    @Test
    void getSettings_已有设置应返回正确值() {
        NotificationSetting existing = new NotificationSetting();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setLikeEnabled(false);
        existing.setCommentEnabled(true);
        existing.setShareEnabled(false);
        existing.setSystemEnabled(true);
        when(notificationSettingMapper.selectByUserId(1L)).thenReturn(existing);

        NotificationSetting settings = notificationSettingService.getSettings(1L);

        assertFalse(settings.getLikeEnabled());
        assertTrue(settings.getCommentEnabled());
        assertFalse(settings.getShareEnabled());
        assertTrue(settings.getSystemEnabled());
    }

    @Test
    void updateSettings_新用户应插入并返回新值() {
        when(notificationSettingMapper.selectByUserId(1L)).thenReturn(null);

        NotificationSetting result = notificationSettingService.updateSettings(
                1L, false, false, true, true);

        assertFalse(result.getLikeEnabled());
        assertFalse(result.getCommentEnabled());
        assertTrue(result.getShareEnabled());
        assertTrue(result.getSystemEnabled());
        verify(notificationSettingMapper).insert(any(NotificationSetting.class));
        verify(notificationSettingMapper, never()).updateById(any(NotificationSetting.class));
    }

    @Test
    void updateSettings_部分字段更新_其他保持不变() {
        NotificationSetting existing = new NotificationSetting();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setLikeEnabled(true);
        existing.setCommentEnabled(true);
        existing.setShareEnabled(true);
        existing.setSystemEnabled(true);
        when(notificationSettingMapper.selectByUserId(1L)).thenReturn(existing);

        // 只改 likeEnabled，其他传 null（保持原值）
        NotificationSetting result = notificationSettingService.updateSettings(
                1L, false, null, null, null);

        assertFalse(result.getLikeEnabled(), "likeEnabled 应被更新为 false");
        assertTrue(result.getCommentEnabled(), "commentEnabled 应保持原值 true");
        assertTrue(result.getShareEnabled(), "shareEnabled 应保持原值 true");
        assertTrue(result.getSystemEnabled(), "systemEnabled 应保持原值 true");
        verify(notificationSettingMapper).updateById(any(NotificationSetting.class));
        verify(notificationSettingMapper, never()).insert(any(NotificationSetting.class));
    }
}
