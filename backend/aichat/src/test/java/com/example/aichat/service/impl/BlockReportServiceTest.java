package com.example.aichat.service.impl;

import com.example.aichat.entity.UserBlock;
import com.example.aichat.entity.UserReport;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.mapper.UserBlockMapper;
import com.example.aichat.mapper.UserReportMapper;
import com.example.aichat.service.ContentAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BlockReportService 单元测试（Mockito 隔离）。
 *
 * 覆盖点：
 *   1. blockUser：不能拉黑自己；
 *   2. blockUser：null 参数返回 null（原实现 userId.equals NPE）；
 *   3. blockUser：已拉黑幂等返回既有记录；
 *   4. blockUser：正常拉黑落库；
 *   5. createReport：正常创建举报记录。
 */
@ExtendWith(MockitoExtension.class)
class BlockReportServiceTest {

    @Mock
    private UserBlockMapper userBlockMapper;
    @Mock
    private UserReportMapper userReportMapper;
    @Mock
    private ContentAuditService contentAuditService;

    @InjectMocks
    private BlockReportServiceImpl blockReportService;

    @Test
    void blockUser_自己拉黑自己应返回null() {
        UserBlock result = blockReportService.blockUser(1L, 1L);
        assertNull(result);
        verify(userBlockMapper, never()).insert(any(UserBlock.class));
    }

    @Test
    void blockUser_null参数应返回null不抛NPE() {
        UserBlock result = blockReportService.blockUser(null, 2L);
        assertNull(result);
        verifyNoInteractions(userBlockMapper);
    }

    @Test
    void blockUser_已拉黑应幂等返回既有记录() {
        UserBlock existing = new UserBlock();
        existing.setUserId(1L);
        existing.setBlockedUserId(2L);
        when(userBlockMapper.findBlock(1L, 2L)).thenReturn(existing);

        UserBlock result = blockReportService.blockUser(1L, 2L);

        assertSame(existing, result);
        verify(userBlockMapper, never()).insert(any(UserBlock.class));
    }

    @Test
    void blockUser_正常拉黑应落库() {
        when(userBlockMapper.findBlock(1L, 2L)).thenReturn(null);

        UserBlock result = blockReportService.blockUser(1L, 2L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(2L, result.getBlockedUserId());
        verify(userBlockMapper).insert(any(UserBlock.class));
    }

    @Test
    void createReport_正常应落库并置PENDING() {
        when(contentAuditService.audit(any(), any(), any())).thenReturn(AuditResult.pass());

        UserReport result = blockReportService.createReport(1L, "user", 2L, "垃圾内容");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("PENDING", result.getStatus());
        verify(userReportMapper).insert(any(UserReport.class));
        // 举报闭环整合：举报理由应做一次审核并记录日志
        verify(contentAuditService).audit(eq("垃圾内容"), eq("report"), eq(1L));
        verify(contentAuditService).recordAuditLog(eq(1L), eq("report"), eq(2L), eq("垃圾内容"), any(AuditResult.class));
    }
}
