package com.example.aichat.service.impl;

import com.example.aichat.entity.SocialPost;
import com.example.aichat.entity.User;
import com.example.aichat.entity.UserInterest;
import com.example.aichat.entity.UserViewRecord;
import com.example.aichat.mapper.SocialPostMapper;
import com.example.aichat.mapper.UserFollowMapper;
import com.example.aichat.mapper.UserInterestMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.UserViewRecordMapper;
import com.example.aichat.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    
    private final SocialPostMapper socialPostMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final UserInterestMapper userInterestMapper;
    private final UserViewRecordMapper userViewRecordMapper;

    public RecommendationServiceImpl(SocialPostMapper socialPostMapper,
                                     UserMapper userMapper,
                                     UserFollowMapper userFollowMapper,
                                     UserInterestMapper userInterestMapper,
                                     UserViewRecordMapper userViewRecordMapper) {
        this.socialPostMapper = socialPostMapper;
        this.userMapper = userMapper;
        this.userFollowMapper = userFollowMapper;
        this.userInterestMapper = userInterestMapper;
        this.userViewRecordMapper = userViewRecordMapper;
    }

    @Override
    public List<SocialPost> getRecommendedPosts(Long userId, int limit) {
        List<SocialPost> allPublicPosts = socialPostMapper.getPublicPosts(limit * 2);
        
        Set<Long> following = userFollowMapper.getFollowing(userId).stream()
            .map(f -> f.getFollowingId())
            .collect(Collectors.toSet());
        
        Set<Long> viewed = userViewRecordMapper.getViewedByUserAndType(userId, "post").stream()
            .map(v -> v.getTargetId())
            .collect(Collectors.toSet());
        
        List<SocialPost> filtered = new ArrayList<>();
        for (SocialPost post : allPublicPosts) {
            if (!viewed.contains(post.getId())) {
                filtered.add(post);
            }
        }
        
        Map<String, Integer> interests = userInterestMapper.getInterestsByUserId(userId).stream()
            .collect(Collectors.toMap(UserInterest::getInterest, UserInterest::getWeight));
        
        filtered.sort((a, b) -> {
            int scoreA = calculateScore(a, following, interests);
            int scoreB = calculateScore(b, following, interests);
            return Integer.compare(scoreB, scoreA);
        });
        
        return filtered.size() > limit ? filtered.subList(0, limit) : filtered;
    }

    private int calculateScore(SocialPost post, Set<Long> following, Map<String, Integer> interests) {
        int score = 0;
        
        if (following != null && following.contains(post.getUserId())) {
            score += 50;
        }
        
        score += post.getLikeCount() * 2;
        score += post.getCommentCount() * 3;
        score += post.getShareCount() * 4;
        
        String content = post.getContent();
        if (content != null && interests != null && !interests.isEmpty()) {
            for (Map.Entry<String, Integer> entry : interests.entrySet()) {
                if (content.toLowerCase().contains(entry.getKey().toLowerCase())) {
                    score += entry.getValue() * 2;
                }
            }
        }
        
        return score;
    }

    @Override
    public List<User> getRecommendedUsers(Long userId, int limit) {
        List<User> allUsers = userMapper.selectList(null);
        
        Set<Long> following = userFollowMapper.getFollowing(userId).stream()
            .map(f -> f.getFollowingId())
            .collect(Collectors.toSet());
        
        List<User> filtered = new ArrayList<>();
        for (User user : allUsers) {
            if (!user.getId().equals(userId) && !following.contains(user.getId())) {
                filtered.add(user);
            }
        }
        
        filtered.sort((a, b) -> {
            int followersA = userFollowMapper.countFollowers(a.getId());
            int followersB = userFollowMapper.countFollowers(b.getId());
            return Integer.compare(followersB, followersA);
        });
        
        return filtered.size() > limit ? filtered.subList(0, limit) : filtered;
    }

    @Override
    public void updateUserInterest(Long userId, String interest, int weight) {
        UserInterest existing = userInterestMapper.findByUserAndInterest(userId, interest);
        
        if (existing != null) {
            userInterestMapper.incrementWeight(userId, interest, weight);
        } else {
            UserInterest userInterest = new UserInterest();
            userInterest.setUserId(userId);
            userInterest.setInterest(interest.toLowerCase());
            userInterest.setWeight(weight);
            userInterest.setCreatedAt(LocalDateTime.now());
            userInterest.setUpdatedAt(LocalDateTime.now());
            
            userInterestMapper.insert(userInterest);
        }
        
        logger.debug("[推荐系统] 用户 {} 更新兴趣: {} +{}", userId, interest, weight);
    }

    @Override
    public void trackPostView(Long userId, Long postId) {
        UserViewRecord existing = userViewRecordMapper.findByUserAndTarget(userId, "post", postId);
        
        if (existing != null) {
            userViewRecordMapper.incrementViewCount(userId, "post", postId, LocalDateTime.now());
        } else {
            UserViewRecord record = new UserViewRecord();
            record.setUserId(userId);
            record.setTargetType("post");
            record.setTargetId(postId);
            record.setViewCount(1);
            record.setLastViewTime(LocalDateTime.now());
            record.setCreatedAt(LocalDateTime.now());
            
            userViewRecordMapper.insert(record);
        }
    }

    @Override
    public void trackPostLike(Long userId, Long postId) {
        SocialPost post = socialPostMapper.selectById(postId);
        if (post != null) {
            updateUserInterest(userId, "liked_post", 10);
            String content = post.getContent();
            if (content != null && content.length() > 0) {
                List<String> keywords = extractKeywords(content);
                keywords.forEach(k -> updateUserInterest(userId, k, 5));
            }
        }
    }

    @Override
    public void trackPostComment(Long userId, Long postId) {
        SocialPost post = socialPostMapper.selectById(postId);
        if (post != null) {
            updateUserInterest(userId, "commented_post", 8);
            String content = post.getContent();
            if (content != null && content.length() > 0) {
                List<String> keywords = extractKeywords(content);
                keywords.forEach(k -> updateUserInterest(userId, k, 4));
            }
        }
    }

    private List<String> extractKeywords(String content) {
        Set<String> keywords = new HashSet<>();
        String[] words = content.split("[\\s,，.。!！?？、]+");
        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 10) {
                keywords.add(word.toLowerCase());
            }
        }
        return new ArrayList<>(keywords);
    }
}