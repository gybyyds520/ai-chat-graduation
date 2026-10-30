package com.example.aichat.service.impl;

import com.example.aichat.entity.User;
import com.example.aichat.entity.UserStatistics;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.mapper.UserStatisticsMapper;
import com.example.aichat.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileServiceImpl.class);
    
    private final UserMapper userMapper;
    private final UserStatisticsMapper userStatisticsMapper;

    public ProfileServiceImpl(UserMapper userMapper, UserStatisticsMapper userStatisticsMapper) {
        this.userMapper = userMapper;
        this.userStatisticsMapper = userStatisticsMapper;
    }

    @Override
    public User getProfile(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public User updateProfile(Long userId, Map<String, Object> updates) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        // 边界校验：昵称/简介长度上限，避免超长写入导致 DB 截断或异常。
        // nickname 列 varchar(50)；bio 为 text，业务上限 500 字符。
        if (updates.get("nickname") instanceof String nickname && !nickname.isBlank()
                && nickname.length() > 50) {
            throw new com.example.aichat.common.exception.BusinessException(
                    com.example.aichat.common.exception.UserErrorCode.NICKNAME_TOO_LONG);
        }
        if (updates.get("bio") instanceof String bio && bio.length() > 500) {
            throw new com.example.aichat.common.exception.BusinessException(
                    com.example.aichat.common.exception.UserErrorCode.BIO_TOO_LONG);
        }

        updates.forEach((key, value) -> {
            // 修复：请求体里某些字段可能是非 String 类型（布尔/数字），直接 (String) 强转
            // 会抛 ClassCastException 导致 500。非字符串值统一跳过（这些资料字段均为文本）。
            if (!(value instanceof String str)) {
                return;
            }
            switch (key) {
                case "nickname": user.setNickname(str); break;
                case "email": user.setEmail(str); break;
                case "bio": user.setBio(str); break;
                case "interests": user.setInterests(str); break;
                case "location": user.setLocation(str); break;
                case "website": user.setWebsite(str); break;
                case "gender": user.setGender(str); break;
                case "birthday": user.setBirthday(str); break;
                case "avatarUrl": user.setAvatarUrl(str); break;
            }
        });
        
        userMapper.updateById(user);
        logger.info("[个人资料] 用户 {} 更新资料", userId);
        return user;
    }

    @Override
    public String uploadAvatar(Long userId, String filePath) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        
        String avatarUrl = filePath;
        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);
        
        logger.info("[头像上传] 用户 {} 上传头像: {}", userId, avatarUrl);
        return avatarUrl;
    }

    @Override
    public UserStatistics getStatistics(Long userId) {
        UserStatistics stats = userStatisticsMapper.getByUserId(userId);
        if (stats == null) {
            stats = initializeStatistics(userId);
        }
        return stats;
    }

    @Override
    public UserStatistics incrementStatistic(Long userId, String statType) {
        UserStatistics stats = userStatisticsMapper.getByUserId(userId);
        if (stats == null) {
            stats = initializeStatistics(userId);
            return stats;
        }
        
        switch (statType) {
            case "post": userStatisticsMapper.incrementPostCount(userId); break;
            case "comment": userStatisticsMapper.incrementCommentCount(userId); break;
            case "like": userStatisticsMapper.incrementLikeCount(userId, 1); break;
            case "follower": userStatisticsMapper.incrementFollowerCount(userId); break;
            case "following": userStatisticsMapper.incrementFollowingCount(userId); break;
            case "share": userStatisticsMapper.incrementShareCount(userId); break;
        }
        
        return userStatisticsMapper.getByUserId(userId);
    }

    @Override
    public UserStatistics initializeStatistics(Long userId) {
        UserStatistics stats = new UserStatistics();
        stats.setUserId(userId);
        stats.setPostCount(0);
        stats.setCommentCount(0);
        stats.setLikeCount(0);
        stats.setFollowerCount(0);
        stats.setFollowingCount(0);
        stats.setShareCount(0);
        stats.setCreatedAt(LocalDateTime.now());
        stats.setUpdatedAt(LocalDateTime.now());
        
        userStatisticsMapper.insert(stats);
        return stats;
    }
}