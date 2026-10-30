package com.example.aichat.service;

import java.util.Map;

public interface EmotionAnalysisService {
    
    String analyzeEmotion(String text);
    
    double analyzeIntensity(String text, String emotionType);
    
    Map<String, Object> analyzeFull(String text);
    
    String getExpressionForEmotion(String emotionType);
    
    String getActionForEmotion(String emotionType);
}