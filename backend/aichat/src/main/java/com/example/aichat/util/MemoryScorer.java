package com.example.aichat.util;

import com.example.aichat.entity.AiMemory;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 记忆评分 / 衰减 / 召回融合排序纯函数（市场级升级）。
 *
 * <p>全部为静态无副作用方法，便于单元测试。时间均以 {@code LocalDateTime} 传入，
 * 不在内部调用 {@code now()}，保证测试可复现。
 *
 * <p>衰减公式：
 * <pre>
 *   decay(lastReferencedAt, now) = 1 / (1 + daysSinceLastReference / 30)
 *   score = baseImportance * decay
 * </pre>
 * 钉选记忆（pinned=true）不衰减（decay=1）；常引用记忆（referencedCount &gt; 5）加权 ×1.5。
 *
 * <p>向量召回融合排序：
 * <pre>
 *   importanceWeight = importance / 5
 *   finalScore = vectorSimilarity * 0.5 + timeDecay * 0.3 * importanceWeight
 *   钉选记忆 ×1.5 加成
 * </pre>
 */
public final class MemoryScorer {

    /** 衰减半衰期窗口（天）：距上次引用 30 天时衰减系数为 0.5。 */
    public static final double DECAY_WINDOW_DAYS = 30.0;
    /** 常引用阈值：引用次数超过该值给予 ×1.5 加权。 */
    public static final int FREQUENT_REFERENCE_THRESHOLD = 5;
    /** 常引用 / 钉选加权系数。 */
    public static final double BOOST = 1.5;

    private MemoryScorer() {
    }

    /**
     * 计算单条记忆的综合召回得分（未融合向量相似度时使用）。
     *
     * @param memory 记忆实体（可为 null 字段）
     * @param now    当前时间
     * @return 得分；pinned 记忆不衰减，常引用记忆 ×1.5
     */
    public static double calculateMemoryScore(AiMemory memory, LocalDateTime now) {
        if (memory == null) {
            return 0.0;
        }
        int base = memory.getImportance() == null ? 3 : Math.max(1, Math.min(5, memory.getImportance()));
        double decay = shouldDecay(memory) ? decayFactor(memory.getLastUsedAt(), now) : 1.0;
        double score = base * decay;
        int refCount = memory.getReferencedCount() == null ? 0 : memory.getReferencedCount();
        if (refCount > FREQUENT_REFERENCE_THRESHOLD) {
            score *= BOOST;
        }
        return score;
    }

    /**
     * 该记忆是否参与时间衰减。钉选记忆不衰减。
     */
    public static boolean shouldDecay(AiMemory memory) {
        if (memory == null) {
            return true;
        }
        return !Boolean.TRUE.equals(memory.getPinned());
    }

    /**
     * 衰减系数 = 1 / (1 + daysSinceLastReference / 30)。
     * lastUsedAt 为空时视为从未引用（按创建至今计；无创建时间则不衰减=1）。
     */
    public static double decayFactor(LocalDateTime lastReferencedAt, LocalDateTime now) {
        if (lastReferencedAt == null || now == null) {
            return 1.0;
        }
        if (!now.isAfter(lastReferencedAt)) {
            return 1.0;
        }
        long days = Math.max(0, Duration.between(lastReferencedAt, now).toDays());
        return 1.0 / (1.0 + days / DECAY_WINDOW_DAYS);
    }

    /**
     * 向量召回融合排序得分。
     *
     * @param vectorScore 向量语义相似度（0~1）
     * @param memory       记忆实体
     * @param now          当前时间
     * @return 融合后得分；钉选记忆 ×1.5
     */
    public static double fuseMemoryRanking(double vectorScore, AiMemory memory, LocalDateTime now) {
        double vs = Math.max(0.0, Math.min(1.0, vectorScore));
        int importance = memory == null || memory.getImportance() == null
                ? 3 : Math.max(1, Math.min(5, memory.getImportance()));
        double importanceWeight = importance / 5.0;
        double timeDecay = shouldDecay(memory) ? decayFactor(memory == null ? null : memory.getLastUsedAt(), now) : 1.0;
        double finalScore = vs * 0.5 + timeDecay * 0.3 * importanceWeight;
        if (memory != null && Boolean.TRUE.equals(memory.getPinned())) {
            finalScore *= BOOST;
        }
        return finalScore;
    }
}
