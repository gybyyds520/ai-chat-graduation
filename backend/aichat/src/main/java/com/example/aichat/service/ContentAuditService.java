package com.example.aichat.service;

import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.dto.audit.PageResult;
import com.example.aichat.entity.ContentAuditLog;

import java.util.Map;

/**
 * 内容审核服务：全平台主动审核 + 审核日志记录与统计。
 *
 * 主要写路径在内容入库前调用 {@link #audit}，不通过则由调用方抛出
 * BusinessException(SocialErrorCode.CONTENT_VIOLATION)。
 */
public interface ContentAuditService {

    /**
     * 同步审核文本内容。
     *
     * @param content     待审核文本（可空，空内容视为通过）
     * @param contentType 内容类型（post/comment/private_chat/group_chat/ai_character/...）
     * @param userId     提交用户 ID
     * @return 审核结果（passed / matchedWords / riskLevel / reason）
     */
    AuditResult audit(String content, String contentType, Long userId);

    /**
     * 判断指定用户是否处于"未成年保护模式生效"状态。
     *
     * <p>规则：minorModeEnabled 为 TRUE 时强制生效；为 FALSE 时不生效；
     * 为 null 时按自声明年龄（age &lt; 18）自动判断。用户不存在时返回 false。</p>
     *
     * @param userId 用户 ID（可空，空返回 false）
     * @return 未成年保护模式是否生效
     */
    boolean isMinorUser(Long userId);

    /**
     * 记录一条审核日志（独立事务，即使外层业务回滚也保留违规证据）。
     */
    void recordAuditLog(Long userId, String contentType, Long targetId, String content, AuditResult result);

    /**
     * 管理后台分页查询审核日志。
     */
    PageResult<ContentAuditLog> getAuditLogs(int page, int size, String riskLevel, String contentType);

    /**
     * 审核统计：总审核数、拦截数、各风险等级分布、今日数据。
     */
    Map<String, Object> getAuditStats();
}
