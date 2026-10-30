package com.example.aichat.service.quota;

/**
 * 会员等级体系。
 *
 * FREE：每日 AI 聊天 20、语音 5、图片 0、文档 0，无高级功能；
 * BASIC：每日 AI 聊天 100、语音 20、图片 5、文档 3，基础高级功能；
 * PREMIUM：每日 AI 聊天 500、语音 100、图片 20、文档 10，全部高级功能。
 */
public enum MembershipLevel {

    FREE("免费版", 20, 5, 0, 0, 1, false),
    BASIC("基础版", 100, 20, 5, 3, 5, true),
    PREMIUM("高级版", 500, 100, 20, 10, 20, true);

    private final String label;
    private final int aiChatPerDay;
    private final int voiceCallPerDay;
    private final int imageGenPerDay;
    private final int docUploadPerDay;
    private final int deepResearchPerDay;
    private final boolean advanced;

    MembershipLevel(String label, int aiChatPerDay, int voiceCallPerDay,
                   int imageGenPerDay, int docUploadPerDay, int deepResearchPerDay, boolean advanced) {
        this.label = label;
        this.aiChatPerDay = aiChatPerDay;
        this.voiceCallPerDay = voiceCallPerDay;
        this.imageGenPerDay = imageGenPerDay;
        this.docUploadPerDay = docUploadPerDay;
        this.deepResearchPerDay = deepResearchPerDay;
        this.advanced = advanced;
    }

    public String getLabel() {
        return label;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    /** 返回该等级对某功能的每日配额上限（0 表示该功能不可用）。 */
    public int limitFor(FeatureType featureType) {
        return switch (featureType) {
            case AI_CHAT -> aiChatPerDay;
            case VOICE_CALL -> voiceCallPerDay;
            case IMAGE_GEN -> imageGenPerDay;
            case DOC_UPLOAD -> docUploadPerDay;
            case DEEP_RESEARCH -> deepResearchPerDay;
        };
    }

    /** 等级配置是否合法（升级路径 FREE->BASIC->PREMIUM）。 */
    public boolean canUpgradeTo(MembershipLevel target) {
        return target != null && target.ordinal() > this.ordinal();
    }

    public static MembershipLevel fromString(String s) {
        if (s == null || s.isBlank()) {
            return FREE;
        }
        try {
            return MembershipLevel.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FREE;
        }
    }
}
