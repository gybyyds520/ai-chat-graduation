package com.example.aichat.service.quota;

/**
 * 受配额管控的功能类型。
 */
public enum FeatureType {

    AI_CHAT("ai_chat"),
    VOICE_CALL("voice_call"),
    IMAGE_GEN("image_gen"),
    DOC_UPLOAD("doc_upload"),
    DEEP_RESEARCH("deep_research");

    private final String code;

    FeatureType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static FeatureType fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("featureType 不能为空");
        }
        for (FeatureType f : values()) {
            if (f.code.equalsIgnoreCase(code)) {
                return f;
            }
        }
        throw new IllegalArgumentException("未知功能类型: " + code);
    }
}
