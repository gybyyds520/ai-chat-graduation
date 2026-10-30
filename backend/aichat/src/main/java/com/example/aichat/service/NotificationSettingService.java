package com.example.aichat.service;

import com.example.aichat.entity.NotificationSetting;

/**
 * 用户通知设置服务：读取 / 更新用户级通知开关。
 */
public interface NotificationSettingService {

    /**
     * 获取用户通知设置；不存在则返回默认值（全部开启）。
     */
    NotificationSetting getSettings(Long userId);

    /**
     * 插入或更新用户通知设置（upsert）。
     * 未传的布尔字段保持原值（已存在记录时）或默认 true（新建时）。
     */
    NotificationSetting updateSettings(Long userId,
                                      Boolean likeEnabled,
                                      Boolean commentEnabled,
                                      Boolean shareEnabled,
                                      Boolean systemEnabled);
}
