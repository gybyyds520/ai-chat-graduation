package com.example.aichat.service;

import java.util.List;
import java.util.Map;

public interface ConversationSummaryService {
    
    String generateSummary(String sessionId, Long userId, Long characterId);
    
    String getLatestSummary(String sessionId);
    
    List<Map<String, Object>> getSummariesByUserId(Long userId, int limit);
    
    List<String> extractTopics(String sessionId);
    
    void updateTopics(String sessionId, List<String> topics);
}