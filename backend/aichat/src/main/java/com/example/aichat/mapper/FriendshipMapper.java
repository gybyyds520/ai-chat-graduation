package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.Friendship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {
    /**
     * 好友列表：显式列名避免 friendship 表冗余列(username/nickname/avatar 为 NULL)
     * 与 JOIN 列同名时 MyBatis 映射到 NULL 的问题。
     */
    @Select("SELECT f.id, f.user_id, f.friend_id, f.status, f.create_time, f.accept_time, " +
            "u.username, u.nickname, u.avatar FROM friendship f " +
            "JOIN user u ON f.friend_id = u.id WHERE f.user_id = #{userId} AND f.status = 'accepted'")
    List<Friendship> getFriendList(@Param("userId") Long userId);

    /**
     * 待处理请求（发起方为 friend_id 侧）：同上去除 f.* 同名列冲突。
     */
    @Select("SELECT f.id, f.user_id, f.friend_id, f.status, f.create_time, f.accept_time, " +
            "u.username, u.nickname, u.avatar FROM friendship f " +
            "JOIN user u ON f.user_id = u.id WHERE f.friend_id = #{userId} AND f.status = 'pending'")
    List<Friendship> getPendingRequests(@Param("userId") Long userId);
    
    @Select("SELECT * FROM friendship WHERE user_id = #{userId} AND friend_id = #{friendId}")
    Friendship findByUserAndFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);
}