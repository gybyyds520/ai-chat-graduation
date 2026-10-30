package com.example.aichat.service.voice.adapter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MockLlmAdapter 单元测试。
 */
class MockLlmAdapterTest {

    private final MockLlmAdapter adapter = new MockLlmAdapter();

    @Test
    void 默认回复应包含用户文本() {
        String reply = adapter.generateReply("你好", "sys", List.of());
        assertNotNull(reply);
        assertTrue(reply.contains("你好"));
        assertFalse(adapter.isEcho());
    }

    @Test
    void echo模式应原样返回用户文本() {
        adapter.setEcho(true);
        String reply = adapter.generateReply("回声测试", "sys", List.of("hist"));
        assertEquals("回声测试", reply);
        assertTrue(adapter.isEcho());
    }
}
