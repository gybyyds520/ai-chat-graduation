package com.example.aichat.service.ai;

import reactor.core.publisher.Flux;

/**
 * AI模型提供者接口
 * 统一抽象不同AI服务商（Ollama/OpenAI/DeepSeek等）的调用方式
 * 模型名格式：provider:modelId（如 ollama:llama3.2:3b、openai:gpt-4o、deepseek:deepseek-chat）
 */
public interface ModelProvider {

    /**
     * 获取提供者标识（如 "ollama"、"openai"、"deepseek"）
     */
    String getProviderName();

    /**
     * 流式对话
     * @param modelId 模型ID（不含provider前缀）
     * @param prompt 用户消息
     * @param systemPrompt 系统提示词
     * @return 流式响应token
     */
    Flux<String> chatStream(String modelId, String prompt, String systemPrompt);

    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
}
