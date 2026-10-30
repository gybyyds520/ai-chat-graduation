package com.example.aichat.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FeedController#calculateEffectiveness 确定性评分规则测试。
 *
 * 规则：基础 50；长度 >500 +15 / >200 +10 / >50 +5；有文件 +10；有图片 +5；
 * 有标签 +5；deep +10 / standard +5 / fast +0；urgent +5 / high +3 / medium +0 / low -5；
 * clamp [30, 99]。
 */
class FeedEffectivenessTest {

    // 1. 短文本(<50) + fast + low => 50 + 0 + 0 + 0 + 0 + 0 + (-5) = 45（较低）
    @Test
    void 短文本_评分较低() {
        int score = FeedController.calculateEffectiveness("短", false, false, null, "fast", "low");
        assertEquals(45, score);
        assertTrue(score <= 60, "短文本 fast/low 应较低");
    }

    // 2. 长文本(>500) + standard + medium + 无文件图片标签 => 50 + 15 + 5 = 70（较高）
    @Test
    void 长文本_评分较高() {
        String longText = "长".repeat(600);
        int shortScore = FeedController.calculateEffectiveness("短", false, false, null, "standard", "medium");
        int longScore = FeedController.calculateEffectiveness(longText, false, false, null, "standard", "medium");
        assertEquals(70, longScore);
        assertTrue(longScore > shortScore, "长文本评分应高于短文本");
    }

    // 3. deep 模式比 fast 模式高（相同输入）
    @Test
    void deep模式_比fast模式高() {
        String content = "测试内容".repeat(15); // 60 字 > 50，长度 +5
        int fast = FeedController.calculateEffectiveness(content, false, false, null, "fast", "medium");
        int deep = FeedController.calculateEffectiveness(content, false, false, null, "deep", "medium");
        // 长度>50 => +5；fast +0 => 55；deep +10 => 65
        assertEquals(55, fast);
        assertEquals(65, deep);
        assertTrue(deep > fast, "deep 模式 effectiveness 应高于 fast");
    }

    // 4. 无标签/无文件时仍有基础分（非 0）
    @Test
    void 无标签无文件_仍有基础分() {
        int score = FeedController.calculateEffectiveness("短", false, false, null, "standard", "medium");
        assertEquals(55, score);
        assertTrue(score > 0, "无标签无文件时仍应有基础分，不应为 0");
    }

    // 5. clamp：urgent+deep+长文本+文件+图片+标签 全部叠加不应超过 99
    @Test
    void 评分_clamp到99上限() {
        String longText = "长".repeat(800);
        int score = FeedController.calculateEffectiveness(longText, true, true, "a,b,c", "deep", "urgent");
        // 50 +15 +10 +5 +5 +10 +5 = 100 => clamp 99
        assertEquals(99, score);
    }

    // 6. clamp：low 优先级 + 空内容 + fast 不应低于 30
    @Test
    void 评分_clamp到30下限() {
        int score = FeedController.calculateEffectiveness("", false, false, null, "fast", "low");
        // 50 +0 +0 +0 +0 +0 -5 = 45
        assertEquals(45, score);
        assertTrue(score >= 30);
    }
}
