package com.example.aichat.service;

import java.util.List;
import java.util.Map;

public interface ContextCompressionService {
    
    String compressContext(List<Map<String, Object>> messages, int maxTokens);
    
    Map<String, Object> extractKeyPoints(List<Map<String, Object>> messages);
    
    String generateTopicSummary(List<Map<String, Object>> messages);
    
    List<Map<String, Object>> pruneContext(List<Map<String, Object>> messages, int targetSize);
}
