package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.FriendGroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendGroupMemberMapper extends BaseMapper<FriendGroupMember> {
    
    @Select("SELECT fgm.* FROM friend_group_member fgm WHERE fgm.group_id = #{groupId}")
    List<FriendGroupMember> getMembersByGroupId(@Param("groupId") Long groupId);
    
    @Select("SELECT fgm.* FROM friend_group_member fgm WHERE fgm.friend_id = #{friendId}")
    List<FriendGroupMember> getGroupsByFriendId(@Param("friendId") Long friendId);
    
    @Select("SELECT fgm.* FROM friend_group_member fgm WHERE fgm.group_id = #{groupId} AND fgm.friend_id = #{friendId}")
    FriendGroupMember findByGroupAndFriend(@Param("groupId") Long groupId, @Param("friendId") Long friendId);
}