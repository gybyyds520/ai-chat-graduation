package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.SocialErrorCode;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.entity.CommunityMember;
import com.example.aichat.entity.CommunityPost;
import com.example.aichat.entity.CommunityPostComment;
import com.example.aichat.entity.CommunityPostLike;
import com.example.aichat.entity.InterestCommunity;
import com.example.aichat.mapper.CommunityMemberMapper;
import com.example.aichat.mapper.CommunityPostCommentMapper;
import com.example.aichat.mapper.CommunityPostLikeMapper;
import com.example.aichat.mapper.CommunityPostMapper;
import com.example.aichat.mapper.InterestCommunityMapper;
import com.example.aichat.service.ContentAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * CommunityService 单元测试（Mockito 隔离）。
 *
 * 覆盖点：
 *   1. createPost：空/超长/正常/敏感词；
 *   2. deletePost：作者本人 / 社群管理员 / 非作者非管理员 403 / 帖子不存在；
 *   3. toggleLike（点赞去重）：首次点赞 / 重复点赞取消 / 取消后再赞 / 帖子不存在；
 *   4. createCommunity：正常创建社群并加入成员；
 *   5. createComment：正常 / 空内容；
 *   6. deleteComment：作者本人 / 他人 403；
 *   7. pinPost：管理员置顶 / 非管理员 403；
 *   8. removeMember：管理员移除普通成员 / 不能移除自己；
 *   9. deleteCommunity：管理员 / 非管理员 403。
 */
@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock
    private InterestCommunityMapper interestCommunityMapper;
    @Mock
    private CommunityMemberMapper communityMemberMapper;
    @Mock
    private CommunityPostMapper communityPostMapper;
    @Mock
    private CommunityPostCommentMapper communityPostCommentMapper;
    @Mock
    private CommunityPostLikeMapper communityPostLikeMapper;
    @Mock
    private ContentAuditService contentAuditService;

    @InjectMocks
    private CommunityServiceImpl communityService;

    private CommunityPost samplePost(Long id, Long authorId) {
        CommunityPost p = new CommunityPost();
        p.setId(id);
        p.setCommunityId(10L);
        p.setUserId(authorId);
        p.setContent("帖子内容");
        p.setIsDeleted(false);
        p.setLikeCount(0);
        p.setCommentCount(0);
        p.setIsPinned(false);
        return p;
    }

    private InterestCommunity sampleCommunity(Long id, Long ownerId, int postCount) {
        InterestCommunity c = new InterestCommunity();
        c.setId(id);
        c.setName("兴趣群");
        c.setOwnerId(ownerId);
        c.setPostCount(postCount);
        c.setMemberCount(5);
        return c;
    }

    private CommunityPostComment sampleComment(Long id, Long authorId) {
        CommunityPostComment c = new CommunityPostComment();
        c.setId(id);
        c.setPostId(100L);
        c.setUserId(authorId);
        c.setContent("评论内容");
        return c;
    }

    // ============ createPost ============

    @Test
    void createPost_空内容应抛COMMUNITY_POST_BLANK() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> communityService.createPost(10L, 1L, "Alice", "  "));
        assertEquals(SocialErrorCode.COMMUNITY_POST_BLANK, ex.getErrorCode());
        verifyNoInteractions(communityPostMapper);
    }

    @Test
    void createPost_超长内容应抛COMMUNITY_POST_TOO_LONG() {
        String tooLong = "x".repeat(2001);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> communityService.createPost(10L, 1L, "Alice", tooLong));
        assertEquals(SocialErrorCode.COMMUNITY_POST_TOO_LONG, ex.getErrorCode());
        verifyNoInteractions(communityPostMapper);
    }

    @Test
    void createPost_正常应落库并递增帖子数() {
        when(interestCommunityMapper.selectById(10L)).thenReturn(sampleCommunity(10L, 1L, 3));
        when(contentAuditService.audit(anyString(), anyString(), anyLong())).thenReturn(AuditResult.pass());

        CommunityPost result = communityService.createPost(10L, 1L, "Alice", "新帖子");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertFalse(result.getIsDeleted());
        verify(communityPostMapper).insert(any(CommunityPost.class));
        verify(interestCommunityMapper).updateById(argThat((InterestCommunity c) -> c.getPostCount() == 4));
    }

    @Test
    void createPost_含敏感词应抛CONTENT_VIOLATION且不落库() {
        AuditResult rejected = AuditResult.reject(List.of("色情网站"), "HIGH", "命中敏感词");
        when(contentAuditService.audit(anyString(), anyString(), anyLong())).thenReturn(rejected);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> communityService.createPost(10L, 1L, "Alice", "点击这里色情网站"));

        assertEquals(SocialErrorCode.CONTENT_VIOLATION, ex.getErrorCode());
        verify(communityPostMapper, never()).insert(any(CommunityPost.class));
        verify(contentAuditService).recordAuditLog(eq(1L), eq("post"), isNull(), anyString(), eq(rejected));
    }

    // ============ deletePost（作者 / 管理员 / 越权）============

    @Test
    void deletePost_作者本人应可删除() {
        when(communityPostMapper.selectById(100L)).thenReturn(samplePost(100L, 1L));

        communityService.deletePost(100L, 1L);

        verify(communityPostMapper).updateById(argThat((CommunityPost p) -> Boolean.TRUE.equals(p.getIsDeleted())));
    }

    @Test
    void deletePost_社群管理员应可删他人帖子() {
        when(communityPostMapper.selectById(100L)).thenReturn(samplePost(100L, 1L));
        // 社群 owner 是 2L，2L 不是作者但作为管理员可删
        when(interestCommunityMapper.selectById(10L)).thenReturn(sampleCommunity(10L, 2L, 3));

        communityService.deletePost(100L, 2L);

        verify(communityPostMapper).updateById(argThat((CommunityPost p) -> Boolean.TRUE.equals(p.getIsDeleted())));
    }

    @Test
    void deletePost_非作者非管理员应抛POST_NO_PERMISSION() {
        when(communityPostMapper.selectById(100L)).thenReturn(samplePost(100L, 1L));
        // 社群 owner 是 9L，3L 既非作者也非管理员
        when(interestCommunityMapper.selectById(10L)).thenReturn(sampleCommunity(10L, 9L, 3));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> communityService.deletePost(100L, 3L));
        assertEquals(SocialErrorCode.POST_NO_PERMISSION, ex.getErrorCode());
        verify(communityPostMapper, never()).updateById(any(CommunityPost.class));
    }

    @Test
    void deletePost_帖子不存在应幂等返回() {
        when(communityPostMapper.selectById(999L)).thenReturn(null);

        assertDoesNotThrow(() -> communityService.deletePost(999L, 1L));
        verify(communityPostMapper, never()).updateById(any(CommunityPost.class));
    }

    // ============ toggleLike（点赞去重）============

    @Test
    void toggleLike_首次点赞应返回true并递增() {
        CommunityPost post = samplePost(100L, 1L);
        when(communityPostMapper.selectById(100L)).thenReturn(post);
        when(communityPostLikeMapper.findByPostAndUser(100L, 2L)).thenReturn(null);

        boolean liked = communityService.toggleLike(100L, 2L);

        assertTrue(liked);
        verify(communityPostLikeMapper).insert(any(CommunityPostLike.class));
        verify(communityPostMapper).updateById(argThat((CommunityPost p) -> p.getLikeCount() == 1));
    }

    @Test
    void toggleLike_重复点赞应取消并递减() {
        CommunityPost post = samplePost(100L, 1L);
        post.setLikeCount(1);
        CommunityPostLike existing = new CommunityPostLike();
        existing.setId(55L);
        existing.setPostId(100L);
        existing.setUserId(2L);
        when(communityPostMapper.selectById(100L)).thenReturn(post);
        when(communityPostLikeMapper.findByPostAndUser(100L, 2L)).thenReturn(existing);

        boolean liked = communityService.toggleLike(100L, 2L);

        assertFalse(liked);
        verify(communityPostLikeMapper).deleteById(55L);
        verify(communityPostMapper).updateById(argThat((CommunityPost p) -> p.getLikeCount() == 0));
    }

    @Test
    void toggleLike_取消后再赞应恢复点赞() {
        CommunityPost post = samplePost(100L, 1L);
        post.setLikeCount(0);
        when(communityPostMapper.selectById(100L)).thenReturn(post);
        when(communityPostLikeMapper.findByPostAndUser(100L, 2L)).thenReturn(null);

        boolean liked = communityService.toggleLike(100L, 2L);

        assertTrue(liked);
        verify(communityPostLikeMapper).insert(any(CommunityPostLike.class));
        verify(communityPostMapper).updateById(argThat((CommunityPost p) -> p.getLikeCount() == 1));
    }

    @Test
    void toggleLike_帖子不存在应抛POST_NOT_FOUND() {
        when(communityPostMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> communityService.toggleLike(999L, 2L));
        assertEquals(SocialErrorCode.POST_NOT_FOUND, ex.getErrorCode());
        verify(communityPostLikeMapper, never()).insert(any(CommunityPostLike.class));
    }

    // ============ createCommunity ============

    @Test
    void createCommunity_正常应创建社群并加入成员() {
        InterestCommunity result = communityService.createCommunity(1L, "新社群", "描述", "tech", "/uploads/community/a.png");

        assertNotNull(result);
        verify(interestCommunityMapper).insert(any(InterestCommunity.class));
        verify(communityMemberMapper).insert(any(CommunityMember.class));
    }

    // ============ 评论 CRUD ============

    @Test
    void createComment_正常应落库并递增评论数() {
        CommunityPost post = samplePost(100L, 1L);
        post.setCommentCount(2);
        when(communityPostMapper.selectById(100L)).thenReturn(post);
        when(contentAuditService.audit(anyString(), anyString(), anyLong())).thenReturn(AuditResult.pass());

        CommunityPostComment comment = communityService.createComment(100L, 2L, "Bob", "好帖");

        assertNotNull(comment);
        assertEquals(100L, comment.getPostId());
        verify(communityPostCommentMapper).insert(any(CommunityPostComment.class));
        verify(communityPostMapper).updateById(argThat((CommunityPost p) -> p.getCommentCount() == 3));
    }

    @Test
    void createComment_空内容应抛COMMENT_BLANK() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> communityService.createComment(100L, 2L, "Bob", "  "));
        assertEquals(SocialErrorCode.COMMUNITY_COMMENT_BLANK, ex.getErrorCode());
        verifyNoInteractions(communityPostCommentMapper);
    }

    @Test
    void createComment_帖子不存在应抛POST_NOT_FOUND() {
        when(communityPostMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> communityService.createComment(999L, 2L, "Bob", "好帖"));
        assertEquals(SocialErrorCode.POST_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(communityPostCommentMapper);
    }

    @Test
    void getComments_应返回分页结构() {
        when(communityPostCommentMapper.countByPostId(100L)).thenReturn(25L);

        Map<String, Object> page = communityService.getComments(100L, 2, 10);

        assertEquals(2, page.get("page"));
        assertEquals(10, page.get("size"));
        assertEquals(25L, page.get("total"));
        assertEquals(Boolean.TRUE, page.get("hasMore"));
    }

    @Test
    void deleteComment_作者本人应可删除并递减评论数() {
        CommunityPost post = samplePost(100L, 1L);
        post.setCommentCount(3);
        when(communityPostCommentMapper.selectById(77L)).thenReturn(sampleComment(77L, 2L));
        when(communityPostMapper.selectById(100L)).thenReturn(post);

        communityService.deleteComment(77L, 2L);

        verify(communityPostCommentMapper).deleteById(77L);
        verify(communityPostMapper).updateById(argThat((CommunityPost p) -> p.getCommentCount() == 2));
    }

    @Test
    void deleteComment_他人评论应抛COMMENT_NO_PERMISSION() {
        when(communityPostCommentMapper.selectById(77L)).thenReturn(sampleComment(77L, 2L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> communityService.deleteComment(77L, 9L));
        assertEquals(SocialErrorCode.COMMENT_NO_PERMISSION, ex.getErrorCode());
        verify(communityPostCommentMapper, never()).deleteById(anyLong());
    }

    // ============ 管理员：置顶 / 移除成员 ============

    @Test
    void pinPost_管理员应可置顶帖子() {
        CommunityPost post = samplePost(100L, 1L);
        when(interestCommunityMapper.selectById(10L)).thenReturn(sampleCommunity(10L, 2L, 3));
        when(communityPostMapper.selectById(100L)).thenReturn(post);

        boolean pinned = communityService.pinPost(10L, 100L, 2L);

        assertTrue(pinned);
        verify(communityPostMapper).updateById(argThat((CommunityPost p) -> Boolean.TRUE.equals(p.getIsPinned())));
    }

    @Test
    void pinPost_非管理员应抛COMMUNITY_NOT_OWNER() {
        when(interestCommunityMapper.selectById(10L)).thenReturn(sampleCommunity(10L, 2L, 3));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> communityService.pinPost(10L, 100L, 9L));
        assertEquals(SocialErrorCode.COMMUNITY_NOT_OWNER, ex.getErrorCode());
        verify(communityPostMapper, never()).updateById(any(CommunityPost.class));
    }

    @Test
    void removeMember_管理员应可移除普通成员() {
        CommunityMember member = new CommunityMember();
        member.setId(88L);
        member.setRole("member");
        when(interestCommunityMapper.selectById(10L)).thenReturn(sampleCommunity(10L, 2L, 3));
        when(communityMemberMapper.findMember(10L, 5L)).thenReturn(member);

        communityService.removeMember(10L, 5L, 2L);

        verify(communityMemberMapper).deleteById(88L);
        verify(interestCommunityMapper).updateById(argThat((InterestCommunity c) -> c.getMemberCount() == 4));
    }

    @Test
    void removeMember_管理员不能移除自己() {
        when(interestCommunityMapper.selectById(10L)).thenReturn(sampleCommunity(10L, 2L, 3));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> communityService.removeMember(10L, 2L, 2L));
        assertEquals(SocialErrorCode.POST_NO_PERMISSION, ex.getErrorCode());
        verify(communityMemberMapper, never()).deleteById(anyLong());
    }

    // ============ deleteCommunity ============

    @Test
    void deleteCommunity_管理员应可删除社群() {
        when(interestCommunityMapper.selectById(10L)).thenReturn(sampleCommunity(10L, 2L, 3));

        communityService.deleteCommunity(10L, 2L);

        verify(communityPostMapper).delete(any());
        verify(communityMemberMapper).delete(any());
        verify(interestCommunityMapper).deleteById(10L);
    }

    @Test
    void deleteCommunity_非管理员应抛COMMUNITY_NOT_OWNER() {
        when(interestCommunityMapper.selectById(10L)).thenReturn(sampleCommunity(10L, 2L, 3));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> communityService.deleteCommunity(10L, 9L));
        assertEquals(SocialErrorCode.COMMUNITY_NOT_OWNER, ex.getErrorCode());
        verify(interestCommunityMapper, never()).deleteById(anyLong());
    }
}
