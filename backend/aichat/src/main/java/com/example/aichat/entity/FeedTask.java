package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 投喂任务记录 - 对应 feed_task 表。
 * 替代 FeedController 之前的 Mock 返回数据，状态/统计均来自真实持久化记录。
 */
@Data
@TableName("feed_task")
public class FeedTask {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private String taskId;

    @TableField("user_id")
    private Long userId;

    /** 内容类型: text/file/image */
    @TableField("content_type")
    private String contentType;

    /** 文本内容(text类型) */
    private String content;

    /** 上传文件/图片路径(file/image类型) */
    @TableField("file_path")
    private String filePath;

    /** 原始文件名 */
    @TableField("file_name")
    private String fileName;

    /** 多文件路径(逗号分隔)，前端一次提交多个文件时使用 */
    @TableField("file_paths")
    private String filePaths;

    /** 多原始文件名(逗号分隔) */
    @TableField("file_names")
    private String fileNames;

    /** 多图片路径(逗号分隔) */
    @TableField("image_paths")
    private String imagePaths;

    /** 多图片原始名(逗号分隔) */
    @TableField("image_names")
    private String imageNames;

    /** 关联知识库文档 ID，null 表示未入库（图片/不支持类型/入库失败） */
    @TableField("document_id")
    private Long documentId;

    /** 提取的知识点（分块）数量，用于 feedback */
    @TableField("knowledge_points")
    private Integer knowledgePoints;

    /** 优先级: low/medium/high/urgent */
    private String priority;

    /** 处理模式: fast/standard/deep */
    @TableField("process_mode")
    private String processMode;

    /** 标签(逗号分隔) */
    private String tags;

    /** 备注 */
    private String remark;

    /** 状态: pending/processing/completed/failed */
    private String status;

    /** 进度百分比 */
    private Integer progress;

    /** 学习效果百分比 */
    private Integer effectiveness;

    /** 处理耗时(毫秒) */
    private Long duration;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;
}
