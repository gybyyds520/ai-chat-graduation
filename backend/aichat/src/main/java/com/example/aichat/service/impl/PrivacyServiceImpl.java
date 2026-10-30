package com.example.aichat.service.impl;

import com.example.aichat.entity.PrivacySetting;
import com.example.aichat.mapper.PrivacySettingMapper;
import com.example.aichat.service.PrivacyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrivacyServiceImpl implements PrivacyService {

    private static final Logger logger = LoggerFactory.getLogger(PrivacyServiceImpl.class);
    
    private final PrivacySettingMapper privacySettingMapper;
    private final ObjectMapper objectMapper;

    public PrivacyServiceImpl(PrivacySettingMapper privacySettingMapper, ObjectMapper objectMapper) {
        this.privacySettingMapper = privacySettingMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PrivacySetting getSetting(Long userId, String key) {
        return privacySettingMapper.findByUserAndKey(userId, key);
    }

    @Override
    public Map<String, Object> getAllSettings(Long userId) {
        List<PrivacySetting> settings = privacySettingMapper.getSettingsByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        
        for (PrivacySetting setting : settings) {
            try {
                result.put(setting.getKey(), objectMapper.readValue(setting.getValue(), new TypeReference<Object>() {}));
            } catch (Exception e) {
                result.put(setting.getKey(), setting.getValue());
            }
        }
        
        return result;
    }

    @Override
    public PrivacySetting updateSetting(Long userId, String key, String value) {
        PrivacySetting existing = privacySettingMapper.findByUserAndKey(userId, key);
        
        if (existing != null) {
            existing.setValue(value);
            existing.setUpdatedAt(LocalDateTime.now());
            privacySettingMapper.updateById(existing);
            return existing;
        }
        
        PrivacySetting setting = new PrivacySetting();
        setting.setUserId(userId);
        setting.setKey(key);
        setting.setValue(value);
        setting.setCreatedAt(LocalDateTime.now());
        setting.setUpdatedAt(LocalDateTime.now());
        
        privacySettingMapper.insert(setting);
        logger.info("[隐私设置] 用户 {} 更新设置: {}", userId, key);
        return setting;
    }

    @Override
    public void deleteSetting(Long userId, String key) {
        PrivacySetting setting = privacySettingMapper.findByUserAndKey(userId, key);
        if (setting != null) {
            privacySettingMapper.deleteById(setting.getId());
        }
    }
}