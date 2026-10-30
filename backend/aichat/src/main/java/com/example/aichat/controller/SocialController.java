package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.SocialComment;
import com.example.aichat.entity.SocialLike;
import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.SocialShare;
import com.example.aichat.service.SocialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    @Autowired
    private SocialService socialService;

    @PostMapping("/post")
    public R createPost(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String content = (String) request.get("content");
        
        if (content == null || content.trim().isEmpty()) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：发帖人强制取当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        
        String mediaType = (String) request.get("mediaType");
        String mediaUrls = (String) request.get("mediaUrls");
        String videoUrl = (String) request.get("videoUrl");
        String videoCover = (String) request.get("videoCover");
        String location = (String) request.get("location");
        String visibility = (String) request.get("visibility");
        
        SocialPost post = socialService.createPost(userId, content, mediaType, mediaUrls, videoUrl, videoCover, location, visibility);
        return R.success(post);
    }

    @GetMapping("/post/{postId}")
    public R getPost(@PathVariable Long postId) {
        SocialPost post = socialService.getPostById(postId);
        if (post != null) {
            return R.success(post);
        }
        return R.error("动态不存在");
    }

    @GetMapping("/posts/public")
    public R getPublicPosts(@RequestParam(defaultValue = "20") int limit) {
        List<SocialPost> posts = socialService.getPublicPosts(limit);
        return R.success(posts);
    }

    @GetMapping("/posts/user/{userId}")
    public R getUserPosts(@PathVariable Long userId, @RequestParam(defaultValue = "20") int limit) {
        List<SocialPost> posts = socialService.getUserPosts(userId, limit);
        return R.success(posts);
    }

    @PostMapping("/posts/friends")
    public R getFriendsPosts(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> userIds = (List<Long>) request.get("userIds");
        int limit = request.get("limit") != null ? ((Number) request.get("limit")).intValue() : 20;
        
        List<SocialPost> posts = socialService.getFriendsPosts(userIds, limit);
        return R.success(posts);
    }

    @DeleteMapping("/post/{postId}")
    public R deletePost(@PathVariable Long postId, @RequestParam Long userId) {
        // 安全修复（P0）：只能删除自己的动态（服务层二次校验归属）
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        socialService.deletePost(postId, current);
        return R.success("删除成功");
    }

    @PostMapping("/comment")
    public R createComment(@RequestBody Map<String, Object> request) {
        Object postIdObj = request.get("postId");
        Object userIdObj = request.get("userId");
        String content = (String) request.get("content");
        
        if (postIdObj == null || content == null || content.trim().isEmpty()) {
            return R.error("参数错误");
        }
        
        Long postId;
        try {
            postId = ((Number) postIdObj).longValue();
        } catch (ClassCastException e) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：评论者强制取当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        
        Long parentId = request.get("parentId") != null ? ((Number) request.get("parentId")).longValue() : null;
        
        SocialComment comment = socialService.createComment(postId, userId, content, parentId);
        if (comment != null) {
            return R.success(comment);
        }
        return R.error("评论失败");
    }

    @GetMapping("/comments/{postId}")
    public R getComments(@PathVariable Long postId) {
        List<SocialComment> comments = socialService.getComments(postId);
        return R.success(comments);
    }

    @GetMapping("/replies/{parentId}")
    public R getReplies(@PathVariable Long parentId) {
        List<SocialComment> replies = socialService.getReplies(parentId);
        return R.success(replies);
    }

    @DeleteMapping("/comment/{commentId}")
    public R deleteComment(@PathVariable Long commentId, @RequestParam Long userId) {
        // 安全修复（P0）：只能删除自己的评论（服务层二次校验归属）
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        socialService.deleteComment(commentId, current);
        return R.success("删除成功");
    }

    @PostMapping("/like")
    public R toggleLike(@RequestBody Map<String, Object> request) {
        String targetType = (String) request.get("targetType");
        Object targetIdObj = request.get("targetId");
        Object userIdObj = request.get("userId");
        
        if (targetType == null || targetIdObj == null) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：点赞者强制取当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        Long targetId;
        try {
            targetId = ((Number) targetIdObj).longValue();
        } catch (ClassCastException e) {
            return R.error("参数错误");
        }
        
        SocialLike like = socialService.toggleLike(targetType, targetId, userId);
        boolean liked = like != null;
        int count = socialService.getLikeCount(targetType, targetId);
        
        return R.success(Map.of("liked", liked, "count", count));
    }

    @GetMapping("/like/check")
    public R checkLike(@RequestParam String targetType, @RequestParam Long targetId, @RequestParam Long userId) {
        // 安全修复（P0）：只能查询自己的点赞状态
        Long current = com.example.aichat.common.security.AuthContext.requireSelf(userId);
        boolean liked = socialService.isLiked(targetType, targetId, current);
        int count = socialService.getLikeCount(targetType, targetId);
        return R.success(Map.of("liked", liked, "count", count));
    }

    @PostMapping("/share")
    public R sharePost(@RequestBody Map<String, Object> request) {
        Object postIdObj = request.get("postId");
        Object userIdObj = request.get("userId");
        
        if (postIdObj == null) {
            return R.error("参数错误");
        }
        
        // 安全修复（P0）：分享者强制取当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        Long postId;
        try {
            postId = ((Number) postIdObj).longValue();
        } catch (ClassCastException e) {
            return R.error("参数错误");
        }
        
        String shareText = (String) request.get("shareText");
        String shareType = (String) request.get("shareType");
        Long targetId = request.get("targetId") != null ? ((Number) request.get("targetId")).longValue() : null;
        
        SocialShare share = socialService.sharePost(postId, userId, shareText, shareType, targetId);
        if (share != null) {
            return R.success(share);
        }
        return R.error("分享失败");
    }

    @PostMapping("/upload/image")
    public R uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.error("请选择图片");
        }
        
        try {
            File saved = com.example.aichat.util.FileUploadUtil.save(file, "social/images");
            return R.success(Map.of("url", com.example.aichat.util.FileUploadUtil.toUrl("social/images", saved)));
        } catch (IOException e) {
            return R.error("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload/video")
    public R uploadVideo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.error("请选择视频");
        }

        try {
            File saved = com.example.aichat.util.FileUploadUtil.save(file, "social/videos");
            return R.success(Map.of("url", com.example.aichat.util.FileUploadUtil.toUrl("social/videos", saved)));
        } catch (IOException e) {
            return R.error("上传失败: " + e.getMessage());
        }
    }
}