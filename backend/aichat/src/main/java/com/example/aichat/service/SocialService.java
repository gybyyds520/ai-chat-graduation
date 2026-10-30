package com.example.aichat.service;

import com.example.aichat.entity.SocialComment;
import com.example.aichat.entity.SocialLike;
import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.SocialShare;

import java.util.List;
import java.util.Map;

public interface SocialService {
    
    SocialPost createPost(Long userId, String content, String mediaType, String mediaUrls, String videoUrl, String videoCover, String location, String visibility);
    
    SocialPost getPostById(Long postId);
    
    List<SocialPost> getPublicPosts(int limit);
    
    List<SocialPost> getUserPosts(Long userId, int limit);
    
    List<SocialPost> getFriendsPosts(List<Long> userIds, int limit);
    
    void deletePost(Long postId, Long userId);
    
    SocialComment createComment(Long postId, Long userId, String content, Long parentId);
    
    List<SocialComment> getComments(Long postId);
    
    List<SocialComment> getReplies(Long parentId);
    
    void deleteComment(Long commentId, Long userId);
    
    SocialLike toggleLike(String targetType, Long targetId, Long userId);
    
    boolean isLiked(String targetType, Long targetId, Long userId);
    
    int getLikeCount(String targetType, Long targetId);
    
    SocialShare sharePost(Long postId, Long userId, String shareText, String shareType, Long targetId);
    
    List<SocialShare> getShares(Long postId);
}