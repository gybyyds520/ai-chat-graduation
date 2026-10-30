package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiSoulExperience;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 灵魂经历流水 Mapper。
 *
 * 继承 BaseMapper<AiSoulExperience>，CRUD 与分页均由 MyBatis-Plus 提供。
 */
@Mapper
public interface AiSoulExperienceMapper extends BaseMapper<AiSoulExperience> {
}
