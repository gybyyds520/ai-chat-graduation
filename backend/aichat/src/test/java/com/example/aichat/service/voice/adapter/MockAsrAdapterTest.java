package com.example.aichat.service.voice.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MockAsrAdapter 单元测试。
 */
class MockAsrAdapterTest {

    private final MockAsrAdapter adapter = new MockAsrAdapter();

    @Test
    void transcribe_应返回固定文本() {
        String text = adapter.transcribe(new byte[]{1, 2, 3}, "a.webm");
        assertEquals(MockAsrAdapter.FIXED_TEXT, text);
    }

    @Test
    void isAvailable恒为true且名称正确() {
        assertTrue(adapter.isAvailable());
        assertEquals("mock-asr", adapter.getName());
    }
}
