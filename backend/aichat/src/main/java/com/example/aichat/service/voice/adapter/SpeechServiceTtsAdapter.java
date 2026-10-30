package com.example.aichat.service.voice.adapter;

import com.example.aichat.service.ai.SpeechService;
import com.example.aichat.service.voice.VoiceProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 生产 TTS 适配器，复用现有 SpeechService（Google Translate / OpenAI TTS）。
 *
 * 多音色策略：现有 SpeechService.textToSpeech(text) 不支持音色参数，
 * 因此当传入非默认 voiceId 时，记录日志并降级到默认音色合成，保证功能可用。
 * 后续 SpeechService 扩展音色透传后，再在此处把 voiceId 传入底层即可。
 */
@Component
public class SpeechServiceTtsAdapter implements TtsAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SpeechServiceTtsAdapter.class);

    private final SpeechService speechService;

    public SpeechServiceTtsAdapter(SpeechService speechService) {
        this.speechService = speechService;
    }

    @Override
    public byte[] synthesize(String text) {
        return speechService.textToSpeech(text);
    }

    @Override
    public byte[] synthesize(String text, String voiceId) {
        VoiceProfile profile = VoiceProfile.fromId(voiceId);
        if (!VoiceProfile.DEFAULT.getVoiceId().equals(profile.getVoiceId())) {
            // 当前 SpeechService 不支持音色参数，降级到默认音色
            logger.info("SpeechService 暂不支持音色 {}，降级到默认音色合成", profile.getVoiceId());
        }
        return speechService.textToSpeech(text);
    }

    @Override
    public boolean isAvailable() {
        return speechService.isTtsAvailable();
    }

    @Override
    public String getName() {
        return "speech-service-tts";
    }

    @Override
    public String getAudioFormat() {
        // SpeechService TTS 返回 MP3（audio/mpeg）
        return "mpeg";
    }
}
