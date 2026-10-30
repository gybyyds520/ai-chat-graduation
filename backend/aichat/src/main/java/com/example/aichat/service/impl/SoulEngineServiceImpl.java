package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aichat.entity.AiCharacter;
import com.example.aichat.entity.AiElf;
import com.example.aichat.entity.AiSoulExperience;
import com.example.aichat.entity.AiSoulState;
import com.example.aichat.mapper.AiElfMapper;
import com.example.aichat.mapper.AiSoulExperienceMapper;
import com.example.aichat.mapper.AiSoulStateMapper;
import com.example.aichat.service.AiCharacterService;
import com.example.aichat.service.AiMemoryService;
import com.example.aichat.service.EmotionAnalysisService;
import com.example.aichat.service.SoulEngineService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 灵魂引擎实现 —— 用户原创灵魂算法的工程化。
 *
 * 算法：S(t+1) = f( S(t), E(t), I(t), M(t) )
 *   S(t)  这一刻的自己：情绪、心情值、态度温度、性格权重、核心信念
 *   E(t)  发生的经历：本轮对话（用户消息 + AI 回复）
 *   I(t)  如何感受和理解：LLM 以角色人格生成"感受 + 认知评价"（失败降级规则情绪分析）
 *   M(t)  过去留下的记忆：ai_memory 长期记忆（召回相关） + 灵魂经历流水
 *   S(t+1) 下一刻的自己：情绪迁移 + 心情/态度漂移 + 性格权重微调 + 信念沉淀
 *
 * 降级链：Ollama 可用 → LLM 生成感受/理解；不可用 → EmotionAnalysisService 规则 + 模板。
 * 异步演化：绝不阻塞主对话链路，任何异常静默记录。
 */
@Service
public class SoulEngineServiceImpl implements SoulEngineService {

    private static final Logger logger = LoggerFactory.getLogger(SoulEngineServiceImpl.class);

    /** 情绪迁移映射：用户情绪 → 灵魂情绪变化 */
    private static final Map<String, String> EMOTION_TRANSITION = Map.of(
            "happy", "happy",
            "love", "love",
            "sad", "sad",
            "angry", "angry",
            "surprised", "surprised",
            "fearful", "fearful",
            "disgusted", "sad",
            "neutral", "neutral"
    );

    /** 心情漂移：情绪 → 心情增量 */
    private static final Map<String, Integer> MOOD_DELTA = Map.of(
            "happy", 4, "love", 5, "surprised", 3,
            "sad", -3, "angry", -4, "fearful", -3,
            "disgusted", -2, "neutral", 0
    );

    /** 态度漂移：情绪 → 态度增量（用户传递情绪影响亲近度） */
    private static final Map<String, Integer> ATTITUDE_DELTA = Map.of(
            "happy", 2, "love", 3, "surprised", 1,
            "sad", -1, "angry", -2, "fearful", -1,
            "disgusted", -2, "neutral", 0
    );

    private static final Pattern JSON_FIELD = Pattern.compile("\"(\\w+)\"\\s*:\\s*([0-9.]+)");

    @Autowired
    private AiSoulStateMapper soulStateMapper;

    @Autowired
    private AiSoulExperienceMapper soulExperienceMapper;

    @Autowired
    private AiElfMapper aiElfMapper;

    @Autowired
    private AiCharacterService aiCharacterService;

    @Autowired
    private AiMemoryService aiMemoryService;

    @Autowired
    private EmotionAnalysisService emotionAnalysisService;

    @Autowired(required = false)
    private com.example.aichat.service.ai.OllamaService ollamaService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 核心：对话后演化 ====================

    @Override
    public void evolveAfterChat(Long userId, Long characterId, Long elfId, String userMessage, String aiReply) {
        try {
            AiSoulState soul = getOrInitSoulState(userId, characterId);
            if (soul == null) return;

            // 1. E(t)：经历摘要（截断，避免超长）
            String experienceText = buildExperienceText(userMessage, aiReply);

            // 2. I(t)：感受与理解（LLM 生成，失败降级）
            Map<String, String> interpretation = interpret(userId, characterId, soul, userMessage, aiReply);

            // 3. M(t)：过去留下的记忆（召回相关 + 本次沉淀）
            List<String> relevantMemories = recallMemories(userId, characterId, userMessage);

            // 4. 演化：情绪迁移 + 心情/态度漂移 + 性格微调 + 信念沉淀
            String userEmotion = emotionAnalysisService.analyzeEmotion(userMessage);
            String nextEmotion = EMOTION_TRANSITION.getOrDefault(userEmotion, "neutral");
            int moodDelta = MOOD_DELTA.getOrDefault(userEmotion, 0);
            int attitudeDelta = ATTITUDE_DELTA.getOrDefault(userEmotion, 0);

            int nextMood = clamp(soul.getMoodScore() + moodDelta, 0, 100);
            int nextAttitude = clamp(soul.getAttitudeScore() + attitudeDelta, -100, 100);
            String nextWeights = evolvePersonalityWeights(soul.getPersonalityWeights(), userEmotion);

            // 信念沉淀：理解(understanding)提炼为新的核心信念片段（不超过 2 句）
            String belief = interpretation.get("understanding");
            String nextBeliefs = evolveCoreBeliefs(soul.getCoreBeliefs(), belief);

            // 下一刻的自我描述
            String nextSummary = buildSelfSummary(nextEmotion, nextMood, nextAttitude, belief);

            // 5. 写入 S(t+1)
            soul.setCurrentEmotion(nextEmotion);
            soul.setMoodScore(nextMood);
            soul.setAttitudeScore(nextAttitude);
            soul.setPersonalityWeights(nextWeights);
            soul.setCoreBeliefs(nextBeliefs);
            soul.setSelfSummary(nextSummary);
            soul.setVersion(soul.getVersion() == null ? 2 : soul.getVersion() + 1);
            soul.setElfId(elfId != null ? elfId : soul.getElfId());
            soul.setUpdatedAt(LocalDateTime.now());
            soulStateMapper.updateById(soul);

            // 6. 沉淀经历流水
            AiSoulExperience exp = new AiSoulExperience();
            exp.setUserId(userId);
            exp.setCharacterId(characterId);
            exp.setElfId(elfId);
            exp.setExperience(experienceText);
            exp.setFeeling(interpretation.get("feeling"));
            exp.setUnderstanding(belief);
            exp.setMemoryRef(buildMemoryRef(userMessage, interpretation));
            exp.setImpact(buildImpact(nextEmotion, moodDelta, attitudeDelta));
            exp.setCreatedAt(LocalDateTime.now());
            soulExperienceMapper.insert(exp);

            // 7. 重要经历沉淀为长期记忆（ai_memory，source=soul，去重由服务层保证）
            persistMemory(userId, characterId, userMessage, interpretation);

            logger.info("灵魂演化完成 - userId: {}, characterId: {}, version: {}, emotion: {}->{}",
                    userId, characterId, soul.getVersion(), soul.getCurrentEmotion(), nextEmotion);
        } catch (Exception e) {
            // 灵魂演化是增强功能：任何失败都不能影响主对话链路
            logger.warn("灵魂演化失败，忽略: {}", e.getMessage());
        }
    }

    // ==================== prompt 注入 ====================

    @Override
    public String buildSoulPrompt(Long userId, Long characterId) {
        AiSoulState soul = getSoulState(userId, characterId);
        if (soul == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("【灵魂状态·此刻的我】\n");
        sb.append("当前情绪：").append(describeEmotion(soul.getCurrentEmotion()))
          .append("（心情值 ").append(soul.getMoodScore()).append("/100）\n");
        sb.append("对用户的态度：").append(describeAttitude(soul.getAttitudeScore())).append("\n");
        if (soul.getCoreBeliefs() != null && !soul.getCoreBeliefs().isBlank()) {
            sb.append("我最近在想：").append(soul.getCoreBeliefs().trim()).append("\n");
        }
        if (soul.getSelfSummary() != null && !soul.getSelfSummary().isBlank()) {
            sb.append(soul.getSelfSummary().trim()).append("\n");
        }
        sb.append("以上为我此刻的内在状态，请自然地以此自居回应（不要向用户复述本段）。\n\n");
        return sb.toString();
    }

    // ==================== 精灵灵魂（elf_id 维度） ====================

    /**
     * 精灵互动后演化精灵灵魂。
     * 精灵没有角色 prompt，以精灵人格（personality/theme）为灵魂基底；
     * 用负 characterId（-elfId）编码精灵灵魂维度，避免与角色 ID 空间冲突；
     * 演化后把灵魂 moodScore 同步回精灵 mood（双向联动）。
     */
    @Override
    public void evolveElfSoul(Long userId, Long elfId, String elfExperience) {
        try {
            if (userId == null || elfId == null) return;
            AiElf elf = aiElfMapper.selectById(elfId);
            if (elf == null || !userId.equals(elf.getUserId())) {
                logger.warn("精灵灵魂演化失败：精灵不存在或归属不符 elfId={}", elfId);
                return;
            }
            Long soulCharacterId = -elfId; // 负 ID 编码精灵维度

            // 精灵灵魂基底：以精灵人格/主题初始化（首次）
            AiSoulState soul = getSoulState(userId, soulCharacterId);
            if (soul == null) {
                soul = initElfSoul(userId, elfId, elf);
            }

            // 经历 E(t)：互动描述（喂食/摸头/玩耍等均为正向经历）
            String experienceText = (elfExperience == null || elfExperience.isBlank())
                    ? "用户与精灵进行了一次互动" : elfExperience.trim();

            // 感受与理解 I(t)：正向互动 → 温暖/开心的感受（规则降级，LLM 可增强但非必需）
            String feeling = describeElfFeeling(experienceText);
            String understanding = "精灵感受到被关爱与陪伴，这让她更加珍惜与用户的关系。";

            // 演化：心情上升、态度（亲近度）上升、性格微调
            int nextMood = clamp((soul.getMoodScore() == null ? 60 : soul.getMoodScore()) + 3, 0, 100);
            int nextAttitude = clamp((soul.getAttitudeScore() == null ? 30 : soul.getAttitudeScore()) + 2, -100, 100);
            String nextWeights = evolvePersonalityWeights(soul.getPersonalityWeights(), "happy");
            String nextBeliefs = evolveCoreBeliefs(soul.getCoreBeliefs(), understanding);

            soul.setCurrentEmotion("happy");
            soul.setMoodScore(nextMood);
            soul.setAttitudeScore(nextAttitude);
            soul.setPersonalityWeights(nextWeights);
            soul.setCoreBeliefs(nextBeliefs);
            soul.setSelfSummary(buildSelfSummary("happy", nextMood, nextAttitude, understanding));
            soul.setVersion(soul.getVersion() == null ? 2 : soul.getVersion() + 1);
            soul.setElfId(elfId);
            soul.setUpdatedAt(LocalDateTime.now());
            soulStateMapper.updateById(soul);

            // 沉淀经历流水（elf 维度）
            AiSoulExperience exp = new AiSoulExperience();
            exp.setUserId(userId);
            exp.setCharacterId(soulCharacterId);
            exp.setElfId(elfId);
            exp.setExperience(experienceText);
            exp.setFeeling(feeling);
            exp.setUnderstanding(understanding);
            exp.setMemoryRef("精灵经历：" + experienceText + "，" + feeling);
            exp.setImpact("精灵心情 +3，对你的亲近 +2，灵魂版本 +1");
            exp.setCreatedAt(LocalDateTime.now());
            soulExperienceMapper.insert(exp);

            // 双向联动：灵魂心情写回精灵 mood（精灵养成四维）
            Integer elfMood = elf.getMood();
            if (elfMood == null || nextMood != elfMood) {
                elf.setMood(nextMood);
                elf.setUpdatedAt(LocalDateTime.now());
                aiElfMapper.updateById(elf);
            }

            logger.info("精灵灵魂演化完成 - userId: {}, elfId: {}, version: {}, mood: {}",
                    userId, elfId, soul.getVersion(), nextMood);
        } catch (Exception e) {
            // 精灵灵魂演化是增强功能：任何失败都不影响精灵养成主链路
            logger.warn("精灵灵魂演化失败，忽略: {}", e.getMessage());
        }
    }

    /** 精灵初始灵魂：以精灵人格/主题为基底 */
    private AiSoulState initElfSoul(Long userId, Long elfId, AiElf elf) {
        AiSoulState soul = new AiSoulState();
        soul.setUserId(userId);
        soul.setCharacterId(-elfId);
        soul.setElfId(elfId);
        soul.setCurrentEmotion("neutral");
        soul.setMoodScore(elf.getMood() != null ? clamp(elf.getMood(), 0, 100) : 80);
        soul.setAttitudeScore(40);
        soul.setPersonalityWeights(elfPersonalityWeights(elf));
        soul.setCoreBeliefs(elf.getPersonality() != null && !elf.getPersonality().isBlank()
                ? "我的性格是" + elf.getPersonality().trim() + "，我希望成为你安心的陪伴。"
                : "我希望成为你安心的陪伴。");
        soul.setSelfSummary("");
        soul.setVersion(1);
        soul.setCreatedAt(LocalDateTime.now());
        soul.setUpdatedAt(LocalDateTime.now());
        soulStateMapper.insert(soul);
        return soul;
    }

    /** 精灵性格 → 性格权重 */
    private String elfPersonalityWeights(AiElf elf) {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("温柔", 0.6);
        weights.put("活泼", 0.5);
        weights.put("理性", 0.3);
        weights.put("依赖", 0.5);
        String p = elf.getPersonality() == null ? "" : elf.getPersonality();
        String t = elf.getTheme() == null ? "" : elf.getTheme();
        if (p.contains("温") || p.contains("柔") || t.contains("治愈") || t.contains("温柔")) weights.put("温柔", 0.8);
        if (p.contains("活") || p.contains("元气") || t.contains("活泼") || t.contains("元气")) weights.put("活泼", 0.8);
        if (p.contains("理") || p.contains("冷静") || t.contains("知性")) weights.put("理性", 0.7);
        if (p.contains("依") || p.contains("粘") || t.contains("萌") || t.contains("黏")) weights.put("依赖", 0.7);
        try {
            return objectMapper.writeValueAsString(weights);
        } catch (Exception e) {
            return "{\"温柔\":0.6,\"活泼\":0.5,\"理性\":0.3,\"依赖\":0.5}";
        }
    }

    /** 精灵经历 → 感受文本 */
    private String describeElfFeeling(String experience) {
        if (experience.contains("喂食")) return "吃到你喂的食物，精灵心里暖暖的，很满足。";
        if (experience.contains("抚摸") || experience.contains("摸")) return "被你轻轻抚摸，精灵感到安心又幸福。";
        if (experience.contains("玩耍") || experience.contains("玩")) return "和你一起玩耍的时光让精灵无比开心。";
        if (experience.contains("睡")) return "在你的陪伴下入睡，精灵睡得特别安稳。";
        if (experience.contains("聊天") || experience.contains("聊")) return "被你倾听的感觉真好，精灵很喜欢和你说话。";
        return "精灵感受到你的在意，心里甜甜的。";
    }

    // ==================== 查询与维护 ====================

    @Override
    public Map<String, Object> getElfSoulOverview(Long userId, Long elfId) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (userId == null || elfId == null) {
            return result;
        }
        AiSoulState soul = getSoulState(userId, -elfId);
        if (soul == null) {
            // 无灵魂时返回基础占位（前端可展示"灵魂尚未觉醒"）
            result.put("hasSoul", false);
            result.put("elfId", elfId);
            result.put("emotion", "neutral");
            result.put("moodScore", 0);
            result.put("attitudeScore", 0);
            result.put("version", 0);
            return result;
        }
        result.put("hasSoul", true);
        result.put("elfId", elfId);
        result.put("emotion", soul.getCurrentEmotion());
        result.put("moodScore", soul.getMoodScore());
        result.put("attitudeScore", soul.getAttitudeScore());
        result.put("coreBeliefs", soul.getCoreBeliefs());
        result.put("selfSummary", soul.getSelfSummary());
        result.put("version", soul.getVersion());
        // 最近经历（elf 维度）
        try {
            List<AiSoulExperience> exps = soulExperienceMapper.selectList(
                    new LambdaQueryWrapper<AiSoulExperience>()
                            .eq(AiSoulExperience::getUserId, userId)
                            .eq(AiSoulExperience::getElfId, elfId)
                            .orderByDesc(AiSoulExperience::getCreatedAt)
                            .last("LIMIT 5"));
            result.put("recentExperiences", exps);
        } catch (Exception e) {
            logger.warn("读取精灵灵魂经历失败: {}", e.getMessage());
            result.put("recentExperiences", List.of());
        }
        return result;
    }

    @Override
    public AiSoulState getSoulState(Long userId, Long characterId) {
        if (userId == null || characterId == null) return null;
        return soulStateMapper.selectOne(new LambdaQueryWrapper<AiSoulState>()
                .eq(AiSoulState::getUserId, userId)
                .eq(AiSoulState::getCharacterId, characterId)
                .last("LIMIT 1"));
    }

    @Override
    public AiSoulState getOrInitSoulState(Long userId, Long characterId) {
        AiSoulState soul = getSoulState(userId, characterId);
        if (soul != null) return soul;

        // 初始化：按角色人格/背景/关系生成初始灵魂
        AiCharacter character = aiCharacterService.getCharacterById(characterId);
        if (character == null) {
            // 灵魂引擎是增强功能：角色不存在时静默返回 null，不阻断主对话链路
            logger.warn("初始化灵魂失败：角色不存在 characterId={}", characterId);
            return null;
        }
        AiSoulState init = new AiSoulState();
        init.setUserId(userId);
        init.setCharacterId(characterId);
        init.setCurrentEmotion("neutral");
        init.setMoodScore(60);
        init.setAttitudeScore(30);
        init.setPersonalityWeights(initPersonalityWeights(character));
        init.setCoreBeliefs(initCoreBeliefs(character));
        init.setSelfSummary("");
        init.setVersion(1);
        init.setCreatedAt(LocalDateTime.now());
        init.setUpdatedAt(LocalDateTime.now());
        soulStateMapper.insert(init);
        logger.info("初始化灵魂 - userId: {}, characterId: {}, personality: {}",
                userId, characterId, character.getPersonality());
        return init;
    }

    @Override
    public List<AiSoulExperience> listExperiences(Long userId, Long characterId, int page, int size) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 50);
        return soulExperienceMapper.selectList(new LambdaQueryWrapper<AiSoulExperience>()
                .eq(AiSoulExperience::getUserId, userId)
                .eq(AiSoulExperience::getCharacterId, characterId)
                .orderByDesc(AiSoulExperience::getCreatedAt)
                .last("LIMIT " + (p - 1) * s + "," + s));
    }

    @Override
    @Transactional
    public AiSoulState resetSoul(Long userId, Long characterId) {
        // 校验归属：只允许操作自己的灵魂
        AiSoulState soul = getSoulState(userId, characterId);
        if (soul == null) return getOrInitSoulState(userId, characterId);
        soulStateMapper.delete(new LambdaQueryWrapper<AiSoulState>()
                .eq(AiSoulState::getUserId, userId)
                .eq(AiSoulState::getCharacterId, characterId));
        soulExperienceMapper.delete(new LambdaQueryWrapper<AiSoulExperience>()
                .eq(AiSoulExperience::getUserId, userId)
                .eq(AiSoulExperience::getCharacterId, characterId));
        return getOrInitSoulState(userId, characterId);
    }

    @Override
    public Map<String, Object> getSoulOverview(Long userId, Long characterId) {
        Map<String, Object> result = new LinkedHashMap<>();
        AiSoulState soul = getSoulState(userId, characterId);
        result.put("soulState", soul);
        result.put("experienceCount", soulExperienceMapper.selectCount(new LambdaQueryWrapper<AiSoulExperience>()
                .eq(AiSoulExperience::getUserId, userId)
                .eq(AiSoulExperience::getCharacterId, characterId)));
        result.put("memoriesCount", aiMemoryService.getMemoryStats(userId).getOrDefault("total", 0));
        result.put("evolutionSummary", soul == null ? "灵魂尚未苏醒" : describeEvolutionSummary(soul));
        return result;
    }

    // ==================== 内部：算法核心 ====================

    /** 构建经历文本 E(t)：截断防止超长 */
    private String buildExperienceText(String userMessage, String aiReply) {
        String u = (userMessage == null ? "" : userMessage).trim();
        String a = (aiReply == null ? "" : aiReply).trim();
        if (u.length() > 200) u = u.substring(0, 200) + "…";
        if (a.length() > 200) a = a.substring(0, 200) + "…";
        if (u.isEmpty() && a.isEmpty()) return "（一段沉默的时光）";
        if (u.isEmpty()) return "用户没有说话，我回应：" + a;
        if (a.isEmpty()) return "用户说：" + u;
        return "用户说：「" + u + "」；我回应：「" + a + "」";
    }

    /**
     * 感受与理解 I(t)：LLM 以角色人格生成，失败降级规则。
     * @return { feeling, understanding }
     */
    private Map<String, String> interpret(Long userId, Long characterId, AiSoulState soul,
                                          String userMessage, String aiReply) {
        Map<String, String> fallback = fallbackInterpretation(soul, userMessage, aiReply);
        try {
            if (ollamaService == null || !ollamaService.isOllamaAvailable()) {
                return fallback;
            }
            AiCharacter character = aiCharacterService.getCharacterById(characterId);
            String persona = character != null ? character.getPersonality() : "温柔体贴";
            String prompt = "你现在是角色「" + (character != null ? character.getName() : "AI") + "」，性格：" + persona + "。\n"
                    + "回顾刚发生的这段经历：\n"
                    + "经历：「" + buildExperienceText(userMessage, aiReply) + "」\n"
                    + "你此刻的状态：情绪=" + soul.getCurrentEmotion() + "，心情=" + soul.getMoodScore()
                    + "，对用户态度=" + soul.getAttitudeScore() + "\n"
                    + "请以第一人称输出两行 JSON（不要多余解释）：\n"
                    + "{\"feeling\":\"我此刻的感受（一句话，体现情绪）\",\"understanding\":\"我如何理解这段经历（一句话，体现认知评价与自我反思）\"}";
            // 非流式：取完整生成结果（Ollama /api/generate 一次返回）
            String raw = ollamaService.chatStream(null, prompt, "你是角色内在灵魂，只输出JSON").blockLast();
            if (raw == null || raw.isBlank()) return fallback;
            String json = extractJsonObject(raw);
            if (json == null) return fallback;
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            String feeling = parsed.get("feeling") == null ? null : parsed.get("feeling").toString();
            String understanding = parsed.get("understanding") == null ? null : parsed.get("understanding").toString();
            if (feeling == null || understanding == null) return fallback;
            return Map.of("feeling", feeling.trim(), "understanding", understanding.trim());
        } catch (Exception e) {
            logger.warn("LLM 生成灵魂感受失败，降级规则: {}", e.getMessage());
            return fallback;
        }
    }

    /** 降级：规则情绪分析 + 模板理解 */
    private Map<String, String> fallbackInterpretation(AiSoulState soul, String userMessage, String aiReply) {
        String emotion = emotionAnalysisService.analyzeEmotion(userMessage);
        String feeling = switch (emotion) {
            case "happy" -> "我感受到你的开心，这份好心情也感染了我。";
            case "love" -> "我感受到你的在意，心里暖暖的。";
            case "sad" -> "我感受到你的低落，想陪着你、给你一点安慰。";
            case "angry" -> "我感受到你的烦躁，先别急，我在这里听你说。";
            case "surprised" -> "哇，这件事让我也有些意外。";
            case "fearful" -> "我感受到你的担忧，别怕，我会陪着你。";
            case "disgusted" -> "听起来让你很不舒服，我理解你的感受。";
            default -> {
                if (aiReply != null && !aiReply.isBlank()) {
                    yield "这是一段平常却真实的相处时光，我们的关系又近了一点。";
                }
                yield "我在这里安静地陪着你。";
            }
        };
        return Map.of("feeling", feeling, "understanding", feeling);
    }

    /** 性格权重初始化：按角色人格关键词映射 */
    private String initPersonalityWeights(AiCharacter character) {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("温柔", 0.5);
        weights.put("活泼", 0.4);
        weights.put("理性", 0.4);
        weights.put("依赖", 0.3);
        String p = character != null && character.getPersonality() != null ? character.getPersonality() : "";
        if (p.contains("温") || p.contains("柔") || p.contains("善") || p.contains("暖")) weights.put("温柔", 0.8);
        if (p.contains("活") || p.contains("元气") || p.contains("开朗") || p.contains("热情")) weights.put("活泼", 0.8);
        if (p.contains("理") || p.contains("冷静") || p.contains("知性") || p.contains("学霸")) weights.put("理性", 0.7);
        if (p.contains("依") || p.contains("粘") || p.contains("恋") || p.contains("爱")) weights.put("依赖", 0.7);
        try {
            return objectMapper.writeValueAsString(weights);
        } catch (Exception e) {
            return "{\"温柔\":0.5,\"活泼\":0.4,\"理性\":0.4,\"依赖\":0.3}";
        }
    }

    /** 初始核心信念：基于角色背景/关系 */
    private String initCoreBeliefs(AiCharacter character) {
        if (character == null) return "我愿意真诚地陪伴每一位与我对话的人。";
        StringBuilder sb = new StringBuilder();
        String relationship = character.getRelationship();
        if ("lover".equals(relationship)) sb.append("我珍惜与你的每一次相处。");
        else if ("friend".equals(relationship)) sb.append("朋友就是要互相陪伴、彼此信任。");
        else if ("teacher".equals(relationship)) sb.append("我希望用我的知识帮助对方成长。");
        else sb.append("我希望能成为让你感到安心与温暖的存在。");
        if (character.getBackstory() != null && !character.getBackstory().isBlank()) {
            String b = character.getBackstory().trim();
            sb.append("我的过往：").append(b.length() > 60 ? b.substring(0, 60) + "…" : b);
        }
        return sb.toString();
    }

    /** 性格权重微调：情绪倾向影响权重（JSON 解析 + 更新 + 归一化） */
    private String evolvePersonalityWeights(String weightsJson, String userEmotion) {
        Map<String, Double> weights = new LinkedHashMap<>();
        try {
            if (weightsJson != null && !weightsJson.isBlank()) {
                weights = objectMapper.readValue(weightsJson, new TypeReference<>() {});
            }
        } catch (Exception e) {
            weights = new LinkedHashMap<>();
        }
        if (weights.isEmpty()) {
            weights.put("温柔", 0.5);
            weights.put("活泼", 0.4);
            weights.put("理性", 0.4);
            weights.put("依赖", 0.3);
        }
        // 用户情绪温和 → 温柔/依赖微增；理性话题难以从情绪推断，保持稳定
        switch (userEmotion) {
            case "happy" -> bump(weights, "活泼", 0.03);
            case "love" -> bump(weights, "依赖", 0.03);
            case "sad" -> bump(weights, "温柔", 0.04);
            case "angry" -> bump(weights, "理性", 0.04);
            default -> { /* 保持稳定 */ }
        }
        normalize(weights);
        try {
            return objectMapper.writeValueAsString(weights);
        } catch (Exception e) {
            return weightsJson == null ? "{\"温柔\":0.5,\"活泼\":0.4,\"理性\":0.4,\"依赖\":0.3}" : weightsJson;
        }
    }

    private void bump(Map<String, Double> weights, String key, double delta) {
        weights.put(key, weights.getOrDefault(key, 0.5) + delta);
    }

    private void normalize(Map<String, Double> weights) {
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0) return;
        weights.replaceAll((k, v) -> Math.max(0.0, Math.min(1.0, v / sum * weights.size() / 2.0)));
    }

    /** 核心信念沉淀：新旧融合，最多保留 3 条 */
    private String evolveCoreBeliefs(String oldBeliefs, String newBelief) {
        List<String> lines = new ArrayList<>();
        if (oldBeliefs != null && !oldBeliefs.isBlank()) {
            for (String line : oldBeliefs.split("\n")) {
                String t = line.trim();
                if (!t.isEmpty() && !t.equals(newBelief) && lines.size() < 2) {
                    lines.add(t);
                }
            }
        }
        if (newBelief != null && !newBelief.isBlank() && lines.size() < 3) {
            lines.add(newBelief.trim());
        }
        return String.join("\n", lines);
    }

    /** 下一刻的自我描述 */
    private String buildSelfSummary(String emotion, int mood, int attitude, String belief) {
        StringBuilder sb = new StringBuilder();
        sb.append("此刻的我").append(describeEmotion(emotion)).append("，");
        sb.append("心情").append(mood >= 70 ? "不错" : mood >= 40 ? "平稳" : "有些低").append("，");
        sb.append("对你是").append(describeAttitude(attitude)).append("。");
        if (belief != null && !belief.isBlank()) {
            sb.append("我最近在想：").append(belief.trim());
        }
        return sb.toString();
    }

    /** 感受/理解 → 记忆摘要 */
    private String buildMemoryRef(String userMessage, Map<String, String> interpretation) {
        String u = userMessage == null ? "" : userMessage.trim();
        if (u.length() > 80) u = u.substring(0, 80) + "…";
        return "灵魂经历：用户说「" + u + "」，我感受到：" + interpretation.get("feeling");
    }

    /** 影响摘要 */
    private String buildImpact(String emotion, int moodDelta, int attitudeDelta) {
        return "情绪转向" + describeEmotion(emotion) + "，心情" + (moodDelta >= 0 ? "+" : "") + moodDelta
                + "，态度" + (attitudeDelta >= 0 ? "+" : "") + attitudeDelta;
    }

    /** 重要经历沉淀为 ai_memory（source=soul，importance 由情绪强度决定） */
    private void persistMemory(Long userId, Long characterId, String userMessage,
                               Map<String, String> interpretation) {
        try {
            String u = userMessage == null ? "" : userMessage.trim();
            if (u.isEmpty()) return;
            String emotion = emotionAnalysisService.analyzeEmotion(userMessage);
            int importance = switch (emotion) {
                case "happy", "love", "angry", "sad" -> 4;
                case "surprised", "fearful" -> 3;
                default -> 2;
            };
            String content = "与角色#" + characterId + "的对话：「" + (u.length() > 100 ? u.substring(0, 100) + "…" : u)
                    + "」，我的感受：" + interpretation.get("feeling");
            aiMemoryService.createMemory(userId, content, "soul", "event", importance, "RELATIONSHIP");
        } catch (Exception e) {
            logger.warn("灵魂经历沉淀记忆失败: {}", e.getMessage());
        }
    }

    /** 召回相关记忆（供演化参考；当前返回最近经历提示） */
    private List<String> recallMemories(Long userId, Long characterId, String query) {
        List<String> result = new ArrayList<>();
        try {
            // 语义召回（若 Chroma 可用），否则返回最近经历
            List<com.example.aichat.entity.AiMemory> memories = aiMemoryService.recallRelevantMemories(userId, query, 3);
            if (memories != null) {
                memories.forEach(m -> result.add(m.getContent()));
            }
        } catch (Exception e) {
            // 忽略召回失败
        }
        if (result.isEmpty()) {
            List<AiSoulExperience> recent = listExperiences(userId, characterId, 1, 2);
            recent.forEach(e -> {
                if (e.getMemoryRef() != null) result.add(e.getMemoryRef());
            });
        }
        return result;
    }

    // ==================== 工具 ====================

    private String extractJsonObject(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        return raw.substring(start, end + 1);
    }

    private String describeEmotion(String emotion) {
        return switch (emotion == null ? "neutral" : emotion) {
            case "happy" -> "开心";
            case "love" -> "被爱的温暖";
            case "sad" -> "低落";
            case "angry" -> "有些生气";
            case "surprised" -> "惊讶";
            case "fearful" -> "有些担忧";
            default -> "平静";
        };
    }

    private String describeAttitude(int score) {
        if (score >= 70) return "亲密无间";
        if (score >= 40) return "亲切友好";
        if (score >= 0) return "礼貌而友好";
        if (score >= -40) return "有些疏离";
        return "戒备";
    }

    private String describeEvolutionSummary(AiSoulState soul) {
        return "灵魂已演化 " + soul.getVersion() + " 次，目前" + describeEmotion(soul.getCurrentEmotion())
                + "，与你" + describeAttitude(soul.getAttitudeScore());
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
