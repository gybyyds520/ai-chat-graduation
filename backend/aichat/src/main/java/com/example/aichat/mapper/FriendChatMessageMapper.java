package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.FriendChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendChatMessageMapper extends BaseMapper<FriendChatMessage> {
    @Select("SELECT f.*, su.username as senderName, su.nickname as senderNickname, " +
            "ru.username as receiverName, ru.nickname as receiverNickname " +
            "FROM friend_chat_message f " +
            "JOIN `user` su ON f.sender_id = su.id " +
            "JOIN `user` ru ON f.receiver_id = ru.id " +
            "WHERE (f.sender_id = #{userId} AND f.receiver_id = #{friendId}) " +
            "OR (f.sender_id = #{friendId} AND f.receiver_id = #{userId}) " +
            "ORDER BY f.create_time DESC LIMIT #{limit}")
    List<FriendChatMessage> getChatHistory(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("limit") int limit);
    
    @Select("SELECT COUNT(*) FROM friend_chat_message " +
            "WHERE receiver_id = #{userId} AND status = 0")
    int countUnreadMessages(@Param("userId") Long userId);
    
    @Select("UPDATE friend_chat_message SET status = 1 " +
            "WHERE receiver_id = #{userId} AND sender_id = #{friendId} AND status = 0")
    void markMessagesAsRead(@Param("userId") Long userId, @Param("friendId") Long friendId);
    
    @Select("${sql}")
    List<FriendChatMessage> selectBySql(@Param("sql") String sql);
    
    @Select("${sql}")
    Integer selectCountBySql(@Param("sql") String sql);
}