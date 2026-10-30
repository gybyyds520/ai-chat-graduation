package com.example.aichat.service;

import com.example.aichat.entity.User;
import com.example.aichat.entity.UserStatistics;

import java.util.Map;

public interface ProfileService {
    
    User getProfile(Long userId);
    
    User updateProfile(Long userId, Map<String, Object> updates);
    
    String uploadAvatar(Long userId, String filePath);
    
    UserStatistics getStatistics(Long userId);
    
    UserStatistics incrementStatistic(Long userId, String statType);
    
    UserStatistics initializeStatistics(Long userId);
}