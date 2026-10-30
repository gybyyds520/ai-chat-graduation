package com.example.aichat.service.voice.adapter;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MockTtsAdapter 单元测试（含多音色区分）。
 */
class MockTtsAdapterTest {

    private final MockTtsAdapter adapter = new MockTtsAdapter();

    @Test
    void synthesize_应生成合法WAV头() {
        byte[] wav = adapter.synthesize("你好");
        assertNotNull(wav);
        assertTrue(wav.length > 44);
        // RIFF 魔数
        assertEquals("RIFF", new String(wav, 0, 4, StandardCharsets.US_ASCII));
        // WAVE
        assertEquals("WAVE", new String(wav, 8, 4, StandardCharsets.US_ASCII));
        assertEquals("wav", adapter.getAudioFormat());
        assertTrue(adapter.isAvailable());
    }

    @Test
    void 不同音色应返回不同字节内容() {
        byte[] defaultVoice = adapter.synthesize("文本", "default");
        byte[] warmVoice = adapter.synthesize("文本", "warm");
        byte[] coldVoice = adapter.synthesize("文本", "cold");
        // 三者互不等同
        assertFalse(java.util.Arrays.equals(defaultVoice, warmVoice));
        assertFalse(java.util.Arrays.equals(warmVoice, coldVoice));
        assertFalse(java.util.Arrays.equals(defaultVoice, coldVoice));
    }

    @Test
    void 未知音色应降级为default且内容与default一致() {
        byte[] a = adapter.synthesize("文本", "not-exist");
        byte[] b = adapter.synthesize("文本", "default");
        assertArrayEquals(a, b);
        assertTrue(adapter.isAvailable());
        assertEquals("mock-tts", adapter.getName());
    }
}
