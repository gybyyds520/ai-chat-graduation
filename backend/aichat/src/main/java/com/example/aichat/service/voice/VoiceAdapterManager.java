package com.example.aichat.service.voice;

import com.example.aichat.service.voice.adapter.AsrAdapter;
import com.example.aichat.service.voice.adapter.LlmAdapter;
import com.example.aichat.service.voice.adapter.TtsAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 语音适配器管理器。
 *
 * 职责：从所有已注册的适配器 Bean 中，为 ASR / LLM / TTS 各挑选一个可用实现。
 * 选择策略：优先生产实现（名称不以 mock- 开头且 isAvailable()=true）；
 *           生产实现不可用时自动降级到 Mock 实现，并在 adapterInfo.mockMode 标记，
 *           供前端展示"模拟模式"提示。
 */
@Component
public class VoiceAdapterManager {

    private static final Logger logger = LoggerFactory.getLogger(VoiceAdapterManager.class);

    private final List<AsrAdapter> asrAdapters;
    private final List<LlmAdapter> llmAdapters;
    private final List<TtsAdapter> ttsAdapters;

    public VoiceAdapterManager(List<AsrAdapter> asrAdapters,
                               List<LlmAdapter> llmAdapters,
                               List<TtsAdapter> ttsAdapters) {
        this.asrAdapters = asrAdapters;
        this.llmAdapters = llmAdapters;
        this.ttsAdapters = ttsAdapters;
    }

    /**
     * 选定一次语音通话使用的三件套适配器。
     */
    public synchronized Selection select() {
        AsrAdapter asr = pickAsr();
        LlmAdapter llm = pickLlm();
        TtsAdapter tts = pickTts();

        boolean mockMode = isMock(asr.getName()) && isMock(llm.getName()) && isMock(tts.getName());
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("asr", asr.getName());
        info.put("llm", llm.getName());
        info.put("tts", tts.getName());
        info.put("audioFormat", tts.getAudioFormat());
        info.put("mockMode", mockMode);

        logger.info("语音适配器选择: asr={}, llm={}, tts={}, mockMode={}",
                asr.getName(), llm.getName(), tts.getName(), mockMode);
        return new Selection(asr, llm, tts, info);
    }

    private AsrAdapter pickAsr() {
        for (AsrAdapter a : asrAdapters) {
            if (a.isAvailable() && !isMock(a.getName())) return a;
        }
        for (AsrAdapter a : asrAdapters) {
            if (a.isAvailable()) return a;
        }
        return asrAdapters.isEmpty() ? null : asrAdapters.get(0);
    }

    private LlmAdapter pickLlm() {
        for (LlmAdapter a : llmAdapters) {
            if (a.isAvailable() && !isMock(a.getName())) return a;
        }
        for (LlmAdapter a : llmAdapters) {
            if (a.isAvailable()) return a;
        }
        return llmAdapters.isEmpty() ? null : llmAdapters.get(0);
    }

    private TtsAdapter pickTts() {
        for (TtsAdapter a : ttsAdapters) {
            if (a.isAvailable() && !isMock(a.getName())) return a;
        }
        for (TtsAdapter a : ttsAdapters) {
            if (a.isAvailable()) return a;
        }
        return ttsAdapters.isEmpty() ? null : ttsAdapters.get(0);
    }

    private boolean isMock(String name) {
        return name != null && name.startsWith("mock-");
    }

    /** 一次选中的适配器组合。 */
    public static class Selection {
        private final AsrAdapter asr;
        private final LlmAdapter llm;
        private final TtsAdapter tts;
        private final Map<String, Object> adapterInfo;

        public Selection(AsrAdapter asr, LlmAdapter llm, TtsAdapter tts, Map<String, Object> adapterInfo) {
            this.asr = asr;
            this.llm = llm;
            this.tts = tts;
            this.adapterInfo = adapterInfo;
        }

        public AsrAdapter getAsr() {
            return asr;
        }

        public LlmAdapter getLlm() {
            return llm;
        }

        public TtsAdapter getTts() {
            return tts;
        }

        public Map<String, Object> getAdapterInfo() {
            return adapterInfo;
        }
    }
}
