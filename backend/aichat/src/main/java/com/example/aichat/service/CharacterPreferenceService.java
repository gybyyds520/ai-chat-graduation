package com.example.aichat.service;

import com.example.aichat.entity.CharacterPreference;

import java.util.List;
import java.util.Map;

public interface CharacterPreferenceService {
    
    CharacterPreference getPreferences(Long userId, Long characterId);
    
    CharacterPreference savePreferences(Long userId, Long characterId, String preferences);
    
    CharacterPreference updatePreferences(Long userId, Long characterId, Map<String, Object> updates);
    
    void recordUsage(Long userId, Long characterId);
    
    List<Long> getRecentCharacterIds(Long userId, int limit);
    
    boolean deletePreferences(Long userId, Long characterId);
    
    boolean deleteAllByUserId(Long userId);
    
    boolean deleteAllByCharacterId(Long characterId);
}