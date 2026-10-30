package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.ChatImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatImageMapper extends BaseMapper<ChatImage> {
    
    @Select("SELECT * FROM chat_image WHERE session_id = #{sessionId} ORDER BY created_at DESC")
    List<ChatImage> findBySessionId(@Param("sessionId") String sessionId);
    
    @Select("SELECT * FROM chat_image WHERE character_id = #{characterId} ORDER BY created_at DESC LIMIT #{limit}")
    List<ChatImage> findByCharacterId(@Param("characterId") Long characterId, @Param("limit") int limit);
}
