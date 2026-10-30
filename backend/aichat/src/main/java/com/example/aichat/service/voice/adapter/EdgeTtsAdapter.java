package com.example.aichat.service.voice.adapter;

import com.example.aichat.service.voice.VoiceProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Edge TTS 多音色适配器。
 *
 * 设计：Edge TTS 本身为 Python 包（edge-tts），在纯 Java 后端环境无法直接调用。
 * 因此本适配器采用"Java 多音色枚举 + 离线降级"方案：
 *   - getAvailableVoices() 返回 ≥5 种预设音色（温柔女声/温柔男声/沉稳/活泼/可爱等），
 *     与 VoiceProfile 枚举对齐，供前端下拉选择；
 *   - synthesize() 实际合成委托给内置的 MockTtsAdapter（离线 WAV，按音色区分采样值），
 *     保证切换音色即时生效且测试完全离线；
 *   - 后续若部署环境安装了 edge-tts Python 服务，可在本类中扩展 HTTP 调用并保留降级路径。
 */
@Component
public class EdgeTtsAdapter implements TtsAdapter {

    private static final Logger log = LoggerFactory.getLogger(EdgeTtsAdapter.class);

    private final MockTtsAdapter fallback = new MockTtsAdapter();

    @Override
    public byte[] synthesize(String text) {
        return synthesize(text, null);
    }

    @Override
    public byte[] synthesize(String text, String voiceId) {
        // Java 枚举模式：直接走离线 MockTtsAdapter，按 voiceId 区分采样值
        log.debug("EdgeTts(Java枚举模式) 使用音色 {} 合成文本长度={}", voiceId, text == null ? 0 : text.length());
        return fallback.synthesize(text, voiceId);
    }

    @Override
    public boolean isAvailable() {
        // Java 枚举模式恒可用（不依赖外部网络/Python 服务）
        return true;
    }

    @Override
    public String getName() {
        return "edge-tts";
    }

    @Override
    public String getAudioFormat() {
        return "wav";
    }

    /**
     * 返回全部可选音色（≥5 种）。
     * 与 VoiceProfile 枚举保持一致，覆盖温柔女声/温柔男声/沉稳/活泼/可爱等场景。
     */
    @Override
    public List<VoiceProfile> getAvailableVoices() {
        return Arrays.asList(VoiceProfile.values());
    }
}
