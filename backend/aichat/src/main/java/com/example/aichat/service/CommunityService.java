package com.example.aichat.service;

import com.example.aichat.entity.CommunityMember;
import com.example.aichat.entity.CommunityPost;
import com.example.aichat.entity.CommunityPostComment;
import com.example.aichat.entity.InterestCommunity;

import java.util.List;
import java.util.Map;

public interface CommunityService {

    InterestCommunity createCommunity(Long userId, String name, String description, String category, String avatarUrl);

    InterestCommunity getCommunityById(Long communityId);

    List<InterestCommunity> getCommunities(Long userId, String category);

    List<InterestCommunity> searchCommunities(String keyword);

    void joinCommunity(Long communityId, Long userId);

    void leaveCommunity(Long communityId, Long userId);

    List<CommunityMember> getMembers(Long communityId);

    CommunityPost createPost(Long communityId, Long userId, String userName, String content);

    List<CommunityPost> getPosts(Long communityId, int limit);

    /**
     * 帖子分页查询。
     * @return {list, total, page, size, hasMore}
     */
    Map<String, Object> getPostsPaged(Long communityId, int page, int size);

    /**
     * 点赞切换（去重）：已赞则取消，未赞则点赞。
     * @return true 表示点赞后当前为已赞状态，false 表示取消后为未赞状态
     */
    boolean toggleLike(Long postId, Long userId);

    /**
     * 删除社群帖子（仅作者本人或社群管理员可删除）。
     * @param postId 帖子 ID
     * @param userId 当前操作用户 ID
     */
    void deletePost(Long postId, Long userId);

    /**
     * 删除社群（仅社群管理员 owner 可删除）。
     */
    void deleteCommunity(Long communityId, Long userId);

    // ============ 评论 ============

    CommunityPostComment createComment(Long postId, Long userId, String userName, String content);

    /** 评论分页查询。 */
    Map<String, Object> getComments(Long postId, int page, int size);

    /** 删除评论（仅评论作者本人）。 */
    void deleteComment(Long commentId, Long userId);

    // ============ 管理员 ============

    /** 置顶/取消置顶帖子（仅社群管理员）。返回切换后的置顶状态。 */
    boolean pinPost(Long communityId, Long postId, Long adminId);

    /** 移除成员（仅社群管理员，且不能移除自己/其他管理员）。 */
    void removeMember(Long communityId, Long targetUserId, Long adminId);
}
