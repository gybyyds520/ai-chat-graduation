package com.example.aichat.util;

import com.example.aichat.entity.AiMemory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 记忆衰减 / 优先级 / 融合排序纯函数测试。
 * 衰减计算 ≥3 用例；融合排序 ≥2 用例。
 */
class MemoryScorerTest {

    private AiMemory mem(int importance, Integer referencedCount, Boolean pinned, LocalDateTime lastUsedAt) {
        AiMemory m = new AiMemory();
        m.setImportance(importance);
        m.setReferencedCount(referencedCount);
        m.setPinned(pinned);
        m.setLastUsedAt(lastUsedAt);
        return m;
    }

    // ---------- 衰减计算（≥3） ----------

    @Test
    void decay_刚引用_不衰减() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        AiMemory m = mem(3, 0, false, now);
        // decay = 1/(1+0/30)=1，score = 3*1 = 3
        assertEquals(3.0, MemoryScorer.calculateMemoryScore(m, now), 1e-9);
    }

    @Test
    void decay_30天未引用_衰减一半() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        LocalDateTime last = now.minusDays(30);
        AiMemory m = mem(4, 0, false, last);
        // decay = 1/(1+30/30)=0.5，score = 4*0.5 = 2
        assertEquals(2.0, MemoryScorer.calculateMemoryScore(m, now), 1e-9);
    }

    @Test
    void decay_90天未引用_衰减到四分之一() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        LocalDateTime last = now.minusDays(90);
        AiMemory m = mem(4, 0, false, last);
        // decay = 1/(1+90/30)=1/4=0.25，score = 4*0.25 = 1
        assertEquals(1.0, MemoryScorer.calculateMemoryScore(m, now), 1e-9);
    }

    @Test
    void decay_钉选记忆_不衰减() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        LocalDateTime last = now.minusDays(90);
        AiMemory m = mem(5, 0, true, last);
        assertFalse(MemoryScorer.shouldDecay(m));
        // pinned -> decay=1，score=5
        assertEquals(5.0, MemoryScorer.calculateMemoryScore(m, now), 1e-9);
    }

    @Test
    void decay_常引用记忆_加权15倍() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        AiMemory m = mem(3, 6, false, now); // referencedCount=6 > 5
        assertTrue(MemoryScorer.shouldDecay(m));
        // score = 3*1*1.5 = 4.5
        assertEquals(4.5, MemoryScorer.calculateMemoryScore(m, now), 1e-9);
    }

    @Test
    void decay_lastUsedAt为空_不衰减() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        AiMemory m = mem(3, 0, false, null);
        assertEquals(3.0, MemoryScorer.calculateMemoryScore(m, now), 1e-9);
    }

    // ---------- 融合排序（≥2） ----------

    @Test
    void fuse_基础融合_向量08重要度5() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        AiMemory m = mem(5, 0, false, now); // importanceWeight=1, timeDecay=1
        // 0.8*0.5 + 1*0.3*1 = 0.4 + 0.3 = 0.7
        assertEquals(0.7, MemoryScorer.fuseMemoryRanking(0.8, m, now), 1e-9);
    }

    @Test
    void fuse_钉选记忆_15加成() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        AiMemory m = mem(5, 0, true, now);
        double base = 0.8 * 0.5 + 1.0 * 0.3 * 1.0; // 0.7
        assertEquals(base * 1.5, MemoryScorer.fuseMemoryRanking(0.8, m, now), 1e-9);
    }

    @Test
    void fuse_旧记忆时间衰减_拉低得分() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        LocalDateTime last = now.minusDays(30); // decay=0.5
        AiMemory m = mem(5, 0, false, last);
        // 0.8*0.5 + 0.5*0.3*1.0 = 0.4 + 0.15 = 0.55
        assertEquals(0.55, MemoryScorer.fuseMemoryRanking(0.8, m, now), 1e-9);
    }

    @Test
    void fuse_重要度低_importanceWeight小() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 24, 12, 0);
        AiMemory m = mem(1, 0, false, now); // importanceWeight=0.2
        // 0.8*0.5 + 1*0.3*0.2 = 0.4 + 0.06 = 0.46
        assertEquals(0.46, MemoryScorer.fuseMemoryRanking(0.8, m, now), 1e-9);
    }
}
