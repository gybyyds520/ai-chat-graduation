package com.example.aichat.service.impl;

import com.example.aichat.entity.AiModel;
import com.example.aichat.mapper.AiModelMapper;
import com.example.aichat.service.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModelServiceImpl implements ModelService {

    private static final Logger logger = LoggerFactory.getLogger(ModelServiceImpl.class);
    
    private final AiModelMapper aiModelMapper;

    public ModelServiceImpl(AiModelMapper aiModelMapper) {
        this.aiModelMapper = aiModelMapper;
        initDefaultModels();
    }

    private void initDefaultModels() {
        if (aiModelMapper.selectCount(null) == 0) {
            AiModel model1 = new AiModel();
            model1.setModelName("Llama 3.2 3B");
            model1.setModelKey("llama3.2:3b");
            model1.setProvider("Ollama");
            model1.setDescription("轻量级开源模型，适合日常对话");
            model1.setCostPerToken(0.0);
            model1.setMaxTokens(4096);
            model1.setAvailable(true);
            model1.setCategory("chat");
            model1.setPriority(1);
            model1.setCreatedAt(LocalDateTime.now());
            aiModelMapper.insert(model1);

            AiModel model2 = new AiModel();
            model2.setModelName("Llama 3.2 7B");
            model2.setModelKey("llama3.2:7b");
            model2.setProvider("Ollama");
            model2.setDescription("平衡性能与速度的开源模型");
            model2.setCostPerToken(0.0);
            model2.setMaxTokens(8192);
            model2.setAvailable(true);
            model2.setCategory("chat");
            model2.setPriority(2);
            model2.setCreatedAt(LocalDateTime.now());
            aiModelMapper.insert(model2);

            AiModel model3 = new AiModel();
            model3.setModelName("Mistral");
            model3.setModelKey("mistral");
            model3.setProvider("Ollama");
            model3.setDescription("高性能开源模型，擅长推理");
            model3.setCostPerToken(0.0);
            model3.setMaxTokens(8192);
            model3.setAvailable(true);
            model3.setCategory("chat");
            model3.setPriority(3);
            model3.setCreatedAt(LocalDateTime.now());
            aiModelMapper.insert(model3);

            AiModel model4 = new AiModel();
            model4.setModelName("Phi 3");
            model4.setModelKey("phi3");
            model4.setProvider("Ollama");
            model4.setDescription("微软出品的小型高效模型");
            model4.setCostPerToken(0.0);
            model4.setMaxTokens(4096);
            model4.setAvailable(true);
            model4.setCategory("chat");
            model4.setPriority(4);
            model4.setCreatedAt(LocalDateTime.now());
            aiModelMapper.insert(model4);

            logger.info("[模型服务] 初始化默认模型完成");
        }
    }

    @Override
    public List<AiModel> getAvailableModels() {
        return aiModelMapper.getAvailableModels();
    }

    @Override
    public List<AiModel> getModelsByCategory(String category) {
        return aiModelMapper.getModelsByCategory(category);
    }

    @Override
    public AiModel getModelByKey(String modelKey) {
        return aiModelMapper.findByModelKey(modelKey);
    }

    @Override
    public void addModel(AiModel model) {
        model.setCreatedAt(LocalDateTime.now());
        aiModelMapper.insert(model);
        logger.info("[模型服务] 添加模型: {}", model.getModelName());
    }

    @Override
    public void updateModel(Long id, AiModel model) {
        AiModel existing = aiModelMapper.selectById(id);
        if (existing != null) {
            existing.setModelName(model.getModelName());
            existing.setDescription(model.getDescription());
            existing.setCostPerToken(model.getCostPerToken());
            existing.setMaxTokens(model.getMaxTokens());
            existing.setCategory(model.getCategory());
            existing.setPriority(model.getPriority());
            aiModelMapper.updateById(existing);
        }
    }

    @Override
    public void deleteModel(Long id) {
        aiModelMapper.deleteById(id);
    }

    @Override
    public void toggleModelStatus(Long id, boolean available) {
        AiModel model = aiModelMapper.selectById(id);
        if (model != null) {
            model.setAvailable(available);
            aiModelMapper.updateById(model);
        }
    }
}