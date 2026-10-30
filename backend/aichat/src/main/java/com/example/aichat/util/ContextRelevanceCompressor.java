package com.example.aichat.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对话上下文相关性压缩纯函数（算法升级 3）。
 *
 * <p>现状为简单截断，升级为相关性裁剪：
 * <ul>
 *   <li>始终保留系统消息（role=system，含角色人设 / 记忆引用前缀）</li>
 *   <li>始终保留最近 N 轮对话</li>
 *   <li>中间轮次按与最新 query 的相关性打分（字符 bigram 重叠），高相关保留、低相关丢弃</li>
 *   <li>命中"记忆/人设"等关键标记的中间消息强制保留，不丢关键信息</li>
 *   <li>最后按 maxTokens（按字符数估算）截断超预算消息</li>
 * </ul>
 * 静态纯函数，无 Spring 依赖。
 */
public final class ContextRelevanceCompressor {

    /** 始终保留的最近轮数（不含系统消息）。 */
    public static final int RECENT_TURNS = 4;
    /** 关键信息标记：命中即强制保留。 */
    private static final String[] KEY_MARKERS = {"记忆", "人设", "关于用户", "长期记忆", "性格"};
    /** 中文按字符 token 估算系数：约 1 个汉字 ≈ 1 token（保守估计）。 */
    private static final double CHARS_PER_TOKEN = 1.0;

    private ContextRelevanceCompressor() {
    }

    /**
     * 压缩上下文。
     *
     * @param messages  消息列表，每条含 role(String)/content(String)
     * @param query     最新用户 query（相关性打分基准）
     * @param maxTokens token 预算（按字符数估算）；&lt;=0 表示不做预算截断
     * @return 压缩后的消息列表（顺序与原列表一致）
     */
    public static List<Map<String, Object>> compressContext(List<Map<String, Object>> messages,
                                                            String query,
                                                            int maxTokens) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        int n = messages.size();
        Set<Integer> keep = new HashSet<>();

        // 1) 系统消息全保留
        for (int i = 0; i < n; i++) {
            if (isSystem(messages.get(i))) {
                keep.add(i);
            }
        }
        // 2) 最近 N 轮全保留
        int recentFrom = Math.max(0, n - RECENT_TURNS);
        for (int i = recentFrom; i < n; i++) {
            keep.add(i);
        }
        // 3) 中间消息：关键标记强制保留；否则按与 query 的 bigram 相关性打分
        Set<String> queryBigrams = bigrams(query);
        for (int i = 0; i < recentFrom; i++) {
            Map<String, Object> msg = messages.get(i);
            if (isSystem(msg)) {
                continue; // 已保留
            }
            String content = contentOf(msg);
            if (hasKeyMarker(content)) {
                keep.add(i);
                continue;
            }
            double rel = relevance(content, queryBigrams);
            if (rel >= 0.1) {
                keep.add(i);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (keep.contains(i)) {
                result.add(messages.get(i));
            }
        }

        // 4) token 预算截断（仅当仍超预算时，从尾部往前裁非系统、非最近的消息）
        if (maxTokens > 0) {
            result = enforceBudget(result, maxTokens);
        }
        return result;
    }

    private static List<Map<String, Object>> enforceBudget(List<Map<String, Object>> kept, int maxTokens) {
        int budget = (int) (maxTokens * CHARS_PER_TOKEN);
        int used = 0;
        List<Map<String, Object>> trimmed = new ArrayList<>();
        // 先从头累加，超预算则丢弃非系统且非最后一条的消息
        for (int i = 0; i < kept.size(); i++) {
            Map<String, Object> msg = kept.get(i);
            int len = contentOf(msg).length();
            if (used + len > budget && i > 0 && !isSystem(msg) && i < kept.size() - 1) {
                continue; // 丢弃该中间消息
            }
            trimmed.add(msg);
            used += len;
        }
        return trimmed;
    }

    private static boolean isSystem(Map<String, Object> msg) {
        Object role = msg.get("role");
        return role != null && "system".equalsIgnoreCase(role.toString());
    }

    private static String contentOf(Map<String, Object> msg) {
        Object c = msg.get("content");
        return c == null ? "" : c.toString();
    }

    private static boolean hasKeyMarker(String content) {
        for (String m : KEY_MARKERS) {
            if (content.contains(m)) {
                return true;
            }
        }
        return false;
    }

    /** 字符 bigram 集合（中文按字切分）。 */
    static Set<String> bigrams(String text) {
        Set<String> set = new HashSet<>();
        if (text == null) {
            return set;
        }
        String s = text.trim();
        for (int i = 0; i < s.length() - 1; i++) {
            set.add(s.substring(i, i + 2));
        }
        return set;
    }

    /** 内容与 query 的 bigram 重叠率（0~1）。 */
    static double relevance(String content, Set<String> queryBigrams) {
        if (content == null || queryBigrams == null || queryBigrams.isEmpty()) {
            return 0.0;
        }
        Set<String> cb = bigrams(content);
        if (cb.isEmpty()) {
            return 0.0;
        }
        int overlap = 0;
        for (String g : cb) {
            if (queryBigrams.contains(g)) {
                overlap++;
            }
        }
        return (double) overlap / cb.size();
    }
}
