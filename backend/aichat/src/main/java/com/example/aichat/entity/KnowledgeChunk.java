package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档分块实体。
 *
 * 对应表 knowledge_chunk，每个文档按滑动窗口切分为多个分块，
 * 检索时以分块为单位计算相似度并注入上下文。
 */
@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属文档 ID */
    private Long documentId;

    /** 归属用户 ID（冗余，便于按用户隔离检索与删除） */
    private Long userId;

    /** 分块序号（从 0 开始连续递增） */
    private Integer chunkIndex;

    /** 分块文本内容 */
    private String content;

    /** 估算 token 数（可选，保留扩展字段） */
    private Integer tokenCount;

    private LocalDateTime createdAt;
}
