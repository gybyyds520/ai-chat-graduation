package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.KnowledgeQuery;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库问答记录 Mapper。
 */
@Mapper
public interface KnowledgeQueryMapper extends BaseMapper<KnowledgeQuery> {
}
