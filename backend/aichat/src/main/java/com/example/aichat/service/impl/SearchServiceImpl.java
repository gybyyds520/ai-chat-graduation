package com.example.aichat.service.impl;

import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.SocialPostMapper;
import com.example.aichat.mapper.SocialPostTagMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
    
    private final UserMapper userMapper;
    private final SocialPostMapper socialPostMapper;
    private final SocialPostTagMapper socialPostTagMapper;

    public SearchServiceImpl(UserMapper userMapper,
                             SocialPostMapper socialPostMapper,
                             SocialPostTagMapper socialPostTagMapper) {
        this.userMapper = userMapper;
        this.socialPostMapper = socialPostMapper;
        this.socialPostTagMapper = socialPostTagMapper;
    }

    @Override
    public List<User> searchUsers(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String pattern = "%" + keyword.toLowerCase() + "%";
        List<User> users = userMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .like("username", pattern)
                .or()
                .like("nickname", pattern)
                .last("LIMIT " + limit)
        );
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    @Override
    public List<SocialPost> searchPosts(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String pattern = "%" + keyword + "%";
        return socialPostMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SocialPost>()
                .eq("is_deleted", false)
                .like("content", pattern)
                .orderByDesc("created_at")
                .last("LIMIT " + limit)
        );
    }

    @Override
    public List<String> searchTags(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return socialPostTagMapper.getAllTags().stream().limit(limit).collect(Collectors.toList());
        }
        return socialPostTagMapper.searchTags(keyword).stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<SocialPost> getPostsByTag(String tagName, int limit) {
        List<Long> postIds = socialPostTagMapper.getPostIdsByTag(tagName);
        if (postIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return socialPostMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SocialPost>()
                .eq("is_deleted", false)
                .in("id", postIds)
                .orderByDesc("created_at")
                .last("LIMIT " + limit)
        );
    }
}