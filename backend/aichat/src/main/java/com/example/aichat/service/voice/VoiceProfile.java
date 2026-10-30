package com.example.aichat.service.voice;

import java.util.Arrays;
import java.util.Optional;

/**
 * 预设音色配置。
 *
 * 共 7 种音色，每种包含：voiceId、展示名称、描述、适用场景、以及一个用于 Mock TTS
 * 区分音频内容的采样率标识（sampleRate）。生产 SpeechServiceTtsAdapter 若底层支持
 * 音色参数则透传，否则降级到默认音色。
 */
public enum VoiceProfile {

    DEFAULT("default", "默认音色", "通用对话", "通用聊天场景", 22050),
    WARM("warm", "温暖柔和", "温暖治愈", "情感陪伴、夜间闲聊", 24000),
    COLD("cold", "冷静沉稳", "理性客观", "知识问答、工作助手", 16000),
    YOUNG("young", "年轻活力", "轻快活泼", "轻松闲聊、娱乐场景", 32000),
    OLD("old", "成熟稳重", "低沉厚重", "正式对话、历史讲述", 18000),
    FEMALE("female", "温柔女声", "亲和细腻", "陪伴对话、朗读", 26000),
    MALE("male", "沉稳男声", "稳重可靠", "播报、正式场合", 20000),
    CUTE("cute", "可爱萌音", "俏皮可爱", "萌系对话、儿童场景", 28000);

    private final String voiceId;
    private final String name;
    private final String description;
    private final String scene;
    private final int sampleRate;

    VoiceProfile(String voiceId, String name, String description, String scene, int sampleRate) {
        this.voiceId = voiceId;
        this.name = name;
        this.description = description;
        this.scene = scene;
        this.sampleRate = sampleRate;
    }

    public String getVoiceId() {
        return voiceId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getScene() {
        return scene;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * 按 voiceId 查找音色；未找到或入参为空时返回 DEFAULT（降级默认音色）。
     */
    public static VoiceProfile fromId(String voiceId) {
        if (voiceId == null || voiceId.trim().isEmpty()) {
            return DEFAULT;
        }
        String normalized = voiceId.trim();
        Optional<VoiceProfile> matched = Arrays.stream(values())
                .filter(v -> v.voiceId.equalsIgnoreCase(normalized))
                .findFirst();
        return matched.orElse(DEFAULT);
    }

    /** 判断 voiceId 是否为合法音色。 */
    public static boolean isValid(String voiceId) {
        if (voiceId == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(v -> v.voiceId.equalsIgnoreCase(voiceId.trim()));
    }
}
