package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.GroupChatMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupChatMemberMapper extends BaseMapper<GroupChatMember> {
    
    @Select("SELECT gcm.* FROM group_chat_member gcm WHERE gcm.group_id = #{groupId} AND gcm.status = 1")
    List<GroupChatMember> getMembersByGroupId(@Param("groupId") Long groupId);
    
    @Select("SELECT COUNT(*) FROM group_chat_member WHERE group_id = #{groupId} AND user_id = #{userId} AND status = 1")
    int countMemberByGroupAndUser(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    @Select("SELECT gcm.* FROM group_chat_member gcm WHERE gcm.group_id = #{groupId} AND gcm.user_id = #{userId}")
    GroupChatMember findMemberByGroupAndUser(@Param("groupId") Long groupId, @Param("userId") Long userId);
}