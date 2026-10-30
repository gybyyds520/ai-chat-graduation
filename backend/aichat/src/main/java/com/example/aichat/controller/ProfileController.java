package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.User;
import com.example.aichat.entity.UserStatistics;
import com.example.aichat.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/{userId}")
    public R getProfile(@PathVariable Long userId) {
        User user = profileService.getProfile(userId);
        if (user != null) {
            return R.success(user);
        }
        return R.error("用户不存在");
    }

    @PutMapping("/{userId}")
    public R updateProfile(@PathVariable Long userId, @RequestBody Map<String, Object> updates) {
        // 安全修复（P0）：仅本人可修改资料
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        User user = profileService.updateProfile(current, updates);
        if (user != null) {
            return R.success(user);
        }
        return R.error("更新失败");
    }

    @PostMapping("/upload-avatar")
    public R uploadAvatar(@RequestParam("file") MultipartFile file, @RequestParam Long userId) {
        if (file.isEmpty()) {
            return R.error("请选择图片");
        }
        
        // 安全修复（P0）：头像仅本人可上传
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        try {
            File saved = com.example.aichat.util.FileUploadUtil.save(file, "avatars");
            String avatarUrl = com.example.aichat.util.FileUploadUtil.toUrl("avatars", saved);
            profileService.uploadAvatar(current, avatarUrl);
            return R.success(Map.of("url", avatarUrl));
        } catch (IOException e) {
            return R.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics/{userId}")
    public R getStatistics(@PathVariable Long userId) {
        UserStatistics stats = profileService.getStatistics(userId);
        return R.success(stats);
    }

    @PostMapping("/statistics/increment")
    public R incrementStatistic(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String statType = (String) request.get("statType");
        
        if (statType == null) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：只能累加自己的统计数据
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        
        UserStatistics stats = profileService.incrementStatistic(userId, statType);
        return R.success(stats);
    }
}