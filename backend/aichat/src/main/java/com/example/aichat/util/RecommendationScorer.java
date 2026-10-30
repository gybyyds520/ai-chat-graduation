package com.example.aichat.util;

import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.UserProfile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 好友 / 角色推荐融合排序纯函数（算法升级 1）。
 *
 * <p>现状为简单标签匹配，升级为三维加权融合：
 * <pre>
 *   score = 0.5 * interestSimilarity + 0.3 * popularity + 0.2 * freshness
 * </pre>
 * <ul>
 *   <li>interestSimilarity：用户画像 commonTopics 与角色 tags 的话题共现（Jaccard 相似度）</li>
 *   <li>popularity：useCount / 候选池最大 useCount（归一化 0~1）</li>
 *   <li>freshness：角色 id / 候选池最大 id（自增主键近似创建时间，越新越接近 1）</li>
 * </ul>
 * 全部静态纯函数，无 Spring/DB 依赖。
 */
public final class RecommendationScorer {

    public static final double W_INTEREST = 0.5;
    public static final double W_POPULARITY = 0.3;
    public static final double W_FRESHNESS = 0.2;

    private RecommendationScorer() {
    }

    /**
     * 计算单个角色对指定用户画像的推荐分。
     *
     * @param character   待推荐角色
     * @param userProfile 用户画像（commonTopics 为 JSON 数组字符串，可空）
     * @param allCharacters 候选池全部公开角色（用于归一化 popularity/freshness）
     */
    public static double calculateRecommendationScore(AiCharacter character,
                                                     UserProfile userProfile,
                                                     List<AiCharacter> allCharacters) {
        if (character == null) {
            return 0.0;
        }
        double interest = interestSimilarity(character, userProfile);
        double popularity = popularity(character, allCharacters);
        double freshness = freshness(character, allCharacters);
        return W_INTEREST * interest + W_POPULARITY * popularity + W_FRESHNESS * freshness;
    }

    /**
     * 兴趣相似度：用户 commonTopics 与角色 tags 的 Jaccard 相似度（0~1）。
     */
    static double interestSimilarity(AiCharacter character, UserProfile userProfile) {
        Set<String> userTopics = parseTopics(userProfile == null ? null : userProfile.getCommonTopics());
        Set<String> charTags = parseCsv(character.getTags());
        if (userTopics.isEmpty() || charTags.isEmpty()) {
            return 0.0;
        }
        Set<String> intersect = new HashSet<>(userTopics);
        intersect.retainAll(charTags);
        Set<String> union = new HashSet<>(userTopics);
        union.addAll(charTags);
        return union.isEmpty() ? 0.0 : (double) intersect.size() / union.size();
    }

    /** 热度归一化：useCount / 候选池最大 useCount。 */
    static double popularity(AiCharacter character, List<AiCharacter> allCharacters) {
        int self = character.getUseCount() == null ? 0 : character.getUseCount();
        int max = 0;
        if (allCharacters != null) {
            for (AiCharacter c : allCharacters) {
                if (c != null && c.getUseCount() != null && c.getUseCount() > max) {
                    max = c.getUseCount();
                }
            }
        }
        if (max <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) self / max);
    }

    /** 新鲜度归一化：自增 id / 候选池最大 id（id 越大越新）。 */
    static double freshness(AiCharacter character, List<AiCharacter> allCharacters) {
        Long self = character.getId();
        if (self == null || allCharacters == null || allCharacters.isEmpty()) {
            return 0.0;
        }
        long max = Long.MIN_VALUE;
        for (AiCharacter c : allCharacters) {
            if (c != null && c.getId() != null && c.getId() > max) {
                max = c.getId();
            }
        }
        if (max <= 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) self / max);
    }

    /** 解析 JSON 数组字符串（如 ["学习","游戏"]）为小写去重集合。 */
    private static Set<String> parseTopics(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) {
            return new HashSet<>();
        }
        String cleaned = jsonArray.replaceAll("[\\[\\]\"']", "");
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /** 解析逗号分隔标签字符串为小写去重集合。 */
    private static Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return new HashSet<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
