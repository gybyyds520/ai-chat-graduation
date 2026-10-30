package com.example.aichat.service.impl;

import com.example.aichat.entity.NotificationSetting;
import com.example.aichat.mapper.NotificationSettingMapper;
import com.example.aichat.service.NotificationSettingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationSettingServiceImpl implements NotificationSettingService {

    private final NotificationSettingMapper notificationSettingMapper;

    public NotificationSettingServiceImpl(NotificationSettingMapper notificationSettingMapper) {
        this.notificationSettingMapper = notificationSettingMapper;
    }

    @Override
    public NotificationSetting getSettings(Long userId) {
        NotificationSetting existing = notificationSettingMapper.selectByUserId(userId);
        if (existing != null) {
            return existing;
        }
        // 默认值：全部开启
        NotificationSetting defaults = new NotificationSetting();
        defaults.setUserId(userId);
        defaults.setLikeEnabled(Boolean.TRUE);
        defaults.setCommentEnabled(Boolean.TRUE);
        defaults.setShareEnabled(Boolean.TRUE);
        defaults.setSystemEnabled(Boolean.TRUE);
        return defaults;
    }

    @Override
    public NotificationSetting updateSettings(Long userId,
                                              Boolean likeEnabled,
                                              Boolean commentEnabled,
                                              Boolean shareEnabled,
                                              Boolean systemEnabled) {
        NotificationSetting existing = notificationSettingMapper.selectByUserId(userId);
        if (existing == null) {
            NotificationSetting settings = new NotificationSetting();
            settings.setUserId(userId);
            settings.setLikeEnabled(likeEnabled != null ? likeEnabled : Boolean.TRUE);
            settings.setCommentEnabled(commentEnabled != null ? commentEnabled : Boolean.TRUE);
            settings.setShareEnabled(shareEnabled != null ? shareEnabled : Boolean.TRUE);
            settings.setSystemEnabled(systemEnabled != null ? systemEnabled : Boolean.TRUE);
            settings.setUpdatedAt(LocalDateTime.now());
            notificationSettingMapper.insert(settings);
            return settings;
        }
        // 部分字段更新：null 表示保持原值
        if (likeEnabled != null) {
            existing.setLikeEnabled(likeEnabled);
        }
        if (commentEnabled != null) {
            existing.setCommentEnabled(commentEnabled);
        }
        if (shareEnabled != null) {
            existing.setShareEnabled(shareEnabled);
        }
        if (systemEnabled != null) {
            existing.setSystemEnabled(systemEnabled);
        }
        existing.setUpdatedAt(LocalDateTime.now());
        notificationSettingMapper.updateById(existing);
        return existing;
    }
}
