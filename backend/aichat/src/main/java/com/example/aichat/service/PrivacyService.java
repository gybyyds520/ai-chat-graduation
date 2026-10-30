package com.example.aichat.service;

import com.example.aichat.entity.PrivacySetting;

import java.util.List;
import java.util.Map;

public interface PrivacyService {
    
    PrivacySetting getSetting(Long userId, String key);
    
    Map<String, Object> getAllSettings(Long userId);
    
    PrivacySetting updateSetting(Long userId, String key, String value);
    
    void deleteSetting(Long userId, String key);
}