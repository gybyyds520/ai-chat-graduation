package com.example.aichat.service.impl;

import com.example.aichat.entity.Friendship;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.FriendshipMapper;
import com.example.aichat.mapper.UserBlockMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.FriendshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FriendshipServiceImpl implements FriendshipService {

    private static final Logger logger = LoggerFactory.getLogger(FriendshipServiceImpl.class);
    
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_ACCEPTED = "accepted";
    private static final String STATUS_REJECTED = "rejected";

    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;
    private final UserBlockMapper userBlockMapper;

    public FriendshipServiceImpl(FriendshipMapper friendshipMapper, UserMapper userMapper,
                                 UserBlockMapper userBlockMapper) {
        this.friendshipMapper = friendshipMapper;
        this.userMapper = userMapper;
        this.userBlockMapper = userBlockMapper;
    }

    private boolean isValidUserId(Long userId) {
        if (userId == null || userId <= 0) {
            logger.warn("无效的用户ID: {}", userId);
            return false;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            logger.warn("用户不存在: {}", userId);
            return false;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendFriendRequest(Long userId, Long friendId) {
        // 可维护性修复（P1）：移除原"══════"装饰线日志，污染日志格式不利于 ELK 解析
        logger.info("[发送好友请求] 用户 {} 向用户 {} 发送好友请求", userId, friendId);

        if (!isValidUserId(userId)) {
            logger.error("[发送好友请求] 失败：发起用户不存在");
            return false;
        }

        if (!isValidUserId(friendId)) {
            logger.error("[发送好友请求] 失败：目标用户不存在");
            return false;
        }

        if (userId.equals(friendId)) {
            logger.warn("[发送好友请求] 失败：不能向自己发送好友请求");
            return false;
        }

        // 拉黑拦截（双向）：无论谁拉黑谁，都不允许再发起好友请求。
        if (userBlockMapper != null) {
            if (userBlockMapper.isBlocked(userId, friendId) > 0) {
                logger.warn("[发送好友请求] 失败：用户 {} 已拉黑用户 {}", userId, friendId);
                return false;
            }
            if (userBlockMapper.isBlocked(friendId, userId) > 0) {
                logger.warn("[发送好友请求] 失败：用户 {} 已拉黑用户 {}，无法向其发送请求", friendId, userId);
                return false;
            }
        }

        try {
            Friendship existing = friendshipMapper.findByUserAndFriend(userId, friendId);
            if (existing != null) {
                if (STATUS_REJECTED.equals(existing.getStatus())) {
                    logger.info("[发送好友请求] 之前发送的请求被拒绝，允许重新发送");
                    existing.setStatus(STATUS_PENDING);
                    existing.setCreateTime(LocalDateTime.now());
                    existing.setAcceptTime(null);
                    friendshipMapper.updateById(existing);
                    logger.info("[发送好友请求] 请求状态已重置为待处理");
                    return true;
                } else if (STATUS_PENDING.equals(existing.getStatus())) {
                    logger.warn("[发送好友请求] 失败：已有待处理的请求");
                    return false;
                } else if (STATUS_ACCEPTED.equals(existing.getStatus())) {
                    logger.warn("[发送好友请求] 失败：已经是好友关系");
                    return false;
                }
                logger.warn("[发送好友请求] 失败：未知状态: {}", existing.getStatus());
                return false;
            }

            Friendship reverse = friendshipMapper.findByUserAndFriend(friendId, userId);
            if (reverse != null) {
                if (STATUS_PENDING.equals(reverse.getStatus())) {
                    logger.info("[发送好友请求] 对方已发送过请求，自动同意");
                    reverse.setStatus(STATUS_ACCEPTED);
                    reverse.setAcceptTime(LocalDateTime.now());
                    friendshipMapper.updateById(reverse);
                    
                    Friendship friendship = new Friendship();
                    friendship.setUserId(userId);
                    friendship.setFriendId(friendId);
                    friendship.setStatus(STATUS_ACCEPTED);
                    friendship.setCreateTime(LocalDateTime.now());
                    friendship.setAcceptTime(LocalDateTime.now());
                    friendshipMapper.insert(friendship);
                    logger.info("[发送好友请求] 已自动建立双向好友关系");
                    return true;
                } else if (STATUS_REJECTED.equals(reverse.getStatus())) {
                    logger.info("[发送好友请求] 对方之前拒绝了请求，允许重新发送");
                    reverse.setStatus(STATUS_PENDING);
                    reverse.setCreateTime(LocalDateTime.now());
                    reverse.setAcceptTime(null);
                    friendshipMapper.updateById(reverse);
                    logger.info("[发送好友请求] 请求状态已重置为待处理");
                    return true;
                } else if (STATUS_ACCEPTED.equals(reverse.getStatus())) {
                    logger.warn("[发送好友请求] 失败：已经是好友关系（反向）");
                    return false;
                }
                logger.warn("[发送好友请求] 失败：反向关系未知状态: {}", reverse.getStatus());
                return false;
            }

            Friendship friendship = new Friendship();
            friendship.setUserId(userId);
            friendship.setFriendId(friendId);
            friendship.setStatus(STATUS_PENDING);
            friendship.setCreateTime(LocalDateTime.now());
            friendshipMapper.insert(friendship);
            
            logger.info("[发送好友请求] 成功");
            return true;
        } catch (Exception e) {
            logger.error("[发送好友请求] 异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acceptFriendRequest(Long userId, Long requesterId) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("[接受好友请求] 用户 {} 接受用户 {} 的好友请求", userId, requesterId);
        
        if (!isValidUserId(userId)) {
            logger.error("[接受好友请求] 失败：用户不存在");
            return false;
        }
        
        if (!isValidUserId(requesterId)) {
            logger.error("[接受好友请求] 失败：请求者不存在");
            return false;
        }

        if (userId.equals(requesterId)) {
            logger.warn("[接受好友请求] 失败：不能接受自己的请求");
            return false;
        }

        try {
            Friendship request = friendshipMapper.findByUserAndFriend(requesterId, userId);
            if (request == null) {
                logger.warn("[接受好友请求] 失败：未找到待处理的好友请求");
                return false;
            }
            
            if (!STATUS_PENDING.equals(request.getStatus())) {
                logger.warn("[接受好友请求] 失败：请求状态不是待处理，当前状态: {}", request.getStatus());
                return false;
            }

            request.setStatus(STATUS_ACCEPTED);
            request.setAcceptTime(LocalDateTime.now());
            friendshipMapper.updateById(request);

            Friendship existingReverse = friendshipMapper.findByUserAndFriend(userId, requesterId);
            if (existingReverse != null) {
                existingReverse.setStatus(STATUS_ACCEPTED);
                existingReverse.setAcceptTime(LocalDateTime.now());
                friendshipMapper.updateById(existingReverse);
            } else {
                Friendship friendship = new Friendship();
                friendship.setUserId(userId);
                friendship.setFriendId(requesterId);
                friendship.setStatus(STATUS_ACCEPTED);
                friendship.setCreateTime(LocalDateTime.now());
                friendship.setAcceptTime(LocalDateTime.now());
                friendshipMapper.insert(friendship);
            }
            
            logger.info("[接受好友请求] 成功");
            return true;
        } catch (Exception e) {
            logger.error("[接受好友请求] 异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectFriendRequest(Long userId, Long requesterId) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("[拒绝好友请求] 用户 {} 拒绝用户 {} 的好友请求", userId, requesterId);
        
        if (!isValidUserId(userId)) {
            logger.error("[拒绝好友请求] 失败：用户不存在");
            return false;
        }
        
        if (!isValidUserId(requesterId)) {
            logger.error("[拒绝好友请求] 失败：请求者不存在");
            return false;
        }

        if (userId.equals(requesterId)) {
            logger.warn("[拒绝好友请求] 失败：不能拒绝自己的请求");
            return false;
        }

        try {
            Friendship request = friendshipMapper.findByUserAndFriend(requesterId, userId);
            if (request == null) {
                logger.warn("[拒绝好友请求] 失败：未找到待处理的好友请求");
                return false;
            }
            
            if (!STATUS_PENDING.equals(request.getStatus())) {
                logger.warn("[拒绝好友请求] 失败：请求状态不是待处理，当前状态: {}", request.getStatus());
                return false;
            }

            request.setStatus(STATUS_REJECTED);
            friendshipMapper.updateById(request);
            
            logger.info("[拒绝好友请求] 成功");
            return true;
        } catch (Exception e) {
            logger.error("[拒绝好友请求] 异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeFriend(Long userId, Long friendId) {
        logger.info("═══════════════════════════════════════════════════════════════");
        logger.info("[删除好友] 用户 {} 删除好友 {}", userId, friendId);
        
        if (!isValidUserId(userId)) {
            logger.error("[删除好友] 失败：用户不存在");
            return false;
        }
        
        if (!isValidUserId(friendId)) {
            logger.error("[删除好友] 失败：好友不存在");
            return false;
        }

        if (userId.equals(friendId)) {
            logger.warn("[删除好友] 失败：不能删除自己");
            return false;
        }

        try {
            Friendship f1 = friendshipMapper.findByUserAndFriend(userId, friendId);
            Friendship f2 = friendshipMapper.findByUserAndFriend(friendId, userId);
            
            boolean deleted = false;
            if (f1 != null) {
                friendshipMapper.deleteById(f1.getId());
                deleted = true;
            }
            if (f2 != null) {
                friendshipMapper.deleteById(f2.getId());
                deleted = true;
            }
            
            if (deleted) {
                logger.info("[删除好友] 成功");
            } else {
                logger.warn("[删除好友] 警告：未找到好友关系");
            }
            return true;
        } catch (Exception e) {
            logger.error("[删除好友] 异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Friendship> getFriendList(Long userId) {
        if (!isValidUserId(userId)) {
            logger.warn("[获取好友列表] 失败：用户不存在");
            return List.of();
        }
        
        try {
            List<Friendship> friends = friendshipMapper.getFriendList(userId);
            logger.debug("[获取好友列表] 用户 {} 的好友数量: {}", userId, friends.size());
            return friends;
        } catch (Exception e) {
            logger.error("[获取好友列表] 异常: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<Friendship> getPendingRequests(Long userId) {
        if (!isValidUserId(userId)) {
            logger.warn("[获取待处理请求] 失败：用户不存在");
            return List.of();
        }
        
        try {
            List<Friendship> requests = friendshipMapper.getPendingRequests(userId);
            logger.debug("[获取待处理请求] 用户 {} 的待处理请求数量: {}", userId, requests.size());
            return requests;
        } catch (Exception e) {
            logger.error("[获取待处理请求] 异常: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public Map<String, Object> getUserInfoByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("[获取用户信息] 失败：用户名为空");
            return null;
        }
        
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.eq("username", username);
            User user = userMapper.selectOne(wrapper);
            
            if (user == null) {
                logger.warn("[获取用户信息] 失败：用户不存在: {}", username);
                return null;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", user.getId());
            result.put("username", user.getUsername());
            result.put("nickname", user.getNickname());
            result.put("avatar", user.getAvatar());
            return result;
        } catch (Exception e) {
            logger.error("[获取用户信息] 异常: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String checkFriendStatus(Long userId, Long targetId) {
        if (!isValidUserId(userId)) {
            logger.warn("[检查好友状态] 失败：用户不存在");
            return "stranger";
        }
        
        if (!isValidUserId(targetId)) {
            logger.warn("[检查好友状态] 失败：目标用户不存在");
            return "stranger";
        }

        if (userId.equals(targetId)) {
            return "self";
        }

        try {
            Friendship f1 = friendshipMapper.findByUserAndFriend(userId, targetId);
            Friendship f2 = friendshipMapper.findByUserAndFriend(targetId, userId);
            
            if (f1 != null && STATUS_ACCEPTED.equals(f1.getStatus())) {
                return "friend";
            }
            if (f2 != null && STATUS_PENDING.equals(f2.getStatus())) {
                return "pending_received";
            }
            if (f1 != null && STATUS_PENDING.equals(f1.getStatus())) {
                return "pending_sent";
            }
            if (f1 != null && STATUS_REJECTED.equals(f1.getStatus())) {
                return "rejected_sent";
            }
            if (f2 != null && STATUS_REJECTED.equals(f2.getStatus())) {
                return "rejected_received";
            }
            return "stranger";
        } catch (Exception e) {
            logger.error("[检查好友状态] 异常: {}", e.getMessage(), e);
            return "stranger";
        }
    }
}