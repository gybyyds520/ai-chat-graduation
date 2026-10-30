package com.example.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aichat.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库文档分块 Mapper。
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    /**
     * 按文档 ID 查询全部分块，按分块序号升序。
     */
    @Select("SELECT * FROM knowledge_chunk WHERE document_id = #{documentId} ORDER BY chunk_index ASC")
    List<KnowledgeChunk> selectByDocumentId(@Param("documentId") Long documentId);
}
