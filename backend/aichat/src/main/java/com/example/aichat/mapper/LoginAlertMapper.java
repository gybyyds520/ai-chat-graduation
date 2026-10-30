package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.LoginAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface LoginAlertMapper extends BaseMapper<LoginAlert> {
    
    @Select("SELECT * FROM login_alert WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<LoginAlert> getAlertsByUserId(@Param("userId") Long userId);
    
    @Update("UPDATE login_alert SET status = 'read' WHERE user_id = #{userId} AND status = 'unread'")
    void markAllAsRead(@Param("userId") Long userId);
}