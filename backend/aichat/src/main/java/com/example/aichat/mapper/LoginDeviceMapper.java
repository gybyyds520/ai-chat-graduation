package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.LoginDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LoginDeviceMapper extends BaseMapper<LoginDevice> {
    
    @Select("SELECT * FROM login_device WHERE user_id = #{userId} ORDER BY last_login_time DESC")
    List<LoginDevice> getDevicesByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM login_device WHERE user_id = #{userId} AND device_id = #{deviceId}")
    LoginDevice findByDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);
}