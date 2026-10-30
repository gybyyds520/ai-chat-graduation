package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.dto.audit.PageResult;
import com.example.aichat.entity.ContentAuditLog;
import com.example.aichat.service.ContentAuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 内容审核管理后台端点。
 *
 * 全部仅管理员可访问（@PreAuthorize hasRole('ADMIN')）。
 * 敏感词库第一版为代码常量，故仅提供日志查询与统计，不做词库增删改。
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
public class ContentAuditController {

    private final ContentAuditService contentAuditService;

    public ContentAuditController(ContentAuditService contentAuditService) {
        this.contentAuditService = contentAuditService;
    }

    /** 分页查询审核日志，可按风险等级/内容类型过滤。 */
    @GetMapping("/logs")
    public R<PageResult<ContentAuditLog>> getLogs(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) String riskLevel,
                                                 @RequestParam(required = false) String contentType) {
        return R.success(contentAuditService.getAuditLogs(page, size, riskLevel, contentType));
    }

    /** 审核统计：总数/拦截数/风险分布/今日数据。 */
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.success(contentAuditService.getAuditStats());
    }
}
