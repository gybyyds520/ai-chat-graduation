package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {
    
    @Select("SELECT * FROM group_member WHERE group_id = #{groupId}")
    List<GroupMember> getMembersByGroupId(@Param("groupId") Long groupId);
    
    @Select("SELECT * FROM group_member WHERE group_id = #{groupId} AND user_id = #{userId}")
    GroupMember findMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
    
    @Select("SELECT * FROM group_member WHERE user_id = #{userId}")
    List<GroupMember> getMembersByUserId(@Param("userId") Long userId);
}