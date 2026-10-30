package com.example.aichat.service;

import com.example.aichat.entity.CharacterShare;

import java.util.List;

public interface CharacterShareService {
    
    CharacterShare shareWithUser(Long characterId, Long fromUserId, Long toUserId, String permissions);
    
    boolean unshareWithUser(Long characterId, Long toUserId);
    
    boolean revokeAllShares(Long characterId);
    
    List<CharacterShare> getSharedToUsers(Long characterId);
    
    List<CharacterShare> getSharedFromUsers(Long userId);
    
    List<CharacterShare> getSharedWithMe(Long userId);
    
    boolean hasAccess(Long userId, Long characterId);
    
    String getPermissions(Long userId, Long characterId);
    
    boolean hasPermission(Long userId, Long characterId, String permission);
    
    boolean isOwner(Long userId, Long characterId);
}