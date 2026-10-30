package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档元数据实体。
 *
 * 对应表 knowledge_document，记录用户上传的原始文档及其解析状态。
 * status 取值：pending（待解析）/ processing（解析中）/ ready（就绪可问答）/ failed（解析失败）。
 */
@Data
@TableName("knowledge_document")
public class KnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 ID（IDOR 防护：仅本人可访问） */
    private Long userId;

    /** 原始文件名 */
    private String fileName;

    /** 文件类型：txt / pdf / docx（md 按 txt 处理） */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 服务器存储绝对路径 */
    private String filePath;

    /** 文档标题（用户可指定，缺省取文件名） */
    private String title;

    /** 分块数量 */
    private Integer chunkCount;

    /** 解析状态：pending / processing / ready / failed */
    private String status;

    /** 失败时的错误信息 */
    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
