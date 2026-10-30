package com.example.aichat.service;

import com.example.aichat.entity.FriendGroup;
import com.example.aichat.entity.FriendGroupMember;

import java.util.List;

public interface FriendGroupService {
    
    FriendGroup createGroup(Long userId, String name);
    
    FriendGroup getGroupById(Long groupId);
    
    List<FriendGroup> getGroupsByUserId(Long userId);
    
    FriendGroup updateGroup(Long groupId, String name);
    
    void deleteGroup(Long groupId);
    
    void addMember(Long groupId, Long friendId, String remark);
    
    void removeMember(Long groupId, Long friendId);
    
    List<FriendGroupMember> getMembersByGroupId(Long groupId);
    
    void updateRemark(Long groupId, Long friendId, String remark);
}