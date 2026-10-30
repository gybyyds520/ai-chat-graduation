package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiSoulState;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 灵魂状态 Mapper。
 *
 * 继承 BaseMapper<AiSoulState>，CRUD 与分页均由 MyBatis-Plus 提供；
 * 归属过滤在 Service 层用 QueryWrapper 组装（杜绝横向越权）。
 */
@Mapper
public interface AiSoulStateMapper extends BaseMapper<AiSoulState> {
}
