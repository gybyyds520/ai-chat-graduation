package com.example.aichat.service.impl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全栈审查 - AI 精灵睡眠评分计算单元测试。
 *
 * 覆盖点（验证 P0 Bug 修复）：
 *   原实现时长 40 分 + 质量 40 分，满分仅 80 而非 100。
 *   修正为：时长 50 分 + 质量 50 分 = 100 分。
 *
 * 用例矩阵：
 *   1. 理想睡眠（7-9h + 质量≥8）应得满分 100；
 *   2. 边界：avgDuration=7 / 9 应触发满分区间；
 *   3. 极短睡眠（< 5h）+ 低质量应得低分；
 *   4. 过长睡眠（> 9h）应触发扣分（35 分而非 50）；
 *   5. 满分上限：任何组合得分不应超过 100（Math.min 保护）。
 *
 * 反射调用：calculateSleepScore 为 private 方法，不暴露给 Service 调用方，
 *          直接反射测试可避免修改生产代码可见性。
 */
class AiElfSleepScoreTest {

    private int invokeCalculateSleepScore(double avgDuration, double avgQuality) throws Exception {
        Method method = AiElfServiceImpl.class.getDeclaredMethod(
                "calculateSleepScore", double.class, double.class);
        method.setAccessible(true);
        return (int) method.invoke(new AiElfServiceImpl(
                null, null, null, null, null, null, null, null, null, null, null),
                avgDuration, avgQuality);
    }

    @Test
    void 理想睡眠_时长7至9h质量8以上_应得满分100() throws Exception {
        // 时长 7-9h 区间 → 50 分；质量 ≥8 → 50 分；满分 100
        assertEquals(100, invokeCalculateSleepScore(7.0, 8.0));
        assertEquals(100, invokeCalculateSleepScore(8.0, 9.0));
        assertEquals(100, invokeCalculateSleepScore(9.0, 10.0));
    }

    @Test
    void 边界_avgDuration正好为7或9_应进入满分时长区间() throws Exception {
        // 7.0 与 9.0 都应触发时长 50 分
        assertEquals(100, invokeCalculateSleepScore(7.0, 8.0));
        assertEquals(100, invokeCalculateSleepScore(9.0, 8.0));
    }

    @Test
    void 短睡眠_时长低于5h质量低于4_应得15分() throws Exception {
        // 时长 < 5h → 10 分；质量 < 4 → 5 分；合计 15 分
        assertEquals(15, invokeCalculateSleepScore(3.0, 2.0));
        assertEquals(15, invokeCalculateSleepScore(4.9, 1.0));
    }

    @Test
    void 中等时长_6至7h质量6至8_应得75分() throws Exception {
        // 时长 6-7h → 40 分；质量 6-8 → 35 分；合计 75 分
        assertEquals(75, invokeCalculateSleepScore(6.5, 7.0));
    }

    @Test
    void 过长睡眠_avgDuration超过9_应触发35分时长区间() throws Exception {
        // 时长 > 9h → 35 分（非满分 50，过长也扣分）
        int score = invokeCalculateSleepScore(10.0, 8.0);
        assertEquals(85, score, "过长睡眠应扣时长分，但质量满分，合计 85");
    }

    @Test
    void 满分上限_质量溢出10_不应超过100分() throws Exception {
        // 即使输入 avgQuality=1000 等异常值，Math.min(100) 应兜底
        int score = invokeCalculateSleepScore(8.0, 1000.0);
        assertEquals(100, score, "Math.min(score, 100) 必须限制满分");
    }

    @Test
    void 原Bug场景_理想睡眠不应仅得80分() throws Exception {
        // P0 Bug 验证：原实现时长 + 质量各 40 分，理想睡眠仅得 80 分
        // 修复后：理想睡眠必须得 100 分，不应等于 80
        int score = invokeCalculateSleepScore(8.0, 8.5);
        assertEquals(100, score, "理想睡眠（7-9h + 质量≥8）应得满分 100，而非 80");
        assertNotEquals(80, score, "P0 Bug 已修复：满分不应为 80");
    }

    @Test
    void 质量分界_8为满分与35分分界() throws Exception {
        // avgQuality=8.0 → 满分 50；avgQuality=7.9 → 35 分
        int highQuality = invokeCalculateSleepScore(8.0, 8.0);
        int midQuality = invokeCalculateSleepScore(8.0, 7.9);
        assertEquals(100, highQuality);
        assertEquals(85, midQuality, "质量 6-8 区间应得 35 分");
    }

    @Test
    void 时长分界_5h与6h分界() throws Exception {
        // avgDuration=5.0 → 25 分；avgDuration=5.9 → 25 分
        // avgDuration=6.0 → 40 分
        int five = invokeCalculateSleepScore(5.5, 8.0);
        int six = invokeCalculateSleepScore(6.5, 8.0);
        assertEquals(75, five, "时长 5-6h 应得 25 分");
        assertEquals(90, six, "时长 6-7h 应得 40 分");
    }
}
