package com.example.aichat.service.voice;

import com.example.aichat.service.voice.adapter.AsrAdapter;
import com.example.aichat.service.voice.adapter.LlmAdapter;
import com.example.aichat.service.voice.adapter.TtsAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VoiceAdapterManager 选择策略单元测试。
 */
class VoiceAdapterManagerTest {

    /** 可用的"生产"ASR。 */
    private static AsrAdapter prodAsr(boolean available) {
        return new AsrAdapter() {
            @Override public String transcribe(byte[] audio, String fileName) { return "prod"; }
            @Override public boolean isAvailable() { return available; }
            @Override public String getName() { return "prod-asr"; }
        };
    }

    private static LlmAdapter prodLlm(boolean available) {
        return new LlmAdapter() {
            @Override public String generateReply(String u, String s, java.util.List<String> h) { return "prod"; }
            @Override public boolean isAvailable() { return available; }
            @Override public String getName() { return "prod-llm"; }
        };
    }

    private static TtsAdapter prodTts(boolean available) {
        return new TtsAdapter() {
            @Override public byte[] synthesize(String text) { return new byte[]{1}; }
            @Override public boolean isAvailable() { return available; }
            @Override public String getName() { return "prod-tts"; }
            @Override public String getAudioFormat() { return "mp3"; }
        };
    }

    private static TtsAdapter mockTts() {
        return new TtsAdapter() {
            @Override public byte[] synthesize(String text) { return new byte[]{9}; }
            @Override public boolean isAvailable() { return true; }
            @Override public String getName() { return "mock-tts"; }
            @Override public String getAudioFormat() { return "wav"; }
        };
    }

    @Test
    void 生产适配器可用时应优先选择生产() {
        VoiceAdapterManager mgr = new VoiceAdapterManager(
                List.of(prodAsr(true)), List.of(prodLlm(true)), List.of(prodTts(true)));
        VoiceAdapterManager.Selection sel = mgr.select();
        assertEquals("prod-asr", sel.getAsr().getName());
        assertEquals("prod-llm", sel.getLlm().getName());
        assertEquals("prod-tts", sel.getTts().getName());
        assertFalse((Boolean) sel.getAdapterInfo().get("mockMode"));
    }

    @Test
    void 生产适配器不可用时应降级mock() {
        VoiceAdapterManager mgr = new VoiceAdapterManager(
                List.of(prodAsr(false), new MockAsrStub()),
                List.of(prodLlm(false), new MockLlmStub()),
                List.of(prodTts(false), mockTts()));
        VoiceAdapterManager.Selection sel = mgr.select();
        assertEquals("mock-asr", sel.getAsr().getName());
        assertEquals("mock-llm", sel.getLlm().getName());
        assertEquals("mock-tts", sel.getTts().getName());
        assertTrue((Boolean) sel.getAdapterInfo().get("mockMode"));
        assertEquals("wav", sel.getAdapterInfo().get("audioFormat"));
    }

    @Test
    void adapterInfo应包含三件套名称() {
        VoiceAdapterManager mgr = new VoiceAdapterManager(
                List.of(prodAsr(true)), List.of(prodLlm(true)), List.of(prodTts(true)));
        Map<String, Object> info = mgr.select().getAdapterInfo();
        assertEquals("prod-asr", info.get("asr"));
        assertEquals("prod-llm", info.get("llm"));
        assertEquals("prod-tts", info.get("tts"));
    }

    /** 测试用 Mock ASR 桩。 */
    static class MockAsrStub implements AsrAdapter {
        @Override public String transcribe(byte[] audio, String fileName) { return "mock"; }
        @Override public boolean isAvailable() { return true; }
        @Override public String getName() { return "mock-asr"; }
    }

    /** 测试用 Mock LLM 桩。 */
    static class MockLlmStub implements LlmAdapter {
        @Override public String generateReply(String u, String s, java.util.List<String> h) { return "mock"; }
        @Override public boolean isAvailable() { return true; }
        @Override public String getName() { return "mock-llm"; }
    }
}
