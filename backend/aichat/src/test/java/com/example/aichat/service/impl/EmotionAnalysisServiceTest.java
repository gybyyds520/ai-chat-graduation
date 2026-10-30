package com.example.aichat.service.impl;

import com.example.aichat.service.EmotionAnalysisService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 模块 - 情感分析服务单元测试。
 *
 * 覆盖点（纯本地规则，不依赖 AI/Ollama）：
 *   1. 正面文本 -> happy；
 *   2. 负面文本 -> sad / angry；
 *   3. 中性文本 -> neutral；
 *   4. 空文本 / null 兜底；
 *   5. 情感强度 intensity 落在 [0,1] 且感叹号/大写/程度副词加权；
 *   6. analyzeFull 返回完整四元组（emotion/intensity/expression/action）。
 */
class EmotionAnalysisServiceTest {

    private final EmotionAnalysisService service = new EmotionAnalysisServiceImpl();

    @Test
    void 正面文本_应识别为happy() {
        assertEquals("happy", service.analyzeEmotion("今天我好开心呀"));
        assertEquals("happy", service.analyzeEmotion("I am so happy!"));
    }

    @Test
    void 负面文本_应识别为sad或angry() {
        assertEquals("sad", service.analyzeEmotion("我今天很难过"));
        assertEquals("angry", service.analyzeEmotion("这件事让我很生气"));
    }

    @Test
    void 中性文本_应识别为neutral() {
        assertEquals("neutral", service.analyzeEmotion("请问现在几点了"));
    }

    @Test
    void 空与null文本_应兜底为neutral且强度为0() {
        assertEquals("neutral", service.analyzeEmotion(null));
        assertEquals("neutral", service.analyzeEmotion(""));
        assertEquals(0.0, service.analyzeIntensity(null, "neutral"), 1e-9);
        assertEquals(0.0, service.analyzeIntensity("", "neutral"), 1e-9);
    }

    @Test
    void 情感强度_应限制在0到1之间且加权() {
        double base = service.analyzeIntensity("普通一句话", "neutral");
        double loud = service.analyzeIntensity("真的!!! 太喜欢了!!!", "happy");
        assertTrue(base >= 0.0 && base <= 1.0, "基础强度应在[0,1]");
        assertTrue(loud > base, "感叹号与程度副词应提升强度");
        assertTrue(loud <= 1.0, "强度上限为1.0");
        // 极端输入不应超过上限
        double extreme = service.analyzeIntensity("!!! 非常非常非常棒 ABCDEFGHIJKLMNOPQRSTUVWXYZ", "happy");
        assertTrue(extreme <= 1.0, "Math.min 必须兜底到 1.0");
    }

    @Test
    void analyzeFull_应返回完整四元组() {
        Map<String, Object> result = service.analyzeFull("我太开心了");
        assertEquals("happy", result.get("emotion"));
        assertTrue(result.get("intensity") instanceof Number);
        assertNotNull(result.get("expression"));
        assertNotNull(result.get("action"));
    }
}
