package com.example.aichat.dto.audit;

import lombok.Data;

/**
 * 审核日志分页查询参数（管理后台）。
 */
@Data
public class AuditLogQuery {

    /** 页码，从 1 开始。 */
    private int page = 1;

    /** 每页大小。 */
    private int size = 20;

    /** 可选：按风险等级过滤（LOW / MEDIUM / HIGH）。 */
    private String riskLevel;

    /** 可选：按内容类型过滤（post/comment/private_chat/group_chat/ai_character/report ...）。 */
    private String contentType;
}
