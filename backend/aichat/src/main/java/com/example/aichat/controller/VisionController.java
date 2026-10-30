package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.service.VisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 视觉理解控制器（M4 ⑫）。
 *
 * <p>POST /api/vision/analyze：上传 jpg/png/webp 图片，调用本地 Ollama llava 生成描述。
 * 受 {@code aichat.vision.enabled} 开关控制，关闭时返回未启用提示（接口可达、离线基线不回退）。</p>
 */
@RestController
@RequestMapping("/api/vision")
public class VisionController {

    /** 允许的图片 MIME 类型。 */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp");

    @Autowired
    private VisionService visionService;

    @PostMapping("/analyze")
    public R<Map<String, Object>> analyze(@RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) String prompt,
                                          @RequestParam(required = false) Long userId) {
        // 安全：归属用户强制取当前登录用户，杜绝横向越权
        AuthContext.requireSelf(userId);

        if (file == null || file.isEmpty()) {
            return R.badRequest("请上传图片文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return R.badRequest("仅支持 jpg/png/webp 图片");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            return R.badRequest("图片大小不能超过 10MB");
        }

        try {
            VisionService.VisionAnalysis result = visionService.analyze(
                    file.getBytes(), file.getOriginalFilename(), prompt);
            return R.success(Map.of(
                    "enabled", result.enabled(),
                    "success", result.success(),
                    "description", result.description() == null ? "" : result.description(),
                    "message", result.message() == null ? "" : result.message(),
                    "model", "llava"
            ));
        } catch (Exception e) {
            return R.error("图片分析失败: " + e.getMessage());
        }
    }

    /** 视觉理解能力状态（前端据此决定是否展示图片按钮）。 */
    @PostMapping("/status")
    public R<Map<String, Object>> status(@RequestParam(required = false) Long userId) {
        AuthContext.requireSelf(userId);
        return R.success(Map.of("enabled", visionService.isEnabled()));
    }
}
