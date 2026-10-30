package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiMemorySetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 记忆全局开关 Mapper。每用户一行，CRUD 由 MyBatis-Plus 提供。
 */
@Mapper
public interface AiMemorySettingMapper extends BaseMapper<AiMemorySetting> {
}
