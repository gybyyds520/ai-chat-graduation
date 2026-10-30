package com.example.aichat.controller;

import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.User;
import com.example.aichat.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 个性化推荐接口。
 *
 * 安全加固（P0）：推荐内容/行为跟踪基于“当前用户”的兴趣画像，
 * 一律以登录令牌中的用户为准，忽略客户端传入的 userId，防止读取他人画像。
 */
@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/posts")
    public R getRecommendedPosts(@RequestParam Long userId, @RequestParam(defaultValue = "20") int limit) {
        Long current = AuthContext.requireSelf(userId);
        limit = Math.min(Math.max(limit, 1), 100);
        List<SocialPost> posts = recommendationService.getRecommendedPosts(current, limit);
        return R.success(posts);
    }

    @GetMapping("/users")
    public R getRecommendedUsers(@RequestParam Long userId, @RequestParam(defaultValue = "10") int limit) {
        Long current = AuthContext.requireSelf(userId);
        limit = Math.min(Math.max(limit, 1), 50);
        List<User> users = recommendationService.getRecommendedUsers(current, limit);
        return R.success(users);
    }

    @PostMapping("/track/view")
    public R trackView(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        Object postIdObj = request.get("postId");
        
        if (postIdObj == null) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(userIdObj == null ? null : ((Number) userIdObj).longValue());
        Long postId;
        try {
            postId = ((Number) postIdObj).longValue();
        } catch (ClassCastException e) {
            return R.error("参数错误");
        }
        
        recommendationService.trackPostView(userId, postId);
        return R.success("跟踪成功");
    }

    @PostMapping("/track/like")
    public R trackLike(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        Object postIdObj = request.get("postId");
        
        if (postIdObj == null) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(userIdObj == null ? null : ((Number) userIdObj).longValue());
        Long postId;
        try {
            postId = ((Number) postIdObj).longValue();
        } catch (ClassCastException e) {
            return R.error("参数错误");
        }
        
        recommendationService.trackPostLike(userId, postId);
        return R.success("跟踪成功");
    }

    @PostMapping("/track/comment")
    public R trackComment(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        Object postIdObj = request.get("postId");
        
        if (postIdObj == null) {
            return R.error("参数错误");
        }
        
        Long userId = AuthContext.requireSelf(userIdObj == null ? null : ((Number) userIdObj).longValue());
        Long postId;
        try {
            postId = ((Number) postIdObj).longValue();
        } catch (ClassCastException e) {
            return R.error("参数错误");
        }
        
        recommendationService.trackPostComment(userId, postId);
        return R.success("跟踪成功");
    }
}
