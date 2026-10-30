package com.example.aichat.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模型路由器
 * 根据模型名前缀路由到对应的ModelProvider
 * 模型名格式：provider:modelId（如 ollama:llama3.2:3b、openai:gpt-4o、deepseek:deepseek-chat）
 * 无前缀时默认使用ollama（向后兼容）
 */
@Service
public class ModelRouter {

    private static final Logger logger = LoggerFactory.getLogger(ModelRouter.class);
    private static final String DEFAULT_MODEL = "ollama:llama3.2:3b";

    private final Map<String, ModelProvider> providers;

    @Autowired
    public ModelRouter(List<ModelProvider> providerList) {
        // 按providerName建立索引
        this.providers = providerList.stream()
                .collect(Collectors.toMap(ModelProvider::getProviderName, p -> p));
        logger.info("ModelRouter初始化完成，已注册提供者: {}", providers.keySet());
    }

    /**
     * 流式对话（自动路由到对应Provider）
     * @param fullModelName 完整模型名（如 ollama:llama3.2:3b 或 openai:gpt-4o）
     * @param prompt 用户消息
     * @param systemPrompt 系统提示词
     */
    public Flux<String> chatStream(String fullModelName, String prompt, String systemPrompt) {
        String modelName = (fullModelName == null || fullModelName.isEmpty()) ? DEFAULT_MODEL : fullModelName;

        // 解析provider前缀和modelId
        int colonIndex = modelName.indexOf(':');
        String providerName;
        String modelId;

        if (colonIndex > 0 && providers.containsKey(modelName.substring(0, colonIndex))) {
            providerName = modelName.substring(0, colonIndex);
            modelId = modelName.substring(colonIndex + 1);
        } else if (modelName.startsWith("gpt-") || modelName.startsWith("deepseek")) {
            // 无前缀但匹配已知云模型名，走openai兼容
            providerName = "openai";
            modelId = modelName;
        } else {
            // 无前缀，默认ollama（向后兼容）
            providerName = "ollama";
            modelId = modelName;
        }

        ModelProvider provider = providers.get(providerName);
        if (provider == null) {
            logger.error("未找到模型提供者: {}, 可用: {}", providerName, providers.keySet());
            return Flux.just("⚠️ 不支持的模型提供者: " + providerName);
        }

        logger.info("路由模型请求 - provider: {}, modelId: {}", providerName, modelId);
        return provider.chatStream(modelId, prompt, systemPrompt);
    }

    /**
     * 获取所有已注册的提供者名称
     */
    public Map<String, ModelProvider> getProviders() {
        return providers;
    }
}
