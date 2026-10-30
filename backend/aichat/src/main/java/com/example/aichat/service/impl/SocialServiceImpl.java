package com.example.aichat.service.impl;

import com.example.aichat.entity.SocialComment;
import com.example.aichat.entity.SocialLike;
import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.SocialShare;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.SocialCommentMapper;
import com.example.aichat.mapper.SocialLikeMapper;
import com.example.aichat.mapper.SocialPostMapper;
import com.example.aichat.mapper.SocialShareMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.event.UserDataCaptureEvent;
import com.example.aichat.service.NotificationService;
import com.example.aichat.service.SocialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SocialServiceImpl implements SocialService {

    private static final Logger logger = LoggerFactory.getLogger(SocialServiceImpl.class);

    /** 用户数据采集事件发布器（字段注入 + null 守卫，保证既有直接 new 的单测不破坏） */
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    private final SocialPostMapper socialPostMapper;
    private final SocialCommentMapper socialCommentMapper;
    private final SocialLikeMapper socialLikeMapper;
    private final SocialShareMapper socialShareMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public SocialServiceImpl(SocialPostMapper socialPostMapper,
                            SocialCommentMapper socialCommentMapper,
                            SocialLikeMapper socialLikeMapper,
                            SocialShareMapper socialShareMapper,
                            UserMapper userMapper,
                            NotificationService notificationService) {
        this.socialPostMapper = socialPostMapper;
        this.socialCommentMapper = socialCommentMapper;
        this.socialLikeMapper = socialLikeMapper;
        this.socialShareMapper = socialShareMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public SocialPost createPost(Long userId, String content, String mediaType, String mediaUrls, String videoUrl, String videoCover, String location, String visibility) {
        // 参数校验：content 长度限制 2000，防止超长内容导致 DB 异常与存储压力
        if (content == null || content.isBlank()) {
            throw new BusinessException(SocialErrorCode.POST_CONTENT_BLANK);
        }
        if (content.length() > 2000) {
            throw new BusinessException(SocialErrorCode.POST_CONTENT_TOO_LONG);
        }
        User user = userMapper.selectById(userId);

        SocialPost post = new SocialPost();
        post.setUserId(userId);
        post.setUserName(user != null ? user.getNickname() : "未知用户");
        post.setContent(content);
        post.setMediaType(mediaType != null ? mediaType : "text");
        post.setMediaUrls(mediaUrls);
        post.setVideoUrl(videoUrl);
        post.setVideoCover(videoCover);
        post.setLocation(location);
        post.setVisibility(visibility != null ? visibility : "public");
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setShareCount(0);
        post.setIsDeleted(false);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        socialPostMapper.insert(post);
        logger.info("[社交动态] 用户 {} 发布动态: {}", userId, content);

        // 数据池自动采集：发布成功后发布事件，事务提交后由监听器异步入库
        if (eventPublisher != null) {
            try {
                Map<String, Object> meta = new HashMap<>();
                meta.put("postId", post.getId());
                meta.put("mediaType", post.getMediaType());
                meta.put("location", location);
                meta.put("visibility", visibility);
                eventPublisher.publishEvent(new UserDataCaptureEvent(
                        userId, "social_feed", content, meta,
                        "post:" + post.getId(), LocalDateTime.now()));
            } catch (Exception e) {
                logger.warn("社交动态采集事件发布失败, userId={}: {}", userId, e.getMessage());
            }
        }

        return post;
    }

    @Override
    public SocialPost getPostById(Long postId) {
        return socialPostMapper.selectById(postId);
    }

    @Override
    public List<SocialPost> getPublicPosts(int limit) {
        return socialPostMapper.getPublicPosts(limit);
    }

    @Override
    public List<SocialPost> getUserPosts(Long userId, int limit) {
        return socialPostMapper.getUserPosts(userId, limit);
    }

    @Override
    public List<SocialPost> getFriendsPosts(List<Long> userIds, int limit) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return socialPostMapper.getFriendsPosts(userIds, limit);
    }

    @Override
    public void deletePost(Long postId, Long userId) {
        SocialPost post = socialPostMapper.selectById(postId);
        if (post != null && post.getUserId().equals(userId)) {
            post.setIsDeleted(true);
            socialPostMapper.updateById(post);
            logger.info("[社交动态] 用户 {} 删除动态: {}", userId, postId);
        }
    }

    @Override
    @Transactional
    // 安全修复：补充 @Transactional 保证 comment 插入 + post count 更新 + 通知原子化
    public SocialComment createComment(Long postId, Long userId, String content, Long parentId) {
        // 参数校验：content 长度限制 500
        if (content == null || content.isBlank()) {
            throw new BusinessException(SocialErrorCode.COMMENT_CONTENT_BLANK);
        }
        if (content.length() > 500) {
            throw new BusinessException(SocialErrorCode.COMMENT_CONTENT_BLANK, "评论内容不能超过500个字符");
        }
        User user = userMapper.selectById(userId);
        SocialPost post = socialPostMapper.selectById(postId);

        // 修复 NPE 风险：原实现返回 null，Controller 调用 .getXxx() 会 NPE
        if (post == null || post.getIsDeleted()) {
            throw new BusinessException(SocialErrorCode.POST_NOT_FOUND);
        }

        SocialComment comment = new SocialComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setUserName(user != null ? user.getNickname() : "未知用户");
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setLikeCount(0);
        comment.setIsDeleted(false);
        comment.setCreatedAt(LocalDateTime.now());

        socialCommentMapper.insert(comment);
        socialPostMapper.incrementCommentCount(postId);

        if (!post.getUserId().equals(userId)) {
            notificationService.createNotification(
                post.getUserId(),
                "comment",
                "post",
                postId,
                userId,
                comment.getUserName(),
                comment.getUserName() + " 评论了你的动态"
            );
        }

        logger.info("[社交评论] 用户 {} 评论动态 {}: {}", userId, postId, content);
        return comment;
    }

    @Override
    public List<SocialComment> getComments(Long postId) {
        return socialCommentMapper.getCommentsByPost(postId);
    }

    @Override
    public List<SocialComment> getReplies(Long parentId) {
        return socialCommentMapper.getRepliesByParent(parentId);
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        SocialComment comment = socialCommentMapper.selectById(commentId);
        if (comment != null && comment.getUserId().equals(userId)) {
            comment.setIsDeleted(true);
            socialCommentMapper.updateById(comment);
            logger.info("[社交评论] 用户 {} 删除评论: {}", userId, commentId);
        }
    }

    @Override
    @Transactional
    // 安全修复：toggleLike 涉及多步写操作（删除/插入 like + 更新 post/comment count + 通知），
    // 必须原子化，否则可能出现"已点赞但 count 未递减"等数据不一致
    public SocialLike toggleLike(String targetType, Long targetId, Long userId) {
        // 参数校验：targetType 仅允许 post/comment，防止 SQL 注入与业务异常
        if (!"post".equals(targetType) && !"comment".equals(targetType)) {
            throw new BusinessException(SocialErrorCode.POST_NOT_FOUND, "targetType 必须为 post 或 comment");
        }
        SocialLike existing = socialLikeMapper.findLike(targetType, targetId, userId);

        if (existing != null) {
            socialLikeMapper.deleteById(existing.getId());
            if ("post".equals(targetType)) {
                socialPostMapper.decrementLikeCount(targetId);
            } else if ("comment".equals(targetType)) {
                socialCommentMapper.decrementLikeCount(targetId);
            }
            return null;
        }

        SocialLike like = new SocialLike();
        like.setTargetType(targetType);
        like.setTargetId(targetId);
        like.setUserId(userId);
        like.setCreatedAt(LocalDateTime.now());

        socialLikeMapper.insert(like);

        if ("post".equals(targetType)) {
            socialPostMapper.incrementLikeCount(targetId);
            SocialPost post = socialPostMapper.selectById(targetId);
            if (post != null && !post.getUserId().equals(userId)) {
                User user = userMapper.selectById(userId);
                notificationService.createNotification(
                    post.getUserId(),
                    "like",
                    "post",
                    targetId,
                    userId,
                    user != null ? user.getNickname() : "未知用户",
                    "点赞了你的动态"
                );
            }
        } else if ("comment".equals(targetType)) {
            socialCommentMapper.incrementLikeCount(targetId);
        }

        logger.info("[社交点赞] 用户 {} 点赞 {}: {}", userId, targetType, targetId);
        return like;
    }

    @Override
    public boolean isLiked(String targetType, Long targetId, Long userId) {
        return socialLikeMapper.findLike(targetType, targetId, userId) != null;
    }

    @Override
    public int getLikeCount(String targetType, Long targetId) {
        return socialLikeMapper.countLikes(targetType, targetId);
    }

    @Override
    @Transactional
    // 安全修复：补充 @Transactional，sharePost 涉及 share 插入 + count 更新 + 通知
    public SocialShare sharePost(Long postId, Long userId, String shareText, String shareType, Long targetId) {
        User user = userMapper.selectById(userId);
        SocialPost post = socialPostMapper.selectById(postId);

        // 修复 NPE 风险：原实现返回 null，Controller 调用 .getXxx() 会 NPE
        if (post == null || post.getIsDeleted()) {
            throw new BusinessException(SocialErrorCode.POST_NOT_FOUND);
        }
        
        SocialShare share = new SocialShare();
        share.setPostId(postId);
        share.setUserId(userId);
        share.setUserName(user != null ? user.getNickname() : "未知用户");
        share.setShareText(shareText);
        share.setShareType(shareType != null ? shareType : "feed");
        share.setTargetId(targetId);
        share.setCreatedAt(LocalDateTime.now());
        
        socialShareMapper.insert(share);
        socialPostMapper.incrementShareCount(postId);
        
        if (!post.getUserId().equals(userId)) {
            notificationService.createNotification(
                post.getUserId(),
                "share",
                "post",
                postId,
                userId,
                share.getUserName(),
                share.getUserName() + " 分享了你的动态"
            );
        }
        
        logger.info("[社交分享] 用户 {} 分享动态 {}", userId, postId);
        return share;
    }

    @Override
    public List<SocialShare> getShares(Long postId) {
        return socialShareMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SocialShare>()
                .eq("post_id", postId)
        );
    }
}