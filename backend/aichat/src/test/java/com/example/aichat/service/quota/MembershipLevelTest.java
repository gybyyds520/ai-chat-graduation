package com.example.aichat.service.quota;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MembershipLevel 等级配置与升级路径测试。
 */
class MembershipLevelTest {

    @Test
    void 各等级配额应符合配置() {
        assertEquals(20, MembershipLevel.FREE.limitFor(FeatureType.AI_CHAT));
        assertEquals(5, MembershipLevel.FREE.limitFor(FeatureType.VOICE_CALL));
        assertEquals(0, MembershipLevel.FREE.limitFor(FeatureType.IMAGE_GEN));
        assertEquals(0, MembershipLevel.FREE.limitFor(FeatureType.DOC_UPLOAD));
        assertEquals(1, MembershipLevel.FREE.limitFor(FeatureType.DEEP_RESEARCH));

        assertEquals(100, MembershipLevel.BASIC.limitFor(FeatureType.AI_CHAT));
        assertEquals(5, MembershipLevel.BASIC.limitFor(FeatureType.IMAGE_GEN));
        assertEquals(3, MembershipLevel.BASIC.limitFor(FeatureType.DOC_UPLOAD));
        assertEquals(5, MembershipLevel.BASIC.limitFor(FeatureType.DEEP_RESEARCH));

        assertEquals(500, MembershipLevel.PREMIUM.limitFor(FeatureType.AI_CHAT));
        assertEquals(20, MembershipLevel.PREMIUM.limitFor(FeatureType.IMAGE_GEN));
        assertEquals(20, MembershipLevel.PREMIUM.limitFor(FeatureType.DEEP_RESEARCH));
    }

    @Test
    void 升级路径应严格递增() {
        assertTrue(MembershipLevel.FREE.canUpgradeTo(MembershipLevel.BASIC));
        assertTrue(MembershipLevel.BASIC.canUpgradeTo(MembershipLevel.PREMIUM));
        assertFalse(MembershipLevel.PREMIUM.canUpgradeTo(MembershipLevel.BASIC));
        assertFalse(MembershipLevel.FREE.canUpgradeTo(MembershipLevel.FREE));
    }

    @Test
    void fromString_空或非法应回退FREE() {
        assertEquals(MembershipLevel.FREE, MembershipLevel.fromString(null));
        assertEquals(MembershipLevel.FREE, MembershipLevel.fromString(""));
        assertEquals(MembershipLevel.PREMIUM, MembershipLevel.fromString("premium"));
        assertEquals(MembershipLevel.BASIC, MembershipLevel.fromString("BASIC"));
    }
}
