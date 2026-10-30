package com.example.aichat.service;

import com.example.aichat.entity.ChatImage;
import com.example.aichat.entity.ChatFile;

import java.util.List;
import java.util.Map;

public interface MultimodalService {
    
    ChatImage processImage(String sessionId, Long userId, Long characterId, String imageUrl);
    
    String performOCR(String imageUrl);
    
    String describeImage(String imageUrl);
    
    ChatFile processFile(String sessionId, Long userId, Long characterId, String fileName, String fileUrl, String fileType, Long fileSize);
    
    String extractTextFromFile(String fileUrl, String fileType);
    
    List<ChatImage> getImagesBySession(String sessionId);
    
    List<ChatFile> getFilesBySession(String sessionId);
}
