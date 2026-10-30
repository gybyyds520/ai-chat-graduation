package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.UserBlock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserBlockMapper extends BaseMapper<UserBlock> {
    
    @Select("SELECT * FROM user_block WHERE user_id = #{userId}")
    List<UserBlock> getBlockedUsers(@Param("userId") Long userId);
    
    @Select("SELECT * FROM user_block WHERE user_id = #{userId} AND blocked_user_id = #{blockedUserId}")
    UserBlock findBlock(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);
    
    @Select("SELECT COUNT(*) FROM user_block WHERE user_id = #{userId} AND blocked_user_id = #{blockedUserId}")
    int isBlocked(@Param("userId") Long userId, @Param("blockedUserId") Long blockedUserId);
}