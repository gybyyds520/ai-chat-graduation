package com.example.aichat.service.voice.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ollama / 本地 Whisper ASR 生产扩展点。
 *
 * 当前默认未接入真实本地语音模型，isAvailable() 返回 false，
 * VoiceAdapterManager 检测到不可用时自动降级到 MockAsrAdapter。
 * 后续接入本地 whisper.cpp / Ollama 语音接口时，只需在此处补全 transcribe 实现，
 * 并让 isAvailable() 在模型就绪时返回 true 即可，上层无需改动。
 */
@Component
public class OllamaAsrAdapter implements AsrAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OllamaAsrAdapter.class);

    @Value("${voice.asr.ollama.enabled:false}")
    private boolean enabled;

    @Value("${voice.asr.ollama.endpoint:}")
    private String endpoint;

    @Override
    public String transcribe(byte[] audio, String fileName) {
        if (!isAvailable()) {
            logger.warn("Ollama ASR 未启用，应降级到 Mock；本次调用返回 null");
            return null;
        }
        // TODO: 接入本地 whisper 模型后在此实现真实识别
        logger.info("Ollama ASR 占位实现, 文件名={}, 大小={}", fileName, audio != null ? audio.length : 0);
        return null;
    }

    @Override
    public boolean isAvailable() {
        return enabled && endpoint != null && !endpoint.trim().isEmpty();
    }

    @Override
    public String getName() {
        return "ollama-asr";
    }
}
