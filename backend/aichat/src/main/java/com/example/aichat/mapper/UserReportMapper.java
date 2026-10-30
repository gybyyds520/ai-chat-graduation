package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.UserReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserReportMapper extends BaseMapper<UserReport> {
    
    @Select("SELECT * FROM user_report WHERE status = #{status} ORDER BY created_at DESC")
    List<UserReport> getReportsByStatus(@Param("status") String status);
    
    @Select("SELECT * FROM user_report WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<UserReport> getReportsByUser(@Param("userId") Long userId);
    
    @Select("SELECT * FROM user_report WHERE target_type = #{targetType} AND target_id = #{targetId}")
    List<UserReport> getReportsByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Select("SELECT COUNT(1) FROM user_report WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId} AND status = 'PENDING'")
    int countPendingReport(@Param("userId") Long userId, @Param("targetType") String targetType, @Param("targetId") Long targetId);
    
    @Update("UPDATE user_report SET status = #{status}, review_result = #{reviewResult}, reviewed_at = #{reviewedAt} WHERE id = #{reportId}")
    int reviewReport(@Param("reportId") Long reportId, @Param("status") String status, @Param("reviewResult") String reviewResult, @Param("reviewedAt") LocalDateTime reviewedAt);
}