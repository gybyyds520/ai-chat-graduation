package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.GroupChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupChatMessageMapper extends BaseMapper<GroupChatMessage> {
    
    @Select("SELECT gcm.*, u.nickname as senderNickname FROM group_chat_message gcm " +
            "JOIN `user` u ON gcm.sender_id = u.id " +
            "WHERE gcm.group_id = #{groupId} " +
            "ORDER BY gcm.create_time DESC LIMIT #{limit}")
    List<GroupChatMessage> getGroupChatHistory(@Param("groupId") Long groupId, @Param("limit") int limit);
    
    @Select("SELECT COUNT(*) FROM group_chat_message WHERE group_id = #{groupId} AND status = 0")
    int countUnreadMessages(@Param("groupId") Long groupId);
}