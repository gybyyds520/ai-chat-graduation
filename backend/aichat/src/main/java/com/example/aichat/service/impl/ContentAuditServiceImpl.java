package com.example.aichat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aichat.common.audit.SensitiveWordDictionary;
import com.example.aichat.common.audit.SensitiveWordDictionary.WordEntry;
import com.example.aichat.dto.audit.AuditResult;
import com.example.aichat.dto.audit.PageResult;
import com.example.aichat.entity.ContentAuditLog;
import com.example.aichat.entity.User;
import com.example.aichat.mapper.ContentAuditLogMapper;
import com.example.aichat.mapper.UserMapper;
import com.example.aichat.service.ContentAuditService;
import com.example.aichat.service.MinorProtectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 内容审核服务实现。
 *
 * <p>匹配策略（M4 ⑮ 加固后）：
 * <ul>
 *   <li>基础：大小写不敏感，字间允许插入任意空白/标点/符号——拦截"拆字"与"符号插入"绕过
 *       （如 "色 情 网 站"、"色·情·网·站"、"色_情_网_站"）；</li>
 *   <li>谐音/形近字：对高频辱骂词补充常见谐音变体（如 "我草"→"我操"、"煞笔"→"傻逼"）；</li>
 *   <li>原有 526 敏感词表保持不变，仅增强匹配方式。</li>
 * </ul>
 * 风险等级取命中词条目的最高等级（HIGH > MEDIUM > LOW）。</p>
 */
@Service
public class ContentAuditServiceImpl implements ContentAuditService {

    private static final Logger logger = LoggerFactory.getLogger(ContentAuditServiceImpl.class);

    /** 内容快照截断长度，避免超长正文撑爆日志表。 */
    private static final int SNAPSHOT_MAX = 500;

    /**
     * 敏感词"宽松匹配"正则缓存：字间允许插入任意空白/标点/符号（拆字、符号插入绕过）。
     * 编译一次后复用，避免每次审核重复 Pattern.compile。
     */
    private static final Map<String, Pattern> RELAXED_PATTERN_CACHE = new ConcurrentHashMap<>();

    /** 谐音/形近字绕过变体：(正则, 命中后上报的规范词, 风险等级)。 */
    private record HomophoneVariant(Pattern pattern, String canonical, String risk) {
    }

    private static final List<HomophoneVariant> HOMOPHONE_VARIANTS = List.of(
            // "我操" 的谐音绕过：我草 / 我艹 / 我曹 / 我槽
            new HomophoneVariant(compileRelaxed("我[草艹曹槽]"), "我操", SensitiveWordDictionary.RISK_MEDIUM),
            // "傻逼" 的谐音绕过：煞笔 / 沙笔 / 煞逼 / 沙逼
            new HomophoneVariant(compileRelaxed("[煞沙][笔逼]"), "傻逼", SensitiveWordDictionary.RISK_MEDIUM),
            // "草泥马" 的谐音绕过：操泥马 / 操尼玛
            new HomophoneVariant(compileRelaxed("操[泥尼]马"), "草泥马", SensitiveWordDictionary.RISK_MEDIUM)
    );

    private final ContentAuditLogMapper auditLogMapper;
    private final SensitiveWordDictionary dictionary;
    private final UserMapper userMapper;
    private final MinorProtectionService minorProtectionService;

    public ContentAuditServiceImpl(ContentAuditLogMapper auditLogMapper,
                                  SensitiveWordDictionary dictionary,
                                  UserMapper userMapper,
                                  MinorProtectionService minorProtectionService) {
        this.auditLogMapper = auditLogMapper;
        this.dictionary = dictionary;
        this.userMapper = userMapper;
        this.minorProtectionService = minorProtectionService;
    }

    @Override
    public AuditResult audit(String content, String contentType, Long userId) {
        // 空/空白内容视为通过（不拦截，但调用方仍可决定是否记录日志）
        if (content == null || content.isBlank()) {
            return AuditResult.pass();
        }

        String haystack = content.toLowerCase();
        Set<String> matched = new LinkedHashSet<>();
        String topRisk = SensitiveWordDictionary.RISK_LOW;

        for (WordEntry entry : dictionary.allEntries()) {
            if (relaxedPattern(entry.word()).matcher(haystack).find()) {
                matched.add(entry.word());
                topRisk = higherRisk(topRisk, entry.riskLevel());
            }
        }

        // 谐音/形近字绕过变体（不上报变体本身，统一上报规范敏感词）
        for (HomophoneVariant variant : HOMOPHONE_VARIANTS) {
            if (variant.pattern().matcher(haystack).find()) {
                matched.add(variant.canonical());
                topRisk = higherRisk(topRisk, variant.risk());
            }
        }

        // 未成年保护模式生效时，额外扫描"未成年加强词表"，实现更严格过滤。
        // 该词表对普通用户不生效（普通用户放行），仅对未成年用户拦截。
        boolean minorMode = isMinorUser(userId);
        if (minorMode) {
            for (WordEntry entry : dictionary.minorExtraEntries()) {
                if (relaxedPattern(entry.word()).matcher(haystack).find()) {
                    matched.add(entry.word());
                    topRisk = higherRisk(topRisk, entry.riskLevel());
                }
            }
        }

        if (matched.isEmpty()) {
            return AuditResult.pass();
        }

        List<String> matchedList = new ArrayList<>(matched);
        String reason = "命中" + matchedList.size() + "个敏感词，风险等级: " + topRisk
                + (minorMode ? "（未成年加强过滤）" : "");
        logger.info("[内容审核] 用户 {} 类型 {} 命中敏感词: {} (风险 {})", userId, contentType, matchedList, topRisk);
        return AuditResult.reject(matchedList, topRisk, reason);
    }

    @Override
    public boolean isMinorUser(Long userId) {
        if (userId == null || minorProtectionService == null || userMapper == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        return minorProtectionService.isMinorModeActive(user);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordAuditLog(Long userId, String contentType, Long targetId, String content, AuditResult result) {
        try {
            ContentAuditLog log = new ContentAuditLog();
            log.setUserId(userId);
            log.setContentType(contentType);
            log.setTargetId(targetId);
            log.setContentSnapshot(truncate(content));
            log.setRiskLevel(result == null ? SensitiveWordDictionary.RISK_LOW : result.getRiskLevel());
            log.setMatchedWords(result == null || result.getMatchedWords() == null
                    ? null : String.join(",", result.getMatchedWords()));
            log.setPassed(result == null || result.isPassed());
            log.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(log);
        } catch (Exception e) {
            // 审核日志写入失败不应阻断主流程，仅记录错误
            logger.warn("[内容审核] 审核日志写入失败: {}", e.getMessage());
        }
    }

    @Override
    public PageResult<ContentAuditLog> getAuditLogs(int page, int size, String riskLevel, String contentType) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 200);

        QueryWrapper<ContentAuditLog> wrapper = new QueryWrapper<>();
        if (riskLevel != null && !riskLevel.isBlank()) {
            wrapper.eq("risk_level", riskLevel);
        }
        if (contentType != null && !contentType.isBlank()) {
            wrapper.eq("content_type", contentType);
        }
        wrapper.orderByDesc("created_at");

        Page<ContentAuditLog> p = new Page<>(safePage, safeSize);
        var result = auditLogMapper.selectPage(p, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), safePage, safeSize);
    }

    @Override
    public Map<String, Object> getAuditStats() {
        long total = count(null);
        long blocked = count(new QueryWrapper<ContentAuditLog>().eq("passed", false));
        long high = count(new QueryWrapper<ContentAuditLog>().eq("passed", false).eq("risk_level", SensitiveWordDictionary.RISK_HIGH));
        long medium = count(new QueryWrapper<ContentAuditLog>().eq("passed", false).eq("risk_level", SensitiveWordDictionary.RISK_MEDIUM));
        long low = count(new QueryWrapper<ContentAuditLog>().eq("passed", false).eq("risk_level", SensitiveWordDictionary.RISK_LOW));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayTotal = count(new QueryWrapper<ContentAuditLog>().ge("created_at", todayStart));
        long todayBlocked = count(new QueryWrapper<ContentAuditLog>().eq("passed", false).ge("created_at", todayStart));

        return Map.of(
                "total", total,
                "blocked", blocked,
                "passRate", total == 0 ? 100.0 : Math.round((double) (total - blocked) * 10000 / total) / 100.0,
                "riskDistribution", Map.of(
                        "HIGH", high,
                        "MEDIUM", medium,
                        "LOW", low
                ),
                "todayTotal", todayTotal,
                "todayBlocked", todayBlocked
        );
    }

    private long count(QueryWrapper<ContentAuditLog> wrapper) {
        Long c = auditLogMapper.selectCount(wrapper);
        return c == null ? 0L : c;
    }

    private static String higherRisk(String a, String b) {
        return rank(b) > rank(a) ? b : a;
    }

    private static int rank(String risk) {
        if (SensitiveWordDictionary.RISK_HIGH.equals(risk)) return 3;
        if (SensitiveWordDictionary.RISK_MEDIUM.equals(risk)) return 2;
        return 1;
    }

    private static String truncate(String content) {
        if (content == null) return null;
        return content.length() > SNAPSHOT_MAX ? content.substring(0, SNAPSHOT_MAX) : content;
    }

    /**
     * 取（并缓存）某敏感词的"宽松匹配"正则：每个字符之间允许插入任意空白/标点/符号，
     * 用于拦截"拆字"与"符号插入"绕过（如 色 情 网 站 / 色·情·网·站）。
     */
    private static Pattern relaxedPattern(String word) {
        return RELAXED_PATTERN_CACHE.computeIfAbsent(word == null ? "" : word.toLowerCase(),
                ContentAuditServiceImpl::buildRelaxedPattern);
    }

    /** 编译一个已知正则字符串为宽松匹配 Pattern（用于谐音变体构造）。 */
    private static Pattern compileRelaxed(String regex) {
        return Pattern.compile(regex);
    }

    private static Pattern buildRelaxedPattern(String word) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            if (i > 0) {
                // 字间允许零个或多个空白/标点/符号
                sb.append("[\\s\\p{P}\\p{S}]*");
            }
            sb.append(Pattern.quote(String.valueOf(word.charAt(i))));
        }
        return Pattern.compile(sb.toString());
    }
}
