package com.example.aichat.service.voice.adapter;

import com.example.aichat.service.voice.VoiceProfile;

import java.util.List;

/**
 * 语音合成（TTS）适配器接口。
 *
 * 设计目的：把"文本 -> 音频字节"的具体实现抽成可插拔接口，
 * 支持 Mock 与 SpeechService（Google / OpenAI TTS）两种生产形态。
 */
public interface TtsAdapter {

    /**
     * 使用默认音色合成语音。
     *
     * @param text 待合成文本
     * @return 音频字节；合成失败返回 null
     */
    byte[] synthesize(String text);

    /**
     * 指定音色合成语音。
     *
     * @param text   待合成文本
     * @param voiceId 音色标识（null 或无法识别时降级到默认音色）
     * @return 音频字节；合成失败返回 null
     */
    default byte[] synthesize(String text, String voiceId) {
        // 默认实现忽略音色参数，保持向后兼容；支持多音色的适配器重写本方法
        return synthesize(text);
    }

    /** 当前适配器是否可用。 */
    boolean isAvailable();

    /** 适配器名称。 */
    String getName();

    /** 音频格式标识（如 wav / mp3 / mpeg），供前端正确播放。 */
    String getAudioFormat();

    /**
     * 列出本适配器支持的全部音色。
     *
     * 默认返回空列表（单音色适配器）；多音色适配器（MockTtsAdapter / EdgeTtsAdapter）
     * 应重写本方法返回 ≥5 种音色供前端下拉选择。
     */
    default List<VoiceProfile> getAvailableVoices() {
        return List.of();
    }
}
