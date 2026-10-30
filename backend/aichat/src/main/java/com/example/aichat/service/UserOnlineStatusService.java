package com.example.aichat.service;

import com.example.aichat.entity.UserOnlineStatus;

import java.util.List;
import java.util.Map;

public interface UserOnlineStatusService {
    
    UserOnlineStatus getStatus(Long userId);
    
    UserOnlineStatus updateStatus(Long userId, String status);
    
    UserOnlineStatus updateSessionId(Long userId, String sessionId);
    
    void recordActivity(Long userId);
    
    List<UserOnlineStatus> getFriendStatuses(List<Long> friendIds);
    
    Map<Long, String> getFriendStatusMap(List<Long> friendIds);
    
    boolean isOnline(Long userId);
    
    int cleanupOfflineUsers();
    
    void broadcastStatus(Long userId);
}