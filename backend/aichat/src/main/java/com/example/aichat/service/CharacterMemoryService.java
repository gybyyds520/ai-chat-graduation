package com.example.aichat.service;

import java.util.List;
import java.util.Map;

public interface CharacterMemoryService {
    
    void addMemory(Long characterId, Long userId, String key, String value, String type, double importance);
    
    String getMemory(Long characterId, String key);
    
    List<Map<String, Object>> getImportantMemories(Long characterId, int limit);
    
    void updateMemory(Long characterId, String key, String value);
    
    void removeMemory(Long characterId, String key);
    
    String buildMemoryPrompt(Long characterId);
    
    void extractMemoriesFromMessage(Long characterId, Long userId, String message);
}