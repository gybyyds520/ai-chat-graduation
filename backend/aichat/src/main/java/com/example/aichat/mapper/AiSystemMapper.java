package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiSystem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * AI系统Mapper
 */
@Mapper
public interface AiSystemMapper extends BaseMapper<AiSystem> {
    
    @Select("SELECT * FROM ai_system WHERE api_key = #{apiKey} AND status = 1")
    AiSystem selectByApiKey(String apiKey);
    
    @Select("SELECT * FROM ai_system WHERE system_name = #{systemName}")
    AiSystem selectBySystemName(String systemName);
}
