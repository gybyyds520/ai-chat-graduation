package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.common.exception.AiErrorCode;
import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.entity.AiMemory;
import com.example.aichat.entity.AiMemorySetting;
import com.example.aichat.event.UserDataCaptureEvent;
import com.example.aichat.mapper.AiMemoryMapper;
import com.example.aichat.mapper.AiMemorySettingMapper;
import com.example.aichat.service.AiMemoryService;
import com.example.aichat.service.MemoryVectorClient;
import com.example.aichat.util.MemoryCategory;
import com.example.aichat.util.MemoryClassifier;
import com.example.aichat.util.MemoryScorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 记忆服务实现。
 *
 * 安全：所有读改写删除均以 userId 为归属维度，先 selectById 再比对 userId，
 * 非本人访问抛 MEMORY_FORBIDDEN(403)，不存在抛 MEMORY_NOT_FOUND(404)。
 */
@Service
public class AiMemoryServiceImpl implements AiMemoryService {

    private static final Logger logger = LoggerFactory.getLogger(AiMemoryServiceImpl.class);

    private final AiMemoryMapper aiMemoryMapper;

    private final AiMemorySettingMapper aiMemorySettingMapper;

    public AiMemoryServiceImpl(AiMemoryMapper aiMemoryMapper,
                               AiMemorySettingMapper aiMemorySettingMapper) {
        this.aiMemoryMapper = aiMemoryMapper;
        this.aiMemorySettingMapper = aiMemorySettingMapper;
    }

    /** 数据池自动采集事件发布器。 */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 记忆向量召回客户端（市场级升级 3）。仅当 aichat.memory.vector.enabled=true 时装配；
     * 单测/离线环境为 null，此时降级为本地关键词召回，不破坏 898 基线。
     */
    @Autowired(required = false)
    private MemoryVectorClient memoryVectorClient;

    /**
     * 校验记忆存在且归属当前用户。userId 由 requireUserId 保证非空。
     */
    private AiMemory requireOwnership(Long userId, Long memoryId) {
        AiMemory memory = aiMemoryMapper.selectById(memoryId);
        if (memory == null) {
            throw new BusinessException(AiErrorCode.MEMORY_NOT_FOUND);
        }
        if (!userId.equals(memory.getUserId())) {
            throw new BusinessException(AiErrorCode.MEMORY_FORBIDDEN);
        }
        return memory;
    }

    @Override
    public AiMemory createMemory(Long userId, String content, String source,
                                 String memoryType, Integer importance, String category) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(AiErrorCode.MEMORY_CONTENT_BLANK);
        }
        AiMemory memory = new AiMemory();
        memory.setUserId(userId);
        memory.setContent(content.trim());
        memory.setSource(source != null && !source.isBlank() ? source : "manual");
        memory.setMemoryType(memoryType != null && !memoryType.isBlank() ? memoryType : "other");
        int imp = importance != null ? importance : 3;
        if (imp < 1) imp = 1;
        if (imp > 5) imp = 5;
        memory.setImportance(imp);
        // 市场级升级 1：用户未显式指定分类时，按内容自动归类（FACT/PREFERENCE/EVENT/RELATIONSHIP）
        memory.setCategory((category != null && !category.isBlank())
                ? category
                : MemoryClassifier.classify(memory.getContent()).name());
        memory.setIsActive(Boolean.TRUE);
        memory.setReferencedCount(0);
        memory.setPinned(Boolean.FALSE);
        LocalDateTime now = LocalDateTime.now();
        memory.setCreatedAt(now);
        memory.setUpdatedAt(now);
        aiMemoryMapper.insert(memory);
        logger.info("[AI记忆] 用户 {} 创建记忆 id={} type={} category={}",
                userId, memory.getId(), memory.getMemoryType(), memory.getCategory());

        // 向量开关开启时旁路向量化入库（失败不阻断）
        if (memoryVectorClient != null) {
            try {
                memoryVectorClient.ingestMemory(userId, memory.getId(), memory.getContent());
            } catch (Exception e) {
                logger.warn("[AI记忆] 向量入库旁路异常(忽略): {}", e.getMessage());
            }
        }

        // 数据池自动采集：记忆内容摘要
        if (eventPublisher != null) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("memoryId", memory.getId());
            meta.put("memoryType", memory.getMemoryType());
            eventPublisher.publishEvent(new UserDataCaptureEvent(
                    userId, "ai_memory", memory.getContent(), meta,
                    "memory:" + memory.getId(), now));
        }
        return memory;
    }

    @Override
    @Transactional
    public AiMemory updateMemory(Long userId, Long memoryId, String content,
                                 String memoryType, Integer importance, String category, Boolean isActive) {
        AiMemory memory = requireOwnership(userId, memoryId);
        if (content != null) {
            if (content.isBlank()) {
                throw new BusinessException(AiErrorCode.MEMORY_CONTENT_BLANK);
            }
            memory.setContent(content.trim());
        }
        if (memoryType != null && !memoryType.isBlank()) {
            memory.setMemoryType(memoryType);
        }
        if (importance != null) {
            int imp = importance;
            if (imp < 1) imp = 1;
            if (imp > 5) imp = 5;
            memory.setImportance(imp);
        }
        if (category != null) {
            memory.setCategory(category);
        }
        if (isActive != null) {
            memory.setIsActive(isActive);
        }
        memory.setUpdatedAt(LocalDateTime.now());
        aiMemoryMapper.updateById(memory);
        return memory;
    }

    @Override
    public void deleteMemory(Long userId, Long memoryId) {
        requireOwnership(userId, memoryId);
        aiMemoryMapper.deleteById(memoryId);
        logger.info("[AI记忆] 用户 {} 删除记忆 id={}", userId, memoryId);
    }

    @Override
    public AiMemory getMemory(Long userId, Long memoryId) {
        return requireOwnership(userId, memoryId);
    }

    @Override
    public IPage<AiMemory> listMemories(Long userId, int page, int size,
                                        String memoryType, String category, Boolean isActive,
                                        String keyword, String source) {
        int p = page < 1 ? 1 : page;
        int s = size < 1 ? 10 : Math.min(size, 100);
        Page<AiMemory> pageParam = new Page<>(p, s);
        QueryWrapper<AiMemory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (memoryType != null && !memoryType.isBlank()) {
            wrapper.eq("memory_type", memoryType);
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq("category", category);
        }
        if (source != null && !source.isBlank()) {
            wrapper.eq("source", source);
        }
        if (isActive != null) {
            wrapper.eq("is_active", isActive);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("content", keyword.trim());
        }
        wrapper.orderByDesc("is_active").orderByDesc("created_at");
        return aiMemoryMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Map<String, Object> getMemoryStats(Long userId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        Long total = aiMemoryMapper.selectCount(
                new QueryWrapper<AiMemory>().eq("user_id", userId));
        stats.put("total", total == null ? 0 : total);

        // 各类型数量
        List<Map<String, Object>> typeRows = aiMemoryMapper.selectMaps(
                new QueryWrapper<AiMemory>()
                        .select("memory_type AS memoryType, COUNT(*) AS cnt")
                        .eq("user_id", userId)
                        .groupBy("memory_type"));
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        if (typeRows != null) {
            for (Map<String, Object> row : typeRows) {
                Object t = row.get("memoryType");
                Object c = row.get("cnt");
                typeCounts.put(t == null ? "unknown" : t.toString(), ((Number) c).longValue());
            }
        }
        stats.put("typeCounts", typeCounts);

        // 重要性分布
        List<Map<String, Object>> impRows = aiMemoryMapper.selectMaps(
                new QueryWrapper<AiMemory>()
                        .select("importance AS importance, COUNT(*) AS cnt")
                        .eq("user_id", userId)
                        .groupBy("importance"));
        Map<String, Long> importanceDist = new LinkedHashMap<>();
        if (impRows != null) {
            for (Map<String, Object> row : impRows) {
                Object k = row.get("importance");
                Object c = row.get("cnt");
                importanceDist.put(k == null ? "0" : k.toString(), ((Number) c).longValue());
            }
        }
        stats.put("importanceDistribution", importanceDist);
        return stats;
    }

    @Override
    @Transactional
    public AiMemory toggleMemory(Long userId, Long memoryId) {
        AiMemory memory = requireOwnership(userId, memoryId);
        boolean current = Boolean.TRUE.equals(memory.getIsActive());
        memory.setIsActive(!current);
        memory.setUpdatedAt(LocalDateTime.now());
        aiMemoryMapper.updateById(memory);
        return memory;
    }

    @Override
    public List<AiMemory> getActiveMemoriesForContext(Long userId, int limit) {
        // 全局开关关闭时不注入任何长期记忆（用户可一键关闭跨会话记忆）。
        // aiMemorySettingMapper 为 null（单测/未装配）时视为开启，保持兼容。
        if (!isMemoryEnabled(userId)) {
            return new ArrayList<>();
        }
        int lim = limit <= 0 ? 10 : Math.min(limit, 50);
        QueryWrapper<AiMemory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_active", true)
                .orderByDesc("importance")
                .orderByDesc("last_used_at")
                .orderByDesc("created_at")
                .last("LIMIT " + lim);
        List<AiMemory> list = aiMemoryMapper.selectList(wrapper);
        return list != null ? list : new ArrayList<>();
    }

    /** 读取用户记忆全局开关；无记录默认 true。 */
    private boolean isMemoryEnabled(Long userId) {
        AiMemorySetting setting = aiMemorySettingMapper.selectById(userId);
        return setting == null || !Boolean.FALSE.equals(setting.getEnabled());
    }

    @Override
    public void markMemoryUsed(Long userId, List<Long> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long id : memoryIds) {
            if (id == null) continue;
            AiMemory memory = aiMemoryMapper.selectById(id);
            if (memory != null && userId.equals(memory.getUserId())) {
                memory.setLastUsedAt(now);
                // 市场级升级 2：引用次数 +1（>5 触发常引用加权）
                int ref = memory.getReferencedCount() == null ? 0 : memory.getReferencedCount();
                memory.setReferencedCount(ref + 1);
                aiMemoryMapper.updateById(memory);
            }
        }
    }

    // ---------- 自动提炼（source=chat） ----------

    /** 名称：我叫X / 我的名字是X */
    private static final Pattern P_NAME = Pattern.compile("(?:我叫|我的名字是)([^，。！？\\s、]{1,12})");
    /** 喜好：我喜欢X */
    private static final Pattern P_LIKE = Pattern.compile("我喜欢([^，。！？\\s、]{1,20})");
    /** 热爱：我爱X（避免与"我爱你"等极短情感句混淆，要求长度≥2） */
    private static final Pattern P_LOVE = Pattern.compile("我爱([^，。！？\\s、]{2,20})");
    /** 厌恶：我讨厌X / 我不喜欢X */
    private static final Pattern P_DISLIKE = Pattern.compile("(?:我讨厌|我不喜欢)([^，。！？\\s、]{1,20})");
    /** 居住：我住在X / 我住于X */
    private static final Pattern P_LIVE = Pattern.compile("我住(?:在|于)([^，。！？\\s、]{1,20})");
    /** 来源：我来自X */
    private static final Pattern P_FROM = Pattern.compile("我来自([^，。！？\\s、]{1,20})");
    /** 年龄：我今年X岁 / 我X岁 */
    private static final Pattern P_AGE = Pattern.compile("我(?:今年)?([一二三四五六七八九十百零\\d]{1,3})岁");
    /** 生日：我的生日是X / 生日X */
    private static final Pattern P_BIRTHDAY = Pattern.compile("(?:我的)?生日(?:是|在)?(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}日?)");
    /** 职业：我在X工作 / 我在X上班 */
    private static final Pattern P_JOB = Pattern.compile("我在([^，。！？\\s、]{1,20})(?:工作|上班)");
    /** 宠物：我养了X / 我养过X */
    private static final Pattern P_PET = Pattern.compile("我养(?:了|过)?(?:一只|一条|一个)?([^，。！？\\s、]{1,12})");

    @Override
    public int extractMemoriesFromMessage(Long userId, String message) {
        if (message == null || message.isBlank()) {
            return 0;
        }
        String msg = message.trim();
        if (msg.length() > 200) {
            msg = msg.substring(0, 200);
        }

        int added = 0;
        added += extractOnce(userId, msg, P_NAME, "我叫%s", "fact", 4) ? 1 : 0;
        added += extractOnce(userId, msg, P_LIKE, "我喜欢%s", "preference", 3) ? 1 : 0;
        added += extractOnce(userId, msg, P_LOVE, "我爱%s", "preference", 3) ? 1 : 0;
        added += extractOnce(userId, msg, P_DISLIKE, "我不喜欢%s", "preference", 3) ? 1 : 0;
        added += extractOnce(userId, msg, P_LIVE, "我住在%s", "fact", 4) ? 1 : 0;
        added += extractOnce(userId, msg, P_FROM, "我来自%s", "fact", 3) ? 1 : 0;
        added += extractOnce(userId, msg, P_AGE, "我今年%s岁", "fact", 4) ? 1 : 0;
        added += extractOnce(userId, msg, P_BIRTHDAY, "我的生日是%s", "event", 4) ? 1 : 0;
        added += extractOnce(userId, msg, P_JOB, "我在%s工作", "fact", 3) ? 1 : 0;
        added += extractOnce(userId, msg, P_PET, "我养了%s", "fact", 3) ? 1 : 0;
        return added;
    }

    private boolean extractOnce(Long userId, String message, Pattern pattern,
                                String template, String type, int importance) {
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return false;
        }
        String value = matcher.group(1) != null ? matcher.group(1).trim() : "";
        if (value.isEmpty()) {
            return false;
        }
        String content = String.format(template, value);
        if (existsSimilar(userId, content)) {
            logger.info("[AI记忆] 自动提炼命中重复内容，跳过: {}", content);
            return false;
        }
        createMemory(userId, content, "chat", type, importance, "自动提炼");
        logger.info("[AI记忆] 自动提炼: userId={} content={} type={}", userId, content, type);
        return true;
    }

    /**
     * 去重：同用户存在 content 完全相同，或内容前 12 字模糊命中（近似相似）时视为重复。
     */
    private boolean existsSimilar(Long userId, String content) {
        QueryWrapper<AiMemory> exact = new QueryWrapper<>();
        exact.eq("user_id", userId).eq("content", content);
        if (aiMemoryMapper.selectCount(exact) > 0) {
            return true;
        }
        if (content.length() >= 8) {
            String probe = content.substring(0, Math.min(12, content.length()));
            QueryWrapper<AiMemory> fuzzy = new QueryWrapper<>();
            fuzzy.eq("user_id", userId).like("content", probe);
            if (aiMemoryMapper.selectCount(fuzzy) > 0) {
                return true;
            }
        }
        return false;
    }

    // ---------- 全局开关 / 钉选（M1 / A3） ----------

    @Override
    public Map<String, Object> getMemorySetting(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", isMemoryEnabled(userId));
        return result;
    }

    @Override
    public void updateMemorySetting(Long userId, boolean enabled) {
        AiMemorySetting setting = aiMemorySettingMapper.selectById(userId);
        if (setting == null) {
            setting = new AiMemorySetting();
            setting.setUserId(userId);
            setting.setEnabled(enabled);
            setting.setUpdatedAt(LocalDateTime.now());
            aiMemorySettingMapper.insert(setting);
        } else {
            setting.setEnabled(enabled);
            setting.setUpdatedAt(LocalDateTime.now());
            aiMemorySettingMapper.updateById(setting);
        }
        logger.info("[AI记忆] 用户 {} 全局开关置为 {}", userId, enabled);
    }

    @Override
    public AiMemory pinMessageToMemory(Long userId, String content, String source) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(AiErrorCode.MEMORY_CONTENT_BLANK);
        }
        String src = (source == null || source.isBlank()) ? "pinned" : source;
        // 钉选消息为高重要性长期记忆（importance=5，source=pinned），不去重（用户主动钉选）
        AiMemory pinned = createMemory(userId, content.trim(), src, "event", 5, "钉选记忆");
        // 市场级升级 2：显式钉选，不参与时间衰减
        pinned.setPinned(Boolean.TRUE);
        AiMemory patch = new AiMemory();
        patch.setId(pinned.getId());
        patch.setPinned(Boolean.TRUE);
        aiMemoryMapper.updateById(patch);
        return pinned;
    }

    // ================= 市场级升级：批量删除 / 导出 / 语义召回 / 主动引用 =================

    @Override
    @Transactional
    public int batchDeleteMemories(Long userId, List<Long> memoryIds) {
        if (memoryIds == null || memoryIds.isEmpty()) {
            return 0;
        }
        int deleted = 0;
        for (Long id : memoryIds) {
            if (id == null) continue;
            try {
                requireOwnership(userId, id);
                aiMemoryMapper.deleteById(id);
                deleted++;
            } catch (BusinessException e) {
                // 非本人 / 不存在的 id 跳过，不中断整批
                logger.warn("[AI记忆] 批量删除跳过 id={}: {}", id, e.getMessage());
            }
        }
        logger.info("[AI记忆] 用户 {} 批量删除记忆 {} 条", userId, deleted);
        return deleted;
    }

    @Override
    public List<AiMemory> exportAllMemories(Long userId) {
        QueryWrapper<AiMemory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("is_active", true);
        List<AiMemory> list = aiMemoryMapper.selectList(wrapper);
        if (list == null) {
            return new ArrayList<>();
        }
        // 按综合评分（重要度*衰减）降序导出
        LocalDateTime now = LocalDateTime.now();
        return list.stream()
                .sorted(Comparator.comparingDouble(
                        (AiMemory m) -> MemoryScorer.calculateMemoryScore(m, now)).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<AiMemory> recallRelevantMemories(Long userId, String query, int topK) {
        if (!isMemoryEnabled(userId) || query == null || query.isBlank()) {
            return new ArrayList<>();
        }
        int k = topK <= 0 ? 3 : Math.min(topK, 10);
        // 取该用户全部生效记忆作为候选池（小数据量，内存内融合排序）
        QueryWrapper<AiMemory> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("is_active", true);
        List<AiMemory> candidates = aiMemoryMapper.selectList(wrapper);
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }
        // 拷贝为可变列表（selectList 可能返回不可变集合，后续要原地排序）
        candidates = new ArrayList<>(candidates);

        LocalDateTime now = LocalDateTime.now();
        boolean vectorUsed = false;
        Map<Long, Double> vectorScores = new HashMap<>();
        // 向量开关开启且 sidecar 可用：用语义相似度
        if (memoryVectorClient != null) {
            List<MemoryVectorClient.MemoryHit> hits =
                    memoryVectorClient.queryMemories(userId, query, k);
            if (hits != null && !hits.isEmpty()) {
                for (MemoryVectorClient.MemoryHit h : hits) {
                    try {
                        vectorScores.put(Long.valueOf(h.memoryId), h.score);
                    } catch (NumberFormatException ignore) {
                    }
                }
                vectorUsed = true;
            }
        }

        final boolean useVector = vectorUsed;
        candidates.sort((a, b) -> {
            double sa = scoreOf(a, useVector, vectorScores, query, now);
            double sb = scoreOf(b, useVector, vectorScores, query, now);
            int cmp = Double.compare(sb, sa);
            return cmp != 0 ? cmp : Long.compare(a.getId(), b.getId());
        });
        return candidates.stream().limit(k).collect(Collectors.toList());
    }

    /** 单条记忆融合分：向量开关命中用向量分，否则用关键词相似度。 */
    private double scoreOf(AiMemory m, boolean useVector, Map<Long, Double> vectorScores,
                          String query, LocalDateTime now) {
        double vs = useVector && vectorScores.containsKey(m.getId())
                ? vectorScores.get(m.getId())
                : keywordSimilarity(query, m.getContent());
        return MemoryScorer.fuseMemoryRanking(vs, m, now);
    }

    /** 降级关键词相似度：query 与 content 的字符 bigram 重叠率（0~1）。 */
    private double keywordSimilarity(String query, String content) {
        if (query == null || content == null) return 0.0;
        Set<String> q = bigrams(query);
        Set<String> c = bigrams(content);
        if (q.isEmpty() || c.isEmpty()) return 0.0;
        int overlap = 0;
        for (String g : c) {
            if (q.contains(g)) overlap++;
        }
        return (double) overlap / c.size();
    }

    private static Set<String> bigrams(String text) {
        Set<String> set = new HashSet<>();
        if (text == null) return set;
        String s = text.trim();
        for (int i = 0; i < s.length() - 1; i++) {
            set.add(s.substring(i, i + 2));
        }
        return set;
    }

    @Override
    public String buildActiveReferenceContext(Long userId, String query, int topK) {
        List<AiMemory> related = recallRelevantMemories(userId, query, topK);
        if (related.isEmpty()) {
            return "";
        }
        // 更新引用：lastUsedAt + referencedCount
        List<Long> ids = related.stream().map(AiMemory::getId).collect(Collectors.toList());
        markMemoryUsed(userId, ids);
        // 注入上下文："用户记得：{memory1}, {memory2}"
        String joined = related.stream()
                .map(AiMemory::getContent)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.joining("；"));
        if (joined.isEmpty()) {
            return "";
        }
        return "用户记得：" + joined;
    }
}
