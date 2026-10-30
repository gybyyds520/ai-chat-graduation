package com.example.aichat.service.impl;

import com.example.aichat.entity.CommunityMember;
import com.example.aichat.entity.CommunityPost;
import com.example.aichat.entity.CommunityPostComment;
import com.example.aichat.entity.CommunityPostLike;
import com.example.aichat.entity.InterestCommunity;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.event.UserDataCaptureEvent;
import com.example.aichat.mapper.CommunityMemberMapper;
import com.example.aichat.mapper.CommunityPostCommentMapper;
import com.example.aichat.mapper.CommunityPostLikeMapper;
import com.example.aichat.mapper.CommunityPostMapper;
import com.example.aichat.mapper.InterestCommunityMapper;
import com.example.aichat.service.CommunityService;
import com.example.aichat.service.ContentAuditService;
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
public class CommunityServiceImpl implements CommunityService {

    private static final Logger logger = LoggerFactory.getLogger(CommunityServiceImpl.class);

    /** 社群帖子内容长度上限。 */
    private static final int MAX_POST_LENGTH = 2000;
    /** 评论内容长度上限。 */
    private static final int MAX_COMMENT_LENGTH = 500;

    private final InterestCommunityMapper interestCommunityMapper;
    private final CommunityMemberMapper communityMemberMapper;
    private final CommunityPostMapper communityPostMapper;
    private final CommunityPostCommentMapper communityPostCommentMapper;
    private final CommunityPostLikeMapper communityPostLikeMapper;
    private final ContentAuditService contentAuditService;

    /** 数据池自动采集事件发布器（字段注入，单测构造时为空则跳过发布）。 */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public CommunityServiceImpl(InterestCommunityMapper interestCommunityMapper,
                                CommunityMemberMapper communityMemberMapper,
                                CommunityPostMapper communityPostMapper,
                                CommunityPostCommentMapper communityPostCommentMapper,
                                CommunityPostLikeMapper communityPostLikeMapper,
                                ContentAuditService contentAuditService) {
        this.interestCommunityMapper = interestCommunityMapper;
        this.communityMemberMapper = communityMemberMapper;
        this.communityPostMapper = communityPostMapper;
        this.communityPostCommentMapper = communityPostCommentMapper;
        this.communityPostLikeMapper = communityPostLikeMapper;
        this.contentAuditService = contentAuditService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterestCommunity createCommunity(Long userId, String name, String description, String category, String avatarUrl) {
        InterestCommunity community = new InterestCommunity();
        community.setName(name);
        community.setDescription(description);
        community.setCategory(category != null ? category : "other");
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            community.setAvatar(avatarUrl);
        }
        community.setOwnerId(userId);
        community.setMemberCount(1);
        community.setPostCount(0);
        community.setVisibility("public");
        community.setCreatedAt(LocalDateTime.now());
        community.setUpdatedAt(LocalDateTime.now());

        interestCommunityMapper.insert(community);

        CommunityMember member = new CommunityMember();
        member.setCommunityId(community.getId());
        member.setUserId(userId);
        member.setRole("owner");
        member.setJoinedAt(LocalDateTime.now());
        communityMemberMapper.insert(member);

        logger.info("[兴趣社群] 用户 {} 创建社群: {}", userId, name);
        return community;
    }

    @Override
    public InterestCommunity getCommunityById(Long communityId) {
        return interestCommunityMapper.selectById(communityId);
    }

    @Override
    public List<InterestCommunity> getCommunities(Long userId, String category) {
        if (category != null) {
            return interestCommunityMapper.getCommunitiesByCategory(category);
        }
        return interestCommunityMapper.getPublicCommunities();
    }

    @Override
    public List<InterestCommunity> searchCommunities(String keyword) {
        return interestCommunityMapper.searchCommunities(keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinCommunity(Long communityId, Long userId) {
        CommunityMember existing = communityMemberMapper.findMember(communityId, userId);
        if (existing != null) {
            return;
        }

        CommunityMember member = new CommunityMember();
        member.setCommunityId(communityId);
        member.setUserId(userId);
        member.setRole("member");
        member.setJoinedAt(LocalDateTime.now());
        communityMemberMapper.insert(member);

        InterestCommunity community = interestCommunityMapper.selectById(communityId);
        if (community != null) {
            community.setMemberCount(community.getMemberCount() + 1);
            interestCommunityMapper.updateById(community);
        }

        logger.info("[兴趣社群] 用户 {} 加入社群: {}", userId, communityId);
    }

    @Override
    public void leaveCommunity(Long communityId, Long userId) {
        CommunityMember member = communityMemberMapper.findMember(communityId, userId);
        if (member != null) {
            communityMemberMapper.deleteById(member.getId());

            InterestCommunity community = interestCommunityMapper.selectById(communityId);
            if (community != null && community.getMemberCount() > 0) {
                community.setMemberCount(community.getMemberCount() - 1);
                interestCommunityMapper.updateById(community);
            }
        }
    }

    @Override
    public List<CommunityMember> getMembers(Long communityId) {
        return communityMemberMapper.getMembersByCommunityId(communityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityPost createPost(Long communityId, Long userId, String userName, String content) {
        // 参数校验（P1）：原实现不校验内容，空/超长帖子可直接落库
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(SocialErrorCode.COMMUNITY_POST_BLANK);
        }
        if (content.length() > MAX_POST_LENGTH) {
            throw new BusinessException(SocialErrorCode.COMMUNITY_POST_TOO_LONG);
        }

        // 内容审核：命中敏感词则记录日志并抛 CONTENT_VIOLATION，事务回滚不产生脏数据
        AuditResult auditResult = contentAuditService.audit(content, "post", userId);
        contentAuditService.recordAuditLog(userId, "post", null, content, auditResult);
        if (!auditResult.isPassed()) {
            throw new BusinessException(SocialErrorCode.CONTENT_VIOLATION);
        }

        CommunityPost post = new CommunityPost();
        post.setCommunityId(communityId);
        post.setUserId(userId);
        post.setUserName(userName);
        post.setContent(content);
        post.setMediaType("text");
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setIsDeleted(false);
        post.setIsPinned(false);
        post.setCreatedAt(LocalDateTime.now());

        communityPostMapper.insert(post);

        InterestCommunity community = interestCommunityMapper.selectById(communityId);
        if (community != null) {
            community.setPostCount(community.getPostCount() + 1);
            interestCommunityMapper.updateById(community);
        }

        logger.info("[社群帖子] 用户 {} 在社群 {} 发布帖子", userId, communityId);

        // 数据池自动采集：帖子内容（sourceId 为帖子主键，去重）
        if (eventPublisher != null) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("communityId", communityId);
            eventPublisher.publishEvent(new UserDataCaptureEvent(
                    userId, "social_post", content, meta,
                    "post:" + post.getId(), LocalDateTime.now()));
        }
        return post;
    }

    @Override
    public List<CommunityPost> getPosts(Long communityId, int limit) {
        return communityPostMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommunityPost>()
                .eq("community_id", communityId)
                .eq("is_deleted", false)
                .orderByDesc("is_pinned")
                .orderByDesc("created_at")
                .last("LIMIT " + limit)
        );
    }

    @Override
    public Map<String, Object> getPostsPaged(Long communityId, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = (safePage - 1) * safeSize;

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommunityPost> base =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommunityPost>()
                .eq("community_id", communityId)
                .eq("is_deleted", false);

        Long total = communityPostMapper.selectCount(base);
        List<CommunityPost> list = communityPostMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommunityPost>()
                .eq("community_id", communityId)
                .eq("is_deleted", false)
                .orderByDesc("is_pinned")
                .orderByDesc("created_at")
                .last("LIMIT " + safeSize + " OFFSET " + offset)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total == null ? 0 : total);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("hasMore", (long) offset + list.size() < (total == null ? 0 : total));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long postId, Long userId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(SocialErrorCode.POST_NOT_FOUND);
        }
        CommunityPostLike existing = communityPostLikeMapper.findByPostAndUser(postId, userId);
        int currentLikes = post.getLikeCount() == null ? 0 : post.getLikeCount();
        if (existing != null) {
            // 已赞 → 取消
            communityPostLikeMapper.deleteById(existing.getId());
            post.setLikeCount(Math.max(0, currentLikes - 1));
            communityPostMapper.updateById(post);
            return false;
        }
        // 未赞 → 点赞
        CommunityPostLike like = new CommunityPostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreatedAt(LocalDateTime.now());
        communityPostLikeMapper.insert(like);
        post.setLikeCount(currentLikes + 1);
        communityPostMapper.updateById(post);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId, Long userId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            // 帖子不存在，幂等返回
            return;
        }
        boolean isAuthor = userId != null && userId.equals(post.getUserId());
        boolean isOwner = isCommunityOwner(post.getCommunityId(), userId);
        // 权限校验（P0）：帖子作者本人或社群管理员可删除，防止越权删除他人帖子
        if (!isAuthor && !isOwner) {
            throw new BusinessException(SocialErrorCode.POST_NO_PERMISSION);
        }
        post.setIsDeleted(true);
        communityPostMapper.updateById(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommunity(Long communityId, Long userId) {
        InterestCommunity community = interestCommunityMapper.selectById(communityId);
        if (community == null) {
            return;
        }
        // 仅社群管理员（owner）可删除社群
        if (userId == null || !userId.equals(community.getOwnerId())) {
            throw new BusinessException(SocialErrorCode.COMMUNITY_NOT_OWNER);
        }
        communityPostMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommunityPost>()
                .eq("community_id", communityId)
        );
        communityMemberMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommunityMember>()
                .eq("community_id", communityId)
        );
        interestCommunityMapper.deleteById(communityId);
    }

    // ============ 评论 ============

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommunityPostComment createComment(Long postId, Long userId, String userName, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(SocialErrorCode.COMMUNITY_COMMENT_BLANK);
        }
        if (content.length() > MAX_COMMENT_LENGTH) {
            throw new BusinessException(SocialErrorCode.COMMUNITY_COMMENT_TOO_LONG);
        }
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null || Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new BusinessException(SocialErrorCode.POST_NOT_FOUND);
        }

        // 内容审核
        AuditResult auditResult = contentAuditService.audit(content, "comment", userId);
        contentAuditService.recordAuditLog(userId, "comment", null, content, auditResult);
        if (!auditResult.isPassed()) {
            throw new BusinessException(SocialErrorCode.CONTENT_VIOLATION);
        }

        CommunityPostComment comment = new CommunityPostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setUserName(userName);
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        communityPostCommentMapper.insert(comment);

        // 维护真实评论数
        post.setCommentCount((post.getCommentCount() == null ? 0 : post.getCommentCount()) + 1);
        communityPostMapper.updateById(post);
        return comment;
    }

    @Override
    public Map<String, Object> getComments(Long postId, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = (safePage - 1) * safeSize;

        Long total = communityPostCommentMapper.countByPostId(postId);
        List<CommunityPostComment> list = communityPostCommentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommunityPostComment>()
                .eq("post_id", postId)
                .orderByDesc("created_at")
                .last("LIMIT " + safeSize + " OFFSET " + offset)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total == null ? 0 : total);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("hasMore", (long) offset + list.size() < (total == null ? 0 : total));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        CommunityPostComment comment = communityPostCommentMapper.selectById(commentId);
        if (comment == null) {
            return;
        }
        // 仅评论作者本人可删除
        if (userId == null || !userId.equals(comment.getUserId())) {
            throw new BusinessException(SocialErrorCode.COMMENT_NO_PERMISSION);
        }
        communityPostCommentMapper.deleteById(commentId);

        // 维护真实评论数
        CommunityPost post = communityPostMapper.selectById(comment.getPostId());
        if (post != null && (post.getCommentCount() == null ? 0 : post.getCommentCount()) > 0) {
            post.setCommentCount(post.getCommentCount() - 1);
            communityPostMapper.updateById(post);
        }
    }

    // ============ 管理员 ============

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pinPost(Long communityId, Long postId, Long adminId) {
        requireCommunityOwner(communityId, adminId);
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null || Boolean.TRUE.equals(post.getIsDeleted())) {
            throw new BusinessException(SocialErrorCode.POST_NOT_FOUND);
        }
        boolean newState = !Boolean.TRUE.equals(post.getIsPinned());
        post.setIsPinned(newState);
        communityPostMapper.updateById(post);
        return newState;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long communityId, Long targetUserId, Long adminId) {
        requireCommunityOwner(communityId, adminId);
        if (adminId.equals(targetUserId)) {
            throw new BusinessException(SocialErrorCode.POST_NO_PERMISSION);
        }
        CommunityMember member = communityMemberMapper.findMember(communityId, targetUserId);
        if (member == null) {
            return;
        }
        // 不能移除其他 owner
        if ("owner".equals(member.getRole())) {
            throw new BusinessException(SocialErrorCode.COMMUNITY_NOT_OWNER);
        }
        communityMemberMapper.deleteById(member.getId());

        InterestCommunity community = interestCommunityMapper.selectById(communityId);
        if (community != null && community.getMemberCount() > 0) {
            community.setMemberCount(community.getMemberCount() - 1);
            interestCommunityMapper.updateById(community);
        }
    }

    // ============ 内部工具 ============

    private boolean isCommunityOwner(Long communityId, Long userId) {
        if (communityId == null || userId == null) {
            return false;
        }
        InterestCommunity community = interestCommunityMapper.selectById(communityId);
        return community != null && userId.equals(community.getOwnerId());
    }

    private void requireCommunityOwner(Long communityId, Long userId) {
        if (!isCommunityOwner(communityId, userId)) {
            throw new BusinessException(SocialErrorCode.COMMUNITY_NOT_OWNER);
        }
    }
}
