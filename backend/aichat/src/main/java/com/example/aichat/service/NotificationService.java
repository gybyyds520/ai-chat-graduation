package com.example.aichat.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.aichat.entity.Notification;

public interface NotificationService {

    Notification createNotification(Long userId, String type, String targetType, Long targetId, Long actorId, String actorName, String message);

    /**
     * 分页查询当前用户未删除通知，支持按类型筛选。
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @param type   通知类型（like/comment/share/system），为空或 "all" 则不过滤
     * @return 分页结果（records/total/current/size）
     */
    IPage<Notification> listNotifications(Long userId, int page, int size, String type);

    int countUnread(Long userId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long notificationId);

    /**
     * 批量软删除当前用户所有未删除通知。
     *
     * @param userId 用户 ID
     * @return 实际删除（软删除）的条数
     */
    int clearAllNotifications(Long userId);

    void pushNotification(Long userId, Notification notification);
}
