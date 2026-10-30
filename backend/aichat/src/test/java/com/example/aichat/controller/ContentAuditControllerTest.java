package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.dto.audit.PageResult;
import com.example.aichat.entity.ContentAuditLog;
import com.example.aichat.service.ContentAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ContentAuditController 单元测试（Mockito 隔离 Service）。
 *
 * 覆盖点：
 *   1. 管理员查询审核日志端点返回正确结构；
 *   2. 统计端点返回正确结构；
 *   3. 控制器类级声明 @PreAuthorize("hasRole('ADMIN')")（非管理员访问由 Spring Security 拒绝 403）。
 */
@ExtendWith(MockitoExtension.class)
class ContentAuditControllerTest {

    @Mock
    private ContentAuditService contentAuditService;

    @InjectMocks
    private ContentAuditController controller;

    @Test
    void getLogs_应调用service并返回分页() {
        ContentAuditLog log = new ContentAuditLog();
        log.setRiskLevel("HIGH");
        PageResult<ContentAuditLog> pageResult = PageResult.of(List.of(log), 1L, 1, 20);
        when(contentAuditService.getAuditLogs(1, 20, "HIGH", "post")).thenReturn(pageResult);

        R<PageResult<ContentAuditLog>> resp = controller.getLogs(1, 20, "HIGH", "post");

        assertEquals(200, resp.getCode());
        assertEquals(1L, resp.getData().getTotal());
        assertEquals(1, resp.getData().getRecords().size());
    }

    @Test
    void getStats_应调用service并返回统计() {
        Map<String, Object> stats = Map.of("total", 50, "blocked", 5, "todayTotal", 3);
        when(contentAuditService.getAuditStats()).thenReturn(stats);

        R<Map<String, Object>> resp = controller.getStats();

        assertEquals(200, resp.getCode());
        assertEquals(50, resp.getData().get("total"));
    }

    @Test
    void 控制器应声明仅管理员可访问() {
        // 非管理员访问由 Spring Security 在类级 @PreAuthorize 处拒绝（403）
        PreAuthorize pa = ContentAuditController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(pa, "ContentAuditController 必须声明 @PreAuthorize");
        String expr = pa.value();
        assertNotNull(expr, "必须配置 hasRole ADMIN");
        assertTrue(expr.contains("hasRole"), "类级 @PreAuthorize 应要求 ADMIN 角色");
    }
}
