package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.ConversationSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConversationSummaryMapper extends BaseMapper<ConversationSummary> {
    
    @Select("SELECT * FROM conversation_summary WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT 1")
    ConversationSummary getLatestBySessionId(@Param("sessionId") String sessionId);
    
    @Select("SELECT * FROM conversation_summary WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<ConversationSummary> getByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}