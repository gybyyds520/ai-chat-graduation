package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.UserOnlineStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserOnlineStatusMapper extends BaseMapper<UserOnlineStatus> {
    
    @Select("SELECT * FROM user_online_status WHERE user_id = #{userId}")
    UserOnlineStatus findByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM user_online_status WHERE status = 'online'")
    List<UserOnlineStatus> findOnlineUsers();
    
    @Select("SELECT * FROM user_online_status WHERE user_id IN (#{userIds})")
    List<UserOnlineStatus> findByUserIds(@Param("userIds") List<Long> userIds);
    
    @Update("UPDATE user_online_status SET status = 'offline', last_active_time = #{time} WHERE last_active_time < #{time}")
    int updateOfflineByInactivity(@Param("time") LocalDateTime time);
    
    @Update("UPDATE user_online_status SET status = #{status}, last_active_time = NOW() WHERE user_id = #{userId}")
    int updateStatus(@Param("userId") Long userId, @Param("status") String status);
    
    @Update("UPDATE user_online_status SET session_id = #{sessionId}, last_active_time = NOW() WHERE user_id = #{userId}")
    int updateSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}