package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.service.ai.ImageGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI图像生成控制器
 * 提供直接图像生成API和意图检测API
 */
@RestController
@RequestMapping("/api/image")
public class ImageGenerationController {

    private static final Logger logger = LoggerFactory.getLogger(ImageGenerationController.class);

    @Autowired
    private ImageGenerationService imageGenerationService;

    @Autowired
    private com.example.aichat.service.QuotaService quotaService;

    /**
     * 直接生成图片
     * POST /api/image/generate
     * Body: { "prompt": "一只可爱的猫" }
     */
    @PostMapping("/generate")
    public R generateImage(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return R.badRequest("图片描述不能为空");
        }

        // M3-A5：已登录用户在生成图片前校验 image_gen 每日额度；匿名调用不计数。
        // 测试环境 aichat.quota.enabled=false 时 checkAndConsume 直接放行。
        if (quotaService != null) {
            try {
                Long uid = com.example.aichat.common.security.AuthContext.getCurrentUserId().orElse(null);
                if (uid != null) {
                    quotaService.checkAndConsume(uid, "image_gen");
                }
            } catch (com.example.aichat.common.exception.BusinessException be) {
                return R.error(be.getErrorCode().getCode(), be.getMessage());
            }
        }

        logger.info("收到图像生成请求, prompt={}", prompt);
        Map<String, Object> result = imageGenerationService.generateImage(prompt.trim());

        String status = (String) result.get("status");
        if ("success".equals(status)) {
            return R.success(result);
        } else if ("disabled".equals(status)) {
            return R.error("图像生成功能未启用");
        } else {
            return R.error((String) result.getOrDefault("message", "图像生成失败"));
        }
    }

    /**
     * 检测消息是否为图像生成请求
     * POST /api/image/detect
     * Body: { "message": "画一只猫" }
     */
    @PostMapping("/detect")
    public R detectIntent(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        boolean isImageRequest = imageGenerationService.isImageGenerationRequest(message);
        String extractedPrompt = isImageRequest ? imageGenerationService.extractImagePrompt(message) : null;

        return R.success(Map.of(
                "isImageRequest", isImageRequest,
                "extractedPrompt", extractedPrompt != null ? extractedPrompt : ""
        ));
    }

    /**
     * 检查图像生成服务状态
     * GET /api/image/health
     */
    @GetMapping("/health")
    public R checkHealth() {
        boolean available = imageGenerationService.isAvailable();
        return R.success(Map.of(
                "available", available,
                "message", available ? "图像生成服务可用" : "图像生成服务不可用"
        ));
    }
}
