package com.example.aichat.service;

import com.example.aichat.entity.LoginDevice;
import com.example.aichat.entity.LoginAlert;
import com.example.aichat.entity.UserBadge;
import com.example.aichat.entity.UserTag;

import java.util.List;
import java.util.Map;

public interface AuthService {
    
    String sendSmsCode(String phone);
    
    boolean verifySmsCode(String phone, String code);
    
    Map<String, Object> loginByPhone(String phone, String code, String deviceId, String ipAddress);
    
    boolean enableMfa(Long userId, String secret);
    
    boolean verifyMfa(Long userId, String code);
    
    List<LoginDevice> getLoginDevices(Long userId);
    
    void trustDevice(Long userId, Long deviceId, boolean trusted);
    
    void logoutDevice(Long userId, Long deviceId);
    
    List<LoginAlert> getLoginAlerts(Long userId);
    
    void markAlertsAsRead(Long userId);
    
    void handleLogin(Long userId, String deviceId, String deviceName, String deviceType, 
                     String os, String browser, String ipAddress);
    
    List<UserBadge> getUserBadges(Long userId);
    
    void awardBadge(Long userId, String badgeId, String badgeName, String description);
    
    List<UserTag> getUserTags(Long userId);
    
    void addUserTag(Long userId, String tagName, String category);
    
    void removeUserTag(Long userId, String tagName);
    
    String exportUserData(Long userId, String exportType);
}