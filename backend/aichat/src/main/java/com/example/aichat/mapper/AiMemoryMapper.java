package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.AiMemory;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 记忆 Mapper。
 *
 * 继承 BaseMapper<AiMemory>，CRUD 与分页均由 MyBatis-Plus 提供，
 * 复杂过滤（类型/分类/生效状态）在 Service 层用 QueryWrapper 组装。
 */
@Mapper
public interface AiMemoryMapper extends BaseMapper<AiMemory> {
}
