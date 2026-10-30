package com.example.aichat.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI兼容模型提供者
 * 支持所有使用OpenAI兼容API格式的服务商（OpenAI/DeepSeek/豆包等）
 * 模型名格式：provider:modelId（如 openai:gpt-4o、deepseek:deepseek-chat）
 */
@Service
public class OpenAICompatibleModelProvider implements ModelProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleModelProvider.class);

    private final WebClient webClient;

    // 各服务商API Key从环境变量读取，避免硬编码
    @Value("${external.api.providers.openai.api-key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;

    @Value("${external.api.providers.deepseek.api-key:${DEEPSEEK_API_KEY:}}")
    private String deepseekApiKey;

    public OpenAICompatibleModelProvider(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public Flux<String> chatStream(String modelId, String prompt, String systemPrompt) {
        // 根据模型ID前缀确定服务商配置
        ProviderConfig config = resolveProvider(modelId);
        if (config == null || config.apiKey == null || config.apiKey.isEmpty()) {
            String providerName = config != null ? config.providerName : "unknown";
            logger.warn("模型 {} 对应的API Key未配置，无法调用云端模型", modelId);
            return Flux.just("⚠️ 云端模型未配置API Key，请在环境变量中设置 " + providerName.toUpperCase() + "_API_KEY");
        }

        String safePrompt = prompt == null ? "" : prompt;
        logger.info("发起{}请求 - model: {}, prompt长度: {}", config.providerName, modelId, safePrompt.length());

        // 构建OpenAI兼容格式的请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("stream", true);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2048);

        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", safePrompt));
        requestBody.put("messages", messages);

        return webClient.post()
                .uri(config.baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + config.apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line != null && !line.isEmpty() && !"[DONE]".equals(line.trim()))
                .map(this::extractContent)
                .filter(content -> content != null && !content.isEmpty())
                .onErrorResume(e -> {
                    logger.error("{}调用失败: {}", config.providerName, e.getMessage());
                    return Flux.just("⚠️ AI服务调用失败: " + e.getMessage());
                });
    }

    @Override
    public boolean isAvailable() {
        // 只要任一API Key配置了即为可用
        return (openaiApiKey != null && !openaiApiKey.isEmpty())
                || (deepseekApiKey != null && !deepseekApiKey.isEmpty());
    }

    /**
     * 根据模型ID解析服务商配置
     */
    private ProviderConfig resolveProvider(String modelId) {
        if (modelId == null) return null;
        String lower = modelId.toLowerCase();

        if (lower.startsWith("gpt-") || lower.startsWith("openai:")) {
            String actualModel = lower.startsWith("openai:") ? lower.substring(7) : modelId;
            return new ProviderConfig("openai", "https://api.openai.com/v1", actualModel, openaiApiKey);
        }
        if (lower.startsWith("deepseek")) {
            return new ProviderConfig("deepseek", "https://api.deepseek.com/v1", modelId, deepseekApiKey);
        }
        // 默认按OpenAI格式处理
        return new ProviderConfig("openai", "https://api.openai.com/v1", modelId, openaiApiKey);
    }

    /**
     * 从OpenAI SSE响应中提取content内容
     */
    @SuppressWarnings("unchecked")
    private String extractContent(String sseData) {
        try {
            // 解析 {"choices":[{"delta":{"content":"xxx"}}]} 格式
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> response = mapper.readValue(sseData, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                if (delta != null) {
                    Object content = delta.get("content");
                    return content != null ? (String) content : "";
                }
            }
        } catch (Exception e) {
            logger.debug("解析SSE数据失败: {}", sseData);
        }
        return "";
    }

    private record ProviderConfig(String providerName, String baseUrl, String modelId, String apiKey) {}
}
