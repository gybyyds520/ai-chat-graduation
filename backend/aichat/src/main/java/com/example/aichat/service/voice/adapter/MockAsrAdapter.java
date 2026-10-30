package com.example.aichat.service.voice.adapter;

import org.springframework.stereotype.Component;

/**
 * Mock 语音识别适配器。
 *
 * 无外部服务时的兜底实现：忽略上传的音频字节，固定返回一句文本，
 * 用于在离线环境走通 ASR -> LLM -> TTS 全链路。
 */
@Component
public class MockAsrAdapter implements AsrAdapter {

    public static final String FIXED_TEXT = "你好，请介绍一下你自己";

    @Override
    public String transcribe(byte[] audio, String fileName) {
        // 离线环境不真正识别音频，固定返回演示文本
        return FIXED_TEXT;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getName() {
        return "mock-asr";
    }
}
