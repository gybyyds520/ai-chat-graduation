package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.entity.Notification;
import com.example.aichat.mapper.NotificationMapper;
import com.example.aichat.service.NotificationService;
import com.example.aichat.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationMapper notificationMapper;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ObjectMapper objectMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   ChatWebSocketHandler chatWebSocketHandler,
                                   ObjectMapper objectMapper) {
        this.notificationMapper = notificationMapper;
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public Notification createNotification(Long userId, String type, String targetType, Long targetId, Long actorId, String actorName, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setActorId(actorId);
        notification.setActorName(actorName);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setIsDeleted(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationMapper.insert(notification);

        pushNotification(userId, notification);
        logger.info("[通知] 创建通知 - userId: {}, type: {}", userId, type);

        return notification;
    }

    @Override
    public IPage<Notification> listNotifications(Long userId, int page, int size, String type) {
        Page<Notification> pageParam = new Page<>(page, size);
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("is_deleted", false);
        if (type != null && !type.isBlank() && !"all".equalsIgnoreCase(type.trim())) {
            wrapper.eq("type", type.trim());
        }
        wrapper.orderByDesc("created_at");
        return notificationMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public int countUnread(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        // 安全加固（P0）：仅通知所有者可标记已读
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null) {
            com.example.aichat.common.security.AuthContext.requireSelf(notification.getUserId());
            notificationMapper.markAsRead(notificationId);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null) {
            // 安全加固（P0）：仅通知所有者可删除
            com.example.aichat.common.security.AuthContext.requireSelf(notification.getUserId());
            notification.setIsDeleted(true);
            notificationMapper.updateById(notification);
        }
    }

    @Override
    public int clearAllNotifications(Long userId) {
        int deleted = notificationMapper.clearAllByUserId(userId);
        logger.info("[通知] 清空全部通知 - userId: {}, deletedCount: {}", userId, deleted);
        return deleted;
    }

    @Override
    public void pushNotification(Long userId, Notification notification) {
        try {
            Map<String, Object> notificationMsg = Map.ofEntries(
                Map.entry("type", "notification"),
                Map.entry("id", notification.getId()),
                Map.entry("userId", userId),
                Map.entry("notificationType", notification.getType()),
                Map.entry("targetType", notification.getTargetType()),
                Map.entry("targetId", notification.getTargetId()),
                Map.entry("actorId", notification.getActorId()),
                Map.entry("actorName", notification.getActorName()),
                Map.entry("message", notification.getMessage()),
                Map.entry("isRead", notification.getIsRead()),
                Map.entry("createdAt", notification.getCreatedAt().toString())
            );
            String json = objectMapper.writeValueAsString(notificationMsg);
            chatWebSocketHandler.sendMessageToUser(userId, json);
        } catch (Exception e) {
            logger.error("推送通知失败", e);
        }
    }
}
