package com.example.aichat.service.voice.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.VoiceErrorCode;
import com.example.aichat.entity.User;
import com.example.aichat.service.UserService;
import com.example.aichat.service.voice.VoiceAdapterManager;
import com.example.aichat.service.voice.VoiceCallService;
import com.example.aichat.service.voice.VoiceProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时语音通话服务实现。
 *
 * 会话状态全部保存在内存 ConcurrentHashMap（userId/characterId/history/createdAt/lastActiveAt），
 * 不建数据库表。每轮 processAudio 走 ASR -> LLM -> TTS 全链路，并用用户的 voicePreference
 * 作为 TTS 音色。所有访问均做 IDOR 校验：非会话所有者访问抛 VOICE_SESSION_UNAUTHORIZED。
 */
@Service
public class VoiceCallServiceImpl implements VoiceCallService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceCallServiceImpl.class);

    /** 单段音频上传大小上限：10MB，与 VoiceChatController 既有限制保持一致。 */
    static final long MAX_AUDIO_SIZE = 10L * 1024 * 1024;

    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个友好的AI助手，请用简洁自然的语气回答。";

    private final VoiceAdapterManager adapterManager;
    private final UserService userService;

    private final ConcurrentHashMap<String, VoiceSession> sessions = new ConcurrentHashMap<>();

    public VoiceCallServiceImpl(VoiceAdapterManager adapterManager, UserService userService) {
        this.adapterManager = adapterManager;
        this.userService = userService;
    }

    @Override
    public String startSession(Long userId, Long characterId) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        VoiceAdapterManager.Selection selection = adapterManager.select();
        VoiceSession session = new VoiceSession();
        session.sessionId = sessionId;
        session.userId = userId;
        session.characterId = characterId;
        session.history = new ArrayList<>();
        session.createdAt = LocalDateTime.now();
        session.lastActiveAt = session.createdAt;
        session.status = "ACTIVE";
        session.selection = selection;
        sessions.put(sessionId, session);
        logger.info("语音会话已开启: sessionId={}, userId={}, characterId={}, mockMode={}",
                sessionId, userId, characterId, selection.getAdapterInfo().get("mockMode"));
        return sessionId;
    }

    @Override
    public Map<String, Object> processAudio(Long userId, String sessionId, byte[] audio, String fileName) {
        VoiceSession session = requireActiveSession(sessionId);
        // IDOR 防护：仅会话所有者可处理音频
        if (!session.userId.equals(userId)) {
            throw new BusinessException(VoiceErrorCode.VOICE_SESSION_UNAUTHORIZED);
        }

        if (audio == null || audio.length == 0) {
            throw new BusinessException(VoiceErrorCode.VOICE_AUDIO_EMPTY);
        }
        if (audio.length > MAX_AUDIO_SIZE) {
            throw new BusinessException(VoiceErrorCode.VOICE_AUDIO_TOO_LARGE);
        }

        long asrStart = System.currentTimeMillis();
        String userText = session.selection.getAsr().transcribe(audio, fileName);
        long asrCost = System.currentTimeMillis() - asrStart;

        Map<String, Object> result = completeTurn(session, userId, userText);
        Map<String, Object> timestamps = (Map<String, Object>) result.get("timestamps");
        timestamps.put("asrMs", asrCost);
        result.put("timestamps", timestamps);
        return result;
    }

    @Override
    public Map<String, Object> processText(Long userId, String sessionId, String userText) {
        VoiceSession session = requireActiveSession(sessionId);
        // IDOR 防护：仅会话所有者可处理文本
        if (!session.userId.equals(userId)) {
            throw new BusinessException(VoiceErrorCode.VOICE_SESSION_UNAUTHORIZED);
        }
        if (userText == null || userText.trim().isEmpty()) {
            throw new BusinessException(VoiceErrorCode.VOICE_AUDIO_EMPTY);
        }
        return completeTurn(session, userId, userText.trim());
    }

    /**
     * 完成一轮 LLM -> TTS -> 记录历史 -> 组装结果（processAudio 与 processText 共用）。
     */
    private Map<String, Object> completeTurn(VoiceSession session, Long userId, String userText) {
        long llmStart = System.currentTimeMillis();
        String aiText = session.selection.getLlm()
                .generateReply(userText, DEFAULT_SYSTEM_PROMPT, session.history);
        long llmCost = System.currentTimeMillis() - llmStart;

        // LLM 不可用时兜底简短回复，保证语音通话始终有内容可朗读
        boolean fallbackReply = aiText == null || aiText.trim().isEmpty();
        if (fallbackReply) {
            aiText = "你好，我是AI助手。你刚才说的是：" + (userText == null ? "" : userText);
            logger.warn("语音 LLM 无有效回复，使用简短兜底回复");
        }

        // 读取用户音色偏好
        String voiceId = resolveVoiceId(userId);

        long ttsStart = System.currentTimeMillis();
        byte[] audioBytes = session.selection.getTts().synthesize(aiText, voiceId);
        long ttsCost = System.currentTimeMillis() - ttsStart;

        // 记录对话历史（兜底回复不写入，避免污染多轮上下文）
        if (userText != null) {
            session.history.add("user: " + userText);
        }
        if (aiText != null && !fallbackReply) {
            session.history.add("ai: " + aiText);
        }
        session.lastActiveAt = LocalDateTime.now();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.sessionId);
        result.put("userText", userText);
        result.put("aiText", aiText);
        result.put("audioBase64", audioBytes != null ? Base64.getEncoder().encodeToString(audioBytes) : "");
        result.put("audioFormat", session.selection.getTts().getAudioFormat());
        result.put("voiceId", voiceId);
        result.put("adapterInfo", session.selection.getAdapterInfo());
        Map<String, Object> timestamps = new LinkedHashMap<>();
        timestamps.put("asrMs", 0L);
        timestamps.put("llmMs", llmCost);
        timestamps.put("ttsMs", ttsCost);
        timestamps.put("totalMs", llmCost + ttsCost);
        result.put("timestamps", timestamps);
        return result;
    }

    @Override
    public void endSession(Long userId, String sessionId) {
        VoiceSession session = requireActiveSession(sessionId);
        if (!session.userId.equals(userId)) {
            throw new BusinessException(VoiceErrorCode.VOICE_SESSION_UNAUTHORIZED);
        }
        session.status = "ENDED";
        sessions.remove(sessionId);
        logger.info("语音会话已结束: sessionId={}, userId={}", sessionId, userId);
    }

    @Override
    public Map<String, Object> getSessionStatus(Long userId, String sessionId) {
        VoiceSession session = sessions.get(sessionId);
        if (session == null) {
            throw new BusinessException(VoiceErrorCode.VOICE_SESSION_NOT_FOUND);
        }
        if (!session.userId.equals(userId)) {
            throw new BusinessException(VoiceErrorCode.VOICE_SESSION_UNAUTHORIZED);
        }
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("sessionId", sessionId);
        status.put("userId", session.userId);
        status.put("characterId", session.characterId);
        status.put("status", session.status);
        status.put("turnCount", session.history.size() / 2);
        status.put("createdAt", session.createdAt.toString());
        status.put("lastActiveAt", session.lastActiveAt.toString());
        status.put("adapterInfo", session.selection.getAdapterInfo());
        return status;
    }

    /** 取会话并校验存在且未结束。 */
    private VoiceSession requireActiveSession(String sessionId) {
        VoiceSession session = sessions.get(sessionId);
        if (session == null || "ENDED".equals(session.status)) {
            throw new BusinessException(VoiceErrorCode.VOICE_SESSION_NOT_FOUND);
        }
        return session;
    }

    /** 读取用户音色偏好，空值或非法值降级为 default。 */
    private String resolveVoiceId(Long userId) {
        try {
            User user = userService.getUserById(userId);
            if (user != null && VoiceProfile.isValid(user.getVoicePreference())) {
                return user.getVoicePreference();
            }
        } catch (Exception e) {
            logger.warn("读取用户音色偏好失败，使用默认音色: {}", e.getMessage());
        }
        return VoiceProfile.DEFAULT.getVoiceId();
    }

    /** 内存会话对象。 */
    private static class VoiceSession {
        String sessionId;
        Long userId;
        Long characterId;
        List<String> history;
        LocalDateTime createdAt;
        LocalDateTime lastActiveAt;
        String status;
        VoiceAdapterManager.Selection selection;
    }
}
