package com.example.aichat.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AI图像生成服务
 * 支持Stable Diffusion WebUI(本地)和OpenAI DALL-E(云端)两种提供者
 * 当服务不可用时优雅降级，返回提示信息
 */
@Service
public class ImageGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ImageGenerationService.class);

    private final WebClient webClient;

    @Value("${image.generation.provider:stable-diffusion}")
    private String provider;

    @Value("${image.generation.sd-api-url:http://localhost:7860}")
    private String sdApiUrl;

    @Value("${image.generation.openai-api-key:}")
    private String openaiApiKey;

    @Value("${image.generation.default-width:512}")
    private int defaultWidth;

    @Value("${image.generation.default-height:512}")
    private int defaultHeight;

    @Value("${image.generation.default-steps:20}")
    private int defaultSteps;

    @Value("${image.upload-dir:./uploads/generated/}")
    private String uploadDir;

    public ImageGenerationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 检测用户消息是否包含图像生成意图
     */
    public boolean isImageGenerationRequest(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("画") || lower.contains("生成图片") || lower.contains("生成图像")
                || lower.contains("draw") || lower.contains("generate image")
                || lower.contains("create image") || lower.contains("画一张")
                || lower.contains("画一个") || lower.contains("画只");
    }

    /**
     * 从用户消息中提取图像生成prompt
     */
    public String extractImagePrompt(String message) {
        // 移除指令性词汇，保留描述部分
        String prompt = message.replaceAll("(?i)(画一张|画一个|画只|画个|画|生成图片|生成图像|帮我画|请画|draw me|generate image|create image|draw|generate)", "").trim();
        return prompt.isEmpty() ? message : prompt;
    }

    /**
     * 生成图片
     * @return 包含imageUrl和status的结果Map，失败时status为error
     */
    public Map<String, Object> generateImage(String prompt) {
        Map<String, Object> result = new HashMap<>();

        if ("disabled".equals(provider)) {
            result.put("status", "disabled");
            result.put("message", "图像生成功能未启用");
            return result;
        }

        try {
            String base64Image;
            if ("openai-dall-e".equals(provider)) {
                base64Image = generateWithDallE(prompt);
            } else {
                base64Image = generateWithStableDiffusion(prompt);
            }

            if (base64Image == null || base64Image.isEmpty()) {
                result.put("status", "error");
                result.put("message", "图像生成失败，请检查服务是否可用");
                return result;
            }

            // 保存图片到本地
            String imageUrl = saveImage(base64Image, prompt);
            result.put("status", "success");
            result.put("imageUrl", imageUrl);
            result.put("prompt", prompt);
            logger.info("图像生成成功, prompt={}, url={}", prompt, imageUrl);
            return result;
        } catch (Exception e) {
            logger.error("图像生成失败: {}", e.getMessage());
            result.put("status", "error");
            result.put("message", "图像生成失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 使用Stable Diffusion WebUI生成图片
     */
    @SuppressWarnings("unchecked")
    private String generateWithStableDiffusion(String prompt) {
        logger.info("使用Stable Diffusion生成图片, prompt={}", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", prompt);
        requestBody.put("negative_prompt", "low quality, blurry, distorted");
        requestBody.put("steps", defaultSteps);
        requestBody.put("width", defaultWidth);
        requestBody.put("height", defaultHeight);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(sdApiUrl + "/sdapi/v1/txt2img")
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("images")) {
                java.util.List<String> images = (java.util.List<String>) response.get("images");
                if (!images.isEmpty()) {
                    return images.get(0);
                }
            }
        } catch (Exception e) {
            logger.warn("Stable Diffusion服务不可用: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 使用OpenAI DALL-E API生成图片
     */
    @SuppressWarnings("unchecked")
    private String generateWithDallE(String prompt) {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            logger.warn("OpenAI API Key未配置，无法使用DALL-E");
            return null;
        }

        logger.info("使用DALL-E生成图片, prompt={}", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "dall-e-3");
        requestBody.put("prompt", prompt);
        requestBody.put("n", 1);
        requestBody.put("size", defaultWidth + "x" + defaultHeight);
        requestBody.put("response_format", "b64_json");

        try {
            Map<String, Object> response = webClient.post()
                    .uri("https://api.openai.com/v1/images/generations")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("data")) {
                java.util.List<Map<String, Object>> data = (java.util.List<Map<String, Object>>) response.get("data");
                if (!data.isEmpty()) {
                    return (String) data.get(0).get("b64_json");
                }
            }
        } catch (Exception e) {
            logger.error("DALL-E调用失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 保存Base64图片到本地文件，返回访问URL
     */
    private String saveImage(String base64Data, String prompt) {
        try {
            // 确保目录存在
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "img_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
            String filePath = uploadDir + fileName;

            // 解码并保存
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(imageBytes);
            }

            logger.info("图片已保存: {}", filePath);
            return "/uploads/generated/" + fileName;
        } catch (Exception e) {
            logger.error("保存图片失败: {}", e.getMessage());
            // 降级：返回base64 data URI
            return "data:image/png;base64," + base64Data;
        }
    }

    /**
     * 检查图像生成服务是否可用
     */
    public boolean isAvailable() {
        if ("disabled".equals(provider)) return false;
        if ("openai-dall-e".equals(provider)) {
            return openaiApiKey != null && !openaiApiKey.isEmpty();
        }
        // 检查Stable Diffusion
        try {
            webClient.get()
                    .uri(sdApiUrl + "/sdapi/v1/options")
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
