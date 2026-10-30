package com.example.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容审核日志。
 *
 * 每次内容审核（无论通过与否）都落一条记录，供管理后台查询与统计。
 */
@Data
@TableName("content_audit_log")
public class ContentAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交内容的用户 ID。 */
    private Long userId;

    /** 内容类型：post/comment/private_chat/group_chat/ai_character/report/ai_generated。 */
    private String contentType;

    /** 被审核内容对象 ID（帖子/评论/消息等，可空）。 */
    private Long targetId;

    /** 审核内容快照（超长截断存储）。 */
    private String contentSnapshot;

    /** 风险等级 LOW / MEDIUM / HIGH。 */
    private String riskLevel;

    /** 命中的敏感词，逗号分隔。 */
    private String matchedWords;

    /** 是否通过。 */
    private Boolean passed;

    private LocalDateTime createdAt;
}
