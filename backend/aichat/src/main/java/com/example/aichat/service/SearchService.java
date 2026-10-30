package com.example.aichat.service;

import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.User;

import java.util.List;

public interface SearchService {
    
    List<User> searchUsers(String keyword, int limit);
    
    List<SocialPost> searchPosts(String keyword, int limit);
    
    List<String> searchTags(String keyword, int limit);
    
    List<SocialPost> getPostsByTag(String tagName, int limit);
}