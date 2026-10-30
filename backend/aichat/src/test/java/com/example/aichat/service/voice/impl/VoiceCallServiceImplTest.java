package com.example.aichat.service.voice.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.VoiceErrorCode;
import com.example.aichat.entity.User;
import com.example.aichat.service.UserService;
import com.example.aichat.service.voice.VoiceAdapterManager;
import com.example.aichat.service.voice.VoiceProfile;
import com.example.aichat.service.voice.adapter.MockAsrAdapter;
import com.example.aichat.service.voice.adapter.MockLlmAdapter;
import com.example.aichat.service.voice.adapter.MockTtsAdapter;
import com.example.aichat.service.voice.adapter.TtsAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * VoiceCallServiceImpl 单元测试（纯 Mockito）。
 * 用真实 Mock 适配器 + Mock VoiceAdapterManager 走通 ASR->LLM->TTS 全链路。
 */
@ExtendWith(MockitoExtension.class)
class VoiceCallServiceImplTest {

    @Mock
    private VoiceAdapterManager adapterManager;

    @Mock
    private UserService userService;

    @InjectMocks
    private VoiceCallServiceImpl service;

    private MockAsrAdapter asr;
    private MockLlmAdapter llm;
    private RecordingTtsAdapter tts;
    private VoiceAdapterManager.Selection selection;

    @BeforeEach
    void setUp() {
        asr = new MockAsrAdapter();
        llm = new MockLlmAdapter();
        tts = new RecordingTtsAdapter();
        selection = new VoiceAdapterManager.Selection(asr, llm, tts,
                Map.of("asr", "mock-asr", "llm", "mock-llm", "tts", "mock-tts",
                        "audioFormat", "wav", "mockMode", true));
        lenient().when(adapterManager.select()).thenReturn(selection);
    }

    @Test
    void startSession_应返回非空sessionId() {
        String sid = service.startSession(1L, 100L);
        assertNotNull(sid);
        assertFalse(sid.isEmpty());
    }

    @Test
    void processAudio_全链路应返回文本与base64音频() {
        String sid = service.startSession(1L, null);
        when(userService.getUserById(1L)).thenReturn(userWithVoice("default"));

        Map<String, Object> result = service.processAudio(1L, sid, new byte[]{1, 2, 3}, "a.webm");

        assertEquals(sid, result.get("sessionId"));
        // ASR 固定文本
        assertEquals(MockAsrAdapter.FIXED_TEXT, result.get("userText"));
        // LLM 回复应包含用户文本
        assertTrue(((String) result.get("aiText")).contains(MockAsrAdapter.FIXED_TEXT));
        // TTS 返回了 base64
        String b64 = (String) result.get("audioBase64");
        assertNotNull(b64);
        assertFalse(b64.isEmpty());
        assertEquals("wav", result.get("audioFormat"));
        // mockMode 透传
        Map<?, ?> info = (Map<?, ?>) result.get("adapterInfo");
        assertEquals(Boolean.TRUE, info.get("mockMode"));
        assertNotNull(result.get("timestamps"));
    }

    @Test
    void processAudio_应把用户音色偏好传给TTS() {
        String sid = service.startSession(1L, null);
        when(userService.getUserById(1L)).thenReturn(userWithVoice("warm"));

        service.processAudio(1L, sid, new byte[]{1}, "a.webm");

        assertEquals("warm", tts.lastVoiceId);
    }

    @Test
    void processAudio_空音频应抛VOICE_AUDIO_EMPTY() {
        String sid = service.startSession(1L, null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.processAudio(1L, sid, new byte[0], "a.webm"));
        assertEquals(VoiceErrorCode.VOICE_AUDIO_EMPTY, ex.getErrorCode());
    }

    @Test
    void processAudio_超大音频应抛VOICE_AUDIO_TOO_LARGE() {
        String sid = service.startSession(1L, null);
        byte[] big = new byte[(int) (VoiceCallServiceImpl.MAX_AUDIO_SIZE + 1)];
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.processAudio(1L, sid, big, "a.webm"));
        assertEquals(VoiceErrorCode.VOICE_AUDIO_TOO_LARGE, ex.getErrorCode());
    }

    @Test
    void processAudio_他人会话应抛UNAUTHORIZED() {
        String sid = service.startSession(1L, null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.processAudio(2L, sid, new byte[]{1}, "a.webm"));
        assertEquals(VoiceErrorCode.VOICE_SESSION_UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void endSession_后再处理应抛NOT_FOUND() {
        String sid = service.startSession(1L, null);
        service.endSession(1L, sid);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.processAudio(1L, sid, new byte[]{1}, "a.webm"));
        assertEquals(VoiceErrorCode.VOICE_SESSION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getSessionStatus_不存在应抛NOT_FOUND() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getSessionStatus(1L, "no-such-session"));
        assertEquals(VoiceErrorCode.VOICE_SESSION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getSessionStatus_他人查询应抛UNAUTHORIZED() {
        String sid = service.startSession(1L, null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getSessionStatus(2L, sid));
        assertEquals(VoiceErrorCode.VOICE_SESSION_UNAUTHORIZED, ex.getErrorCode());
    }

    private User userWithVoice(String voice) {
        User u = new User();
        u.setId(1L);
        u.setVoicePreference(voice);
        return u;
    }

    /** 记录最近一次 voiceId 的 TTS 桩。 */
    static class RecordingTtsAdapter extends MockTtsAdapter {
        String lastVoiceId;
        @Override
        public byte[] synthesize(String text, String voiceId) {
            this.lastVoiceId = voiceId;
            return super.synthesize(text, voiceId);
        }
    }
}
