package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.ChatFile;
import com.example.aichat.entity.ChatImage;
import com.example.aichat.service.MultimodalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/multimodal")
public class MultimodalController {

    @Autowired
    private MultimodalService multimodalService;

    @PostMapping("/image/upload")
    public R uploadImage(@RequestParam("file") MultipartFile file,
                         @RequestParam String sessionId,
                         @RequestParam(required = false) Long userId,
                         @RequestParam(required = false) Long characterId) {
        try {
            File saved = com.example.aichat.util.FileUploadUtil.save(file, "images");

            // 修复健壮性：原 URL 指向不存在的 /api/file/image/{name}（404），
            // 统一使用真实静态映射 /uploads/images/{name}
            String imageUrl = "/uploads/images/" + saved.getName();

            // 安全修复（P0）：归属用户强制取当前登录用户
            Long currentUserId = com.example.aichat.common.security.AuthContext.requireSelf(userId);

            ChatImage chatImage = multimodalService.processImage(sessionId, currentUserId, characterId, imageUrl);
            return R.success(Map.of(
                "imageUrl", imageUrl,
                "ocrResult", chatImage.getOcrResult(),
                "description", chatImage.getImageDescription()
            ));
        } catch (Exception e) {
            return R.error("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/image/process")
    public R processImage(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("sessionId");
        Object userIdObj = request.get("userId");
        Object characterIdObj = request.get("characterId");
        String imageUrl = (String) request.get("imageUrl");
        
        if (sessionId == null || imageUrl == null) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：归属用户强制取当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        Long characterId = characterIdObj != null ? ((Number) characterIdObj).longValue() : null;
        
        ChatImage chatImage = multimodalService.processImage(sessionId, userId, characterId, imageUrl);
        return R.success(Map.of(
            "ocrResult", chatImage.getOcrResult(),
            "description", chatImage.getImageDescription()
        ));
    }

    @GetMapping("/images/{sessionId}")
    public R getImagesBySession(@PathVariable String sessionId) {
        List<ChatImage> images = multimodalService.getImagesBySession(sessionId);
        return R.success(images);
    }

    @PostMapping("/file/upload")
    public R uploadFile(@RequestParam("file") MultipartFile file,
                        @RequestParam String sessionId,
                        @RequestParam(required = false) Long userId,
                        @RequestParam(required = false) Long characterId) {
        try {
            File saved = com.example.aichat.util.FileUploadUtil.save(file, "files");
            String originalFilename = file.getOriginalFilename();

            // 修复健壮性：原 URL 指向不存在的 /api/file/download/{name}（404），
            // 统一使用真实静态映射 /uploads/files/{name}
            String fileUrl = "/uploads/files/" + saved.getName();

            // 安全修复（P0）：归属用户强制取当前登录用户
            Long currentUserId = com.example.aichat.common.security.AuthContext.requireSelf(userId);

            ChatFile chatFile = multimodalService.processFile(
                sessionId, currentUserId, characterId, originalFilename, fileUrl,
                file.getContentType(), file.getSize()
            );

            return R.success(Map.of(
                "fileUrl", fileUrl,
                "fileName", originalFilename,
                "extractedContent", chatFile.getExtractedContent()
            ));
        } catch (Exception e) {
            return R.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/files/{sessionId}")
    public R getFilesBySession(@PathVariable String sessionId) {
        List<ChatFile> files = multimodalService.getFilesBySession(sessionId);
        return R.success(files);
    }
}
