package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.entity.UserProfile;
import com.example.aichat.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户画像接口（简单版）。
 *
 * 路径 /api/profile/me：
 *   - GET  查询当前登录用户画像
 *   - PUT  更新当前登录用户画像（风格偏好 / 话题 / 活跃时段）
 *
 * 注意：与既有 ProfileController(/api/profile/{userId}) 共存，
 *       Spring 对字面量 "me" 的匹配优先级高于路径变量 {userId}，不冲突。
 */
@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    /** 查询当前登录用户画像。 */
    @GetMapping("/me")
    public R<UserProfile> getMyProfile() {
        Long userId = AuthContext.requireUserId();
        return R.success(userProfileService.getOrCreateProfile(userId));
    }

    /**
     * 更新当前登录用户画像。
     * 请求体示例：
     * {
     *   "preferredStyle": "concise",
     *   "commonTopics": ["学习", "游戏"],
     *   "activeTimeSlots": {"morning": 3, "evening": 5}
     * }
     * 任意字段可缺省，缺省字段保持不变。
     */
    @PutMapping("/me")
    public R<UserProfile> updateMyProfile(@RequestBody Map<String, Object> body) {
        Long userId = AuthContext.requireUserId();

        String style = body.get("preferredStyle") == null
                ? null : String.valueOf(body.get("preferredStyle"));

        @SuppressWarnings("unchecked")
        List<String> topics = body.get("commonTopics") instanceof List
                ? (List<String>) body.get("commonTopics") : null;

        @SuppressWarnings("unchecked")
        Map<String, Object> rawSlots = body.get("activeTimeSlots") instanceof Map
                ? (Map<String, Object>) body.get("activeTimeSlots") : null;
        Map<String, Integer> slots = null;
        if (rawSlots != null) {
            slots = new java.util.HashMap<>();
            for (Map.Entry<String, Object> e : rawSlots.entrySet()) {
                if (e.getValue() instanceof Number) {
                    slots.put(e.getKey(), ((Number) e.getValue()).intValue());
                }
            }
        }

        UserProfile updated = userProfileService.updateProfile(userId, style, topics, slots);
        return R.success(updated);
    }
}
