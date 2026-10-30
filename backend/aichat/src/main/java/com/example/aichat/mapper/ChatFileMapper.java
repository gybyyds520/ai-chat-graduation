package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.ChatFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatFileMapper extends BaseMapper<ChatFile> {
    
    @Select("SELECT * FROM chat_file WHERE session_id = #{sessionId} ORDER BY created_at DESC")
    List<ChatFile> findBySessionId(@Param("sessionId") String sessionId);
}
