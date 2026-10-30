package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    
    @Select("SELECT n.* FROM notification n WHERE n.user_id = #{userId} AND n.is_deleted = FALSE ORDER BY n.created_at DESC LIMIT #{limit}")
    List<Notification> getNotifications(@Param("userId") Long userId, @Param("limit") int limit);
    
    @Select("SELECT COUNT(*) FROM notification n WHERE n.user_id = #{userId} AND n.is_read = FALSE AND n.is_deleted = FALSE")
    int countUnread(@Param("userId") Long userId);
    
    @Update("UPDATE notification SET is_read = TRUE WHERE user_id = #{userId}")
    int markAllAsRead(@Param("userId") Long userId);
    
    @Update("UPDATE notification SET is_read = TRUE WHERE id = #{notificationId}")
    int markAsRead(@Param("notificationId") Long notificationId);

    @Update("UPDATE notification SET is_deleted = TRUE WHERE user_id = #{userId} AND is_deleted = FALSE")
    int clearAllByUserId(@Param("userId") Long userId);
}