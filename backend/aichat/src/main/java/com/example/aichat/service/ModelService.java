package com.example.aichat.service;

import com.example.aichat.entity.AiModel;

import java.util.List;

public interface ModelService {
    
    List<AiModel> getAvailableModels();
    
    List<AiModel> getModelsByCategory(String category);
    
    AiModel getModelByKey(String modelKey);
    
    void addModel(AiModel model);
    
    void updateModel(Long id, AiModel model);
    
    void deleteModel(Long id);
    
    void toggleModelStatus(Long id, boolean available);
}