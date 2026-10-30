package com.example.aichat.util;

/**
 * 记忆自动分类器（纯函数，无 Spring / DB 依赖）。
 *
 * <p>分类规则（按优先级从上到下命中即返回）：
 * <ol>
 *   <li>含"喜欢 / 讨厌 / 偏好"           -> {@link MemoryCategory#PREFERENCE}</li>
 *   <li>含"上次 / 昨天 / 记得"           -> {@link MemoryCategory#EVENT}</li>
 *   <li>含"朋友 / 家人 / 同事"           -> {@link MemoryCategory#RELATIONSHIP}</li>
 *   <li>其他                              -> {@link MemoryCategory#FACT}</li>
 * </ol>
 *
 * <p>设计为静态纯函数，便于在 Service 落库前对记忆内容打标签，
 * 也便于独立单元测试（≥4 用例）。
 */
public final class MemoryClassifier {

    private MemoryClassifier() {
    }

    private static final String[] PREFERENCE_KEYWORDS = {"喜欢", "讨厌", "偏好"};
    private static final String[] EVENT_KEYWORDS = {"上次", "昨天", "记得"};
    private static final String[] RELATIONSHIP_KEYWORDS = {"朋友", "家人", "同事"};

    /**
     * 根据记忆内容自动归类。
     *
     * @param content 记忆原文（可空）
     * @return 分类枚举；内容为空时回退 {@link MemoryCategory#FACT}
     */
    public static MemoryCategory classify(String content) {
        if (content == null || content.isBlank()) {
            return MemoryCategory.FACT;
        }
        String text = content.trim();
        if (containsAny(text, PREFERENCE_KEYWORDS)) {
            return MemoryCategory.PREFERENCE;
        }
        if (containsAny(text, EVENT_KEYWORDS)) {
            return MemoryCategory.EVENT;
        }
        if (containsAny(text, RELATIONSHIP_KEYWORDS)) {
            return MemoryCategory.RELATIONSHIP;
        }
        return MemoryCategory.FACT;
    }

    private static boolean containsAny(String text, String[] keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
