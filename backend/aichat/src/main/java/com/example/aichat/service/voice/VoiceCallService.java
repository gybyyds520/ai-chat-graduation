package com.example.aichat.service.voice;

import java.util.Map;

/**
 * 实时语音通话服务（半双工轮次管道）。
 *
 * 一轮 = 上传一段录音 -> ASR 转文本 -> LLM 生成回复 -> TTS 合成音频 -> 返回给前端播放。
 * 会话状态保存在内存 ConcurrentHashMap，不落库。
 */
public interface VoiceCallService {

    /**
     * 开启一次语音会话。
     *
     * @param userId     当前登录用户 ID
     * @param characterId 可选的 AI 角色 ID
     * @return 会话 ID
     */
    String startSession(Long userId, Long characterId);

    /**
     * 处理一轮语音：ASR -> LLM -> TTS 全链路。
     *
     * @param userId   当前登录用户 ID（用于 IDOR 校验）
     * @param sessionId 会话 ID
     * @param audio     音频字节
     * @param fileName  原始文件名
     * @return 包含 userText/aiText/audioBase64/audioFormat/sessionId/adapterInfo/timestamps 的结果
     */
    Map<String, Object> processAudio(Long userId, String sessionId, byte[] audio, String fileName);

    /**
     * 处理一轮文本轮次：跳过 ASR，直接 LLM -> TTS。
     * 供前端浏览器语音识别（Web Speech API）转写文本后调用，避免依赖后端 ASR。
     *
     * @param userId   当前登录用户 ID（用于 IDOR 校验）
     * @param sessionId 会话 ID
     * @param userText 用户已转写的文本
     * @return 与 processAudio 相同结构的响应
     */
    Map<String, Object> processText(Long userId, String sessionId, String userText);

    /**
     * 结束会话（IDOR 校验后调用）。
     */
    void endSession(Long userId, String sessionId);

    /**
     * 查询会话状态（IDOR 校验后调用）。
     */
    Map<String, Object> getSessionStatus(Long userId, String sessionId);
}
