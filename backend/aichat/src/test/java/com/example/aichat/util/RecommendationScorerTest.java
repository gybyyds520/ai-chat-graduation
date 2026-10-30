package com.example.aichat.util;

import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.UserProfile;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 好友/角色推荐融合排序纯函数测试（≥3 用例）。
 */
class RecommendationScorerTest {

    private AiCharacter char_ (long id, String tags, int useCount) {
        AiCharacter c = new AiCharacter();
        c.setId(id);
        c.setTags(tags);
        c.setUseCount(useCount);
        return c;
    }

    private UserProfile profile(String commonTopicsJson) {
        UserProfile p = new UserProfile();
        p.setCommonTopics(commonTopicsJson);
        return p;
    }

    @Test
    void score_兴趣共现贡献_高相似角色得分更高() {
        List<AiCharacter> pool = Arrays.asList(
                char_(1L, "学习,代码", 100),
                char_(2L, "游戏,动漫", 100));
        UserProfile user = profile("[\"学习\",\"代码\",\"电影\"]");
        double sStudy = RecommendationScorer.calculateRecommendationScore(pool.get(0), user, pool);
        double sGame = RecommendationScorer.calculateRecommendationScore(pool.get(1), user, pool);
        // 学习角色与用户 topics 完全共现，兴趣相似度=1；游戏角色=0
        assertTrue(sStudy > sGame, "兴趣匹配的角色应排前面: " + sStudy + " vs " + sGame);
    }

    @Test
    void score_热度归一化_useCount高者得分高() {
        List<AiCharacter> pool = Arrays.asList(
                char_(1L, "学习", 1000),
                char_(2L, "学习", 10));
        UserProfile user = profile("[\"学习\"]");
        double sHot = RecommendationScorer.calculateRecommendationScore(pool.get(0), user, pool);
        double sCold = RecommendationScorer.calculateRecommendationScore(pool.get(1), user, pool);
        // 两者兴趣相似度相同(1)，热度 1.0 vs 0.01，新鲜度相近(id/2)
        assertTrue(sHot > sCold);
    }

    @Test
    void score_新鲜度_新角色略高() {
        List<AiCharacter> pool = Arrays.asList(
                char_(1L, "学习", 50),
                char_(100L, "学习", 50));
        UserProfile user = profile("[\"学习\"]");
        double sOld = RecommendationScorer.calculateRecommendationScore(pool.get(0), user, pool);
        double sNew = RecommendationScorer.calculateRecommendationScore(pool.get(1), user, pool);
        // 兴趣/热度相同，新鲜度 0.01 vs 1.0 → 新角色更高
        assertTrue(sNew > sOld);
    }

    @Test
    void score_空画像_不报错() {
        List<AiCharacter> pool = Arrays.asList(char_(1L, "学习", 10));
        double s = RecommendationScorer.calculateRecommendationScore(pool.get(0), null, pool);
        // 兴趣=0，热度=1，新鲜度=1 → 0.3*1 + 0.2*1 = 0.5
        assertEquals(0.5, s, 1e-9);
    }

    @Test
    void score_权重和为1_分数量纲合理() {
        List<AiCharacter> pool = Arrays.asList(char_(5L, "学习", 100));
        UserProfile user = profile("[\"学习\"]");
        double s = RecommendationScorer.calculateRecommendationScore(pool.get(0), user, pool);
        assertTrue(s >= 0.0 && s <= 1.0, "得分应归一化在 [0,1]: " + s);
    }
}
