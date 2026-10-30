package com.example.aichat.service;

import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.User;

import java.util.List;

public interface RecommendationService {
    
    List<SocialPost> getRecommendedPosts(Long userId, int limit);
    
    List<User> getRecommendedUsers(Long userId, int limit);
    
    void updateUserInterest(Long userId, String interest, int weight);
    
    void trackPostView(Long userId, Long postId);
    
    void trackPostLike(Long userId, Long postId);
    
    void trackPostComment(Long userId, Long postId);
}