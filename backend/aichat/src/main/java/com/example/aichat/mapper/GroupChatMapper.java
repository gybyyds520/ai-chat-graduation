package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.GroupChat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupChatMapper extends BaseMapper<GroupChat> {
    
    @Select("SELECT gc.* FROM group_chat gc JOIN group_member gm ON gc.id = gm.group_id WHERE gm.user_id = #{userId}")
    List<GroupChat> getGroupsByUserId(@Param("userId") Long userId);
    
    @Select("SELECT gc.* FROM group_chat gc WHERE gc.is_public = true ORDER BY gc.member_count DESC")
    List<GroupChat> getPublicGroups();
}