package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库问答记录实体（可选审计/历史）。
 *
 * 对应表 knowledge_query，记录一次基于文档的问答：用户问题、抽取式回答、
 * 引用的分块 ID 列表（JSON 数组字符串）。
 */
@Data
@TableName("knowledge_query")
public class KnowledgeQuery {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 ID */
    private Long userId;

    /** 提问所基于的文档 ID */
    private Long documentId;

    /** 用户问题 */
    private String query;

    /** 回答内容（当前为抽取式） */
    private String answer;

    /** 引用的分块 ID 列表，JSON 数组字符串，如 "[12,15,18]" */
    private String sourceChunks;

    private LocalDateTime createdAt;
}
