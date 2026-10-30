package com.example.aichat.service;

import java.util.Map;

public interface ConversationContextService {
    
    void setContext(String sessionId, Long userId, Long characterId, String key, String value);
    
    String getContext(String sessionId, String key);
    
    Map<String, String> getAllContext(String sessionId);
    
    void removeContext(String sessionId, String key);
    
    void clearSessionContext(String sessionId);
    
    String buildContextPrompt(String sessionId);
}