package com.example.aichat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.entity.ChatMessage;
import com.example.aichat.entity.CommunityPost;
import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.ChatMessageMapper;
import com.example.aichat.mapper.CommunityPostMapper;
import com.example.aichat.mapper.SocialPostMapper;
import com.example.aichat.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * M3-A9："我的数据导出"。
 *
 * <p>GET /api/user/data/export：打包当前登录用户的资料 + AI 对话记录 + 社区帖子为 JSON。
 * 全部 requireUserId（仅能导出本人数据），失败的子项降级为空集合，不阻断整体导出。</p>
 */
@RestController
@RequestMapping("/api/user/data")
public class UserDataExportController {

    private static final Logger logger = LoggerFactory.getLogger(UserDataExportController.class);

    private final UserMapper userMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final SocialPostMapper socialPostMapper;
    private final CommunityPostMapper communityPostMapper;

    public UserDataExportController(UserMapper userMapper,
                                    ChatMessageMapper chatMessageMapper,
                                    SocialPostMapper socialPostMapper,
                                    CommunityPostMapper communityPostMapper) {
        this.userMapper = userMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.socialPostMapper = socialPostMapper;
        this.communityPostMapper = communityPostMapper;
    }

    /**
     * 导出当前登录用户的全部个人数据（JSON 流）。
     */
    @GetMapping("/export")
    public R<Map<String, Object>> exportMyData() {
        Long userId = AuthContext.requireUserId();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportedAt", LocalDateTime.now().toString());
        data.put("userId", userId);
        data.put("profile", safeProfile(userId));
        data.put("aiConversations", safeChatMessages(userId));
        data.put("socialPosts", safeSocialPosts(userId));
        data.put("communityPosts", safeCommunityPosts(userId));

        logger.info("用户数据导出 - userId: {}", userId);
        return R.success(data);
    }

    private Map<String, Object> safeProfile(Long userId) {
        try {
            User u = userMapper.selectById(userId);
            if (u == null) {
                return Map.of();
            }
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("username", u.getUsername());
            profile.put("nickname", u.getNickname());
            profile.put("email", u.getEmail());
            profile.put("membershipLevel", u.getMembershipLevel());
            return profile;
        } catch (Exception e) {
            logger.warn("导出用户资料失败 userId={}: {}", userId, e.getMessage());
            return Map.of();
        }
    }

    private List<ChatMessage> safeChatMessages(Long userId) {
        try {
            return chatMessageMapper.selectList(
                    new QueryWrapper<ChatMessage>().eq("user_id", userId).orderByAsc("create_time"));
        } catch (Exception e) {
            logger.warn("导出AI对话记录失败 userId={}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    private List<SocialPost> safeSocialPosts(Long userId) {
        try {
            return socialPostMapper.selectList(
                    new QueryWrapper<SocialPost>().eq("user_id", userId).orderByDesc("id"));
        } catch (Exception e) {
            logger.warn("导出社区帖子失败 userId={}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    private List<CommunityPost> safeCommunityPosts(Long userId) {
        try {
            return communityPostMapper.selectList(
                    new QueryWrapper<CommunityPost>().eq("user_id", userId).orderByDesc("id"));
        } catch (Exception e) {
            logger.warn("导出社群帖子失败 userId={}: {}", userId, e.getMessage());
            return List.of();
        }
    }
}
