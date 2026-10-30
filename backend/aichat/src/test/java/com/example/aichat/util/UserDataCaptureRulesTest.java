package com.example.aichat.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserDataCaptureRules 纯静态规则单元测试：脱敏 + 完整性评分。
 */
class UserDataCaptureRulesTest {

    @Test
    void 脱敏手机号应替换为星号() {
        String out = UserDataCaptureRules.sanitize("联系我13812345678详谈");
        assertFalse(out.contains("13812345678"));
        assertTrue(out.contains("***"));
    }

    @Test
    void 脱敏邮箱应替换() {
        String out = UserDataCaptureRules.sanitize("邮箱 abc.def+1@example.com 联系");
        assertFalse(out.contains("abc.def+1@example.com"));
        assertTrue(out.contains("***@***"));
    }

    @Test
    void 脱敏身份证应替换() {
        String out = UserDataCaptureRules.sanitize("身份证110101199001011234号");
        assertFalse(out.contains("110101199001011234"));
        assertTrue(out.contains("**********"));
    }

    @Test
    void 超长内容应截断到上限() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20000; i++) sb.append("字");
        String out = UserDataCaptureRules.truncateIfNeeded(sb.toString());
        assertEquals(UserDataCaptureRules.MAX_CONTENT_LENGTH, out.length());
    }

    @Test
    void 评分全维度应得满分() {
        int score = UserDataCaptureRules.score("这是一段长度足够的测试内容", "{\"a\":1}",
                "social_post", "social_post", LocalDateTime.now());
        assertEquals(100, score);
    }

    @Test
    void 评分缺项应扣分() {
        // 空内容、空 metadata、空 tags、短内容、非法类型、空时间 -> 0
        int score = UserDataCaptureRules.score("", "{}", "", "bad_type", null);
        assertEquals(0, score);
    }

    @Test
    void 合法dataType校验() {
        assertTrue(UserDataCaptureRules.isValidDataType("group_chat"));
        assertFalse(UserDataCaptureRules.isValidDataType("hacker"));
        assertFalse(UserDataCaptureRules.isValidDataType(null));
    }
}
