package com.example.aichat.service.impl;

import com.example.aichat.common.exception.BusinessException;
import com.example.aichat.common.exception.UserErrorCode;
import com.example.aichat.dto.search.DeepResearchReport;
import com.example.aichat.dto.search.SourceCitation;
import com.example.aichat.entity.SearchResult;
import com.example.aichat.service.DeepResearchService;
import com.example.aichat.service.WebSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 深度研究服务默认实现（简化版，离线可测）。
 *
 * 多轮自主搜索循环：
 *   第1轮：原始 query 搜索；
 *   第2轮：从第1轮结果提取关键词，生成 "xxx 原理" / "xxx 应用" 子查询搜索；
 *   第3轮：生成 "xxx 趋势" / "xxx 案例" 子查询搜索；
 *   最后整合所有结果生成结构化研究报告。
 *
 * 搜索调用 WebSearchService（测试时 mock），不依赖真实网络；研究状态存内存。
 */
@Service
public class DeepResearchServiceImpl implements DeepResearchService {

    private static final Logger logger = LoggerFactory.getLogger(DeepResearchServiceImpl.class);

    @Autowired
    private WebSearchService webSearchService;

    /**
     * 内存中的研究任务表。
     */
    private final Map<String, ResearchTask> tasks = new ConcurrentHashMap<>();

    private static final int DEFAULT_MAX_ROUNDS = 3;
    private static final int ROUND1_TOP_N = 5;
    private static final int SUB_TOP_N = 3;

    @Override
    public String startResearch(Long userId, String query, Integer maxRounds) {
        return startResearchWithListener(userId, query, maxRounds, null);
    }

    @Override
    public String startResearchWithListener(Long userId, String query, Integer maxRounds, ProgressListener listener) {
        if (userId == null) {
            throw new BusinessException(UserErrorCode.TOKEN_INVALID);
        }
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("研究问题不能为空");
        }
        int rounds = maxRounds == null ? DEFAULT_MAX_ROUNDS : maxRounds;
        if (rounds < 1) {
            rounds = 1;
        }
        if (rounds > 3) {
            rounds = 3;
        }

        ResearchTask task = new ResearchTask();
        task.researchId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        task.userId = userId;
        task.query = query.trim();
        task.maxRounds = rounds;
        task.status = "pending";
        task.currentRound = 0;
        task.progress = 0;
        task.createdAt = LocalDateTime.now();
        task.allResults = new ArrayList<>();
        task.roundDetails = new ArrayList<>();
        tasks.put(task.researchId, task);

        runResearch(task, listener);
        return task.researchId;
    }

    /**
     * 同步执行多轮研究循环（搜索为 mock，离线快速返回）。
     * listener 非空时每步推送 SSE 进度事件。
     */
    private void runResearch(ResearchTask task, ProgressListener listener) {
        try {
            emit(listener, "start", Map.of("researchId", task.researchId, "query", task.query));

            // 第1轮：原始 query
            task.status = "searching";
            task.currentRound = 1;
            task.progress = 20;
            emit(listener, "round", Map.of("round", 1, "query", task.query, "status", "searching"));
            List<SearchResult> round1 = safeSearch(task.query, ROUND1_TOP_N);
            task.allResults.addAll(round1);
            task.roundDetails.add(buildRoundDetail(1, task.query, round1));
            emit(listener, "roundResult", Map.of("round", 1, "resultCount", round1.size()));

            // 提取关键词用于生成子查询
            String keyword = extractKeyTerm(round1, task.query);

            if (task.maxRounds >= 2) {
                task.status = "round2";
                task.currentRound = 2;
                task.progress = 50;
                List<String> subQueries = List.of(keyword + " 原理", keyword + " 应用");
                for (String sq : subQueries) {
                    emit(listener, "round", Map.of("round", 2, "query", sq, "status", "searching"));
                    List<SearchResult> sub = safeSearch(sq, SUB_TOP_N);
                    task.allResults.addAll(sub);
                    task.roundDetails.add(buildRoundDetail(2, sq, sub));
                    emit(listener, "roundResult", Map.of("round", 2, "query", sq, "resultCount", sub.size()));
                }
            }

            if (task.maxRounds >= 3) {
                task.currentRound = 3;
                task.progress = 75;
                List<String> subQueries = List.of(keyword + " 趋势", keyword + " 案例");
                for (String sq : subQueries) {
                    emit(listener, "round", Map.of("round", 3, "query", sq, "status", "searching"));
                    List<SearchResult> sub = safeSearch(sq, SUB_TOP_N);
                    task.allResults.addAll(sub);
                    task.roundDetails.add(buildRoundDetail(3, sq, sub));
                    emit(listener, "roundResult", Map.of("round", 3, "query", sq, "resultCount", sub.size()));
                }
            }

            // 整合报告
            task.status = "integrating";
            task.progress = 90;
            emit(listener, "integrating", Map.of("progress", 90));
            task.completedAt = LocalDateTime.now();
            buildReport(task);

            task.status = "completed";
            task.progress = 100;
            emit(listener, "complete", task.report);
        } catch (Exception e) {
            logger.error("研究任务执行失败: {}", task.researchId, e);
            task.status = "failed";
            task.progress = 100;
            task.completedAt = LocalDateTime.now();
            emit(listener, "error", Map.of("message", "研究执行失败"));
        }
    }

    /** 安全地推送进度事件，listener 为 null 或推送异常时静默忽略。 */
    private void emit(ProgressListener listener, String eventName, Object data) {
        if (listener == null) return;
        try {
            listener.onProgress(eventName, data);
        } catch (Exception e) {
            logger.debug("推送研究进度事件失败（客户端可能已断开）: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getResearchStatus(Long userId, String researchId) {
        ResearchTask task = requireOwner(userId, researchId);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("researchId", task.researchId);
        status.put("query", task.query);
        status.put("status", task.status);
        status.put("currentRound", task.currentRound);
        status.put("maxRounds", task.maxRounds);
        status.put("progress", task.progress);
        status.put("createdAt", task.createdAt);
        status.put("completedAt", task.completedAt);
        return status;
    }

    @Override
    public DeepResearchReport getResearchResult(Long userId, String researchId) {
        ResearchTask task = requireOwner(userId, researchId);
        return task.report;
    }

    @Override
    public List<DeepResearchReport> listResearches(Long userId, int page, int size) {
        if (userId == null) {
            throw new BusinessException(UserErrorCode.TOKEN_INVALID);
        }
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        List<ResearchTask> mine = tasks.values().stream()
                .filter(t -> userId.equals(t.userId))
                .sorted(Comparator.comparing((ResearchTask t) -> t.createdAt).reversed())
                .toList();
        int fromIndex = Math.min((page - 1) * size, mine.size());
        int toIndex = Math.min(fromIndex + size, mine.size());
        List<DeepResearchReport> result = new ArrayList<>();
        for (ResearchTask t : mine.subList(fromIndex, toIndex)) {
            result.add(t.report);
        }
        return result;
    }

    /**
     * 校验任务存在且属于当前用户（IDOR 防护）。
     */
    private ResearchTask requireOwner(Long userId, String researchId) {
        ResearchTask task = tasks.get(researchId);
        if (task == null) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND, "研究任务不存在");
        }
        if (userId == null || !userId.equals(task.userId)) {
            throw new BusinessException(UserErrorCode.TOKEN_INVALID, "无权访问该研究任务");
        }
        return task;
    }

    /**
     * 安全调用搜索，异常时返回空列表（离线可测）。
     */
    private List<SearchResult> safeSearch(String query, int topN) {
        try {
            List<SearchResult> r = webSearchService.search(query, topN);
            return r == null ? new ArrayList<>() : r;
        } catch (Exception e) {
            logger.warn("子搜索失败，忽略: query={}, err={}", query, e.getMessage());
            return new ArrayList<>();
        }
    }

    private DeepResearchReport.RoundDetail buildRoundDetail(int round, String query, List<SearchResult> results) {
        List<String> topTitles = new ArrayList<>();
        int limit = Math.min(3, results.size());
        for (int i = 0; i < limit; i++) {
            SearchResult r = results.get(i);
            topTitles.add(r.getTitle());
        }
        return DeepResearchReport.RoundDetail.builder()
                .round(round)
                .query(query)
                .resultCount(results.size())
                .topTitles(topTitles)
                .build();
    }

    /**
     * 从第1轮结果中提取代表性关键词，作为子查询基础；无结果时退回原始 query 的首个词。
     */
    private String extractKeyTerm(List<SearchResult> round1, String originalQuery) {
        // 统计中文 bigram 频次
        Map<String, Integer> freq = new LinkedHashMap<>();
        StringBuilder corpus = new StringBuilder(originalQuery == null ? "" : originalQuery);
        for (SearchResult r : round1) {
            if (r.getTitle() != null) {
                corpus.append(' ').append(r.getTitle());
            }
            if (r.getSnippet() != null) {
                corpus.append(' ').append(r.getSnippet());
            }
        }
        String text = corpus.toString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\\u4e00-\\u9fff]+").matcher(text);
        while (m.find()) {
            String run = m.group();
            for (int i = 0; i + 2 <= run.length(); i++) {
                String bg = run.substring(i, i + 2);
                freq.merge(bg, 1, Integer::sum);
            }
        }
        // 取频次最高的 bigram，拼成一个有意义的词（频次相同时取先出现者）
        return freq.entrySet().stream()
                .max(Map.Entry.<String, Integer>comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(originalQuery == null || originalQuery.trim().isEmpty() ? "主题" : originalQuery.trim());
    }

    /**
     * 整合所有搜索结果，生成结构化报告。
     */
    private void buildReport(ResearchTask task) {
        // 来源按 url 去重，编号 1..M
        Map<String, SourceCitation> sourceMap = new LinkedHashMap<>();
        int idx = 1;
        for (SearchResult r : task.allResults) {
            String url = r.getUrl() == null ? r.getTitle() : r.getUrl();
            if (url == null || url.isEmpty()) {
                continue;
            }
            if (sourceMap.containsKey(url)) {
                continue;
            }
            sourceMap.put(url, SourceCitation.builder()
                    .index(idx++)
                    .title(r.getTitle())
                    .url(r.getUrl())
                    .snippet(r.getSnippet())
                    .source(r.getSource())
                    .publishTime(r.getPublishTime())
                    .relevanceScore(r.getRelevanceScore())
                    .build());
        }
        List<SourceCitation> sources = new ArrayList<>(sourceMap.values());

        // 关键发现：每个结果取 title + snippet 前 100 字
        List<String> keyFindings = new ArrayList<>();
        java.util.Set<String> seen = new LinkedHashSet<>();
        for (SearchResult r : task.allResults) {
            String title = r.getTitle() == null ? "" : r.getTitle();
            if (title.isEmpty() || seen.contains(title)) {
                continue;
            }
            seen.add(title);
            String snip = r.getSnippet() == null ? "" : r.getSnippet();
            if (snip.length() > 100) {
                snip = snip.substring(0, 100);
            }
            keyFindings.add(title + "：" + snip);
        }

        // 整合摘要：高频关键词拼接
        String summary = buildSummary(task.query, task.allResults);

        DeepResearchReport report = DeepResearchReport.builder()
                .researchId(task.researchId)
                .query(task.query)
                .status(task.status)
                .currentRound(task.currentRound)
                .progress(task.progress)
                .summary(summary)
                .keyFindings(keyFindings)
                .sources(sources)
                .roundDetails(task.roundDetails)
                .createdAt(task.createdAt)
                .completedAt(task.completedAt)
                .build();
        task.report = report;
    }

    /**
     * 从所有 snippet 提取高频关键词，拼接为结构化摘要。
     */
    private String buildSummary(String query, List<SearchResult> all) {
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (SearchResult r : all) {
            String text = (r.getTitle() == null ? "" : r.getTitle()) + " " + (r.getSnippet() == null ? "" : r.getSnippet());
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\\u4e00-\\u9fff]+").matcher(text);
            while (m.find()) {
                String run = m.group();
                for (int i = 0; i + 2 <= run.length(); i++) {
                    String bg = run.substring(i, i + 2);
                    freq.merge(bg, 1, Integer::sum);
                }
            }
        }
        List<String> topTerms = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        StringBuilder sb = new StringBuilder("关于「").append(query).append("」，主要涉及以下方面：");
        if (topTerms.isEmpty()) {
            sb.append("暂未检索到足够的公开信息，建议补充关键词后重新研究。");
        } else {
            int n = 1;
            for (String term : topTerms) {
                sb.append(n++).append(". ").append(term).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * 内存研究任务。
     */
    private static class ResearchTask {
        String researchId;
        Long userId;
        String query;
        int maxRounds;
        String status;
        int currentRound;
        int progress;
        LocalDateTime createdAt;
        LocalDateTime completedAt;
        List<SearchResult> allResults;
        List<DeepResearchReport.RoundDetail> roundDetails;
        DeepResearchReport report;
    }
}
