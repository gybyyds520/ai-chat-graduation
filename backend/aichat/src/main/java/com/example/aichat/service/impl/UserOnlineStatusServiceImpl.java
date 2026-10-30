package com.example.aichat.service.impl;

import com.example.aichat.entity.UserOnlineStatus;
import com.example.aichat.mapper.UserOnlineStatusMapper;
import com.example.aichat.service.UserOnlineStatusService;
import com.example.aichat.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserOnlineStatusServiceImpl implements UserOnlineStatusService {

    private static final Logger logger = LoggerFactory.getLogger(UserOnlineStatusServiceImpl.class);
    
    @Autowired
    private UserOnlineStatusMapper userOnlineStatusMapper;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private ChatWebSocketHandler chatWebSocketHandler;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private ChatWebSocketHandler getChatWebSocketHandler() {
        if (chatWebSocketHandler == null) {
            chatWebSocketHandler = applicationContext.getBean(ChatWebSocketHandler.class);
        }
        return chatWebSocketHandler;
    }

    @Override
    public UserOnlineStatus getStatus(Long userId) {
        return userOnlineStatusMapper.findByUserId(userId);
    }

    @Override
    @Transactional
    public UserOnlineStatus updateStatus(Long userId, String status) {
        UserOnlineStatus existing = userOnlineStatusMapper.findByUserId(userId);
        
        if (existing != null) {
            existing.setStatus(status);
            existing.setLastActiveTime(LocalDateTime.now());
            userOnlineStatusMapper.updateById(existing);
            broadcastStatus(userId);
            return existing;
        }
        
        UserOnlineStatus newStatus = new UserOnlineStatus();
        newStatus.setUserId(userId);
        newStatus.setStatus(status);
        newStatus.setLastActiveTime(LocalDateTime.now());
        newStatus.setCreatedAt(LocalDateTime.now());
        newStatus.setUpdatedAt(LocalDateTime.now());
        
        userOnlineStatusMapper.insert(newStatus);
        broadcastStatus(userId);
        return newStatus;
    }

    @Override
    @Transactional
    public UserOnlineStatus updateSessionId(Long userId, String sessionId) {
        UserOnlineStatus existing = userOnlineStatusMapper.findByUserId(userId);
        
        if (existing != null) {
            existing.setSessionId(sessionId);
            existing.setStatus("online");
            existing.setLastActiveTime(LocalDateTime.now());
            userOnlineStatusMapper.updateById(existing);
            broadcastStatus(userId);
            return existing;
        }
        
        UserOnlineStatus newStatus = new UserOnlineStatus();
        newStatus.setUserId(userId);
        newStatus.setStatus("online");
        newStatus.setSessionId(sessionId);
        newStatus.setLastActiveTime(LocalDateTime.now());
        newStatus.setCreatedAt(LocalDateTime.now());
        newStatus.setUpdatedAt(LocalDateTime.now());
        
        userOnlineStatusMapper.insert(newStatus);
        broadcastStatus(userId);
        return newStatus;
    }

    @Override
    @Transactional
    public void recordActivity(Long userId) {
        UserOnlineStatus existing = userOnlineStatusMapper.findByUserId(userId);
        
        if (existing != null) {
            existing.setLastActiveTime(LocalDateTime.now());
            if (!"online".equals(existing.getStatus())) {
                existing.setStatus("online");
            }
            userOnlineStatusMapper.updateById(existing);
        }
    }

    @Override
    public List<UserOnlineStatus> getFriendStatuses(List<Long> friendIds) {
        if (friendIds == null || friendIds.isEmpty()) {
            return List.of();
        }
        return userOnlineStatusMapper.findByUserIds(friendIds);
    }

    @Override
    public Map<Long, String> getFriendStatusMap(List<Long> friendIds) {
        Map<Long, String> statusMap = new HashMap<>();
        
        if (friendIds == null || friendIds.isEmpty()) {
            return statusMap;
        }
        
        List<UserOnlineStatus> statuses = userOnlineStatusMapper.findByUserIds(friendIds);
        for (UserOnlineStatus status : statuses) {
            statusMap.put(status.getUserId(), status.getStatus());
        }
        
        for (Long friendId : friendIds) {
            if (!statusMap.containsKey(friendId)) {
                statusMap.put(friendId, "offline");
            }
        }
        
        return statusMap;
    }

    @Override
    public boolean isOnline(Long userId) {
        UserOnlineStatus status = userOnlineStatusMapper.findByUserId(userId);
        return status != null && "online".equals(status.getStatus());
    }

    @Override
    @Transactional
    public int cleanupOfflineUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        return userOnlineStatusMapper.updateOfflineByInactivity(threshold);
    }

    @Override
    public void broadcastStatus(Long userId) {
        UserOnlineStatus status = userOnlineStatusMapper.findByUserId(userId);
        if (status == null) return;
        
        try {
            Map<String, Object> statusMsg = Map.of(
                "type", "user_status",
                "userId", userId,
                "status", status.getStatus(),
                "timestamp", System.currentTimeMillis()
            );
            String json = objectMapper.writeValueAsString(statusMsg);
            getChatWebSocketHandler().broadcastToFriends(userId, json);
        } catch (Exception e) {
            logger.error("广播用户状态失败", e);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void scheduledCleanup() {
        int count = cleanupOfflineUsers();
        if (count > 0) {
            logger.info("清理离线用户状态: {} 个", count);
        }
    }
}