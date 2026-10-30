package com.example.aichat.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 新增实体字段映射测试：UserQuota / ToolCallLog。
 */
class EntityFieldsTest {

    @Test
    void userQuota_字段应可读写() {
        UserQuota q = new UserQuota();
        q.setId(1L);
        q.setUserId(9L);
        q.setFeatureType("ai_chat");
        q.setUsedCount(3);
        q.setLimitCount(20);
        q.setResetDate(LocalDate.now());
        q.setCreatedAt(LocalDateTime.now());

        assertEquals(9L, q.getUserId());
        assertEquals("ai_chat", q.getFeatureType());
        assertEquals(3, q.getUsedCount());
        assertEquals(20, q.getLimitCount());
        assertNotNull(q.getResetDate());
    }

    @Test
    void toolCallLog_字段应可读写() {
        ToolCallLog log = new ToolCallLog();
        log.setUserId(9L);
        log.setToolName("calculator");
        log.setParameters("{}");
        log.setResult("{}");
        log.setSuccess(true);
        log.setDurationMs(12L);
        log.setCreatedAt(LocalDateTime.now());

        assertEquals("calculator", log.getToolName());
        assertTrue(log.getSuccess());
        assertEquals(12L, log.getDurationMs());
    }
}
