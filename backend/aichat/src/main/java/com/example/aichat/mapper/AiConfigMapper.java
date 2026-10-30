package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * AI 全局配置 Mapper。
 */
@Mapper
public interface AiConfigMapper extends BaseMapper<AiConfig> {

    /**
     * 按配置键查询配置项。
     */
    @Select("SELECT * FROM ai_config WHERE config_key = #{configKey} LIMIT 1")
    AiConfig selectByKey(@Param("configKey") String configKey);
}
