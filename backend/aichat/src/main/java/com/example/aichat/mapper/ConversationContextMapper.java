package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.ConversationContext;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ConversationContextMapper extends BaseMapper<ConversationContext> {
    
    @Select("SELECT * FROM conversation_context WHERE session_id = #{sessionId} AND expire_time > NOW()")
    List<ConversationContext> getContextBySessionId(@Param("sessionId") String sessionId);
    
    @Select("SELECT * FROM conversation_context WHERE session_id = #{sessionId} AND context_key = #{contextKey} AND expire_time > NOW()")
    ConversationContext getContextByKey(@Param("sessionId") String sessionId, @Param("contextKey") String contextKey);
    
    @Delete("DELETE FROM conversation_context WHERE expire_time <= #{time}")
    void deleteExpired(@Param("time") LocalDateTime time);
    
    @Delete("DELETE FROM conversation_context WHERE session_id = #{sessionId}")
    void deleteBySessionId(@Param("sessionId") String sessionId);
    
    @Select("SELECT * FROM conversation_context WHERE user_id = #{userId} AND character_id = #{characterId} AND expire_time > NOW()")
    List<ConversationContext> getContextByUserAndCharacter(@Param("userId") Long userId, @Param("characterId") Long characterId);
}