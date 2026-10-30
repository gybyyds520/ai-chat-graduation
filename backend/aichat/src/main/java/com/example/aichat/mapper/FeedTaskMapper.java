package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.FeedTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * AI 投喂任务 Mapper - 提供按状态分组统计的真实数据查询。
 */
@Mapper
public interface FeedTaskMapper extends BaseMapper<FeedTask> {

    /**
     * 统计指定用户某状态的任务数。userId 为 null 时统计全部。
     */
    @Select("SELECT COUNT(*) FROM feed_task WHERE status = #{status} AND (#{userId} IS NULL OR user_id = #{userId})")
    int countByStatus(@Param("status") String status, @Param("userId") Long userId);

    /**
     * 查询指定用户最近的任务列表。
     */
    @Select("SELECT * FROM feed_task WHERE (#{userId} IS NULL OR user_id = #{userId}) ORDER BY created_at DESC LIMIT #{limit}")
    List<FeedTask> findRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询指定用户最近一条处理中的任务(用于展示"当前处理任务")。
     */
    @Select("SELECT * FROM feed_task WHERE user_id = #{userId} AND status = 'processing' ORDER BY created_at DESC LIMIT 1")
    FeedTask findProcessingByUser(@Param("userId") Long userId);

    /**
     * 按内容类型统计任务分布（byType）。返回 [content_type, cnt] 行。
     */
    @Select("SELECT content_type AS content_type, COUNT(*) AS cnt FROM feed_task WHERE (#{userId} IS NULL OR user_id = #{userId}) GROUP BY content_type")
    List<Map<String, Object>> countByType(@Param("userId") Long userId);

    /**
     * 统计指定用户投喂任务的平均 effectiveness（无记录时返回 null）。
     */
    @Select("SELECT AVG(effectiveness) FROM feed_task WHERE user_id = #{userId} AND effectiveness IS NOT NULL")
    Double avgEffectiveness(@Param("userId") Long userId);
}
