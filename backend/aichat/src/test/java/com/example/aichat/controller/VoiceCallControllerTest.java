package com.example.aichat.controller;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.common.exception.VoiceErrorCode;
import com.example.aichat.common.security.UserPrincipal;
import com.example.aichat.dto.R;
import com.example.aichat.service.voice.VoiceCallService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * VoiceCallController 单元测试。
 * 覆盖 requireUserId 鉴权与返回结构。
 */
class VoiceCallControllerTest {

    private VoiceCallService voiceCallService;
    private VoiceCallController controller;

    @BeforeEach
    void setUp() {
        voiceCallService = mock(VoiceCallService.class);
        controller = new VoiceCallController(voiceCallService);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        UserPrincipal principal = new UserPrincipal(userId, "user-" + userId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void audio_未登录应抛TOKEN_INVALID() {
        // 不设置 SecurityContext
        MockMultipartFile file = new MockMultipartFile("file", "a.webm", "audio/webm", new byte[]{1});
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.audio("sid", file));
        assertEquals(UserErrorCode.TOKEN_INVALID, ex.getErrorCode());
    }

    @Test
    void audio_空文件应抛VOICE_AUDIO_EMPTY() {
        authenticateAs(1L);
        MockMultipartFile empty = new MockMultipartFile("file", "a.webm", "audio/webm", new byte[0]);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.audio("sid", empty));
        assertEquals(VoiceErrorCode.VOICE_AUDIO_EMPTY, ex.getErrorCode());
    }

    @Test
    void audio_成功应调用service并返回结构() {
        authenticateAs(1L);
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("sessionId", "sid");
        serviceResult.put("userText", "你好");
        serviceResult.put("audioBase64", "QUJD");
        when(voiceCallService.processAudio(eq(1L), eq("sid"), any(), eq("a.webm")))
                .thenReturn(serviceResult);

        MockMultipartFile file = new MockMultipartFile("file", "a.webm", "audio/webm", new byte[]{1, 2, 3});
        R<Map<String, Object>> result = controller.audio("sid", file);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("sid", result.getData().get("sessionId"));
        assertEquals("QUJD", result.getData().get("audioBase64"));
        verify(voiceCallService).processAudio(eq(1L), eq("sid"), any(), eq("a.webm"));
    }

    @Test
    void start_登录用户应返回sessionId() {
        authenticateAs(7L);
        when(voiceCallService.startSession(eq(7L), eq(100L))).thenReturn("new-sid");
        Map<String, Object> body = new HashMap<>();
        body.put("characterId", 100L);
        R<Map<String, Object>> result = controller.start(body);
        assertEquals(200, result.getCode());
        assertEquals("new-sid", result.getData().get("sessionId"));
    }
}
