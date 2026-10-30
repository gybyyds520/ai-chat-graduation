package com.example.aichat.service.impl;

import com.example.aichat.entity.FriendGroup;
import com.example.aichat.entity.FriendGroupMember;
import com.example.aichat.mapper.FriendGroupMapper;
import com.example.aichat.mapper.FriendGroupMemberMapper;
import com.example.aichat.service.FriendGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FriendGroupServiceImpl implements FriendGroupService {

    private static final Logger logger = LoggerFactory.getLogger(FriendGroupServiceImpl.class);
    
    private final FriendGroupMapper friendGroupMapper;
    private final FriendGroupMemberMapper friendGroupMemberMapper;

    public FriendGroupServiceImpl(FriendGroupMapper friendGroupMapper,
                                  FriendGroupMemberMapper friendGroupMemberMapper) {
        this.friendGroupMapper = friendGroupMapper;
        this.friendGroupMemberMapper = friendGroupMemberMapper;
    }

    @Override
    public FriendGroup createGroup(Long userId, String name) {
        FriendGroup existing = friendGroupMapper.findByName(userId, name);
        if (existing != null) {
            return existing;
        }
        
        FriendGroup group = new FriendGroup();
        group.setUserId(userId);
        group.setName(name);
        group.setSortOrder(0);
        group.setCreatedAt(LocalDateTime.now());
        
        friendGroupMapper.insert(group);
        logger.info("[好友分组] 用户 {} 创建分组: {}", userId, name);
        return group;
    }

    @Override
    public FriendGroup getGroupById(Long groupId) {
        return friendGroupMapper.selectById(groupId);
    }

    @Override
    public List<FriendGroup> getGroupsByUserId(Long userId) {
        return friendGroupMapper.getGroupsByUserId(userId);
    }

    @Override
    public FriendGroup updateGroup(Long groupId, String name) {
        FriendGroup group = friendGroupMapper.selectById(groupId);
        if (group != null) {
            group.setName(name);
            friendGroupMapper.updateById(group);
        }
        return group;
    }

    @Override
    public void deleteGroup(Long groupId) {
        friendGroupMemberMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FriendGroupMember>()
                .eq("group_id", groupId)
        );
        friendGroupMapper.deleteById(groupId);
        logger.info("[好友分组] 删除分组: {}", groupId);
    }

    @Override
    public void addMember(Long groupId, Long friendId, String remark) {
        FriendGroupMember existing = friendGroupMemberMapper.findByGroupAndFriend(groupId, friendId);
        if (existing != null) {
            if (remark != null) {
                existing.setRemark(remark);
                friendGroupMemberMapper.updateById(existing);
            }
            return;
        }
        
        FriendGroupMember member = new FriendGroupMember();
        member.setGroupId(groupId);
        member.setFriendId(friendId);
        member.setRemark(remark);
        member.setCreatedAt(LocalDateTime.now());
        
        friendGroupMemberMapper.insert(member);
        logger.info("[好友分组] 添加好友到分组 - groupId: {}, friendId: {}", groupId, friendId);
    }

    @Override
    public void removeMember(Long groupId, Long friendId) {
        friendGroupMemberMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FriendGroupMember>()
                .eq("group_id", groupId)
                .eq("friend_id", friendId)
        );
    }

    @Override
    public List<FriendGroupMember> getMembersByGroupId(Long groupId) {
        return friendGroupMemberMapper.getMembersByGroupId(groupId);
    }

    @Override
    public void updateRemark(Long groupId, Long friendId, String remark) {
        FriendGroupMember member = friendGroupMemberMapper.findByGroupAndFriend(groupId, friendId);
        if (member != null) {
            member.setRemark(remark);
            friendGroupMemberMapper.updateById(member);
        }
    }
}