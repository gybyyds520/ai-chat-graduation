package com.example.aichat.service.voice.adapter;

import com.example.aichat.service.ai.OllamaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ollama LLM 生产扩展点。
 *
 * 通过现有 OllamaService 调用本地大模型。当未配置 Ollama 服务时 isAvailable() 返回 false，
 * VoiceAdapterManager 自动降级到 MockLlmAdapter。
 */
@Component
public class OllamaLlmAdapter implements LlmAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OllamaLlmAdapter.class);

    @Value("${voice.llm.ollama.enabled:false}")
    private boolean enabled;

    @Autowired(required = false)
    private OllamaService ollamaService;

    @Override
    public String generateReply(String userText, String systemPrompt, List<String> history) {
        if (!isAvailable() || ollamaService == null) {
            logger.warn("Ollama LLM 未启用，应降级到 Mock；本次调用返回 null");
            return null;
        }
        // Ollama 服务实际不可达时直接返回 null（由上层兜底简短回复），
        // 避免 chatStream 内部的长篇模拟回复（含环境配置提示）被 TTS 朗读
        try {
            if (!ollamaService.isOllamaAvailable()) {
                logger.warn("Ollama 服务不可达，语音 LLM 返回 null");
                return null;
            }
        } catch (Exception e) {
            logger.warn("Ollama 健康检查失败: {}", e.getMessage());
            return null;
        }
        try {
            // 拼最近对话历史（最多 8 条）+ 当前用户输入
            StringBuilder prompt = new StringBuilder();
            if (history != null && !history.isEmpty()) {
                int start = Math.max(0, history.size() - 8);
                for (int i = start; i < history.size(); i++) {
                    prompt.append(history.get(i)).append("\n");
                }
            }
            prompt.append("user: ").append(userText == null ? "" : userText);
            String sys = (systemPrompt == null || systemPrompt.isEmpty())
                    ? "你是一个友好的AI助手，请用简洁自然的语气回答。" : systemPrompt;
            // 聚合本地 Ollama 流式回复为单段文本
            StringBuilder sb = new StringBuilder();
            java.util.List<String> chunks = ollamaService.chatStream(null, prompt.toString(), sys)
                    .collectList()
                    .block(java.time.Duration.ofSeconds(90));
            if (chunks != null) {
                for (String c : chunks) {
                    if (c != null) sb.append(c);
                }
            }
            if (sb.length() == 0) {
                logger.warn("Ollama 回复为空");
                return null;
            }
            return sb.toString();
        } catch (Exception e) {
            logger.error("Ollama LLM 调用失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled && ollamaService != null;
    }

    @Override
    public String getName() {
        return "ollama-llm";
    }
}
