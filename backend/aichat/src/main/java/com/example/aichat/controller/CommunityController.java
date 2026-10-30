package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.CommunityMember;
import com.example.aichat.entity.CommunityPost;
import com.example.aichat.entity.InterestCommunity;
import com.example.aichat.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @PostMapping("/create")
    public R createCommunity(@RequestBody Map<String, Object> request) {
        Object userIdObj = request.get("userId");
        String name = (String) request.get("name");

        if (name == null || name.isBlank()) {
            return R.error("参数错误");
        }

        // 安全修复（P0）：创建者强制取当前登录用户
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());
        String description = (String) request.get("description");
        String category = (String) request.get("category");
        String avatarUrl = (String) request.get("avatarUrl");

        InterestCommunity community = communityService.createCommunity(userId, name, description, category, avatarUrl);
        return R.success(community);
    }

    /** 创建社群头像上传：复用 FileUploadUtil，返回可访问 URL。 */
    @PostMapping("/upload-avatar")
    public R uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.error("请选择图片");
        }
        // 必须登录才能上传（社群头像属于社群管理操作）
        com.example.aichat.common.security.AuthContext.requireUserId();
        try {
            File saved = com.example.aichat.util.FileUploadUtil.save(file, "community");
            String url = com.example.aichat.util.FileUploadUtil.toUrl("community", saved);
            return R.success(Map.of("url", url));
        } catch (IOException e) {
            return R.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/{communityId}")
    public R getCommunity(@PathVariable Long communityId) {
        InterestCommunity community = communityService.getCommunityById(communityId);
        if (community != null) {
            return R.success(community);
        }
        return R.error("社群不存在");
    }

    @GetMapping("/list")
    public R getCommunities(@RequestParam(required = false) Long userId, @RequestParam(required = false) String category) {
        List<InterestCommunity> communities = communityService.getCommunities(userId, category);
        return R.success(communities);
    }

    @GetMapping("/search")
    public R searchCommunities(@RequestParam String keyword) {
        List<InterestCommunity> communities = communityService.searchCommunities(keyword);
        return R.success(communities);
    }

    @PostMapping("/join")
    public R joinCommunity(@RequestBody Map<String, Object> request) {
        Object communityIdObj = request.get("communityId");
        Object userIdObj = request.get("userId");

        if (communityIdObj == null) {
            return R.error("参数错误");
        }

        // 安全修复（P0）：加入/退出以当前登录用户为准
        Long communityId = ((Number) communityIdObj).longValue();
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());

        communityService.joinCommunity(communityId, userId);
        return R.success("加入成功");
    }

    @PostMapping("/leave")
    public R leaveCommunity(@RequestBody Map<String, Object> request) {
        Object communityIdObj = request.get("communityId");
        Object userIdObj = request.get("userId");

        if (communityIdObj == null) {
            return R.error("参数错误");
        }

        Long communityId = ((Number) communityIdObj).longValue();
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());

        communityService.leaveCommunity(communityId, userId);
        return R.success("已退出");
    }

    @GetMapping("/members/{communityId}")
    public R getMembers(@PathVariable Long communityId) {
        List<CommunityMember> members = communityService.getMembers(communityId);
        return R.success(members);
    }

    @PostMapping("/post")
    public R createPost(@RequestBody Map<String, Object> request) {
        Object communityIdObj = request.get("communityId");
        Object userIdObj = request.get("userId");
        String userName = (String) request.get("userName");
        String content = (String) request.get("content");

        if (communityIdObj == null || content == null) {
            return R.error("参数错误");
        }

        // 安全修复（P0）：发帖人强制取当前登录用户
        Long communityId = ((Number) communityIdObj).longValue();
        Long userId = com.example.aichat.common.security.AuthContext.requireSelf(
                userIdObj == null ? null : ((Number) userIdObj).longValue());

        CommunityPost post = communityService.createPost(communityId, userId, userName, content);
        return R.success(post);
    }

    /** 帖子列表分页：{list, total, page, size, hasMore}。 */
    @GetMapping("/posts/{communityId}")
    public R getPosts(@PathVariable Long communityId,
                      @RequestParam(defaultValue = "1") int page,
                      @RequestParam(defaultValue = "20") int size) {
        return R.success(communityService.getPostsPaged(communityId, page, size));
    }

    /** 点赞切换（去重）：已赞则取消，未赞则点赞。返回 {liked, likeCount}。 */
    @PostMapping("/post/{postId}/like")
    public R likePost(@PathVariable Long postId) {
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        boolean liked = communityService.toggleLike(postId, userId);
        return R.success(Map.of("liked", liked));
    }

    @DeleteMapping("/post/{postId}")
    public R deletePost(@PathVariable Long postId) {
        // 安全加固（P0）：删除帖子必须校验作者身份，服务层据此拒绝非作者/非管理员删除
        Long currentUserId = com.example.aichat.common.security.AuthContext.requireUserId();
        communityService.deletePost(postId, currentUserId);
        return R.success("删除成功");
    }

    // ============ 评论 ============

    @PostMapping("/post/{postId}/comment")
    public R createComment(@PathVariable Long postId, @RequestBody Map<String, Object> request) {
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        String userName = (String) request.get("userName");
        String content = (String) request.get("content");
        return R.success(communityService.createComment(postId, userId, userName, content));
    }

    @GetMapping("/post/{postId}/comments")
    public R getComments(@PathVariable Long postId,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int size) {
        return R.success(communityService.getComments(postId, page, size));
    }

    @DeleteMapping("/comment/{commentId}")
    public R deleteComment(@PathVariable Long commentId) {
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        communityService.deleteComment(commentId, userId);
        return R.success("删除成功");
    }

    // ============ 管理员 ============

    /** 置顶/取消置顶帖子（社群管理员）。 */
    @PutMapping("/{communityId}/post/{postId}/pin")
    public R pinPost(@PathVariable Long communityId, @PathVariable Long postId) {
        Long adminId = com.example.aichat.common.security.AuthContext.requireUserId();
        boolean pinned = communityService.pinPost(communityId, postId, adminId);
        return R.success(Map.of("pinned", pinned));
    }

    /** 移除成员（社群管理员）。 */
    @DeleteMapping("/{communityId}/member/{userId}")
    public R removeMember(@PathVariable Long communityId, @PathVariable Long userId) {
        Long adminId = com.example.aichat.common.security.AuthContext.requireUserId();
        communityService.removeMember(communityId, userId, adminId);
        return R.success("已移除成员");
    }

    @DeleteMapping("/{communityId}")
    public R deleteCommunity(@PathVariable Long communityId) {
        Long userId = com.example.aichat.common.security.AuthContext.requireUserId();
        communityService.deleteCommunity(communityId, userId);
        return R.success("删除成功");
    }
}
