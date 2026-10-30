package com.example.aichat.service.voice.adapter;

/**
 * 语音识别（ASR）适配器接口。
 *
 * 设计目的：把"音频 -> 文本"的具体实现抽成可插拔接口，
 * 上层 VoiceCallService 只依赖本接口，不关心底层是 Mock、Ollama Whisper 还是 OpenAI Whisper。
 */
public interface AsrAdapter {

    /**
     * 将一段音频识别为文本。
     *
     * @param audio    音频字节数据
     * @param fileName 原始文件名（含扩展名，供底层判断格式）
     * @return 识别出的文本；识别失败返回 null
     */
    String transcribe(byte[] audio, String fileName);

    /** 当前适配器是否可用（外部服务就绪 / Mock 恒可用）。 */
    boolean isAvailable();

    /** 适配器名称，供前端展示"模拟模式"提示与排查问题。 */
    String getName();
}
