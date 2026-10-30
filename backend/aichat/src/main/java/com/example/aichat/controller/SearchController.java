package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.User;
import com.example.aichat.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/users")
    public R searchUsers(@RequestParam String keyword, @RequestParam(defaultValue = "20") int limit) {
        // 健壮性加固：limit 夹取到 1..50，防止超大分页拖垮数据库
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<User> users = searchService.searchUsers(keyword, safeLimit);
        // 安全修复（P1）：只返回公开资料字段，避免 phone/email/生日/登录信息等隐私字段泄露
        List<Map<String, Object>> publicProfiles = users.stream().map(u -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", u.getId());
            p.put("username", u.getUsername());
            p.put("nickname", u.getNickname());
            p.put("avatar", u.getAvatar() != null ? u.getAvatar() : u.getAvatarUrl());
            p.put("bio", u.getBio());
            p.put("location", u.getLocation());
            p.put("website", u.getWebsite());
            p.put("gender", u.getGender());
            return p;
        }).collect(Collectors.toList());
        return R.success(publicProfiles);
    }

    @GetMapping("/posts")
    public R searchPosts(@RequestParam String keyword, @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<SocialPost> posts = searchService.searchPosts(keyword, safeLimit);
        return R.success(posts);
    }

    @GetMapping("/tags")
    public R searchTags(@RequestParam(required = false) String keyword, @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<String> tags = searchService.searchTags(keyword, safeLimit);
        return R.success(tags);
    }

    @GetMapping("/posts/tag")
    public R getPostsByTag(@RequestParam String tagName, @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<SocialPost> posts = searchService.getPostsByTag(tagName, safeLimit);
        return R.success(posts);
    }
}
