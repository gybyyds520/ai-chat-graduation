package com.example.aichat.controller;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.VoiceErrorCode;
import com.example.aichat.common.security.AuthContext;
import com.example.aichat.dto.R;
import com.example.aichat.service.voice.VoiceCallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 实时语音通话控制器（半双工轮次管道）。
 *
 * 全部端点 requireUserId 鉴权；会话所有权校验在 Service 层完成（IDOR 防护）。
 * 音频上传复用既有 10MB 限制。
 */
@RestController
@RequestMapping("/api/voice-call")
public class VoiceCallController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceCallController.class);

    private final VoiceCallService voiceCallService;

    public VoiceCallController(VoiceCallService voiceCallService) {
        this.voiceCallService = voiceCallService;
    }

    /**
     * 开启语音会话。
     * POST /api/voice-call/start  body: { characterId?: Long }
     */
    @PostMapping("/start")
    public R<Map<String, Object>> start(@RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthContext.requireUserId();
        Long characterId = null;
        if (body != null && body.get("characterId") != null) {
            try {
                characterId = Long.valueOf(String.valueOf(body.get("characterId")));
            } catch (NumberFormatException ignored) {
                // 非法 characterId 忽略，按无角色处理
            }
        }
        String sessionId = voiceCallService.startSession(userId, characterId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        return R.success(data);
    }

    /**
     * 上传一段录音，走 ASR -> LLM -> TTS 全链路。
     * POST /api/voice-call/{sessionId}/audio  multipart: file
     */
    @PostMapping("/{sessionId}/audio")
    public R<Map<String, Object>> audio(@PathVariable String sessionId,
                                        @RequestParam("file") MultipartFile file) {
        Long userId = AuthContext.requireUserId();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(VoiceErrorCode.VOICE_AUDIO_EMPTY);
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new BusinessException(VoiceErrorCode.VOICE_AUDIO_TOO_LARGE);
        }
        try {
            byte[] audio = file.getBytes();
            Map<String, Object> result = voiceCallService.processAudio(
                    userId, sessionId, audio, file.getOriginalFilename());
            return R.success(result);
        } catch (IOException e) {
            logger.error("读取语音通话音频失败", e);
            throw new BusinessException(VoiceErrorCode.VOICE_AUDIO_EMPTY);
        }
    }

    /**
     * 文本轮次：跳过 ASR，直接 LLM -> TTS。
     * POST /api/voice-call/{sessionId}/text  body: { text: string }
     * 供前端浏览器语音识别（Web Speech API）转写文本后调用。
     */
    @PostMapping("/{sessionId}/text")
    public R<Map<String, Object>> text(@PathVariable String sessionId,
                                       @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthContext.requireUserId();
        String userText = body == null ? null : (body.get("text") == null ? null : String.valueOf(body.get("text")));
        return R.success(voiceCallService.processText(userId, sessionId, userText));
    }

    /**
     * 结束语音会话。
     * POST /api/voice-call/{sessionId}/end
     */
    @PostMapping("/{sessionId}/end")
    public R<String> end(@PathVariable String sessionId) {
        Long userId = AuthContext.requireUserId();
        voiceCallService.endSession(userId, sessionId);
        return R.success("语音会话已结束");
    }

    /**
     * 查询会话状态。
     * GET /api/voice-call/{sessionId}/status
     */
    @GetMapping("/{sessionId}/status")
    public R<Map<String, Object>> status(@PathVariable String sessionId) {
        Long userId = AuthContext.requireUserId();
        return R.success(voiceCallService.getSessionStatus(userId, sessionId));
    }
}
