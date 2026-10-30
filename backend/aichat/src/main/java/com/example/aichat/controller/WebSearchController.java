package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.dto.search.SourceCitation;
import com.example.aichat.entity.SearchResult;
import com.example.aichat.service.SearchCitationService;
import com.example.aichat.service.WebSearchService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网络搜索控制器
 * 提供AI互联网资料查找功能的RESTful接口
 *
 * 安全加固（P0）：外部搜索会向第三方搜索引擎发起真实请求并消耗服务器资源，
 * 全部接口要求登录并叠加限流（aiApi：30次/分钟），防止匿名用户刷流量。
 */
@RestController
@RequestMapping("/api/search")
@PreAuthorize("isAuthenticated()")
public class WebSearchController {

    private static final Logger logger = LoggerFactory.getLogger(WebSearchController.class);

    @Autowired
    private WebSearchService webSearchService;

    @Autowired
    private SearchCitationService searchCitationService;

    /**
     * 执行网络搜索
     * POST /api/search/execute
     *
     * @param requestBody 包含查询参数的请求体
     * @return 搜索结果列表
     */
    @PostMapping("/execute")
    @RateLimiter(name = "aiApi", fallbackMethod = "rateLimited")
    public R<Map<String, Object>> executeSearch(@RequestBody Map<String, Object> requestBody) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取参数
            String query = (String) requestBody.get("query");
            Integer maxResults = requestBody.get("maxResults") != null ? 
                    (Integer) requestBody.get("maxResults") : 10;
            Boolean verifyEnabled = requestBody.get("verifyEnabled") != null ? 
                    (Boolean) requestBody.get("verifyEnabled") : false;

            // 参数验证
            if (query == null || query.trim().isEmpty()) {
                return R.error("搜索关键词不能为空");
            }

            // 记录日志
            logger.info("执行网络搜索: query={}, maxResults={}, verifyEnabled={}", 
                    query, maxResults, verifyEnabled);

            // 执行搜索
            List<SearchResult> results = webSearchService.searchWithVerification(query, maxResults, verifyEnabled);

            // 提取关键信息
            Map<String, Object> extractedInfo = webSearchService.extractKeyInformation(results);

            // 构建响应（新增结构化来源引用 citations，对既有字段 results 保持兼容）
            List<SourceCitation> citations = searchCitationService.buildCitations(results);
            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("results", results);
            response.put("resultCount", results.size());
            response.put("citations", citations);
            // A1：前端约定的 sources 数组 [{title,url,snippet}]，无结果时为空
            response.put("sources", searchCitationService.toSourceMaps(citations));
            response.put("extractedInfo", extractedInfo);
            response.put("durationMs", System.currentTimeMillis() - startTime);
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            logger.info("搜索完成: query={}, 结果数={}, 耗时={}ms", 
                    query, results.size(), System.currentTimeMillis() - startTime);

            return R.success(response);

        } catch (Exception e) {
            logger.error("搜索失败: {}", e.getMessage(), e);
            return R.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 快速搜索（简化接口）
     * GET /api/search/quick
     *
     * @param query 搜索关键词
     * @param limit 结果数量限制
     * @return 搜索结果列表
     */
    @GetMapping("/quick")
    @RateLimiter(name = "aiApi", fallbackMethod = "rateLimitedQuick")
    public R<List<SearchResult>> quickSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            logger.info("快速搜索: query={}, limit={}", query, limit);
            
            List<SearchResult> results = webSearchService.search(query, limit);
            
            logger.info("快速搜索完成: query={}, 结果数={}", query, results.size());
            
            return R.success(results);

        } catch (Exception e) {
            logger.error("快速搜索失败: {}", e.getMessage(), e);
            return R.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 快速搜索限流兜底。
     */
    public R<List<SearchResult>> rateLimitedQuick(String query, int limit, Throwable t) {
        return R.error(429, "搜索过于频繁，请稍后再试");
    }

    /**
     * 生成搜索关键词
     * POST /api/search/keywords
     *
     * @param requestBody 包含用户问题的请求体
     * @return 关键词列表
     */
    @PostMapping("/keywords")
    @RateLimiter(name = "aiApi", fallbackMethod = "rateLimitedKeywords")
    public R<List<String>> generateKeywords(@RequestBody Map<String, Object> requestBody) {
        try {
            String question = (String) requestBody.get("question");
            
            if (question == null || question.trim().isEmpty()) {
                return R.error("问题不能为空");
            }

            logger.info("生成搜索关键词: question={}", question);

            List<String> keywords = webSearchService.generateSearchKeywords(question);

            logger.info("关键词生成完成: count={}", keywords.size());

            return R.success(keywords);

        } catch (Exception e) {
            logger.error("关键词生成失败: {}", e.getMessage(), e);
            return R.error("关键词生成失败: " + e.getMessage());
        }
    }

    /**
     * 关键词生成限流兜底。
     */
    public R<List<String>> rateLimitedKeywords(Map<String, Object> requestBody, Throwable t) {
        return R.error(429, "操作过于频繁，请稍后再试");
    }

    /**
     * 获取搜索历史
     * GET /api/search/history
     *
     * @param limit 返回数量限制
     * @return 搜索历史记录
     */
    @GetMapping("/history")
    public R<List<Map<String, Object>>> getSearchHistory(
            @RequestParam(defaultValue = "20") int limit) {

        try {
            List<Map<String, Object>> history = webSearchService.getSearchHistory(limit);
            
            logger.info("获取搜索历史: count={}", history.size());
            
            return R.success(history);

        } catch (Exception e) {
            logger.error("获取搜索历史失败: {}", e.getMessage(), e);
            return R.error("获取搜索历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取搜索统计信息
     * GET /api/search/stats
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> getSearchStats() {
        try {
            List<Map<String, Object>> history = webSearchService.getSearchHistory(100);
            
            // 计算统计信息
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSearches", history.size());
            
            // 计算平均响应时间
            double avgDuration = history.stream()
                    .mapToLong(h -> (Long) h.get("durationMs"))
                    .average()
                    .orElse(0);
            stats.put("avgResponseTimeMs", Math.round(avgDuration));
            
            // 计算平均结果数
            double avgResults = history.stream()
                    .mapToInt(h -> (Integer) h.get("resultCount"))
                    .average()
                    .orElse(0);
            stats.put("avgResultsPerSearch", Math.round(avgResults));
            
            stats.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            return R.success(stats);

        } catch (Exception e) {
            logger.error("获取搜索统计失败: {}", e.getMessage(), e);
            return R.error("获取搜索统计失败: " + e.getMessage());
        }
    }

    /**
     * 验证搜索结果
     * POST /api/search/verify
     *
     * @param requestBody 包含结果列表的请求体
     * @return 验证后的结果列表
     */
    @PostMapping("/verify")
    @RateLimiter(name = "aiApi", fallbackMethod = "rateLimitedVerify")
    public R<List<SearchResult>> verifyResults(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<SearchResult> results = (List<SearchResult>) requestBody.get("results");
            
            if (results == null || results.isEmpty()) {
                return R.error("结果列表不能为空");
            }

            logger.info("验证搜索结果: count={}", results.size());

            List<SearchResult> verifiedResults = webSearchService.verifyResults(results);

            logger.info("验证完成: verifiedCount={}", 
                    verifiedResults.stream().filter(SearchResult::getVerified).count());

            return R.success(verifiedResults);

        } catch (Exception e) {
            logger.error("验证结果失败: {}", e.getMessage(), e);
            return R.error("验证失败: " + e.getMessage());
        }
    }

    /**
     * 结果验证限流兜底。
     */
    public R<List<SearchResult>> rateLimitedVerify(Map<String, Object> requestBody, Throwable t) {
        return R.error(429, "操作过于频繁，请稍后再试");
    }
}
