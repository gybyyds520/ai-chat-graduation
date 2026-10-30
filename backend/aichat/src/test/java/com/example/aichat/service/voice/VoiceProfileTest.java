package com.example.aichat.service.voice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VoiceProfile 多音色配置单元测试。
 */
class VoiceProfileTest {

    @Test
    void 共应提供7种预设音色() {
        assertTrue(VoiceProfile.values().length >= 5);
    }

    @Test
    void fromId_空入参应返回default() {
        assertEquals(VoiceProfile.DEFAULT, VoiceProfile.fromId(null));
        assertEquals(VoiceProfile.DEFAULT, VoiceProfile.fromId(""));
    }

    @Test
    void fromId_未知音色应降级default() {
        assertEquals(VoiceProfile.DEFAULT, VoiceProfile.fromId("robot-xxx"));
    }

    @Test
    void fromId_合法音色应正确匹配且大小写不敏感() {
        assertEquals(VoiceProfile.WARM, VoiceProfile.fromId("warm"));
        assertEquals(VoiceProfile.WARM, VoiceProfile.fromId("Warm"));
        assertEquals(VoiceProfile.MALE, VoiceProfile.fromId("male"));
    }

    @Test
    void isValid_应正确判断合法与非法音色() {
        assertTrue(VoiceProfile.isValid("warm"));
        assertTrue(VoiceProfile.isValid("FEMALE"));
        assertFalse(VoiceProfile.isValid(null));
        assertFalse(VoiceProfile.isValid("unknown"));
    }
}
