package com.example.aichat.service.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Ollama模型提供者 - 适配器，包装现有OllamaService实现ModelProvider接口
 */
@Service
public class OllamaModelProvider implements ModelProvider {

    @Autowired
    private OllamaService ollamaService;

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public Flux<String> chatStream(String modelId, String prompt, String systemPrompt) {
        // 直接委托给现有OllamaService，保持向后兼容
        return ollamaService.chatStream(modelId, prompt, systemPrompt);
    }

    @Override
    public boolean isAvailable() {
        return ollamaService.isOllamaAvailable();
    }
}
