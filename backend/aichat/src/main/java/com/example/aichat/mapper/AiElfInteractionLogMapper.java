package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiElfInteractionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * AI 精灵互动日志 Mapper。
 */
@Mapper
public interface AiElfInteractionLogMapper extends BaseMapper<AiElfInteractionLog> {

    /**
     * 统计某精灵自 since 时刻起的互动次数（用于"今日互动次数"与每日奖励幂等判断）。
     */
    @Select("SELECT COUNT(*) FROM ai_elf_interaction_log WHERE elf_id = #{elfId} AND created_at >= #{since}")
    long countSince(@Param("elfId") Long elfId, @Param("since") LocalDateTime since);

    /**
     * 统计某精灵自 since 时刻起的指定类型互动次数（用于每日奖励幂等：首次互动加好感）。
     */
    @Select("SELECT COUNT(*) FROM ai_elf_interaction_log WHERE elf_id = #{elfId} AND interaction_type = #{type} AND created_at >= #{since}")
    long countByTypeSince(@Param("elfId") Long elfId, @Param("type") String type, @Param("since") LocalDateTime since);
}
