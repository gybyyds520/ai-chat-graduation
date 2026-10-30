package com.example.aichat.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 精灵养成数值曲线纯函数测试（≥2 用例）。
 */
class ElfCurveTest {

    @Test
    void smoothDecay_指数衰减_随时间单调下降() {
        double t0 = ElfCurve.smoothDecay(100, 100, 0.1, 0);
        double t1 = ElfCurve.smoothDecay(100, 100, 0.1, 1);
        double t2 = ElfCurve.smoothDecay(100, 100, 0.1, 5);
        assertEquals(100.0, t0, 1e-9);           // time=0 不衰减
        assertTrue(t1 < t0 && t2 < t1, "应单调递减: " + t0 + ">" + t1 + ">" + t2);
        assertTrue(t1 > t0 * Math.exp(-0.1) - 1e-9);
    }

    @Test
    void smoothDecay_不超过上限不为负() {
        assertEquals(100.0, ElfCurve.smoothDecay(100, 100, 0.1, 0), 1e-9);
        assertTrue(ElfCurve.smoothDecay(100, 100, 1.0, 100) >= 0.0);
    }

    @Test
    void sleepRecovery_对数曲线_前2小时快恢复边际递减() {
        double r1 = ElfCurve.sleepRecovery(1, 100);
        double r2 = ElfCurve.sleepRecovery(2, 100);
        double r8 = ElfCurve.sleepRecovery(8, 100);
        assertTrue(r1 > 0 && r2 > r1, "应随睡眠时长增加");
        assertEquals(100.0, r8, 1e-9); // 8小时睡满
        // 第1小时恢复量 应大于 第7->8小时恢复量（边际递减）
        double firstStep = r1 - 0.0;
        double lastStep = r8 - ElfCurve.sleepRecovery(7, 100);
        assertTrue(firstStep > lastStep, "前期恢复应快于后期: " + firstStep + " vs " + lastStep);
    }

    @Test
    void logarithmicGrowth_前期快后期慢_100次到顶() {
        double g0 = ElfCurve.logarithmicGrowth(0, 1000, 0);
        double g1 = ElfCurve.logarithmicGrowth(0, 1000, 1);
        double g99 = ElfCurve.logarithmicGrowth(0, 1000, 99);
        double g100 = ElfCurve.logarithmicGrowth(0, 1000, 100);
        assertEquals(1000.0, g100, 1e-9); // 100 次到顶
        assertTrue(g1 > g0, "应单调递增");
        // 边际增量：第 1 次交互的增量 应远大于 第 100 次交互的增量（对数前期快后期慢）
        double firstStep = g1 - g0;
        double lastStep = g100 - g99;
        assertTrue(firstStep > lastStep * 10,
                "首次增量应远大于末次增量: " + firstStep + " vs " + lastStep);
    }
}
