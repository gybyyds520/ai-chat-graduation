package com.example.aichat.service.voice.adapter;

import com.example.aichat.service.voice.VoiceProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EdgeTtsAdapter 多音色选择单元测试。
 *
 * 覆盖：getAvailableVoices 返回 ≥5 种音色、切换音色后合成内容不同、未知音色降级默认。
 */
class EdgeTtsAdapterTest {

    private final EdgeTtsAdapter adapter = new EdgeTtsAdapter();

    @Test
    void getAvailableVoices应返回至少5种音色() {
        List<VoiceProfile> voices = adapter.getAvailableVoices();
        assertNotNull(voices);
        assertTrue(voices.size() >= 5, "音色数量应 >= 5，实际: " + voices.size());
    }

    @Test
    void 应覆盖温柔女声活泼可爱等场景() {
        List<VoiceProfile> voices = adapter.getAvailableVoices();
        // 至少包含温柔女声、可爱、活泼等关键音色
        boolean hasFemale = voices.stream().anyMatch(v -> "female".equals(v.getVoiceId()));
        boolean hasCute = voices.stream().anyMatch(v -> "cute".equals(v.getVoiceId()));
        boolean hasYoung = voices.stream().anyMatch(v -> "young".equals(v.getVoiceId()));
        assertTrue(hasFemale, "应包含温柔女声");
        assertTrue(hasCute, "应包含可爱音色");
        assertTrue(hasYoung, "应包含活泼音色");
    }

    @Test
    void 切换音色后合成内容应不同() {
        byte[] warm = adapter.synthesize("你好", "warm");
        byte[] cute = adapter.synthesize("你好", "cute");
        byte[] cold = adapter.synthesize("你好", "cold");
        assertNotNull(warm);
        assertNotNull(cute);
        assertNotNull(cold);
        assertFalse(java.util.Arrays.equals(warm, cute));
        assertFalse(java.util.Arrays.equals(warm, cold));
    }

    @Test
    void 未知音色应降级默认() {
        byte[] a = adapter.synthesize("你好", "nonexistent-voice");
        byte[] b = adapter.synthesize("你好", "default");
        assertArrayEquals(a, b);
    }

    @Test
    void 适配器应恒可用且返回wav格式() {
        assertTrue(adapter.isAvailable());
        assertEquals("edge-tts", adapter.getName());
        assertEquals("wav", adapter.getAudioFormat());
    }
}
