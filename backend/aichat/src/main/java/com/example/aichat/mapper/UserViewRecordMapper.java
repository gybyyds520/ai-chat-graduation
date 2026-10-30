package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.UserViewRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserViewRecordMapper extends BaseMapper<UserViewRecord> {
    
    @Select("SELECT * FROM user_view_record WHERE user_id = #{userId} AND target_type = #{targetType}")
    List<UserViewRecord> getViewedByUserAndType(@Param("userId") Long userId, @Param("targetType") String targetType);
    
    @Select("SELECT * FROM user_view_record WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    UserViewRecord findByUserAndTarget(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);
    
    @Update("UPDATE user_view_record SET view_count = view_count + 1, last_view_time = #{lastViewTime} WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    int incrementViewCount(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId, @Param("lastViewTime") LocalDateTime lastViewTime);
}