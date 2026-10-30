package com.example.aichat.service;

import com.example.aichat.entity.Friendship;
import com.example.aichat.entity.User;

import java.util.List;
import java.util.Map;

public interface FriendshipService {
    boolean sendFriendRequest(Long userId, Long friendId);
    boolean acceptFriendRequest(Long userId, Long requesterId);
    boolean rejectFriendRequest(Long userId, Long requesterId);
    boolean removeFriend(Long userId, Long friendId);
    List<Friendship> getFriendList(Long userId);
    List<Friendship> getPendingRequests(Long userId);
    Map<String, Object> getUserInfoByUsername(String username);
    String checkFriendStatus(Long userId, Long targetId);
}