package com.example.aichat.service.impl;

import com.example.aichat.service.VisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 视觉理解服务实现：通过 Ollama {@code /api/generate} 的 {@code images} 参数调用 llava。
 *
 * <p>全部免费开源（Ollama + llava 模型），不引入付费依赖。
 * Ollama 不可用或模型未拉取时优雅降级，不抛异常打断聊天流程。</p>
 */
@Service
public class VisionServiceImpl implements VisionService {

    private static final Logger logger = LoggerFactory.getLogger(VisionServiceImpl.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final WebClient webClient;

    @Value("${aichat.vision.enabled:false}")
    private boolean enabled;

    @Value("${aichat.vision.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${aichat.vision.model:llava}")
    private String model;

    public VisionServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public VisionAnalysis analyze(byte[] imageBytes, String filename, String prompt) {
        if (!enabled) {
            return new VisionAnalysis(false, false, "", "视觉理解未启用（aichat.vision.enabled=false），保持离线基线");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return new VisionAnalysis(true, false, "", "图片内容为空");
        }

        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        String actualPrompt = (prompt == null || prompt.isBlank())
                ? "请用简洁中文描述这张图片的主要内容、场景和关键物体。"
                : prompt;

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("prompt", actualPrompt);
        body.put("images", new String[]{imageBase64});
        body.put("stream", false);
        body.put("options", Map.of("temperature", 0.2));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = webClient.post()
                    .uri(baseUrl + "/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(REQUEST_TIMEOUT);

            if (resp == null) {
                return new VisionAnalysis(true, false, "", "Ollama 无响应");
            }
            Object error = resp.get("error");
            if (error != null) {
                logger.warn("[视觉理解] Ollama 返回错误: {}", error);
                return new VisionAnalysis(true, false, "", "模型调用失败: " + error
                        + "（如模型缺失请执行: ollama pull " + model + "）");
            }
            Object description = resp.get("response");
            String text = description == null ? "" : description.toString().trim();
            if (text.isEmpty()) {
                return new VisionAnalysis(true, false, "", "模型返回空描述");
            }
            logger.info("[视觉理解] 图片 {} 分析完成，描述长度={}", filename, text.length());
            return new VisionAnalysis(true, true, text, "ok");
        } catch (Exception e) {
            logger.warn("[视觉理解] 调用 Ollama 失败: {}", e.getMessage());
            return new VisionAnalysis(true, false, "",
                    "Ollama 服务不可用或模型未就绪，请确认 Ollama 已启动并已执行 ollama pull " + model);
        }
    }
}
