package com.example.aichat.util;

/**
 * AI 记忆自动分类枚举（市场级升级：记忆自动分类）。
 *
 * <p>四类标签：
 * <ul>
 *   <li>{@link #FACT}         事实 —— 用户是谁、住哪、做什么等客观事实</li>
 *   <li>{@link #PREFERENCE}   偏好 —— 喜欢/讨厌/偏好类主观倾向</li>
 *   <li>{@link #EVENT}        事件 —— 上次/昨天/记得 等带时间线的事件回忆</li>
 *   <li>{@link #RELATIONSHIP} 关系 —— 朋友/家人/同事 等人际关系</li>
 * </ul>
 *
 * <p>纯枚举，无 Spring 依赖，便于单元测试。
 */
public enum MemoryCategory {
    FACT,
    PREFERENCE,
    EVENT,
    RELATIONSHIP;

    /** 大小写不敏感解析；无法识别时回退 {@link #FACT}。 */
    public static MemoryCategory fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return FACT;
        }
        try {
            return MemoryCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FACT;
        }
    }
}
